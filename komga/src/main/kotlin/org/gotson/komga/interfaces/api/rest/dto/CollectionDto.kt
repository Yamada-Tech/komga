package org.gotson.komga.interfaces.api.rest.dto

import com.fasterxml.jackson.annotation.JsonFormat
import org.gotson.komga.domain.model.SeriesCollection
import org.gotson.komga.language.toUTC
import java.time.LocalDateTime

data class CollectionDto(
  val id: String,
  val name: String,
  val ordered: Boolean,
  val showInSidebar: Boolean,
  val seriesIds: List<String>,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdDate: LocalDateTime,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val lastModifiedDate: LocalDateTime,
  val filtered: Boolean,
)

fun SeriesCollection.toDto(showInSidebar: Boolean = false) =
  CollectionDto(
    id = id,
    name = name,
    ordered = ordered,
    showInSidebar = showInSidebar,
    seriesIds = seriesIds,
    createdDate = createdDate.toUTC(),
    lastModifiedDate = lastModifiedDate.toUTC(),
    filtered = filtered,
  )
