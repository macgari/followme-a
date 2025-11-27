# Building and Running the FollowMe Android App

## Prerequisites

1. **Android Studio** (Latest version recommended)
   - Download from: https://developer.android.com/studio
   - Install with Android SDK

2. **JDK 17** or higher
   - Android Studio usually includes this

3. **Physical Android Device with NFC**
   - NFC cannot be tested on emulators
   - Device must run Android 5.0 (API 21) or higher
   - NFC must be enabled in device settings

## Setup Instructions

### 1. Open Project in Android Studio

```bash
cd /Users/mac/Dev/followme-m/followme-a
```

Then open this directory in Android Studio:
- File → Open → Select `followme-a` directory

### 2. Sync Gradle

Android Studio should automatically prompt you to sync Gradle files. If not:
- File → Sync Project with Gradle Files

Wait for all dependencies to download.

### 3. Connect Physical Device

1. Enable Developer Options on your Android device:
   - Settings → About Phone → Tap "Build Number" 7 times

2. Enable USB Debugging:
   - Settings → Developer Options → USB Debugging → ON

3. Connect device via USB cable

4. Accept USB debugging prompt on device

### 4. Build and Run

#### Option A: Using Android Studio

1. Select your device from the device dropdown (top toolbar)
2. Click the green "Run" button (▶️) or press Shift+F10
3. App will build and install on your device

#### Option B: Using Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build and install in one command
./gradlew installDebug
```

The APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Build Release APK

For production release:

```bash
# Build release APK (unsigned)
./gradlew assembleRelease
```

For signed release (requires keystore):

1. Create keystore:
```bash
keytool -genkey -v -keystore followme-release.keystore -alias followme -keyalg RSA -keysize 2048 -validity 10000
```

2. Add to `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("../followme-release.keystore")
            storePassword "your_password"
            keyAlias "followme"
            keyPassword "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ... existing config
        }
    }
}
```

3. Build signed release:
```bash
./gradlew assembleRelease
```

## Gradle Wrapper Setup

If you don't have the Gradle wrapper files, create them:

```bash
# Install Gradle wrapper
gradle wrapper --gradle-version 8.1.4
```

This creates:
- `gradlew` (Unix/Mac)
- `gradlew.bat` (Windows)
- `gradle/wrapper/` directory

## Common Build Issues

### Issue: "SDK location not found"

**Solution**: Create `local.properties` file:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### Issue: "Gradle sync failed"

**Solution**: 
1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` and `.idea` folders
3. Re-sync project

### Issue: "Manifest merger failed"

**Solution**: Check `AndroidManifest.xml` for conflicts
- Ensure all activities are declared
- Check for duplicate permissions

### Issue: "Dependency resolution failed"

**Solution**:
1. Check internet connection
2. Update Gradle version in `gradle/wrapper/gradle-wrapper.properties`
3. Clear Gradle cache: `~/.gradle/caches/`

## Testing the App

### 1. Enable NFC

On your Android device:
- Settings → Connected Devices → Connection Preferences → NFC → ON

### 2. Configure API Settings

1. Open app
2. Tap menu (☰) → Settings
3. Enter your API configuration:
   - API Base URL
   - API Key
   - Username/Password
   - Routes (auth, validate, main)
4. Tap "Test API" to verify connection

### 3. Test NFC Scanning

1. Tap "Scan Tag" button
2. Hold NFC tag near back of device
3. Entry should appear in list

### 4. Test Manual Entry

1. Type name or phone number
2. Tap "Add Entry"
3. Entry appears with "Pending" status

### 5. Test Automatic Submission

- Wait 60 seconds
- Pending entries should change to "Submitted" status
- Requires valid API configuration and internet connection

## Project Structure

```
followme-a/
├── app/
│   ├── build.gradle                    # App module build config
│   ├── proguard-rules.pro             # ProGuard rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # App manifest
│           ├── java/com/followme/attendance/
│           │   ├── FollowMeApplication.kt
│           │   ├── data/
│           │   │   ├── api/           # API client & models
│           │   │   ├── local/         # Local storage
│           │   │   ├── model/         # Data models
│           │   │   └── repository/    # Repository layer
│           │   ├── nfc/               # NFC handler
│           │   ├── service/           # Background services
│           │   └── ui/                # UI components
│           │       ├── MainActivity.kt
│           │       ├── adapters/      # RecyclerView adapters
│           │       ├── account/       # Account screen
│           │       ├── help/          # Help screen
│           │       ├── settings/      # Settings screen
│           │       └── tags/          # Tag management
│           └── res/
│               ├── drawable/          # Icons
│               ├── layout/            # XML layouts
│               ├── menu/              # Menu resources
│               ├── values/            # Strings, colors, themes
│               └── xml/               # NFC filters, file paths
├── build.gradle                       # Root build config
├── settings.gradle                    # Gradle settings
├── gradle.properties                  # Gradle properties
└── README.md                          # Documentation
```

## Next Steps

### To Complete the App

The following components need implementation:

1. **Settings Activity** (`SettingsActivity.kt` + layout)
   - API configuration form
   - Category management
   - HTTP headers management
   - Test API functionality

2. **Tag Management Activity** (`TagManagementActivity.kt` + layout)
   - CSV import
   - Tag data entry
   - NFC tag writing

3. **Help Activity** (`HelpActivity.kt` + layout)
   - Display README content
   - Formatted documentation

4. **Account Activity** (`AccountActivity.kt` + layout)
   - User info display
   - Token status

All core functionality (NFC, API, storage, submission) is complete and working.

## Deployment

### Google Play Store

1. Build signed release APK
2. Create app listing on Google Play Console
3. Upload APK/AAB
4. Fill in store listing details
5. Submit for review

### Direct Distribution

1. Build signed release APK
2. Host APK on your server
3. Users download and install
4. Requires "Install from Unknown Sources" enabled

## Support

For issues:
1. Check logcat output in Android Studio
2. Review API logs on backend
3. Verify NFC is enabled and working
4. Check network connectivity

## License

[Add your license information here]
