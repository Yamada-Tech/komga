package org.gotson.komga.infrastructure.datasource

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

data class SeriesReadingStatsAggregate(
  val seriesId: String,
  val uniqueReaders: Int,
  val completedBooks: Int,
  val progressEvents: Int,
  val lastReadAt: LocalDateTime?,
)

@Repository
class SeriesReadingStatsRollupRepository(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun aggregateSeriesStats(
    from: LocalDate,
    to: LocalDate,
    limit: Int,
  ): List<SeriesReadingStatsAggregate> {
    val sql =
      """
      SELECT
        series_id,
        -- Daily unique_readers cannot be merged exactly across days without event-level DISTINCT user counting.
        -- We use MAX(unique_readers) as a stable engagement signal for ranking.
        MAX(unique_readers) AS unique_readers,
        SUM(completed_books) AS completed_books,
        SUM(progress_events) AS progress_events,
        MAX(last_read_at) AS last_read_at
      FROM series_reading_stats_daily
      WHERE day >= :from AND day <= :to
      GROUP BY series_id
      ORDER BY
        -- Heavier weight to broad engagement, then completion, then raw progress volume.
        (MAX(unique_readers) * 100 + SUM(completed_books) * 50 + SUM(progress_events)) DESC,
        MAX(last_read_at) DESC
      LIMIT :limit
      """.trimIndent()

    val params =
      MapSqlParameterSource()
        .addValue("from", from)
        .addValue("to", to)
        .addValue("limit", limit)

    return jdbcTemplate.query(sql, params) { rs, _ ->
      SeriesReadingStatsAggregate(
        seriesId = rs.getString("series_id"),
        uniqueReaders = rs.getInt("unique_readers"),
        completedBooks = rs.getInt("completed_books"),
        progressEvents = rs.getInt("progress_events"),
        lastReadAt = rs.getTimestamp("last_read_at")?.toLocalDateTime(),
      )
    }
  }
}
