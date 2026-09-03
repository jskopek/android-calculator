# Calculator

A simple, no-nonsense Android calculator built with Kotlin and Jetpack Compose.

## Features (v0.1)

- **Basic arithmetic** with correct precedence (`12 + 7 × 3 = 33`), percent, sign toggle,
  backspace, and exact decimal math (`0.1 + 0.2 = 0.3`).
- **Live preview** of the result while you type.
- **Ticker-tape history**: every calculation is kept with its full expression and result,
  newest at the bottom, and survives app restarts. Tap a line to reuse its result.
- **Light / dark / system theme**, with Material You dynamic colours on Android 12+.
- **Unit converter** (length, area, temperature, volume, mass, data, speed, time) with a
  tap-to-edit from/to pair and, on wide screens, the value in every unit of the category.
- **Folding-phone friendly**: on the cover screen (or any narrow window) history lives in a
  bottom sheet; unfold and the tape sits beside the keypad. The keypad scales to whatever
  space it has, and state is kept across fold/unfold and rotation.

## Building

Open the project in Android Studio (Ladybug or newer) and run the `app` configuration, or from
the command line:

```sh
./gradlew :app:assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # engine unit tests
```

Requirements: JDK 17+, Android SDK with platform 35. Minimum device: Android 8.0 (API 26).

## Installing updates on a phone

Every push builds a signed release APK and publishes it as a GitHub Release tagged
`v0.1.<build number>`. The easiest way to keep a phone up to date is
[Obtainium](https://github.com/ImranR98/Obtainium): add this repository's URL as an app and it
will notify you when a new release appears and install it with one tap.

Signing needs four repository secrets (Settings → Secrets and variables → Actions):

| Secret              | Value                                   |
|---------------------|-----------------------------------------|
| `KEYSTORE_BASE64`   | the release keystore, base64 encoded    |
| `KEYSTORE_PASSWORD` | keystore password                       |
| `KEY_ALIAS`         | key alias inside the keystore           |
| `KEY_PASSWORD`      | key password                            |

Keep the keystore safe: an APK signed with a different key cannot update an installed app.
Local `assembleRelease` builds without these variables fall back to the debug key.

## Project layout

```
app/src/main/java/ca/skopek/calculator/
├── engine/        Pure Kotlin calculator logic (no Android dependencies, unit tested)
│   ├── Token.kt             Tokens, keys, and the immutable CalculatorState
│   ├── CalculatorEngine.kt  Key handling: what each key does to the state
│   ├── Evaluator.kt         Precedence-aware BigDecimal evaluation
│   ├── NumberFormatter.kt   Locale-aware display formatting
│   └── units/               Unit categories, factors, and the converter
├── data/          History persistence (JSON file) and settings (SharedPreferences)
├── ui/            Compose screens: adaptive layout, display, keypad, history tape, converter
├── CalculatorViewModel.kt   Glues engine, history and settings together
└── ConverterViewModel.kt    State for the unit converter
```

## Ideas for later

- Currency conversion with live rates (a category built at runtime from fetched rates)
- Scientific mode
- Tabletop / half-folded posture layout using Jetpack WindowManager
- Hardware keyboard input
- Editing anywhere in the expression (cursor), parentheses
