# Changelog

All notable changes to the **CatNG** project will be documented in this file.

## [1.5.0] - 2026-08-20

### Added
- **Official LibXray Core Engine Integration**: Embedded native `libv2ray.aar` binary supporting `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- **Direct Tunnel Execution**: Real background Xray core loop started directly from the Android `VpnService` with TUN descriptor.
- **Native Geo Database Assets**: Embedded `geoip.dat` and `geosite.dat` for smart routing and local Iranian IP bypass.
- **Accurate Outbound Delay Measurement**: Real-time ping testing using Xray's `measureOutboundDelay` with Google connectivity check.
- **System Navigation Bar Insets Fix**: Added full `navigationBarsPadding` and `statusBarsPadding` to eliminate overlaps with Android 3-button and gesture navigation bars on small and older screens.

## [1.0.0] - 2026-08-20

### Added
- Initial open-source release of **CatNG**.
- Full **Material Design 3** user interface with Jetpack Compose.
- First-launch **Welcome Screen** onboarding flow.
- Complete parsing and validation for advanced **Xray JSON configurations** (DNS, SockOpt, HappyEyeballs, Sniffing, Routing).
- Multi-protocol URI parser for `vless://`, `vmess://`, `trojan://`, `ss://`, and `socks5://`.
- Intelligent **Clipboard Importer** supporting JSON arrays, links, and subscription URLs.
- **Subscription Group Selector** with folder UI, "All" view, and automated name resolution (`import_sub` fallback).
- Config **Sorting Engine** (Best Ping, Name A-Z, Name Z-A, Newest, Oldest).
- Automatic **Emoji Cleaner** to maintain pure vector iconography across the app UI.
