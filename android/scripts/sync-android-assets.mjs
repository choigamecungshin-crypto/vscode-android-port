#!/usr/bin/env node
// sync-android-assets.mjs
// Copies an already-built out-mobile/ package into the Android shell asset dir so
// the WebView can load the VS Code mobile web bundle.
//
// Usage (from repo root):
//   node android/scripts/sync-android-assets.mjs
//
// 전제: 먼저 `BUILD_SOURCEVERSION="$(git rev-parse HEAD)" pnpm run bundle-mobile`
// 실행해 out-mobile/이 생성돼 있어야 한다. 이 스크립트는 번들링을 수행하지 않는다.

import { copyFile, mkdir, readdir, stat } from 'node:fs/promises';
import { dirname, join, relative, resolve } from 'node:path';
import { parseArgs } from 'node:util';

const { values: args } = parseArgs({
  options: {
    from: { type: 'string' },
    to: { type: 'string' },
    dry: { type: 'boolean' },
  },
});

const root = join(import.meta.dirname, '../..');
const from = resolve(args.from ?? join(root, 'out-mobile'));
const to = resolve(args.to ?? join(root, 'android/app/src/main/assets/vscode-mobile'));

const dry = args.dry ?? false;

async function isDirectory(path) {
  try {
    const s = await stat(path);
    return s.isDirectory();
  } catch {
    return false;
  }
}

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await walk(path));
    } else if (entry.isFile()) {
      files.push(path);
    }
  }
  return files;
}

async function main() {
  if (!(await isDirectory(from))) {
    console.error(`out-mobile root not found: ${from}`);
    console.error('Run first: BUILD_SOURCEVERSION="$(git rev-parse HEAD)" pnpm run bundle-mobile');
    process.exit(1);
  }

  const outFile = join(from, 'mobile.json');
  try {
    await stat(outFile);
  } catch {
    console.error(`out-mobile is missing mobile.json: ${outFile}`);
    process.exit(1);
  }

  // Safety: refuse to clobber paths we don't expect.
  const rel = relative(root, to);
  if (!rel || rel.startsWith('..') || rel.startsWith('/')) {
    console.error('Refusing unsafe destination:', to);
    process.exit(1);
  }

  if (!(await isDirectory(to))) {
    await mkdir(to, { recursive: true });
  }

  console.log(`[sync-android-assets] from ${from}`);
  console.log(`[sync-android-assets] to   ${to}`);

  const files = await walk(from);
  let copied = 0;
  let skipped = 0;
  for (const file of files) {
    const relPath = relative(from, file);
    const dest = join(to, relPath);
    if (dry) {
      console.log(`[dry] would copy ${relPath}`);
      skipped++;
      continue;
    }
    await mkdir(dirname(dest), { recursive: true });
    await copyFile(file, dest);
    copied++;
  }

  console.log(`[sync-android-assets] copied ${copied} files into assets/vscode-mobile/`);

  // out-mobile has no HTML entry: the web host supplies it. Copy the shell's host
  // page to the URL MainActivity loads; WebViewAssetLoader serves it over HTTPS at
  // https://appassets.androidplatform.net/assets/vscode-mobile/index.html.
  const hostPage = join(root, 'android/web/index.html');
  if (!dry) {
    await copyFile(hostPage, join(to, 'index.html'));
    console.log('[sync-android-assets] copied android/web/index.html -> assets/vscode-mobile/index.html');
  } else {
    console.log('[dry] would copy android/web/index.html -> assets/vscode-mobile/index.html');
  }
  if (dry) {
    console.log('[sync-android-assets] dry run complete');
  }
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
