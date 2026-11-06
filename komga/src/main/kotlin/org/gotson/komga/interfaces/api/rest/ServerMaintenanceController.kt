package org.gotson.komga.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import org.gotson.komga.infrastructure.maintenance.DatabaseMaintenanceService
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["api/v1/server"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ServerMaintenanceController(
  private val databaseMaintenanceService: DatabaseMaintenanceService,
) {
  @PostMapping("optimize-and-restart")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Optimize the databases (VACUUM) and restart the server", tags = [OpenApiConfiguration.TagNames.MANAGEMENT])
  fun optimizeAndRestart(): ResponseEntity<Void> {
    databaseMaintenanceService.optimizeAndRestart()
    return ResponseEntity.accepted().build()
  }

  @PostMapping("restart")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Restart the server", tags = [OpenApiConfiguration.TagNames.MANAGEMENT])
  fun restart(): ResponseEntity<Void> {
    databaseMaintenanceService.restart()
    return ResponseEntity.accepted().build()
  }
}
