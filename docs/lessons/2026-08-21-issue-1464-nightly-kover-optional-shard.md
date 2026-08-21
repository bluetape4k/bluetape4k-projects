# 이슈 #1464 - 선택적 Nightly Kover shard의 artifact 조건 정합성

관련 이슈: #1464 · Epic #1418 Slot 5
영향 workflow: `.github/workflows/nightly-tests.yml`

## 맥락

Nightly `Test / Spring Boot (demos)` matrix shard는 demo 모듈 테스트만 실행하고
`kover_tasks`를 비워 coverage 생성을 의도적으로 생략한다. 그러나 coverage
artifact 업로드가 항상 실행되면서 파일이 없는 정상 경로를 오류로 판정했다.

## 결정 또는 발견

1. `kover_tasks`가 비어 있으면 Kover 생성과 coverage artifact 업로드를 모두
   생략한다. 테스트 결과 artifact는 계속 업로드해 demo 테스트 결과를 보존한다.
2. `kover_tasks`를 선언한 shard는 `if-no-files-found: error`를 유지한다. 실제
   coverage 산출물 누락을 성공으로 숨기지 않는 #1336 fail-closed 계약을 보존한다.
3. matrix에 선택적 coverage shard를 추가하거나 변경할 때 생성 단계와 업로드
   단계가 동일한 opt-in 조건을 공유하는 구조 테스트를 함께 갱신한다.

## 결과

테스트가 성공했지만 coverage를 생성하지 않는 shard가 artifact 단계에서 실패하지
않는다. 따라서 해당 job은 성공으로 남고, Coverage Report의 expected-job manifest와
Nightly Status가 선택적 shard 때문에 연쇄 실패하지 않는다.

## 검증

- RED: `python3 .github/scripts/test-aggregate-kover-coverage.py`에서 optional
  `demos` upload 조건 회귀 테스트가 기존 `if: always()`를 탐지해 실패했다.
- GREEN: 같은 테스트가 workflow 조건을 `always() && matrix.kover_tasks != ''`로
  고정한 뒤 전체 fixture를 통과했다.
- 정적 검증: `git diff --check`와 `actionlint`는 child head에서 실행한다.
- 호스팅 검증: 수정 PR의 Spring Boot demos, Coverage Report, Nightly Status
  required checks가 모두 성공해야 한다.

## 향후 지침

선택적 matrix shard는 “생성 여부”만 조건화하지 말고 산출물 업로드·집계 manifest·
성공 판정까지 같은 predicate로 연결한다. 반대로 coverage를 선언한 shard의
`if-no-files-found: error`는 유지해 실제 회귀를 fail-closed로 드러낸다.
