# Implementation Plan - Fix KSP error: unexpected jvm signature V

The build error `[ksp] java.lang.IllegalStateException: unexpected jvm signature V` is caused by a compatibility issue between Kotlin Symbol Processing 2 (KSP2) and Room 2.6.1. KSP2 represents Kotlin's `Unit` as `V` (void) in JVM signatures, which Room 2.6.1 does not recognize when processing `suspend` functions that return `Unit`.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/gradle/libs.versions.toml)
- Update `room` version to `2.8.5` (or at least `2.7.0-alpha11+`) which includes support for KSP2 JVM signatures.
- Align `kotlin` and `ksp` versions to ensure stability. I will use the versions recommended by the environment's latest stable:
    - `kotlin` = `2.4.20`
    - `ksp` = `2.3.12` (as per version lookup)
    - *Note:* If KSP still requires strict Kotlin matching, I will adjust to `2.4.20-1.0.x` equivalents if they fail.

#### [MODIFY] [gradle.properties](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/gradle.properties)
- Add `ksp.useKSP2=false` as a fallback if the version updates do not immediately resolve the signature mismatch or if other KSP processors are still incompatible.

## Verification Plan

### Automated Tests
- Run the Gradle task that was failing:
  `./gradlew :app:kspDebugKotlin`
- Perform a full build:
  `./gradlew assembleDebug`

### Manual Verification
- None required beyond successful build.
