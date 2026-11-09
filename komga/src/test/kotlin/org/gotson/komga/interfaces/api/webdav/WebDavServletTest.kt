package org.gotson.komga.interfaces.api.webdav

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.AgeRestriction
import org.gotson.komga.domain.model.AllowExclude
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.ContentRestrictions
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.SeriesMetadata
import org.gotson.komga.domain.model.UserRoles
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesMetadataRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

class WebDavServletTest {
  private val libraryRepository = mockk<LibraryRepository>(relaxed = true)
  private val seriesRepository = mockk<SeriesRepository>(relaxed = true)
  private val bookRepository = mockk<BookRepository>(relaxed = true)
  private val seriesMetadataRepository = mockk<SeriesMetadataRepository>(relaxed = true)
  private val mediaRepository = mockk<MediaRepository>(relaxed = true)
  private val contentRestrictionChecker = mockk<ContentRestrictionChecker>(relaxed = true)

  private lateinit var servlet: WebDavServlet

  private val user = KomgaUser(email = "user@example.com", password = "")

  @BeforeEach
  fun setUp() {
    servlet = WebDavServlet(libraryRepository, seriesRepository, bookRepository, seriesMetadataRepository, mediaRepository, contentRestrictionChecker)
    authenticate(user)
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(
    komgaUser: KomgaUser,
  ) {
    val authentication = UsernamePasswordAuthenticationToken(KomgaPrincipal(komgaUser), null, KomgaPrincipal(komgaUser).authorities)
    SecurityContextHolder.getContext().authentication = authentication
  }

  private fun request(
    method: String,
    pathInfo: String?,
  ): MockHttpServletRequest =
    MockHttpServletRequest().apply {
      this.method = method
      servletPath = WebDavConfiguration.PATH_PREFIX
      this.pathInfo = pathInfo
      requestURI = WebDavConfiguration.PATH_PREFIX + (pathInfo ?: "")
      contextPath = ""
    }

  @Test
  fun `given any authenticated user when options then returns DAV capabilities`() {
    val response = MockHttpServletResponse()

    servlet.handle(request("OPTIONS", null), response)

    assertThat(response.status).isEqualTo(200)
    assertThat(response.getHeader("DAV")).isEqualTo("1")
    assertThat(response.getHeader("Allow")).isEqualTo("OPTIONS, GET, HEAD, PROPFIND")
  }

  @Test
  fun `given unauthenticated context when propfind then returns 403`() {
    SecurityContextHolder.clearContext()

    val response = MockHttpServletResponse()
    servlet.handle(request("PROPFIND", null), response)

    assertThat(response.status).isEqualTo(403)
  }

  @Test
  fun `given libraries when propfind root then only shared libraries are listed`() {
    every { libraryRepository.findAll() } returns
      listOf(
        Library(name = "Public", root = URL("file:/public"), id = "lib-public"),
        Library(name = "Private", root = URL("file:/private"), id = "lib-private"),
      )
    val limitedUser =
      KomgaUser(
        email = "limited@example.com",
        password = "",
        sharedAllLibraries = false,
        sharedLibrariesIds = setOf("lib-public"),
      )
    authenticate(limitedUser)

    val response = MockHttpServletResponse()
    servlet.handle(request("PROPFIND", null), response)

    assertThat(response.status).isEqualTo(207)
    assertThat(response.contentType).startsWith("application/xml")
    assertThat(response.contentAsString)
      .contains("<d:href>/webdav/</d:href>")
      .contains("<d:displayname>Public</d:displayname>")
      .doesNotContain("Private")
  }

  @Test
  fun `given depth 0 when propfind library then only the library itself is returned`() {
    val library = Library(name = "Library", root = URL("file:/library"), id = "lib1")
    every { libraryRepository.findAll() } returns listOf(library)
    val request = request("PROPFIND", "/lib1").apply { addHeader("Depth", "0") }

    val response = MockHttpServletResponse()
    servlet.handle(request, response)

    assertThat(response.status).isEqualTo(207)
    assertThat(response.contentAsString).contains("<d:href>/webdav/lib1/</d:href>")
    assertThat(response.contentAsString.indexOf("<d:response>")).isEqualTo(response.contentAsString.lastIndexOf("<d:response>"))
  }

  @Test
  fun `given series when propfind library then series directories and one-shot files are listed`() {
    val library = Library(name = "Library", root = URL("file:/library"), id = "lib1")
    every { libraryRepository.findAll() } returns listOf(library)
    val normalSeries = Series(name = "Series A", url = URL("file:/library/series-a"), fileLastModified = LocalDateTime.now(), id = "s1", libraryId = "lib1")
    val oneshotSeries = Series(name = "One-shot", url = URL("file:/library/oneshot"), fileLastModified = LocalDateTime.now(), id = "s2", libraryId = "lib1", oneshot = true)
    every { seriesRepository.findAllByLibraryId("lib1") } returns listOf(normalSeries, oneshotSeries)
    every { bookRepository.findAllBySeriesId("s2") } returns listOf(book("b2", "s2"))

    val response = MockHttpServletResponse()
    servlet.handle(request("PROPFIND", "/lib1"), response)

    assertThat(response.status).isEqualTo(207)
    assertThat(response.contentAsString)
      .contains("<d:displayname>Series A</d:displayname>")
      .contains("<d:collection/>")
      .contains("<d:displayname>oneshot.cbz</d:displayname>")
  }

  @Test
  fun `given one-shot file when propfind then it is served as file`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/oneshot.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, ByteArray(3))
      val library = Library(name = "Library", root = URL("file:/lib"), id = "lib1")
      every { libraryRepository.findAll() } returns listOf(library)
      val oneshotSeries = Series(name = "One-shot", url = URL("file:/lib/oneshot"), fileLastModified = LocalDateTime.now(), id = "s2", libraryId = "lib1", oneshot = true)
      every { seriesRepository.findAllByLibraryId("lib1") } returns listOf(oneshotSeries)
      every { bookRepository.findAllBySeriesId("s2") } returns listOf(book("b2", "s2", path.toUri().toURL()))

