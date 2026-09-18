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
`./gradlew :core:test` 한 줄로 실제 컴파일·테스트가 가능하다. 이 프로젝트를 만든
개발 환경은 Android SDK를 설치할 수 없는 네트워크 정책 하에 있었기 때문에, `app`
모듈은 실제로 빌드해 본 적이 없다 - **로컬 Android Studio에서 반드시 첫 빌드를
확인해야 한다.**

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
2. (선택) 공휴일 자동 조회를 쓰려면 저장소 루트에 `local.properties`를 만들고
   아래 줄을 추가한다 - 파일은 `.gitignore`에 걸려 있어 커밋되지 않는다.
   ```properties
   holiday.api.key=공공데이터포털에서_발급받은_디코딩_서비스키
   ```
   키가 없어도 앱은 완전히 동작한다 - 공휴일은 내장 데이터(고정 양력 공휴일)와
   수동 입력으로 대체된다(스펙 4.14절 확보 우선순위).
3. `app` 모듈을 실행한다. minSdk 33(Android 13) 이상 기기/에뮬레이터가 필요하다.

### 3. 장치 연동 검증 (실기기 필요, 스펙 8.3절)

USB MSC 인식·SAF 접근 흐름(`RealExportAdapter`, `export/DeviceStorageAccess.kt`)은
기기·OS·장치 펌웨어에 따라 동작이 달라질 수 있어 반드시 실기기로 검증해야 한다.
장치 없이 먼저 확인하고 싶다면 설정 화면 → "장치 미리보기"에서 800×480 렌더링
결과를 보거나, "페이로드 파일로 저장"으로 `calendar.txt`를 앱 전용 저장소에
남겨 내용을 직접 확인할 수 있다(`FakeExportAdapter`).

## 이 개발 환경에서 검증한 것 / 못한 것

| 구분 | 상태 |
|---|---|
| `core` 모듈 컴파일·전체 단위테스트 | ✅ 이 환경에서 실제로 실행하고 확인함 |
| `core`의 페이로드 인코더가 스펙 7.2절 예시와 정확히 일치 | ✅ 골든 테스트로 확인함 |
| `app` 모듈 컴파일(Compose/Room/Hilt KSP) | ❌ Android SDK 없음 - 로컬 빌드 필요 |
| 공공데이터포털 특일정보 API 실호출 | ❌ 실제 스키마 미확인 - `HolidayApiPayloadParser`가 알려진 문서 기반으로 방어적으로 작성됨 |
| USB MSC 인식 및 SAF `calendar.txt` 쓰기 | ❌ 실기기 필요 |
| 장치(e-ink) 실물에서의 800×480 렌더링 | ❌ 실기기 필요 (앱 쪽 미리보기는 근사치) |

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
  `FakeExportAdapter`(개발/미리보기용), `RealExportAdapter`(실제 USB+SAF).
- **`app/preview`** - 장치 미리보기(F16) 렌더러. 실제 장치 펌웨어의 근사치일 뿐,
  정확한 픽셀 배치는 임베디드 쪽 구현이 최종 결정한다.

## 라이선스 / 배포

본인 전용 앱이다(원칙 A: 수익화하지 않는다). 별도 라이선스나 배포 계획 없음.
