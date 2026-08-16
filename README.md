# AppLock Guard — Android App Lock Application

A native Android app lock application built with **Kotlin** and **Jetpack Compose** that protects your apps with PIN, Pattern, or Fingerprint authentication.

## 📱 Features

- **Master PIN Lock** — Set a 4–6 digit PIN code
- **Pattern Lock** — Draw a 3×3 pattern to secure your apps
- **Biometric Authentication** — Fingerprint unlock support
- **App Locking** — Select and lock any installed app
- **Background Monitoring** — Foreground service detects when locked apps are opened
- **Auto-start on Boot** — Protection resumes after device restart
- **Failed Attempt Protection** — Locks out after 5 wrong attempts (30s cooldown)
- **Re-lock Timeout** — Configurable time before re-locking an unlocked app
- **Premium Dark UI** — Sleek dark theme with gradient accents

---

## 🏗️ Project Architecture

```
MVVM Architecture
├── Data Layer    → Room DB, EncryptedSharedPreferences, Repository
├── Service Layer → Foreground Service (UsageStatsManager), BootReceiver
├── UI Layer      → Jetpack Compose Screens, ViewModels, Custom Components
└── Utilities     → Biometric, Crypto, Permission helpers
```

---

## 📋 Prerequisites

Before building, make sure you have:

1. **Android Studio** Hedgehog (2023.1.1) or newer
   - Download: https://developer.android.com/studio
2. **JDK 17** (bundled with Android Studio)
3. **Android SDK** with:
   - API Level 34 (Android 14) SDK Platform
   - Build Tools 34.x
4. **A physical Android device** (recommended) or emulator (API 26+)

---

## 🚀 Step-by-Step Build Instructions

### Step 1: Open the Project

1. Launch **Android Studio**
2. Click **File → Open**
3. Navigate to the `LOCK` folder and click **OK**
4. Wait for Gradle sync to complete (first sync may take several minutes)

### Step 2: Build Debug APK (Free, No Signing Needed)

#### Option A: Using Android Studio
1. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for the build to complete
3. Click **"locate"** in the notification to find the APK
4. The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

#### Option B: Using Command Line
```bash
# On Windows
gradlew.bat assembleDebug

# On macOS/Linux
./gradlew assembleDebug
```

The debug APK is at: `app/build/outputs/apk/debug/app-debug.apk`

### Step 3: Install on Device

#### Via USB (ADB)
```bash
# Connect device via USB with USB Debugging enabled
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Via File Transfer
1. Copy the `.apk` file to your phone
2. Open the file on your phone
3. Enable "Install from Unknown Sources" if prompted
4. Tap **Install**

---

## 📦 Build Release APK (Signed)

### Step 1: Generate a Signing Key

```bash
keytool -genkey -v -keystore applock-release.keystore -alias applock -keyalg RSA -keysize 2048 -validity 10000
```

### Step 2: Configure Signing in `app/build.gradle.kts`

Add this inside the `android { }` block:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../applock-release.keystore")
        storePassword = "your_store_password"
        keyAlias = "applock"
        keyPassword = "your_key_password"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... existing config
    }
}
```

### Step 3: Build Release APK

```bash
# Windows
gradlew.bat assembleRelease

# macOS/Linux
./gradlew assembleRelease
```

The signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

---

## ⚙️ Required Permissions

After installing, the app will ask for these permissions:

| Permission | Why It's Needed |
|-----------|----------------|
| **Usage Access** | To detect which app is running in the foreground |
| **Display Over Other Apps** | To show the lock screen over locked apps |
| **Notifications** | To show the "protection active" notification |
| **Boot Completed** | To restart protection after device reboot |

---

## 🔒 How It Works

1. **Setup**: User sets a master PIN or draws a pattern on first launch
2. **App Selection**: User selects which apps to protect
3. **Monitoring**: A foreground service polls `UsageStatsManager` every 500ms
4. **Lock Trigger**: When a locked app is detected in the foreground, the lock overlay appears
5. **Unlock**: User authenticates via PIN, Pattern, or Fingerprint
6. **Re-lock**: After the configured timeout, the app is re-locked

---

## 🧪 Testing

```bash
# Run unit tests
gradlew.bat test

# Run instrumented tests
gradlew.bat connectedAndroidTest
```

---

## 📁 Project Structure

```
LOCK/
├── app/
│   ├── src/main/
│   │   ├── java/com/applock/guard/
│   │   │   ├── AppLockApplication.kt       # App initialization
│   │   │   ├── MainActivity.kt              # Entry point + navigation
│   │   │   ├── data/                        # Database, preferences, repository
│   │   │   ├── service/                     # Foreground service, boot receiver
│   │   │   ├── ui/                          # Compose screens + components
│   │   │   └── util/                        # Biometric, crypto, permission helpers
│   │   ├── res/                             # Resources (strings, themes)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts                         # Project config
├── settings.gradle.kts
├── gradle.properties
└── README.md                                # This file
```

---

## 📝 License

This project is provided as-is for educational purposes. Feel free to modify and distribute.
