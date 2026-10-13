# Signed builds and releases

The **Signed Android release** GitHub Actions workflow uses these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The keystore is restored only in the runner's temporary directory and deleted after the build. It is not included in artifacts or source control. Signed builds run importer tests, Java unit tests, and release lint, then verify the APK package, version, and certificate against the supplied keystore. The Android App Bundle signature is also checked.

## Build without publishing

In GitHub, open **Actions → Signed Android release → Run workflow**, choose `main`, and run it. Or use:

```powershell
gh workflow run release.yml --repo erickolb/RoguesGallery --ref main
```

Download the `RoguesGallery-<version>-signed` artifact from the completed run. It contains:

- `RoguesGallery.apk` — directly installable Android release build.
- `RoguesGallery.aab` — Android App Bundle for an app-store upload.
- `SHA256SUMS.txt` — file checksums.
- `signing-certificate-sha256.txt` — public signing certificate fingerprint.

Manual runs do not create a GitHub release. Existing debug installations with the same application ID must be uninstalled before installing the release-signed APK. Future APK updates must use the same release key.

## Publish a release

Update `versionName` and increase `versionCode` in `android/app/build.gradle.kts`, update the changelog, and push the commit. Push a new tag matching `versionName`, optionally prefixed with `v`:

```powershell
# Example for a future release; use the actual versionName.
git tag -a v1.0-rc2 -m "Rogues Gallery 1.0-rc2"
git push origin v1.0-rc2
```

The workflow builds, verifies, and publishes the signed files on the tagged GitHub release. Versions containing a hyphen, such as release candidates, are marked as prereleases and are not marked latest. Already-published release assets are left unchanged on reruns.

The earlier `1.0-rc1` tag predates the application ID rename and signing workflow. Keep it as historical source; do not move it. A new tag for a release must point to a commit containing this workflow and the intended application ID.

Repository secrets are used only by this tag/manual workflow, not by pull-request builds. Keep repository write access restricted to people trusted to run signed builds.
