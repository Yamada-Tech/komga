import { ReadingPosition } from '@/types/custom'
import { R2Progression } from '@/types/readium'
import urls from '@/functions/urls'

export function r2ProgressionToReadingPosition(progression: R2Progression | undefined = undefined, bookId: string): ReadingPosition | undefined {
  if (!progression) return undefined

  const rawFragment = progression.locator.locations.fragment
  const safeFragment = Array.isArray(rawFragment) 
    ? rawFragment[0] 
    : (typeof rawFragment === 'string' ? rawFragment : undefined)

  return {
    created: progression.modified,
    href: `${urls.originNoSlash}/api/v1/books/${bookId}/resource/${progression.locator.href}`,
    type: progression.locator.type,
    title: progression.locator.title,
    locations: {
      fragment: safeFragment,
      position: progression.locator.locations.position,
      progression: progression.locator.locations.progression,
      totalProgression: progression.locator.locations.totalProgression,
    },
    text: progression.locator.text,
  }
}
