# Surf Shield: Kotlin Multiplatform Desktop Client

A reference VPN client built with **Compose for Desktop (Kotlin Multiplatform)**. This project demonstrates how to use the [Surf Shield Java SDK](https://surfshield.org/docs/desktop-java/) to build a cross-platform desktop VPN for Windows, macOS, and Linux from a single Kotlin codebase.

## Features
- A unified UI using Jetbrains Compose for Desktop.
- Cross-platform JNI extraction via `LeafWrapper`.
- Dynamic JNI artifact resolution at build-time using `com.google.osdetector`.
- Fetch subscriptions, update Geo assets, verify file integrity, and launch the Leaf proxy core.

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/shiroedev2024/kotlin-multiplatform-desktop.git
   ```
2. **Build and Run (Development):**
   ```bash
   ./gradlew run
   ```
3. **Package for Distribution (MSI, DMG, DEB):**
   ```bash
   ./gradlew packageDistributionForCurrentOS
   ```

## SDK Integration Highlight
This project automatically resolves the correct `jni-wrapper` binaries for the host OS at compile time, eliminating manual dependency linking:

```kotlin
val os = osdetector.os
val arch = osdetector.arch
val leafVersion = "1.2.8"

when {
    os == "linux" && arch == "x86_64" -> implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-linux-64")
    os == "osx" && arch == "aarch_64" -> implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-macos-arm64")
    // ... handles Windows and macOS variations
}
```

## Resources
* [Desktop Java SDK Documentation](https://surfshield.org/docs/desktop-java/)
* [REST API Reference](https://surfshield.org/docs/api/)
* [Support & Contact](https://surfshield.org/docs/support/)

## License
Open-sourced under the [Apache 2.0 License](LICENSE).