# eUET (Android) — TODO

Status tracker against the implementation plan: Jetpack Compose + Material 3 app aggregating
**UET StudentHub**, **VNU daotao**, **Canvas**.

Sibling tracker: [`eUET-iOS/TODO.md`](../eUET-iOS/TODO.md) — **kept in sync; update both when
status changes.** Where one platform has verified something live, the other's TODO references it
as the porting recipe.

Legend: `[x]` done · `[~]` partial · `[ ]` not started

> **The shared gate:** StudentHub auth and response shapes were reverse-engineered from HAR notes
> and are **not yet verified against the live API on either platform**. Android has the full
> capture + endpoint stack built (lenient DTOs, `ignoreUnknownKeys`); iOS is blocked on the same
> unknown. The first platform to complete a real sign-in unblocks both.

## Cross-platform status (synced 2026-07-27)

| Capability | iOS | Android |
|---|---|---|
| App shell / theme / navigation | ✅ verified on device | ✅ builds & runs |
| StudentHub — web token capture | 🚧 placeholder (`WebLoginView`) | ✅ built (WebView header intercept) — live-unverified |
| StudentHub — data endpoints | 🚧 skeleton only | ✅ all wired — live-unverified |
| VNU daotao — auth + scrapers | ✅ **verified live on device** | ⛔ placeholder |
| VNU daotao — conduct scores (0–100) | 🚧 sub-tab request missing | ⛔ |
| Canvas | 🚧 skeleton | ⛔ placeholder |
| Documents / syllabus | ⛔ | ⛔ |
| Course registration (dktn) | ⛔ deferred | ⛔ placeholder |
| Notifications / News / Tuition UI | ⛔ placeholders | ✅ wired — live-unverified |
| Offline cache | ✅ SwiftData per-screen | ⛔ blocked (no Room in toolchain; alternative TBD) |
| Multi-provider aggregation | ✅ registry built (priority untested) | ⛔ not started |
| Localization (vi + en) | ⛔ planned | ⛔ planned |
| CI build workflow | 🚧 workflow written, uncommitted | ✅ committed — blocked on SDK 37.1 runner |
| Tests | ⛔ none | ⛔ none |

---

## Phase 0 — Foundation ✅ complete

- [x] Gradle: Compose BOM, Navigation-Compose, Retrofit + kotlinx.serialization, OkHttp, DataStore,
      Coil, Jsoup, splashscreen; version catalog + plugin aliases
- [x] Strip the Views template (fragments, XML layouts, nav graph, menu, ViewBinding)
- [x] `App` (Application) + `MainActivity` (ComponentActivity, edge-to-edge, splash), Compose entry
- [x] Theme: Material 3 + dynamic color (Material You), UET-blue fallback scheme
- [x] Design system: `EUetCard`, `SectionHeader`, `InfoRow`, `DetailScaffold`, state views, `TermSelector`
- [x] Nav shell: bottom bar (Home · Timetable · Grades · More), type-safe routes, `More` hub
- [x] Package split `me.june8th.euet.app` (UI) / `me.june8th.euet.core` (domain/data) — mirrors iOS App/Core
- [x] Builds a runnable debug APK (`:app:assembleDebug`)

**Deviations from the plan (toolchain-forced — see code comments):**
- [x] **Manual DI** (`di/AppContainer` + `LocalAppContainer` + `euetViewModel {}`) instead of Hilt —
      AGP 9 built-in Kotlin rejects KAPT and no KSP build exists for Kotlin 2.4.0.
- [x] **Standard `MaterialTheme`** instead of `MaterialExpressiveTheme` — the M3 *Expressive* APIs
      are `internal` in the resolved Compose BOM.
- [ ] Re-adopt Material 3 Expressive components once a BOM exposes them publicly (one-file change in
      `Theme.kt`; swap back the wavy `LoadingIndicator` in `StateViews.kt`)

---

## Phase 1 — StudentHub auth ✅ built (live-unverified) — *iOS ports from here*

- [x] `LoginScreen` WebView loads StudentHub; captures the bearer token from the SPA's own `/api`
      requests (`shouldInterceptRequest` reads the `Authorization` header; Chrome-like UA)
- [x] `SessionManager` (DataStore) persists token, student code, active term, Canvas token
- [x] **Token encryption at rest** — `TokenCipher` (AES-GCM, Android Keystore); legacy plaintext
      keys scrubbed; fails closed on decrypt error (≈ iOS Keychain storage)
