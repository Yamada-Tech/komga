package org.gotson.komga.infrastructure.maintenance

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.infrastructure.configuration.KomgaProperties
import org.gotson.komga.infrastructure.util.RESTART_PARENT_PID_ENV
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Service
class DatabaseMaintenanceService(
  private val komgaProperties: KomgaProperties,
  @Qualifier("sqliteDataSourceRW") private val mainDataSource: DataSource,
  @Qualifier("tasksDataSourceRW") private val tasksDataSource: DataSource,
  @Qualifier("sessionsDataSource") private val sessionsDataSource: ObjectProvider<DataSource>,
  private val applicationArguments: ApplicationArguments,
  private val applicationContext: ConfigurableApplicationContext,
) {
  private val restartRequested = AtomicBoolean(false)

  fun optimizeAndRestart() = restart(optimizeFirst = true)

  fun restart() = restart(optimizeFirst = false)

  private fun restart(optimizeFirst: Boolean) {
    if (!restartRequested.compareAndSet(false, true)) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "A restart is already in progress")
    }
    Thread {
      try {
        // let the HTTP response be flushed before any heavy operation
        Thread.sleep(1000)
        if (optimizeFirst) vacuumAll()
      } catch (e: Exception) {
        logger.error(e) { "Error while optimizing databases" }
      } finally {
        try {
          spawnRestartProcess()
        } catch (e: Exception) {
          logger.error(e) { "Could not spawn restart process, shutting down without restart" }
        }
        logger.info { "Stopping application so that the new instance can take over" }
        applicationContext.close()
        // force exit in case non-daemon threads (e.g. tray icon) keep the JVM alive
        Thread.sleep(5000)
        logger.info { "Forcing JVM exit" }
        Runtime.getRuntime().halt(0)
      }
    }.start()
  }

  private fun vacuumAll() {
    logger.info { "Optimizing databases (VACUUM)" }
    vacuum(mainDataSource, File(komgaProperties.database.file))
    vacuum(tasksDataSource, File(komgaProperties.tasksDb.file))
    if (komgaProperties.jdbcSessionEnabled) {
      sessionsDataSource.getIfAvailable()?.let { vacuum(it, File(komgaProperties.sessionsDb.file)) }
    }
    logger.info { "Databases optimized" }
  }

  private fun vacuum(
    dataSource: DataSource,
    file: File,
  ) {
    if (!file.exists()) {
      logger.warn { "Database file ${file.path} does not exist, skipping" }
      return
    }
    val sizeBefore = file.length()
    dataSource.connection.use { connection ->
      connection.autoCommit = true
      connection.createStatement().use { statement ->
        statement.execute("PRAGMA busy_timeout=120000")
        statement.execute("VACUUM")
        statement.execute("ANALYZE")
        statement.execute("PRAGMA wal_checkpoint(TRUNCATE)")
      }
    }
    logger.info { "Optimized database ${file.name}: $sizeBefore -> ${file.length()} bytes" }
  }

  private fun spawnRestartProcess() {
    val command = buildRestartCommand()
    logger.info { "Spawning restart process: ${command.joinToString(" ")}" }
    ProcessBuilder(command)
      .directory(File(System.getProperty("user.dir")))
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .apply { environment()[RESTART_PARENT_PID_ENV] = ProcessHandle.current().pid().toString() }
      .start()
  }

  private fun buildRestartCommand(): List<String> {
    val javaBinary = File(System.getProperty("java.home"), "bin${File.separator}java").absolutePath
    val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
    val vmArguments =
      runtimeMxBean.inputArguments
        .filterNot { it.startsWith("-agentlib") || it.startsWith("-agentpath") || it.startsWith("-javaagent") }
    val classpath = System.getProperty("java.class.path").split(File.pathSeparatorChar)
    val applicationArgs = applicationArguments.sourceArgs.toList()
    return if (classpath.size == 1 && classpath.first().endsWith(".jar")) {
      listOf(javaBinary) + vmArguments + listOf("-jar", classpath.first()) + applicationArgs
    } else {
      listOf(javaBinary) + vmArguments + listOf("-classpath", classpath.joinToString(File.pathSeparator), "org.gotson.komga.ApplicationKt") + applicationArgs
    }
  }
}
