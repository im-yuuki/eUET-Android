# eUET (Android)

A student-companion app for **VNU University of Engineering and Technology**, built with
**Jetpack Compose + Material 3 Expressive** (dynamic color / Material You) for a Pixel-native feel.

It aggregates the UET/VNU student backends into one app:

- **UET StudentHub** (`studenthub.uet.edu.vn`) — bearer-token JSON API (primary source).
- **VNU daotao** (`daotao.vnu.edu.vn`) — classic-ASP portal, HTML-scraped (login + scrapers ported
  from the iOS app's live-verified recipes; fallback source for profile/grades/exams).
- **Canvas** (`portal.uet.vnu.edu.vn`) — Canvas REST API via a user-supplied Access Token.

## Features

| Area | Status |
|---|---|
| Sign in — StudentHub (student ID + password + captcha, **or** Google via WebView) · VNU credentials · Canvas token | ✅ |
| Home dashboard (greeting, CPA, today's classes) | ✅ |
| Profile | ✅ |
| Timetable (per term) | ✅ |
| Grades + GPA | ✅ |
| Exams | ✅ |
| Notifications (infinite scroll) + News (images) | ✅ |
| Tuition / bills (invoice via Custom Tab) | ✅ |
| Canvas (courses, planner, missing, unread) | ✅ |
| Training points (term GPA; conduct scores pending portal capture) | ✅ |
| Documents (VNU syllabus PDFs) | ✅ |
| Offline cache + multi-provider aggregation | ✅ |
| Localization (vi base + en) | ✅ |
| Settings + sign out | ✅ |
| Course registration (dktn) | 🚧 deferred (matched with iOS) |

## Architecture

Single `:app` module, layered packages under `me.june8th.euet`:

- `core/` — `designsystem` (theme, reusable components), `network`, `datastore`, `common`, `model`
- `data/` — `source/studenthub` (Retrofit API, DTOs, mappers), `repository`, `auth`
- `feature/` — one package per screen: a stateless `XxxScreen` + `XxxViewModel` (`StateFlow<UiState>`)
- `navigation/` — nav host + routes; `di/` — manual DI container

**Manual DI** (`di/AppContainer` + `LocalAppContainer` + `euetViewModel { }`) instead of Hilt — the
AGP 9 built-in-Kotlin toolchain here supports neither KAPT nor a KSP build for Kotlin 2.4.0.
Storage is DataStore. Auth attaches a bearer token via an OkHttp interceptor.

## Build & run

```bash
./gradlew :app:assembleDebug      # build debug APK
./gradlew installDebug            # install on a connected device/emulator
```

Then launch the app and connect a provider: StudentHub (Google sign-in in the WebView), the VNU
portal (student ID + password), and/or Canvas (paste an Access Token from Canvas → Account →
Settings).

> **Live data:** the StudentHub auth/response shapes were reverse-engineered from HAR notes and are
> not yet verified against the live API. DTOs are lenient (nullable + `ignoreUnknownKeys`). If a
> screen shows an error or blanks after signing in, the response envelope or a field name may differ
> — check the `OkHttp` logcat output and adjust `data/source/studenthub/StudentHubDtos.kt`.
