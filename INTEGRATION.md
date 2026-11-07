# Integration contract

Status: current source contract plus explicitly PROPOSED future contracts.
Reviewed against branch `vincent` on 2026-11-07.
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
- Modules 2–6 have dashboard placeholders only; their implementation is pending.
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
- Latest verified baseline: debug build and 43 local unit tests passed; device success is owner-reported.

## Actual navigation

1. Manifest launcher is LoginActivity; other declared Activities are not exported.
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

## PROPOSED station, report and maintenance contracts

No models, repositories, collections or status workflows below are implemented here.
Collection names mirror existing rules comments; those comments grant no access.
Affected owners and integration lead must approve fields, permissions and indexes first.

| PROPOSED path | Candidate fields; not a frozen schema |
|---|---|
| `refill_stations/{stationId}` | name: string, location: GeoPoint, status: string, createdAt/updatedAt: timestamp |
| `maintenance_reports/{reportId}` | stationId: string, reporterUid: string, description: string, status: string, createdAt/updatedAt: timestamp |
| `work_orders/{workOrderId}` | reportId: string, stationId: string, assignedStaffUid: string or null, status: string, createdAt/updatedAt: timestamp |

PROPOSED: generate a stable Firestore document ID once per station/report/work order.
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

PROPOSED feature roots under `com.example.umborrowbuddy`:
`features.module2` through `features.module6`; keep existing module1/auth and module1/profile.
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

Verified local root and Git directory: UMBorrowBuddy and its own UMBorrowBuddy/.git.
Branch inspected: vincent; origin: https://github.com/junonnnnn/um-borrow-buddy.git.
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
- [ ] Build debug APK and pass existing plus changed-feature unit tests.
- [ ] Verify registration, verification, reset, profile persistence and logout on device.
- [ ] Verify all three role destinations, account switch, missing profile and failed/invalid role reads.
- [ ] Review/test rules for signed-out, unverified, owner, other-user, staff and admin identities.
- [ ] Test actual cross-module station/report/work-order identity relationships.
- [ ] Record device, backend, branch/build, expected/actual result and reviewer in the PR.
