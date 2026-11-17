# Releasing to Google Play

## One-time: upload key
1. Create the key, and keep the `.jks` file and its password somewhere safe (a password manager plus an offline backup). If you lose it, you can reset it in Play Console, but that takes time.
   ```bash
   keytool -genkeypair -v -keystore ~/keys/mileage-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 9125
   ```
2. Copy `keystore.properties.example` to `keystore.properties` in the repo root and fill it in. Git ignores the file, along with `*.jks`.
3. Enroll in **Play App Signing** when you create the app. Google holds the app signing key, and this key only signs your uploads.

## Every release
1. Bump `versionCode` (must increase on every upload) and `versionName` in `app/build.gradle.kts`.
2. `./gradlew check :app:verifyNoForbiddenPermissions` runs the tests and the permission guard.
3. `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest :benchmark:verifyStartupBudget` checks startup is still within budget (needs a device or emulator).
4. `./gradlew :app:bundleRelease` writes `app/build/outputs/bundle/release/app-release.aab`. It refuses to run without an upload key, so a debug-signed bundle can't reach Play.
5. Upload to the **internal** track first, then closed testing, then production. Tag the commit `vX.Y.Z`.

`bundleRelease` uses only the upload key. `assembleRelease` falls back to the debug key when no upload key exists, so local release and benchmark builds still install on the emulator.
