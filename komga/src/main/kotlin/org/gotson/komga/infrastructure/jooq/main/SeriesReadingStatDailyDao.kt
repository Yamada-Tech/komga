package org.gotson.komga.infrastructure.jooq.main

import org.gotson.komga.domain.model.SeriesReadingStatDaily
import org.gotson.komga.domain.persistence.SeriesReadingStatDailyRepository
import org.gotson.komga.infrastructure.jooq.SplitDslDaoBase
import org.gotson.komga.jooq.main.Tables
import org.gotson.komga.language.toCurrentTimeZone
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Component
class SeriesReadingStatDailyDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  SeriesReadingStatDailyRepository {
  private val s = Tables.SERIES_READING_STATS_DAILY
  private val re = Tables.READING_EVENT

  /**
   * Recompute stats for the given (seriesId, day) from the READING_EVENT table and upsert.
   * [day] is expected to be in the system default timezone.
   */
  override fun recomputeAndUpsert(
    seriesId: String,
    day: LocalDate,
  ) {
    // Convert local-time day boundaries to UTC for comparison against stored UTC timestamps
    val dayStart = day.atStartOfDay(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()
    val dayEnd =
      day
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime()

    val dayCondition =
      re.SERIES_ID
        .eq(seriesId)
        .and(re.CREATED_AT.ge(dayStart))
        .and(re.CREATED_AT.lt(dayEnd))

    val uniqueReaders =
      dslRW
        .selectCount()
        .from(dslRW.selectDistinct(re.USER_ID).from(re).where(dayCondition))
        .fetchOne(0, Int::class.java) ?: 0

    val completedBooks =
      dslRW
        .selectCount()
        .from(re)
        .where(dayCondition.and(re.TYPE.eq("COMPLETED")))
        .fetchOne(0, Int::class.java) ?: 0

    val progressEvents =
      dslRW
        .selectCount()
        .from(re)
        .where(dayCondition)
        .fetchOne(0, Int::class.java) ?: 0

    val lastReadAt =
      dslRW
        .select(DSL.max(re.CREATED_AT))
        .from(re)
        .where(dayCondition)
        .fetchOne(0, LocalDateTime::class.java)

    if (progressEvents == 0) return

    dslRW
      .insertInto(s, s.SERIES_ID, s.DAY, s.UNIQUE_READERS, s.COMPLETED_BOOKS, s.PROGRESS_EVENTS, s.LAST_READ_AT)
      .values(seriesId, day, uniqueReaders, completedBooks, progressEvents, lastReadAt)
      .onDuplicateKeyUpdate()
      .set(s.UNIQUE_READERS, uniqueReaders)
      .set(s.COMPLETED_BOOKS, completedBooks)
      .set(s.PROGRESS_EVENTS, progressEvents)
      .set(s.LAST_READ_AT, lastReadAt)
      .execute()
  }

  override fun findBySeriesIdAndDayBetween(
    seriesId: String,
    from: LocalDate,
    to: LocalDate,
  ): List<SeriesReadingStatDaily> =
    dslRO
      .selectFrom(s)
      .where(s.SERIES_ID.eq(seriesId))
      .and(s.DAY.between(from, to))
      .fetchInto(s)
      .map {
        SeriesReadingStatDaily(
          seriesId = it.seriesId,
          day = it.day,
          uniqueReaders = it.uniqueReaders,
          completedBooks = it.completedBooks,
          progressEvents = it.progressEvents,
          lastReadAt = it.lastReadAt?.toCurrentTimeZone(),
        )
      }

  override fun findTopSeriesByPeriod(
    from: LocalDate,
    to: LocalDate,
    limit: Int,
  ): List<Pair<String, Int>> =
    dslRO
      .select(s.SERIES_ID, DSL.sum(s.PROGRESS_EVENTS).cast(Int::class.java))
      .from(s)
      .where(s.DAY.between(from, to))
      .groupBy(s.SERIES_ID)
      .orderBy(DSL.sum(s.PROGRESS_EVENTS).desc())
      .limit(limit)
      .fetch()
      .mapNotNull { r -> r.value1()?.let { id -> id to (r.value2() ?: 0) } }

  override fun deleteBySeriesId(seriesId: String) {
    dslRW.deleteFrom(s).where(s.SERIES_ID.eq(seriesId)).execute()
  }

  override fun deleteAll() {
    dslRW.deleteFrom(s).execute()
  }
}
