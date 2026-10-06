# Aurora modernization architecture

Aurora adopts the architecture principles demonstrated by Google's Now in Android sample while
retaining its existing mihomo runtime. The sample is a reference for application architecture and
UI engineering, not a source for VPN or proxy business logic.

## Non-negotiable boundary

- `:core` remains the existing mihomo, Go, JNI and CMake integration. It is treated as a stable
  runtime dependency and is not rewritten.
- VPN behavior and profile compatibility must remain functional throughout migration.
- Every migration stage must produce a buildable, installable application.

## Target layers

1. Compose UI: screen composables receive immutable UI state and emit user actions.
2. ViewModels: expose `StateFlow<UiState>` and process screen-level actions.
3. Repositories: provide the only UI-facing access to profiles, runtime state, settings and logs.
4. Data sources: adapt the existing Binder/service/database APIs into coroutine and Flow APIs.
5. Runtime: the existing `:service` and untouched `:core` modules.

Data flows upward. User actions flow downward. Compose code must not call Binder interfaces,
activities, databases or the mihomo runtime directly.

## Current state (2026-10-07)

- `:app` has a Compose single-activity shell. Home, Proxy, Connections and Settings are the four
  peer destinations. Profiles is a child destination reached from Settings.
- `:designsystem` supplies Material 3 theme tokens and reusable components.
- Home, Profiles and Connections have ViewModels. Connection queries/closing and profile import
  have repository or coordinator boundaries with unit tests. Proxy presentation and several
  runtime/settings actions are still coordinated by `MainActivity`.
- The existing `:service` and `:core` runtime remain in use. This is an incremental migration,
  not a replacement of the mihomo engine or VPN service.
- Legacy resources must be removed only when their remaining references have been audited.

## Remaining migration order

1. Preserve the regression baseline for navigation, profile import/rollback, connections and
   proxy-group expansion; add tests whenever a runtime action is moved.
2. Extract the remaining Proxy and settings orchestration from `MainActivity` in small,
   behavior-preserving slices. Keep Binder/service interactions behind adapters.
3. Add UI/screenshot coverage for the four top-level destinations and critical sheets.
4. Audit remaining legacy resources and remove only proven-unreferenced assets and configuration.
5. Profile startup and scrolling before considering module splits or new dependencies.

## Navigation semantics

- Home, Proxy, Connections and Settings are peer destinations and never animate like child pages.
- Profiles is a Settings child destination; returning to it must not overwrite the remembered
  top-level destination.
- Short input or source-selection tasks use Material bottom sheets or dialogs.
- System-owned tasks, such as choosing a local file, use the Android system picker.
- Advanced editing and detail views are child destinations and may use forward/back transitions.

## Dependency policy

- Prefer stable AndroidX and Compose releases.
- Upgrade the build toolchain separately from UI migration.
- Introduce dependency injection only after repository boundaries exist.
- Do not add a framework merely because Now in Android uses it; each dependency must solve an
  Aurora requirement.

## Deferred feature: hide app icon

The hide-app-icon feature is not complete. Disabling the launcher alias removed the system's
launch entry, but on a tested PixelOS launcher the icon remained visible on the home screen and
in the app drawer. The option is hidden from users who have not enabled it. Users who enabled it
in an earlier release retain a recovery action in General settings to restore the launcher alias.
Do not advertise this as a working privacy feature until launcher behavior and recovery are
validated on supported devices.

## Other deferred features

- In-app editing of proxy strategy groups/configuration is not part of the present migration.
- Connection-to-app attribution requires a reliable Android-side source before it can be
  advertised as exact; current labels are best effort.
- TLS interception/MITM is out of scope.
