package com.followme.attendance.ui.help

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.followme.attendance.R

class HelpActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this)
        setContentView(webView)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.help_documentation)
        
        loadContent()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun loadContent() {
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    :root {
                        --primary-color: #007AFF;
                        --secondary-color: #5856D6;
                        --success-color: #34C759;
                        --warning-color: #FF9500;
                        --text-color: #333;
                        --bg-color: #fff;
                        --code-bg: #f4f4f4;
                        --border-color: #ddd;
                        --section-bg: #f9f9f9;
                    }
                    
                    @media (prefers-color-scheme: dark) {
                        :root {
                            --primary-color: #0A84FF;
                            --secondary-color: #5E5CE6;
                            --success-color: #32D74B;
                            --warning-color: #FF9F0A;
                            --text-color: #eee;
                            --bg-color: #1c1c1e;
                            --code-bg: #2c2c2e;
                            --border-color: #444;
                            --section-bg: #2c2c2e;
                        }
                    }
                    
                    * {
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        line-height: 1.6;
                        color: var(--text-color);
                        background-color: var(--bg-color);
                        padding: 16px;
                        margin: 0;
                        font-size: 15px;
                    }
                    
                    h1 {
                        color: var(--primary-color);
                        margin-top: 0;
                        margin-bottom: 0.5em;
                        border-bottom: 3px solid var(--primary-color);
                        padding-bottom: 12px;
                        font-size: 28px;
                        font-weight: 700;
                    }
                    
                    h2 {
                        color: var(--secondary-color);
                        margin-top: 1.8em;
                        margin-bottom: 0.8em;
                        border-bottom: 2px solid var(--border-color);
                        padding-bottom: 8px;
                        font-size: 22px;
                        font-weight: 600;
                    }
                    
                    h3 {
                        color: var(--primary-color);
                        margin-top: 1.5em;
                        margin-bottom: 0.6em;
                        font-size: 18px;
                        font-weight: 600;
                    }
                    
                    h4 {
                        color: var(--text-color);
                        margin-top: 1.2em;
                        margin-bottom: 0.5em;
                        font-size: 16px;
                        font-weight: 600;
                    }
                    
                    p {
                        margin: 0.8em 0;
                    }
                    
                    code {
                        font-family: "SF Mono", Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
                        background-color: var(--code-bg);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-size: 0.88em;
                        color: var(--secondary-color);
                    }
                    
                    pre {
                        background-color: var(--code-bg);
                        padding: 14px;
                        border-radius: 8px;
                        overflow-x: auto;
                        border: 1px solid var(--border-color);
                        margin: 1em 0;
                    }
                    
                    pre code {
                        background-color: transparent;
                        padding: 0;
                        border: none;
                        color: var(--text-color);
                        font-size: 13px;
                    }
                    
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 1.2em 0;
                        font-size: 14px;
                    }
                    
                    th, td {
                        border: 1px solid var(--border-color);
                        padding: 10px;
                        text-align: left;
                    }
                    
                    th {
                        background-color: var(--code-bg);
                        font-weight: 600;
                    }
                    
                    .note {
                        background-color: rgba(255, 149, 0, 0.1);
                        border-left: 4px solid var(--warning-color);
                        padding: 14px;
                        margin: 1.5em 0;
                        border-radius: 0 8px 8px 0;
                    }
                    
                    .success {
                        background-color: rgba(52, 199, 89, 0.1);
                        border-left: 4px solid var(--success-color);
                        padding: 14px;
                        margin: 1.5em 0;
                        border-radius: 0 8px 8px 0;
                    }
                    
                    .section {
                        background-color: var(--section-bg);
                        padding: 16px;
                        border-radius: 10px;
                        margin: 1.5em 0;
                        border: 1px solid var(--border-color);
                    }
                    
                    ul, ol {
                        padding-left: 24px;
                        margin: 0.8em 0;
                    }
                    
                    li {
                        margin-bottom: 6px;
                    }
                    
                    strong {
                        font-weight: 600;
                        color: var(--primary-color);
                    }
                    
                    .endpoint {
                        background-color: var(--section-bg);
                        padding: 12px;
                        border-radius: 6px;
                        margin: 1em 0;
                        border-left: 4px solid var(--primary-color);
                    }
                    
                    .field-desc {
                        margin-left: 1.5em;
                        font-size: 14px;
                        color: var(--text-color);
                        opacity: 0.9;
                    }
                    
                    .toc {
                        background-color: var(--section-bg);
                        padding: 16px;
                        border-radius: 10px;
                        margin: 1.5em 0;
                    }
                    
                    .toc a {
                        color: var(--primary-color);
                        text-decoration: none;
                        display: block;
                        padding: 4px 0;
                    }
                    
                    .toc a:active {
                        opacity: 0.6;
                    }
                </style>
            </head>
            <body>
                <h1>📱 FollowMe API & Database Documentation</h1>
                
                <p>This App is completey public, free and does not require any user registeration, signup, or subscription. This App serves as a free shell that let users manage their own clients using their own Database and API. This guide provides comprehensive information about the API requirements and database schema needed to set up a backend server for the FollowMe attendance tracking application.</p>
                
                <div class="toc">
                    <strong>📋 Table of Contents</strong>
                    <a href="#api-overview">API Overview</a>
                    <a href="#auth-endpoint">1. Authentication Endpoint</a>
                    <a href="#validate-endpoint">2. Token Validation Endpoint</a>
                    <a href="#attendance-endpoint">3. Attendance Submission Endpoint</a>
                    <a href="#database-schema">Database Schema</a>
                    <a href="#nfc-format">NFC Tag Format</a>
                    <a href="#troubleshooting">Troubleshooting</a>
                </div>
                
                <h2 id="api-overview">🌐 API Overview</h2>
                
                <div class="section">
                    <p>The FollowMe app requires a RESTful API backend with three main endpoints:</p>
                    <ul>
                        <li><strong>Authentication:</strong> Creates and returns an access token</li>
                        <li><strong>Token Validation:</strong> Verifies token validity</li>
                        <li><strong>Attendance Submission:</strong> Receives and stores attendance records</li>
                    </ul>
                    
                    <div class="note">
                        <strong>⚠️ Important:</strong> All endpoints should use HTTPS in production. HTTP is supported for development only.
                    </div>
                </div>
                
                <h2 id="auth-endpoint">🔐 1. Authentication Endpoint</h2>
                
                <div class="endpoint">
                    <strong>Endpoint:</strong> <code>POST {baseURL}/{authRoute}</code><br>
                    <span class="field-desc">Example: <code>POST https://api.example.com/authenticate</code></span>
                </div>
                
                <h3>Request Headers</h3>
                <pre><code>Content-Type: application/json
