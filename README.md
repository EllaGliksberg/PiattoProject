# Piatto

Piatto is an Android recipe-sharing social app built in Kotlin. Users can register or sign in, create recipe posts with images and device location, browse posts from other users, save recipes, view profile content, discover external recipes, and see location-based posts on Google Maps.

The project is structured around MVVM, Firebase, Room, Navigation Component, SafeArgs, Retrofit, and Google Maps.

## Main Features

- Firebase Authentication for login, registration, auto-login, and logout.
- Firestore-backed social recipe posts.
- Room local cache for posts.
- Delta sync using `lastUpdated` and `SharedPreferences`.
- Feed screen with pull-to-refresh.
- Post creation and editing with gallery image selection.
- Current device location is attached to posts when permission/location are available.
- Map screen displays posts with valid coordinates as markers.
- Map marker click opens the matching post details screen with SafeArgs.
- Profile screen with editable name, username, bio, and image.
- User-owned posts and saved posts sections.
- Post details screen with save count, save toggle, edit button for owner, and back navigation.
- External recipe search using TheMealDB through Retrofit and Gson.
- Picasso image loading/caching for URL images.
- Base64 image fallback for remote image sharing when Firebase Storage is unavailable.

## Tech Stack

- Language: Kotlin
- Minimum SDK: 26
- Target SDK: 36
- Architecture: MVVM with Repository layer
- UI: Fragments, ViewBinding, Material Components
- Navigation: Android Navigation Component + SafeArgs
- Remote backend: Firebase Auth + Cloud Firestore
- Local cache: Room
- Maps/location: Google Maps SDK + Fused Location Provider
- REST API: Retrofit + Gson
- Image loading: Picasso for URL images, custom Base64 decoding for Firestore image strings
- Async: Kotlin coroutines, `viewModelScope`, suspend functions, `Dispatchers.IO`

## Project Structure

```text
app/src/main/java/com/example/piattoproject
+-- MainActivity.kt
+-- data/remote/externalrecipes
|   +-- MealApiService.kt
|   +-- RetrofitClient.kt
|   +-- DTO classes
+-- repository
|   +-- ExternalRecipesRepository.kt
+-- ui/auth
|   +-- AuthFragment.kt
|   +-- AuthViewModel.kt
|   +-- FirebaseAuthRepository.kt
+-- ui/post
|   +-- FeedFragment.kt
|   +-- AddPostFragment.kt
|   +-- AddPostViewModel.kt
|   +-- PostViewModel.kt
|   +-- PostRepository.kt
|   +-- Post.kt
|   +-- PostDao.kt
|   +-- AppLocalDbRepository.kt
+-- ui/postdetails
|   +-- PostDetailsFragment.kt
|   +-- PostDetailsViewModel.kt
+-- ui/map
|   +-- MapFragment.kt
|   +-- MapViewModel.kt
+-- ui/profile
|   +-- ProfileFragment.kt
|   +-- ProfileViewModel.kt
|   +-- FirebaseProfileRepository.kt
|   +-- FirebaseUserPostsRepository.kt
+-- ui/externalrecipes
|   +-- ExternalRecipesFragment.kt
|   +-- ExternalRecipesViewModel.kt
|   +-- ExternalRecipesAdapter.kt
+-- utils
    +-- ImageUtils.kt
```

Navigation is defined in:

```text
app/src/main/res/navigation/nav_graph.xml
```

## Architecture Overview

The app follows this general flow:

```text
Fragment
-> ViewModel
-> Repository
-> Firestore / Room / REST API
```

Fragments are responsible for rendering UI and forwarding user actions. ViewModels hold screen state with `LiveData` and launch asynchronous work with `viewModelScope`. Repositories contain Firebase, Room, and network access.

Examples:

- `FeedFragment` observes `PostViewModel.posts`.
- `PostViewModel` exposes Room-backed posts from `PostRepository.allPosts`.
- `PostRepository` performs Firestore sync and writes changed posts into Room.
- `MapViewModel` calls `PostRepository.loadMapPosts()` and does not access Firestore directly.

