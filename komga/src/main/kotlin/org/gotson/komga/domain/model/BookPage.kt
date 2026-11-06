package org.gotson.komga.domain.model

open class BookPage(
  val fileName: String,
  val mediaType: String,
  val dimension: Dimension? = null,
  val fileSize: Long? = null,
) {
  override fun toString(): String = "BookPage(fileName='$fileName', mediaType='$mediaType', dimension=$dimension, fileSize=$fileSize)"

  fun copy(
    fileName: String = this.fileName,
    mediaType: String = this.mediaType,
    dimension: Dimension? = this.dimension,
    fileSize: Long? = this.fileSize,
  ) = BookPage(
    fileName = fileName,
    mediaType = mediaType,
    dimension = dimension,
    fileSize = fileSize,
  )
}