X-API-Key: {apiKey}</code></pre>
                
                <h3>Request Body</h3>
                <pre><code>{
  "username": "user@example.com",
  "password": "password123"
}</code></pre>
                
                <h3>Success Response (200 OK)</h3>
                <pre><code>{
  "success": true,
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "123",
    "name": "John Doe",
    "email": "user@example.com",
    "role": "admin"
  }
}</code></pre>
                
                <h4>Required Fields</h4>
                <ul>
                    <li><code>success</code>: Boolean indicating authentication success</li>
                    <li><code>accessToken</code>: JWT or token string for subsequent requests</li>
                    <li><code>expiresIn</code>: Token expiration time in seconds</li>
                    <li><code>user.id</code>: Unique user identifier</li>
                    <li><code>user.role</code>: User role ("admin", "teacher", etc.)</li>
                </ul>
                
                <div class="note">
                    <strong>💡 Alternative Formats:</strong> The app also supports legacy formats where user data is in a <code>teacher</code> object or directly at the root level.
                </div>
                
                <h3>Error Response</h3>
                <p>Any non-200 status code indicates authentication failure.</p>
                
                <h2 id="validate-endpoint">✅ 2. Token Validation Endpoint</h2>
                
                <div class="endpoint">
                    <strong>Endpoint:</strong> <code>GET {baseURL}/{validateRoute}</code><br>
                    <span class="field-desc">Example: <code>GET https://api.example.com/validate_token</code></span>
                </div>
                
                <h3>Request Headers</h3>
                <pre><code>Authorization: Bearer {accessToken}</code></pre>
                
                <h3>Success Response (200 OK)</h3>
                <pre><code>{
  "valid": true,
  "user": {
    "id": "123",
    "name": "John Doe",
    "email": "user@example.com",
    "role": "admin"
  },
  "expiresAt": "2024-12-31T23:59:59Z"
}</code></pre>
                
                <h3>Error Response</h3>
                <ul>
                    <li><strong>401 Unauthorized:</strong> Token expired or invalid</li>
                    <li><strong>Other non-200 codes:</strong> Validation failed</li>
                </ul>
                
                <h2 id="attendance-endpoint">📝 3. Attendance Submission Endpoint</h2>
                
                <div class="endpoint">
                    <strong>Endpoint:</strong> <code>POST {baseURL}/{mainRoute}</code><br>
                    <span class="field-desc">Example: <code>POST https://api.example.com/mark</code></span>
                </div>
                
                <h3>Request Headers</h3>
                <pre><code>Content-Type: application/json
