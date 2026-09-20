# 해군돌이

해군 복무자를 위한 군생활 관리 앱. 남은 복무 일수, 휴가·전투휴무·6주 외박 주기·
월별 외출 횟수를 관리하고, 자작 e-ink 탁상 캘린더 장치에 USB로 데이터를 밀어
넣는(sync) 컴패니언 역할을 겸한다.

설계 근거와 모든 규정 수치의 출처는 프로젝트 설계 문서(v4.0)에 있다. 이 저장소의
코드는 그 문서를 그대로 구현한 것이며, 아래 각 섹션에서 "스펙 N장"이라고 표기한
것은 그 문서를 가리킨다.

## 이 저장소를 처음 열었다면

**이 프로젝트는 `core`(순수 Kotlin/JVM)와 `app`(Android) 두 모듈로 나뉜다.**

```
Hagoondori/
├── core/   출타 계산 로직, 데이터 모델, 장치 전송 페이로드 인코딩.
│           Android 의존성이 전혀 없다. ./gradlew :core:test 로 바로 검증 가능.
└── app/    Compose + Room + Hilt 기반 Android 앱. Android Studio가 필요하다.
```

이렇게 나눈 이유는 순전히 실용적이다: 출타 규정 계산(6주 외박 차수 매칭 등)과
장치 전송 포맷은 이 앱에서 가장 정교하고 실수하면 안 되는 부분인데, 그 로직이
Android에 전혀 의존하지 않으므로 `core`에 두면 에뮬레이터나 실기기 없이도
`./gradlew :core:test` 한 줄로 실제 컴파일·테스트가 가능하다. `app` 모듈은 처음
여러 커밋에 걸쳐 Android SDK를 설치할 수 없는 네트워크 정책 하의 개발 환경에서
작성되어 한 번도 빌드해 본 적이 없었으나, 이후 Android SDK(cmdline-tools,
platform-tools, `platforms;android-35`, `build-tools;35.0.0`)를 받아 `./gradlew
:app:assembleDebug`로 첫 빌드를 실제로 통과시켰다 - 이 과정에서 발견된 실제
컴파일 오류 2건(`app/src/main/res/values/themes.xml`의 존재하지 않는 플랫폼
테마 `Theme.DeviceDefault.DayNight.NoActionBar` 참조, `SyncStatusBanner.kt`의
`androidx.compose.foundation.layout.weight` 불필요한 import가 Compose 내부
프로퍼티와 이름이 충돌하던 문제)를 수정했다. 다만 이는 컴파일 성공을 의미할
뿐, 실기기/에뮬레이터에서 화면이 의도대로 동작하는지는 여전히 확인되지 않았다 -
**Android Studio에서 에뮬레이터나 실기기로 실행해 UI 동작을 확인해야 한다.**

## 빠른 시작

### 1. core 모듈 검증 (Android Studio 없이 바로 가능)

```bash
./gradlew :core:test
```

도메인 모델, 4개 계산기(휴가/전투휴무/외박/외출), 문자셋 검증기, 페이로드
인코더/CRC32, 공휴일 API 응답 파서까지 90개 이상의 단위 테스트가 여기 있다.
특히 `OvernightScheduleCalculatorTest`는 6주 외박 주기의 지연·선행·소멸이 뒤섞인
케이스를 정밀하게 검증한다 - 이 앱의 존재 이유에 해당하는 로직이다.

### 2. app 모듈 (Android Studio 필요)

1. Android Studio에서 이 저장소를 연다. (Gradle sync 시 최신 AGP/Compose 버전
   업그레이드를 제안하면 받아들여도 무방하다 - `gradle/libs.versions.toml`의
   버전은 Google Maven 저장소에 접근할 수 없는 환경에서 작성되어 실제 다운로드로
   검증하지 못했다.)
2. (선택) 공휴일 자동 조회를 쓰려면 앱 실행 후 설정 화면의 "공공데이터포털 서비스키"에
   키를 입력한다. 키는 빌드(APK)나 소스에 들어가지 않고 이 기기의 앱 전용 저장소에만
   보관된다. 키가 없어도 앱은 완전히 동작한다 - 공휴일은 내장 데이터(고정 양력 공휴일)와
   수동 입력으로 대체된다(스펙 4.14절 확보 우선순위). 키가 있으면 앱을 켤 때 7일마다
   자동으로 갱신한다.
