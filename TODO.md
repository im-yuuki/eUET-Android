# eUET (Android) — TODO

Status tracker against the implementation plan: Jetpack Compose + Material 3 Expressive app
aggregating **UET StudentHub**, **VNU daotao**, **Canvas**.

Sibling tracker: [`eUET-iOS/TODO.md`](../eUET-iOS/TODO.md) — **kept in sync; update both when
status changes.** Where one platform has verified something live, the other's TODO references it
as the porting recipe.

Legend: `[x]` done · `[~]` partial · `[ ]` not started

> **The shared gate (narrowed 2026-07-27):** a real HAR capture of the StudentHub SPA login landed,
> confirming a **credential login path that needs no Google account** — `GET /api/auth/captcha`
> then `POST /api/auth/login {userName, password, captchaId, captchaValue}`. Both platforms now
> implement it (user types the captcha; no solver). **Critical correction from the capture: the
> authenticated XHRs carried NO `Authorization` header**, implying a cookie session rather than the
> bearer scheme originally assumed — both clients now keep a cookie jar with the bearer attached
> only when a token exists. Response *bodies* were not captured, so the envelope and field names
> are still unconfirmed. A real sign-in is now a matter of typing student ID + password + captcha.

## Cross-platform status (synced 2026-07-27, post build-out)

| Capability | iOS | Android |
|---|---|---|
| App shell / theme / navigation | ✅ verified on device | ✅ builds & runs |
| Native design-language pass (2026-07-27) | ✅ Tab API (18+) + minimizable tab bar (26+), gated | ✅ M3 Expressive theme/motion + `LoadingIndicator` |
| StudentHub — web token capture | ✅ built (WKWebView fetch/XHR hook) — live-unverified | ✅ built (WebView header intercept) — live-unverified |
| StudentHub — credential login (ID + password + captcha) | ✅ built — 18 tests | ✅ built — 26 tests |
| StudentHub — data endpoints | ✅ all wired — live-unverified | ✅ all wired — live-unverified |
| VNU daotao — auth + scrapers | ✅ **verified live on device** | ✅ fully ported (bad-credential login verified live; success path pending real credentials) |
| VNU daotao — conduct scores (0–100) | 🚧 sub-tab request missing | 🚧 same (footer explains; term GPA shown) |
| Canvas (Access Token auth) | ✅ built — needs a real token to verify | ✅ built — needs a real token to verify |
| Documents / syllabus | ✅ built (QuickLook) — live-unverified | ✅ built (Custom Tab) — live-unverified |
| Course registration (dktn) | ⛔ deferred | ⛔ deferred (kept matched) |
| Notifications / News / Tuition UI | ✅ wired — live-unverified | ✅ wired (paging, images, invoices) — live-unverified |
| Offline cache | ✅ SwiftData per-screen | ✅ JSON snapshot cache per-screen |
| Multi-provider aggregation | ✅ registry built (priority untested live) | ✅ `AggregateRepository` (priority untested live) |
| Mixed-source conflicts (preferred source + diff view) | ✅ built — 13 detector tests | ✅ built — 16 detector tests |
| Motion / navigation transitions (reduced-motion aware) | ✅ zoom hero + crossfades + numericText | ✅ shared-axis nav + shared-element hero + morphing chips |
| Localization (vi base + en) | ✅ 146 + 22 keys | ✅ 123 strings + 2 plurals |
| CI build workflow | ✅ committed — runner iOS 26 SDK unverified | ✅ committed — blocked on SDK 37.1 runner |
| Tests | ✅ 91 unit tests (CoreTests) | ✅ 103 unit tests |

---

## Phase 0 — Foundation ✅ complete

- [x] Gradle: Compose BOM, Navigation-Compose, Retrofit + kotlinx.serialization, OkHttp, DataStore,
      Coil, Jsoup, splashscreen, androidx.browser; version catalog + plugin aliases
- [x] Strip the Views template; `App` + `MainActivity` (edge-to-edge, splash), Compose entry
- [x] Theme: Material 3 + dynamic color (Material You), UET-blue fallback scheme
- [x] Design system: `EUetCard`, `SectionHeader`, `InfoRow`, `DetailScaffold`, state views,
      `TermSelector`, `SkeletonRows`, `RefreshableBox`
- [x] Nav shell: bottom bar (Home · Timetable · Grades · More), type-safe routes, `More` hub
- [x] Package split `app` / `core` — mirrors iOS App/Core
- [x] Builds a runnable debug APK (`:app:assembleDebug`)

