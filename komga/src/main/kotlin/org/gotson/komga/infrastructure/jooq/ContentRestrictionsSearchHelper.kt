package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.domain.model.ContentRestrictions
import org.jooq.Condition
import org.jooq.impl.DSL

/**
 * Fork note: age rating and sharing labels have been removed from series metadata,
 * so content restrictions always evaluate to allowed.
 */
class ContentRestrictionsSearchHelper(
  val restrictions: ContentRestrictions,
) {
  fun toCondition(): Pair<Condition, Set<RequiredJoin>> = DSL.noCondition() to emptySet()
}