3. `app` 모듈을 실행한다. minSdk 33(Android 13) 이상 기기/에뮬레이터가 필요하다.

### 3. 장치 연동 검증 (실기기 필요, 스펙 8.3절)

USB MSC 인식·SAF 접근 흐름(`RealExportAdapter`, `export/DeviceStorageAccess.kt`)은
기기·OS·장치 펌웨어에 따라 동작이 달라질 수 있어 반드시 실기기로 검증해야 한다.
장치 화면 미리보기(F16)는 실사용에 필요하지 않아 제거했다. 페이로드 내용은
`FakeExportAdapter`가 앱 전용 저장소에 남기는 `calendar.txt`로 확인할 수 있다.

## 이 개발 환경에서 검증한 것 / 못한 것

| 구분 | 상태 |
|---|---|
| `core` 모듈 컴파일·전체 단위테스트 | ✅ 이 환경에서 실제로 실행하고 확인함 |
| `core`의 페이로드 인코더가 스펙 7.2절 예시와 정확히 일치 | ✅ 골든 테스트로 확인함 |
| `app` 모듈 컴파일(Compose/Room/Hilt KSP, `assembleDebug`) | ✅ Android SDK를 받아 실제로 빌드 성공까지 확인함 (컴파일 오류 2건 수정) |
| `app` 모듈의 에뮬레이터 실행 및 UI 동작 | ✅ API 35 x86_64 에뮬레이터(adb)에서 실제로 확인함 - 아래 "에뮬레이터에서 확인한 것" 참고 |
| 공공데이터포털 특일정보 API 실호출 | ❌ 실제 스키마 미확인 - `HolidayApiPayloadParser`가 알려진 문서 기반으로 방어적으로 작성됨 |
| 전송 범위 계산(`TransmissionRange`), 장치 검증 규칙 미러(`PayloadVerifier`: 빈 파일·절단·CRC 불일치) | ✅ `core` 단위 테스트로 확인 |
| `calendar.txt` 전송 명세 v5 반영: `G`(외출) 레코드, 전송 범위(동기화 달 1일 ~ +180일이 속한 달 말일), 16,000바이트 상한 | ✅ 명세 부록 예시 파일과 바이트 단위로 일치(`Z|531|B78A4E13`)하는 골든 테스트 |
| USB MSC 인식 및 SAF `calendar.txt` 쓰기, 쓴 뒤 재읽기 검증, 장치 폴더(루트) 선택 검증(`DeviceFolderCheck`) | ❌ 실기기 필요 (컴파일만 확인. 에뮬레이터는 USB／실제 SAF 트리를 흉내낼 수 없음) |
| `device_filter.xml`의 USB vendor-id(0x2E8A) | ❌ product-id는 실기기에서 확인 필요 |
| 장치(e-ink) 실물에서의 800×480 렌더링 | ❌ 실기기 필요 |

### 사용 후 반영한 변경 (실기기 사용 피드백)

- 휴가 종류(정기/포상/위로)는 DB를 열 때 미리 채워 두고, 종류 직접 추가 화면은 제거했다.
- 장치 미리보기 화면(F16)을 제거했다.
- 외박은 시작~종료일 기간으로 기록하며(DB 버전 2, `overnight_record.endDate`), 달력과
  장치 페이로드(`L` 레코드, 라벨 `외박`)에 휴가처럼 기간으로 표시된다. 6주 차수 매칭은
  시작일만 쓰고 휴가 잔여 일수는 차감하지 않는다.
- 외출은 달력에서 날짜를 골라 바로 기록/삭제할 수 있고, 장치에는 `G|날짜`로 전달된다.
  근무입력 탭에서도 여러 날짜를 골라 외출을 일괄 기록할 수 있다.
- 외박은 주(차수) 단위로만 센다. 며칠 지연/선행 같은 일 단위 추적은 화면에서 뺐다
  (차수 매칭 계산 자체는 그대로).
