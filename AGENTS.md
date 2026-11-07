# Agent instructions

## Scope and reading order

- Read this file first for every task.
- Read only the relevant [INTEGRATION.md](INTEGRATION.md) sections when the task needs technical contracts.
- Read [README.md](README.md) for setup, run instructions and project status.
- Do not read historical files in `docs/module1-evidence/` by default; consult them only for evidence-specific tasks.
- Treat source as the implementation authority; report documentation conflicts.
- Label claims IMPLEMENTED, PROPOSED, or UNVERIFIED as appropriate.

## Stack and identity

- Implement Android application code in Java and XML Views.
- Keep package, namespace and application ID `com.example.umborrowbuddy`.
- Gradle Kotlin DSL is build configuration; it does not authorize Kotlin/Compose screens.
- Keep the single `:app` project and existing Gradle wrapper.
- Do not rename the legacy project, theme or app label as incidental cleanup.
- Match the existing Activities, callbacks, AppCompat and Material Views approach.

## Module ownership

| Module | Owner | Boundary | Resource prefix |
|---|---|---|---|
| 1 | Vincent | Authentication, roles, profile | `m1_` |
| 2 | Jin Seng | Station Map & Directory | `m2_` |
| 3 | Jun Jing | Issue Reporting | `m3_` |
| 4 | Jie Xin | Maintenance Tracking | `m4_` |
| 5 | Zhe Yu | AI Report Assistance | `m5_` |
| 6 | Jun Onn | Community Participation | `m6_` |

- Keep feature work inside its module boundary.
- Preserve teammates' meaningful tracked and untracked work.
- Coordinate cross-module edits with the affected owner and integration lead.
- The integration lead and shared-file reviewers must be designated by the team.

## Preserve Module 1

- Preserve working registration, verification, login, reset, profile and logout flows.
- Preserve the three dashboard destinations and public integration interfaces.
- Reuse `AuthRepository`, `AuthGuard`, `RoleRouter`, `UserProfile` and `UserRole`.
- Do not create competing authentication sessions, role enums or user collections.
- Retrieve the server profile before making role-based navigation decisions.
- Keep read errors and missing/invalid roles explicit; never turn failures into REPORTER.
- Use `RoleRouter.routeProfile` for a retrieved profile; null means profile creation.
- Do not use the legacy defaulting parser as validation of remote role data.
- Do not assign privileged roles or rewrite existing roles from client profile edits.

## Naming and shared changes

- Use PascalCase Java classes and camelCase methods/fields.
- Use lowercase snake_case XML resources and the owning module's prefix.
- Example new names: `m2_activity_station_directory.xml`, `m2_station_title`.
- Prefer `features.moduleN` under the existing package for new module code.
- Keep existing names and signatures stable; do not rename shared interfaces incidentally.
- Put display text in string resources; preserve labels, insets and loading/error feedback.
- Obtain integration-lead approval before changing manifest, Gradle/catalog or SDK versions.
- The same approval applies to shared models, auth/router, MainActivity, themes and Firebase rules/config.
- Owner review is required before repurposing a dashboard placeholder.
- Keep proposed schemas provisional until affected owners approve them.

## Firebase and privacy

- Reuse project `um-borrow-buddy`; confirm the target before any backend operation.
- Obtain `app/google-services.json` through the approved private team channel.
- Keep the existing ignore policy; never add service-account keys to the Android app.
- Never log passwords, tokens, credentials or sensitive profile fields.
- Never weaken Firestore rules to make a client request pass.
- Local rules are not confirmed deployed or tested; do not claim otherwise.
- Do not deploy rules/indexes, create backends, alter live data or assign roles without authorization.
- UI role guards supplement server authorization; they do not replace it.

## Validation and Git safety

- Before editing, check repository root, Git directory, branch, origin and dirty state.
- Keep all edits inside this Android project; never change a parent repository.
- Do not discard, reset or overwrite existing work.
- Do not stage, commit, push, merge or switch branches without explicit authorization.
- Before proposing integration, run `./gradlew assembleDebug testDebugUnitTest`.
- On Windows PowerShell use `.\gradlew.bat assembleDebug testDebugUnitTest`.
- Preserve existing tests and add focused regression coverage for changed behavior.
- Report build, unit tests, device checks and Firebase/rules checks separately.
- Never claim physical-device testing from compilation or another person's report.
- For documentation-only changes, verify links/API names and cite the latest test evidence.
- Keep historical coursework/AI-usage evidence; label superseded guidance rather than deleting it.