Authorization: Bearer {accessToken}</code></pre>
                
                <h3>Request Body</h3>
                <pre><code>[
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
]</code></pre>
                
                <h4>Field Descriptions</h4>
                <ul>
                    <li><code>name</code>: Person's name or phone number (string)</li>
                    <li><code>ts</code>: ISO8601 timestamp in UTC (e.g., "2024-01-15T10:30:00.000Z")</li>
                    <li><code>category</code>: Category key (e.g., "Main", "tasbeha")</li>
                    <li><code>user_id</code>: ID of the user submitting the record (optional)</li>
                </ul>
                
                <h3>Success Response (200 OK)</h3>
                
                <h4>Format 1 - Array of Objects (Recommended)</h4>
                <pre><code>[
  {
    "name": "John Doe",
    "phone": "1234567890"
  },
  {
    "name": "UNMATCHED",
    "phone": "9876543210"
  }
]</code></pre>
                
                <p class="field-desc">If <code>name</code> is "UNMATCHED", the phone number was not found in the directory. The app will display an orange phone icon (📞) for these entries.</p>
                
                <h4>Format 2 - Array of Strings (Legacy)</h4>
                <pre><code>[
  "John Doe",
  "Jane Smith"
]</code></pre>
                
                <h3>Error Response</h3>
                <p>Non-200 status codes indicate failure. Failed entries are marked with a red exclamation (❌) and will be automatically retried.</p>
                
                <h2 id="database-schema">🗄️ Database Schema</h2>
                
                <h3>Users Table</h3>
                <pre><code>CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- Hashed (bcrypt, etc.)
    email VARCHAR(255),
    name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'teacher',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);</code></pre>
                
                <h3>Attendance Records Table</h3>
                <pre><code>CREATE TABLE attendance_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    timestamp TIMESTAMP NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'Main',
    user_id VARCHAR(255),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_attendance_timestamp ON attendance_records(timestamp);
CREATE INDEX idx_attendance_category ON attendance_records(category);
CREATE INDEX idx_attendance_user_id ON attendance_records(user_id);
CREATE INDEX idx_attendance_phone ON attendance_records(phone);</code></pre>
                
                <h3>Phone Directory Table (Optional but Recommended)</h3>
                <p>Used for matching phone numbers to names:</p>
                <pre><code>CREATE TABLE phone_directory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    normalized_phone VARCHAR(20) NOT NULL,  -- Digits only
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_phone_directory_normalized ON phone_directory(normalized_phone);
CREATE UNIQUE INDEX idx_phone_directory_phone ON phone_directory(phone);</code></pre>
                
                <h4>Phone Number Matching Logic</h4>
                <p>The API should normalize phone numbers (remove formatting) and match against the directory:</p>
                <pre><code>-- Example matching query
