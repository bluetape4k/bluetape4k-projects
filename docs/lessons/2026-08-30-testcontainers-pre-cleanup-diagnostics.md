# Testcontainers 진단은 테스트 JVM 종료 전에 증거를 보존해야 한다

## 배경

`examples.yml`은 각 Gradle test task가 끝난 뒤 Testcontainers label이 붙은 Docker
container ID를 비교하고 로그를 수집했다. 그러나 callbackFlow의 실제 Kafka 테스트는
`ShutdownQueue`에 `KafkaServer`를 등록한다. 테스트 JVM이 종료되면 container가 먼저
정리될 수 있으므로, workflow가 사후 container 목록을 읽을 때는 이미 대상이 사라진다.

GitHub Actions run
[`32996076207`](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/32996076207)의
artifact `9616858277`에서는 이 경계가 그대로 드러났다. workflow run은 실패했고
callbackFlow 테스트 자체는 통과했지만, 해당 task의 `manifest.json`은
`containers=[]`만 기록했고 collector는 성공을 반환했다. 따라서 이 artifact만으로는
“container를 관찰하지 못함”과 “진단 수집 성공”을 구분할 수 없었다.

## 근본 원인

Gradle task 종료는 진단 대상 resource가 남아 있다는 보장이 아니다. Testcontainers와
테스트 fixture가 소유한 container는 JVM shutdown hook이나 명시적 cleanup에서 먼저
제거될 수 있다. task 전후 `docker ps` 차집합은 남아 있는 container만 찾으므로 transient
container의 실패 증거를 보존하는 수단이 될 수 없다.

collector도 빈 container 목록을 정상 결과로 처리했고 manifest에 수집 상태를 기록하지
않았다. 이 때문에 resource lifecycle의 관찰 실패가 성공 상태로 축약됐다.

## 결정

- callbackFlow Kafka 테스트는 실패를 다시 던지기 전에 container ID를 파일명으로 사용해
  `docker logs --tail 200`의 bounded raw broker log와 선택 metadata receipt를
  `build/testcontainers-raw`에 기록한다.
- CI가 export 경로를 지정한 경우 실제 Kafka 테스트는 성공 여부와 관계없이 cleanup 전에
  raw log를 만든다. 같은 Gradle task의 뒤쪽 테스트가 실패하더라도 이미 종료된 broker의
  증거를 보존하기 위해서다. 로컬 기본 실행은 임시 디렉터리를 사용하며 `ShutdownQueue`와
  Testcontainers cleanup 동작을 변경하지 않는다.
- workflow는 `bluetape4k.testcontainers.diagnostics.dir` system property로 test JVM의
  export 경로를 정하고, Gradle task의 실제 exit code와 metadata receipt를 collector에
  전달한다.
- collector는 raw log를 repository 내부 경로와 allowlist의 immutable image digest로
  검증하고, 기존 sanitizer와 전체 byte cap을 적용한 결과만 artifact 경로에 쓴다.
- raw log는 collector 실행 직후 삭제하며 artifact upload 대상에 포함하지 않는다.
- manifest는 `container_not_observed`, `diagnostic_collection_failed`,
  `diagnostic_collection_succeeded` 중 하나와 `task_exit_code`를 기록한다.
- 실패한 task에서 container와 pre-cleanup log를 모두 관찰하지 못하면 collector도
  실패를 반환해 진단 누락을 fail-closed로 처리한다.

## 결과와 검증

- 기존 artifact 10개의 manifest가 모두 `containers=[]`이고 수집 상태 필드가 없음을
  확인해 이전 계약의 오판을 재현했다.
- Python 회귀 테스트는 실패 task의 무관찰, collector 실패, live container 성공,
  pre-cleanup log sanitize, workflow wiring을 구분한다.
- Kotlin 회귀 테스트는 raw broker log가 `2_000_000` bytes를 넘지 않고, 실제 Kafka
  container의 `id`, `image`, `image_id`, `created`가 cleanup 전에 기록됨을 검증한다.
- 실제 Kafka callbackFlow 테스트에서 cleanup 전 raw log와 metadata receipt가 생성되고,
  collector가 이를 sanitize한 뒤 workflow가 raw 파일을 제거하는 경계를 확인했다.
- `py_compile`, `ruff`, `actionlint`, callbackFlow 대상 테스트와 coroutines-demo 전체
  테스트를 실행했다.

## 놓친 점

사후 진단 수집 코드는 container label과 image allowlist를 엄격히 검사했지만, 검사 시점에
대상이 살아 있다는 전제를 별도로 검증하지 않았다. 보안 경계가 정확해도 lifecycle 경계가
틀리면 artifact에는 사용할 수 있는 증거가 남지 않는다.

또한 빈 목록은 합법적인 성공 task에서도 발생할 수 있으므로, `containers=[]`만으로
collector 실패를 판정할 수도 없다. task exit code와 진단 상태를 함께 기록해야 한다.

## 향후 지침

- process-owned transient resource의 실패 진단은 process 종료 전에 증거를 기록하거나,
  process가 종료 전에 bounded evidence를 export하도록 한다.
- 사후 collector는 “관찰하지 못함”, “수집 실패”, “수집 성공”을 서로 다른 상태로 남긴다.
- 실패 task에서 필수 진단이 없으면 artifact upload의 존재만으로 통과시키지 않는다.
- raw 진단은 업로드하지 않고 repository 내부 임시 경로, size cap, sanitizer, immutable
  allowlist를 모두 통과한 결과만 보존한다.
- cleanup을 비활성화해 진단을 얻는 방식은 기본 선택으로 사용하지 않는다. resource
  ownership을 바꾸지 않는 pre-cleanup export를 우선한다.

## 문서 SPW 감사

- SPW-01: PASS — CI 유지보수자를 대상으로 issue #1536, workflow, collector, callbackFlow
  fixture와 기존 artifact를 근거로 범위를 고정했다.
- SPW-02: PASS — 배경, 근본 원인, 결정, 결과와 검증, 놓친 점, 향후 지침을 기록했다.
- SPW-03: PASS — 식별자와 상태 token을 보존하고 한국어 기술 문장으로 작성했다.
- SPW-04: PASS — source, workflow, artifact manifest, 로컬 검증 결과를 문서의 각 주장과
  대조했다.
- SPW-05: PASS — 최종 Markdown 구조와 링크, 수치, 명령·상태 token을 다시 확인했다.
