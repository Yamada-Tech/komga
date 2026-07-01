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

## Windows 11 Portable Build (No-Installation Method)
For Windows 11 users who want to build and run the custom Komga server instantly without registering system-wide environment variables or installing native installers.

1. Setup Required Tools (Portable/ZIP Versions)
Download JDK 21 (Windows/x64 ZIP) from jdk.java.net/21 and extract it. Rename and place the folder exactly at: C:\tools\jdk (Ensure C:\tools\jdk\bin\java.exe exists).
Download Node.js (Windows Binary .zip 64-bit) from nodejs.org and extract it. Move all internal files directly under: C:\tools\node (Ensure C:\tools\node\node.exe and npm.cmd exist directly in this directory).
2. Execute Automated Compilation
Download this repository as a ZIP and extract it anywhere (e.g., your Desktop).
Double-click the build.cmd file located in the root directory.
The script will automatically bind local portable tools, compile the Vue 2 frontend UI, and package the executable backend.
3. Launch the Server
Once completed, navigate to the build/distributions/ directory.
Extract the generated KomgaCustom-windows-portable.zip to your preferred location.
Double-click run-komga.bat inside the extracted folder to launch the server. It will automatically open your browser at http://localhost:25600.
