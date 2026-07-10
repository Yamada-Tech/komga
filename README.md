# Project Overview

This is the README for our project.

# Komga (Custom Fork)

This is a custom fork based on the official [Komga](https://github.com/gotson/komga) project.

The goal of this repository is to maintain the original Komga as a base while experimenting with custom features and improvements.

## Disclaimer

> Releases are published after reasonable testing, but **no warranty is provided** for functionality or stability.
> Bug reports and feature requests are welcome — feel free to open an issue.

## Custom Changes

### Features
2026/07/10
- **Trending Manga Section on Dashboard** — Added a "Trending" section to the dashboard that displays trending manga series, with full i18n support for all major locales.
- **E-Ink Theme Mode** — Added a dedicated E-Ink theme optimized for 6-inch e-ink displays, including a custom item browser, section tabs, and touch scrolling disabled for e-ink compatibility.
- **Authentication Activity: Clear History** — Added a "Clear History" button to the Authentication Activity page, backed by a new API endpoint, with localized confirmation dialog.

### Bug Fixes

- **Duplicate Login Event Deduplication** — Fixed duplicate login success events being recorded within a 30-second window per userId + IP using Caffeine cache-based deduplication.
