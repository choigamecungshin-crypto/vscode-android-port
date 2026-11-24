# Android shell (M1)

Kotlin + WebView 앱 셸로, 기존 VS Code 모바일 웹 번들(`out-mobile/`)을 호스트한다.
Compose 에디터 재작성 없음. Electron 없음.

## 빌드

```sh
export JAVA_HOME="$HOME/.jdk-21"
export ANDROID_HOME="$HOME/.android/sdk"
export ANDROID_SDK_ROOT="$HOME/.android/sdk"
cd android
./gradlew :app:assembleDebug
```

생성물: `android/app/build/outputs/apk/debug/app-debug.apk`

## SDK/도구 버전(이 작업공간 기준)
- JDK: Amazon Corretto 21.0.13.1 (`$HOME/.jdk-21`)
- Android cmdline-tools + platform 36 + build-tools 36.1.0 (`$HOME/.android/sdk`)
- Gradle wrapper: 8.11.1 (`android/gradle/wrapper/`)
- AGP: 8.9.2
- Kotlin: 2.0.20
- AndroidX activity: 1.9.3

## 번들 동기화(M1-2)
WebView가 실제 VS Code 모바일 번들을 로드하려면 먼저 `out-mobile/`을 만들고 자산 디렉터리로 복사해야 한다.

1. 번들 생성(repo 루트):
   ```sh
   BUILD_SOURCEVERSION="$(git rev-parse HEAD)" pnpm run bundle-mobile
   ```
2. Android 자산으로 복사:
   ```sh
   node android/scripts/sync-android-assets.mjs
   ```
3. 다시 빌드:
   ```sh
   ./gradlew :app:assembleDebug
   ```

주의: 현재 `out-mobile/`이 없으면 디버그 APK는 `assets/vscode-mobile/`에
placeholder 파일만 담는다. 이 경우 WebView는 안내 HTML을 로드하며, 실제
VS Code 모바일 워크벤치는 뜨지 않는다. 이는 M1에서 의도된 상태다.

## 현재 범위/미검증
- APK 빌드는 성공하지만, 실제 기기/에뮬레이터에서 WebView가 `out-mobile` 번들을
  제대로 렌더링하는지는 **미검증**이다(에뮬레이터·실기기·ADB 부재로 이 환경에선
  실행 검증 불가).
- `WebViewAssetLoader` 기반 HTTPS origin 서빙은 아직 적용하지 않았다.
  현재 asset 서빙은 `file:///android_asset/` 기반이다. M1에서는 작동하지만,
  향후 확장 호스트 same-origin 요건을 위해 HTTPS origin으로 전환 예정이다.
- 백 버튼 처리는 WebView 뒤로 가기 우선 → 비어 있으면 액티비티 종료 순.
- 클립보드/IME/키보드 가림 대응은 이후 마일스톤(M2~M3)에서 구현한다.
