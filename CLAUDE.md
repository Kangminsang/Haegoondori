# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 안내다. 프로젝트 배경은
[README.md](README.md)를 먼저 본다.

## 모듈 구조

- `core/` - 순수 Kotlin/JVM. Android SDK 없이 `./gradlew :core:test`로 바로 검증 가능.
- `app/` - Compose + Room + Hilt 기반 Android 앱. Android SDK 필요.

## Android SDK / 빌드

이 개발 환경에는 Android SDK가 `C:\Users\kangm\AppData\Local\Android\Sdk`에 설치돼
있다 (cmdline-tools, platform-tools, `platforms;android-35`, `build-tools;35.0.0`,
emulator, `system-images;android-35;google_apis;x86_64`). `local.properties`는
`.gitignore` 대상이라 커밋되지 않으므로, 새 워크트리에서는 아래 내용으로 직접
만들어야 한다:

```properties
sdk.dir=C\:\\Users\\kangm\\AppData\\Local\\Android\\Sdk
```

빌드/테스트:

```bash
./gradlew :core:test          # core 모듈 단위 테스트 (Android SDK 불필요)
./gradlew :app:assembleDebug  # app 모듈 디버그 APK 빌드
```

## 에뮬레이터로 실기 테스트하기

AVD `hagoondori_test`(Pixel 6 프로필, API 35 `google_apis` x86_64)가
`C:\Users\kangm\.android\avd\hagoondori_test.avd`에 이미 만들어져 있다. 화면
동작(터치, 네비게이션, 실제 저장/렌더링)은 컴파일 성공만으로는 확인되지
않으므로, UI를 건드리는 변경 후에는 가능하면 에뮬레이터에서 골든 패스를 직접
눌러본다 - 실제로 이 방식으로만 잡히는 버그가 있었다(아래 "알아둘 점" 참고).

### 에뮬레이터 실행

```bash
export JAVA_HOME="C:\Program Files\Java\jdk-22"
SDK_DIR="/c/Users/kangm/AppData/Local/Android/Sdk"
"$SDK_DIR/emulator/emulator.exe" -avd hagoondori_test -no-snapshot -gpu auto &
```

부팅 대기(최초 부팅은 1~2분 걸릴 수 있다):

```bash
ADB="$SDK_DIR/platform-tools/adb.exe"
for i in $(seq 1 24); do
  boot=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')
  [ "$boot" = "1" ] && break
  sleep 15
done
```

### 설치 및 실행

```bash
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" shell am start -n com.kangminsang.hagoondori/.MainActivity
```

### 조작

- `input tap X Y`, `input swipe X1 Y1 X2 Y2 DURATION_MS`, `input keyevent KEYCODE_BACK`
- `input text "ascii only"` - **한글은 지원 안 된다.** `KeyCharacterMap`에 한글
  매핑이 없어 `NullPointerException`이 난다. 버튼/저장 흐름 같은 기능 자체를
  검증할 때는 영문 테스트 문자열로 대체하고, 한글 문자셋·길이 제한 검증
  (`HangulRenderableValidator`, `TitleValidator` 등)은 이미 `core` 단위 테스트가
  다루므로 에뮬레이터에서 굳이 한글을 입력해볼 필요는 없다.
- `screencap -p`로 찍는 스크린샷은 **기기 실제 해상도**(예: 1080x2400)다. Read
  도구가 "displayed at 900x2000" 같은 축소 비율을 알려주면, 화면에서 읽은 좌표에
  그 배율(예: 1.2배)을 곱해야 실제 탭 좌표가 나온다. 이 환산을 매번 암산하면
  자주 빗나가므로, 중요한 버튼은 아래 방법으로 정확한 bounds를 구하는 편이 낫다.

### 정확한 탭 좌표 구하기

Compose UI도 접근성 트리에는 노출되므로, `uiautomator dump`로 정확한 bounds를
얻는 편이 화면 비율 암산보다 훨씬 안정적이다:

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell uiautomator dump /sdcard/ui.xml
MSYS_NO_PATHCONV=1 "$ADB" pull /sdcard/ui.xml ui.xml
grep -o 'text="버튼 텍스트"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' ui.xml
```

Git Bash는 `/`로 시작하는 인자를 자동으로 Windows 경로로 변환한다. `/sdcard/...`
같은 기기 쪽 절대경로를 adb에 넘길 땐 `MSYS_NO_PATHCONV=1`을 꼭 붙인다 - 안
붙이면 `/sdcard/ui.xml`이 `C:/Program Files/Git/sdcard/ui.xml`처럼 엉뚱하게
변환되어 조용히 실패한다.

### 크래시 확인

UI를 조작할 때마다 크래시 여부를 바로 확인한다:

```bash
"$ADB" logcat -d -t 60 *:E 2>&1 | grep -i "hagoondori\|FATAL"
```

`FrameTracker` 경고(`force finish cuj, time out`)는 에뮬레이터 자체의 프레임
벤치마크 로그이고 앱과 무관하니 무시해도 된다.

### 앱이 만든 파일 확인 (예: 장치 전송 페이로드)

`getExternalFilesDir` 아래에 쓰는 파일(`FakeExportAdapter`가 만드는
`calendar.txt` 등)은 `run-as` 없이 바로 읽을 수 있다:

```bash
MSYS_NO_PATHCONV=1 "$ADB" shell cat /sdcard/Android/data/com.kangminsang.hagoondori/files/calendar_preview/calendar.txt
```

앱 내부 저장소(`/data/data/com.kangminsang.hagoondori/...`)를 봐야 한다면
`run-as com.kangminsang.hagoondori`가 필요하다.

### 알아둘 점: 컴파일 성공 ≠ 화면이 맞게 그려짐

`DevicePreviewRenderer.kt`(장치 미리보기, F16)에는 컴파일도 되고 크래시도 나지
않지만 텍스트가 다른 도형과 다른 배율로 그려져 겹쳐 보이는 버그가 있었다 - `Sp`
단위 폰트 크기가 커스텀 캔버스 `scale()` 변환과 별개로 기기 실제 밀도를 한 번 더
곱해 먹었기 때문이다. `-gpu swiftshader_indirect`(소프트웨어 렌더링)와
`-gpu auto`(하드웨어 가속) 양쪽에서 똑같이 재현되는 걸 확인하고서야 에뮬레이터
렌더링 아티팩트가 아니라 진짜 로직 버그라고 확신할 수 있었다. 렌더링이 이상해
보이면 GPU 모드를 바꿔 재현되는지부터 확인하면 "에뮬레이터 탓"과 "진짜 버그"를
빠르게 구분할 수 있다.
