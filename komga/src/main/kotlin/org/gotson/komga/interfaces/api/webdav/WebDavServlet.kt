package org.gotson.komga.interfaces.api.webdav

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.Library
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.UserRoles
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.infrastructure.web.getMediaTypeOrDefault
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.springframework.http.HttpRange
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.server.ResponseStatusException
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.name

internal sealed interface WebDavNode {
  val name: String
}

internal class WebDavDir(
  override val name: String,
  val children: () -> List<WebDavNode>,
) : WebDavNode

internal class WebDavFile(
  override val name: String,
  val book: Book,
) : WebDavNode

private class PropfindEntry(
  val href: String,
  val name: String,
  val directory: Boolean,
  val lastModified: String,
  val created: String,
  val contentType: String?,
  val contentLength: Long?,
  val etag: String?,
)

/**
 * Read-only WebDAV endpoint exposing the Komga libraries as a virtual tree:
 * `/webdav/<Library>/<Series>/<File>`, one-shot books being listed directly under their library root.
 *
 * Supported methods: OPTIONS, PROPFIND (Depth 0/1), GET, HEAD. Any other method returns 405.
 * Access control mirrors the REST API: library sharing ([KomgaUser.canAccessLibrary]), content restrictions
 * ([ContentRestrictionChecker]) and the `FILE_DOWNLOAD` role for file retrieval.
 */
