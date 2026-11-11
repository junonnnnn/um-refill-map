# Integration contract

Status: current source contract plus explicitly PROPOSED future contracts.
Current app branding and Gradle root project name: UM RefillMap.
Module 1 contract reviewed against branch `vincent` on 2026-11-07;
Module 2 progress and checkout details updated against local `main` on 2026-11-11.
Agents read [AGENTS.md](AGENTS.md) first, then only the relevant sections here.
Use [README.md](README.md) for setup; historical evidence is excluded from default agent reading.

## Status and architecture

- IMPLEMENTED: one `:app`, Java + XML Activities, AppCompat and Material Views.
- IMPLEMENTED: Firebase Authentication and the default FirebaseFirestore instance.
- Configured Firebase project: `um-borrow-buddy`; package: `com.example.umborrowbuddy`.
- No separate backend, Compose navigation graph or DI framework exists in this checkout.
- Activities own UI/loading/errors; AuthRepository uses asynchronous Firebase Task callbacks.
- UserProfile/UserRole live under `com.example.umborrowbuddy.shared.model`.
- Auth classes live under `com.example.umborrowbuddy.features.module1.auth`.
- ProfileActivity lives under `com.example.umborrowbuddy.features.module1.profile`.
- IMPLEMENTED: Module 2 standalone FSKTM map/directory under `features.module2`,
  using bundled station data, MapLibre Native 13.6.1 and OpenFreeMap.
- Reporter navigation still has its integration placeholder; Modules 3–6 remain pending.
- Latest local build and 45 unit tests passed on 2026-11-11; Module 2 API 37 emulator
  checks and limits are recorded locally in `docs/plans/2026-11-11-module-2-acceptance.md`;
  that evidence is excluded from this commit.
- Owner-reported: Module 1 passed physical-device tests, including all three roles.
- UNVERIFIED: local rules deployment/testing and future cross-module flows.

## Module 1 requirements

Owner: Vincent; WIA2007 Authentication, User Roles & Profile.
- FR1: register with exactly @siswa.um.edu.my or @um.edu.my; verify email before protected access.
- Password policy requirement: at least 8 characters, uppercase (A–Z), lowercase (a–z) and digit (0–9).
- The current validator uses Java Character classification, which also accepts Unicode letters/digits.
- Keep field-level validation and neutral password-reset feedback; support login and logout.
- FR2: display the assigned role read-only; never allow self-assignment of privileged roles.
- FR3: create/view/edit displayName (1–60), faculty (1–100), residentialCollege (1–100).
- Profile role display uses m1_profile_role; m1_profile_dashboard_btn returns to the assigned dashboard.
- Schema, server-read error handling, APIs and three-role destinations are specified below.
- Module 1 baseline: debug build and 43 local unit tests passed; the later combined
  45-test result above includes Module 2. Physical-device success is owner-reported.

## Actual navigation

1. Main manifest launcher is LoginActivity; other main Activities are not exported.
   Debug builds also expose the StationDirectoryDemo alias targeting the
   non-exported StationDirectoryActivity, with a separate debug task affinity.
   This standalone demo does not use the sign-in flow below.
2. LoginActivity checks an existing session onStart or authenticates entered credentials.
3. Unverified users open VerifyEmailActivity; protected screens check AuthGuard.
4. Verified login/verification retrieves `users/{currentAuthUid}` using Source.SERVER.
5. Existing documents require a valid raw role before profile deserialization/routing.
6. Read failure or invalid/missing role in an existing document produces callback failure.
7. An absent document produces onSuccess(null); routeProfile opens ProfileActivity.
8. ProfileActivity creates/edits the profile and refreshes it before dashboard navigation.
9. Logout signs out and returns to LoginActivity with the task cleared.

| Assigned role | Canonical role destination |
|---|---|
| REPORTER | `com.example.umborrowbuddy.MainActivity` |
| MAINTENANCE_STAFF | `features.module1.auth.StaffDashboardActivity` |
| ADMIN | `features.module1.auth.AdminDashboardActivity` |

The latter two names are relative to `com.example.umborrowbuddy`.
MainActivity redirects non-reporters; AdminDashboardActivity permits ADMIN.
StaffDashboardActivity currently permits MAINTENANCE_STAFF and ADMIN.
Canonical ADMIN routing still selects AdminDashboardActivity.
All three destinations expose profile/logout and preserve future module placeholders.