SELECT name 
FROM phone_directory 
WHERE normalized_phone = REPLACE(REPLACE(REPLACE(REPLACE(?, ' ', ''), '-', ''), '(', ''), ')', '')
LIMIT 1;</code></pre>
                
                <h3>API Tokens Table (Optional)</h3>
                <p>For JWT token metadata storage:</p>
                <pre><code>CREATE TABLE api_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_tokens_user_id ON api_tokens(user_id);
CREATE INDEX idx_tokens_expires_at ON api_tokens(expires_at);</code></pre>
                
                <h2 id="nfc-format">🏷️ NFC Tag Format</h2>
                
                <h3>Reading Tags</h3>
                <p>The app reads NFC tags in the following formats:</p>
                
                <h4>1. JSON Format (Primary)</h4>
                <pre><code>{
  "name": "John Doe",
  "phone": "1234567890",
  "category": "Main"
}</code></pre>
                <p class="field-desc">MIME Type: <code>application/json</code></p>
                
                <h4>2. URI Format</h4>
                <pre><code>https://example.com
tel:1234567890
mailto:user@example.com</code></pre>
                <p class="field-desc">NDEF Type: URI (Well-Known Type)</p>
                
                <h4>3. Combined Format</h4>
                <p>Tags can contain both URI and JSON records.</p>
                
                <h3>Writing Tags (Admin Only)</h3>
                <p>The app can write NFC tags with:</p>
                <ul>
                    <li><strong>URI Record:</strong> If <code>url</code> key exists in data</li>
                    <li><strong>JSON Record:</strong> All other key-value pairs</li>
                </ul>
                
                <div class="note">
                    <strong>💡 Tag Requirements:</strong> Tags must support NDEF format and be writable (not locked). Recommended: NTAG213, NTAG215, or NTAG216.
                </div>
                
                <h2 id="troubleshooting">🔧 Troubleshooting</h2>
                
                <h3>Authentication Issues</h3>
                <div class="section">
                    <p><strong>Problem:</strong> "Authentication failed" error</p>
                    <p><strong>Solutions:</strong></p>
                    <ul>
                        <li>Verify API Base URL is correct and accessible</li>
                        <li>Check API Key is valid</li>
                        <li>Ensure username/password are correct</li>
                        <li>Verify server is accessible from device network</li>
                        <li>Check server logs for detailed error messages</li>
                    </ul>
                </div>
                
                <h3>Submission Issues</h3>
                <div class="section">
                    <p><strong>Problem:</strong> Entries stuck with orange clock icon (⏰)</p>
                    <p><strong>Solutions:</strong></p>
                    <ul>
                        <li>Check internet connection</li>
                        <li>Verify authentication is valid (tap "Test API" in Settings)</li>
                        <li>Ensure Main Route endpoint is correct</li>
                        <li>Verify entries have valid name or phone number</li>
                    </ul>
                    
                    <p><strong>Problem:</strong> Entries show red exclamation (❌)</p>
                    <p><strong>Solutions:</strong></p>
                    <ul>
                        <li>Check API endpoint accepts the request format</li>
                        <li>Verify server logs for errors</li>
                        <li>Ensure category values match API expectations</li>
                        <li>Failed entries will be automatically retried</li>
                    </ul>
                </div>
                
                <h3>Status Icons</h3>
                <div class="section">
                    <ul>
                        <li>✅ <strong>Green checkmark:</strong> Successfully submitted</li>
                        <li>📞 <strong>Orange phone:</strong> Phone number submitted but not matched to a name</li>
                        <li>⏰ <strong>Orange clock:</strong> Pending submission</li>
                        <li>❌ <strong>Red exclamation:</strong> Submission failed (will retry)</li>
                    </ul>
                </div>
                
                <div class="success">
                    <strong>✨ Need Help?</strong> Review your API logs for detailed error messages and ensure all database tables are created with proper indexes.
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
}
