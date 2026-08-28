# Issue #1565 publication gate 독립 검토

## 검토 범위와 판정

- 범위: `buildSrc/src/main/kotlin/PublishingSigningSupport.kt`, 세 GitHub Actions
  workflow, `scripts/test_release_workflow_policy.py`, 한·영 README, testlog와 lesson.
- 로컬 코드·문서 P0/P1: 0건.
- 외부 운영 P1: `maven-central-release` 환경 보호 규칙과 secret 범위는 아직
  변경하지 않았다. 이 항목은 별도의 운영 승인과 read-back이 필요하다.
- 최종 판정: 코드와 로컬 검증은 `DONE`, SNAPSHOT dispatch·환경 변경·merge는
  `PENDING`이다.

## 검토 결과

1. `applyBluetape4kPomMetadata`가 developer organization 메타데이터의 단일 원본이며
   `Bluetape4k`와 `https://github.com/bluetape4k`를 설정한다.
2. CI·RELEASE·SNAPSHOT의 metadata 검증은 generation, strict POM check, module
   metadata generation을 한 단계에서 실행하고, publication 명령은
   `--no-configuration-cache --no-build-cache`를 명시한다.
3. 정책 parser는 raw 문자열이 아니라 YAML `jobs.*.steps[].name/run` 구조에서
   지정 job의 실행 step을 추출한다. 비실행 YAML scalar, 다른 job 이동, step/job
   `if`·`continue-on-error` guard, `echo`·shell operator·Gradle option 값과
   heredoc 우회를 음성 fixture로 거부한다.
4. 추가 publication 호출은 shell token으로 분리해 정확히 한 번만 허용한다. 주석과
   heredoc 본문에 있는 동일 문자열은 실행 호출로 세지 않는다.
5. `origin/develop`에는 `bluetape4k/tenant`, `bluetape4k/tenant-reactor`,
   `ktor/tenant` artifact 경로가 없으므로 #1562의 세 artifact 검증은 해당 통합 후
   별도로 다시 실행해야 한다.

## 검증 증적

- `python3 -m unittest scripts.test_release_workflow_policy -v`: 36개 통과,
  실패·오류·skip 0.
- `./gradlew -p buildSrc test --no-daemon --no-configuration-cache --no-build-cache`:
  성공, 19개 Kotlin test 통과.
- core strict POM generation/check: 성공.
- 전체 publication generation/check/metadata: `BUILD SUCCESSFUL`, 419개 task 중
  227개 실행·192개 up-to-date,
  POM 76개·dependency 31,598개, metadata 76개·variant 157개·dependency 1,487개,
  실패 0.
- publication Ruby tests: 16개 test, 38개 assertion, 실패·오류·skip 0.
- `actionlint`, `git diff --check`, 한국어 용어 감사: 통과.

## 후속 경계

- 환경 `maven-central-release`의 required reviewer, `develop` branch restriction,
  admin bypass와 `CENTRAL_*`·`SIGNING_*` secret 범위는 운영 변경 승인 전까지
  유지한다.
- #1562가 통합된 뒤 세 TenantContext artifact에 대해 strict POM과 metadata 검사를
  다시 실행하고, 그 결과를 확인한 뒤에만 SNAPSHOT dispatch를 검토한다.
- 이 branch는 exact-head CI와 review read-back 후 merge-ready에서 중단한다. merge는
  해당 exact head에 대한 최신 승인을 별도로 받아야 한다.

## Superpowers 문서 감사

- SPW-01: PASS — 독자, 목적, 근거 파일, issue/branch 식별자와 외부 운영 미확인 범위를
  고정했다.
- SPW-02: PASS — 범위, finding, disposition, 검증, 후속 경계와 판정을 포함한다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 명령, API, URL, 수치와 상태 토큰을
  보존했다.
- SPW-04: PASS — 현재 diff, workflow 구조, 테스트 수치와 `origin/develop` 경로를
  대조했다.
- SPW-05: PASS — Markdown heading, 목록, code token과 최종 판정을 다시 읽었다.

## 한국어 자연스러움 감사

- KO-01~KO-07: PASS — 기술 용어를 일관되게 유지하고 불확실한 운영 상태를
  `PENDING`으로 명시했다.
