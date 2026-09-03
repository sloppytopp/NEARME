plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
}

// NOTE: :core (included from ../core) declares its own "org.jetbrains.kotlin.jvm"
// plugin version in core/build.gradle.kts, since that module also builds
// standalone (see core/settings.gradle.kts) with plain `gradle test` — no
// Android SDK required. Do not also declare that plugin id here; Gradle
// only tolerates one version declaration per plugin id across the build.
