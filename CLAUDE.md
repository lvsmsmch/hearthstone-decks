# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & run

Single-module Gradle (Groovy DSL) Android project. Use the wrapper:

- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- Build release bundle: `./gradlew bundleRelease` (output lands at `app/release/app-release.aab` — already tracked in git)
- Install on connected device: `./gradlew installDebug`
- Clean: `./gradlew clean`
- Lint: `./gradlew lint`

There is no test source set (`app/src/test`, `app/src/androidTest` do not exist). Don't add tests unless asked.

Toolchain pins: Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0, Hilt 2.56.2, JVM/Java target 17, `compileSdk`/`targetSdk` 35, `minSdk` 26. Bumping any of these is a deliberate change — do not "modernize" them as a side effect.

All library and plugin coordinates live in a Gradle version catalog at `gradle/libs.versions.toml`. `app/build.gradle` and the root `build.gradle` reference the catalog via `libs.<alias>` / `alias(libs.plugins.<id>)` — when adding or bumping a dependency, edit the TOML rather than hard-coding versions in the Groovy build files.

`buildFeatures.buildConfig true` is required (AGP 8 made `BuildConfig` generation opt-in); the codebase reads `BuildConfig.VERSION_NAME` and `BuildConfig.DEBUG`, so don't drop it.

The packaging block uses the new `packaging { resources { … } }` DSL (the AGP 8.0+ replacement for `packagingOptions`). Don't revert it.

`onNewIntent(intent: Intent)` (non-null) is required because `targetSdk` is 35 — the older nullable signature stopped overriding in API 35.

kapt is still used for Hilt and Room. The Kotlin 2.0+ kapt support is in alpha; the build prints a warning that it falls back to language version 1.9 for stub generation. That is expected — do not flip `kapt.use.k2` on without checking that Hilt + Room still process annotations cleanly.

`google-services.json` is committed at `app/google-services.json` (Firebase Analytics/Messaging/Crashlytics/Performance are wired in). When bumping `versionName`, also bump `versionCode` in `app/build.gradle`.

## Architecture

Clean-architecture layout under `app/src/main/java/com/cyberquick/hearthstonedecks/`:

- `domain/` — pure Kotlin: entities, `repositories/` (interfaces only), `usecases/` (each `invoke()` returns `Result<T>`), and the `common/Result.kt` sealed type (`Success` / `Error`) used everywhere.
- `data/` — implementations: `repository/*Impl.kt` bind to domain repository interfaces; `db/` is Room (`RoomDB`, `DeckDao`, `DeckEntity`, mappers); `server/` has two distinct backends (see below).
- `presentation/` — single-Activity (`MainActivity`) hosting fragments via `supportFragmentManager`; `viewmodels/`, `adapters/`, `dialogs/`, `fragments/`. The drawer in `MainActivity` swaps top-level fragments (`OnlineStandardPageFragment`, `OnlineWildPageFragment`, `FavoritePageFragment`, `AboutAppFragment`).
- `di/DataModule.kt` — the only Hilt module. Provides `RoomDB`, the two Retrofit APIs (`BLIZZARD_API_URL` and `BLIZZARD_OAUTH_URL`), and binds `*Impl` → repository interfaces. Add new bindings here.
- `App.kt` — `@HiltAndroidApp`, holds a `Preferences` instance.

### Two data sources for online decks (important)

`OnlineDecksImpl` composes two unrelated remote sources:

1. **`HearthpwnApi`** — *not* Retrofit. Scrapes `https://www.hearthpwn.com` HTML with Jsoup to produce `Page`/`DeckPreview` listings and per-deck description + deck code. CSS selectors are hard-coded and brittle; if listings break, hearthpwn changed its DOM. URL templates and selectors live at the top of `HearthpwnApi.kt`.
2. **`BattleNetRepository`** — Retrofit against Blizzard's official API. Used to expand a deck code into actual `Card` data. Handles OAuth: caches `currentToken`, retries once on failure by re-fetching a token. Region is hard-coded to `"eu"` in `getUserRegion()`; locale is mapped from `Locale.getDefault()`.

`OnlineDecksImpl.getDeck` chains them: hearthpwn → deck code + description, then battle.net → cards.

`SetsImpl` is hybrid: ships local JSON (`res/raw/sets`, `res/raw/set_groups`) as a fallback and refreshes from battle.net on app start (`MainActivity.doOtherStuff()`).

### Result / LoadingState pattern

Anything async returns `domain.common.Result<T>` (`Success(data)` | `Error(exception)`). ViewModels expose `LiveData<LoadingState<T>>` (`Loading` / `Loaded` / `Failed`) and use `BaseViewModel.makeLoadingRequest { ... }`, which:

- short-circuits if already loading (unless `allowInterrupt = true`),
- adds a synthetic delay on errors when the response was suspiciously fast (`delayIfExecutionTimeIsSmall`) so failures don't feel like a no-op tap,
- registers the coroutine `Job` so `onCleared()` cancels everything.

Stick to this pattern for new async flows rather than exposing raw coroutines/`Result`.

### Pagination

`PageViewModel` (and its three Hilt subclasses `OnlineStandardPageViewModel`, `OnlineWildPageViewModel`, `FavoritePageViewModel`) is parameterized by a `GetPageUseCase` and tracks `Position(current, total)` + `AllowNavigation`. Filter changes call `updateCurrentPage(evenIfLoaded = true)` to force reload.

### Room schema

`RoomDB` is at version 2 with an `AutoMigration` spec that drops the old `cards` and `deck_ids_to_card_ids` tables and the `description`/`code` columns from `decks` (cards/deck-code now come from the network on demand). Schemas are exported to `app/schemas/` (configured via `room.schemaLocation`). Any schema change needs a migration; `fallbackToDestructiveMigration()` is intentionally commented out.

## Conventions worth respecting

- Hilt is the only DI; use `@Inject constructor` and add `@Provides` to `DataModule` for things you can't construct directly.
- View binding is enabled (`buildFeatures.viewBinding true`); fragments use generated `FragmentXxxBinding`. Don't pull in Compose or DataBinding.
- Logging uses `android.util.Log` with ad-hoc tags (`tag_network`, `tag_sets`, `tag_api`, …) — match the surrounding style rather than introducing Timber.
- User-facing strings live in `app/src/main/res/values*/strings.xml` (many translations). Don't hard-code UI text in Kotlin.
- Firebase event names are centralized in `utils/FirebaseEvents.kt` (`Event` enum) — log via `logFirebaseEvent(context, Event.X)`.
