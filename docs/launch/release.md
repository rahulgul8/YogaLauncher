# Releasing to Google Play

## Build types

| Build | Signed with | Yoga guard | Use |
| --- | --- | --- | --- |
| `debug` | debug key | on | development |
| `profile` | debug key | on | release-speed (R8) checks on a phone; the founder's daily build |
| `release` | upload key | on while `guardInRelease = true` | Play bundles |

Yoga guard is in every build today (founder, 2026-11-07). One switch in `app/build.gradle.kts`, `guardInRelease`,
takes it out of Play bundles if its Accessibility API declaration is ever held up: set it to `false` and the release build
drops the service (declared only in `app/src/guard/AndroidManifest.xml`) and hides every guard screen (`BuildConfig.GUARD`).
With guard in, the first Play upload needs the Accessibility API declaration and a demonstration video (see `play-listing.md`).

## Upload key

Play App Signing holds the app signing key; we hold only the upload key.

- Keystore: `~/.secrets/yoga-upload.jks`, alias `yoga-upload`, credentials in `~/.secrets/yoga-upload.properties`
  (or set `YOGA_UPLOAD_PROPERTIES`). Never commit or print either file. Back both up somewhere safe.
- SHA-256: `E7:BB:A9:7E:A7:12:DF:1C:5F:05:56:AD:6B:3E:48:16:71:BF:A2:D9:EE:DF:5E:3F:FD:4B:93:C3:B8:9B:CB:10`
- Lost key: request an upload key reset in Play Console (App integrity).

## Building

```
./gradlew :app:bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

Bump `versionCode` for every upload. The first bundle is uploaded by hand in Play Console (the API can't create an app);
later ones go through `scripts/play-upload.mjs` with the TUWI service account, after the founder's go-ahead.

## Choosing an update priority

Pass `--priority N` to `play-upload.mjs upload` or `promote`. Use priority 2 for normal releases. Use 4 for an
important update that should open Play's flexible update dialog. Reserve 5 for rare critical fixes because it opens a
full-screen immediate update. The script defaults to 2 and accepts values from 0 to 5.

## Database

Schema JSON is exported to `app/schemas`. Version 8 is the first released schema. Any entity change: bump the version,
add a `Migration`, and commit the new schema file. Only downgrades fall back to wiping data.
