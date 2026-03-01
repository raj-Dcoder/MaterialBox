<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="120" alt="MaterialBox Logo"/>
</p>

<h1 align="center">MaterialBox</h1>

<p align="center">
  <strong>Your study materials, organized.</strong><br/>
  A modern Android app to store, organize, and access all your study materials in one place.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

---

## 📖 About

**MaterialBox** helps students organize their study materials with a clean, intuitive 3-tier hierarchy:

```
📚 Subject (e.g. "Data Structures")
  └── 📁 Topic (e.g. "Binary Trees")
        └── 📄 Material (PDFs, notes, links, images, docs)
```

No cloud dependency. No ads. Everything stays offline on your device.

---

## ✨ Features

| Feature | Description |
|---|---|
| 📂 **3-Tier Organization** | Subject → Topic → Material hierarchy for structured note management |
| 📄 **Multi-Format Support** | Store PDFs, DOCX, TXT, images, web links, and text notes |
| 📎 **File Upload** | Import documents directly from your device's file system |
| 🔗 **Link Saving** | Save web URLs as materials for quick reference |
| 📝 **Text Notes** | Create and edit text notes with in-app editor |
| 👁️ **View Tracking** | Track how many times you've accessed each material |
| 🔃 **Smart Sorting** | Toggle between "Recent" and "Most Viewed" materials on the home screen |
| 🗑️ **Cascade Deletion** | Deleting a subject removes all its topics and materials automatically |
| 🎨 **Modern UI** | Gen-Z inspired design with Poppins font, indigo/teal palette, and edge-to-edge display |
| 🌙 **Dark Mode** | Full dark mode support with Material You theming |
| 📱 **Predictive Back** | Smooth Android 14+ predictive back gesture support |

---

## 🏗️ Architecture

MaterialBox follows modern Android architecture principles:

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │ Screens  │  │Components│  │  Theme   │      │
│  │(Compose) │  │ (Cards)  │  │(M3+Custom)│     │
│  └────┬─────┘  └──────────┘  └──────────┘      │
│       │                                          │
│  ┌────▼─────────────────────┐                   │
│  │     ViewModels (Hilt)    │                   │
│  └────┬─────────────────────┘                   │
├───────┼─────────────────────────────────────────┤
│       │         Data Layer                       │
│  ┌────▼─────┐  ┌──────────┐  ┌──────────┐      │
│  │Repository│──│   DAOs   │──│   Room   │      │
│  └──────────┘  └──────────┘  │ Database │      │
│                               └──────────┘      │
└─────────────────────────────────────────────────┘
```

**Key patterns:**
- **MVVM** with Kotlin StateFlow for reactive UI
- **Repository pattern** for data abstraction
- **Dependency Injection** with Hilt
- **Single Activity** architecture with Compose Navigation
- **Offline-first** — all data stored locally with Room

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI** | Jetpack Compose + Material 3 |
| **Navigation** | Navigation Compose 2.9.0 |
| **Database** | Room 2.6.1 (SQLite) |
| **DI** | Hilt (Dagger) 2.50 |
| **Async** | Kotlin Coroutines + StateFlow |
| **Image Loading** | Coil 2.5.0 |
| **Camera** | CameraX 1.3.1 |
| **PDF** | PdfBox Android 2.0.27 |
| **Build** | Gradle (Kotlin DSL) |
| **Min SDK** | API 24 (Android 7.0) |
| **Target SDK** | API 35 (Android 15) |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 11** or higher
- **Android SDK** with API 35 installed

### Setup

```bash
# Clone the repository
git clone https://github.com/raj-Dcoder/MaterialBox.git

# Open in Android Studio
# File → Open → Select the MaterialBox folder

# Build the project
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Build Variants

| Variant | Command | Description |
|---|---|---|
| Debug | `./gradlew assembleDebug` | Development build with debugging enabled |
| Release | `./gradlew assembleRelease` | Optimized build with R8 minification |

---

## 📁 Project Structure

```
app/src/main/java/com/rajveer/materialbox/
├── data/
│   ├── entity/          # Room entities (Subject, Topic, Material)
│   ├── dao/             # Data Access Objects
│   ├── converter/       # Type converters (Date ↔ Long)
│   ├── repository/      # Repository layer
│   └── AppDatabase.kt   # Room database definition
├── di/
│   └── AppModule.kt      # Hilt dependency injection module
├── navigation/
│   ├── NavGraph.kt       # Navigation graph with transitions
│   └── NavRoutes.kt      # Route definitions
├── ui/
│   ├── components/       # Reusable composables (MaterialCard, SubjectCard, TopicCard)
│   ├── screens/          # Screen composables (Home, SubjectDetail, TopicDetail, etc.)
│   └── theme/            # Colors, Typography (Poppins), Theme
├── util/
│   └── DateUtils.kt      # Date formatting utilities
├── MainActivity.kt       # Single activity entry point
└── MaterialBoxApplication.kt  # Hilt application class
```

---

## 🎨 Design

MaterialBox features a custom Gen-Z inspired design system:

- **Font:** Poppins (Regular, Medium, SemiBold, Bold)
- **Primary:** Indigo/Purple `#5C6BC0`
- **Accent:** Teal `#00BFA5`
- **Tertiary:** Coral `#FF6B9D`
- **Material Type Colors:** Each file type (PDF, LINK, NOTE, etc.) has its own accent color for quick visual identification
- **Edge-to-edge** display with transparent status bar
- **Collapsible** large header on the home screen

---

## 🤝 Contributing

Contributions are welcome! Here's how:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/raj-Dcoder">Rajveer</a>
</p>
