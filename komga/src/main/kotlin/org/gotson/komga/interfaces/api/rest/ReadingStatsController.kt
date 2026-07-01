package org.gotson.komga.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.persistence.SeriesReadingStatDailyRepository
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.gotson.komga.interfaces.api.rest.dto.SeriesReadingStatDailyDto
import org.gotson.komga.interfaces.api.rest.dto.TopSeriesReadingStatDto
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.READING_STATS)
@PreAuthorize("hasRole('ADMIN')")
class ReadingStatsController(
  private val seriesReadingStatDailyRepository: SeriesReadingStatDailyRepository,
) {
  @GetMapping("api/v1/reading-stats/series/{seriesId}")
  @Operation(
    summary = "Get daily reading stats for a series",
    description = "Returns daily aggregate reading statistics for the specified series within a date range.",
    tags = [OpenApiConfiguration.TagNames.READING_STATS],
  )
  fun getSeriesDailyStats(
    @PathVariable seriesId: String,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
  ): List<SeriesReadingStatDailyDto> =
    seriesReadingStatDailyRepository
      .findBySeriesIdAndDayBetween(seriesId, from, to)
      .map {
        SeriesReadingStatDailyDto(
          seriesId = it.seriesId,
          day = it.day,
          uniqueReaders = it.uniqueReaders,
          completedBooks = it.completedBooks,
          progressEvents = it.progressEvents,
          lastReadAt = it.lastReadAt,
        )
      }

  @GetMapping("api/v1/reading-stats/series/top")
  @Operation(
    summary = "Get top series by reading activity",
    description = "Returns series ranked by total progress events within a date range. Suitable for weekly, monthly, or yearly popularity displays.",
    tags = [OpenApiConfiguration.TagNames.READING_STATS],
  )
  fun getTopSeries(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    @RequestParam(defaultValue = "20") limit: Int,
  ): List<TopSeriesReadingStatDto> =
    seriesReadingStatDailyRepository
      .findTopSeriesByPeriod(from, to, limit.coerceIn(1, 100))
      .map { (seriesId, totalProgressEvents) ->
        TopSeriesReadingStatDto(seriesId = seriesId, totalProgressEvents = totalProgressEvents)
      }
}
