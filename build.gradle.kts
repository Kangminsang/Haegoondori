// 루트 프로젝트는 의도적으로 비워둔다.
//
// Gradle은 어떤 태스크를 실행하든 루트 프로젝트의 build.gradle.kts는 항상 평가(configure)한다
// (configureondemand=true 설정도 "필요 없는 서브프로젝트"만 건너뛸 뿐, 루트 자체는 예외 없이 평가된다).
// 만약 여기서 `plugins { alias(libs.plugins.android.application) apply false }` 같은 관용적인
// 패턴을 쓰면, apply=false여도 Gradle이 그 플러그인 아티팩트를 즉시 google() 저장소에서
// 해석하려 시도한다 — 그 결과 :core:test 처럼 Android와 무관한 태스크를 실행할 때도
// Android SDK 배포처(dl.google.com)에 대한 네트워크 접근이 필요해진다.
//
// 이를 피하기 위해 Android/Compose/Hilt/KSP 관련 플러그인은 여기서 선언하지 않고
// app/build.gradle.kts에서 직접 선언한다. 그러면 그 플러그인들은 :app 서브프로젝트가
// 실제로 configure될 때만 해석되며, core만 빌드/테스트할 때는 전혀 관여하지 않는다.