**Deviations from the plan (toolchain-forced — see code comments):**
- [x] **Manual DI** (`di/AppContainer` + `LocalAppContainer` + `euetViewModel {}`) instead of Hilt —
      AGP 9 built-in Kotlin rejects KAPT and no KSP build exists for Kotlin 2.4.0.
- [x] ~~Standard `MaterialTheme`~~ **Material 3 Expressive adopted** (2026-07-27):
      `MaterialExpressiveTheme` + `MotionScheme.expressive()` in `Theme.kt`; wavy `LoadingIndicator`
      replaces every indeterminate spinner. Requires pinning `material3 = 1.5.0-alpha24` over the
      BOM (Expressive is `internal` in the 1.4.x stables) — drop the catalog override once a stable
      material3 ships it.

---

## Phase 1 — StudentHub auth ✅ built (live-unverified) — *iOS ported 2026-07-27*

- [x] `LoginScreen` WebView captures the bearer from the SPA's own `/api` requests
- [x] `SessionManager` (DataStore) persists token, student code, active term, Canvas token
- [x] **Token encryption at rest** — `TokenCipher` (AES-GCM, Android Keystore); fails closed
- [x] `AuthInterceptor` attaches bearer; **401 → drops the StudentHub session**
- [x] Auth gate: `RootViewModel.authState`; sign-in UI: provider chooser → per-provider flows
- [ ] **Verify live that the token surfaces in the `/api` request header** (the capture bet — the
      shared gate). Fallback: read `localStorage`/`sessionStorage` via `evaluateJavascript`.
      *(iOS's port additionally hooks `fetch`/XHR and scans storage — reuse if the header
      intercept misses.)*
- [ ] Google may block OAuth in a WebView (`disallowed_useragent`) — fallback: Custom Tabs +
      deep-link redirect (androidx.browser now already a dependency). **Largely mooted** by the
      credential path below, which avoids Google entirely.
- [x] **Credential login (no Google account)** ✅ built 2026-07-27 from a real HAR capture:
      `GET /api/auth/captcha` → captcha id + base64 image (lenient DTO: field names unconfirmed) →
      `POST /api/auth/login {userName, password, captchaId, captchaValue}`. Sign-in offers both
      methods. **Both outcomes return HTTP 200**, so success is decided by a
      `GET /api/student/detail` probe fired right after the POST; the body only supplies the bearer
      (if any) and the error copy (captcha-specific vs. bad credentials). Captcha is displayed for
      the user to read — no solver/OCR. Challenge ids are single-use → refetched after every
      failure. **The captcha id is bound to the cookie the captcha response sets**, so one stable
      OkHttp client + `StudentHubCookieJar` serves both calls; cookies persisted encrypted
      (TokenCipher). `AuthInterceptor` skips `/api/auth/`, attaches the bearer only when present.
      26 parser unit tests.
- [x] **No silent re-auth on this path — by design, not an oversight**: re-login requires a fresh
      human-solved captcha, so a stored password cannot renew the session. The opt-in encrypted
      password only *prefills* the reconnect form so the user retypes just the captcha.

---

## Phase 2 — StudentHub core + Profile + Home ✅ wired (live-unverified)

