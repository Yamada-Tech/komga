package org.gotson.komga.interfaces.api.rest.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class SeriesReadingStatDailyDto(
  val seriesId: String,
  val day: LocalDate,
  val uniqueReaders: Int,
  val completedBooks: Int,
  val progressEvents: Int,
  val lastReadAt: LocalDateTime?,
)

data class TopSeriesReadingStatDto(
  val seriesId: String,
  val totalProgressEvents: Int,
)
