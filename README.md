# Smart Dialer & Caller ID 🛡️📞

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-blue.svg?style=flat)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Storage-Room%20DB-orange.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> A modern, privacy-first, offline-capable **Smart Dialer, Real-Time Caller ID, and Community Spam Protection** application built with Kotlin and Jetpack Compose.
> 
> *শেখ ফরিদের [Smart-Dialer-Caller-ID](https://github.com/rsk934128-dot/Smart-Dialer-Caller-ID) প্রজেক্ট আর্কিটেকচারের ওপর ভিত্তি করে নির্মিত একটি আধুনিক, সময়োপযোগী এবং স্বয়ংসম্পূর্ণ অ্যান্ড্রয়েড ডায়ালার অ্যাপ।*

---

## 📱 Features (প্রধান সুবিধাসমূহ)

### 1. ⚡ Smart T9 Keypad & Instant Alphanumeric Search
- **Instant T9 Matching:** Search contacts as you type using standard telephone keypad mapping (`2=ABC`, `3=DEF`, etc.) or direct number substrings.
- **Haptic & DTMF Sound:** Tactile vibration and realistic DTMF dialing tones.
- **Quick Actions:** One-tap call, SMS, add new contact, and hide/show keypad toggle.

### 2. 🛡️ Real-Time Caller ID & Live Ringing Overlay
- **Live Incoming Call HUD:** Floating Caller ID card displaying caller identity, photo avatar, and verification badge.
- **Community Spam Alerts:** Identifies reported fraudsters (`Fake Bank KYC Agent`, `Crypto Scam`, `Unsolicited Telemarketing`).
- **Interactive Call Controls:** Accept call, decline, send quick SMS templates, or instant **"Block & Report"**.
- **Interactive Call Simulation:** Built-in test buttons (**Verified Call** & **Spam Call**) to experience live Caller ID popups right in the app.

### 3. 🇧🇩 Telecom Operator & Regional Prefix Engine (`CarrierLookupEngine`)
- Automatically detects Bangladesh operators based on phone prefix:
  - `017` / `013` ➜ **Grameenphone (GP 4G/5G)**
  - `019` / `014` ➜ **Banglalink**
  - `018` ➜ **Robi Axiata**
  - `016` ➜ **Airtel Bangladesh**
  - `015` ➜ **Teletalk**
  - `02` ➜ **BTCL Landline (Dhaka)**
- Supports international dialing codes (`+880`, `+1`, `+44`, `+91`, `+971`, etc.).

### 4. 🚫 Spam Shield & Community Registry
- **Live Number Reputation Checker:** Search any unknown number to see fraud reports, risk percentage, and category.
- **Auto-Block Engine:** Toggle auto-rejection for high-risk fraud callers and private/masked numbers.
- **Custom Blocklist:** Easily block or unblock any number with custom reasons.
- **Report Spam:** Submit new suspicious numbers directly into the local/community registry.

### 5. 📊 Call Telemetry & Analytics Dashboard
- Comprehensive metrics: Total calls, cumulative talk time (hours/minutes), missed calls, and blocked spam count.
- **Visual Ratio Meter:** Interactive bar showing Incoming vs. Outgoing call percentage.
- **Top Frequent Callers:** Rank list of most frequent contacts with call counts and duration.

### 6. 🌐 Bilingual Support (বাংলা ও English)
- Seamless one-tap toggle on the top app bar to switch between English and বাংলা (Bengali).

---

## 🏗️ Architecture & Tech Stack

The project adheres to **Modern Android Architecture (MVVM + Clean Architecture)**:

```
app/src/main/java/com/example/
├── data/
│   ├── db/                 # Room Database, AppDao, TypeConverters
│   ├── model/              # Entities: ContactEntity, CallLogEntity, SpamReportEntity, BlockedNumberEntity
│   └── repository/         # DialerRepository (Room + Device Provider Sync)
├── util/
│   ├── CarrierLookupEngine.kt   # Operator prefix detection & formatting
│   └── T9SearchEngine.kt        # Fast T9 alphanumeric contact matcher
├── ui/
│   ├── components/         # KeypadMatrix, CallLogCard, CallerIdOverlay, ActiveCallDialog
│   ├── screens/            # DialerScreen, RecentsScreen, ContactsScreen, SpamShieldScreen, AnalyticsScreen
│   ├── theme/              # Material 3 ColorScheme, Typography, Theme
│   └── viewmodel/          # DialerViewModel (StateFlow & Coroutines)
└── MainActivity.kt         # Edge-to-Edge Navigation Scaffold
```

- **UI:** Jetpack Compose + Material 3 (M3)
- **Architecture:** Model-View-ViewModel (MVVM)
- **Concurrency:** Kotlin Coroutines & StateFlow
- **Database:** Room Database (SQLite ORM) with KSP
- **CI/CD:** GitHub Actions automated build & release workflow

---

## 🚀 How to Run & Build Locally

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 or higher
- Android SDK 36 (Min SDK 24)

### Clone the Repository
```bash
git clone https://github.com/rsk934128-dot/Smart-Dialer-Caller-ID.git
cd Smart-Dialer-Caller-ID
```

### Build Debug APK
```bash
# On Linux / macOS
gradle assembleDebug

# On Windows
gradle assembleDebug
```
The generated APK will be available at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

---

## 📦 How to Publish a Release on GitHub

1. **Tag your release commit:**
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0: Initial production release of Smart Dialer & Caller ID"
   ```

2. **Push tag to GitHub:**
   ```bash
   git push origin main --tags
   ```

3. **Automatic CI/CD:**
   The included GitHub Actions workflow (`.github/workflows/android-release.yml`) will automatically:
   - Run tests
   - Build the APK
   - Create a GitHub Release with download assets attached!

---

## 🔒 Permissions & Privacy
This application is designed **Local-First & Privacy-Focused**:
- `READ_PHONE_STATE` & `CALL_PHONE`: To place calls and manage active call state.
- `READ_CALL_LOG` & `WRITE_CALL_LOG`: To display and record call history locally.
- `READ_CONTACTS`: To display your phonebook contacts and perform T9 matching on-device.
- *Your call logs and contacts are never sent to external unverified servers without user consent.*

---

## 👥 Credits & Acknowledgments
- Inspired by **Sheikh Farid's** vision and repository [Smart-Dialer-Caller-ID](https://github.com/rsk934128-dot/Smart-Dialer-Caller-ID).
- Built with Google Jetpack Compose & Material 3.

---

## 📄 License
This project is open-source under the [Apache License 2.0](LICENSE).
