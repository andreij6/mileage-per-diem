# feature:insights:data

Android. Writes year-in-review share cards to the cache and hands back a shareable content URI. Package `com.aijones.mileage.feature.insights.data`.

## Public API
- `CacheShareCardWriter` implements `ShareCardWriter`: writes PNG bytes to `cacheDir/share/<name>` on `@IoDispatcher` (same name overwrites) and returns `FileProvider.getUriForFile(context, packageName + ".files", file)`.
- `InsightsDataModule` (Hilt, `SingletonComponent`) binds it.
- Relies on the app's `FileProvider` (`${applicationId}.files`, `cache-path share/` in `app/src/main/res/xml/file_paths.xml`). Nothing is uploaded; the app has no `INTERNET`.

## Dependencies
`feature:insights:domain`, `core:common`, `core:model`, androidx.core (FileProvider).

## Tests
Robolectric (`src/test`): `CacheShareCardWriterTest` writes, overwrites and checks the URI. `src/test/AndroidManifest.xml` + `src/test/res/xml/test_share_paths.xml` register a test FileProvider. FileProvider caches its path strategy per authority in a static, so keep URI assertions in one test method.
