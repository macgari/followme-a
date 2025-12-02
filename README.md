# FollowMe Android App

FollowMe is a native Android application for attendance tracking using NFC tags. It is designed to be a feature-complete port of the existing iOS application, offering robust offline support, secure authentication, and efficient tag management.

## 📱 Features

### Core Functionality
*   **NFC Scanning:**
    *   **Single Scan:** Scan individual tags one by one.
    *   **Continuous Scan:** Rapidly scan multiple tags in succession without tapping the screen.
    *   **Format Support:** Reads both JSON and URI NDEF records.
    *   **Tag Types:** Supports NTAG, Mifare, and other standard NFC tag types.
*   **Manual Entry:**
    *   Manually add attendance entries for users without tags (using name or phone number).
*   **Offline Support:**
    *   All scans are saved locally first.
    *   Automatic background synchronization when internet connectivity is restored.
    *   Visual indicators for entry status (Pending, Submitted, Failed, Unmatched).

### Data Management
*   **Categories:** Organize scans into different categories (e.g., "Main", "Class A", "Event B").
*   **Entry Management:**
    *   View history of scanned tags.
    *   Select and delete specific entries.
    *   Bulk delete operations.
*   **CSV Import:** Import tag data from CSV files for bulk tag writing (Admin only).

### Security & Authentication
*   **Secure Storage:** Uses Android's `EncryptedSharedPreferences` to safely store authentication tokens and sensitive settings.
*   **Role-Based Access:**
    *   **Standard User:** Can scan tags and submit attendance.
    *   **Admin/Teacher:** Access to Tag Management and advanced settings.
    *   **Granular Permissions:** Support for specific permissions like `canEditTags`.
*   **Token Validation:** Automatic token refresh and validation.

### Tag Management (Admin/Permission Required)
*   **Write Tags:** Program blank NFC tags with user data.
*   **Batch Writing:** Write a sequence of tags from an imported CSV list.
*   **Manual Tag Creation:** Create and write custom tags on the fly.

## 🛠 Tech Stack

*   **Language:** Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
*   **UI:** XML Layouts with Material Design 3
*   **Networking:** Retrofit + OkHttp
*   **JSON Parsing:** Gson
*   **Concurrency:** Kotlin Coroutines + StateFlow
*   **Local Storage:** EncryptedSharedPreferences

## 🚀 Getting Started

### Prerequisites
*   Android Studio Iguana or later.
*   JDK 17.
*   Physical Android device with NFC support (Emulator does not support NFC scanning).

### Installation
1.  Clone the repository:
    ```bash
    git clone https://github.com/yourusername/followme-android.git
    ```
2.  Open the project in Android Studio.
3.  Sync Gradle files.
4.  Connect your Android device via USB.
5.  Run the app (`Shift + F10`).

### Configuration
1.  Go to **Settings** within the app.
2.  Enter your API Base URL.
3.  Enter your API Key and Credentials.
4.  (Optional) Configure custom categories.

## 📖 Usage Guide

### Scanning Attendance
1.  Open the app.
2.  Select the appropriate **Category** from the dropdown.
3.  Tap **"Scan Tag"** for a single scan or **"Continuous Scan"** for multiple.
4.  Hold the back of your phone near an NFC tag.
5.  The app will beep/vibrate and add the entry to the list.

### Submitting Data
*   The app attempts to submit data automatically every 60 seconds.
*   You can monitor the status icons next to each entry:
    *   🕒 **Clock:** Pending submission.
    *   ✅ **Check:** Successfully submitted.
    *   ❌ **X:** Submission failed (will retry).
    *   ❓ **Question Mark:** Unmatched (server received it but couldn't link to a user).

### Writing Tags (Admins)
1.  Navigate to **Settings** -> **Tag Management** (or use the 3-dot menu if enabled).
2.  **Import CSV:** Load a list of users/tags.
3.  **Create Manual Tag:** Enter specific key-value pairs.
4.  Tap **"Write Tags"** and hold your phone near a blank NFC tag.

## 🤝 Contributing

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'Add some amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## 📄 License

[Your License Here]