## Authentication

Authentication uses Firebase Auth.

Supported behavior:

- Register with email/password.
- Login with email/password.
- Auto-login when `FirebaseAuth.currentUser` exists.
- Logout from the profile screen.

When switching users, the profile ViewModel tracks the current Firebase `uid`, clears stale state on logout, and reloads profile data when a different user logs in. This prevents the profile page from showing the previous user on the same device.

## Firestore Data

### Posts Collection

Posts are stored in the `posts` collection.

Important fields:

```text
id: String
recipeTitle: String
description: String
imageUrl: String
creatorName: String
creatorUid: String
latitude: Double?
longitude: Double?
lastUpdated: Long
savesCount: Int
```

`lastUpdated` is stored as a numeric timestamp from `System.currentTimeMillis()`. It is updated when a post is created or edited.

### Users Collection

User profile data is stored in the `users` collection by Firebase UID.

Important fields:

```text
fullName: String
username: String
bio: String
imageUrl: String?
updatedAt: server timestamp
```

Saved posts are stored under:

```text
users/{uid}/savedPosts/{postId}
```

## Room Local Cache

Room is used as the local cache for posts.

Main files:

- `Post.kt`: Room entity.
- `PostDao.kt`: DAO with LiveData read and suspend read/write methods.
- `AppLocalDbRepository.kt`: Room database.
- `PostRepository.kt`: repository that writes synced data into Room.

The feed observes local Room data:

```text
FeedFragment
-> PostViewModel.posts
-> PostRepository.allPosts
-> PostDao.getAll()
-> LiveData<List<Post>>
```

Room database name:

```text
piatto_db.db
```

`allowMainThreadQueries()` is not used. Room writes and single reads are suspend functions or are executed inside `Dispatchers.IO`.

## Delta Sync

The feed uses a minimal delta-sync strategy.

Flow:

```text
Feed opens or user pulls to refresh
-> PostViewModel.refreshPosts()
-> PostRepository.syncPostsDelta()
-> read last sync from SharedPreferences
-> query Firestore where lastUpdated > lastSync
-> insert changed posts into Room
-> update last sync only after successful Room write
-> Feed updates automatically from Room LiveData
```

SharedPreferences:

```text
name: post_sync_prefs
key: last_posts_sync
```

Firestore query:

```kotlin
firestore.collection("posts")
    .whereGreaterThan("lastUpdated", lastSync)
    .orderBy("lastUpdated", Query.Direction.ASCENDING)
```

Current limitation: delta sync handles created and updated posts. Remote delete synchronization is not implemented with tombstones. Deletes performed by the current user update the local cache through the existing delete flow.

## Image Handling

### Base64 Firestore Fallback

Firebase Storage is unavailable in the current project environment, so selected images are shared remotely through Firestore as compressed Base64 strings.

Implementation:

- Selected images are decoded from URI.
- Images are resized to a maximum side of 800 pixels.
- Images are compressed as JPEG with quality 72.
- The final string is stored as:

```text
data:image/jpeg;base64,...
```

This is handled in `ImageUtils.encodeImageUriToBase64()`.

Image display supports:

- Base64 Firestore strings.
- Remote URL strings through Picasso.
- Empty/null image references with placeholder handling.

Important note: Firebase Storage dependency exists in Gradle, but Firebase Storage upload is intentionally not implemented because the current environment cannot use it. Do not remove the Base64 fallback unless Firebase Storage is fully available and migrated.

## Maps and Location

The app uses Google Maps and the Fused Location Provider.

Permissions:

```xml
ACCESS_FINE_LOCATION
ACCESS_COARSE_LOCATION
INTERNET
```

Post location flow:

```text
AddPostFragment
-> request/check location permission
-> use lastLocation if available
-> fallback to getCurrentLocation()
-> pass latitude/longitude to AddPostViewModel
-> PostRepository stores numeric latitude/longitude in Firestore
```

Map flow:

```text
MapFragment
-> MapViewModel
-> PostRepository.loadMapPosts()
-> filter posts with latitude/longitude
-> render map markers
-> marker.tag = post.id
-> marker click navigates to PostDetailsFragment(postId)
```

