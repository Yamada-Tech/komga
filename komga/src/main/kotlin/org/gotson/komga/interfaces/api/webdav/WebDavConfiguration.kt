package org.gotson.komga.interfaces.api.webdav

import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class WebDavConfiguration {
  @Bean
  fun webDavServletRegistration(
    libraryRepository: LibraryRepository,
    seriesRepository: SeriesRepository,
    bookRepository: BookRepository,
    seriesMetadataRepository: SeriesMetadataRepository,
    mediaRepository: MediaRepository,
    contentRestrictionChecker: ContentRestrictionChecker,
  ): ServletRegistrationBean<WebDavServlet> =
    ServletRegistrationBean(
      WebDavServlet(
        libraryRepository,
        seriesRepository,
        bookRepository,
        seriesMetadataRepository,
        mediaRepository,
        contentRestrictionChecker,
      ),
      "$PATH_PREFIX/*",
    )

  companion object {
    const val PATH_PREFIX = "/webdav"
  }
}
