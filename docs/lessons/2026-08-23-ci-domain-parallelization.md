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
- 성능 수집은 일반 회귀 검증과 분리한다. `CI`와 `Nightly`는
  `excludeBenchmarks=true`를 전달하고, 공통 Gradle test 설정은 `benchmark` tag가 붙은
  JUnit benchmark만 제외한다.
- 전체 compile build에서도 `protobuf-codec-benchmark`, `serializer-benchmark`,
  `web-framework-benchmark`의 `build` task를 제외한다.
- Daily CI의 별도 Testcontainers image gate는 `testing/testcontainers/**`, image gate
  manifest·runner 변경 또는 `workflow_dispatch`에서만 실행한다. 일반 `shared` 변경과
  workflow·계약 테스트 파일 변경만으로는 실행하지 않는다.
- 각 도메인의 Testcontainers 사용 테스트는 유지하고, `mock-web-server`와
  `mock-webflux-server` 변경은 실제 소비자인 IO HTTP 도메인 테스트가 모두 받는다.
- Nightly의 `full`·`testcontainers` scope와 release의 전체 image gate는 유지한다.
- Daily coverage는 경로 필터로 선택된 부분 집계만 포함하므로 Coveralls 업로드를 하지
  않는다. Daily의 Kover 집계와 fail-closed artifact 검증은 계속 유지한다.
- Coveralls는 전체 저장소 범위를 보장하는 Nightly `full` scope에서만 업로드한다.
  Testcontainers matrix shard의 동일 모듈 부분 report는 외부 비교 목록에서 제외하고,
  로컬 Nightly aggregate에는 계속 포함한다.

## 결과

도메인 test job은 전체 `Build`와 동시에 시작할 수 있다. 따라서 일반적인 단일 도메인
변경의 실행 경로는 `Build + 도메인 테스트`의 합이 아니라 두 작업 중 더 오래 걸리는
시간에 가까워진다. 실제 절감 시간은 GitHub-hosted runner 대기 시간과 Gradle cache
상태에 따라 달라지므로 첫 PR 실행에서 확인해야 한다.

`build.gradle.kts`, `settings.gradle.kts`, `gradle/**`, `ci.yml` 같은 공통 경로가
변경되면 기존 `shared` 규칙에 따라 모든 도메인 테스트가 계속 실행된다. 관련 없는
테스트만 건너뛰되, 공통 빌드 계약의 영향 범위는 축소하지 않았다.

별도 JMH task는 두 workflow에서 호출하지 않고, benchmark 전용 프로젝트도 compile
build 대상에서 제외한다. 일반 `test`에 섞여 있던 Lettuce,
Redisson, HTTP client, workflow 실행 모델 benchmark는 동일한 `benchmark` tag로 분류해
CI 자원을 사용하지 않게 했다. benchmark fixture와 지원 코드의 correctness test는 일반
회귀 테스트로 남겨 성능 실행 제외가 기능 검증 공백으로 이어지지 않게 했다.

별도 image startup-workload gate는 52개 image family를 순차 검증하므로 일반 CI에서
40분 이상 걸릴 수 있다. 이를 없애지는 않고, Daily CI에서는 Testcontainers 서버나 gate
자체가 바뀌는 경우와 수동 실행으로 한정했다. 일반 모듈 변경은 해당 도메인의 실제
Testcontainers 사용 테스트가 맡고, 전체 image family drift는 Nightly와 release gate가
계속 검증한다.

Coveralls 실패도 같은 범위 문제였다. PR `32640318114`의 Daily aggregate에는 45개
Kover report가 있었고 custom aggregate는 `92.50%`였지만, `develop` 기준 실행
`32632717769`는 path-filter 결과 2개 report만 업로드했다. 따라서 Coveralls가 서로 다른
부분 집계를 저장소 전체 기준선으로 비교해 `Coverage decreased (-6.7%) to 81.715%`를
보고했다. 이는 report 생성·업로드 실패가 아니라 외부 비교 범위가 불변하지 않았던
계약 오류다.

## 검증

- RED: 기존 workflow에서 12개 도메인 job이 `build`를 기다려 계약 테스트가 실패했다.
- GREEN: 도메인 job의 `needs`를 `changes`, `catalog-governance`로 바꾼 뒤 계약 테스트
  4개가 통과했다.
- `Build`는 `validate-wrapper`, `catalog-governance`를 계속 기다리고 `CI Status`가
  `build`를 계속 집계하는지 확인했다.
- `Coverage Report`가 image gate를 제외한 모든 coverage job을 계속 기다리는지
  확인했다.
- benchmark 제외 계약은 RED 6 failures에서 시작해 workflow opt-out, Gradle tag filter,
  네 benchmark class의 tag를 연결한 뒤 전체 7개 계약 테스트가 통과했다.
- 네 benchmark class만 각각 선택한 Gradle 검증에서 모두 `0 passing`과
  `No tests found for given includes`를 확인해 실행 계획에서 제외됐음을 증명했다.
- image gate 선택 실행 계약은 기존 workflow에서 일반 `shared`·workflow·계약 테스트
  변경까지 트리거해 RED가 되었고, 관련 경로와 수동 실행만 남긴 뒤 전체 12개 계약
  테스트가 통과했다.
