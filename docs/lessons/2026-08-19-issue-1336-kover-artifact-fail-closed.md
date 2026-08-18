# 이슈 #1336 - Kover coverage artifact fail-closed 집계

## 배경

Coverage Report가 `actions/download-artifact` 실패를 `continue-on-error: true`로
무시하고, Kover XML이 하나도 없어도 `_No coverage reports found._`를 출력한 뒤
성공했다. 그 결과 실행되어야 할 테스트 job이나 coverage 산출물 경로가 빠져도
CI가 green으로 남을 수 있었다.

## 결정

- daily와 nightly Coverage Report가 `needs` 결과를
  `expected-jobs.manifest`로 기록한다.
- `success` job이 하나라도 있으면 artifact download 실패, expected report 0개,
  손상된 XML, instruction counter가 없는 report를 모두 실패시킨다.
- 모든 coverage job이 `skipped`인 경우에만 명시적인
  `_No coverage reports expected: all coverage jobs were skipped._` 성공으로
  기록한다. skip 상태에서 report가 생기면 계약 위반으로 실패한다.
- 각 test job의 Kover artifact upload는 `if-no-files-found: error`를 사용해
  산출물 누락을 원래 job 결과에 반영한다.
- Coveralls는 report가 실제로 있을 때만 실행하되 `fail-on-error: true`로
  업로드 실패를 Coverage Report 결과에 반영한다.
- nightly Testcontainers coverage는 plan flag가 `true`일 때만 expected job으로
  취급해 의도적 skip과 artifact 생성 실패를 구분한다.

## 검증

- RED: 기존 집계기는 expected report 0개, 손상 XML, failed job, zero-instruction
  report를 모두 성공 코드로 처리했다.
- GREEN: `python3 .github/scripts/test-aggregate-kover-coverage.py` — 9개 fixture 통과
- 정적 검증: `python3 -m py_compile` — 두 Python script 통과
- 정적 검증: `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml` — 통과
- 정적 검증: `git diff --check` — 통과

## 향후 가드

새 coverage test job이나 matrix shard를 추가할 때는 test job의 artifact upload,
Coverage Report의 `needs` manifest, intentional skip 조건을 한 변경에서 함께
갱신한다. 집계기 fixture에는 최소한 expected report 0개, download 실패, failed/
cancelled job, 손상 XML, zero-instruction XML, all-skipped 성공을 유지한다.
