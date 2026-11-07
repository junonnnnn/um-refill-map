# Module 1 Development Plan

> **HISTORICAL / SUPERSEDED — original staged development plan.**
> Preserve the original gates and assessment evidence below; they are not current implementation instructions.
> Current baseline: Java/XML, Firebase Auth/Firestore, AGP 9.5.1, Gradle 9.6.0, daemon JDK 25.
> The project has its own Git root/.git on branch vincent; parent-repository and item-lending notes are historical.
> Debug build and 43 unit tests passed; Vincent reports physical-device Module 1 success.
> Rules deployment/testing and cross-module acceptance remain unverified.
> Current requirements: [Module 1 PRD](../../INTEGRATION.md#module-1-requirements); general contracts: [INTEGRATION](../../INTEGRATION.md).


Status: COMPLETED ON BRANCH `vincent`
Date: 2026-11-07 (Updated)
Owner: Vincent; one shared Android application.

## 1. Verified local baseline

- :app is the only included project.
- Current project: UMBorrowBuddy at C:/Users/Vincent Loh/AndroidStudioProjects/UMBorrowBuddy; Java + XML Views, AppCompat. Original MainActivity.java and activity_main.xml are preserved; the manifest now launches features.module1.auth.LoginActivity.
- Namespace/application ID: com.example.umborrowbuddy.
- Gradle Kotlin DSL and libs.versions.toml; declared AGP 9.5.0.
- minSdk 24, compileSdk/targetSdk 37, Java source/target compatibility 11.
- Earlier wrapper inspection recorded Gradle 9.6.0 and daemon toolchain Java 25; no successful build has been established.
- Firebase dependencies are absent in the inspected app build file. No local Firebase setup was established in the inspection.
- Existing tests are starter examples, not Module 1 coverage.

## 2. Git safety — inspected before documentation

Read-only checks on 2026-11-02 resolved the new UMBorrowBuddy worktree to C:/Users/Vincent Loh. The new project is still not isolated from the parent Git repository. No Git mutations were performed; edits are confined to UMBorrowBuddy. Do not stage or commit until repository isolation is resolved.

Historical GroupAssignment inspection resolved Git metadata to C:/Users/Vincent Loh/.git. No .git entry was found in GroupAssignment; no remote or GIT_DIR/GIT_WORK_TREE override was reported. Git therefore discovers the enclosing user-directory repository by walking upward. The check does not establish who created that repository or why.

Never delete, move, or modify the parent .git or unrelated repositories to fix this project. Never run git add, commit, push, init, or removal commands without explicit user approval.

PROPOSED safest route after approval:
1. Clone https://github.com/junonnnnn/um-borrow-buddy into a new, unused folder and open that checkout in Android Studio.
2. A clone has its own .git, so Git stops at that folder even if a parent repository exists.
3. Verify the new repository root, origin, branch, and existing team work before transferring anything.
4. Compare the actual team starter with this local starter. Copy only reviewed Module 1 documents/changes; never overwrite the whole team project.
5. Preserve this existing GroupAssignment folder. Do not initialise it or attach the parent repository to the team remote.
6. Coordinate one feature/module1-auth-profile branch, small reviewed pull requests, and a working main branch. Jin Seng uses a separate item feature branch.
7. Exclude local.properties, build outputs, local IDE state, credentials and service-account keys from publication; review the team ignore policy before staging.

No Git mutation or cloning is authorised by this document.

## 3. Historical transition proposal — superseded locally

The user created a separate Java/XML UMBorrowBuddy starter. No automatic Kotlin conversion is required or performed. The following transition notes describe the older GroupAssignment proposal, not steps to apply to this project.

Retain one app and Gradle Kotlin DSL. Java application logic does not require Groovy build scripts.
After team approval, add distinctly named Java activities and XML layouts alongside the existing Kotlin starter. Keep MainActivity.kt and theme files unchanged. Coordinate launcher routing in a separate reviewed shared-file change.
Retain Compose dependencies initially if teammates need them; removing them is a later explicit decision.
Do not change package, application ID, SDKs, Gradle versions, or existing source without approval.

Use Activities + XML, small repository/helper classes, and callbacks from asynchronous Firebase Tasks. Avoid DI frameworks, a custom backend, and an elaborate layered architecture.
PROPOSED Views base: AppCompatActivity with a compatible XML theme and AndroidX AppCompat; Material Components only if needed. Compose Material3 does not supply XML widgets.
Use findViewById initially; View Binding is optional and requires a coordinated Gradle change.
Handle system/keyboard insets, scrollable forms, loading, errors, and recreation without introducing Compose to Module 1.

## 4. File responsibilities

LoginActivity.java, m1_activity_login.xml and m1_strings.xml are created for the UI-only stage. Profile and shared model packages contain package-info.java placeholders only. All other implementation below remains PROPOSED.

Under app/src/main/java/com/example/umborrowbuddy/ (auth and profile below are in features/module1/):
- auth/LoginActivity.java, RegisterActivity.java, VerifyEmailActivity.java, ResetPasswordActivity.java: screen interaction.
- auth/AuthRepository.java: FirebaseAuth operations; never retain an Activity in long-lived callbacks.
- auth/AuthGuard.java: common eligibility checks/results; navigation remains with the caller.
- auth/UmEmailValidator.java: one consistent UM email validator.
- profile/ProfileActivity.java: create/view/edit profile.
- profile/ProfileRepository.java: users/{uid} reads/writes and error results.
- shared/model/UserProfile.java: agreed Java data model, no-argument constructor/accessors for mapping.

Under res/layout/: m1_activity_login.xml, m1_activity_register.xml, m1_activity_verify_email.xml, m1_activity_reset_password.xml, m1_activity_profile.xml.
PROPOSED separate res/values/m1_strings.xml; prefix Module 1 resource names/IDs m1_.
Shared files requiring coordination: AndroidManifest.xml, Gradle scripts/catalog, XML theme, launcher/navigation, shared model, Firebase config, rules and indexes.
No Item model or item code is assigned to Vincent here.

## 5. Firebase prerequisites — manual, after agreement

Confirm the existing shared Firebase project and Android app registration match the approved application ID. Confirm database ID, edition and location before choosing final SDK/rules details. Do not create a second backend.
The designated setup owner enables Email/Password Auth, records password policy and email templates, and supplies the matching google-services.json.
After approval, add the Google Services plugin and Firebase Android BoM with Auth/Firestore Java SDKs; select supported versions then and record them. No dependency versions are prescribed as current by this document.
Review network permission, XML theme compatibility, and Gradle sync. No Admin SDK or service-account key belongs in the APK.
Rules owner reviews and deploys rules only after joint tests and authorisation. Do not use open/test-mode rules as an acceptance shortcut.

## 6. Student implementation stages and gates

| Stage | Manual student task | Review / checkpoint |
|---|---|---|
| 0 | Build the unchanged starter in Android Studio; record environment and outcome | Resolve build failure before changing dependencies |
| 1 | Review these documents with teammates; resolve proposed contracts | Stack, repo, Firebase and navigation decisions recorded |
| 2 | Open approved isolated checkout; apply approved dependencies/config | Sync and baseline build; explain each build change |
| 3 | Create one Java/XML Login screen, then remaining forms | Explain event handling; inspect keyboard/insets/rotation on device |
| 4 | Implement email validation and registration/verification | Accepted/rejected domain cases; actual UM inbox; deny unverified access |
| 5 | Implement login, shared guard, restoration and logout | Wrong credentials, offline refresh, Back/direct entry, account switch |
| 6 | Implement password reset | Validation, neutral confirmation, failure and real reset journey |
| 7 | Implement profile read/create/edit | Save/restart/load, failed read versus missing document, failed save |
| 8 | Review and test rules with emulator/test identities | Identity and cross-user denial matrix; retain evidence |
| 9 | Joint navigation/ownership tests with Jin Seng | Module 2 remains implemented by Jin Seng |
| 10 | Student explains code; reviewer checks evidence and AI log | Definition of Done review before any approved merge |

At every stage Vincent reviews/adapts the code and records what he learned. Assistance should address one reviewed step at a time; do not autonomously generate the entire assessed module.

## 7. Testing and evidence plan

Use local tests for email/field validation, device UI/manual tests for screen/session behaviour, and Firebase Emulator rules tests for access control. Emulator success does not prove real email delivery; test verification/reset with accessible UM inboxes separately.
Test signed out, unverified UM, verified UM A/B, and verified non-UM identities. Cover process recreation, offline/network failures, repeated taps, denied writes, profile persistence and cross-account cached-state isolation.
Stop user-specific listeners and clear rendered data on logout; never show a previous account's cached profile. Do not automatically wipe all Firestore persistence used by teammates without a shared design decision.
Keep evidence records: test ID, date, build/branch, device/API, backend/emulator target, preconditions, expected/actual result, status, screenshot/log reference, student explanation. All implementation tests are currently NOT RUN.

## 8. Definition of Done

- Approved stack, shared contracts, Firebase target and file owners recorded.
- FR1–FR3 and PRD acceptance cases pass with student-produced evidence.
- Server rules deny ineligible and cross-user access; no passwords/tokens in Firestore.
- Build and agreed device checks pass; no secrets in changes.
- Joint Module 2 tests pass without expanding Vincent's implementation scope.
- Student can explain code, failures and security decisions; AI Usage Log complete.
- Teammate review completed; Git publication remains subject to explicit approval.

First manual action: select Build → Make Project (or the IDE's equivalent Assemble Project command) on the unchanged starter and record the result.
