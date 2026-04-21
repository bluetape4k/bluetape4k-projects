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

- [ ] 관련 테스트 실행 및 통과 확인
- [ ] `docs/testlogs/YYYY-MM.md` 에 결과 기록 (doc-only 변경 제외)

## 체크리스트

- [ ] 변경된 모듈의 `README.md` + `README.ko.md` 업데이트
- [ ] 공개 API에 KDoc 추가
- [ ] `worktree`에서 작업 후 PR 생성
- [ ] `testing/mock-web-server` 변경 시 Docker 이미지 재빌드:
  `./gradlew :bluetape4k-mock-web-server:jibDockerBuild --no-configuration-cache`
- [ ] superpowers 작업인 경우 `docs/superpowers/index/YYYY-MM.md` 업데이트
- [ ] `virtualthread/api` 변경 시 `jdk21` + `jdk25` 동시 반영
- [ ] CodeRabbit 리뷰 확인 후 머지
