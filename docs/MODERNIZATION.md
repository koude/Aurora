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

## Module migration map

- `:app`: single-activity application shell and top-level navigation.
- `:designsystem`: Aurora Material 3 tokens and reusable Compose components.
- Future `:data`: repository implementations and service adapters.
- Future feature modules: home, proxy, profiles and settings.
- Legacy `:design`: XML/Data Binding screens retained only until their Compose replacements ship.
- Existing `:service`: retained first, then simplified behind repositories without changing behavior.
- Existing `:core`: retained unchanged.

## Migration order

1. Establish the Compose design system and dependency baseline.
2. Add repository contracts and adapters around existing service/Binder calls.
3. Build a single-activity Compose shell with four parallel top-level destinations.
4. Migrate Profiles first because its service operations and modal navigation are well bounded.
5. Migrate Home, Proxy and Settings in that order.
6. Migrate secondary screens and replace activity transitions with destination or modal semantics.
7. Remove legacy Data Binding layouts and obsolete activities only after behavior parity tests pass.
8. Add screenshot, ViewModel, repository and navigation tests; then add baseline profiles.

## Navigation semantics

- Home, Proxy, Profiles and Settings are peer destinations and never animate like child pages.
- Short input or source-selection tasks use Material bottom sheets or dialogs.
- System-owned tasks, such as choosing a local file, use the Android system picker.
- Advanced editing and detail views are child destinations and may use forward/back transitions.

## Dependency policy

- Prefer stable AndroidX and Compose releases.
- Upgrade the build toolchain separately from UI migration.
- Introduce dependency injection only after repository boundaries exist.
- Do not add a framework merely because Now in Android uses it; each dependency must solve an
  Aurora requirement.
