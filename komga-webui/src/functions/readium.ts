import { ReadingPosition } from '@d-i-t-a/reader'
import { R2Progression } from '@/types/readium'
import urls from '@/functions/urls'

export function r2ProgressionToReadingPosition(progression: R2Progression | undefined = undefined, bookId: string): ReadingPosition | undefined {
  if (!progression) return undefined

  return {
    created: progression.modified,
    href: `${urls.originNoSlash}/api/v1/books/${bookId}/resource/${progression.locator.href}`,
    type: progression.locator.type,
    title: progression.locator.title,
    locations: {
      fragment: Array.isArray(progression.locator.locations.fragment) ? progression.locator.locations.fragment[0] : (typeof progression.locator.locations.fragment === 'string' ? progression.locator.locations.fragment : undefined),
      position: progression.locator.locations.position,
      progression: progression.locator.locations.progression,
      totalProgression: progression.locator.locations.totalProgression,
    },
    text: progression.locator.text,
  }
}