## Existing public Java APIs

AuthRepository methods below are instance methods; obtain one with `new AuthRepository()`.
A public `AuthRepository(FirebaseAuth, FirebaseFirestore)` constructor also exists.
`Callback<T>` exposes `void onSuccess(T result)` and `void onFailure(String errorMessage)`.

| AuthRepository method | Result / behavior |
|---|---|
| `FirebaseUser getCurrentUser()` | Current Firebase user, or null |
| `String getCurrentUid()` | Current Auth UID, or null |
| `void register(String email, String password, Callback<RegistrationResult> callback)` | Creates Auth account and attempts verification email |
| `void sendEmailVerification(Callback<Void> callback)` | Sends verification to current user |
| `void checkEmailVerification(Callback<Boolean> callback)` | Reloads user and attempts token refresh |
| `void login(String email, String password, Callback<FirebaseUser> callback)` | Signs in and attempts token refresh |
| `void sendPasswordReset(String email, Callback<Void> callback)` | Requests reset email |
| `void logout()` | Signs out |
| `void getUserProfile(String uid, Callback<UserProfile> callback)` | Server profile, null when absent, failure on read/invalid role |
| `void saveUserProfile(UserProfile profile, Callback<Void> callback)` | Merges profile fields; preserves existing role |

RegistrationResult exposes `getUser()`, `isVerificationEmailSent()`, `getVerificationErrorMessage()`.
Registration success can include verification-email failure; preserve this distinction.
Auth/token refresh success does not replace the server profile-role check.
Profile creation is deferred until after email verification; register() does not write users.
Never pass a null UID into downstream ownership fields.

| Shared API | Contract |
|---|---|
| `AuthGuard.checkAuthState()` | Returns AuthState from current user/email verification |
| `AuthGuard.evaluate(String email, boolean isEmailVerified)` | Pure state evaluator |
| `AuthGuard.isRoleAuthorized(UserRole role, UserRole... allowedRoles)` | Explicit role membership; null/empty rejected |
| `RoleRouter.getDestinationClass(UserRole role)` | Returns Activity class; legacy null maps to MainActivity |
| `RoleRouter.routeUserByRole(Activity activity, UserRole role)` | Starts destination and clears prior task |
| `RoleRouter.routeProfile(Activity activity, UserProfile profile)` | Routes validated profile; null opens profile creation |
| `UserRole.requireValid(String value)` | Case-insensitive/trimmed parse; throws for missing/invalid input |
| `UserRole.fromString(String roleStr)` | Legacy parser defaults invalid input to REPORTER; not remote validation |
| `UserProfile.getRoleEnum()` | Parsed role; use repository-validated profiles |
| `UserProfile.isComplete()` / `validate()` | Completeness flag / null or field error |
| `UserProfile.toMap()` | Model map; does not itself enforce role authorization |

AuthState values: NOT_LOGGED_IN, NON_UM_EMAIL, UNVERIFIED_EMAIL, AUTHENTICATED_AND_VERIFIED.
UserProfile has public no-arg and five-/six-string constructors, field getters/setters,
and getRole()/setRole(String), setRoleEnum(UserRole); timestamp accessors use java.util.Date.
The five-string constructor omits role; its local default is REPORTER.
Validators: UmEmailValidator.isValidUmEmail/normalizeEmail; UmPasswordValidator.validate/isValid.
Keep these interfaces stable; do not introduce a second user model or role enum.

## Implemented users contract

Collection/document: `users/{uid}`; document ID is the Firebase Authentication UID.
Repository reads set the model UID from the document ID.
The following are model/repository contracts, not proof of deployed rule validation.

| Field | Firestore value | Current contract |
|---|---|---|
| uid | string | Auth UID; immutable identity |
| email | string | UM email; normalization trims and lowercases domain only |
| displayName | string | Required by client validation; trimmed length 1–60 |
| faculty | string | Required by client validation; trimmed length 1–100 |
| residentialCollege | string | Required by client validation; trimmed length 1–100 |
| role | string | REPORTER, MAINTENANCE_STAFF or ADMIN |
| createdAt | timestamp | Server timestamp on creation; preserved on normal updates |
| updatedAt | timestamp | Server timestamp on profile save |

