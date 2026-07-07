[![Open Collective backers and sponsors](https://img.shields.io/opencollective/all/komga?label=OpenCollective%20Sponsors&color=success)](https://opencollective.com/komga) [![GitHub Sponsors](https://img.shields.io/github/sponsors/gotson?label=Github%20Sponsors&color=success)](https://github.com/sponsors/gotson)
[![Discord](https://img.shields.io/discord/678794935368941569?label=Discord&color=blue)](https://discord.gg/TdRpkDu)

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/gotson/komga/tests.yml?branch=master)](https://github.com/gotson/komga/actions?query=workflow%3ATests+branch%3Amaster)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/gotson/komga?color=blue&label=download&sort=semver)](https://github.com/gotson/komga/releases) [![GitHub all releases](https://img.shields.io/github/downloads/gotson/komga/total?color=blue&label=github%20downloads)](https://github.com/gotson/komga/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/gotson/komga)](https://hub.docker.com/r/gotson/komga)

[![Translation status](https://hosted.weblate.org/widgets/komga/-/webui/svg-badge.svg)](https://hosted.weblate.org/engage/komga/)

# ![app icon](./.github/readme-images/app-icon.png) Komga

Komga is a media server for your comics, mangas, BDs, magazines and eBooks.

#### Chat on [Discord](https://discord.gg/TdRpkDu)
<br>
<br>

## ⚠️ Custom Fork NoticeThis
repository is a custom fork of the original gotson/komga. It contains personalized enhancements optimized for local NAS deployment and specific UI preferences.
For the official documentation and original source, please visit the Official Komga Repository.
<br>
<br>

## Yamada-Tech Custom Features

This custom fork includes tailored optimization features for premium, streamlined self-hosting, focusing on interactive user engagement, minimalist administrative control, and lightweight container virtualization. 

### 1. Interactive "Trending Manga" Carousel

*   **Append-Only Tracking:** Replaced the traditional state-based database architecture with a resilient `READING_EVENT` event-driven log base to capture real-time, dynamic reader progress. 
*   **Daily Aggregation Metric:** Introduces the `SERIES_READING_STATS_DAILY` processing table to efficiently calculate engagement analytics (unique readers, velocity spikes, and completion frequencies). 
*   **Visual Engagement UI:** A beautiful, responsive horizontal-scrolling showcase positioned directly at the peak of the main Dashboard. It dynamically renders active series cover arts to promote concurrent reading milestones based on aggregated user vibe scores. 

### 2. Dynamic Sidebar Component Toggle

*   **Administrative Switches:** Extended the core administrative template (`ServerSettings.vue`) and backend internal runtime configuration bindings (`KomgaSettingsProvider`) with dedicated boolean controls. 
*   **On/Off Menu Visibility Control:** Systems administrators can now check/uncheck display parameters inside the management dashboard to hide or show complex navigation trees instantly. 
*   **Supported Toggles:**
    *   Import Menu
    *   Media Analysis Menu
    *   Server System History 
*   **Upgrade-Proof Resilience:** Safely structured via highly decoupled conditional layout renderers (`v-if`), ensuring seamless upstream synchronization with official repository updates without inducing client-side presentation conflicts. 

### 3. Infrastructure & Lifecycle Automation

*   **Streamlined Gradle-Vite Pipeline:** Integrated backend server assembly and frontend WebUI compilation into a single, cohesive command execution path (`:komga:prepareThymeLeaf`), eliminating fragmented manual directory cloning operations. 
*   **NTFS Asset Compilation Patch:** Bypasses recursive background file-scanning constraints unique to Windows developer environments by enforcing modular webpack plugin exclusion rules, dropping memory load by several gigabytes. 
*   **Low-Footprint Runtime Target:** Formulated to pack fully-independent executable JAR payloads directly into optimized deployment layers, guaranteeing minimal resource utilization when mounted inside high-efficiency host nodes like Synology NAS equipment.



## Windows Local Build Setup Guide

This guide details the system prerequisites and execution workflow required to successfully run the local build pipeline on Windows 11. 

### 1.System Prerequisites

**Important:** Do NOT use the standard executable installers (`.msi` or `.exe`). You must download the **ZIP / Archive distributions** of the following runtimes and extract them manually to ensure they match the hardcoded script paths. 

*   **Java Development Kit (JDK):** **Strictly requires JDK 21.** Download the compressed ZIP archive (e.g., from Adoptium Eclipse Temurin or Oracle) and extract it so the binaries reside at `C:\tools\jdk\bin`. Compilation will fail on lower or higher major versions. 
*   **Node.js & npm:** Download the Windows Binary ZIP archive (`.zip`) from the official Node.js downloads page. Extract the contents directly under `C:\tools\node` so that `npm.cmd` and `node.exe` are located right inside that directory. 

### 2.Build & Execution Workflow

1.  Open your standard Windows Command Prompt (`cmd`).
   
2.  Run the pre-configured orchestration script from the repository root:
    
    cmd
        build-project.bat
    
3.  After the pipeline outputs `BUILD SUCCESSFUL`, execute the fully-bundled independent execution artifact to initialize the web application:
    
    cmd
        java -jar "komga/complete/komga-*.**.*.-cunstom.jar"
    
4.  Access `http://localhost:25600` via your web browser to confirm the working status of the user interface.
