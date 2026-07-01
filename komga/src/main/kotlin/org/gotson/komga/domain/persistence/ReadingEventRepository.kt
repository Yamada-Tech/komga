package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.ReadingEvent

interface ReadingEventRepository {
  fun insert(event: ReadingEvent)

  fun deleteByBookId(bookId: String)

  fun deleteByUserId(userId: String)

  fun deleteAll()
}
