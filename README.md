# CubeWizard

CubeWizard is an Android app that scans a standard 3×3 Rubik’s Cube with the device camera, computes a solution locally, and guides the user through the moves with an interactive cube visualization. It is designed to make solving approachable without requiring users to enter a cube state manually or rely on an online solver.

## Features

- **Guided six-face scan:** Scan the cube face by face with orientation instructions and a live 3×3 camera guide.
- **Live color detection:** Analyze sticker colors from camera frames, with color references that adapt to the camera and lighting.
- **Review and correction:** Inspect detected colors before confirming a face, tap stickers to correct them, edit previously captured faces, or recapture.
- **Cube-state validation:** Check sticker counts and cube-piece constraints, and show localized guidance when the scanned state is incomplete or invalid.
- **Offline solving:** Find a solution on the device using an implementation of Kociemba’s two-phase algorithm. Solver lookup tables are warmed up ahead of solving.
- **Interactive move guidance:** Follow the solution one move at a time with a 3D cube visualization, orientation reminders, previous/next controls, and replay/reset actions.
- **Optional spoken moves:** Use Android text-to-speech to hear move instructions.
- **Onboarding:** Show scan instructions and remember when they have been dismissed.

## How it works

1. Grant camera permission and follow the instructions for the face currently requested.
2. Position the cube in the on-screen guide and capture the face.
3. Review the nine detected stickers. Correct any errors, then confirm the face or recapture it.
4. Repeat for all six faces. CubeWizard arranges the captured colors in the solver’s `U, R, F, D, L, B` facelet order and validates the resulting cube.
5. Follow the generated solution in the visualizer. Keep the indicated faces oriented as instructed and advance through each turn.

The camera color analyzer samples the sticker regions and classifies colors in Lab color space. Center stickers are fixed to their known face color and can be used to calibrate color references as scanning progresses. The solver validates corner and edge permutations and orientations before searching for a solution.

## Technology

- Kotlin and Android Gradle Plugin
- Jetpack Compose and Material 3
- CameraX for camera preview and image analysis
- Kotlin Coroutines and `StateFlow` for asynchronous work and UI state
- Hilt for dependency injection
- Preferences DataStore for onboarding state
- Android TextToSpeech for optional spoken instructions
- JUnit and AndroidX instrumentation/Compose UI testing

## Requirements

- Android Studio with Android SDK Platform 35 installed
- JDK 17
- Android device or emulator running Android 7.0 (API 24) or later
- A camera for the cube scanning flow

The app's minimum supported Android version is API 24. The project compiles against API 35 and targets API 35. Camera permission is required for scanning. Cube solving runs offline after installation; spoken instructions depend on an available Android text-to-speech engine.
