# LOOPSHIFT

**Find the path. Hold the flow.**

LOOPSHIFT is a fast, tactile ring-routing puzzle game for Android by **Kartik Labs**.

## Current playable v1

- Four concentric routing rings
- Tap any ring to rotate its gate
- Energy node advances inward on a timed pulse
- Missed alignment costs a life
- Score + combo / FLOW system
- Speed increases as score rises
- Pause, restart and game-over flow
- Local best score persistence
- Haptic feedback
- Portrait-first dark neon UI
- Android 16 / API 36 target
- GitHub Actions debug APK build

## Controls

Tap directly on a ring. Each tap rotates that ring's gate one sector clockwise. Align the active node with the glowing gate before the next pulse so it can move toward the core.

Reach the core repeatedly to build a combo. At **FLOW x5** and above, successful routes award stronger bonuses.

## Stack

- Kotlin
- Jetpack Compose
- Compose Canvas
- Native Android haptics
- SharedPreferences for v1 best-score persistence

## Build

Open the repository in Android Studio and run the `app` configuration, or use Gradle 8.13 with JDK 17:

```bash
gradle assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Roadmap

Next releases can add Rush / Zen / Daily Shift modes, audio layers, animated particles, themes, achievements, Play Games leaderboards, onboarding, difficulty balancing and signed Play Store release automation.
