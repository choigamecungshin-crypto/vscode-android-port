# VS Code Agents Instructions

This file provides instructions for AI coding agents working with the VS Code codebase.

For detailed project overview, architecture, coding guidelines, and validation steps, see the [Copilot Instructions](.github/copilot-instructions.md).

Prefer `CancellationToken`/`CancellationTokenSource` internally and adapt `AbortSignal` only at native or third-party API boundaries; see the [cancellation guidance](.github/copilot-instructions.md#cancellation).

## Android port work

For Android-port tasks, read [ANDROID_PORTING.md](ANDROID_PORTING.md) and the [android-port agent skill](.agent/skills/android-port/SKILL.md) before changing code. Use [FREEBUFF.md](FREEBUFF.md) for the Codespaces Freebuff workflow.

Keep Android host/runtime work isolated from shared workbench code. Do not attempt to build Electron for Android. Work on feature branches and report only validations that were actually run.