Markers use a small version of the post photo when the image is Base64-decodable. If no valid image is available, the map uses a food-style fallback marker.

## Navigation and SafeArgs

The app uses a single `MainActivity` with fragments managed by the Navigation Component.

Main destinations:

- Auth
- Feed
- Add/Edit Post
- Post Details
- Map
- Profile
- Profile Posts List
- External Recipes

Parameterized navigation uses SafeArgs. Examples:

- Feed to Post Details: `FeedFragmentDirections.actionFeedToPostDetails(post.id)`
- Map marker to Post Details: `MapFragmentDirections.actionMapToPostDetails(postId)`
- Post Details to Edit Post: `PostDetailsFragmentDirections.actionPostDetailsToAddPost(postId, true)`
- Profile to Profile Posts List: `ProfileFragmentDirections.actionProfileToProfilePostsList(...)`

Back buttons on Post Details and Add/Edit Post call:

```kotlin
findNavController().popBackStack()
```

This returns to the actual previous screen instead of hardcoding a destination.

## External Recipes API

The external recipe screen integrates with TheMealDB:

```text
https://www.themealdb.com/api/json/v1/1/search.php?s={query}
```

Networking stack:

- `RetrofitClient`
- `MealApiService`
- `ExternalRecipesRepository`
- `ExternalRecipesViewModel`
- `ExternalRecipesFragment`

Results are displayed in a RecyclerView with image loading through `ImageUtils`, which delegates URL images to Picasso.

## Setup Instructions

### 1. Firebase

Create or connect a Firebase project and enable:

- Firebase Authentication with email/password.
- Cloud Firestore.

Place the Firebase configuration file at:

```text
app/google-services.json
```

Suggested Firestore collections:

```text
posts
users
users/{uid}/savedPosts
```

### 2. Google Maps API Key

Add your Maps API key to `local.properties`:

```properties
GOOGLE_MAPS_API_KEY=your_api_key_here
```

Gradle injects it into the app as:

```text
@string/google_maps_key
```

The manifest reads this value for Google Maps:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="@string/google_maps_key" />
```

### 3. Build

From the project root:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

Or build/run the app from Android Studio.

## Manual Test Plan

### Auth

1. Register a new user.
2. Logout.
3. Login with the same user.
4. Close and reopen the app.
5. Verify auto-login.

### Profile User Switching

1. Login as user A.
2. Open Profile and verify user A data.
3. Logout.
4. Login as user B on the same device.
5. Open Profile.
6. Verify user B data appears, not cached user A data.

### Create Post

1. Open Feed.
2. Tap add post.
3. Choose an image.
4. Enter title and description.
5. Allow location permission.
6. Save.
7. Verify the post appears in Feed.
8. Verify Firestore document has `latitude`, `longitude`, and numeric `lastUpdated`.

### Edit Post

1. Open a post created by the current user.
2. Tap Edit Post.
3. Change text or image.
4. Save.
5. Verify Feed and Post Details show updated data.
6. Verify `lastUpdated` changed in Firestore.

### Room Cache and Delta Sync

1. Open Feed online once.
2. Verify posts appear.
3. Disable internet.
4. Reopen Feed.
5. Verify cached posts still appear.
6. Re-enable internet.
7. Pull to refresh.
8. Verify changed posts sync into Room.

### Map

1. Create a post while location permission is granted.
2. Open Map.
3. Verify the post appears as a marker.
4. Tap marker.
5. Verify Post Details opens for that post.

### External Recipes

1. Open Recipes screen.
2. Search for a recipe term.
3. Verify TheMealDB results appear with images.

## Known Limitations

- Firebase Storage upload is not implemented because the current project environment does not support it. Images are stored as compressed Base64 strings in Firestore instead.
- Delta sync does not implement remote delete tombstones.
- Manual map-based location selection is not implemented. Posts use current device location at creation/edit time.
- Camera capture is not implemented; image selection currently uses gallery/document picker flows.

## Build Status

The project currently compiles with:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```