- [x] `AuthInterceptor` attaches `Authorization: Bearer …`; **401 → drops the StudentHub session**
      so `RootViewModel` routes back to sign-in (no silent refresh path — token is WebView-captured)
- [x] Auth gate: `RootViewModel.authState` toggles Login ↔ main scaffold on `isLoggedIn`
- [ ] **Verify live that the token surfaces in the `/api` request header** (the capture bet — the
      same unknown iOS Phase 3 is blocked on). Fallback: read `localStorage`/`sessionStorage` via
      `evaluateJavascript`.
- [ ] Google may block OAuth in a WebView (`disallowed_useragent`) — fallback: Custom Tabs +
      deep-link redirect (iOS equivalent: `ASWebAuthenticationSession`)
- [ ] (Optional) parent-style `POST /api/auth/login` (username + password + captcha) as an alternate
      path — also listed on iOS

---

## Phase 2 — StudentHub core + Profile + Home ✅ wired (live-unverified)

- [x] Retrofit `StudentHubApi`, lenient DTOs, DTO→domain mappers, `StudentRepository`
      — *iOS Phase 3 ports its DTO/endpoint list from `core/data/source/studenthub/`*
- [x] `NetworkResult` + `safeApiCall` (friendly error mapping), `UiState` (≈ iOS `LoadState`)
- [x] **Profile** — avatar initial, student info card (`/api/student/detail`)
- [x] **Home dashboard** — greeting, CPA + credits snapshot, today's classes, quick actions
      (fans out profile + results + today's timetable)
- [ ] Confirm response envelope: if the API wraps in `{ "data": … }`, wire the ready-made
      `ApiEnvelope<T>` in `StudentHubDtos.kt` and change API return types
- [ ] Confirm the `weekday` scheme used for "today's classes" (assumed Mon=2 … Sun=8)
- [ ] Dashboard parity with the iOS full-dashboard goal: Canvas unread/missing, tuition alert,
      recent notifications (once those sources exist)

---

## Phase 3 — Timetable ✅ wired (live-unverified)

- [x] Term selector (chips), per-term `POST /api/student/tkb`
- [x] Grouped by weekday, sorted by period, class cards (course, period range, room)
- [x] Active term persisted to `SessionManager`
- [ ] Verify `sessionStart/End` → period/time mapping against real rows
- [ ] (Nice) a grid/day view in addition to the grouped list

---

## Phase 4 — Grades + GPA + Exams ✅ wired (live-unverified)

- [x] **Grades** — transcript grouped by term (`/api/student/kqht`), per-course score + letter
- [x] **GPA** — CPA / credits summary card (`/api/student/results`)
- [x] **Exams** — term selector, exam cards with date/time/room/seat/method (`/api/student/exam-schedule`)
- [ ] Verify point fields are numeric (DTO uses `Double?`; may arrive as strings → parse in mapper)
- [ ] (Nice) term-GPA trend chart — iOS computes a credit-weighted trend from the transcript;
      same derivation applies here

---

## Phase 5 — Notifications + News + Tuition ✅ wired (live-unverified)

- [x] **Notifications** — announcements list + unread dot (`/api/noti/user/{studentCode}`)
- [x] **News** — horizontal carousel (`/api/student/news`)
- [x] **Tuition** — outstanding-balance summary + per-bill cards, VND formatting (`/api/student/getAllBills`)
- [~] Notifications load **page 0 only** — no infinite scroll / mark-read yet (repo already takes a
      `page` arg; iOS specs the same list as "paginated, infinite scroll, mark-read")
- [ ] News images not rendered — Coil is a dependency but no `AsyncImage` is wired
- [ ] `invoiceUrl` on bills not opened — add tap action via Custom Tab (iOS: in-app Safari)

---

## Phase 6 — Canvas 🚧 placeholder only

- [x] `CanvasScreen` placeholder + "More" entry
- [ ] Decide auth: official Canvas Access Token vs. SAML/OAuth web capture (`portal.uet.vnu.edu.vn`)
      — same open question as iOS Phase 4; resolve once, apply to both
- [ ] `CanvasApi` + DTOs + mappers: `dashboard_cards`, `planner/items`, `missing_submissions`,
      `conversations/unread_count`
- [ ] Repository + ViewModel + real screen (active courses, upcoming assignments, unread inbox)
- [ ] Persist Canvas token/cookie (a `canvasToken` slot already exists in `SessionManager`)

---

## Phase 7 — VNU daotao adapter 🚧 placeholder only — *port from iOS (verified live)*

The iOS provider has been verified end-to-end on a device; port its recipes rather than re-derive:

- [x] `TrainingScreen` + `DocumentsScreen` placeholders + "More" entries
- [ ] Cookie-session login — iOS-verified recipe: `POST /dkmh/login.asp` with `chkSubmit="ok"` +
      GET-seed request first; persistent OkHttp `CookieJar`
- [ ] `isAuthenticated` via **negative-signature detection** (login form / expired notice in the
      response), not a positive marker — iOS-verified
- [ ] Profile scraper (`TabStdSelf.asp`) — real field names `StdCode`/`StdName`/`StdDob` + selected
      `<option>`s; also yields `hidStdID`/`UnivID` needed by the exam page — iOS-verified
- [ ] Grades scraper (`listpoint_Brc1.asp`) — headerless grid; resolve columns **relative to the
      course-code cell** — iOS-verified against 8 terms
- [ ] Exam scraper (`StdExamination.asp?selViewType=StdExam`) — locate table by header signature;
      needs `selUniv`/`selStd`/`vTermID` — iOS-built; **still unvalidated against a term with real
      exam rows on both platforms**
- [ ] Mid-session silent re-auth (stored credentials + retry-on-expiry) — iOS: `withReauth`
- [ ] **Training points** screen — term GPA from `TabStdStudy.asp`; **conduct scores (0–100) load
      under a sub-tab neither platform has captured yet** (open on iOS too)
- [ ] **Documents** screen — syllabus PDF listing (`Syllabus/default.asp`, first page only)
- [ ] daotao as fallback source for grades/exams where StudentHub lacks them (Jsoup dependency
      already present, unused)

---

## Phase 8 — Course registration 🚧 placeholder only

- [x] `RegistrationScreen` placeholder + "More" entry
- [ ] Wire `/api/student/dktn`, `/api/student/program`, `/api/student/programs/dky-window`
- [ ] DTOs + repository + ViewModel + screen (program structure, registration window, advising)
- *iOS: not started either (listed as deferred there); keep scope matched when picked up*

---

## Phase 9 — Offline cache, aggregation & polish 🚧 partial

- [x] **Settings** — about + **sign out** (confirmation dialog → clears session → back to Login)
- [ ] **Offline cache** — Room was planned but **cannot be used** (no annotation processor in this
      toolchain). Alternatives: hand-written DataStore/JSON snapshot cache, or SQLDelight (KSP-free).
      iOS ships offline-first SwiftData per screen — match that behavior, not necessarily the tech.
- [ ] **Multi-source aggregation** — a registry/priority layer merging StudentHub + daotao + Canvas
      per capability (port the iOS `ProviderRegistry` design). Not started.
- [ ] **Localization** — vi (base) + en via `strings.xml`; UI copy is currently hardcoded English.
      Same task open on iOS (`Localizable.xcstrings`). Locale-aware VND/date formatting throughout.
- [ ] Pull-to-refresh on list screens (only error-state "Retry" exists today)
- [ ] Loading skeletons (iOS has `LoadingRows`/`.redacted`; Android shows spinners only)
- [ ] Predictive-back opt-in; shared-element / expressive motion polish
- [ ] Empty/error copy pass; dark mode QA pass (iOS: verified)
- [ ] Self-contained `@Preview`s with fake repositories per screen (iOS lists the same gap)

---

## Cross-cutting / infra

- [x] GitHub Actions: build debug APK + upload artifact (`.github/workflows/android.yml`) — committed
- [ ] **CI is blocked** until the runner can fetch **SDK platform 37.1** (bleeding-edge toolchain)
- [ ] Tests — none yet (no unit tests for mappers/VMs, no Compose UI tests); same gap on iOS
- [ ] App icon refresh (template launcher; adaptive/monochrome pass)
- [ ] README "planned" table to update as Canvas/daotao/registration land
- [ ] Keep this tracker and `eUET-iOS/TODO.md` in sync when either platform's status changes

---

## Deferred / future (matched with iOS)

- [ ] Home-screen widget (today's classes / next exam) — Glance (iOS: WidgetKit)
- [ ] Exam-countdown ongoing notification (iOS: Live Activity)
- [ ] Push / periodic background refresh (WorkManager)

---

## Verification cheatsheet

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew installDebug
```

- Emulator/device opens to **sign-in** (no saved session). All StudentHub screens populate only
  after a real Google sign-in in the WebView — **not yet exercised on either platform**.
- daotao/Canvas screens show placeholder states until their adapters are built (Phases 6–7).
- Watch OkHttp `BASIC` logs in logcat to diagnose DTO/envelope mismatches on first live run.
