package db.migration.sqlite

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource

private val logger = KotlinLogging.logger {}

/**
 * Fork-only consolidated cleanup migration. All fork-specific removals (and the last-login
 * date addition) are gathered here in a single file, instead of the previous scattered
 * migrations:
 * - V20261102140000__remove_authentication_activity_add_last_login.sql
 * - V20261105120000__remove_page_hash.sql
 * - V20261110000000__remove_series_metadata_extras.sql
 *
 * The version is pinned to 99... so that it always runs **last**, after every upstream
 * migration, and after the next upstream merge as well. Future fork-specific schema removals
 * should be appended here as long as this version has not been applied to a live database yet;
 * once applied, create a new V9999... file instead.
 *
 * Every statement is idempotent: tables are dropped with IF EXISTS, and column changes are
 * guarded by PRAGMA checks. This makes it safe both on databases where the superseded
 * migrations above were already applied (the corresponding statements become no-ops) and on
 * fresh databases (this migration performs the full removal itself).
 */
class V99991110000000__fork_db_cleanup : BaseJavaMigration() {
  override fun migrate(context: Context) {
    val jdbcTemplate = JdbcTemplate(SingleConnectionDataSource(context.connection, true))

    // --- Authentication activity tracking is not needed in this fork, only the last login date per user is kept
    jdbcTemplate.execute("DROP INDEX IF EXISTS idx__authentication_activity__user_id")
    jdbcTemplate.execute("DROP TABLE IF EXISTS AUTHENTICATION_ACTIVITY")
    addColumn(jdbcTemplate, "USER", "LAST_LOGIN_DATE", "datetime NULL DEFAULT NULL")

    // --- Page-level duplicate detection and the historical event log are removed.
    // Book.FILE_HASH (file level) is kept: it is used for scanning optimizations and duplicate file detection.
    jdbcTemplate.execute("DROP TABLE IF EXISTS PAGE_HASH_THUMBNAIL")
    jdbcTemplate.execute("DROP TABLE IF EXISTS PAGE_HASH")
    dropColumn(jdbcTemplate, "MEDIA_PAGE", "FILE_HASH")
    dropColumn(jdbcTemplate, "LIBRARY", "HASH_PAGES")
    jdbcTemplate.execute("DROP TABLE IF EXISTS HISTORICAL_EVENT_PROPERTIES")
    jdbcTemplate.execute("DROP TABLE IF EXISTS HISTORICAL_EVENT")

    // --- Series metadata extras (language, publisher, age rating, total book count) are unused in this fork.
    val seriesMetadataColumns = listOf(
      "LANGUAGE", "LANGUAGE_LOCK",
      "PUBLISHER", "PUBLISHER_LOCK",
      "AGE_RATING", "AGE_RATING_LOCK",
      "TOTAL_BOOK_COUNT", "TOTAL_BOOK_COUNT_LOCK",
    )
    for (column in seriesMetadataColumns) {
      dropColumn(jdbcTemplate, "SERIES_METADATA", column)
    }
    jdbcTemplate.execute("DROP TABLE IF EXISTS SERIES_METADATA_LINK")
    jdbcTemplate.execute("DROP TABLE IF EXISTS SERIES_METADATA_ALTERNATE_TITLE")
    jdbcTemplate.execute("DROP TABLE IF EXISTS SERIES_METADATA_SHARING")
  }

  private fun columns(
    jdbcTemplate: JdbcTemplate,
    table: String,
  ): Set<String> =
    jdbcTemplate.queryForList("PRAGMA table_info($table)")
      .mapNotNull { it["name"] as? String }
      .map { it.uppercase() }
      .toSet()

  private fun addColumn(
    jdbcTemplate: JdbcTemplate,
    table: String,
    column: String,
    definition: String,
  ) {
    if (column !in columns(jdbcTemplate, table)) {
      logger.info { "Adding $table.$column column" }
      jdbcTemplate.execute("ALTER TABLE $table ADD COLUMN $column $definition")
    }
  }

  private fun dropColumn(
    jdbcTemplate: JdbcTemplate,
    table: String,
    column: String,
  ) {
    if (column in columns(jdbcTemplate, table)) {
      logger.info { "Dropping $table.$column column" }
      jdbcTemplate.execute("ALTER TABLE $table DROP COLUMN $column")
    }
  }
}
