# Module 1 Verification Report — UM Water Refill Reporting & Maintenance System

> **HISTORICAL / SUPERSEDED — earlier verification snapshot retained below.**
> Latest verified baseline (2026-11-07): debug build passed; 43 local tests passed, zero failures.
> Suites: ExampleUnitTest 1; AuthGuardTest 5; ProfileRoleRetrievalTest 4; RoleRouterTest 5;
> UmEmailValidatorTest 10; UmPasswordValidatorTest 8; UserProfileTest 7; UserRoleTest 3.
> Vincent reports successful physical-device Module 1 testing, including all three roles.
> Corrections to the original snapshot: registration does not write users; verified profile save creates the document.
> Local rules deny client role changes even for ADMIN; privileged assignment requires an authorized admin process.
> The older claim about guards on every Activity/onStart is too broad; see current navigation contracts.
> Local rules deployment/testing is NOT CONFIRMED; the original “Not Deployed” heading is not deployment evidence.
> Modules 2–6 identity fields in the body are proposals, not implemented schemas or APIs.
> See [INTEGRATION](../../INTEGRATION.md); the original 37-test snapshot below is historical, not the latest total.


**Date:** 2026-11-07
**Author:** Vincent Loh
**Branch:** `vincent`
**Build Status:** BUILD & TESTS PASSED (37 Unit Tests Passed)

## 1. Executive Summary

Module 1 (Authentication, User Roles & Profiles) for the UM Water Refill Station Reporting & Maintenance System is fully implemented and unit-tested.

### Implemented Features:
1. **UM Domain Authentication:** Registration strictly limited to `@siswa.um.edu.my` and `@um.edu.my` emails.
2. **Password Policy Enforcement:** Password validation requiring **>= 8 characters, 1 uppercase letter, 1 lowercase letter, and 1 number** with clear error messages via `UmPasswordValidator`.
3. **Email Verification Gate:** Mandatory verification email dispatch and gate checks via `VerifyEmailActivity` and `AuthGuard`.
4. **Three User Roles:** Defined `REPORTER`, `MAINTENANCE_STAFF`, and `ADMIN` in `UserRole`. Stored in Firestore document `users/{uid}` under field `role`. Default role is `REPORTER`.
5. **Role Protection & Anti-Self-Assignment:**
   - Registration writes initial document with `role = "REPORTER"`.
   - `saveUserProfile()` ignores client-submitted role overrides, preserving existing Firestore role.
   - Local `firestore.rules` proposed with `request.resource.data.role == resource.data.role` enforcement for updates.
6. **Role-Based Navigation & Access Control:**
   - `REPORTER` → `MainActivity` (Shared / Reporter Portal)
   - `MAINTENANCE_STAFF` → `StaffDashboardActivity` (Staff Work Orders Dashboard)
   - `ADMIN` → `AdminDashboardActivity` (System Admin Control Panel)
   - Guard checks on every activity (`onStart`) enforce session, verification, and role authorization.

## 2. Unit Test Results

Executed via `./gradlew app:testDebugUnitTest`:
- Total Tests Executed: **37**
- Passed: **37**
- Failed: **0**

### Test Suites:
- `UmEmailValidatorTest` (10 tests passed)
- `UmPasswordValidatorTest` (8 tests passed)
- `AuthGuardTest` (5 tests passed)
- `UserProfileTest` (7 tests passed)
- `UserRoleTest` (3 tests passed)
- `RoleRouterTest` (4 tests passed)

## 3. Security Limitations & Propositions

1. **Security Limitations:**
   - Client-side code enforcement is backed by `AuthGuard` and `AuthRepository`. However, true database security relies on Firestore Security Rules.
2. **Proposed Firestore Rules (Not Deployed):**
   - Rules prevent users from setting `role` to anything other than `REPORTER` on creation and prevent updating `role` unless the request comes from an `ADMIN`.

## 4. Integration Steps for Modules 2–6

1. **Owner Identification:**
   Use `AuthRepository.getCurrentUid()` to populate `createdByUid` or `reporterUid` when submitting station reports (Module 2/3) or work orders (Module 4/5).
2. **Role Verification:**
   Call `AuthGuard.isRoleAuthorized(profile.getRoleEnum(), UserRole.MAINTENANCE_STAFF, UserRole.ADMIN)` before allowing staff/admin actions.
3. **User Profile Lookup:**
   Use `AuthRepository.getUserProfile(uid, callback)` to display reporter or maintenance staff details in reports or work order lists.
