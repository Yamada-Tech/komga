package org.gotson.komga.infrastructure.jooq.main

import org.gotson.komga.domain.model.ReadingEvent
import org.gotson.komga.domain.model.ReadingEventType
import org.gotson.komga.domain.persistence.ReadingEventRepository
import org.gotson.komga.infrastructure.jooq.SplitDslDaoBase
import org.gotson.komga.jooq.main.Tables
import org.gotson.komga.language.toUTC
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ReadingEventDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  ReadingEventRepository {
  private val re = Tables.READING_EVENT

  override fun insert(event: ReadingEvent) {
    dslRW
      .insertInto(re, re.ID, re.USER_ID, re.BOOK_ID, re.SERIES_ID, re.TYPE, re.CREATED_AT)
      .values(event.id, event.userId, event.bookId, event.seriesId, event.type.name, event.createdAt.toUTC())
      .execute()
  }

  override fun deleteByBookId(bookId: String) {
    dslRW.deleteFrom(re).where(re.BOOK_ID.eq(bookId)).execute()
  }

  override fun deleteByUserId(userId: String) {
    dslRW.deleteFrom(re).where(re.USER_ID.eq(userId)).execute()
  }

  override fun deleteAll() {
    dslRW.deleteFrom(re).execute()
  }
}
