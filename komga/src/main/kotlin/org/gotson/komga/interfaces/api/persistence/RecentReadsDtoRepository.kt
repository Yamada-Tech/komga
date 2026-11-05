package org.gotson.komga.interfaces.api.persistence

import org.gotson.komga.interfaces.api.rest.dto.RecentReadDto

interface RecentReadsDtoRepository {
  fun findRecentReads(limit: Int): List<RecentReadDto>
}
