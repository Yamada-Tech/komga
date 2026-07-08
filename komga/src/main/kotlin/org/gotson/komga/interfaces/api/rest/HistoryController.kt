package org.gotson.komga.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.persistence.AuthenticationActivityRepository
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/history", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.HISTORY)
class HistoryController(
  private val authenticationActivityRepository: AuthenticationActivityRepository,
) {
  @PostMapping("authentication-activity/clear")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
    summary = "Clear authentication activity",
    description = "Deletes all authentication activity entries. Required role: **ADMIN**",
  )
  fun clearAuthenticationActivity() = authenticationActivityRepository.deleteAll()
}
