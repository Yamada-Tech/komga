package org.gotson.komga.infrastructure.jooq.main

import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.infrastructure.jooq.SplitDslDaoBase
import org.gotson.komga.infrastructure.jooq.TempTable.Companion.withTempTable
import org.gotson.komga.jooq.main.Tables
import org.gotson.komga.jooq.main.tables.records.SeriesMetadataRecord
import org.gotson.komga.language.toCurrentTimeZone
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class SeriesMetadataDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
  @param:Value("#{@komgaProperties.database.batchChunkSize}") private val batchSize: Int,
) : SplitDslDaoBase(dslRW, dslRO),
  SeriesMetadataRepository {
  private val d = Tables.SERIES_METADATA
  private val g = Tables.SERIES_METADATA_GENRE
  private val st = Tables.SERIES_METADATA_TAG

  override fun findById(seriesId: String): SeriesMetadata = dslRO.findOne(seriesId)!!.toDomain(dslRO.findGenres(seriesId), dslRO.findTags(seriesId))

  override fun findByIdOrNull(seriesId: String): SeriesMetadata? = dslRO.findOne(seriesId)?.toDomain(dslRO.findGenres(seriesId), dslRO.findTags(seriesId))

  private fun DSLContext.findOne(seriesId: String) =
    this
      .selectFrom(d)
      .where(d.SERIES_ID.eq(seriesId))
      .fetchOneInto(d)

  private fun DSLContext.findGenres(seriesId: String) =
    this
      .select(g.GENRE)
      .from(g)
      .where(g.SERIES_ID.eq(seriesId))
      .fetchSet(g.GENRE)

  private fun DSLContext.findTags(seriesId: String) =
    this
      .select(st.TAG)
      .from(st)
      .where(st.SERIES_ID.eq(seriesId))
      .fetchSet(st.TAG)

  @Transactional
  override fun insert(metadata: SeriesMetadata) {
    dslRW
      .insertInto(d)
      .set(d.SERIES_ID, metadata.seriesId)
      .set(d.STATUS, metadata.status.toString())
      .set(d.TITLE, metadata.title)
      .set(d.TITLE_SORT, metadata.titleSort)
      .set(d.SUMMARY, metadata.summary)
      .set(d.READING_DIRECTION, metadata.readingDirection?.toString())
      .set(d.STATUS_LOCK, metadata.statusLock)
      .set(d.TITLE_LOCK, metadata.titleLock)
      .set(d.TITLE_SORT_LOCK, metadata.titleSortLock)
      .set(d.SUMMARY_LOCK, metadata.summaryLock)
      .set(d.READING_DIRECTION_LOCK, metadata.readingDirectionLock)
      .set(d.GENRES_LOCK, metadata.genresLock)
      .set(d.TAGS_LOCK, metadata.tagsLock)
      .execute()

    dslRW.insertGenres(metadata)
    dslRW.insertTags(metadata)
  }

  @Transactional
  override fun update(metadata: SeriesMetadata) {
    dslRW
      .update(d)
      .set(d.STATUS, metadata.status.toString())
      .set(d.TITLE, metadata.title)
      .set(d.TITLE_SORT, metadata.titleSort)
      .set(d.SUMMARY, metadata.summary)
      .set(d.READING_DIRECTION, metadata.readingDirection?.toString())
      .set(d.STATUS_LOCK, metadata.statusLock)
      .set(d.TITLE_LOCK, metadata.titleLock)
      .set(d.TITLE_SORT_LOCK, metadata.titleSortLock)
      .set(d.SUMMARY_LOCK, metadata.summaryLock)
      .set(d.READING_DIRECTION_LOCK, metadata.readingDirectionLock)
      .set(d.GENRES_LOCK, metadata.genresLock)
      .set(d.TAGS_LOCK, metadata.tagsLock)
      .set(d.LAST_MODIFIED_DATE, LocalDateTime.now(ZoneId.of("Z")))
      .where(d.SERIES_ID.eq(metadata.seriesId))
      .execute()

    dslRW
      .deleteFrom(g)
      .where(g.SERIES_ID.eq(metadata.seriesId))
      .execute()

    dslRW
      .deleteFrom(st)
      .where(st.SERIES_ID.eq(metadata.seriesId))
      .execute()

    dslRW.insertGenres(metadata)
    dslRW.insertTags(metadata)
  }

  private fun DSLContext.insertGenres(metadata: SeriesMetadata) {
    if (metadata.genres.isNotEmpty()) {
      metadata.genres.chunked(batchSize).forEach { chunk ->
        this
          .batch(
            this
              .insertInto(g, g.SERIES_ID, g.GENRE)
              .values(null as String?, null),
          ).also { step ->
            chunk.forEach {
              step.bind(metadata.seriesId, it)
            }
          }.execute()
      }
    }
  }

  private fun DSLContext.insertTags(metadata: SeriesMetadata) {
    if (metadata.tags.isNotEmpty()) {
      metadata.tags.chunked(batchSize).forEach { chunk ->
        this
          .batch(
            this
              .insertInto(st, st.SERIES_ID, st.TAG)
              .values(null as String?, null),
          ).also { step ->
            chunk.forEach {
              step.bind(metadata.seriesId, it)
            }
          }.execute()
      }
    }
  }

  @Transactional
  override fun delete(seriesId: String) {
    dslRW.deleteFrom(g).where(g.SERIES_ID.eq(seriesId)).execute()
    dslRW.deleteFrom(st).where(st.SERIES_ID.eq(seriesId)).execute()
    dslRW.deleteFrom(d).where(d.SERIES_ID.eq(seriesId)).execute()
  }

  @Transactional
  override fun delete(seriesIds: Collection<String>) {
    dslRW.withTempTable(batchSize, seriesIds).use {
      dslRW.deleteFrom(g).where(g.SERIES_ID.`in`(it.selectTempStrings())).execute()
      dslRW.deleteFrom(st).where(st.SERIES_ID.`in`(it.selectTempStrings())).execute()
      dslRW.deleteFrom(d).where(d.SERIES_ID.`in`(it.selectTempStrings())).execute()
    }
  }

  override fun count(): Long = dslRO.fetchCount(d).toLong()

  private fun SeriesMetadataRecord.toDomain(
    genres: Set<String>,
    tags: Set<String>,
  ) = SeriesMetadata(
    status = SeriesMetadata.Status.valueOf(status),
    title = title,
    titleSort = titleSort,
    summary = summary,
    readingDirection =
      readingDirection?.let {
        SeriesMetadata.ReadingDirection.valueOf(readingDirection)
      },
    genres = genres,
    tags = tags,
    statusLock = statusLock,
    titleLock = titleLock,
    titleSortLock = titleSortLock,
    summaryLock = summaryLock,
    readingDirectionLock = readingDirectionLock,
    genresLock = genresLock,
    tagsLock = tagsLock,
    seriesId = seriesId,
    createdDate = createdDate.toCurrentTimeZone(),
    lastModifiedDate = lastModifiedDate.toCurrentTimeZone(),
  )
}
