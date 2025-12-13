# ELib - Library Management System

A simple Android library management application built with Java and Firebase Firestore.

## Features

- **Add Books**: Add new books to the library with title, author, ISBN, and year
- **View Books**: Display all books in a recycler view
- **Edit Books**: Update existing book information
- **Delete Books**: Remove books from the library
- **Search**: Search books by title, author, or ISBN
- **Firestore Integration**: All data is stored in Firebase Firestore

## Setup Instructions

1. **Create a Firebase Project**:
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project
   - Enable Firestore Database
   - Add an Android app to your project with package name: `com.elib.library`
   - Download the `google-services.json` file

2. **Configure the App**:
   - Replace the `app/google-services.json` file with your downloaded file from Firebase
   - The file should contain your actual project configuration

3. **Firestore Rules**:
   - In Firebase Console, go to Firestore Database > Rules
   - Set the rules to allow read/write access (for development):
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if true;
       }
     }
   }
   ```
   **Note**: These rules allow anyone to read/write. For production, implement proper security rules.

4. **Build and Run**:
   - Open the project in Android Studio
   - Sync Gradle files
   - Build and run the app on an emulator or device

## Project Structure

```
app/
├── src/main/
│   ├── java/com/elib/library/
│   │   ├── MainActivity.java      # Main activity with UI and Firestore operations
│   │   ├── Book.java              # Book model class
│   │   └── BookAdapter.java      # RecyclerView adapter for books
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml  # Main activity layout
│   │   │   ├── item_book.xml      # Book item layout
│   │   │   └── dialog_add_book.xml # Add/Edit book dialog
│   │   └── values/
│   │       ├── strings.xml
│   │       ├── colors.xml
│   │       └── themes.xml
│   └── AndroidManifest.xml
└── build.gradle
```

## Dependencies

- AndroidX AppCompat
- Material Design Components
- Firebase Firestore
- RecyclerView

## Usage

1. Tap the floating action button (+) to add a new book
2. Fill in the book details (title, author, ISBN, year)
3. Tap Save to add the book
4. Use the search bar to filter books
5. Tap Edit on any book card to modify its information
6. Tap Delete to remove a book from the library

## Requirements

- Android Studio
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34
- Java 8+

## Notes

- This app does not include authentication. All Firestore operations are open.
- For production use, implement proper Firestore security rules and authentication.

