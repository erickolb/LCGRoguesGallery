# LCG Rogues Gallery

An experimental Android cover gallery for comics marked **Have It** in League of Comic Geeks. Android home-screen label: **Rogues Gallery**; repository: `LCGRoguesGallery`.

## Use

1. Sign in on League's own HTTPS page inside the app.
2. Navigate to your own profile collection, then tap **Load Have It**.
3. Wait for the owned-issue import. Collections grouped by series are expanded into their individual owned issues, with progress shown while loading.
4. Use Previous/Next or Play/Pause (15 seconds per cover). Tap the cover to hide or show controls. Menu includes refresh, credits, and sign out.

Passwords are entered directly into League's WebView; the app does not read or store passwords. League session cookies remain in Android's private WebView storage until sign out. Imported titles and image URLs remain in memory; no server or Docker container is required. Android backup is disabled. Covers require internet access. Rotation preserves the current gallery; process termination requires reloading it.

## Integration status

This is an **experimental integration**, not an official League API client. The adapter requests `/comic/get_comics` with `list=2` and `list_option=issue`, following Alistair Brown's library. It supports League's current `.title` / `.cover` markup as well as the older `.comic-title` / `.comic-cover-art` format. When League returns series groups, version 0.1.3 requests each series with `series_id` while retaining the owned-collection and account filters. If necessary, it reads only the issue list on that profile's owned-collection page. Representative series covers and general series recommendations are excluded. Individual variants remain distinct.

League cover thumbnails are promoted to their `large-` image URLs, preserving the variant filename and query string. If a larger image is unavailable, the viewer falls back to its original thumbnail. A sampled League cover was 170×259 pixels as a thumbnail and 600×911 pixels in the large version; dimensions depend on the source artwork.

Selectors and collection grouping were checked against live public pages. Regression tests cover a 51-series / 188-issue collection, variants, image URL selection, and pagination. Pagination follows recognized next links or page counts; unsupported changes may require adapter updates. The updated importer still needs confirmation in the signed-in Android session. Missing cover images are skipped.

The upstream `leagueofcomicgeeks` package is Node.js-specific, so it is **not a runtime dependency** of this Android application. Its collection approach is adapted into a browser script. Its old login scraping depends on UserVoice identification markup; this app instead uses League's own login. Cloudflare may reject embedded browsers; this app does not bypass challenges. If login is blocked, an approved integration or export import will be needed.

## Build

Install JDK 17 or newer and Android SDK 37, set `JAVA_HOME` and `ANDROID_HOME`, then run:

```powershell
.\android\gradlew.bat -p android :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Debug signing is for development. Production signing and Play Store publication are not configured.

## Credits

<a href="https://www.flaticon.com/free-icons/comic-book" title="comic-book icons">Comic-book icons created by Magnific - Flaticon</a>

The supplied icon is centered at 75% of Android's 72dp launcher viewport (54dp inside the 108dp adaptive foreground canvas) on LCG navy `#101F3E`.

Collection adapter based on [leagueofcomicgeeks by Alistair Brown](https://github.com/alistairjcbrown/leagueofcomicgeeks), copyright 2017 Alistair Brown, MIT license. The full upstream license is bundled in the app and shown in Credits.

League of Comic Geeks and comic cover artwork belong to their respective owners. This app is unofficial and unaffiliated.

Local website/credits source: [docs/index.html](docs/index.html). It is ready for static hosting but has not been published.
