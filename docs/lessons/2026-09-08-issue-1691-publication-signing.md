# publication 서명은 생성된 서명 파일로 검증한다

## 배경과 결정

[#1691](https://github.com/bluetape4k/bluetape4k-projects/issues/1691)의 기존 테스트는 가짜 GPG 실행 파일과 존재하지 않는 publication으로 설정값만 확인했다. 이 결과는 실제 `sign(publication)` 실행이나 `.asc`의 유효성을 증명하지 못한다.

회귀 테스트는 저장소가 빌드한 `configurePublishingSigning`과 생성된 `PublishingSigningKeySupport.kt`를 그대로 사용한다. 임시 키와 격리된 GPG home으로 로컬 publication을 서명하고 `gpg --verify`로 산출물을 확인한다. 실제 배포 키, 원격 publication과 release는 검증 범위에 포함하지 않는다. 잘못된 키의 실패 경로에서는 비밀번호와 private armor가 출력에 노출되지 않는지도 확인한다.

## Base64 입력 계약

`resolveSigningKey`의 현재 strict Base64 정책을 유지한다.

- 원본 바이트 길이에 따라 필요한 `=` 또는 `==`가 모두 있으면 디코딩한다.
- 필요한 padding을 제거한 입력은 디코딩하지 않고 공백까지 포함한 원본을 반환한다. 입력을 자동 복구하지 않는다.
- 원본 바이트 길이가 3의 배수이면 padding이 필요 없으므로 `=` 없는 완전한 Base64도 허용한다.
- Base64 형식이 잘못됐거나 디코딩 결과가 유효한 UTF-8 ASCII private armor가 아니면 원본을 반환한다.
- armor 외형의 정규화와 실제 PGP 키 검증은 다르다. PGP 키 파싱과 서명은 Gradle signing 단계에서 검증한다.

padding이 필요한 키를 padding 없이 저장한 소비자는 표준 Base64 인코더로 다시 인코딩하거나 raw armor를 제공해야 한다. 공통 helper를 변경해야 할 때는 생성 파일을 직접 고치지 않고 중앙 원본과 동기화 절차를 사용한다.

## 검증과 재발 방지

`PublishingSigningSupportTest`는 padding 경계를 고정하며 실제 서명 테스트는 Projects 어댑터의 publication 경로를 검증한다. 중앙 helper의 단위 테스트를 이 어댑터의 통합 검증으로 대체하지 않는다. 실행 결과는 연결 PR의 검증 항목에서 확인한다.

`buildSrc`는 애플리케이션 프로젝트보다 먼저 빌드되므로 기존 `kotlin.test`와 Gradle TestKit을 사용한다. 프로젝트의 assertions 모듈을 추가해 빌드 부트스트랩에 순환 의존성을 만들지 않는다.

작업 기록 초기화에서도 추정한 경로와 식별자 대신 helper의 입력 계약을 먼저 확인한다. 소유자 파일은 state-root의 `handles` 아래에 두고, lane write scope는 저장소 기준 상대 경로로 지정한다. 초기화 성공은 테스트나 리뷰 성공의 근거가 아니다.

macOS 기본 임시 디렉터리에 긴 fixture 이름을 붙이면 GPG agent의 Unix 소켓 연결이 실패할 수 있다. 최초 실행의 `IPC connect call failed`와 `No agent running`을 같은 경로의 키 생성 명령으로 재현했다. 짧은 `/tmp/signing-*` 디렉터리와 GPG home의 `0700` 권한을 사용하며, 종료 시 해당 home의 agent와 임시 키를 정리한다. 테스트 실패를 실제 서명 어댑터 결함으로 분류하기 전에 키 생성 단계부터 분리해 확인한다.
