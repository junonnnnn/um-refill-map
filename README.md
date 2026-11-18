# UM RefillMap — UM Water Refill Station Reporting & Maintenance

WIA2007 team Android application for reporting and maintaining UM water refill stations.
Current app branding and Gradle root project name are UM RefillMap.
The local checkout folder is `um-refill-map`; historical evidence keeps its original name.
Java package and application ID remain `com.example.umborrowbuddy`.

## Read first

1. [AGENTS.md](AGENTS.md): implementation boundaries and agent instructions.
2. [INTEGRATION.md](INTEGRATION.md): APIs, data relationships and team workflow.
3. This README: status and local setup.

These three files are the only required general reading.
`docs/module1-evidence/` contains historical coursework and assessment evidence only.
Agents read AGENTS.md first, relevant INTEGRATION.md sections as needed, and this README for setup.

## Modules and status

| Module | Owner | Responsibility | Status in this checkout |
|---|---|---|---|
| 1 | Vincent | Authentication, User Roles & Profile | Implemented |
| 2 | Jin Seng | Station Map & Directory | Implemente |
| 3 | Jun Jing | Issue Reporting | Planned |
| 4 | Jie Xin | Maintenance Tracking | Planned |
| 5 | Zhe Yu | AI Report Assistance | Planned |
| 6 | Jun Onn | Community Participation | Planned |

Module 1 includes UM-email registration, email verification, login, password reset,
profile creation/editing, logout and server-backed role routing.
REPORTER opens MainActivity; MAINTENANCE_STAFF opens StaffDashboardActivity;
ADMIN opens AdminDashboardActivity.
MainActivity hosts Module 2 after server-backed reporter validation. Profile/logout
and the staff/admin destinations are preserved. Modules 3–6 remain planned.

Module 2 includes a campus map with status pins, station details and open reports,
faculty/college/water-type filters, nearest working station and external walking directions.
The standalone FSKTM demo retains three surveyed stations with Unknown status.
Missing water/maintenance details are shown as Not recorded.

## Verification status

- Latest saved results, 12 November 2026: debug build, 56 unit tests and 25 API 37
  emulator tests passed, with no failures or skips.
- Local station/report rules checks passed; deployment remains UNVERIFIED.
- Vincent reports successful physical-device Module 1 testing across all three roles.
- Module 2 physical GPS, actual walking routes, API 24 and TalkBack remain UNVERIFIED.
- Pending: verified campus inventory/metadata, live rules rollout and Module 3/4 writers.
- The local Firebase fixture helper is currently missing; restore it before replaying
  the full integration suite. Detailed acceptance remains in
  `docs/plans/2026-11-12-module-2-campus-acceptance.md`.

## Development environment

- Java + XML Views, AppCompat/Material; one `:app` project.
- Java source/target 11; configured Gradle daemon toolchain 25.
- Gradle wrapper 9.6.0; Android Gradle Plugin 9.5.1.
- compileSdk/targetSdk 37; minSdk 24.
- Firebase Android BoM 33.7.0 with Authentication and Cloud Firestore.
- Use an Android Studio version supporting the checked-in AGP and SDK configuration.

## Setup and Firebase onboarding

1. Obtain the team checkout from https://github.com/junonnnnn/um-refill-map.
2. Open the repository root in Android Studio and inspect branch/dirty state.
3. Use your approved working branch; Module 2 work is on `module2`.
4. Install SDK platform 37 and let Studio provision the configured Gradle toolchain.
5. Request matching `google-services.json` from the authorized Firebase project owner.
6. Use the approved private team channel; place the file at `app/google-services.json`.
7. Confirm project ID `um-borrow-buddy` and Android package `com.example.umborrowbuddy`.
8. Let Studio create local SDK configuration; keep `local.properties` untracked.
9. Sync Gradle, select `app`, choose an API 24+ device/emulator, and Run.

Do not create a replacement Firebase project or deploy rules during onboarding.
The JSON is Git-ignored and is not included automatically in a fresh clone.
Backend access, Email/Password enablement and database details must be confirmed
with the project owner; see [Firebase onboarding](INTEGRATION.md#firebase-onboarding).

## Build and test

Run from the repository root in Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

On macOS/Linux use `bash gradlew assembleDebug testDebugUnitTest`.
Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Local test report: `app/build/reports/tests/testDebugUnitTest/index.html`.
Unit tests cover authentication/profile/roles, station validation, filters, nearest selection
and report states. Emulator report: `app/build/reports/androidTests/connected/debug/index.html`.
Firebase integration requires connectivity; role reads intentionally require the server.

## Run the Module 2 demo

Start an Android emulator, then run from the project root:

```bash
bash gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **FSKTM Refill Demo** from the app drawer. It uses bundled data without Firebase.
The normal **UM RefillMap** launcher uses Module 1 login and the configured backend.
If needed, use the SDK's `platform-tools/adb`. CLI builds do not require Studio Gradle sync.

## Before team integration

Follow the ownership, review and acceptance checklist in [INTEGRATION.md](INTEGRATION.md).
Agree the integration lead, merge target, proposed schemas and Firebase/rules ownership.
Keep changes focused; preserve Module 1 interfaces and existing teammate work.
No staging, commit, push, merge or Firebase deployment is implied by this documentation.
