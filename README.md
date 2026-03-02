# HearMeOut - Your Sixth Sense 👂✨

HearMeOut is a mobile accessibility application designed to empower the deaf and hard-of-hearing community. By leveraging On-Device AI, Real-time Cloud Services, and Hardware Haptics, the app provides a safer and more inclusive daily experience.

## 🚀 Key Features

* **Phone Relay Assistant:** Real-time speech-to-text and text-to-speech for seamless phone conversations.
* **Smart Transcription:** Multi-speaker diarization and automated meeting summaries using Google Speech Services.
* **Safety Radar:** An AI-powered sound detector (YAMNet) that alerts users to emergency sounds (Siren, Smoke Alarm, Door knock) via Flashlight and Vibration.
* **Accessibility Map:** A community-driven map for monitoring environmental noise levels and accessibility ratings, powered by Firebase.
* **High-Contrast Mode:** Fully optimized UI for users with visual impairments, supporting RTL (Hebrew) and dynamic font scaling.

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Local Database:** Room Persistence Library (Offline-first approach)
* **Cloud Backend:** Firebase (Auth, Firestore, Cloud Storage)
* **AI/ML:** TensorFlow Lite (YAMNet model for sound classification)
* **Asynchronous Logic:** Kotlin Coroutines & Flow
* **Architecture:** MVVM (Model-View-ViewModel)

## 📸 Screenshots
*(Add your screenshots here later)*

## 📜 How it Works
The app uses a **Repository Pattern** to manage data between the local Room DB and the Firebase cloud. Accessibility settings are managed globally via **CompositionLocalProvider**, ensuring a consistent user experience.
