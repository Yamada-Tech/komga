package org.gotson.komga.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.persistence.ReferentialRepository
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.rest.dto.AuthorDto
import org.gotson.komga.interfaces.api.rest.dto.toDto
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.REFERENTIAL)
class ReferentialV1Controller(
  private val referentialRepository: ReferentialRepository,
) {
  @GetMapping("authors")
  @Deprecated("Use GET /v2/authors instead")
  @Operation(summary = "List authors", description = "Use GET /api/v2/authors instead. Deprecated since 1.20.0.", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getAuthorsDeprecated(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "search", defaultValue = "") search: String,
    @RequestParam(name = "library_id", required = false) libraryId: String?,
    @RequestParam(name = "collection_id", required = false) collectionId: String?,
    @RequestParam(name = "series_id", required = false) seriesId: String?,
  ): List<AuthorDto> =

    when {
      libraryId != null -> referentialRepository.findAllAuthorsByNameAndLibrary(search, libraryId, principal.user.getAuthorizedLibraryIds(null))
      collectionId != null -> referentialRepository.findAllAuthorsByNameAndCollection(search, collectionId, principal.user.getAuthorizedLibraryIds(null))
      seriesId != null -> referentialRepository.findAllAuthorsByNameAndSeries(search, seriesId, principal.user.getAuthorizedLibraryIds(null))
      else -> referentialRepository.findAllAuthorsByName(search, principal.user.getAuthorizedLibraryIds(null))
    }.map { it.toDto() }

  @GetMapping("authors/names")
  @Deprecated("Use GET /v2/authors/names instead")
  @Operation(summary = "List authors' names", description = "Use GET /v2/authors/names instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getAuthorsNames(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "search", defaultValue = "") search: String,
  ): List<String> = referentialRepository.findAllAuthorsNamesByName(search, principal.user.getAuthorizedLibraryIds(null))

  @GetMapping("authors/roles")
  @Deprecated("Use GET /v2/authors/roles instead")
  @Operation(summary = "List authors' roles", description = "Use GET /v2/authors/roles instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getAuthorsRoles(
    @AuthenticationPrincipal principal: KomgaPrincipal,
  ): List<String> = referentialRepository.findAllAuthorsRoles(principal.user.getAuthorizedLibraryIds(null))

  @GetMapping("genres")
  @Deprecated("Use GET /v2/genres instead")
  @Operation(summary = "List genres", description = "Use GET /v2/genres instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getGenres(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "library_id", required = false) libraryIds: Set<String> = emptySet(),
    @RequestParam(name = "collection_id", required = false) collectionId: String?,
  ): Set<String> =
    when {
      libraryIds.isNotEmpty() -> referentialRepository.findAllGenresByLibraries(libraryIds, principal.user.getAuthorizedLibraryIds(null))
      collectionId != null -> referentialRepository.findAllGenresByCollection(collectionId, principal.user.getAuthorizedLibraryIds(null))
      else -> referentialRepository.findAllGenres(principal.user.getAuthorizedLibraryIds(null))
    }

  @GetMapping("tags")
  @Deprecated("Use GET /v2/tags instead")
  @Operation(summary = "List tags", description = "Use GET /v2/tags instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getTags(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "library_id", required = false) libraryIds: Set<String> = emptySet(),
    @RequestParam(name = "collection_id", required = false) collectionId: String?,
  ): Set<String> =
    when {
      libraryIds.isNotEmpty() -> referentialRepository.findAllSeriesAndBookTagsByLibraries(libraryIds, principal.user.getAuthorizedLibraryIds(null))
      collectionId != null -> referentialRepository.findAllSeriesAndBookTagsByCollection(collectionId, principal.user.getAuthorizedLibraryIds(null))
      else -> referentialRepository.findAllSeriesAndBookTags(principal.user.getAuthorizedLibraryIds(null))
    }

  @GetMapping("tags/book")
  @Deprecated("Use GET /v2/tags instead")
  @Operation(summary = "List book tags", description = "Use GET /v2/tags instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getBookTags(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "series_id", required = false) seriesId: String?,
    @RequestParam(name = "readlist_id", required = false) readListId: String?,
    @RequestParam(name = "library_id", required = false) libraryIds: Set<String> = emptySet(),
  ): Set<String> =
    when {
      seriesId != null -> referentialRepository.findAllBookTagsBySeries(seriesId, principal.user.getAuthorizedLibraryIds(null))
      readListId != null -> referentialRepository.findAllBookTagsByReadList(readListId, principal.user.getAuthorizedLibraryIds(null))
      libraryIds.isNotEmpty() -> referentialRepository.findAllBookTags(principal.user.getAuthorizedLibraryIds(libraryIds))
      else -> referentialRepository.findAllBookTags(principal.user.getAuthorizedLibraryIds(null))
    }

  @GetMapping("tags/series")
  @Deprecated("Use GET /v2/tags instead")
  @Operation(summary = "List series tags", description = "Use GET /v2/tags instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getSeriesTags(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "library_id", required = false) libraryId: String?,
    @RequestParam(name = "collection_id", required = false) collectionId: String?,
  ): Set<String> =
    when {
      libraryId != null -> referentialRepository.findAllSeriesTagsByLibrary(libraryId, principal.user.getAuthorizedLibraryIds(null))
      collectionId != null -> referentialRepository.findAllSeriesTagsByCollection(collectionId, principal.user.getAuthorizedLibraryIds(null))
      else -> referentialRepository.findAllSeriesTags(principal.user.getAuthorizedLibraryIds(null))
    }

  @GetMapping("series/release-dates")
  @Deprecated("Use GET /v2/series/release-years instead")
  @Operation(summary = "List series release dates", description = "Use GET /v2/series/release-years instead. Deprecated since 1.26.0", tags = [OpenApiConfiguration.TagNames.DEPRECATED])
  fun getSeriesReleaseDates(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam(name = "library_id", required = false) libraryIds: Set<String> = emptySet(),
    @RequestParam(name = "collection_id", required = false) collectionId: String?,
  ): Set<String> =
    when {
      libraryIds.isNotEmpty() -> referentialRepository.findAllSeriesReleaseDatesByLibraries(libraryIds, principal.user.getAuthorizedLibraryIds(null))
      collectionId != null -> referentialRepository.findAllSeriesReleaseDatesByCollection(collectionId, principal.user.getAuthorizedLibraryIds(null))
      else -> referentialRepository.findAllSeriesReleaseDates(principal.user.getAuthorizedLibraryIds(null))
    }.map { it.year.toString() }.toSet()
}
