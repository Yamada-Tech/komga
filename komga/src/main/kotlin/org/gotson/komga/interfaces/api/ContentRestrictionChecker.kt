package org.gotson.komga.interfaces.api

import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.persistence.ThumbnailBookRepository
import org.gotson.komga.domain.persistence.ThumbnailSeriesRepository
import org.gotson.komga.interfaces.api.rest.dto.BookDto
import org.gotson.komga.interfaces.api.rest.dto.SeriesDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

/**
 * Fork note: content restrictions (age rating, sharing labels) are effectively disabled
 * because the corresponding series metadata fields have been removed. Only library
 * access checks are enforced.
 */
@Component
class ContentRestrictionChecker(
  private val bookRepository: BookRepository,
  private val thumbnailBookRepository: ThumbnailBookRepository,
  private val seriesRepository: SeriesRepository,
  private val thumbnailSeriesRepository: ThumbnailSeriesRepository,
) {
  /**
   * Convenience function to check for content restriction.
   * This will retrieve data from repositories if needed.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionBook(
    komgaUser: KomgaUser,
    book: BookDto,
  ) {
    if (!komgaUser.canAccessLibrary(book.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  }

  /**
   * Convenience function to check for content restriction.
   * This will retrieve data from repositories if needed.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionBook(
    komgaUser: KomgaUser,
    book: Book,
  ) {
    if (!komgaUser.canAccessLibrary(book.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  }

  /**
   * Convenience function to check for content restriction.
   * This will retrieve data from repositories if needed.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionBook(
    komgaUser: KomgaUser,
    bookId: String,
  ) {
    if (!komgaUser.canAccessAllLibraries()) {
      bookRepository.getLibraryIdOrNull(bookId)?.let {
        if (!komgaUser.canAccessLibrary(it)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * Convenience function to check for content restriction.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionBookThumbnail(
    komgaUser: KomgaUser,
    thumbnailId: String,
  ) {
    if (!komgaUser.canAccessAllLibraries()) {
      thumbnailBookRepository.getLibraryIdOrNull(thumbnailId)?.let {
        if (!komgaUser.canAccessLibrary(it)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * Convenience function to check for content restriction.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionSeries(
    komgaUser: KomgaUser,
    series: SeriesDto,
  ) {
    if (!komgaUser.canAccessLibrary(series.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  }

  /**
   * Convenience function to check for content restriction.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionSeries(
    komgaUser: KomgaUser,
    seriesId: String,
  ) {
    if (!komgaUser.canAccessAllLibraries()) {
      seriesRepository.getLibraryId(seriesId)?.let {
        if (!komgaUser.canAccessLibrary(it)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }

  /**
   * Convenience function to check for content restriction.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestrictionSeriesThumbnail(
    komgaUser: KomgaUser,
    thumbnailId: String,
  ) {
    if (!komgaUser.canAccessAllLibraries()) {
      thumbnailSeriesRepository.getLibraryIdOrNull(thumbnailId)?.let {
        if (!komgaUser.canAccessLibrary(it)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }
}
