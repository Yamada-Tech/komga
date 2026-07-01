package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.SeriesReadingStatDaily
import java.time.LocalDate

interface SeriesReadingStatDailyRepository {
  fun recomputeAndUpsert(
    seriesId: String,
    day: LocalDate,
  )

  fun findBySeriesIdAndDayBetween(
    seriesId: String,
    from: LocalDate,
    to: LocalDate,
  ): List<SeriesReadingStatDaily>

  fun findTopSeriesByPeriod(
    from: LocalDate,
    to: LocalDate,
    limit: Int,
  ): List<Pair<String, Int>>

  fun deleteBySeriesId(seriesId: String)

  fun deleteAll()
}
