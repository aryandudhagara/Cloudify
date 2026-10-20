# **Cloudify - Offline Note & Task Management App**

*A modern, private, and 100% offline note-taking and task management Android application built with Room (SQLite), Material Design, and Google Keep-style user experience.*

---

## 🌟 **Key Features**

### 📴 **100% Offline & Free Architecture**
- **Zero Cloud Billing**: Operates completely on-device without external cloud services or subscriptions.
- **Local SQLite Storage (Room)**: Fast, reliable, and private data persistence for notes, tasks, bin, and user accounts.
- **Full Privacy**: Your data never leaves your device.

---

### 📝 **Google Keep-Style Note Editor**
- **📌 Pin Notes**: One-tap pushpin icon in the top action bar. Pinned notes automatically float to the top of the grid and display a pin badge on the card.
- **🎨 Color Palette Sheet**: Interactive bottom sheet with circular color swatches to dynamically tint notes and subtitle indicators with checkmark indicators.
- **📷 Attachment Sheet**:
  - **Take Photo**: Launch device camera directly from the editor.
  - **Add Image**: Attach images from your gallery saved securely to local app storage.
- **⚙️ More Options Sheet**:
  - **Make a Copy**: Instantly duplicate notes.
  - **Copy Note Text**: One-tap clipboard copy of the entire note.
  - **Send / Share**: Share note text to WhatsApp, Gmail, Messages, or any installed app.
  - **Delete**: Move note to the Recycle Bin with a clear, theme-adaptive confirmation dialog.
- **📊 Real-Time Word & Character Counter**: Live counter in the bottom status bar updating as you type.

---

### 📋 **Task & To-Do Management**
- Create, schedule, update, and complete tasks with due dates.
- Interactive **Calendar View** bottom sheet to view tasks by date.
- Completed tasks archive with strike-through and bulk clear options.

---

### 🗑️ **Recycle Bin**
- Safely restore deleted notes back to your active list.
- Permanently purge deleted notes when no longer needed.

---

### 🔐 **Local Authentication & User Management**
- **Default Administrator Account**:
  - **Username**: `admin`
  - **Password**: `admin123`
- **Case-Insensitive Login**: Works seamlessly regardless of keyboard auto-capitalization (`admin`, `Admin`, etc.).
- **Offline Registration**: Register new user profiles locally with automatic instant login.
- **Local Password Reset**: Reset account passwords locally without internet.
- **Role-Based Admin Panel**: System administrators can view user statistics and manage local accounts.

---

### 🎨 **Modern UI & Adaptive Contrast**
- **Smart Note Card Contrast**: Dynamic brightness calculation ensures dark text on light notes (yellow, lavender) and white text on dark notes.
- **High-Contrast Dialogs**: Custom confirmation dialogs with clearly visible buttons on both Light and Dark themes.
- **Dark & Light Mode**: Full Day/Night theme support with instant switching.

---

## 🛠️ **Technology Stack**

| Component | Technology |
| :--- | :--- |
| **Language** | Java 17 |
| **Minimum SDK** | Android 8.0 (API Level 26) |
| **Target / Compile SDK** | Android 15 (API Level 35) |
| **Local Database** | Room (SQLite) with LiveData |
| **UI Framework** | Android Jetpack, XML, Material Design Components |
| **Image Loading** | Bumptech Glide |
| **Session Management** | SharedPreferences (`SessionManager`) |
| **Build Tool** | Gradle 8.13 (Kotlin DSL) |

---

## 🚀 **Getting Started**

### **Option 1: Install Pre-Built APK via USB (Fastest)**
Ensure **USB Debugging** is enabled on your Android phone, connect it via USB, and run:
```powershell
& "C:\Users\dudha\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "app-debug.apk"
```

### **Option 2: Open & Run in Android Studio**
1. Open **Android Studio**.
2. Select **File > Open** and navigate to this folder.
3. Allow Gradle to sync dependencies.
4. Select your connected device or emulator and click **Run ▶**.

---

## 🔑 **Default Credentials**

| Role | Username | Password |
| :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` |
| **Standard User** | *Register any username* | *Set your own password* |

---

## 📂 **Project Directory Structure**

```text
app/
├── src/main/java/com/example/notes_app/
│   ├── Activity/             # Activities (LoginPage, Register, home_page, CreatingNoteActivity, etc.)
│   ├── Models/               # Data models (Notes, Task, users)
│   ├── adapter/              # RecyclerView Adapters (recayclerviewAdapter, TaskAdapter, BinAdapter, etc.)
│   ├── bottomSheetFragment/  # Bottom sheets (CreateTaskBottomSheet, ShowCalendarViewBottomSheet)
│   ├── dao/                  # Room DAOs (NoteDao, TaskDao, UserDao)
│   ├── database/             # Room Databases (NoteDatabase, TaskDatabase, UserDatabase)
│   ├── entities/             # Room Entities (NoteEntity, TaskEntity)
│   ├── listeners/            # Event listener interfaces
│   └── SessionManager.java   # Local session and login state manager
└── src/main/res/
    ├── layout/               # Layout files including Google Keep-style bottom sheets & custom dialogs
    ├── drawable/             # Vector icons (pushpin, palette, camera, copy, share, etc.)
    └── values/               # Colors, themes, styles (Light & Dark mode definitions)
```
