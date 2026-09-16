# ProSett Firewall & Guard

ProSett is a 100% on-device local firewall and application manager for Android. It provides granular control over which applications can access Wi-Fi, Cellular Data, and execute background processes, ensuring maximum privacy and battery life without relying on external telemetry or cloud servers.

## Features

* **On-Device Firewall:** Uses Android's native `VpnService` to locally manage traffic, blocking connections based on your preferences without requiring root access.
* **Granular Network Control:** Toggle Wi-Fi and Mobile Data access individually for every installed application.
* **Background Process Management:** Easily restrict and stop applications from running in the background.
* **Batch Operations:** Quickly cut or allow Wi-Fi and Cellular data globally with quick-action rules.
* **Theming Engine:** Choose between System Default, Light, Standard Dark, and Deep Tinted Dark modes for a personalized experience.
* **Privacy First:** All rules, application statuses, and metrics are stored locally on your device using a Room database.

## Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material Design 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Data Persistence:** Room Database
* **Async/Concurrency:** Kotlin Coroutines & Flow
* **Core API:** Android VpnService

## Building the Project

This project uses Gradle and includes a GitHub Actions workflow for Continuous Integration.

To build the application locally:

```bash
# Grant execute permission to the Gradle wrapper
chmod +x gradlew

# Build the project
./gradlew build
```

## Continuous Integration

The repository includes a GitHub Action workflow (`.github/workflows/android.yml`) that automatically triggers a build on pushes and pull requests to the `main` branch to ensure code integrity.
