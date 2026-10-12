# Projects PR #1819 코드 검토 기록

## 검토 범위

- 저장소: `bluetape4k/bluetape4k-projects`
- 대상 변경: base `72e869690dd8de6bf66246fd5d35fb7e4601f024`부터 코드 head `83f3682e46925304c9c71f1e183537fe05ad6d8e`까지의 PR 변경
- 검토 유형: `inline fallback review` — 독립 reviewer의 attestation이 아니다.
- 계기: 독립 `code-reviewer`와 `architect` agent가 사용량 제한으로 리뷰를 반환하지 못했다.

검토한 파일:

- `.github/workflows/ci.yml`, `settings.gradle.kts`: central catalog immutable SHA pin
- `buildSrc/src/main/kotlin/PublishingSigningSupport.kt`: 최종 Maven POM 정규화
- `buildSrc/src/test/kotlin/io/bluetape4k/gradle/PublishingSigningSupportTest.kt`: 정규화 회귀 테스트
- `docs/lessons/2026-10-12-github-pr-issue-closure.md`, `docs/testlogs/2026-10.md`: 관련 결정과 검증 기록

## 판정

Inline verdict: PASS; P0=0, P1=0.

## 검토 결과

- **모듈과 API:** 변경된 Kotlin 코드는 `buildSrc`의 게시 보조 기능이다. 공개 라이브러리 API, ABI, 런타임 모듈은 바뀌지 않는다.
- **POM 정규화:** `GenerateMavenPom`의 최종 XML 파일을 읽은 뒤 `dependencyManagement`의 직접 dependency만 검사한다. group, artifact, type, classifier가 같은 항목에서 fingerprint가 같은 중복만 제거하고, metadata가 다르면 Gradle 빌드를 실패시킨다. 버전 충돌을 임의로 선택하지 않는다.
- **보안과 데이터 경계:** 정규화 입력은 Gradle이 생성한 게시 POM이며 결과는 해당 task의 destination 파일에만 쓴다. 의존성 해석, 사용자 데이터, 애플리케이션 실행 경로를 변경하지 않는다.
- **회귀 검증:** 테스트는 동일 중복 제거, 같은 관리 좌표의 버전 충돌 거부, 실제 `GenerateMavenPom` 산출물 경로를 다룬다. 9개 publisher의 실제 POM 192개와 Maven model 192개도 검증했다.
- **Catalog pin:** `settings.gradle.kts` 기본값과 CI checkout이 모두 central `develop`의 immutable SHA `198147fd6ca2992fae32e175bd5f3d396c497096`를 사용한다.
- **문서와 PR:** 한국어 테스트 기록이 오늘의 후보 SHA와 결과를 적는다. PR은 보조 작업이므로 `Part of bluetape4k-exposed#885`를 유지하고 이슈 자동 종료 표현을 사용하지 않는다.

## 발견 사항과 제한

- P0/P1 발견 사항은 없다.
- `buildSrc`에 Detekt 또는 ktlint task가 등록되어 있지 않아 정적 분석 결과는 없다. BuildSrc 테스트와 전체 publisher POM 검증은 통과했다.
- Projects `develop` 단독 POM baseline도 후보 normalizer 없이 82/82 통과했다. 따라서 normalizer는 현재 Projects POM 오류의 필수 수정이라기보다 이후 동일 중복을 제거하고 상충 항목을 거부하는 예방 장치다.
- Leader PR #955의 최신 head `e8ec1d3fe4f3af0fef52a4a0e4d9283068f980b4`는 `Test / leader-k8s`와 `CI Status`가 모두 통과했다. 이 head를 포함한 최신 후보 조합에서 consumer POM/Maven model 검증도 통과했다.
- 독립 architecture review, 독립 reviewer 증거, PR 갱신 후의 exact-head CI는 이 기록에 포함하지 않는다.
