# Firebase Profile Setup

## Firestore rules

```txt
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

## Notes

- This project now uses Firestore for text profile fields only.
- Profile images are stored locally on device using `SharedPreferences`.
- Firebase Storage is not required.

## Manual verification checklist

1. In Firebase Console, enable Authentication with Anonymous sign-in.
2. In Firebase Console, publish the Firestore rules above.
3. Install and run the app.
4. Open profile page and verify initial profile appears.
5. Edit name/username/bio and tap Save.
6. Close and reopen app, verify updated data persists.
7. Pick a new profile image and tap Save.
8. Reopen app and verify image persists.
9. Confirm one user document exists at `users/{uid}`.
10. Confirm selected profile image still appears after relaunch (local device persistence).
