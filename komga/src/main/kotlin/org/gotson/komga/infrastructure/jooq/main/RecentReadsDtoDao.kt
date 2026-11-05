package org.gotson.komga.infrastructure.jooq.main

import org.gotson.komga.infrastructure.jooq.SplitDslDaoBase
import org.gotson.komga.interfaces.api.persistence.RecentReadsDtoRepository
import org.gotson.komga.interfaces.api.rest.dto.RecentReadDto
import org.gotson.komga.jooq.main.Tables
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class RecentReadsDtoDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  RecentReadsDtoRepository {
  private val r = Tables.READ_PROGRESS
  private val b = Tables.BOOK
  private val bm = Tables.BOOK_METADATA
  private val s = Tables.SERIES
  private val sm = Tables.SERIES_METADATA
  private val u = Tables.USER
  private val m = Tables.MEDIA

  override fun findRecentReads(limit: Int): List<RecentReadDto> =
    dslRO
      .select(
        r.USER_ID,
        u.EMAIL,
        r.BOOK_ID,
        DSL.coalesce(bm.TITLE, b.NAME),
        b.SERIES_ID,
        DSL.coalesce(sm.TITLE, s.NAME),
        r.PAGE,
        m.PAGE_COUNT,
        r.COMPLETED,
        DSL.coalesce(r.READ_DATE, r.LAST_MODIFIED_DATE),
      ).from(r)
      .innerJoin(b)
      .on(b.ID.eq(r.BOOK_ID))
      .innerJoin(u)
      .on(u.ID.eq(r.USER_ID))
      .leftJoin(bm)
      .on(bm.BOOK_ID.eq(b.ID))
      .innerJoin(s)
      .on(s.ID.eq(b.SERIES_ID))
      .leftJoin(sm)
      .on(sm.SERIES_ID.eq(s.ID))
      .leftJoin(m)
      .on(m.BOOK_ID.eq(b.ID))
      .where(DSL.coalesce(r.READ_DATE, r.LAST_MODIFIED_DATE).isNotNull)
      .orderBy(DSL.coalesce(r.READ_DATE, r.LAST_MODIFIED_DATE).desc())
      .limit(limit)
      .fetch {
        RecentReadDto(
          userId = it.value1(),
          userEmail = it.value2(),
          bookId = it.value3(),
          bookTitle = it.value4(),
          seriesId = it.value5(),
          seriesTitle = it.value6(),
          page = it.value7(),
          pagesCount = it.value8(),
          completed = it.value9(),
          readDate = it.value10()!!,
        )
      }
}
