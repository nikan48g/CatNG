# CatNG

An open-source, modern Android client for Xray with a complete Material Design 3 experience, fast concurrent ping testing, smart subscription group management, and pure vector iconography.

---

## Key Features

- **Pure Material Design 3**: Built 100% with Jetpack Compose, dynamic color theming, and zero emoji clutter (uses clean, high-quality vector icons exclusively).
- **Advanced Xray JSON Support**: Native parsing for complex Xray configurations, including custom DNS servers, sockopt (`happyEyeballs`), sniffing, and stream settings (WS, gRPC, TCP, TLS, Reality).
- **Multi-Protocol Link Import**: Supports standard URI schemes (`vless://`, `vmess://`, `trojan://`, `ss://`, `socks5://`).
- **Subscription Management**: 
  - Folder-based subscription filtering with an **All** group view.
  - Automatic detection of subscription profile names or fallback to `import_sub`.
- **Fast Concurrent Ping & Sorting**:
  - Live TCP latency checks with color-coded badges.
  - Sort nodes by fastest ping, alphabetical order, or date added.
- **Smart Clipboard Import**: Auto-detects whether the clipboard content contains a raw JSON object/array, multi-line protocol links, or a subscription URL.
- **Android VpnService Integration**: Built-in VPN tunnel controller with live speed metrics and duration tracking.

---

## Screenshots & Architecture

```
CatNG
├── app/
│   ├── src/main/java/com/hnn/catng/
│   │   ├── data/          # Local storage and state repositories
│   │   ├── model/         # Xray JSON, ConfigItem, and Subscription data models
│   │   ├── parser/        # JSON & URI parser engines and EmojiCleaner
│   │   ├── ping/          # Concurrent TCP latency tester
│   │   ├── ui/
│   │   │   ├── dialogs/   # Material 3 dialogs (Manual Config, Subscriptions, Sort)
│   │   │   ├── main/      # MainScreen and Connection Controls
│   │   │   ├── theme/     # Material 3 Color Schemes & Typography
│   │   │   └── welcome/   # First-launch onboarding screen
│   │   └── vpn/           # Android VpnService tunnel implementation
```

---

## Building from Source

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 / 21
- Android SDK 35+

### Build Steps
```bash
# Clone the repository
git clone https://github.com/nikan48g/CatNG.git
cd CatNG

# Run tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

Output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## License

This project is licensed under the [MIT License](LICENSE).
