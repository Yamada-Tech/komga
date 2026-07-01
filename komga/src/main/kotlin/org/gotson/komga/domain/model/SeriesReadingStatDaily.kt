package org.gotson.komga.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class SeriesReadingStatDaily(
  val seriesId: String,
  val day: LocalDate,
  val uniqueReaders: Int,
  val completedBooks: Int,
  val progressEvents: Int,
  val lastReadAt: LocalDateTime?,
)