New profile documents receive REPORTER; client updates omit existing role assignments.
Privileged roles must be assigned through an authorized administrative process.
Profile fields are not passwords, tokens or credentials; never store those here.
isComplete() checks the three editable fields; it is not a universal dashboard gate.
Local rules also protect uid/email/createdAt on updates; see security status below.

## Module responsibilities and dependencies

| Module / owner | Scope | Integration dependency |
|---|---|---|
| 1 / Vincent | Auth, roles, profile | Shared Firebase identity and navigation |
| 2 / Jin Seng | Station Map & Directory | M1 eligibility; canonical station ID |
| 3 / Jun Jing | Issue Reporting | M1 reporter UID; M2 station ID |
| 4 / Jie Xin | Maintenance Tracking | M1 staff role; M3 report ID and M2 station ID |
| 5 / Zhe Yu | AI Report Assistance | M3 draft/report context; approved AI backend/configuration |
| 6 / Jun Onn | Community Participation | M1 UID; agree whether interactions attach to station or report |

These are responsibility/dependency proposals for future code, not existing public APIs.
Each module owner implements its feature; shared changes require affected-owner review.

## IMPLEMENTED Module 2 pilot contract

- Scope: FSKTM only. StationDirectoryActivity reads
  [m2_fsktm_stations.json](app/src/main/assets/m2_fsktm_stations.json).
- Local record fields: `stationId`, `name`, `building`, `floor`, `landmark`,
  `latitude`, `longitude`, `status`. These are asset fields, not an approved
  Firestore schema. Station is package-private, not a shared cross-module API.
- Preserve `fsktm-001` (Block B Entrance), `fsktm-002` (Block B Student Lounge),
  and `fsktm-003` (Block A Refill Station) in any later storage migration.
- Local status values: `AVAILABLE`, `OUT_OF_SERVICE`, `UNKNOWN`. All surveyed
  records currently use `UNKNOWN`; no student report or maintenance update is wired.
- Map/List retains the camera; identical coordinates share a count marker while
  List entries stay separate. Both views open read-only station details.
- Foreground location accepts approximate access. Startup centers within 500 m
  of FSKTM when a fix is available; otherwise FSKTM is the default. My location
  explicitly recenters. List sorts by straight-line distance, or name without a fix.
- Current implementation choices: 15-second map feedback timeout and 12-second
  location timeout. Offline map failure provides Retry while List stays usable.
- PROPOSED: reporter map landing through existing Module 1 interfaces, approved
  Firestore station reads, Module 3 selected-station handoff, and Module 4 confirmed
  status updates. Student reports should leave station status unchanged until
  staff/admin confirmation; that workflow is not yet implemented.

## PROPOSED station, report and maintenance contracts

The Firestore repositories, collections and status workflows below are not implemented.
The local Module 2 asset model above does not freeze these backend contracts.
Collection names mirror existing rules comments; those comments grant no access.
Affected owners and integration lead must approve fields, permissions and indexes first.

| PROPOSED path | Candidate fields; not a frozen schema |
|---|---|
| `refill_stations/{stationId}` | name: string, location: GeoPoint, status: string, createdAt/updatedAt: timestamp |
| `maintenance_reports/{reportId}` | stationId: string, reporterUid: string, description: string, status: string, createdAt/updatedAt: timestamp |
| `work_orders/{workOrderId}` | reportId: string, stationId: string, assignedStaffUid: string or null, status: string, createdAt/updatedAt: timestamp |

PROPOSED: retain the existing pilot station IDs as station document IDs; generate
a stable ID once for each new station/report/work order.
Never use a station display name, email address, list position or title as an identity.
PROPOSED relationships:
- reporterUid and assignedStaffUid reference `users/{FirebaseAuthUid}`.
- report.stationId references `refill_stations/{stationId}`.
- workOrder.reportId references `maintenance_reports/{reportId}`.
- workOrder.stationId must match the referenced report's stationId.
- AI drafts retain the selected station ID; the user confirms content before submission.
- Community target type/ID, moderation and duplicate-interaction policy remain undecided.

Firestore does not enforce foreign keys; future rules/backend checks must protect relationships.
Decide deletion/orphan handling, status enums/transitions and assignment authority together.
Photo storage, AI persistence/provider, community collections and indexes remain PROPOSED/TBD.
Do not create old item-lending collections such as items/borrow_requests from historical notes.

