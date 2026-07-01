package org.gotson.komga.infrastructure.jooq.main

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.ReadProgress
import org.gotson.komga.domain.model.ReadingEvent
import org.gotson.komga.domain.model.ReadingEventType
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.infrastructure.datasource.SeriesReadingStatsRollupRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
class ReadingEventDaoTest(
  @Autowired private val readingEventDao: ReadingEventDao,
  @Autowired private val seriesReadingStatDailyDao: SeriesReadingStatDailyDao,
  @Autowired private val seriesReadingStatsRollupRepository: SeriesReadingStatsRollupRepository,
  @Autowired private val userRepository: KomgaUserRepository,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val libraryRepository: LibraryRepository,
) {
  private val library = makeLibrary()
  private val series = makeSeries("Series")
  private val series2 = makeSeries("Series 2")
  private val user1 = KomgaUser("readevt_user1@example.org", "")
  private val user2 = KomgaUser("readevt_user2@example.org", "")
  private val book1 = makeBook("ReadEvtBook1")
  private val book2 = makeBook("ReadEvtBook2")
  private val book3 = makeBook("ReadEvtBook3")

  @BeforeAll
  fun setup() {
    libraryRepository.insert(library)
    seriesRepository.insert(series.copy(libraryId = library.id))
    seriesRepository.insert(series2.copy(libraryId = library.id))
    userRepository.insert(user1)
    userRepository.insert(user2)
    bookRepository.insert(book1.copy(libraryId = library.id, seriesId = series.id))
    bookRepository.insert(book2.copy(libraryId = library.id, seriesId = series.id))
    bookRepository.insert(book3.copy(libraryId = library.id, seriesId = series2.id))
  }

  @AfterEach
  fun cleanup() {
    readingEventDao.deleteAll()
    seriesReadingStatDailyDao.deleteAll()
  }

  @AfterAll
  fun tearDown() {
    bookRepository.deleteAll()
    seriesRepository.deleteAll()
    userRepository.deleteAll()
    libraryRepository.deleteAll()
  }

  @Test
  fun `given a reading event when inserted then it can be retrieved via stats`() {
    val today = LocalDate.now()

    readingEventDao.insert(
      ReadingEvent(
        userId = user1.id,
        bookId = book1.id,
        seriesId = series.id,
        type = ReadingEventType.PROGRESSED,
      ),
    )

    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)

    val stats = seriesReadingStatDailyDao.findBySeriesIdAndDayBetween(series.id, today, today)
    assertThat(stats).hasSize(1)
    with(stats.first()) {
      assertThat(seriesId).isEqualTo(series.id)
      assertThat(day).isEqualTo(today)
      assertThat(progressEvents).isEqualTo(1)
      assertThat(completedBooks).isEqualTo(0)
      assertThat(uniqueReaders).isEqualTo(1)
      assertThat(lastReadAt).isNotNull
    }
  }

  @Test
  fun `given completed reading event when inserted then completedBooks is counted`() {
    val today = LocalDate.now()

    readingEventDao.insert(
      ReadingEvent(
        userId = user1.id,
        bookId = book1.id,
        seriesId = series.id,
        type = ReadingEventType.COMPLETED,
      ),
    )

    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)

    val stats = seriesReadingStatDailyDao.findBySeriesIdAndDayBetween(series.id, today, today)
    assertThat(stats).hasSize(1)
    with(stats.first()) {
      assertThat(completedBooks).isEqualTo(1)
      assertThat(progressEvents).isEqualTo(1)
    }
  }

  @Test
  fun `given two users reading same series on same day then uniqueReaders is 2`() {
    val today = LocalDate.now()

    readingEventDao.insert(
      ReadingEvent(userId = user1.id, bookId = book1.id, seriesId = series.id, type = ReadingEventType.PROGRESSED),
    )
    readingEventDao.insert(
      ReadingEvent(userId = user2.id, bookId = book2.id, seriesId = series.id, type = ReadingEventType.PROGRESSED),
    )

    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)

    val stats = seriesReadingStatDailyDao.findBySeriesIdAndDayBetween(series.id, today, today)
    assertThat(stats).hasSize(1)
    assertThat(stats.first().uniqueReaders).isEqualTo(2)
    assertThat(stats.first().progressEvents).isEqualTo(2)
  }

  @Test
  fun `given no events for day when recomputing then no stat is upserted`() {
    val yesterday = LocalDate.now().minusDays(1)

    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, yesterday)

    val stats = seriesReadingStatDailyDao.findBySeriesIdAndDayBetween(series.id, yesterday, yesterday)
    assertThat(stats).isEmpty()
  }

  @Test
  fun `given events on multiple days when querying period then only days in range are returned`() {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    readingEventDao.insert(
      ReadingEvent(
        userId = user1.id,
        bookId = book1.id,
        seriesId = series.id,
        type = ReadingEventType.PROGRESSED,
        createdAt = LocalDateTime.now(),
      ),
    )
    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)

    val stats = seriesReadingStatDailyDao.findBySeriesIdAndDayBetween(series.id, yesterday, today)
    assertThat(stats).hasSize(1)
    assertThat(stats.first().day).isEqualTo(today)
  }

  @Test
  fun `given multiple series events when querying top series then ordering is by progress events`() {
    val today = LocalDate.now()

    // 2 events for series
    readingEventDao.insert(ReadingEvent(userId = user1.id, bookId = book1.id, seriesId = series.id, type = ReadingEventType.PROGRESSED))
    readingEventDao.insert(ReadingEvent(userId = user2.id, bookId = book2.id, seriesId = series.id, type = ReadingEventType.PROGRESSED))
    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)

    val top = seriesReadingStatDailyDao.findTopSeriesByPeriod(today, today, 10)
    assertThat(top).isNotEmpty
    assertThat(top.first().first).isEqualTo(series.id)
    assertThat(top.first().second).isEqualTo(2)
  }

  @Test
  fun `given events when deleting by book then events are removed`() {
    readingEventDao.insert(
      ReadingEvent(userId = user1.id, bookId = book1.id, seriesId = series.id, type = ReadingEventType.PROGRESSED),
    )
    readingEventDao.deleteByBookId(book1.id)
    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, LocalDate.now())

    val stats = seriesReadingStatDailyDao.findBySeriesIdAndDayBetween(series.id, LocalDate.now(), LocalDate.now())
    assertThat(stats).isEmpty()
  }

  @Test
  fun `given period stats when aggregating then ranking uses weighted score`() {
    val today = LocalDate.now()

    // score = 1*100 + 0*50 + 4 = 104
    repeat(4) {
      readingEventDao.insert(
        ReadingEvent(
          userId = user1.id,
          bookId = book1.id,
          seriesId = series.id,
          type = ReadingEventType.PROGRESSED,
          createdAt = today.atTime(10, 0).plusMinutes(it.toLong()),
        ),
      )
    }

    // score = 2*100 + 0*50 + 2 = 202
    readingEventDao.insert(
      ReadingEvent(
        userId = user1.id,
        bookId = book3.id,
        seriesId = series2.id,
        type = ReadingEventType.PROGRESSED,
        createdAt = today.atTime(11, 0),
      ),
    )
    readingEventDao.insert(
      ReadingEvent(
        userId = user2.id,
        bookId = book3.id,
        seriesId = series2.id,
        type = ReadingEventType.PROGRESSED,
        createdAt = today.atTime(12, 0),
      ),
    )

    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)
    seriesReadingStatDailyDao.recomputeAndUpsert(series2.id, today)

    val top = seriesReadingStatsRollupRepository.aggregateSeriesStats(today, today, 10)

    assertThat(top).hasSize(2)
    assertThat(top.first().seriesId).isEqualTo(series2.id)
    assertThat(top.first().uniqueReaders).isEqualTo(2)
    assertThat(top.first().completedBooks).isEqualTo(0)
    assertThat(top.first().progressEvents).isEqualTo(2)
  }

  @Test
  fun `given multiple days when aggregating then sums and max unique readers are returned`() {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    readingEventDao.insert(
      ReadingEvent(
        userId = user1.id,
        bookId = book1.id,
        seriesId = series.id,
        type = ReadingEventType.COMPLETED,
        createdAt = today.atTime(10, 0),
      ),
    )
    readingEventDao.insert(
      ReadingEvent(
        userId = user1.id,
        bookId = book2.id,
        seriesId = series.id,
        type = ReadingEventType.PROGRESSED,
        createdAt = yesterday.atTime(9, 0),
      ),
    )
    readingEventDao.insert(
      ReadingEvent(
        userId = user2.id,
        bookId = book2.id,
        seriesId = series.id,
        type = ReadingEventType.PROGRESSED,
        createdAt = yesterday.atTime(10, 0),
      ),
    )

    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, today)
    seriesReadingStatDailyDao.recomputeAndUpsert(series.id, yesterday)

    val stats = seriesReadingStatsRollupRepository.aggregateSeriesStats(yesterday, today, 10)

    assertThat(stats).hasSize(1)
    with(stats.first()) {
      assertThat(seriesId).isEqualTo(series.id)
      assertThat(uniqueReaders).isEqualTo(2)
      assertThat(completedBooks).isEqualTo(1)
      assertThat(progressEvents).isEqualTo(3)
      assertThat(lastReadAt).isNotNull
    }
  }
}