- Nightly의 `full`·`testcontainers` 전체 gate와 Testcontainers 본체·Spring bridge
  테스트가 유지되는지 계약으로 확인했다.
- Coveralls 범위 계약은 Daily 업로드가 존재하는 상태에서 RED가 되었고, Daily 업로드
  제거와 Nightly `full` 전용 업로드·Testcontainers shard 제외를 적용한 뒤 Kover 계약
  테스트 26개, 도메인 CI 계약 테스트 12개, Kafka4/CSV coverage 검증, `actionlint`가
  통과했다.
- 수정 전 PR에서 GitHub Actions 자체는 모두 성공했고 Coveralls만 실패했으며, 현재
  원인은 비동기 Coveralls 비교 범위로 재현했다. 새 head의 hosted CI에서 Daily에
  `coverage/coveralls` status가 다시 생성되지 않는지 확인해야 한다.

## 놓친 점과 주의사항

path filter로 job이 `skipped`됐다는 사실은 해당 영역의 테스트 coverage가 검증됐다는
뜻이 아니다. 변경 영향이 공통 경계를 통과할 수 있는 파일은 `shared`에 유지하고,
새 도메인이나 공통 build logic을 추가할 때 filter와 의존 그래프 계약을 함께 갱신해야
한다.

Daily CI에서 image gate job이 `skipped`됐다는 결과는 image family coverage 자체를
검증했다는 뜻이 아니다. 이는 해당 diff가 별도 startup-workload 검증 대상이 아니라는
트리거 판정의 증거이며, 전체 family coverage는 Nightly `full`·`testcontainers`와
release gate의 성공으로 판단해야 한다.

Daily Kover aggregate가 성공했다는 사실도 Coveralls 저장소 기준선과 동일한 범위를
사용했다는 뜻이 아니다. path-filter CI에서는 외부 coverage publisher를 연결하지 말고,
전체 범위가 보장되는 Nightly `full` 결과만 장기 기준선으로 사용해야 한다. Nightly
matrix에 같은 모듈의 부분 report를 추가할 때는 Coveralls 입력에서 중복 shard를
제외하거나 먼저 합치는 계약을 갱신한다.

Manual Documentation은 Gradle module inventory와 `docs/manual/manifest.yaml`의
모듈 목록도 대조한다. `bluetape4k-testcontainers-spring` 모듈이 추가된 뒤 manifest와
EN/KO manual이 함께 등록되지 않아, `build.gradle.kts` 변경만으로도 이 계약이 실패했다.
새 모듈을 추가할 때는 `manifest.yaml`, 생성된 manifest JSON, 양쪽 locale 문서를
같은 변경에서 갱신하고 `exportManualModuleInventory`와 매뉴얼 검증을 실행한다.

전체 `Build`가 일찍 실패해도 이미 시작한 도메인 테스트는 계속 실행될 수 있어 실패한
실행의 runner 사용량은 늘 수 있다. 성공 경로의 직렬 대기를 제거하고 독립된 실패 증거를
함께 수집하기 위한 의도적인 절충이며, `CI Status`는 어느 한쪽 실패도 성공으로 바꾸지 않는다.

## 향후 지침

- 새 도메인 test job은 `Build`를 직렬 선행조건으로 추가하지 않는다.
- 도메인 job은 변경 감지와 공통 catalog 검증 뒤에 실행하고, 전체 `Build`와 병렬화한다.
- 새 coverage job을 추가하면 `Coverage Report`의 `needs`, expected manifest, artifact
  계약을 같은 변경에서 갱신한다.
- Coverage publisher는 invariant full-scope input에서만 실행한다. Daily처럼 선택된
  도메인만 테스트하는 workflow에 Coveralls를 다시 연결하지 않는다.
- 새 JUnit benchmark는 `@Tag("benchmark")`를 붙인다. CI나 Nightly에 benchmark를 다시
  포함하려면 일반 test에 섞지 말고 별도 opt-in workflow와 실행 예산을 둔다.
- 새 Testcontainers image family나 gate 구현 파일을 추가하면 Daily image gate path
  filter, manifest, 선택 계약을 함께 갱신한다. 일반 `shared` 트리거는 다시 추가하지 않는다.
- mock server image를 추가하거나 이동하면 실제 소비 도메인의 path filter에도 연결한다.
- 첫 hosted PR 실행에서는 job 시작 시각과 전체 소요 시간을 비교해 병렬 실행이 실제로
  적용됐는지 확인한다.

## 문서 SPW 감사

- SPW-01: PASS — 대상 독자는 CI 유지보수자이고, 근거는 현재 `ci.yml`, Manual
  Documentation 실행, `testcontainers-spring` README·소스·테스트다.
- SPW-02: PASS — 배경, 결정, 결과, 검증, 매뉴얼 manifest 보수, 놓친 점, 향후 가드를
  기록했다.
- SPW-03: PASS — 식별자와 명령을 보존하고 한국어 기술 문장으로 작성했다.
- SPW-04: PASS — 의존 그래프, coverage fail-closed 계약, Gradle inventory와 manual
  manifest 정렬을 workflow·검증기·소스와 대조했다.
- SPW-05: PASS — 최종 diff와 EN/KO Markdown을 다시 읽고 매뉴얼 계약·링크·식별자·검증
  범위를 확인했다.