- 포상/위로휴가는 부여 내역(날짜·일수·내용)을 카드에서 볼 수 있고, 부여마다 유효 기간(선택)을
  둘 수 있다. 사용은 유효 기간이 먼저 끝나는 부여부터 차감하고, 기간이 지난 미사용분은
  소멸로 표시된다(DB 버전 4, `LeaveCalculator.summarize`).

### 에뮬레이터에서 확인한 것 (2026-09-19)

Android SDK로 API 35(`google_apis`, x86_64) 에뮬레이터(`hagoondori_test` AVD)를
직접 띄우고 `adb`로 조작해 다음을 실기기와 동일한 방식으로 확인했다 - 매 조작마다
`logcat`으로 크래시(FATAL/AndroidRuntime)가 없는지도 함께 확인했다.

- 최초 실행 시 빈 상태 안내(복무 정보 없음) → 설정에서 입대일/전역일 입력 →
  저장 → 대시보드 D-day/외출 잔여가 실시간으로 갱신되는 전체 흐름
- 달력 화면에서 일반 일정(F4) 추가 → 달력 셀에 마커 표시 → 삭제까지, 이 세션에서
  새로 구현한 기능이 실제로 Room에 저장되고 화면에 반영됨을 확인
- 출타관리 화면에서 휴가 종류 추가
- 근무입력 화면에서 여러 날짜를 골라 당직 일괄 저장 → 선택한 날짜의 다음 날에
  비번이 자동으로 추가되는 로직(4.16절)이 실제로 동작함을 확인
- 설정 → 장치 미리보기(F16, 밤/낮 테마 전환)와 "페이로드 파일로 저장" →
  앱 전용 저장소에 생성된 `calendar.txt`를 직접 열어 `V/M/P/D/Z` 형식이 스펙
  그대로 나오는 것까지 확인
- **버그 발견 및 수정 2건** (둘 다 "한 번도 컴파일된 적 없는 코드"에서 나온 실수):
  1. `app/src/main/res/values/themes.xml` - 존재하지 않는 플랫폼 테마
     `Theme.DeviceDefault.DayNight.NoActionBar` 참조 (컴파일 자체가 안 됨)
  2. `DevicePreviewRenderer.kt` - 장치 미리보기 헤더 텍스트("전역 D+…")가 다른
     도형과 달리 실제 화면 밀도로 측정된 뒤 캔버스 배율이 한 번 더 곱해져,
     텍스트만 2~3배 커져 달력 그리드와 겹쳐 보이는 버그. `sizeSp`를 밀도로
     나눠 상쇄하도록 고쳤다 - GPU 가속 여부와 무관하게 재현되었으므로 에뮬레이터
     렌더링 아티팩트가 아니라 실제 로직 버그였다.

## 아키텍처 한눈에 보기

- **`core/model`** - 스펙 4장의 데이터 모델. 파생값(D-day, 차감일수, 외박
  예정일 등)은 저장하지 않고 매번 계산한다.
- **`core/calc`** - 출타 계산 로직(3장). `OvernightScheduleCalculator`가
  6주 외박 차수 매칭 알고리즘의 핵심이다.
- **`core/export`, `core/payload`** - 장치 연동 계약(6/7장). `PayloadEncoder`가
  `calendar.txt` 텍스트 포맷을 만들고, `Crc32`가 무결성 검증값을 계산한다.
- **`core/holiday`** - 공휴일 확보 전략 중 "해석"과 "폴백 데이터" 부분만
  (네트워크 호출 자체는 app에 있음 - 테스트 가능하게 하려고 분리함).
- **`app/data`** - Room(로컬 저장) + Repository(원본 CRUD만, 계산 없음).
- **`app/ui`** - Compose 5개 화면(대시보드/달력/출타관리/근무입력/설정) +
  ViewModel. ViewModel이 Repository의 Flow를 모아 `core.calc`에 넘기고 결과만
  노출한다 - 계산 로직 자체는 UI 계층에 없다.
- **`app/export`** - `SnapshotBuilder`(여러 Repository → `CalendarSnapshot`),
  `FakeExportAdapter`(개발용), `RealExportAdapter`(실제 USB+SAF).

## 라이선스 / 배포

본인 전용 앱이다(원칙 A: 수익화하지 않는다). 별도 라이선스나 배포 계획 없음.
