# VS Code Android Port

## Goal

Turn this upstream Code - OSS checkout into an Android application that runs locally on ARM64 devices. Keep upstream desktop behavior intact unless a change is required for a shared cross-platform abstraction.

## Important technical decision

Do **not** try to compile Electron for Android. Electron does not provide an Android target. The first viable architecture is:

- **Android host:** Kotlin application, lifecycle, permissions, storage access, foreground service, notifications, back handling and native intents.
- **Workbench UI:** the existing VS Code web workbench rendered in Android WebView.
- **Local runtime:** an Android-compatible ARM64 Node.js runtime hosting the VS Code web/server components on loopback.
- **Native integration:** narrowly scoped bridges for filesystem, clipboard, keyboard, intents and terminal process management.
- **Terminal / PTY:** Android-compatible native modules, validated independently on real Android before integration.
- **Extensions:** define a supported extension-host/runtime matrix; do not promise desktop-native extensions will work on Android.

This is an implementation direction, not a claim that the current repository already contains a working Android app.

## Milestones

### M0 — Repository and agent baseline
- Preserve upstream history and source layout.
- Work on feature branches; never force-push or rewrite upstream history.
- Record every architectural decision and validation result.
- Keep Android-specific code isolated from shared workbench code where possible.

### M1 — Android feasibility spike
- Add a minimal Android app shell and WebView.
- Prove a local HTTP/WebSocket workbench session on an ARM64 Android device.
- Verify app lifecycle, process restart, loopback connectivity and offline startup.
- Capture logcat and screenshots for each test.

### M2 — Runtime and filesystem
- Package or bootstrap a compatible ARM64 Node runtime.
- Validate file access using Android Storage Access Framework and scoped storage.
- Test workspace open/save, search and file watching.
- Avoid assuming desktop paths or unrestricted shared-storage access.

### M3 — Terminal and extensions
- Bring up a real PTY and shell under the app UID.
- Verify Ctrl/Alt/Esc/Tab, function keys, IME and touch keyboard behavior.
- Establish extension compatibility tests and document unsupported native modules.

### M4 — Product integration
- Add mobile navigation, responsive layout, Android back handling, permissions and crash recovery.
- Test low-memory behavior, screen rotation, background/foreground transitions and battery restrictions.

### M5 — Release readiness
- Build debug APK in GitHub Actions.
- Run unit/instrumentation tests on an Android emulator where supported.
- Publish artifacts only from explicit release workflows; never store signing keys in the repository.

## Required validation for every change

1. State the exact Android behavior being changed.
2. Identify the affected VS Code layer and platform boundary.
3. Add or update a focused test.
4. Run the smallest relevant lint, typecheck or test command.
5. For Android host/runtime changes, attach build output plus logcat evidence; for UI changes, attach screenshots at phone-sized dimensions.
6. Report commands actually run and distinguish passed, failed, skipped and not available.

Do not claim an APK works unless it was built successfully and installed/launched on an Android emulator or device.

## Constraints

- Target ABI initially: `arm64-v8a`.
- Target Android baseline: choose and document a minimum API level before adding native dependencies.
- Development environment: GitHub Codespaces and GitHub Actions; do not require a personal desktop.
- Keep secrets out of source files, prompts, logs and artifacts.
- Do not commit generated build outputs, SDKs, NDKs, emulator images or downloaded runtime archives.
- Do not bulk-convert TypeScript to Kotlin. Reuse the web workbench and replace platform-specific Electron services incrementally.

## First engineering task

Before writing product code, inspect:
- `package.json`, `product.json` and build scripts
- `src/vs/code/electron` and `src/vs/server`
- existing web workbench entry points
- `build/` and `.devcontainer/`

Produce an architecture map identifying what can be reused, what depends on Electron/Node native modules, and what must be implemented in the Android host. Then propose a small M1 proof-of-concept with explicit build and device-test criteria.