      val response = MockHttpServletResponse()
      servlet.handle(request("GET", "/lib1/oneshot.cbz"), response)

      assertThat(response.status).isEqualTo(200)
      assertThat(response.contentAsByteArray).hasSize(3)
    }
  }

  @Test
  fun `given series when propfind then books are listed with content type and length`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/series-a/vol1.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, ByteArray(42))
      val series = Series(name = "Series A", url = URL("file:/lib/series-a"), fileLastModified = LocalDateTime.now(), id = "s1", libraryId = "lib1")
      every { seriesRepository.findAllByLibraryId("lib1") } returns listOf(series)
      every { bookRepository.findAllBySeriesId("s1") } returns listOf(book("b1", "s1", path.toUri().toURL()))
      every { mediaRepository.findByIdOrNull("b1") } returns Media(mediaType = "application/zip", bookId = "b1")

      val response = MockHttpServletResponse()
      servlet.handle(request("PROPFIND", "/lib1/Series A"), response)

      assertThat(response.status).isEqualTo(207)
      assertThat(response.contentAsString)
        .contains("<d:displayname>vol1.cbz</d:displayname>")
        .contains("<d:getcontentlength>42</d:getcontentlength>")
        .contains("<d:getcontenttype>application/zip</d:getcontenttype>")
    }
  }

  @Test
  fun `given book file when get then whole file is returned`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/series-a/vol1.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, "abcdefghij".toByteArray())
      stubSeriesWithBook(path)

      val response = MockHttpServletResponse()
      servlet.handle(request("GET", "/lib1/Series A/vol1.cbz"), response)

      assertThat(response.status).isEqualTo(200)
      assertThat(response.getHeader("Accept-Ranges")).isEqualTo("bytes")
      assertThat(response.getHeader("ETag")).isEqualTo("\"b1-10\"")
      assertThat(String(response.contentAsByteArray)).isEqualTo("abcdefghij")
    }
  }

  @Test
  fun `given range header when get then partial content is returned`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/series-a/vol1.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, "abcdefghij".toByteArray())
      stubSeriesWithBook(path)
      val request = request("GET", "/lib1/Series A/vol1.cbz").apply { addHeader("Range", "bytes=2-4") }

      val response = MockHttpServletResponse()
      servlet.handle(request, response)

      assertThat(response.status).isEqualTo(206)
      assertThat(response.getHeader("Content-Range")).isEqualTo("bytes 2-4/10")
      assertThat(String(response.contentAsByteArray)).isEqualTo("cde")
    }
  }

  @Test
  fun `given unsatisfiable range when get then 416 is returned`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/series-a/vol1.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, "abcdefghij".toByteArray())
      stubSeriesWithBook(path)
      val request = request("GET", "/lib1/Series A/vol1.cbz").apply { addHeader("Range", "bytes=20-30") }

      val response = MockHttpServletResponse()
      servlet.handle(request, response)

      assertThat(response.status).isEqualTo(416)
      assertThat(response.getHeader("Content-Range")).isEqualTo("bytes */10")
    }
  }

  @Test
  fun `given book without FILE_DOWNLOAD role when get then 403`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/series-a/vol1.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, "abcdefghij".toByteArray())
      stubSeriesWithBook(path)
      authenticate(KomgaUser(email = "limited@example.com", password = "", roles = setOf(UserRoles.PAGE_STREAMING)))

      val response = MockHttpServletResponse()
      servlet.handle(request("GET", "/lib1/Series A/vol1.cbz"), response)

      assertThat(response.status).isEqualTo(403)
    }
  }

  @Test
  fun `given head request when book then headers only`() {
    Jimfs.newFileSystem(Configuration.unix()).use { fs ->
      val path = fs.getPath("/lib/series-a/vol1.cbz")
      Files.createDirectories(path.parent)
      Files.createFile(path)
      Files.write(path, "abcdefghij".toByteArray())
      stubSeriesWithBook(path)

      val response = MockHttpServletResponse()
      servlet.handle(request("HEAD", "/lib1/Series A/vol1.cbz"), response)

      assertThat(response.status).isEqualTo(200)
      assertThat(response.contentLength).isEqualTo(10)
      assertThat(response.contentAsByteArray).isEmpty()
    }
  }

  @Test
  fun `given missing path when propfind then 404`() {
    every { libraryRepository.findAll() } returns emptyList()

    val response = MockHttpServletResponse()
    servlet.handle(request("PROPFIND", "/nope"), response)

    assertThat(response.status).isEqualTo(404)
  }

  @Test
  fun `given write method when service then 405`() {
    val response = MockHttpServletResponse()

    servlet.handle(request("PUT", "/lib1/Series A/vol1.cbz"), response)

    assertThat(response.status).isEqualTo(405)
    assertThat(response.getHeader("Allow")).isEqualTo(WebDavServlet.ALLOWED_METHODS)
  }

  @Test
  fun `given restricted series when propfind library then restricted series is hidden`() {
    val library = Library(name = "Library", root = URL("file:/library"), id = "lib1")
    every { libraryRepository.findAll() } returns listOf(library)
    val allowed = Series(name = "Allowed", url = URL("file:/library/allowed"), fileLastModified = LocalDateTime.now(), id = "s1", libraryId = "lib1")
    val blocked = Series(name = "Blocked", url = URL("file:/library/blocked"), fileLastModified = LocalDateTime.now(), id = "s2", libraryId = "lib1")
    every { seriesRepository.findAllByLibraryId("lib1") } returns listOf(allowed, blocked)
    every { seriesMetadataRepository.findByIdOrNull("s1") } returns SeriesMetadata(title = "Allowed", seriesId = "s1", ageRating = 6)
    every { seriesMetadataRepository.findByIdOrNull("s2") } returns SeriesMetadata(title = "Blocked", seriesId = "s2", ageRating = 18)
    val restrictedUser =
      KomgaUser(
        email = "restricted@example.com",
        password = "",
        restrictions = ContentRestrictions(ageRestriction = AgeRestriction(age = 9, restriction = AllowExclude.ALLOW_ONLY)),
      )
    authenticate(restrictedUser)

    val response = MockHttpServletResponse()
    servlet.handle(request("PROPFIND", "/lib1"), response)

    assertThat(response.status).isEqualTo(207)
    assertThat(response.contentAsString)
      .contains("<d:displayname>Allowed</d:displayname>")
      .doesNotContain("Blocked")
  }

  private fun stubSeriesWithBook(path: Path) {
    val library = Library(name = "Library", root = URL("file:/lib"), id = "lib1")
    every { libraryRepository.findAll() } returns listOf(library)
    val series = Series(name = "Series A", url = URL("file:/lib/series-a"), fileLastModified = LocalDateTime.now(), id = "s1", libraryId = "lib1")
    every { seriesRepository.findAllByLibraryId("lib1") } returns listOf(series)
    every { bookRepository.findAllBySeriesId("s1") } returns listOf(book("b1", "s1", path.toUri().toURL()))
  }

  private fun book(
    id: String,
    seriesId: String,
    url: URL = URL("file:/lib/series-a/oneshot.cbz"),
  ): Book =
    Book(
      name = url.path.substringAfterLast('/'),
      url = url,
      fileLastModified = LocalDateTime.now(),
      id = id,
      seriesId = seriesId,
      libraryId = "lib1",
    )
}