class WebDavServlet(
  private val libraryRepository: LibraryRepository,
  private val seriesRepository: SeriesRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
) : HttpServlet() {
  override fun service(
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) = handle(request, response)

  /**
   * Entry point for unit tests: [jakarta.servlet.http.HttpServlet.service] is protected in Java, and therefore
   * not callable from Kotlin test code.
   */
  internal fun handle(
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    response.setHeader("DAV", "1")
    when (request.method) {
      "OPTIONS" -> handleOptions(response)
      "PROPFIND" -> handlePropfind(request, response)
      "GET" -> handleGet(request, response, withBody = true)
      "HEAD" -> handleGet(request, response, withBody = false)
      else -> sendMethodNotAllowed(response)
    }
  }

  private fun handleOptions(response: HttpServletResponse) {
    response.status = HttpServletResponse.SC_OK
    response.setHeader("Allow", ALLOWED_METHODS)
    response.setHeader("MS-Author-Via", "DAV")
    response.flushBuffer()
  }

  private fun handlePropfind(
    request: HttpServletRequest,
    response: HttpServletResponse,
  ) {
    val user = getCurrentUser() ?: return sendForbidden(response)
    val node = resolveNode(request, user) ?: return sendNotFound(response)
    val depth = parseDepth(request)
    val selfHref = selfHref(request, node)
    val entries = mutableListOf(entryFor(selfHref, node))
    if (depth > 0 && node is WebDavDir) {
      val base = selfHref.trimEnd('/')
      node.children().forEach { entries.add(entryFor("$base/${it.name.percentEncode()}", it)) }
    }
    response.status = MULTI_STATUS
    response.contentType = "application/xml;charset=UTF-8"
    response.writer.write(multistatusXml(entries))
  }

  private fun handleGet(
    request: HttpServletRequest,
    response: HttpServletResponse,
    withBody: Boolean,
  ) {
    val user = getCurrentUser() ?: return sendForbidden(response)
    val node = resolveNode(request, user) ?: return sendNotFound(response)
    when (node) {
      is WebDavDir -> sendMethodNotAllowed(response)
      is WebDavFile -> serveFile(request, response, user, node, withBody)
    }
  }

  private fun serveFile(
    request: HttpServletRequest,
    response: HttpServletResponse,
    user: KomgaUser,
    file: WebDavFile,
    withBody: Boolean,
  ) {
    if (!user.isAdmin && !user.roles.contains(UserRoles.FILE_DOWNLOAD)) return sendForbidden(response)
    try {
      contentRestrictionChecker.checkContentRestrictionBook(user, file.book)
    } catch (ex: ResponseStatusException) {
      return if (ex.statusCode.value() == HttpStatus.NOT_FOUND.value()) sendNotFound(response) else sendForbidden(response)
    }
    val path = file.book.path
    val attrs: BasicFileAttributes? =
      try {
        Files.readAttributes(path, BasicFileAttributes::class.java)
      } catch (_: Exception) {
        null
      }
    if (attrs == null) return sendNotFound(response)
    val size = attrs.size()

    response.setHeader("Accept-Ranges", "bytes")
    response.setHeader("Last-Modified", httpDate(LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneOffset.UTC)))
    response.setHeader("ETag", "\"${file.book.id}-$size\"")

    var start = 0L
    var end = size - 1
    var status = HttpServletResponse.SC_OK
    val range = parseRange(request)
    if (range != null) {
      val rangeStart = range.getRangeStart(size)
      val rangeEnd = minOf(range.getRangeEnd(size), size - 1)
      if (rangeStart < 0 || rangeStart > rangeEnd || rangeStart >= size) {
        response.status = HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE
        response.setHeader("Content-Range", "bytes */$size")
        return
      }
      start = rangeStart
      end = rangeEnd
      status = HttpServletResponse.SC_PARTIAL_CONTENT
    }

    response.status = status
    if (status == HttpServletResponse.SC_PARTIAL_CONTENT) response.setHeader("Content-Range", "bytes $start-$end/$size")
    response.setContentLengthLong(end - start + 1)
    response.contentType = getMediaTypeOrDefault(mediaRepository.findByIdOrNull(file.book.id)?.mediaType).toString()

    if (withBody && size > 0) writeRange(path, response.outputStream, start, end - start + 1)
  }

  private fun writeRange(
    path: Path,
    output: OutputStream,
    start: Long,
    length: Long,
  ) {
    Files.newInputStream(path).use { input ->
      var skipped = 0L
      while (skipped < start) {
        val n = input.skip(start - skipped)
        if (n <= 0) break
        skipped += n
      }
      val buffer = ByteArray(BUFFER_SIZE)
      var remaining = length
      while (remaining > 0) {
        val read = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
        if (read == -1) break
        output.write(buffer, 0, read)
        remaining -= read
      }
      output.flush()
    }
  }

  private fun parseRange(request: HttpServletRequest): HttpRange? {
    val header = request.getHeader("Range") ?: return null
    if (header.contains("*")) return null
    return try {
      HttpRange.parseRanges(header).singleOrNull()
    } catch (_: Exception) {
      null
    }
  }

  private fun parseDepth(request: HttpServletRequest): Int = if (request.getHeader("Depth") == "0") 0 else 1

  private fun resolveNode(
    request: HttpServletRequest,
    user: KomgaUser,
  ): WebDavNode? {
    val segments =
      request.pathInfo
        ?.trim('/')
        ?.split('/')
        ?.filter { it.isNotBlank() }
        ?: emptyList()
    return when (segments.size) {
      0 -> rootNode(user)
      1 -> findLibrary(user, segments[0])?.let { library -> libraryDir(user, library) }
      2 ->
        oneshotBook(user, segments[0], segments[1])?.let { book -> WebDavFile(book.path.name, book) }
          ?: findSeries(user, segments[0], segments[1])?.let { series -> WebDavDir(series.name) { seriesChildren(series.id) } }

      3 -> bookFile(user, segments[0], segments[1], segments[2])
      else -> null
    }
  }

  private fun rootNode(user: KomgaUser): WebDavNode {
    val libraries = accessibleLibraries(user)
    return WebDavDir(name = ROOT_NAME) { libraries.map { library -> libraryDir(user, library) } }
  }

  private fun libraryDir(
    user: KomgaUser,
    library: Library,
  ): WebDavDir {
    val siblings = accessibleLibraries(user)
    return WebDavDir(displayName(library, siblings)) { libraryChildren(user, library.id) }
  }

  private fun accessibleLibraries(user: KomgaUser): List<Library> =
    libraryRepository
      .findAll()
      .filter { it.unavailableDate == null && user.canAccessLibrary(it.id) }
      .sortedBy { it.id }

  private fun libraryChildren(
    user: KomgaUser,
    libraryId: String,
  ): List<WebDavNode> {
    val series = allowedSeries(user, libraryId)
    val directories =
      series
        .filter { !it.oneshot }
        .map { s -> WebDavDir(displayName(s, series)) { seriesChildren(s.id) } }
    val oneshotFiles =
      series
        .filter { it.oneshot }
        .flatMap { bookRepository.findAllBySeriesId(it.id) }
        .filter { it.deletedDate == null }
        .sortedBy { it.id }
        .map { book -> WebDavFile(book.path.name, book) }
    return directories + oneshotFiles
  }

  private fun seriesChildren(seriesId: String): List<WebDavNode> =
    bookRepository
      .findAllBySeriesId(seriesId)
      .filter { it.deletedDate == null }
      .sortedBy { it.id }
      .map { book -> WebDavFile(book.path.name, book) }

  private fun findLibrary(
    user: KomgaUser,
    segment: String,
  ): Library? {
    val libraries = accessibleLibraries(user)
    return libraries.find { it.id == segment }
      ?: libraries.filter { displayName(it, libraries) == segment }.minByOrNull { it.id }
  }

  private fun findSeries(
    user: KomgaUser,
    librarySegment: String,
    seriesSegment: String,
  ): Series? {
    val library = findLibrary(user, librarySegment) ?: return null
    val series = allowedSeries(user, library.id)
    return series.find { it.id == seriesSegment }
      ?: series.filter { displayName(it, series) == seriesSegment }.minByOrNull { it.id }
  }

  private fun oneshotBook(
    user: KomgaUser,
    librarySegment: String,
    fileName: String,
  ): Book? {
    val library = findLibrary(user, librarySegment) ?: return null
    return allowedSeries(user, library.id)
      .filter { it.oneshot }
      .flatMap { bookRepository.findAllBySeriesId(it.id) }
      .filter { it.deletedDate == null && it.path.name == fileName }
      .minByOrNull { it.id }
  }

  private fun bookFile(
    user: KomgaUser,
    librarySegment: String,
    seriesSegment: String,
    fileName: String,
  ): WebDavFile? {
    val series = findSeries(user, librarySegment, seriesSegment) ?: return null
    val byId =
      bookRepository
        .findByIdOrNull(fileName)
        ?.takeIf { it.seriesId == series.id && it.deletedDate == null }
    val book =
      byId
        ?: bookRepository
          .findAllBySeriesId(series.id)
          .filter { it.deletedDate == null && it.path.name == fileName }
          .minByOrNull { it.id }
    return book?.let { WebDavFile(it.path.name, it) }
  }

  private fun allowedSeries(
    user: KomgaUser,
    libraryId: String,
  ): List<Series> =
    seriesRepository
      .findAllByLibraryId(libraryId)
      .filter { it.deletedDate == null }
      .sortedBy { it.id }

  private fun entryFor(
    href: String,
    node: WebDavNode,
  ): PropfindEntry =
    when (node) {
      is WebDavDir -> {
        val now = LocalDateTime.now()
        PropfindEntry(
          href = href,
          name = node.name,
          directory = true,
          lastModified = httpDate(now),
          created = iso8601(now),
          contentType = null,
          contentLength = null,
          etag = null,
        )
      }

      is WebDavFile -> {
        val attrs: BasicFileAttributes? =
          try {
            Files.readAttributes(node.book.path, BasicFileAttributes::class.java)
          } catch (_: Exception) {
            null
          }
        val size = attrs?.size() ?: node.book.fileSize
        val lastModified =
          attrs?.let { LocalDateTime.ofInstant(it.lastModifiedTime().toInstant(), ZoneOffset.UTC) }
            ?: node.book.fileLastModified
        val created =
          attrs?.let { LocalDateTime.ofInstant(it.creationTime().toInstant(), ZoneOffset.UTC) }
            ?: lastModified
        PropfindEntry(
          href = href,
          name = node.name,
          directory = false,
          lastModified = httpDate(lastModified),
          created = iso8601(created),
          contentType = getMediaTypeOrDefault(mediaRepository.findByIdOrNull(node.book.id)?.mediaType).toString(),
          contentLength = size,
          etag = "\"${node.book.id}-$size\"",
        )
      }
    }

  private fun multistatusXml(entries: List<PropfindEntry>): String =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<d:multistatus xmlns:d=\"DAV:\">" +
      entries.joinToString(separator = "") { responseXml(it) } +
      "</d:multistatus>"

  private fun responseXml(entry: PropfindEntry): String =
    "<d:response>" +
      "<d:href>${entry.href.escapeXml()}</d:href>" +
      "<d:propstat><d:prop>" +
      "<d:resourcetype>${if (entry.directory) "<d:collection/>" else ""}</d:resourcetype>" +
      "<d:displayname>${entry.name.escapeXml()}</d:displayname>" +
      entry.contentType?.let { "<d:getcontenttype>${it.escapeXml()}</d:getcontenttype>" }.orEmpty() +
      entry.contentLength?.let { "<d:getcontentlength>$it</d:getcontentlength>" }.orEmpty() +
      entry.etag?.let { "<d:getetag>${it.escapeXml()}</d:getetag>" }.orEmpty() +
      "<d:getlastmodified>${entry.lastModified.escapeXml()}</d:getlastmodified>" +
      "<d:creationdate>${entry.created.escapeXml()}</d:creationdate>" +
      "</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>"

  private fun selfHref(
    request: HttpServletRequest,
    node: WebDavNode,
  ): String {
    val uri = request.requestURI ?: (WebDavConfiguration.PATH_PREFIX + (request.pathInfo ?: ""))
    return if (node is WebDavDir && !uri.endsWith("/")) "$uri/" else uri
  }

  private fun displayName(
    library: Library,
    siblings: List<Library>,
  ): String = if (siblings.count { it.name == library.name } > 1) "${library.name.replace('/', '-')} (${library.id})" else library.name

  private fun displayName(
    series: Series,
    siblings: List<Series>,
  ): String = if (siblings.count { it.name == series.name } > 1) "${series.name.replace('/', '-')} (${series.id})" else series.name

  private fun getCurrentUser(): KomgaUser? = (SecurityContextHolder.getContext().authentication?.principal as? KomgaPrincipal)?.user

  private fun httpDate(value: LocalDateTime): String = RFC_1123.format(value.atOffset(ZoneOffset.UTC))

  private fun iso8601(value: LocalDateTime): String = ISO_8601.format(value.atOffset(ZoneOffset.UTC))

  private fun sendMethodNotAllowed(response: HttpServletResponse) {
    response.status = HttpServletResponse.SC_METHOD_NOT_ALLOWED
    response.setHeader("Allow", ALLOWED_METHODS)
    response.flushBuffer()
  }

  private fun sendNotFound(response: HttpServletResponse) {
    response.status = HttpServletResponse.SC_NOT_FOUND
    response.flushBuffer()
  }

  private fun sendForbidden(response: HttpServletResponse) {
    response.status = HttpServletResponse.SC_FORBIDDEN
    response.flushBuffer()
  }

  companion object {
    const val ALLOWED_METHODS = "OPTIONS, GET, HEAD, PROPFIND"
    private const val MULTI_STATUS = 207
    private const val BUFFER_SIZE = 8192
    private const val ROOT_NAME = "webdav"
    private val RFC_1123: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
    private val ISO_8601: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  }
}

private fun String.percentEncode(): String =
  URLEncoder
    .encode(this, StandardCharsets.UTF_8)
    .replace("+", "%20")

private fun String.escapeXml(): String =
  this
    .replace("\u0026", "\u0026amp;")
    .replace("<", "\u0026lt;")
    .replace(">", "\u0026gt;")
    .replace("\"", "\u0026quot;")
