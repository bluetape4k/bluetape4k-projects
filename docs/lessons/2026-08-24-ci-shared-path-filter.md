# CI 공통 경로는 도메인 선택 필터의 shared 계약으로 고정한다

## 배경

Daily CI는 `dorny/paths-filter`로 변경된 도메인만 테스트한다. 기존 `shared` 필터는
Gradle 공통 파일과 `.github/workflows/ci.yml`만 포함했다. 따라서 다른 workflow나 CI
helper, CSV/Kafka coverage validator가 바뀌어도 도메인 test job이 모두 `skipped`될 수
있었다. `changes` job 자체가 성공하는 것만으로는 해당 변경이 도메인 테스트에 반영됐다는
증거가 되지 않는다.

## 결정

- `shared`는 Gradle 공통 입력을 계속 포함한다.
- `.github/workflows/**`, `.github/scripts/**`, `.github/actions/**`를 공통 CI
  orchestration 입력으로 포함한다.
- `scripts/validate-ci-*.rb`를 CI coverage 선택 계약의 공통 입력으로 포함한다.
- 일반 문서/다이어그램 생성 스크립트 전체를 `shared`에 넣지는 않는다. 해당 파일은
  도메인 테스트 선택을 바꾸지 않으므로 불필요한 전 도메인 실행을 만들지 않는다.
- `test-ci-domain-parallelization.py`가 위 목록을 회귀 계약으로 검사하고, `changes` job이
  계속 이 검증기를 실행한다.

## 결과

CI orchestration 또는 도메인 선택 계약이 바뀌면 모든 도메인 test job이 실행된다. 일반
모듈 변경은 기존 도메인별 filter만 통과하므로 변경 없는 모듈을 계속 테스트하지 않는다.
workflow 변경이 Nightly/Release 전용이어도 CI 정의의 영향 범위를 보수적으로 검증하며,
일반 `scripts/**` 변경까지 전파하지 않아 생성 도구 수정으로 인한 불필요한 비용은 막는다.

## 검증

- RED: 기존 shared 목록에서 workflow/helper/validator wildcard 4개가 누락되어 회귀 테스트
  12개 중 4개 subtest가 실패했다.
- GREEN: shared 목록에 `.github/workflows/**`, `.github/scripts/**`,
  `.github/actions/**`, `scripts/validate-ci-*.rb`를 추가한 뒤 12개 계약 테스트가
  통과했다.
- 후속 정적 검증에서 `actionlint`, `git diff --check`, 관련 workflow/test 계약을 다시
  실행한다.

## 향후 지침

- 새로운 CI helper/action 또는 공통 path-filter 계약을 추가하면 `shared` 목록과 회귀
  테스트를 같은 변경에서 갱신한다.
- 모듈 전용 파일은 해당 도메인 filter에만 추가하고 `shared`로 승격하지 않는다.
- `skipped` job은 coverage 증거가 아니므로, shared 경로 변경의 hosted CI에서는 모든
  도메인 test job이 실제로 시작했는지 확인한다.
