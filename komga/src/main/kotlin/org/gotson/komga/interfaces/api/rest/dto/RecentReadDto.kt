package org.gotson.komga.interfaces.api.rest.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class RecentReadDto(
  val userId: String,
  val userEmail: String,
  val bookId: String,
  val bookTitle: String,
  val seriesId: String,
  val seriesTitle: String,
  val page: Int,
  val pagesCount: Int?,
  val completed: Boolean,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val readDate: LocalDateTime,
)
