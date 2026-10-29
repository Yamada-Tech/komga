package org.gotson.komga.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.gotson.komga.infrastructure.configuration.KomgaProperties
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.gotson.komga.infrastructure.configuration.LogoImageService
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.gotson.komga.infrastructure.kobo.KepubConverter
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.infrastructure.web.WebServerEffectiveSettings
import org.gotson.komga.interfaces.api.rest.dto.SettingMultiSource
import org.gotson.komga.interfaces.api.rest.dto.SettingsDto
import org.gotson.komga.interfaces.api.rest.dto.SettingsUpdateDto
import org.gotson.komga.interfaces.api.rest.dto.public
import org.gotson.komga.interfaces.api.rest.dto.toDomain
import org.gotson.komga.interfaces.api.rest.dto.toDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import kotlin.time.Duration.Companion.days

@RestController
@RequestMapping(value = ["api/v1/settings"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.SERVER_SETTINGS)
class SettingsController(
  private val komgaSettingsProvider: KomgaSettingsProvider,
  private val logoImageService: LogoImageService,
  @param:Value($$"${server.port:#{null}}") private val configServerPort: Int?,
  @param:Value($$"${server.servlet.context-path:#{null}}") private val configServerContextPath: String?,
  private val serverSettings: WebServerEffectiveSettings,
  private val kepubConverter: KepubConverter,
  private val multipartProperties: MultipartProperties,
  private val imageAnalyzer: ImageAnalyzer,
  private val komgaProperties: KomgaProperties,
) {
  @GetMapping
  @Operation(summary = "Retrieve server settings")
  fun getServerSettings(
    @AuthenticationPrincipal principal: KomgaPrincipal,
  ): SettingsDto {
    val settingsDto =
      SettingsDto(
        komgaSettingsProvider.deleteEmptyCollections,
        komgaSettingsProvider.deleteEmptyReadLists,
        komgaSettingsProvider.rememberMeDuration.inWholeDays,
        komgaSettingsProvider.thumbnailSize.toDto(),
        komgaSettingsProvider.taskPoolSize,
        SettingMultiSource(configServerPort, komgaSettingsProvider.serverPort, serverSettings.effectiveServerPort),
        SettingMultiSource(configServerContextPath, komgaSettingsProvider.serverContextPath, serverSettings.effectiveServletContextPath),
        komgaSettingsProvider.koboProxy,
        komgaSettingsProvider.koboPort,
        SettingMultiSource(kepubConverter.kepubifyConfigurationPath, komgaSettingsProvider.kepubifyPath, kepubConverter.kepubifyPath?.toString()),
        multipartProperties.maxFileSize?.toBytes(),
        komgaSettingsProvider.nextUiAsDefault,
      )

    return if (principal.user.isAdmin) settingsDto else settingsDto.public()
  }

  @PatchMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update server settings", description = "You can omit fields you don't want to update")
  @PreAuthorize("hasRole('ADMIN')")
  fun updateServerSettings(
    @Valid @RequestBody
    @Parameter(description = "Fields to update. You can omit fields you don't want to update.")
    newSettings: SettingsUpdateDto,
  ) {
    newSettings.deleteEmptyCollections?.let { komgaSettingsProvider.deleteEmptyCollections = it }
    newSettings.deleteEmptyReadLists?.let { komgaSettingsProvider.deleteEmptyReadLists = it }
    newSettings.rememberMeDurationDays?.let { komgaSettingsProvider.rememberMeDuration = it.days }
    if (newSettings.renewRememberMeKey == true) komgaSettingsProvider.renewRememberMeKey()
    newSettings.thumbnailSize?.let { komgaSettingsProvider.thumbnailSize = it.toDomain() }
    newSettings.taskPoolSize?.let { komgaSettingsProvider.taskPoolSize = it }

    if (newSettings.isSet("serverPort")) komgaSettingsProvider.serverPort = newSettings.serverPort
    if (newSettings.isSet("serverContextPath")) komgaSettingsProvider.serverContextPath = newSettings.serverContextPath

    newSettings.koboProxy?.let { komgaSettingsProvider.koboProxy = it }
    if (newSettings.isSet("koboPort")) komgaSettingsProvider.koboPort = newSettings.koboPort
    if (newSettings.isSet("kepubifyPath")) komgaSettingsProvider.kepubifyPath = newSettings.kepubifyPath
    newSettings.nextUiAsDefault?.let { komgaSettingsProvider.nextUiAsDefault = it }
    newSettings.siteName?.let { komgaSettingsProvider.siteName = it }
  }

  @PostMapping("logo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Upload logo image", description = "Upload a 512x512 PNG logo image")
  @PreAuthorize("hasRole('ADMIN')")
  fun uploadLogo(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestPart("file") file: MultipartFile,
  ) {
    // Validate file type
    val isPng = file.contentType == "image/png" || file.originalFilename?.endsWith(".png", ignoreCase = true) == true
    if (!isPng) throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PNG images are supported")

    // Validate image dimensions (512x512)
    val imageBytes = file.bytes
    if (!logoImageService.validateLogoImage(imageBytes)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo must be 512x512 pixels")

    // Save the logo image
    val fileName = "logo.png"
    logoImageService.saveLogoImage(imageBytes, fileName)

    // Update settings with logo filename
    komgaSettingsProvider.logoImage = fileName
  }

  @GetMapping("logo", produces = [MediaType.IMAGE_PNG_VALUE])
  @Operation(summary = "Get logo image", description = "Get the currently uploaded logo image")
  fun getLogoImage(): ResponseEntity<ByteArray> {
    val logoFileName = komgaSettingsProvider.logoImage ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val logoBytes = logoImageService.getLogoImage(logoFileName) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(logoBytes)
  }
}
