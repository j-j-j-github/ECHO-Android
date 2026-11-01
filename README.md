<div align="center">

  <img src="docs/images/logo.png" alt="ECHO Logo" width="120" style="border-radius: 24px; box-shadow: 0 8px 24px rgba(0,0,0,0.5);"/>

  # E C H O
  
  **A fleeting space for the things we carry.**  
  *No accounts. No history. No judgment. Every thought fades after 48 hours.*

  <br/>

  [![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![Supabase](https://img.shields.io/badge/Backend-Supabase%20Realtime-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com)
  [![Firebase](https://img.shields.io/badge/Cloud-Firebase%20FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
  [![Refresh Rate](https://img.shields.io/badge/Display-120Hz%20Unlocked-00E5FF?style=for-the-badge)](#)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Table of Contents

- [Vision & Philosophy](#-vision--philosophy)
- [Visual Showcase](#-visual-showcase)
  - [Where to Store Screenshots](#-where-to-store-screenshots)
- [Core Features](#-core-features)
- [Architecture & Tech Stack](#-architecture--tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started & Local Setup](#-getting-started--local-setup)
- [Supabase Backend Architecture](#-supabase-backend-architecture)
- [Privacy & Security by Design](#-privacy--security-by-design)
- [Contributing](#-contributing)
- [Creator & Contact](#-creator--contact)

---

## 🌌 Vision & Philosophy

Modern social platforms are engineered for permanence, surveillance, and performance anxiety. Every opinion is archived indefinitely, tied to a permanent identity, and quantified through algorithmic vanity loops.

**ECHO** is built as an antidote to modern digital noise:

- **Ephemeral Life Cycle (48 Hours):** Nothing in the Void lasts forever. Every whisper, confession, and reply automatically dissolves into silence after 48 hours.
- **Absolute Anonymity (Zero Account Lock-in):** No email sign-ups, no passwords, no phone number verifications, and no user profiles. Your presence is backed by an ephemeral device-bound cryptographic **Soul Signature**.
- **Tactile Emotional Tuning:** Instead of an engagement-driven algorithmic feed, users manually tune their internal frequency to match their current emotional state.
- **Pure Resonance:** There are no follower counts or public social graphs. You resonate with thoughts, exchange anonymous replies, and let go.

---

## 📸 Visual Showcase

> [!TIP]
> **Screenshot Directory:** Store all preview images inside [`docs/images/`](docs/images/).

| **The Void Feed** | **Whisper Composer** | **Frequency Tuner** |
| :---: | :---: | :---: |
| <img src="docs/images/feed.png" alt="The Void Feed" width="240"/> | <img src="docs/images/whisper.png" alt="Whisper Composer" width="240"/> | <img src="docs/images/frequency.png" alt="Frequency Tuner" width="240"/> |
| *Pitch-black 120Hz infinite stream* | *Minimalist thought broadcaster* | *Tactile haptic mood slider* |

| **Soul Energy & Gyroscope** | **Spotlight Mode** | **Emotional Journey** |
| :---: | :---: | :---: |
| <img src="docs/images/soul_energy.png" alt="Soul Energy Sphere" width="240"/> | <img src="docs/images/spotlight.png" alt="Spotlight Post" width="240"/> | <img src="docs/images/journey.png" alt="Emotional Journey" width="240"/> |
| *Real-time 3D gravity-particle sphere* | *Illuminated high-energy card* | *Mood heatmaps & telemetry* |

### 📂 Where to Store Screenshots

To display your screenshots in this README:
1. Capture screenshots of the app from your device or emulator.
2. Name the image files according to the table above:
   - `docs/images/feed.png`
   - `docs/images/whisper.png`
   - `docs/images/frequency.png`
   - `docs/images/soul_energy.png`
   - `docs/images/spotlight.png`
   - `docs/images/journey.png`
   - `docs/images/notifications.png`
3. Place them directly in the `docs/images/` directory in the repository root.

---

## ✨ Core Features

### 1. 🎛️ Tactile Frequency Tuner
- A custom hardware-feel rotary dial rendered directly in Compose Canvas.
- Offers calibrated haptic ticks on every graduation.
- Filters the Void into five emotional wavelengths:
  - 🔴 **DISTURBED** (`#FFFF453A`)
  - 🟠 **HEAVY** (`#FFFF9F0A`)
  - ⚪ **NEUTRAL** (`#94A3B8`)
  - 🔵 **CALM** (`#64D2FF`)
  - 🟢 **RADIANT** (`#34C759`)

### 2. 🌀 3D Gravity Gyroscope & Soul Energy
- Integrated with Android `SensorManager` and `Sensor.TYPE_ACCELEROMETER`.
- Renders an interactive 3D particle sphere using real-time trigonometric math and gravity vector physics.
- Tilting your physical device shifts the liquid surface and orbital particle halos in real-time.
- Energy is charged through daily check-ins and meaningful resonances.

### 3. 💡 The Spotlight System
- When your **Soul Energy** reaches 100%, you can activate **Spotlight Mode** on your next Echo.
- Spotlight converts the post into an inverted, luminous card featuring a dynamic chromatic aurora border, guaranteeing visibility in the dark feed.

### 4. 🔑 Cryptographic Soul Signatures & Device Portability
- Users don't register accounts; an anonymous, deterministic UUID signature is generated on first launch.
- Users can export their **Soul Key** to transfer their karma, energy level, and journey stats to a new device or browser without revealing their personal identity.

### 5. 🪐 Themed Spaces
- Categorize whispers into distinct mental spaces:
  - `Inner Thoughts`
  - `Vent`
  - `Advice`
  - `Relationships`
  - `All (Collective Consciousness)`

### 6. 📊 Emotional Journey & Telemetry
- A privacy-preserving personal telemetry dashboard.
- Displays monthly mood calendars, dominant mood calculations, activity streaks, and lifetime resonance statistics.

### 7. 📡 Offline-First Architecture & WorkManager
- Fully resilient against intermittent network connections.
- Whispers and resonances created offline are queued in local cache and seamlessly synced when connectivity is restored via AndroidX `WorkManager` (`EchoWorker`).
- Optimistic UI updates ensure instantaneous UI response for relates and un-relates.

### 8. 🔔 The Secret Log & Deep Linking
- Real-time notifications powered by Firebase Cloud Messaging (FCM) synced to Supabase installation records.
- Tapping on a resonance notification directly deep-links the user into the relevant Echo thread with a fluid entry transition.

### 9. ⚡ Hardware Display Optimization (120Hz)
- Explicit display mode negotiation via `Window.attributes.preferredDisplayModeId` unlocks native 90Hz / 120Hz panels for maximum fluid motion.

---

## 🛠️ Architecture & Tech Stack

ECHO is engineered according to **Clean Architecture** and **Modern Android Development (MAD)** standards:

```
┌────────────────────────────────────────────────────────┐
│               PRESENTATION LAYER (UI)                  │
│   Jetpack Compose • Material 3 • Navigation Compose    │
│   Custom Canvas Graphics • Sensors • ExoPlayer         │
└───────────────────────────▲────────────────────────────┘
                            │ (StateFlow / UI Events)
┌───────────────────────────┴────────────────────────────┐
│                    DOMAIN LAYER                        │
│   Use Cases • Repository Interfaces • Domain Models    │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                     DATA LAYER                         │
│   Supabase PostgREST & Realtime • Firebase Messaging   │
│   AndroidX WorkManager • DataStore & SharedPreferences │
└────────────────────────────────────────────────────────┘
```

| Component | Technology | Description |
|---|---|---|
| **Language** | Kotlin 2.0+ | Coroutines, Flow, StateFlow, Serialization |
| **UI Framework** | Jetpack Compose (BOM 2024+) | Declarative UI, Animations, Edge-to-Edge |
| **Design System** | Material 3 + Custom Void Theme | Pure OLED black palette, custom Serif typography |
| **Dependency Injection** | Dagger Hilt 2.51+ | Singleton & ViewModel injection, Hilt WorkerFactory |
| **Backend & Database** | Supabase (Kotlin SDK) | PostgREST, Realtime listeners, custom Postgres RPCs |
| **Push Notifications** | Firebase Cloud Messaging (FCM) | Background and foreground alert delivery |
| **Background Sync** | AndroidX WorkManager | Periodic 15-minute background synchronization |
| **Media & Audio** | AndroidX Media3 ExoPlayer | High-performance looping video onboarding |
| **Sensors** | Android Sensor Framework | Accelerometer-based physics simulation |
| **Local Storage** | SharedPreferences & DataStore | Offline queues, installation tokens, cached preferences |
| **Google Play APIs** | Play In-App Review & App Update | Seamless in-app feedback and flexible updates |

---

## 📁 Project Structure

```
com.example.echo/
├── EchoApp.kt                         # Application entrypoint & WorkManager setup
├── core/
│   ├── di/
│   │   ├── NetworkModule.kt           # Supabase client & signature injection
│   │   └── RepositoryModule.kt        # Repository interface binding
│   ├── theme/
│   │   ├── Color.kt                   # Void palettes & chromatic gradients
│   │   ├── Theme.kt                   # Dynamic system theme provider
│   │   └── Type.kt                    # EB Garamond serif typography
│   └── utils/
│       ├── NetworkMonitor.kt          # Real-time network connectivity observer
│       ├── SharedPreferencesManager.kt# Offline queue, read states & preferences
│       ├── TimeUtils.kt               # 48-hour countdown math & formatting
│       └── UserPreferences.kt         # Soul signature generator
├── data/
│   ├── remote/dto/                    # Data Transfer Objects (Echo, Reply, Stats)
│   └── repository/
│       └── EchoRepositoryImpl.kt      # Supabase PostgREST queries, RPCs & offline sync
├── domain/
│   └── repository/
│       └── EchoRepository.kt          # Core domain repository contract
└── presentation/
    ├── MainActivity.kt                # 120Hz enabler, FCM listener & Navigation
    ├── feed/
    │   ├── VoidScreen.kt              # Infinite Void feed, animations & gestures
    │   ├── VoidViewModel.kt           # Feed state, optimistic updates & mood filter
    │   └── DetailOverlay.kt           # Echo inspection & thread reply composer
    ├── post/
    │   └── WhisperScreen.kt           # Fullscreen thought composer & spotlight toggle
    ├── components/
    │   ├── FrequencyModal.kt          # Canvas-rendered mechanical dial
    │   ├── SoulEnergy.kt              # 3D Gyroscope-driven particle sphere
    │   ├── JourneyOverlay.kt          # Mood calendar, streaks & identity console
    │   ├── NotificationPanel.kt       # "The Secret Log" drawer
    │   ├── GetStartedDialog.kt        # ExoPlayer looping video onboarding
    │   └── AboutOverlay.kt            # Manifesto, privacy directives & mission
    ├── notification/
    │   ├── EchoFirebaseMessagingService.kt # FCM token updates & notification dispatcher
    │   └── EchoWorker.kt              # WorkManager background task for offline queue
    └── splash/
        └── SplashScreen.kt            # Ambient launch screen transition
```

---

## 🚀 Getting Started & Local Setup

### Prerequisites
- **Android Studio** Ladybug (2024.2.1+) or newer
- **JDK 17** or **JDK 21**
- **Android SDK Platform 35** (compileSdk 35, minSdk 26)

### 1. Clone the Repository
```bash
git clone https://github.com/j-j-j-github/ECHO-Android.git
cd ECHO-Android
```

### 2. Configure Local Properties (`local.properties`)
Create a `local.properties` file in the project root (or copy from the provided template):
```bash
cp local.properties.example local.properties
```

Add your Android SDK directory and Supabase credentials:
```properties
sdk.dir=/Users/your-username/Library/Android/sdk

# Supabase Credentials
SUPABASE_URL="https://your-project-ref.supabase.co"
SUPABASE_KEY="your-supabase-anon-key"
```

### 3. Configure Firebase (`google-services.json`)
ECHO uses Firebase for Push Notifications (FCM).

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.j3labs.echo`.
3. Download the `google-services.json` file.
4. Place it in the `app/` folder:
   ```bash
   cp /path/to/downloaded/google-services.json app/google-services.json
   ```
*(A reference structure is available in [`app/google-services.json.example`](app/google-services.json.example).)*

### 4. Build and Run
Open the project in Android Studio, allow Gradle to sync dependencies, and run on a physical device or emulator:
```bash
./gradlew assembleDebug
```

---

## 🗄️ Supabase Backend Architecture

ECHO uses Supabase with PostgreSQL. To set up your own database instance, run the following SQL schema:

<details>
<summary><b>Click to expand PostgreSQL Schema & RPCs</b></summary>

```sql
-- 1. Create Echoes table (expires in 48 hours)
create table public.echoes (
    id uuid default gen_random_uuid() primary key,
    content text not null,
    category text default 'Inner Thoughts',
    mood int default 2,
    color_code text default null,
    signature_id text not null,
    likes int default 0,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 2. Create Replies table
create table public.replies (
    id uuid default gen_random_uuid() primary key,
    echo_id uuid references public.echoes(id) on delete cascade not null,
    content text not null,
    signature_id text not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 3. Create Relates Log table
create table public.relates_log (
    id uuid default gen_random_uuid() primary key,
    echo_id uuid references public.echoes(id) on delete cascade not null,
    sender_signature_id text not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique(echo_id, sender_signature_id)
);

-- 4. Create User Stats & Karma table
create table public.user_stats (
    signature_id text primary key,
    energy int default 0,
    daily_resonances_counted int default 0,
    total_relates_received int default 0,
    total_echoes_posted int default 0,
    display_name text default null,
    last_login_date date default current_date
);

-- 5. Create FCM Tokens table
create table public.fcm_tokens (
    installation_id text primary key,
    fcm_token text not null,
    updated_at timestamp with time zone default now()
);

-- 6. RPC Function: Fetch active echoes within 48-hour lifecycle
create or replace function get_echoes_for_user(requesting_signature text)
returns setof echoes as $$
begin
    return query
    select * from public.echoes
    where created_at >= (now() - interval '48 hours')
    order by created_at desc
    limit 200;
end;
$$ language plpgsql security definer;
```
</details>

---

## 🛡️ Privacy & Security by Design

- **Zero Advertising Trackers:** The Google Advertising ID permission is explicitly removed:
  ```xml
  <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>
  ```
- **Zero Third-Party Ad Networks:** No AdMob, Meta Audience Network, or data broker SDKs.
- **No PII Stored:** No phone numbers, real names, GPS locations, or contacts are accessed or saved.
- **Hardware-Only Identity:** Your cryptographic Soul Signature remains local to your device unless explicitly exported by you.

---

## 🤝 Contributing

Contributions from designers, engineers, and philosophers are welcome:

1. **Fork** the repository.
2. **Create** your feature branch: `git checkout -b feature/NewAesthetic`.
3. **Commit** your changes: `git commit -m 'feat: Add subtle glow shader to feed items'`.
4. **Push** to the branch: `git push origin feature/NewAesthetic`.
5. **Open** a Pull Request.

---

## 🏢 Creator & Contact

Designed and engineered with care by **J3 Labs**.

- **Website:** [thej3labs.com](https://www.thej3labs.com)
- **Web App:** [voidecho.space](https://www.voidecho.space)
- **Support & Inquiries:** `thej3labs@gmail.com`
- **Privacy Policy:** [voidecho.space/privacy](https://www.voidecho.space/privacy)
- **Terms of Service:** [voidecho.space/terms](https://www.voidecho.space/terms)

<br/>

<div align="center">
  <sub>Everything you leave behind belongs to the Void.</sub>
</div>
