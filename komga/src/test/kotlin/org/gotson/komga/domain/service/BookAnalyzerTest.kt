package org.gotson.komga.domain.service

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearAllMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaExtensionEpub
import org.gotson.komga.infrastructure.mediacontainer.epub.EpubExtractor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import java.time.LocalDateTime

@SpringBootTest
class BookAnalyzerTest {
  @SpykBean
  private lateinit var bookAnalyzer: BookAnalyzer

  @SpykBean
  private lateinit var epubExtractor: EpubExtractor

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Nested
  inner class ArchiveFormats {
    @ParameterizedTest
    @ValueSource(
      strings = [
        "rar4.rar", "rar5.rar", "rar4-solid.rar", "rar5-solid.rar",
      ],
    )
    fun `given rar archives when analyzing then media status is READY`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).startsWith("application/x-rar-compressed")
      assertThat(media.status).isEqualTo(Media.Status.READY)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "rar4-encrypted.rar", "rar5-encrypted.rar",
      ],
    )
    fun `given rar encrypted archive when analyzing then media status is UNSUPPORTED`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).startsWith("application/x-rar-compressed")
      assertThat(media.status).isEqualTo(Media.Status.UNSUPPORTED)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "7zip.7z", "7zip-encrypted.7z",
      ],
    )
    fun `given 7zip archive when analyzing then media status is UNSUPPORTED`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/x-7z-compressed")
      assertThat(media.status).isEqualTo(Media.Status.UNSUPPORTED)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "zip.zip", "zip-bzip2.zip", "zip-copy.zip", "zip-deflate64.zip", "zip-lzma.zip", "zip-ppmd.zip",
      ],
    )
    fun `given zip archive when analyzing then media status is READY`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pages).hasSize(1)
    }

    @Test
    fun `given zip encrypted archive when analyzing then media status is ERROR`() {
      val file = ClassPathResource("archives/zip-encrypted.zip")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.ERROR)
    }

    @Test
    fun `given epub archive when analyzing then media status is READY`() {
      val file = ClassPathResource("archives/epub3.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
    }
  }

  @Nested
  inner class Epub {
    @Test
    fun `given broken epub archive when analyzing then media status is ERROR`() {
      val file = ClassPathResource("archives/zip-as-epub.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.ERROR)
      assertThat(media.pages).hasSize(0)
    }

    @Test
    fun `given regular epub archive when analyzing then comment is empty`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).isNull()
    }

    @Test
    fun `given epub archive when toc cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getToc(any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1035")
      assertThat(extension).isNotNull
      assertThat(extension!!.toc).isEmpty()
    }

    @Test
    fun `given epub archive when landmarks cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getLandmarks(any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1036")
      assertThat(extension).isNotNull
      assertThat(extension!!.landmarks).isEmpty()
    }

    @Test
    fun `given epub archive when page list cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getPageList(any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1037")
      assertThat(extension).isNotNull
      assertThat(extension!!.pageList).isEmpty()
    }

    @Test
    fun `given epub archive when divina pages cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getDivinaPages(any(), any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1038")
      assertThat(media.pages).isEmpty()
    }

    @Test
    fun `given epub archive when positions cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.computePositions(any(), any(), any(), any(), any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1039")
      assertThat(extension).isNotNull
      assertThat(extension!!.positions).isEmpty()
    }
  }
}
