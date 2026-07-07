package org.gotson.komga.interfaces.api.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.infrastructure.configuration.KomgaProperties
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("api/v1/history", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.HISTORY)
class HistoryController(
  komgaProperties: KomgaProperties,
) {
  private val configDir = Path(komgaProperties.configDir ?: "${System.getProperty("user.home")}/.komga")

  @PostMapping("authentication-activity/clear")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Request authentication activity optimization",
    description = "Creates a flag file in the config directory to signal that an optimization is needed. Required role: **ADMIN**",
  )
  fun requestAuthenticationActivityClear() {
    try {
      val flagFile = configDir.resolve("need_optimize.flag")
      if (!flagFile.exists()) {
        Files.createFile(flagFile)
        logger.info { "Created optimization flag file: $flagFile" }
      } else {
        logger.debug { "Optimization flag file already exists: $flagFile" }
      }
    } catch (e: Exception) {
      logger.error(e) { "Failed to create optimization flag file" }
      throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create flag file: ${e.message}")
    }
  }
}
