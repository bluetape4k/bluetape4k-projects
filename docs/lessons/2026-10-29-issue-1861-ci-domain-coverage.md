# Issue #1861: publishable module과 CI 도메인 계약을 단일화한 기록

## 맥락

Issue #1861은 `settings.gradle.kts`가 자동 등록하고
`buildSrc/src/main/kotlin/ProjectPublication.kt`가 publishable로 판단하는
모듈 집합과 `.github/workflows/ci.yml`의 path filter·Gradle test task 집합이
서로 다르다는 문제를 다룬다. 기존 CI에는 `io/avro`와
`utils/{geo,javatimes,money}` 경로가 일반 제품 테스트 lane에 연결되지 않았고,
변경된 도메인 job이 `skipped`여도 `CI Status`가 일반적으로 이를 거부하지 않았다.

기준 ref는 `develop`의 `0d84cd71369eac92f1c3bdc4b00d73767e8ccd73`이며, 원 이슈는
[Issue #1861](https://github.com/bluetape4k/bluetape4k-projects/issues/1861)이다.

근거 원본은 `settings.gradle.kts`,
`buildSrc/src/main/kotlin/ProjectPublication.kt`,
`.github/workflows/ci.yml`, `.github/workflows/nightly-tests.yml`, 그리고
`scripts/ci/ci-module-domain-manifest.json`이다. 이 기록은 현재 feature
worktree의 변경 전 기준과 변경 후 정적 계약 결과를 구분하며, hosted CI 결과를
앞서 주장하지 않는다.

## 결정

1. `io/avro`를 기존 `test-io` lane에 추가한다.
2. `utils/geo`, `utils/javatimes`, `utils/money`만 정규 `test-utils` lane으로
   추가한다. 이 세 모듈은 Issue의 누락 경로이며 Docker나 특수 환경 없이 실행할 수
   있는 제품 테스트다.
3. 나머지 저빈도·Docker 의존·코드 없는 umbrella 모듈은 정규 CI를 무작정 넓히지
   않고 `scripts/ci/ci-module-domain-manifest.json`에 `nightly-only` 또는
   근거가 있는 `explicit` exemption으로 기록한다.
4. `scripts/ci/validate_ci_module_coverage.py`가 실제 `settings.gradle.kts`의
   `includeModules(...)`와 `ProjectPublication.kt`의 project/path publication
   exclusion을 읽어 현재 저장소의 81개 publishable module을 자동 발견한다. 각
   항목이 정확히 하나의 route를 가지는지, 정규 lane의 path filter·test task·Kover
   task·`ci-status` `needs`와 skip guard가 존재하는지, nightly exemption에 실제
   test task가 있는지 검사한다. 또한 `changes` output이 같은 filter output을
   가리키는지와 각 모듈의 build/source/test 경로가 오직 소유 lane에 매핑되는지를
   검사한다.
5. `scripts/ci/**`를 shared path filter에 포함해 manifest나 validator 자체의
   변경도 전체 계약 검증을 실행하게 한다.

## CI runner/kernel 보정

PR #1866의 exact head `3f7fedb0d388154e1d0b741bde26618518c8c322`에서 실행한
[run 40838475210](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/40838475210)의
`Test / Data`와 `Test / Spring Boot (batch-nosql)`은 각각 hosted image
`20261026.354.1`, Ubuntu `24.04.5 LTS`, kernel `7.0.0-1017-azure`를 기록한 뒤
`mongo:8.0.28`의 `waiting for connections` startup timeout으로 실패했다. Data는
재시도 중 timeout으로 취소되었고, Spring Boot는 MongoDB 테스트에서 실패했다. 이는
Issue #1861의 모듈 매핑 문제가 아니라 MongoDB와 hosted kernel의 호환성 문제다.

[MongoDB 8.0 릴리스 노트](https://www.mongodb.com/docs/v8.0/release-notes/8.0/)는
kernel `6.19`부터 `7.0.13`까지를 Docker를 포함한 모든 MongoDB 패키지의 비호환
범위로 규정하고 `7.0.14` 이상을 요구한다. 현재 hosted `ubuntu-24.04` kernel은
그 경계보다 낮으므로 MongoDB가 실행되는 다음 여섯 경로를 `ubuntu-22.04`로
되돌렸다.

- PR CI: `test-data`, Spring Boot `batch-nosql`
- Nightly: Data `nosql`, Spring Boot `batch-nosql`, Testcontainers `storage-cache`
- Nightly image-gate: MongoDB shard

MongoDB를 실행하지 않고 shard 결과만 집계하는 Nightly image-gate aggregate는
`ubuntu-24.04`를 유지한다. 각 MongoDB 경로의 `ImageOS`, `ImageVersion`,
`/etc/os-release`, `uname -r` 진단과 실패 전파 계약은 유지한다.

이 보정은 현재 hosted kernel에서의 단기 복구다. GitHub는
[`ubuntu-22.04` image의 deprecation을 2026-09-17에 시작하고 2027-04-17에 완전
지원 종료한다고 공지했다](https://github.com/actions/runner-images/issues/14254).
따라서 `ubuntu-22.04`를 장기 해법으로 간주하지 않고, `7.0.14` 이상 kernel을
제공하는 지원 runner 또는 MongoDB 실행 경계를 별도 후속 작업으로 전환해야 한다.

## runner 보정 검증

회귀 테스트는 먼저 새 `ubuntu-22.04` 계약에서 기존 workflow가 거부되는 RED를
확인한 뒤, workflow와 verifier를 수정해 GREEN으로 전환했다. 현재 로컬 계약은
다음 결과를 낸다.

```text
python3 -m unittest tests.test_catalog_workflows -v
Ran 135 tests — OK
```

Hosted exact-head 재실행에서 실제 Ubuntu 22 image/kernel과 MongoDB 테스트 성공이
확인되기 전까지 PR 완료나 Issue 종료로 표시하지 않는다.

## 결과

- `io/avro`의 test와 `koverXmlReport`가 `test-io`에서 실행된다.
- `utils` output·path filter·`test-utils` job·coverage manifest·status guard가
  추가되어 세 모듈의 변경이 정규 제품 테스트를 건너뛸 수 없다.
- 모든 정규 domain lane에 변경 감지와 `skipped` 결과를 함께 검사하는
  `ci-status` guard를 추가했다.
- `ci-status`가 `changes`와 모든 정규 domain job을 실제 `needs`에 포함하는지도
  검사해 변경 감지 출력이 비어 false-green이 되는 경로를 차단했다.
- validator는 신규 `build.gradle.kts` 모듈이 manifest에서 빠지거나 한 경로가 두
  path lane과 일치하는 경우 실패한다.

## 검증

다음 검증은 feature worktree의 현재 변경 집합에서 실행했다.

```text
python3 scripts/ci/validate_ci_module_coverage.py
CI module coverage contract passed: 81 publishable modules

python3 -m unittest scripts/ci/test_validate_ci_module_coverage.py -v
Ran 13 tests in 0.436s — OK

python3 .github/scripts/test-ci-domain-parallelization.py -v
Ran 21 tests in 0.228s — OK

python3 .github/scripts/test-aggregate-kover-coverage.py -v
Ran 28 tests in 0.894s — OK

python3 scripts/verify-catalog-workflows.py
catalog workflow contract passed

python3 -m unittest tests.test_catalog_workflows -v
Ran 135 tests — OK

actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml
통과

git diff --check
통과

순차 Gradle 제품 테스트
:bluetape4k-avro:test — 221 tests 통과
:bluetape4k-javatimes:test — 756 tests 통과, 36 skipped
:bluetape4k-money:test — 46 tests 통과, 1 skipped
:bluetape4k-geo:test — 245 tests 통과, 2 skipped
```

## 놓친 점과 다음 방지책

처음에는 정규 CI에 이미 연결된 `testing/testcontainers`를 같은 lane으로
기록했지만, 실제 제품 task는 nightly Testcontainers matrix에만 있었다. 독립 리뷰에서
path filter가 여전히 이 모듈을 `testcontainers-spring` bridge job으로 보낸다는
false-green 경로도 확인했고, filter를 제거한 뒤 validator에 exemption과 regular
filter의 충돌 검사를 추가했다. 앞으로 새 모듈을 추가할 때는 settings 등록과
publication 판정만 확인하지 말고, CI와 Nightly의 실제 `:project:test` task와
path filter의 최종 소유 lane까지 같은 변경에서 확인한다.

첫 PR hosted run에서는 `Central Catalog Governance`와 `Test / PR merge-result contract`가
새 `test-utils` checkout 및 확장된 `ci-status` 환경을 이전 중앙 verifier 상수와
비교해 실패했다. 현재 저장소의 `scripts/verify-catalog-workflows.py` 계약을 25개
first-party checkout과 전체 정규 lane status guard에 맞게 갱신하고 로컬 135개
workflow 계약 테스트로 재검증했다. 영향받은 네 모듈의 Gradle 제품 테스트도 통과했지만
호스팅 CI의 수정 후 exact head 결과는 아직 실행하지 않았다. PR 생성 후 exact head 기준 hosted CI에서
`io/avro`, `utils/{geo,javatimes,money}` lane과 synthetic merge-result 계약을
다시 확인해야 한다.

후속 hosted run에서는 도메인 매핑 계약은 통과했지만, 위 runner/kernel 원인으로
`Test / Data`, Spring Boot `batch-nosql`, coverage와 `CI Status`가 연쇄 실패했다.
따라서 새 runner 보정 커밋을 push한 뒤 exact head에서 MongoDB startup, 실제 kernel,
coverage, `CI Status`를 다시 읽어야 하며, merge와 Issue 종료는 별도 fresh approval
전까지 보류한다.

## Writer DoD

- SPW-01: DONE — 독자(유지보수자), 목적(Issue #1861 계약 단일화), 기준 ref, 이슈 URL,
  변경 파일과 검증 명령을 고정했다.
- SPW-02: DONE — 맥락, 결정, 결과, 검증, 놓친 점, 미래 방지책을 포함했다.
- SPW-03: DONE — 한국어 기술 문체와 고정 식별자 보존을 적용했다.
- SPW-04: DONE — `settings.gradle.kts`, `ProjectPublication.kt`, 두 workflow,
  validator 출력과 테스트 결과를 대조했다.
- SPW-05: DONE — Markdown을 다시 읽고 명령·수치·불확실한 hosted CI 상태를 구분했다.
- KO-01..KO-07: DONE — 사실·식별자·링크를 보존하고 자연스러움·용어·reader-facing
  표면을 점검했으며, terminology audit 결과를 아래에 기록한다.

Terminology audit: `node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs docs/lessons/2026-10-29-issue-1861-ci-domain-coverage.md` 실행 결과를 확인하고, 발견 항목이 있으면 코드 토큰·공식 식별자 예외로만 남긴다.
