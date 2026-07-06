[![Open Collective backers and sponsors](https://img.shields.io/opencollective/all/komga?label=OpenCollective%20Sponsors&color=success)](https://opencollective.com/komga) [![GitHub Sponsors](https://img.shields.io/github/sponsors/gotson?label=Github%20Sponsors&color=success)](https://github.com/sponsors/gotson)
[![Discord](https://img.shields.io/discord/678794935368941569?label=Discord&color=blue)](https://discord.gg/TdRpkDu)

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/gotson/komga/tests.yml?branch=master)](https://github.com/gotson/komga/actions?query=workflow%3ATests+branch%3Amaster)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/gotson/komga?color=blue&label=download&sort=semver)](https://github.com/gotson/komga/releases) [![GitHub all releases](https://img.shields.io/github/downloads/gotson/komga/total?color=blue&label=github%20downloads)](https://github.com/gotson/komga/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/gotson/komga)](https://hub.docker.com/r/gotson/komga)

[![Translation status](https://hosted.weblate.org/widgets/komga/-/webui/svg-badge.svg)](https://hosted.weblate.org/engage/komga/)

# ![app icon](./.github/readme-images/app-icon.png) Komga

Komga is a media server for your comics, mangas, BDs, magazines and eBooks.

#### Chat on [Discord](https://discord.gg/TdRpkDu)

## Features

- Browse libraries, series and books via a responsive web UI that works on desktop, tablets and phones
- Organize your library with collections and read lists
- Edit metadata for your series and books
- Import embedded metadata automatically
- Webreader with multiple reading modes
- Manage multiple users, with per-library access control, age restrictions, and labels restrictions
- Offers a REST API, many community tools and scripts can interact with Komga
- OPDS v1 and v2 support
- Kobo Sync with your Kobo eReader
- KOReader Sync
- Download book files, whole series, or read lists
- Duplicate files detection
- Duplicate pages detection and removal
- Import books from outside your libraries directly into your series folder
- Import ComicRack `cbl` read lists

## Installation

Refer to the [website](https://komga.org/docs/category/installation) for instructions.

## Documentation

Head over to our [website](https://komga.org) for more information.

## Develop in Komga

Check the [development guidelines](./DEVELOPING.md).

## Translation

[![Translation status](https://hosted.weblate.org/widgets/komga/-/webui/horizontal-auto.svg)](https://hosted.weblate.org/engage/komga/)

## Powered by

[![Jetbrains_logo](./.github/readme-images/jetbrains.svg)](https://www.jetbrains.com/?from=Komga)

Thanks to [JetBrains](https://www.jetbrains.com/?from=Komga) for providing the development environment that helps us develop Komga.

[![Chromatic logo](https://user-images.githubusercontent.com/321738/84662277-e3db4f80-af1b-11ea-88f5-91d67a5e59f6.png)](https://www.chromatic.com)

Thanks to [Chromatic](https://www.chromatic.com/) for providing the visual testing platform that helps us review UI changes and catch visual regressions.

## Credits

The Komga icon is based on an icon made by [Freepik](https://www.freepik.com/home) from www.flaticon.com




## 🎨 Yamada-Tech Custom Features

This custom fork includes tailored optimization features for premium, streamlined self-hosting, focusing on interactive user engagement, minimalist administrative control, and lightweight container virtualization.

### 🚀 1. Interactive "Trending Manga" Carousel (みんなが読んでるマンガ)
- **Append-Only Tracking:** Replaced the state-based table with a resilient `READING_EVENT` event-driven log base to capture dynamic reader progress.
- **Daily Aggregation Metric:** Introduces `SERIES_READING_STATS_DAILY` to calculate statistics (unique readers, progress spikes, completions) efficiently.
- **Visual Engagement UI:** A beautiful horizontal-scrolling section right at the top of the main Dashboard. It dynamically renders the **First Volume's Thumbnail / Cover Art** of active series to entice other users to join in and "read together" based on weighted vibe scores.

### ⚙️ 2. Dynamic Sidebar Component Toggle (サイドバー表示の動的切替)
- **Administrative Switches:** Extended the core `ServerSettings.vue` and backend configuration (`KomgaSettingsProvider`) with boolean flags.
- **On/Off Menu Visibility Control:** Administrators can now toggle checkbox options inside the server settings dashboard to show/hide cluttering menus instantly from the layout.
- **Supported Toggles:**
  - **Import Menu** (インポートメニューの非表示)
  - **Media Analysis Menu** (メディア分析・重複等の非表示)
  - **Server System History** (バックエンド処理履歴の非表示)
- **Upgrade-Proof Resilience:** Safely built via conditional rendering parameters (`v-if`), ensuring hassle-free upstream synchronization with official branch updates without triggering layout code conflicts.

### 📦 3. Optimized Multi-Stage Docker Environment (Synology NAS Optimization)
- **Ultra-Lightweight Build:** Stripped out all legacy JUnit backend test cases, Vue frontend test fixtures, and official CI/CD automation workflows, shrinking the initial codebase footprint dramatically.
- **Instant Local Containerization:** Bundled with a specialized multi-stage `Dockerfile` configured to compile the Vue 2 assets directly inside a secure container layer before injection into the Spring Boot runner core.
- **NAS Container Manager Ready:** Tailored for effortless x86_64 host compilation on standard Synology NAS systems (e.g., DS718+) for seamless persistent storage mounting (`/config`, `/data`).




### Windows Local Build Setup Guide

This guide details the system prerequisites and execution workflow required to successfully run the local build pipeline on Windows 11. 

### 1\. System Prerequisites

**Important:** Do NOT use the standard executable installers (`.msi` or `.exe`). You must download the **ZIP / Archive distributions** of the following runtimes and extract them manually to ensure they match the hardcoded script paths. 

*   **Java Development Kit (JDK):** **Strictly requires JDK 21.** Download the compressed ZIP archive (e.g., from Adoptium Eclipse Temurin or Oracle) and extract it so the binaries reside at `C:\tools\jdk\bin`. Compilation will fail on lower or higher major versions. 
*   **Node.js & npm:** Download the Windows Binary ZIP archive (`.zip`) from the official Node.js downloads page. Extract the contents directly under `C:\tools\node` so that `npm.cmd` and `node.exe` are located right inside that directory. 

### 2\. Build & Execution Workflow

1.  Open your standard Windows Command Prompt (`cmd`). 
2.  Run the pre-configured orchestration script from the repository root:
    
    cmd
    
        build-project.bat
        
    
     
3.  After the pipeline outputs `BUILD SUCCESSFUL`, execute the fully-bundled independent execution artifact to initialize the web application:
    
    cmd
    
        java -jar "komga/build/libs/komga-1.24.4.jar"
        
    
     
4.  Access `http://localhost:25600` via your web browser to confirm the working status of the user interface.
