package org.gotson.komga.interfaces.api.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.infrastructure.configuration.KomgaProperties
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("api/v1/logo")
@Tag(name = OpenApiConfiguration.TagNames.APP_LOGO)
class LogoController(
  komgaProperties: KomgaProperties,
  private val contentDetector: ContentDetector,
) {
  private val logoDir = Path(komgaProperties.configDir ?: "${System.getProperty("user.home")}/.komga").resolve("logo")

  @GetMapping
  @Operation(summary = "Get the application logo", description = "Returns the custom application logo if one has been uploaded, otherwise returns 404.")
  @SecurityRequirements
  fun getLogo(): ResponseEntity<Resource> {
    val logoFile = findLogoFile() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val mediaType =
      try {
        MediaType.parseMediaType(contentDetector.detectMediaType(logoFile))
      } catch (e: Exception) {
        MediaType.APPLICATION_OCTET_STREAM
      }
    return ResponseEntity
      .ok()
      .contentType(mediaType)
      .body(FileSystemResource(logoFile))
  }

  @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Upload a custom application logo", description = "Upload a new logo image. Only image files are accepted. Required role: **ADMIN**")
  fun uploadLogo(
    @RequestParam("file") file: MultipartFile,
  ) {
    val mediaType = file.inputStream.buffered().use { contentDetector.detectMediaType(it) }
    if (!contentDetector.isImage(mediaType))
      throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)

    val extension =
      file.originalFilename?.substringAfterLast('.', "")?.ifBlank { null }
        ?: mediaType.substringAfterLast('/', "").ifBlank { "png" }

    logoDir.createDirectories()
    // Remove any existing logo files
    deleteExistingLogoFiles()

    val logoFile = logoDir.resolve("logo.$extension")
    Files.write(logoFile, file.bytes)
    logger.info { "Logo uploaded: $logoFile" }
  }

  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete the custom application logo", description = "Deletes the custom application logo. Required role: **ADMIN**")
  fun deleteLogo() {
    deleteExistingLogoFiles()
    logger.info { "Logo deleted" }
  }

  private fun findLogoFile() =
    if (logoDir.exists()) {
      logoDir.listDirectoryEntries("logo.*").firstOrNull { it.isRegularFile() }
    } else {
      null
    }

  private fun deleteExistingLogoFiles() {
    if (logoDir.exists()) {
      logoDir.listDirectoryEntries("logo.*").forEach { it.deleteIfExists() }
    }
  }
}
