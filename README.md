# NEARME

A phone-only "who's been near me repeatedly" detector. Every modern smartphone
already has the radios you need (BLE) exposed through standard OS APIs — the
tool is the app, no dongle or ESP32 required.

Scope for this v1: **BLE only**, Android only. iOS restricts background BLE
scanning heavily enough that a real iOS build is a separate, later effort.

## The actual hard problem

Modern devices randomize/rotate their BLE MAC address specifically to defeat
tracking. That's great for privacy and terrible for "has this thing followed
me around" detection, because you can't just match on MAC over time. Existing
tools sidestep this by narrowing scope to known tracker chipsets (AirTag,
Tile, SmartTag) that advertise recognizable signatures. This project instead
fingerprints *any* BLE advertisement from what a rotating-MAC device still has
to expose to be useful — service UUIDs, manufacturer data, tx power,
advertising cadence — and matches on similarity instead of identity. Nobody
has solved this well in an open tool; it's also the piece most likely to be
wrong, so it's built to fail closed (see below) rather than confidently
merge unrelated devices.

## Architecture

```
core/            Pure Kotlin/JVM. No Android dependency. Fully unit tested.
  model/         BleAdvertisement, LocationBucket, Sighting
  fingerprint/   DeviceFingerprint + FingerprintExtractor + FingerprintMatcher
  identity/      IdentityResolver — resolves rotating-MAC advertisements to a
                 stable TrackedIdentity via fingerprint similarity
  correlation/   CorrelationEngine — turns a sighting history into a
                 "does this look like it's following the user" score
  verdict/       VerdictEngine — NORMAL / WORTH_NOTING / SUSPICIOUS + plain
                 English explanation
  ScanPipeline   wires the above into one ingest() call

android-app/     Android app scaffold wiring core to the platform.
  scan/          BluetoothLeScanner -> BleAdvertisement mapping
  data/          Room persistence (identities survive app restart) +
                 Wi-Fi-BSSID-based LocationBucketProvider
  service/       Foreground service keeping the scan alive
  ui/            Minimal Jetpack Compose device list (verdict badges)
```

### Why the fingerprint/matching logic is the interesting part

`FingerprintMatcher.similarity()` scores two fingerprints 0.0-1.0 across
service UUIDs (Jaccard), manufacturer company IDs, manufacturer payload
prefixes, advertising interval bucket, tx power, and name hash — but only
over the fields where at least one side actually has data. Two advertisements
with no informative overlap at all score **0.0**, not some default midpoint —
if we can't tell, we don't claim a match. That matters here specifically
because most BLE devices around you advertise near-nothing distinctive; the
safe failure mode is spawning a new identity, not silently merging two
unrelated anonymous devices into one and generating a false "this is
following you."

`CorrelationEngine` treats "seen many times at one place" as always NORMAL
(that's a neighbor's smart speaker or your own gear), and only scores
following-risk from *distinct* locations, weighted toward closer time spans
(three places in two hours is much more alarming than three places over
three months).

## What's actually verified vs. not

- **`core/` is real and tested.** `cd core && gradle test` compiles and runs
  16 JUnit5 tests, including two end-to-end scenarios: a tracker with a
  rotating MAC that follows the user across 3 locations in 2 hours (resolves
  to one identity, verdict SUSPICIOUS), and a stationary device with a
  rotating MAC seen 20 times at home over 20 days (verdict NORMAL). This
  module has zero Android dependency and needs only a JDK to build/test.
- **`android-app/` is written but unverified in this environment.** This
  sandbox has no Android SDK and its network policy blocks
  `dl.google.com` (Google's Maven repo, required for the Android Gradle
  Plugin), so `gradle build` on `android-app/` cannot run here — confirmed by
  testing directly, not assumed. The code follows the real Android BLE
  scanning, foreground service, Room, and Compose APIs, but **you need to
  open it in Android Studio to actually compile, run, and fix whatever
  doesn't come together on a real device** (BLE scanning in particular is
  something you can only really validate on hardware).

## Known limitations (v1, by design)

- **Location bucketing is Wi-Fi-BSSID-based, not GPS.** No coordinates are
  ever recorded. This means the correlation engine can't tell apart two
  outdoor locations you visited off Wi-Fi — both fall into a single
  "unknown" bucket in v1. A geohash/GPS-based `LocationBucketProvider` is a
  natural v2 swap behind the existing interface.
- **A truly featureless BLE advertisement (no service UUIDs, no manufacturer
  data, no name) can't be fingerprinted at all** and will spawn a fresh
  identity on every sighting rather than being tracked — an intentional
  fail-closed tradeoff (see above), and an acknowledged gap a determined
  bad actor's hardware could exploit.
- **iOS is not attempted.** Background BLE scanning restrictions on iOS are
  severe enough that porting this design there is a distinct project.
- **Everything is local-only, by design, not just default.** No network
  permission is requested anywhere in the manifest. A stalking-detection tool
  that phones its findings home is a non-starter regardless of intent.

## Building

```
cd core && gradle test        # runs today, no SDK needed
```

`android-app/` needs Android Studio (or a machine with the Android SDK and
unrestricted access to Google's Maven repo) to build.
