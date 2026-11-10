package org.gotson.komga.domain.model

import org.gotson.komga.language.lowerNotBlank
import java.time.LocalDateTime

class SeriesMetadata(
  val status: Status = Status.ONGOING,
  title: String,
  titleSort: String = title,
  summary: String = "",
  val readingDirection: ReadingDirection? = null,
  genres: Set<String> = emptySet(),
  tags: Set<String> = emptySet(),
  val statusLock: Boolean = false,
  val titleLock: Boolean = false,
  val titleSortLock: Boolean = false,
  val summaryLock: Boolean = false,
  val readingDirectionLock: Boolean = false,
  val genresLock: Boolean = false,
  val tagsLock: Boolean = false,
  val seriesId: String = "",
  override val createdDate: LocalDateTime = LocalDateTime.now(),
  override val lastModifiedDate: LocalDateTime = createdDate,
) : Auditable {
  val title = title.trim()
  val titleSort = titleSort.trim()
  val summary = summary.trim()
  val tags = tags.lowerNotBlank().toSet()
  val genres = genres.lowerNotBlank().toSet()

  fun copy(
    status: Status = this.status,
    title: String = this.title,
    titleSort: String = this.titleSort,
    summary: String = this.summary,
    readingDirection: ReadingDirection? = this.readingDirection,
    genres: Set<String> = this.genres,
    tags: Set<String> = this.tags,
    statusLock: Boolean = this.statusLock,
    titleLock: Boolean = this.titleLock,
    titleSortLock: Boolean = this.titleSortLock,
    summaryLock: Boolean = this.summaryLock,
    readingDirectionLock: Boolean = this.readingDirectionLock,
    genresLock: Boolean = this.genresLock,
    tagsLock: Boolean = this.tagsLock,
    seriesId: String = this.seriesId,
    createdDate: LocalDateTime = this.createdDate,
    lastModifiedDate: LocalDateTime = this.lastModifiedDate,
  ) = SeriesMetadata(
    status = status,
    title = title,
    titleSort = titleSort,
    summary = summary,
    readingDirection = readingDirection,
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
    createdDate = createdDate,
    lastModifiedDate = lastModifiedDate,
  )

  enum class Status {
    ENDED,
    ONGOING,
    ABANDONED,
    HIATUS,
  }

  enum class ReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    VERTICAL,
    WEBTOON,
  }

  override fun toString(): String = "SeriesMetadata(status=$status, readingDirection=$readingDirection, statusLock=$statusLock, titleLock=$titleLock, titleSortLock=$titleSortLock, summaryLock=$summaryLock, readingDirectionLock=$readingDirectionLock, genresLock=$genresLock, tagsLock=$tagsLock, seriesId='$seriesId', createdDate=$createdDate, lastModifiedDate=$lastModifiedDate, title='$title', titleSort='$titleSort', summary='$summary', tags=$tags, genres=$genres)"
}
