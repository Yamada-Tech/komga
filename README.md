# ![app icon](./.github/readme-images/app-icon.png) Komga — Yamada-Tech fork

> **This repository is a personal fork of [gotson/komga](https://github.com/gotson/komga).**
> The original upstream README is kept untouched as [**README_org.md**](./README_org.md) — please read it for general information about the upstream project.

| | |
|---|---|
| Upstream baseline | **1.30.1** (merged 2026-11-11, regularly synced with upstream `master`/tags) |
| Fork origin | `Yamada-Tech/komga` (default branch: `master`) |
| License | MIT, same as upstream (Copyright (c) 2019 Gauthier Roebroeck), see [LICENSE](./LICENSE) |

This fork exists for **self-hosted, single-admin usage**: it strips cloud/telemetry-ish and rarely-used upstream features to keep the server lean, and adds personal convenience features (branding, public collections, read-only WebDAV, server maintenance API).

- **No GitHub Actions / CI** — the upstream workflows were removed. All artifacts are built locally (see [Build](#build)).
- **No Docker Hub / releases** published from this fork — build your own jar.
- Upstream feature list and credits: see [About the upstream project](#about-the-upstream-project).

---

## Features

Everything upstream offers still works, except where explicitly listed in [Removed features](#removed-features):

- Browse libraries, series and books via a responsive web UI that works on desktop, tablets and phones
- Organize your library with collections and read lists
- Edit metadata for your series and books
- Import embedded metadata automatically
- Webreader with multiple reading modes
- Manage multiple users, with per-library access control
- Offers a REST API, many community tools and scripts can interact with Komga
- OPDS v1 and v2 support
- Kobo Sync with your Kobo eReader
- KOReader Sync
- Download book files, whole series, or read lists
- Duplicate files detection
- Import ComicRack `cbl` read lists

### Added by this fork

| Feature | Description |
|---|---|
| **Branding** | Custom `siteName` and logo image. `GET /api/v1/settings/branding` and logo download are public; `POST/DELETE /api/v1/settings/logo` are ADMIN. Upload a 512×512 PNG; stored under `komga.config-dir`. Settings UI in the WebUI (server settings). |
| **Public collections** | `Collection.public` flag (default: public). `CollectionDto.public` in API, `show_public_collections` query for unauthenticated browsing, OPDS feeds expose public collections. |
| **Recent reads for admins** | `GET /api/v2/users/recent-reads` (ADMIN only, default 100 items) and `USER.LAST_LOGIN_DATE` (maintained by a login listener) — replaces the removed authentication-activity table. |
| **Optimize & restart** | `POST /api/v1/server/optimize-and-restart`: runs SQLite maintenance (VACUUM/ANALYZE via `DatabaseMaintenanceService`) then restarts the server in-process. Intended for the standalone jar; not for Docker. Button in the WebUI server-management screen. |
| **Read-only WebDAV** | Mounted at `/webdav/*` on the main port. Virtual tree `/webdav/<Library>/<Series>/<File>`. Supports `OPTIONS` (`DAV: 1`), `PROPFIND` (Depth 0/1, 207 Multistatus), `GET`/`HEAD` with `Range`/`ETag`/`Accept-Ranges`; all write methods return `405`. Auth: session, Basic (email/password) or `X-API-Key` header. Access control follows library access, series restrictions, and the `FILE_DOWNLOAD` role. Designed for WebDAV readers such as ComicShare (iOS). |
| **Locale-aware series grouping** | The *Series grouping* UI setting (server → user interface) shows the standard A–Z plus a locale-specific grouping when one is defined for the current UI language: gojūon あ〜ん for Japanese, А〜Я (Cyrillic) for Russian/Bulgarian, and a Ukrainian variant. |
| **Simplified series metadata UI** | Series edit dialog keeps general + genres/tags only; sort title is a picker (A–Z / あ〜ん). Leading `[Author]` prefixes are stripped from titles on save. |

### Removed features

These upstream features were deliberately removed (backend **and** WebUI, with DB columns/tables dropped by the fork migration [`V99991110000000__fork_db_cleanup.kt`](komga/src/flyway/kotlin/db/migration/sqlite/V99991110000000__fork_db_cleanup.kt)):

- **`next-ui`** — the new-UI frontend directory is deleted entirely; any `/next*` links and the "try the new UI" prompts are gone.
- **Page hashes / read history / transient books / book import** — duplicate *page* detection & removal, the historical-event book history, and importing books from outside libraries were dropped. (Duplicate **file** detection is untouched.)
- **Authentication activity** — replaced by `lastLoginDate` + the recent-reads endpoint above.
- **Age ratings & shared labels content restrictions** — series metadata fields `language`, `publisher`, `ageRating`, `totalBookCount`, `links`, `alternateTitles`, `sharingLabels` and their tables were dropped. **Consequence: per-user age/label content restrictions are no longer effective** (the check is a permissive stub); users see every library they can access. The corresponding user-side settings/API fields remain for compatibility but do nothing.
- **GitHub Actions workflows, CI, and upstream-only docs** — `build.yml`/`release.yml`/composite actions, `CONTRIBUTING.md`, `SECURITY.md`, `PRIVACY.md`, `DOCKERHUB.md`, `DEVELOPING.md` (merged into this file's build section), PR template.
- **komga-tray localized messages** — only the English `messages.properties` is kept.
- **ARM32 (`linux/arm/v7`) Docker image** — dropped upstream-side with Java 25; only `linux/amd64` and `linux/arm64/v8` remain.

### Security-related divergences

- Actuator web exposure reduced to `health,info,metrics,shutdown,logfile` (upstream exposes `*`). Consider blocking `/actuator` at a reverse proxy anyway.
- CSP headers (`default-src 'none'; ... sandbox`) on API page-image responses come from upstream 1.30.1.
- Upstream's `api.github.com` release feed and `komga.org` announcements endpoints are still present (cached, admin-only UI screens); this fork publishes nothing there, so they are useless but harmless. See note in [.ai_note.md](./.ai_note.md).
- No TLS termination, no login lockout upstream or in the fork: if you expose the server, use a reverse proxy with TLS + HSTS + rate limiting.

---

## Build

### Requirements

- **Java JDK 25+** (required; the fork builds and runs on Java 25 — classes are major version 69, a Java 21 runtime will *not* work). Set `JAVA_HOME` to a JDK 25, e.g. Adoptium Temurin 25.
- **Node.js 22+** for the WebUI (`.nvmrc` says 22; a newer LTS such as 24.x works fine).

### 1. Build the WebUI

```sh
cd komga-webui
npm ci
npm run build        # add NODE_OPTIONS=--max-old-space-size=4096 on Windows if fork-ts-checker dies
```

### 2. Build the backend jar (with WebUI embedded)

From the repository root — **do not forget the webui copy tasks**, or you will produce a jar with a stale/empty WebUI:

```sh
# Windows (cmd.exe), version 1.30.1
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
gradlew.bat :komga:webuiCopyDist :komga:webuiCopyIndex :komga:bootJar -x test
```

```sh
# Linux / macOS
export JAVA_HOME=/path/to/jdk-25
./gradlew :komga:webuiCopyDist :komga:webuiCopyIndex :komga:bootJar -x test
```

Output: `komga/build/libs/komga-<version>.jar` — **use this jar**, not `komga-<version>-plain.jar` (the plain jar contains no WebUI and no launcher).

Smoke test:

```sh
java -jar komga/build/libs/komga-<version>.jar --komga.config-dir=/tmp/komga-smoke --server.port=28090
# wait for "Started ApplicationKt", then check / and /api/v1/settings/branding return 200
```

### Backend development

See [komga/README.md](./komga/README.md) for Spring profiles (`dev`, `localdb`, `noclaim`) and Gradle tasks (`bootRun`, `test`, jOOQ codegen). Quick start:

```sh
gradlew.bat :komga:bootRun --args="--spring.profiles.active=dev,noclaim"
```

### Frontend development

See [komga-webui/README.md](./komga-webui/README.md). Run `npm run serve` (port 8081) against a backend started with the `dev` profile (port 8080, CORS enabled).

### Desktop wrapper

`komga-tray` is a thin JetBrains Compose desktop wrapper with a tray icon (see [komga-tray/AGENTS.md](./komga-tray/AGENTS.md)); build with `gradlew :komga-tray:jar`.

### Tests & style

```sh
gradlew.bat :komga:test
gradlew.bat :komga:ktlintMainSourceSetCheck :komga:ktlintTestSourceSetCheck
cd komga-webui && npm run lint
```

Commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) standard.

---

## Deployment notes (this fork)

- **Runtime: Java 25 is mandatory.** Running the jar with Java 21 fails at startup with `UnsupportedClassVersionError`.
- Database: **SQLite only** — `database.sqlite`, plus `tasks.sqlite` and `sessions.sqlite` (Spring Session JDBC) inside the config dir. Put the config dir on a **local filesystem** (`komga.database.check-local-filesystem=true` refuses network shares).
- First start after an upgrade runs Flyway migrations automatically, including the fork consolidation migration `V99991110000000__fork_db_cleanup.kt` (drops removed features' tables/columns; idempotent). **Back up the whole config dir (incl. `-wal`/`-shm`) before upgrading.** `spring.flyway.ignore-migration-patterns: ["*:missing","*:future"]` is required for this fork's DB history — do not remove it.
- `LogoImageService` resolves the logo path relative to `komga.config-dir`; always pass `--komga.config-dir`.
- WebUI-included jar sanity check: SPA routes such as `/dashboard` must return 200 (a WebUI-less jar returns 500).
- WebDAV clients: URL `http(s)://host:port/webdav`, Basic auth with a Komga user, or the `X-API-Key` header (some clients only support Basic — use email/password there).

### Upstream merges

This fork is kept in sync with upstream tags. Rules when merging (details in [.ai_note.md](./.ai_note.md)): deleted files (`next-ui/**`, workflows, upstream-only docs, `messages_*.properties`) must be `git rm`-ed again each time; `komga/docs/openapi.json` is resolved `--ours`; fork features (branding, public collections, recent-reads, WebDAV, `V99991110000000`) are grep-checked after every merge.

---

## About the upstream project

Komga is a media server for your comics, mangas, BDs, magazines and eBooks.
Upstream: [gotson/komga](https://github.com/gotson/komga) — full README: [README_org.md](./README_org.md)

- Website & installation docs (upstream releases): [komga.org](https://komga.org)
- Chat on [Discord](https://discord.gg/TdRpkDu)
- Support the upstream author: [OpenCollective Sponsors](https://opencollective.com/komga) / [GitHub Sponsors](https://github.com/sponsors/gotson)
- Translations are done by upstream on [Weblate](https://hosted.weblate.org/engage/komga/); this fork inherits them via upstream merges.

### Powered by

[![Jetbrains_logo](./.github/readme-images/jetbrains.svg)](https://www.jetbrains.com/?from=Komga)

Thanks to [JetBrains](https://www.jetbrains.com/?from=Komga) for providing the development environment that helps develop Komga.

[![Chromatic logo](https://user-images.githubusercontent.com/321738/84662277-e3db4f80-af1b-11ea-88f5-91d67a5e59f6.png)](https://www.chromatic.com)

Thanks to [Chromatic](https://www.chromatic.com/) for providing the visual testing platform that helps review UI changes and catch visual regressions.

### Credits

The Komga icon is based on an icon made by [Freepik](https://www.freepik.com/home) from www.flaticon.com
