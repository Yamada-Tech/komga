package org.gotson.komga.domain.model

import com.github.f4b6a3.tsid.TsidCreator
import java.time.LocalDateTime

data class ReadingEvent(
  val userId: String,
  val bookId: String,
  val seriesId: String,
  val type: ReadingEventType,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val id: String = TsidCreator.getTsid256().toString(),
)

enum class ReadingEventType {
  PROGRESSED,
  COMPLETED,
}
