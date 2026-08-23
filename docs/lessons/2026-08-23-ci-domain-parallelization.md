# 도메인 CI는 전체 Build와 병렬로 실행해야 한다

## 배경

Daily CI는 이미 `dorny/paths-filter`로 `core`, `io`, `infra`, `data` 등 변경 영역을
구분하고 관련 테스트만 선택했다. 그러나 선택된 모든 도메인 job이 `Build`를 `needs`로
참조해, 전체 `./gradlew build -x test --parallel`이 끝난 뒤에야 테스트를 시작했다.
단일 도메인 변경에서도 전체 compile 시간과 도메인 테스트 시간이 직렬로 더해졌다.

최근 `develop` 실행
[`32630371344`](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/32630371344)에서도
관련 없는 도메인 job은 건너뛰었지만, `Test / Key Utils`는 `Build` 완료 뒤에 실행됐다.

## 결정

- 전체 `Build`와 publication metadata 검증은 독립된 필수 check로 유지한다.
- 도메인 test job과 Testcontainers image gate는 `changes`와
  `catalog-governance`만 선행조건으로 삼는다.
- `CI Status`는 계속 `build`, 도메인 test job, image gate, `coverage-report`를 모두
  집계해 실패를 닫힌 상태로 처리한다.
- `Coverage Report`는 실행된 coverage job의 artifact를 계속 요구하고, 모든 관련 job이
  의도적으로 `skipped`인 경우만 report 부재를 허용한다.
- `.github/scripts/test-ci-domain-parallelization.py`로 위 의존 그래프를 회귀 계약으로
  고정하고 `changes` job에서 실행한다.

## 결과

도메인 test job은 전체 `Build`와 동시에 시작할 수 있다. 따라서 일반적인 단일 도메인
변경의 실행 경로는 `Build + 도메인 테스트`의 합이 아니라 두 작업 중 더 오래 걸리는
시간에 가까워진다. 실제 절감 시간은 GitHub-hosted runner 대기 시간과 Gradle cache
상태에 따라 달라지므로 첫 PR 실행에서 확인해야 한다.

`build.gradle.kts`, `settings.gradle.kts`, `gradle/**`, `ci.yml` 같은 공통 경로가
변경되면 기존 `shared` 규칙에 따라 모든 도메인 테스트가 계속 실행된다. 관련 없는
테스트만 건너뛰되, 공통 빌드 계약의 영향 범위는 축소하지 않았다.

## 검증

- RED: 기존 workflow에서 12개 도메인 job이 `build`를 기다려 계약 테스트가 실패했다.
- GREEN: 도메인 job의 `needs`를 `changes`, `catalog-governance`로 바꾼 뒤 계약 테스트
  4개가 통과했다.
- `Build`는 `validate-wrapper`, `catalog-governance`를 계속 기다리고 `CI Status`가
  `build`를 계속 집계하는지 확인했다.
- `Coverage Report`가 image gate를 제외한 모든 coverage job을 계속 기다리는지
  확인했다.

## 놓친 점과 주의사항

path filter로 job이 `skipped`됐다는 사실은 해당 영역의 테스트 coverage가 검증됐다는
뜻이 아니다. 변경 영향이 공통 경계를 통과할 수 있는 파일은 `shared`에 유지하고,
새 도메인이나 공통 build logic을 추가할 때 filter와 의존 그래프 계약을 함께 갱신해야
한다.

Testcontainers image gate의 순차 실행 계약과 release gate는 이번 변경 범위가 아니다.
Daily CI에서 시작 시점만 앞당겼으며, gate 내부 실행 방식이나 판정 조건은 바꾸지 않았다.

전체 `Build`가 일찍 실패해도 이미 시작한 도메인 테스트는 계속 실행될 수 있어 실패한
실행의 runner 사용량은 늘 수 있다. 성공 경로의 직렬 대기를 제거하고 독립된 실패 증거를
함께 수집하기 위한 의도적인 절충이며, `CI Status`는 어느 한쪽 실패도 성공으로 바꾸지 않는다.

## 향후 지침

- 새 도메인 test job은 `Build`를 직렬 선행조건으로 추가하지 않는다.
- 도메인 job은 변경 감지와 공통 catalog 검증 뒤에 실행하고, 전체 `Build`와 병렬화한다.
- 새 coverage job을 추가하면 `Coverage Report`의 `needs`, expected manifest, artifact
  계약을 같은 변경에서 갱신한다.
- 첫 hosted PR 실행에서는 job 시작 시각과 전체 소요 시간을 비교해 병렬 실행이 실제로
  적용됐는지 확인한다.

## 문서 SPW 감사

- SPW-01: PASS — 대상 독자는 CI 유지보수자이고, 근거는 현재 `ci.yml`, 계약 테스트,
  최근 Actions 실행이다.
- SPW-02: PASS — 배경, 결정, 결과, 검증, 놓친 점, 향후 가드를 기록했다.
- SPW-03: PASS — 식별자와 명령을 보존하고 한국어 기술 문장으로 작성했다.
- SPW-04: PASS — 의존 그래프와 coverage fail-closed 계약을 workflow 및 테스트와
  대조했다.
- SPW-05: PASS — 최종 diff와 Markdown을 다시 읽어 구조, 링크, 식별자, 검증 범위를
  확인했다.
