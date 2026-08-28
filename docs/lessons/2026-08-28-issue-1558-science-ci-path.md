# #1558 `utils/science` 일반 PR CI 경로를 고정한다

관련 이슈: [#1558](https://github.com/bluetape4k/bluetape4k-projects/issues/1558)
대상 workflow: `.github/workflows/ci.yml`

## 배경

`utils/science`는 NetCDF, 좌표 변환, GIS, Exposed 연동을 한 모듈에서 제공한다.
그러나 일반 PR CI의 `changes` output과 `dorny/paths-filter`에는
`utils/science/**`가 없었다. 따라서 해당 모듈을 수정해도 전체 `Build`가 성공하면
전용 테스트 job 없이 PR 검사가 끝날 수 있었다.

실제 `develop`의 최신 성공 run
[`33107008042`](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33107008042)은
`18472064c594ab2dee835cff6695cd6ef9538ea5`에서 성공했지만, job 목록에
`Test / Science`가 없고 `Test / Key Utils`만 생성됐다. 이는 CI 성공이
`utils/science` 테스트 실행을 증명하지 않는다는 뜻이다.

## 결정

- `changes.outputs.science`와 `science: 'utils/science/**'` path filter를 추가한다.
- `Test / Science` job을 `changes`, `catalog-governance` 뒤에 실행하고
  `:bluetape4k-science:test`를 호출한다.
- 일반 CI에서는 `-PexcludeTags=slow-netcdf`를 명시한다. 이 모듈의 Gradle 기본값도
  같은 태그를 제외하며, 공개 CF 샘플 회귀는 Nightly의
  `-PincludeTags=slow-netcdf` 경로에서 계속 실행한다.
- Science job의 test result와 Kover artifact를 `Coverage Report`의 `needs` 및
  expected job manifest에 포함한다.
- `CI Status`가 `science` 변경을 감지했는데 job이 `skipped`이면 실패하도록 한다.
- `.github/scripts/test-ci-domain-parallelization.py`에서 정상 route를 검사하고,
  output·path filter·job을 각각 제거한 세 가지 mutation이 실패하는지 회귀 계약으로
  고정한다.

## 결과

변경 파일은 다음 두 workflow 계약과 lesson이다.

- `.github/workflows/ci.yml`
- `.github/scripts/test-ci-domain-parallelization.py`
- `docs/lessons/2026-08-28-issue-1558-science-ci-path.md`

`utils/science/**` 변경은 이제 일반 PR에서 정규 Science 테스트와 Kover artifact를
선택한다. `slow-netcdf`는 PR 실행 시간에 섞이지 않고 기존 Nightly 전용 경로에
남는다. 다른 도메인 job과 마찬가지로 전체 `Build`와 Science 테스트는 서로
직렬화하지 않는다.

## 검증

- CI domain 계약 테스트 17개가 통과했다. Science output, path filter, job, coverage
  wiring, `CI Status` skip guard와 세 mutation 거부를 포함한다.
- Kover 집계 계약 테스트 27개가 통과했다.
- `ruby scripts/validate-ci-csv-coverage.rb`와
  `ruby scripts/validate-ci-kafka4-coverage.rb`가 통과했다.
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`가
  `1.7.12`에서 통과했다.
- `python3 -B -m py_compile .github/scripts/test-ci-domain-parallelization.py`와
  `git diff --check`가 통과했다.
- 정확한 `fix/issue-1558-science-ci-path` worktree에서
  `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew
  :bluetape4k-science:test -PexcludeTags=slow-netcdf --no-configuration-cache
  --rerun-tasks`를 실행해 `233 passing`을 확인했다. 실패와 명시적 skipped 출력은
  없었다.
- hosted CI의 새 head run과 PR 생성은 이번 범위에 포함하지 않았다. 기존 run
  `33107008042`는 수정 전 경로가 Science job을 만들지 않았다는 baseline 증거다.

## 놓친 점과 주의사항

path filter가 job을 `skipped`로 만든 사실은 테스트 coverage가 있다는 뜻이 아니다.
새 도메인을 추가하거나 기존 도메인의 경계를 바꿀 때는 output, filter, test job,
coverage 집계, 최종 status를 한 변경에서 연결해야 한다. 느린 fixture를 정규 CI에
다시 넣을 때는 `slow-netcdf`를 Nightly와 분리한다는 비용 계약도 함께 검토해야 한다.

## 실패한 가정과 예방

| 실패한 가정/판단 | 발견 증거 또는 교정 | 수정 결정 | 향후 예방 확인 |
| --- | --- | --- | --- |
| 성공한 일반 CI가 Science 테스트도 실행했다고 판단 | run `33107008042`는 성공했지만 `Test / Science` job이 없었다 | path filter부터 최종 status까지 Science 경로를 신설하고 skip guard를 추가 | 새 도메인 route마다 live job 생성 여부와 정적 mutation 계약을 함께 확인 |
| Nightly의 `slow-netcdf` 실행을 일반 CI에 재사용하면 충분하다고 판단 | Nightly `test-utils`는 `run_standard`에서만 실행되며 PR 검사와 별개다 | PR 정규 테스트는 `-PexcludeTags=slow-netcdf`, Nightly는 `-PincludeTags=slow-netcdf`로 분리 | 두 workflow의 태그 인자를 회귀 테스트에서 동시에 확인 |

## 문서 SPW 감사

- SPW-01: PASS — CI 유지보수자를 독자로 정하고, 이슈 #1558, workflow 경로, run
  `33107008042`, 기존 Gradle 태그 설정을 근거로 사용했다.
- SPW-02: PASS — 배경, 결정, 결과, 검증, 놓친 점, 예방 규칙을 포함했다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 명령, 식별자, URL, 수치를 그대로
  보존했다.
- SPW-04: PASS — `ci.yml`, 기존 Nightly job, `utils/science/build.gradle.kts`,
  정적 계약 테스트와 실시간 baseline을 대조했다.
- SPW-05: PASS — 최종 Markdown을 다시 읽고 경로, job 이름, 태그, 검증 수치와 범위를
  확인했다.

## 한국어 자연스러움 감사

- KO-01: PASS — 이슈, run, commit head, 명령, 테스트 수와 검증 범위를 보존했다.
- KO-02: PASS — 중요성 같은 추상 표현 대신 job 부재와 실제 실행 수를 기록했다.
- KO-03: PASS — 번역투와 기계적인 결론을 제거하고 원인-결정-검증 순서로 정리했다.
- KO-04: PASS — `path filter`, `output`, `job`, `artifact`, `slow-netcdf` 용어를
  문맥에 맞게 일관되게 사용했다.
- KO-05: PASS — 과장이나 비유 없이 실패 경로를 직접 기술했다.
- KO-06: PASS — 제목, 본문, 표, 코드 인라인, 링크와 식별자를 검토했다.
- KO-07: PASS — `audit-korean-terms.mjs` 결과를 확인했고 설명되지 않은 용어 충돌이
  없다.