## Package, resource and shared-file ownership

IMPLEMENTED feature root: `com.example.umborrowbuddy.features.module2`.
PROPOSED roots: `features.module3` through `features.module6` under the same package;
keep existing module1/auth and module1/profile.
Java classes use PascalCase; methods/fields camelCase; resource names lowercase snake_case.
Use m1_ through m6_ for owned layouts, IDs, strings and drawables; existing names remain stable.
Example: `features.module2.StationDirectoryActivity`, `m2_activity_station_directory.xml`.
Only promote a model into shared.model after its consumers and owner agree the contract.
Shared-file review is mandatory for:
- AndroidManifest.xml, launcher/navigation, MainActivity and role dashboard wiring.
- Gradle scripts, version catalog, SDK/toolchain settings and dependency changes.
- AuthRepository/AuthGuard/RoleRouter, shared models, themes and common resources.
- Firebase project/configuration, firestore.rules and any future indexes/storage rules.

Integration lead and Firebase/rules owner are not designated in the inspected files.
The team must designate them; module ownership alone does not confer deployment authority.
Record approvals in the PR; do not overwrite another module's files to resolve conflicts.

## Git and review workflow

The current checkout is `um-refill-map` with its own project-local `.git` directory.
Inspected on 2026-11-11: branch `main`; origin `git@github.com:junonnnnn/um-refill-map.git`.
Detailed Module 2 acceptance evidence under `docs/` remains local and uncommitted.
Earlier parent-repository warnings describe historical checkouts, not this current boundary.
PROPOSED branch naming: feature/m2-station-directory, feature/m3-issue-reporting, etc.
Agree the integration target/default branch before opening or merging PRs.
1. Inspect root, branch, origin and dirty state before edits; preserve unfinished work.
2. Work on the approved owner branch; keep PRs limited to one reviewed integration step.
3. Run build/unit tests and include changed contracts plus separate manual-test evidence.
4. Obtain feature-owner review and integration-lead approval for shared-file changes.
5. Resolve conflicts with both owners; rerun affected checks before an authorized merge.
Agents must not stage, commit, push, switch branches or merge without explicit authorization.
Never force-push, reset another owner's work or modify a parent .git directory.

## Firebase onboarding

1. Ask the authorized project owner for access to the existing um-borrow-buddy project.
2. Obtain app/google-services.json from that owner through the approved private team channel,
   or download the matching Android app configuration in Firebase Console if authorized.
3. Confirm project_id=um-borrow-buddy and package_name=com.example.umborrowbuddy.
4. Place it in app/; it is ignored by Git. Never substitute a different project's JSON.
5. Confirm Email/Password Auth enablement, database ID/edition/location and role assignment process.
6. Sync and run against the agreed backend; use approved test identities and protect their data.
Do not put service-account credentials in the APK, repository, screenshots or agent messages.
Onboarding grants no permission to deploy rules, enable services or alter live user roles.

## Local rules and acceptance

Local firestore.rules is NOT CONFIRMED DEPLOYED OR TESTED.
It allows verified UM owners to read/create/update their own profile with identity restrictions.
Verified staff/admin may read user profiles; profile deletion and client role changes are denied.
Creation currently allows an omitted role or REPORTER; the repository always writes REPORTER.
It does not enforce every client field constraint; rules hardening needs separate review/tests.
All undeclared paths, including proposed Modules 2–6 collections, are denied by the local file.
Manual app success does not prove these local rules match production.

- [ ] Designate integration lead, Firebase/rules owner and merge target.
- [ ] Approve future schemas/IDs, status transitions, permissions and required indexes.
- [x] Build debug APK and pass existing plus changed-feature unit tests (2026-11-11: 45 passed).
- [x] Test the standalone Module 2 pilot on an API 37 emulator; see the local acceptance record.
- [ ] Verify Module 2 on physical hardware, including real location acquisition.
- [ ] Verify registration, verification, reset, profile persistence and logout on device.
- [ ] Verify all three role destinations, account switch, missing profile and failed/invalid role reads.
- [ ] Review/test rules for signed-out, unverified, owner, other-user, staff and admin identities.
- [ ] Test actual cross-module station/report/work-order identity relationships.
- [ ] Record device, backend, branch/build, expected/actual result and reviewer in the PR.
