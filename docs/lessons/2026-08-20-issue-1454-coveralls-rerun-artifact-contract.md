# 이슈 #1454 - Coveralls 재실행 산출물 계약

## 배경

PR #1454의 `test-infra` Kover 생성이 일부 모듈 report만 만든 상태에서도
성공으로 남을 수 있었다. 또한 `coverage-report`가 `coverage-*` 패턴으로
artifact를 다시 다운로드한 뒤 aggregate artifact를 같은 이름 계열로
업로드해, workflow 재실행에서 이전 집계 결과가 Coveralls 입력에 섞일 수
있었다.

## 결정

- Infra coverage는 `infra/lettuce`, `cache/cache-lettuce`,
  `infra/redisson`, `cache/cache-redisson`의 네 report를 모두 요구한다.
- Kover 생성 단계는 오류를 무시하지 않고, 별도 inventory 검증 단계에서
  report가 존재하고 비어 있지 않은지 확인한다.
- 집계기는 `--expected-module`을 반복해서 받아 누락·빈 report를 실패로
  처리한다.
- raw artifact는 `coverage-*`, aggregate artifact는
  `aggregate-coverage-all`로 분리해 재실행 산출물이 raw 다운로드 대상이
  되지 않게 한다.
- aggregate 단계가 성공한 경우에만 Coveralls 파일 목록과 업로드를 실행해,
  집계 실패 뒤 partial report가 외부 서비스로 전송되지 않게 한다.
- nightly도 `aggregate-nightly-coverage-all`을 raw 패턴 밖으로 분리하고,
  Infra 13개 모듈의 expected manifest를 집계기에 전달한다. Redis
  characterization task는 CI와 같은 제외 목록으로 regular coverage에서
  분리하고, 모든 nightly Kover 실패는 무시하지 않으며 aggregate artifact는
  rerun 시 `overwrite: true`로 교체한다.

Coverage percentage threshold를 완화하거나 Coveralls 실패를 N/A로 바꾸지는
않는다. 이 변경은 report 생성·수집 경계만 fail-closed로 고정한다.

## 결과와 검증

- `.github/scripts/test-aggregate-kover-coverage.py -v`: 21개 fixture 통과
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml .github/workflows/codeql.yml`: 통과
- `ruby scripts/validate-ci-csv-coverage.rb`: 통과
- `ruby scripts/validate-ci-kafka4-coverage.rb`: 통과
- CodeQL/Testcontainers 정책 unittest: 9개 통과
- `git diff --check`: 통과

새 commit의 hosted CI와 Coveralls 결과는 push 후 exact head에서 다시 확인한다.

## 놓친 점과 재발 방지

기존 집계기는 job 결과와 report 0개만 검사해, 성공한 job의 부분 module
집합을 완전한 산출물로 오인했다. 또 `overwrite: true`는 업로드 시점의
artifact 교체만 보장하므로, 다운로드 패턴과 aggregate 이름이 겹치면
재실행 입력 중복을 막지 못한다.

새 coverage job이나 module을 추가할 때는 expected job manifest, expected
module manifest, raw/aggregate artifact 이름을 함께 검토하고, fixture에
누락·빈 report와 재실행 중복 경계를 유지한다.
