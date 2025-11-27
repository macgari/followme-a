# FollowMe - NFC Attendance Tracking App (Android)

FollowMe is an Android application designed for tracking attendance using NFC (Near Field Communication) technology. The app allows users to scan NFC tags, manually enter attendance data, and automatically submit attendance records to a backend API.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage Guide](#usage-guide)
- [API Requirements](#api-requirements)
- [Database Schema](#database-schema)
- [NFC Tag Format](#nfc-tag-format)
- [Troubleshooting](#troubleshooting)

## Features

### Core Functionality
- **NFC Tag Scanning**: Read attendance data from NFC tags containing JSON or URI records
- **Manual Entry**: Manually enter names or phone numbers for attendance
- **Automatic Submission**: Automatically submits pending entries to the API every minute
- **Category Management**: Organize attendance entries by categories (e.g., "Main", "Tasbeha")
- **Tag Writing**: Write data to NFC tags (admin feature)
- **CSV Import**: Import CSV files to bulk-write tags (admin feature)
- **Role-Based Access**: Admin users can access tag management features

### User Interface
- Real-time authentication status indicator
- Category dropdown for filtering entries
- Status icons for entry submission status:
  - ✅ Green checkmark: Successfully submitted
  - 📞 Orange phone icon: Phone number submitted but not matched to a name
  - ⏰ Orange clock: Pending submission
  - ❌ Red exclamation: Submission failed
- Check/uncheck entries for batch operations
- Delete selected entries

## Requirements

### Android Requirements
- **Android Version**: Android 5.0 (API 21) or later
- **NFC Support**: Device with NFC capability
- **Permissions**: NFC, Internet, Network State

### Backend Requirements
- RESTful API server
- HTTPS recommended (HTTP supported for development)
- Token-based authentication (JWT or similar)
- Database for storing attendance records

## Installation

### Building from Source

1. Clone or download the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on a physical Android device (NFC requires physical hardware)

### APK Installation

1. Download the APK file
2. Enable "Install from Unknown Sources" in device settings
3. Install the APK
4. Grant NFC and Internet permissions

## Configuration

### Initial Setup

1. **Open Settings**: Tap the menu button (☰) in the top-left corner
2. **Configure API Settings**:
   - **API Base URL**: Your backend server URL (e.g., `https://api.example.com` or `192.168.1.100`)
   - **API Key**: Your API key (stored securely)
   - **Username**: Your authentication username
   - **Password**: Your authentication password
   - **Auth Route**: Authentication endpoint path (e.g., `authenticate`)
   - **Validate Route**: Token validation endpoint path (e.g., `validate_token`)
   - **Main Route**: Attendance submission endpoint path (e.g., `mark` or `attendance`)

3. **Test Connection**: Tap "Test API" to verify authentication

4. **Configure Categories** (Optional):
   - Add custom categories in Settings
   - Categories map API values to display names
   - Default category is "Main"

### Settings Fields Explained

- **API Base URL**: The base URL of your backend server. Can include protocol (`http://` or `https://`) or omit it (defaults to `http://`). Trailing slashes are automatically removed.
- **API Key**: Required header (`X-API-Key`) for authentication requests
- **Auth Route**: Path appended to base URL for authentication (e.g., `authenticate` → `{baseURL}/authenticate`)
- **Validate Route**: Path for token validation (e.g., `validate_token` → `{baseURL}/validate_token`)
- **Main Route**: Path for attendance submission (e.g., `mark` → `{baseURL}/mark`)
- **Extensions**: Additional HTTP headers to include in requests (key-value pairs)

## Usage Guide

### Scanning NFC Tags

1. **Single Scan**:
   - Tap the "Scan Tag" button
   - Hold your Android device near an NFC tag
   - The app will read the tag and display the data

2. **Continuous Scanning**:
   - Tap "Start Continuous Scan"
   - Hold your device near multiple tags sequentially
   - Tap "Stop Scanning" when finished

### Manual Entry

1. Enter a name or phone number in the text field
2. Tap "Add Entry" or press Enter
3. The entry will be added to the list with "pending" status

### Viewing and Managing Entries

- **Filter by Category**: Use the category dropdown to filter entries
- **Select Entries**: Tap entries to select/deselect them
- **Check All**: Use the "Check All" button to select all visible entries
- **Delete Selected**: Tap "Delete Selected" to remove entries
- **View Status**: Check the status icon next to each entry

### Submitting Attendance

- **Automatic**: Entries are automatically submitted every minute if:
  - Device is online
  - There are pending or failed entries
  - User is authenticated

- **Manual**: Entries are also submitted when:
  - User manually triggers submission
  - App comes back online after being offline

### Writing Tags (Admin Only)

1. Navigate to **Tags** page (admin only)
2. Import a CSV file or manually add entries
3. Select entries to write
4. Tap "Write Tags"
5. Hold your device near blank NFC tags one by one

### CSV Import Format

The CSV file should have:
- **First row**: Column headers (keys)
- **Subsequent rows**: Data values

Example:
```csv
name,phone,url
John Doe,1234567890,https://example.com
Jane Smith,0987654321,https://example.com
```

**Special Fields**:
- `url`: If present, creates a URI NDEF record (removed from JSON payload)
- Other fields: Stored as JSON in the NFC tag

## API Requirements

### Authentication Endpoint

**Endpoint**: `POST {baseURL}/{authRoute}`

**Headers**:
```
Content-Type: application/json
X-API-Key: {apiKey}
```

**Request Body**:
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "123",
    "name": "John Doe",
    "email": "user@example.com",
    "role": "admin"
  }
}
```

**Alternative Response Formats** (for backward compatibility):
```json
{
  "success": true,
  "accessToken": "...",
  "expiresIn": 3600,
  "teacher": {
    "id": "123",
    "role": "teacher"
  }
}
```

or:
```json
{
  "success": true,
  "accessToken": "...",
  "expiresIn": 3600,
  "id": "123",
  "role": "admin"
}
```

**Error Response** (non-200 status):
- Any non-200 status code indicates failure
- Response body may contain error details

**Required Fields**:
- `success`: Boolean indicating success
- `accessToken`: JWT or token string
- `expiresIn`: Token expiration time in seconds
- `user.id` or `teacher.id` or `id`: User identifier
- `user.role` or `teacher.role` or `role`: User role ("admin", "teacher", etc.)

### Token Validation Endpoint

**Endpoint**: `GET {baseURL}/{validateRoute}`

**Headers**:
```
Authorization: Bearer {accessToken}
```

**Success Response** (200 OK):
```json
{
  "valid": true,
  "user": {
    "id": "123",
    "name": "John Doe",
    "email": "user@example.com",
    "role": "admin"
  },
  "apiKey": "...",
  "expiresAt": "2024-12-31T23:59:59Z"
}
```

**Error Response**:
- 401 Unauthorized: Token expired or invalid
- Other non-200 status codes indicate failure

### Attendance Submission Endpoint

**Endpoint**: `POST {baseURL}/{mainRoute}`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**:
```json
[
  {
    "name": "John Doe",
    "ts": "2024-01-15T10:30:00.000Z",
    "category": "Main",
    "user_id": "123"
  },
  {
    "name": "1234567890",
    "ts": "2024-01-15T10:31:00.000Z",
    "category": "Main",
    "user_id": "123"
  }
]
```

**Field Descriptions**:
- `name`: Name or phone number (string)
- `ts`: ISO8601 timestamp in UTC (string, e.g., "2024-01-15T10:30:00.000Z")
- `category`: Category key (string, e.g., "Main", "tasbeha")
- `user_id`: User ID from authentication (optional string)

**Success Response** (200 OK):

**Format 1 - Array of Objects** (Recommended):
```json
[
  {
    "name": "John Doe",
    "phone": "1234567890"
  },
  {
    "name": "UNMATCHED",
    "phone": "9876543210"
  }
]
```

- If `name` is "UNMATCHED", the phone number was not matched to a name in the database
- The app displays an orange phone icon for unmatched entries

**Format 2 - Array of Strings** (Legacy):
```json
[
  "John Doe",
  "Jane Smith"
]
```

**Error Response**:
- Non-200 status codes indicate failure
- Entries are marked as "failed" and will be retried

## Database Schema

### Minimum Required Tables

#### Users Table

```sql
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Should be hashed (bcrypt, etc.)
    email VARCHAR(255),
    name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'teacher', -- 'admin', 'teacher', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Indexes**:
```sql
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
```

#### Attendance Records Table

```sql
CREATE TABLE attendance_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20), -- Optional, for phone number matching
    timestamp TIMESTAMP NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'Main',
    user_id VARCHAR(255), -- ID of the user who submitted the record
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

**Indexes**:
```sql
CREATE INDEX idx_attendance_timestamp ON attendance_records(timestamp);
CREATE INDEX idx_attendance_category ON attendance_records(category);
CREATE INDEX idx_attendance_user_id ON attendance_records(user_id);
CREATE INDEX idx_attendance_phone ON attendance_records(phone);
```

#### Phone Number Lookup Table (Optional but Recommended)

For matching phone numbers to names:

```sql
CREATE TABLE phone_directory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    normalized_phone VARCHAR(20) NOT NULL, -- Digits only for matching
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Indexes**:
```sql
CREATE INDEX idx_phone_directory_normalized ON phone_directory(normalized_phone);
CREATE UNIQUE INDEX idx_phone_directory_phone ON phone_directory(phone);
```

### Phone Number Matching Logic

The API should:
1. Normalize phone numbers (remove formatting: `(123) 456-7890` → `1234567890`)
2. Match submitted phone numbers against `phone_directory` table
3. Return matched name in response
4. Return `"UNMATCHED"` as name if phone number not found

**Example Matching Query**:
```sql
SELECT name 
FROM phone_directory 
WHERE normalized_phone = REPLACE(REPLACE(REPLACE(REPLACE(?, ' ', ''), '-', ''), '(', ''), ')', '')
LIMIT 1;
```

### API Token Storage (Backend)

If using JWT tokens, store token metadata:

```sql
CREATE TABLE api_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL, -- Hash of the token
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Indexes**:
```sql
CREATE INDEX idx_tokens_user_id ON api_tokens(user_id);
CREATE INDEX idx_tokens_expires_at ON api_tokens(expires_at);
```

## NFC Tag Format

### Reading Tags

The app reads NFC tags in the following formats:

1. **JSON Format** (Primary):
   - MIME Type: `application/json`
   - Content: JSON object with key-value pairs
   - Example:
     ```json
     {
       "name": "John Doe",
       "phone": "1234567890",
       "category": "Main"
     }
     ```

2. **URI Format**:
   - NDEF Type: URI (Well-Known Type)
   - Supported prefixes: `http://`, `https://`, `tel:`, `mailto:`
   - Example: `https://example.com`

3. **Combined Format**:
   - Can contain both URI and JSON records
   - URI record is read separately from JSON

### Writing Tags

The app writes NFC tags with:

1. **URI Record** (if `url` key exists):
   - Creates NDEF URI record
   - Supports prefix codes for common URLs
   - Removes `url` key from JSON payload

2. **JSON Record**:
   - MIME Type: `application/json`
   - Contains all other key-value pairs
   - Example:
     ```json
     {
       "name": "John Doe",
       "phone": "1234567890"
     }
     ```

**Tag Requirements**:
- Must support NDEF format
- Must be writable (not locked)
- Recommended: NTAG213, NTAG215, NTAG216, or similar

## Troubleshooting

### Authentication Issues

**Problem**: "Authentication failed" error
- **Solution**: 
  - Verify API Base URL is correct
  - Check API Key is valid
  - Ensure username/password are correct
  - Check server is accessible from device network

**Problem**: Token expires immediately
- **Solution**: 
  - Check `expiresIn` value in API response (should be seconds)
  - Verify server time is synchronized

### Submission Issues

**Problem**: Entries stuck with orange clock icon
- **Solution**:
  - Check internet connection
  - Verify authentication is valid (tap Test API)
  - Check API endpoint is correct
  - Ensure entries have valid name or phone number

**Problem**: Entries show red exclamation (failed)
- **Solution**:
  - Check API endpoint accepts the request format
  - Verify server logs for errors
  - Ensure category values match API expectations
  - Failed entries will be retried automatically

### NFC Issues

**Problem**: Cannot read NFC tags
- **Solution**:
  - Ensure device supports NFC
  - Enable NFC in device settings
  - Hold device closer to tag
  - Try different tag orientation
  - Check tag is not damaged

**Problem**: Cannot write to NFC tags
- **Solution**:
  - Verify tag is writable (not locked)
  - Ensure tag supports NDEF format
  - Try a different tag
  - Check tag has sufficient memory

### Category Issues

**Problem**: Categories not appearing
- **Solution**:
  - Go to Settings and add categories
  - Ensure category keys match API expectations
  - Restart app after adding categories

### Admin Access Issues

**Problem**: Cannot see "Tags" page
- **Solution**:
  - Verify user role is "admin" in API response
  - Log out and log back in
  - Check API returns role in authentication response

## Security Considerations

1. **API Key**: Stored securely in Android SharedPreferences (encrypted)
2. **Passwords**: Never stored in app; only sent during authentication
3. **Tokens**: Stored locally, expire automatically
4. **HTTPS**: Recommended for production (HTTP supported for development)
5. **Token Validation**: App validates tokens on startup and before submissions

## Support

For issues or questions:
1. Check the Troubleshooting section
2. Review API logs for server-side errors
3. Verify API endpoints match the requirements
4. Ensure database schema matches the specifications

## License

[Add your license information here]

## Version History

- **1.0**: Initial Android release
  - NFC tag reading and writing
  - Automatic attendance submission
  - Category management
  - Role-based access control
  - Phone number matching support
  - 100% feature parity with iOS version
# followme-a
