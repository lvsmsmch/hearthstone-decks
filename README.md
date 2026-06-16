# Hearthstone Decks

Native Android app for browsing Hearthstone decks and cards.

The app shows Standard and Wild deck lists, deck details, card previews,
filters, favorites, and localized card/deck metadata. Deck and card data is
loaded from public Hearthstone community and Battle.net endpoints.

## Features

- Browse Standard and Wild Hearthstone decks.
- Filter decks by hero, game format, expansion, and search query.
- View full deck lists, card counts, dust cost, ratings, and source links.
- Preview card art and details.
- Save favorite decks locally.
- Copy deck codes and open deck source pages.
- Localized UI strings for multiple languages.

## Tech Stack

- Kotlin
- Android Views and Material Components
- Hilt
- Retrofit, OkHttp, and Gson
- Room
- Firebase Analytics, Messaging, Crashlytics, and Performance Monitoring
- Lottie

Firebase is optional for local builds. The app only applies the Google Services
and Crashlytics Gradle plugins when `app/google-services.json` exists.

## Requirements

- Android Studio
- JDK 17
- Android SDK 35
- Min SDK 26

## Build

Open the project in Android Studio and run the `app` configuration, or build from
the command line:

```bash
./gradlew assembleDebug
```

## Local Configuration

Battle.net OAuth credentials are read from `local.properties`:

```properties
BLIZZARD_CLIENT_ID=your-client-id
BLIZZARD_CLIENT_SECRET=your-client-secret
```

For Firebase builds, add your private `google-services.json` file to the `app/`
directory. Do not commit local credentials or release artifacts.

## Project Layout

```text
app/src/main/java/com/cyberquick/hearthstonedecks/
├── data/
│   ├── db/
│   ├── repository/
│   └── server/
├── di/
├── domain/
│   ├── entities/
│   ├── repositories/
│   └── usecases/
├── presentation/
│   ├── activities/
│   ├── adapters/
│   ├── dialogs/
│   ├── fragments/
│   └── viewmodels/
└── utils/
```

## Data Sources

- Battle.net Hearthstone API
- HearthPwn deck pages

This project is not affiliated with or endorsed by Blizzard Entertainment.
