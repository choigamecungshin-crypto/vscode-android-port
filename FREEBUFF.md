# Freebuff agent in GitHub Codespaces

Freebuff is the interactive coding agent for this repository. This guide configures the workspace; it does not run an authenticated agent inside GitHub Actions.

## Start in Codespaces

Open this repository in a Codespace, then run:

```bash
node --version
npm --version
npm install -g freebuff
freebuff --help
freebuff
```

Inside Freebuff, use `/model` to inspect and select an available free model. Model availability, quotas and regional access are controlled by Freebuff and can change. If a free model is unavailable, select another model shown by the CLI; do not add a paid API key just to make this project work.

## First prompt

Use this as the first task:

> Read AGENTS.md, ANDROID_PORTING.md and .agent/skills/android-port/SKILL.md. Audit this repository for an Android port. Do not modify source code yet. Identify the Electron-only entry points, reusable web workbench/server components, native Node dependencies, build constraints and missing Android pieces. Return a dependency map and a small M1 proof-of-concept plan with exact commands and acceptance tests. Do not claim anything was built or tested unless you ran it.

## Agent permissions

Start with the normal permission prompts. Review proposed shell commands and file changes. Do not enable unrestricted execution until the workspace and task are understood. Never paste GitHub tokens, signing credentials or other secrets into the chat.

## Working rules

- Keep changes on `android-port/*` feature branches.
- Ask before changing the default branch, deleting files, force-pushing or adding paid services.
- Commit small, reviewable steps.
- Run targeted tests before larger builds.
- Use GitHub Actions artifacts for APKs; do not commit APKs or SDK/NDK archives.

## GUI testing

Freebuff alone does not provide an Android emulator. GUI tests require an Android emulator/device and ADB. First establish whether the Codespace runner has a usable Android SDK and emulator; hosted GitHub runners may not expose hardware acceleration. If unavailable, keep build/unit tests in Actions and arrange a separate emulator/device test runner. Always retain screenshots and logcat for GUI claims.
