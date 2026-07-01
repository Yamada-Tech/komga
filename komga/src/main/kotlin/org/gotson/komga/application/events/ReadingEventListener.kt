package org.gotson.komga.application.events

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.model.DomainEvent
import org.gotson.komga.domain.model.ReadingEvent
import org.gotson.komga.domain.model.ReadingEventType
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.ReadingEventRepository
import org.gotson.komga.domain.persistence.SeriesReadingStatDailyRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ReadingEventListener(
  private val bookRepository: BookRepository,
  private val readingEventRepository: ReadingEventRepository,
  private val seriesReadingStatDailyRepository: SeriesReadingStatDailyRepository,
) {
  @EventListener
  fun onReadProgressChanged(event: DomainEvent.ReadProgressChanged) {
    val progress = event.progress
    val seriesId = bookRepository.getSeriesIdOrNull(progress.bookId) ?: return

    val type = if (progress.completed) ReadingEventType.COMPLETED else ReadingEventType.PROGRESSED

    val readingEvent =
      ReadingEvent(
        userId = progress.userId,
        bookId = progress.bookId,
        seriesId = seriesId,
        type = type,
        createdAt = progress.readDate,
      )

    try {
      readingEventRepository.insert(readingEvent)
      seriesReadingStatDailyRepository.recomputeAndUpsert(seriesId, progress.readDate.toLocalDate())
    } catch (e: Exception) {
      logger.warn(e) { "Failed to record reading event for book ${progress.bookId}" }
    }
  }
}
