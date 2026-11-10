package org.gotson.komga.infrastructure.metadata.oneshot

import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.MetadataPatchTarget
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.model.SeriesMetadataPatch
import org.gotson.komga.domain.persistence.BookMetadataRepository
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.infrastructure.metadata.SeriesMetadataProvider
import org.springframework.stereotype.Service

@Service
class OneShotSeriesProvider(
  private val bookRepository: BookRepository,
  private val bookMetadataRepository: BookMetadataRepository,
) : SeriesMetadataProvider {
  override fun getSeriesMetadata(series: Series): SeriesMetadataPatch? {
    if (!series.oneshot) return null
    val bookMetadata = bookMetadataRepository.findById(bookRepository.findAllIdsBySeriesId(series.id).first())
    return SeriesMetadataPatch(
      title = bookMetadata.title,
      titleSort = bookMetadata.title,
      status = SeriesMetadata.Status.ENDED,
      summary = bookMetadata.summary,
      readingDirection = null,
      genres = null,
      collections = emptySet(),
    )
  }

  override fun shouldLibraryHandlePatch(
    library: Library,
    target: MetadataPatchTarget,
  ): Boolean = target == MetadataPatchTarget.SERIES
}
