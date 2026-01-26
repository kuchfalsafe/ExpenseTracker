# Troubleshooting MainActivity ClassNotFoundException

## Issue
The app crashes on Nothing device with `ClassNotFoundException: Didn't find class "com.kishan.expensetracker.MainActivity"`

## Solutions Applied

### 1. Multidex Configuration
- ✅ Enabled `multiDexEnabled = true` in build.gradle
- ✅ Created `multidex-config.txt` to keep MainActivity in primary DEX
- ✅ Removed explicit multidex library (Android handles it automatically for API 21+)

### 2. Installation Steps

**IMPORTANT: Completely uninstall the old app before installing the new one**

#### Option A: Using ADB
```bash
# Uninstall completely
adb uninstall com.kishan.expensetracker

# Install fresh APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### Option B: Using Android Studio
1. Go to **Run** > **Edit Configurations**
2. Check **Uninstall application before installing**
3. Run the app

#### Option C: Manual on Device
1. Go to **Settings** > **Apps** > **Expense Tracker**
2. Tap **Uninstall**
3. Install the new APK from file manager or via ADB

### 3. Verify APK Contents

Check if MainActivity is in the APK:
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -i "MainActivity"
```

Check DEX files:
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "classes.*\.dex"
```

You should see multiple DEX files (classes.dex, classes2.dex, etc.)

### 4. Device-Specific Issues

If the issue persists after clean install:

1. **Check Android Version**: Nothing devices might have custom Android modifications
2. **Clear Device Cache**:
   - Settings > Storage > Clear Cache
   - Or boot into recovery and clear cache partition
3. **Check Logcat**:
   ```bash
   adb logcat | grep -i "MainActivity\|ClassNotFoundException\|multidex"
   ```

### 5. Alternative: Build Release APK

Sometimes debug builds have issues. Try building a release APK:
```bash
./gradlew assembleRelease
```

Then install:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### 6. Verify Build Configuration

Ensure these are set in `app/build.gradle.kts`:
- `multiDexEnabled = true`
- `multiDexKeepFile = file("src/main/multidex-config.txt")`
- `minSdk = 26` (multidex is automatic for API 21+)

## Expected Behavior

After clean install:
- App should launch without ClassNotFoundException
- MainActivity should be found and instantiated
- You should see the Expense Tracker home screen

## Still Having Issues?

1. Check if the APK was built correctly:
   ```bash
   ./gradlew clean assembleDebug
   ```

2. Verify MainActivity.kt exists:
   ```bash
   ls -la app/src/main/java/com/kishan/expensetracker/MainActivity.kt
   ```

3. Check package name matches:
   - File: `package com.kishan.expensetracker`
   - Manifest: `android:name=".MainActivity"`
   - Should resolve to: `com.kishan.expensetracker.MainActivity`

4. Try on a different device or emulator to isolate device-specific issues

