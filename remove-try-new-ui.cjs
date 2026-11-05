// Codemod: replace hard-coded "Komga" document titles with the configurable site name helper.
// Run: node document-title-branding.cjs
const fs = require('fs')
const path = require('path')

const files = [
  'komga-webui/src/router.ts',
  'komga-webui/src/views/BrowseBooks.vue',
  'komga-webui/src/views/BrowseBook.vue',
  'komga-webui/src/views/BrowseReadLists.vue',
  'komga-webui/src/views/BrowseReadList.vue',
  'komga-webui/src/views/BrowseLibraries.vue',
  'komga-webui/src/views/BrowseSeries.vue',
  'komga-webui/src/views/BrowseCollections.vue',
  'komga-webui/src/views/BrowseCollection.vue',
  'komga-webui/src/views/BrowseOneshot.vue',
  'komga-webui/src/views/DashboardView.vue',
  'komga-webui/src/views/DivinaReader.vue',
  'komga-webui/src/views/EpubReader.vue',
]

const IMPORT = "import {siteTitle} from '@/functions/branding'"

// document.title = `Komga - ${a}`  ->  document.title = siteTitle(`${a}`)
const tplRe = /document\.title = `Komga - (.*)`/g
// document.title = 'Komga'  ->  document.title = siteTitle()
const plainRe = /document\.title = 'Komga'/g

let changed = 0
for (const f of files) {
  const p = path.join(__dirname, f)
  let src = fs.readFileSync(p, 'utf8')
  const before = src
  src = src.replace(tplRe, (_m, inner) => `document.title = siteTitle(\`${inner}\`)`)
  src = src.replace(plainRe, 'document.title = siteTitle()')
  if (src === before) {
    console.log(`SKIP (no match): ${f}`)
    continue
  }
  if (!src.includes(IMPORT)) {
    // insert the import after the first import line of the script block
    const lines = src.split('\n')
    const idx = lines.findIndex(l => /^import\s+.*\s+from\s+/.test(l.replace(/\r$/, '')))
    if (idx === -1) {
      console.log(`ERROR: no import line in ${f}`)
      process.exit(1)
    }
    lines.splice(idx + 1, 0, IMPORT)
    src = lines.join('\n')
  }
  fs.writeFileSync(p, src, 'utf8')
  changed++
  console.log(`OK: ${f}`)
}
console.log(`changed=${changed}`)
