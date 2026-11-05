package db.migration.sqlite

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource

private val logger = KotlinLogging.logger {}

/**
 * Adds the COLLECTION.PUBLIC column. Collections are public by default, so that existing
 * collections remain visible to all users.
 *
 * Some databases may already contain this column, added by a previous build whose migration
 * file is no longer available (20260701000000). In that case the column is kept as-is and its
 * rows are marked public, since that build had no actual use for the flag and the current
 * visibility must not change.
 */
class V20261102120000__collection_public_column : BaseJavaMigration() {
  override fun migrate(context: Context) {
    val jdbcTemplate = JdbcTemplate(SingleConnectionDataSource(context.connection, true))

    val columns =
      jdbcTemplate.queryForList("PRAGMA table_info(COLLECTION)")
        .mapNotNull { it["name"] as? String }
        .map { it.uppercase() }

    if ("PUBLIC" in columns) {
      logger.info { "COLLECTION.PUBLIC already exists, marking existing collections as public" }
      jdbcTemplate.execute("UPDATE COLLECTION SET PUBLIC = 1 WHERE PUBLIC = 0")
      return
    }

    logger.info { "Adding COLLECTION.PUBLIC column" }
    jdbcTemplate.execute("ALTER TABLE COLLECTION ADD COLUMN PUBLIC boolean NOT NULL DEFAULT 1")
  }
}
