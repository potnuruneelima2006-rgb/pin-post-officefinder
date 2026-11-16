# PIN & Post Office Finder

An Android application developed using **Kotlin**, **XML Layouts**, and **ViewBinding** that allows users to find Indian PIN codes and post office details based on their current location, PIN code, or post office name.

## Features

- **Google Sign-In & Firebase Authentication**: Secure user login.
- **Fused Location Provider & Geocoder**: Automatically detects user location and converts latitude/longitude into a PIN code.
- **India Post API Integration**: Fetches real-time post office details (`https://api.postalpincode.in/`).
- **Search Capabilities**:
  - Search by 6-digit PIN code.
  - Search by Post Office name.
- **RecyclerView Results Display**: Displays detailed post office information including Name, Branch Type, Delivery Status, District, State, and PIN code.
- **Google Maps Integration**: Open post office locations directly in Google Maps using geo intents.
- **Room Database**: Save and manage frequently used/favorite PIN codes locally.

## Tech Stack

- **Language**: Kotlin
- **UI**: XML + ViewBinding + Material Components
- **Architecture**: Coroutines for asynchronous network & database operations
- **Database**: Android Room
- **Authentication**: Firebase Auth & Google Play Services Auth
- **Location**: Google Play Services Location & Android Geocoder

## Setup & Running

1. Clone the repository.
2. Add your `google-services.json` file inside the `app/` directory.
3. Open the project in Android Studio and sync with Gradle files.
4. Run the app on an Android emulator or physical device.