- [x] Retrofit `StudentHubApi`, lenient DTOs, mappers, `StudentRepository`
- [x] `NetworkResult` + `safeApiCall` (+ `ErrorKind` for localized error copy), `UiState`
- [x] **Profile**, **Home dashboard** (greeting, CPA + credits, today's classes, quick actions)
- [x] Offline-first: cached snapshot shown instantly, refresh in background (2026-07-27)
- [ ] Confirm response envelope (`ApiEnvelope<T>` stands ready)
- [ ] Confirm the `weekday` scheme live (assumed Mon=2 … Sun=8; pinned by unit tests)
- [ ] Dashboard parity with iOS's extended dashboard (iOS now shows tuition alert + recent
      notifications on Home; Android Home doesn't yet — port when Home is next touched)

---

## Phase 3 — Timetable ✅ wired (live-unverified)

- [x] Term selector, per-term `POST /api/student/tkb`, grouped by weekday, active term persisted
- [x] Pull-to-refresh + skeleton loading + offline snapshot (2026-07-27)
- [ ] Verify `sessionStart/End` → period/time mapping against real rows
- [ ] (Nice) a grid/day view in addition to the grouped list

---

## Phase 4 — Grades + GPA + Exams ✅ wired (live-unverified)

- [x] **Grades** (`/api/student/kqht`), **GPA** (`/api/student/results`), **Exams** (term selector)
- [x] Pull-to-refresh + skeletons + offline snapshots; daotao fallback via aggregation (2026-07-27)
- [ ] Verify point fields are numeric live (DTO uses `Double?`)
- [ ] (Nice) term-GPA trend chart (iOS has one; same credit-weighted derivation applies)

---

## Phase 5 — Notifications + News + Tuition ✅ complete (live-unverified)

- [x] **Notifications** — paginated **infinite scroll** (near-end trigger, id-dedup, end
      detection), unread dots, pull-to-refresh (2026-07-27)
- [x] **News** — carousel with **Coil `AsyncImage`** thumbnails (placeholder/error states);
      app-wide `SingletonImageLoader` with OkHttp engine (2026-07-27)
- [x] **Tuition** — balance summary + bill cards, locale-aware VND; **`invoiceUrl` opens in a
      themed Chrome Custom Tab** (2026-07-27)
- [ ] Mark-read — endpoint unknown; capture it live first *(same on iOS)*

---

## Phase 6 — Canvas ✅ built (2026-07-27) — needs a real token to verify

- [x] **Auth decision resolved (both platforms): official Canvas Access Token**, pasted by the
      user (Canvas → Account → Settings → "+ New access token"); validated against
      `/api/v1/users/self` before storing (encrypted)
- [x] `CanvasApi` + lenient DTOs + mappers: `dashboard_cards`, `planner/items`,
      `missing_submissions`, `conversations/unread_count` (string-typed count handled)
- [x] `CanvasRepository` + `CanvasViewModel` + real screen (token entry ↔ dashboard: courses,
      upcoming, missing alert, unread chip, disconnect)
- [x] 401 with a stored token → token cleared → screen falls back to connect
- [ ] Verify against a live Canvas account (token generation permitted on the UET instance?)

---

## Phase 7 — VNU daotao adapter ✅ fully ported (2026-07-27) — *from iOS live-verified recipes*

- [x] Cookie-session login + negative-signature `isAuthenticated` + profile scraper (earlier)
- [x] **Grades scraper** (`listpoint_Brc1.asp`) — headerless grid, columns relative to the
      course-code cell; term-header interleaving; unit-tested against fixtures
- [x] **Exam scraper** (`StdExamination.asp?selViewType=StdExam`) — table by header signature;
      `selUniv`/`selStd`/`vTermID` from cached profile context (`hidStdID`/`UnivID`)
- [x] **Silent re-auth** — `authenticated {}` re-logins once with stored credentials on the
      expired-session signature and retries (≈ iOS `withReauth`)
- [x] **Training points screen** — term GPA from `TabStdStudy.asp` (+ transcript-derived
      credit-weighted fallback); explanatory footer for missing conduct scores
- [x] **Documents screen** — syllabus PDF listing (`Syllabus/default.asp`, first page); Custom Tab
- [x] daotao as fallback source for profile/grades/exams (via `AggregateRepository`)
- [ ] **Success path unverified** — needs real credentials entered by hand; only the
      invalid-credentials and network-failure branches have been exercised live
- [ ] Conduct scores (0–100) — sub-tab request still uncaptured *(open on iOS too; whoever
      captures it first shares the recipe)*
- [ ] Exam scraper still unvalidated against a term with real exam rows *(both platforms)*

---

## Phase 8 — Course registration ⛔ deferred (kept matched with iOS)

- [x] `RegistrationScreen` placeholder + "More" entry
- [ ] Wire `/api/student/dktn`, `/api/student/program`, `/api/student/programs/dky-window`
- [ ] DTOs + repository + ViewModel + screen — *pick up on both platforms together*

---

## Phase 9 — Offline cache, aggregation & polish ✅ complete (2026-07-27) except noted

- [x] **Settings** — about + sign out (clears session + snapshot cache)
- [x] **Offline cache** — `SnapshotCache` (JSON file-per-key, atomic writes, corruption-safe;
      Room impossible in this toolchain). Offline-first everywhere: cache → instant data →
      background refresh; errors only surface when no cache exists
- [x] **Multi-source aggregation** — `ProviderRegistry` + `AggregateRepository` (StudentHub →
      daotao fallback for profile/grades/exams; exams source-sticky per term list)
- [x] **Mixed-source conflict detection** (2026-07-27) — user-set preferred source (Settings →
      "Nguồn dữ liệu"; DataStore-persisted) reorders priority for profile/grades/exams; when both
      sources are connected the secondary is fetched concurrently and `ConflictDetector` compares
      (term+course matching, 0.005 numeric tolerance, name/term normalization, `onlyIn` for
      one-sided records — displayed data is never merged). Conflicts surface as a tappable banner
      + per-row badges → `ConflictDiffSheet` (both values labeled by source, preferred
      emphasized); reports cached under sibling keys so banners survive restarts. 16 detector
      unit tests. *Live behavior still gated on the StudentHub sign-in wall above.*
- [x] **Localization** — vi base + en (`values/` + `values-en/`), `ErrorKind`-based error copy,
      locale-aware VND + dates. *Leftover: a few rare raw-English error fallbacks (odd HTTP codes,
      scraper-specific messages) — localize if they ever surface in practice*
- [x] Pull-to-refresh (expressive indicator) on all list screens
- [x] Loading skeletons (`SkeletonRows`, reduced-motion aware)
- [x] Predictive-back opt-in (`enableOnBackInvokedCallback`)
- [x] Self-contained `@Preview`s with `PreviewData` fakes for every screen (incl. dark-mode and
      en-locale variants); stateless `XScreenContent` extraction
- [~] Dark mode: preview variants exist; full on-device QA pass still to do *(iOS: verified)*
- [x] **Motion pass** (2026-07-27) — `designsystem/motion/Motion.kt`: shared-axis nav transitions
      (fade-through between bottom-bar siblings, horizontal slide+fade for the 9 detail routes with
      exact `popEnter`/`popExit` mirrors so predictive back seeks the same spec); **one hero
      shared-element**: Home GPA/credits ↔ Grades GPA card (`SharedTransitionLayout` +
      `sharedBounds`, scopes published only by those two routes); `itemMotion()` on paginating /
      regrouping lists; `UiStateContent` crossfades via `AnimatedContent` keyed on the state *view*
      (so a refresh never crossfades a live list away); animated conflict banner; real expressive
      `FilterChip` shape-morph on the term selector; springy bottom-bar icon selection;
      `AnimatedValueText` rolls GPA/credits/Canvas counts with direction from the delta.
      **All of it gated by `rememberReducedMotion()`** (`ValueAnimator.areAnimatorsEnabled()`),
      which `Skeleton.kt` was refactored onto. Deliberately skipped a full-screen container
      transform (remeasuring Scaffold+LazyColumn per frame is the jank-prone version).

---

## Cross-cutting / infra

- [x] GitHub Actions: build debug APK + upload artifact — committed
- [ ] **CI is blocked** until the runner can fetch **SDK platform 37.1** (bleeding-edge toolchain)
- [x] Tests — **61 JVM unit tests** (daotao scrapers w/ HTML fixtures, StudentHub + Canvas
      mappers/DTO leniency, error mapping). Registry/aggregate need an interface seam to be
      testable — extract if logic grows
- [x] App icon — already adaptive + monochrome with the UET crest (tracker note was stale;
      verified 2026-07-27, no rework needed)
- [x] README feature table updated (2026-07-27)
- [ ] Keep this tracker and `eUET-iOS/TODO.md` in sync when either platform's status changes

---

## Blocked on live access (cannot be completed offline — the remaining wall)

1. StudentHub: one real sign-in (now just student ID + password + captcha — no Google needed) →
   confirms the response envelope, field names, weekday scheme, and whether the session is really
   cookie-based. **This is the cheapest remaining unblock.**
2. VNU daotao: real credentials → confirm login success path, grades/exams against real rows
3. Conduct-scores sub-tab request capture (either platform)
4. Canvas: a real Access Token on the UET instance
5. Notifications mark-read endpoint capture
6. CI runner with SDK 37.1

---

## Deferred / future (matched with iOS)

- [ ] Course registration (dktn) — see Phase 8
- [ ] Home-screen widget (today's classes / next exam) — Glance (iOS: WidgetKit)
- [ ] Exam-countdown ongoing notification (iOS: Live Activity)
- [ ] Push / periodic background refresh (WorkManager)

---

## Verification cheatsheet

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew installDebug
```

- Fresh install opens to **sign-in** (provider chooser). StudentHub screens populate only after a
  real Google sign-in — **not yet exercised on either platform**. VNU sign-in works against the
  live portal (bad credentials rejected); a successful login needs real credentials.
- Canvas connects with a pasted Access Token.
- Watch OkHttp `BASIC` logs in logcat to diagnose DTO/envelope mismatches on first live run.
