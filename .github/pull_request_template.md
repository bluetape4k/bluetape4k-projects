## 변경 요약

<!-- 무엇을, 왜 변경했는지 간략히 설명하세요 -->

## 변경 유형

- [ ] `feat` — 새 기능
- [ ] `fix` — 버그 수정
- [ ] `refactor` — 리팩토링 (기능 변경 없음)
- [ ] `perf` — 성능 개선
- [ ] `test` — 테스트 추가/수정
- [ ] `docs` — 문서만 변경
- [ ] `chore` — 빌드/설정/의존성

## 테스트

- [ ] **로컬 테스트 실행 결과 명시** (예: `./gradlew :module:test` → N passing, 소요 시간)
- [ ] `docs/testlogs/YYYY-MM.md` 에 결과 기록 (doc-only 변경 제외)

## Code Review

- [ ] **Code review 에이전트 실행 완료** (`oh-my-claudecode:code-reviewer` 또는 `pr-review-toolkit:code-reviewer`) — HIGH/CRITICAL 이슈 모두 해소
- [ ] OMC Code Review 확인 후 머지

## Issue 연결 및 자동 종료

- [ ] 단일 PR 또는 default branch 대상 PR은 실제로 해결하는 issue마다 `Closes #<issue-number>`를 한 줄씩 명시한다.
- [ ] stacked child PR(base가 `develop`/`main`이 아닌 경우)은 `Part of #<epic-number>` 또는 `Refs #<issue-number>`를 사용하며 issue 자동 종료를 기대하지 않는다.
- [ ] stacked train의 최종 PR(base가 `develop` 또는 `main`)은 모든 child issue에 대해 `Closes #<issue-number>`를 한 줄씩 명시한다. GitHub는 이 PR이 default branch에 merge될 때 해당 issue를 자동 종료한다.
- [ ] merge 후 `closingIssuesReferences`와 issue state를 live read-back한다. 자동 종료가 의도되지 않은 PR에는 `Closes` 대신 `Part of`/`Refs`를 사용한다.

## 체크리스트

- [ ] 변경된 모듈의 `README.md` + `README.ko.md` 업데이트
- [ ] 공개 API에 KDoc 추가
- [ ] `cache/` 공개 설정/직렬화 변경 시 prior-release ABI(Java/Kotlin) fixture와 serialized fixture evidence 첨부 (증거가 없으면 stable publication을 진행하지 않음)
- [ ] `spring-boot/` 모듈 변경 시 Spring Boot 4 BOM 적용 및 Spring Framework 7.x 호환성 확인
- [ ] `spring-boot/` 모듈 변경 시 `.github/workflows/nightly-tests.yml`의 독립 test task 및 비-demo kover task 등록 확인
- [ ] `worktree`에서 작업 후 PR 생성
- [ ] `testing/mock-web-server` 변경 시 Docker 이미지 재빌드:
  `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`
- [ ] superpowers 작업인 경우 `docs/superpowers/index/YYYY-MM.md` 업데이트
- [ ] `virtualthread/api` 변경 시 `jdk21` + `jdk25` 동시 반영
