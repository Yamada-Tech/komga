import urls from '@/functions/urls'

export const DEFAULT_SITE_NAME = 'Komga'

let siteName: string = (window as any).komgaSiteName || DEFAULT_SITE_NAME

export function getSiteName(): string {
  return siteName
}

export function setSiteName(name?: string | null) {
  siteName = name && name.trim() !== '' ? name : DEFAULT_SITE_NAME
}

/**
 * Build a document title, prefixed with the (possibly customized) site name.
 */
export function siteTitle(...parts: (string | undefined | null)[]): string {
  const tail = parts.filter(p => !!p).join(' - ')
  return tail !== '' ? `${siteName} - ${tail}` : siteName
}

export function logoUrl(version?: number): string {
  const base = `${urls.originNoSlash}/api/v1/settings/logo`
  return version !== undefined ? `${base}?v=${version}` : base
}

function replaceLinkRel(rel: string, href: string) {
  let link = document.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`)
  if (!link) {
    link = document.createElement('link')
    link.rel = rel
    document.head.appendChild(link)
  }
  link.href = href
}

/**
 * Replace the favicons with the custom logo when one is available.
 */
export function applyFavicon(logoAvailable: boolean, version?: number) {
  if (!logoAvailable) return
  const url = logoUrl(version)
  replaceLinkRel('icon', url)
  replaceLinkRel('apple-touch-icon', url)
  document.querySelectorAll<HTMLLinkElement>('link[rel="icon"], link[rel="shortcut icon"]').forEach(l => l.href = url)
}
