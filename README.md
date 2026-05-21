# NutriScan

NutriScan is an intelligent Android application designed to help users make informed food choices by scanning product labels. It leverages advanced Optical Character Recognition (OCR) and AI-powered analysis to extract nutritional information and assess halal compliance instantly.

## Features

- **AI-Powered OCR**: Uses Google's Gemini API with Vision capabilities to accurately extract text from product packaging, even from complex layouts.
- **Dual Analysis Engine**:
  - **Nutrition Analysis**: Parses extracted text to identify and quantify key nutritional data (calories, protein, carbs, fats).
  - **Halal Analysis**: Analyzes the ingredient list forharam components (e.g., alcohol, animal derivatives) using AI reasoning.
- **Interactive Scanner**: Features a real-time camera preview with an intelligent overlay that highlights the exact region of the label being scanned.
- **Clean & Intuitive UI**: Built with Jetpack Compose, following Material Design 3 guidelines for a modern, fluid user experience.
- **Local Database**: Automatically saves all scan history with detailed results for easy review.

## Prerequisites

- Android Studio
- Android SDK (API 26+)
- Gemini API Key (Required for OCR processing)

## Getting Started

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/nantaa/andro-nutriscan.git
   cd nutriscan
   ```

2. Create an `.env` file in the root directory (see `.env.example` for structure):
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. Open the project in Android Studio.
4. Allow Gradle to sync and resolve dependencies.
5. Remove the following line from `app/build.gradle.kts` (to enable debug builds):
   ```kotlin
   signingConfig = signingConfigs.getByName("debugConfig")
   ```

6. Run the app on an emulator or physical device.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **AI/ML**: Google Gemini API (Vision)
- **Database**: Room (SQLite)
- **Image Processing**: ML Kit Vision Barcode Scanning, CameraX

## Project Structure

The project follows a standard Android architecture with clear separation of concerns:

- `app/`: Main application module.
  - `data/`: Data layer (API clients, repositories, database).
  - `domain/`: Business logic (use cases, models).
  - `presentation/`: UI layer (Activities, ViewModels, Composables).
  - `di/`: Dependency Injection modules (Hilt).

## Support

For issues or feature requests, please open an issue on the GitHub repository.

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/7878b719-6d62-40b5-b7bd-f2ed4b5ace6d

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
