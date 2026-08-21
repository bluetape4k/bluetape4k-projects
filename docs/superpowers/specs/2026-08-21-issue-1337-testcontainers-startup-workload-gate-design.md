# #1337 Testcontainers startup/workload gate 설계

## 결정 요약

Epic #1418 Slot 4는 #1331에서 갱신한 Testcontainers 이미지 family가 실제로
기동되고 대표 workload를 처리하는지 stable publish 전에 검증한다. 검증 대상은
현재 태그 갱신 PR #1332가 기록한 52개 변경 family로 고정하되, 각 wrapper의
기존 테스트와 property/endpoint 계약을 재사용한다.

검증 목록을 workflow YAML에 직접 복제하지 않고, source symbol·image·tag·기존
테스트·workload probe를 한 manifest에서 관리한다. CI와 Nightly는 manifest를
읽어 변경 family만 선택하고, Testcontainers 자원을 공유하는 단계는
`max-parallel: 1`로 순차 실행한다. 모든 family의 결과와 진단 artifact가
완료되어야 stable release job이 진행된다.

## 문제와 현재 근거

- Issue #1331은 `TAG/DEFAULT_TAG` 55개를 검토했고, PR #1332는 52개 기본 태그를
  변경했다. PR #1332의 Full DB startup은 Docker Hub unauthenticated pull-rate
  limit으로 `PENDING`이었다.
- 현재 `.github/workflows/ci.yml`은 `testing/testcontainers/**`를
  `io-http` 또는 shared 경계로만 분류하며, 일반 PR에서 전체 Testcontainers
  startup matrix를 요구하지 않는다.
- `.github/workflows/nightly-tests.yml`은 Testcontainers 그룹을 실행하지만,
  기존 그룹 결과를 image family별 stable publish 증거로 연결하지 않는다.
- `BluetapeHttpServer`와 `BluetapeWebfluxServer`는 `/httpbin/get` HTTP 200을
  wait strategy로 사용한다. 이는 해당 family의 최소 workload probe로 재사용할
  수 있다.
- K3s는 privileged Docker runner가 필요하고, ZooKeeper는 listening port가 아닌
  Curator session readiness가 필요하다는 선행 이슈 증거가 있다. 각 provider의
  application-level readiness를 manifest에 명시해야 한다.
- release workflow는 현재 static Testcontainers tag contract를 검사하지만,
  실제 image startup/workload 결과를 publish prerequisite로 요구하지 않는다.

## 목표와 범위

### 목표

1. `#1331` 변경 family 52개가 manifest에 빠짐없이 표현되는지 검증한다.
2. 각 family에 대해 container startup, application readiness, 대표 workload를
   순서대로 실행한다.
3. 성공·실패·인프라 원인을 구분할 수 있는 immutable artifact를 남긴다.
4. PR/Nightly/release 경로가 같은 gate와 결과 계약을 사용하게 한다.
5. required family가 하나라도 미완료이면 stable publish를 차단한다.

### 범위 제외

- 기존 wrapper의 public API, endpoint 이름, credential/property key를 변경하지
  않는다.
- 모든 provider를 새 generic Kotlin abstraction으로 통합하지 않는다.
- Docker Hub rate limit을 성공 또는 검증 면제 상태로 처리하지 않는다.
- 이 Slot에서 이미지 태그 자체를 다시 선택하지 않는다. 태그 변경은 #1331의
  source와 문서 계약을 기준으로 한다.
- #1337의 선행 ZooKeeper readiness 수정은 재구현하지 않고 현재 계약을
  manifest adapter로 연결한다.

## 대안과 선택

### A. Manifest-driven 기존 테스트 재사용 — 선택

family별로 source symbol, image/tag, 기존 test class, readiness 방식, workload
probe를 명시한다. 실행기는 manifest를 검증한 뒤 기존 Gradle test pattern을
순차 호출하고, provider별 진단 collector를 실행한다.

- 장점: 현재 wrapper 계약을 그대로 검증하고, 52개 목록과 CI/release 경계를
  한 원본에서 추적할 수 있다.
- 단점: 신규 family가 추가될 때 manifest 항목과 probe를 함께 작성해야 한다.
- 선택 이유: public API 변경 없이 #1331 변경 집합과 stable publish gate를
  연결한다.

### B. 모든 wrapper를 위한 generic Kotlin smoke harness

공통 인터페이스로 모든 server를 감싸고 generic startup/workload를 실행한다.

- 장점: 실행 진입점이 하나다.
- 단점: provider별 readiness, credential, workload가 달라 abstraction이
  실제 계약을 숨기거나 가장 약한 공통분모로 축소될 위험이 크다.
- 거부 이유: source의 application-level readiness를 보존하지 못한다.

### C. HTTP와 K3s만 우선 검증

실패 증거가 이미 있는 HTTP/K3s family만 required로 고정한다.

- 장점: 구현과 CI 시간이 짧다.
- 단점: 52개 변경 family 전체를 stable publish 전에 검증하라는 #1337 요구를
  만족하지 못한다.
- 거부 이유: 일부 provider 성공을 전체 image family 성공으로 오인하게 된다.

## 구성 요소와 데이터 흐름

```text
TAG source + existing test inventory
        │ static contract
        ▼
image-family-gate.json
        │ select changed families
        ▼
CI/Nightly sequential runner (max-parallel=1)
        │
        ├─ pull/auth/mirror setup (secret-free logs)
        ├─ container startup
        ├─ application readiness
        ├─ representative workload
        └─ inspect/log/events + JUnit result artifact
        ▼
gate-summary.json (all required families)
        │
        ├─ PR required check
        ├─ Nightly summary/artifact
        └─ release publish prerequisite
```

### Manifest 계약

각 항목은 다음 정보를 가진다.

- `id`: 안정적인 family 식별자
- `source`: `TAG`/`DEFAULT_TAG` 선언의 실제 파일과 symbol
- `image`와 `tag`: 실행할 Docker image와 source tag
- `testPattern`: 기존 Testcontainers test class 또는 method
- `readiness`: `http`, `log`, `client-session`, `k3s-api` 중 하나와 세부 조건
- `workload`: 성공을 판정하는 실제 요청/명령과 기대 결과
- `diagnostics`: 실패 시 수집할 Docker/Kubernetes 정보
- `releaseRequired`: stable publish 차단 대상 여부

manifest 검증기는 source tag, README tag 표, Nightly pre-pull, test pattern이
서로 일치하는지 확인한다. 누락·중복·미존재 symbol·빈 workload는 즉시 실패한다.

### 실행 및 결과 계약

runner는 항목을 한 번에 하나씩 실행한다. 각 항목의 결과에는 image/tag,
resolved digest, runner/Java/Docker metadata, startup/readiness/workload
duration, outcome, failure category, artifact paths를 기록한다. 결과 파일은
`success`, `product_failure`, `infrastructure_failure`, `blocked` 중 하나를
사용하며, `infrastructure_failure`도 gate 결과는 실패로 남긴다.

Docker 실패에는 `docker inspect`, `docker logs`, container events를 남긴다.
K3s 항목은 pod status, events, logs, readiness timeout을 남긴다. 인증 정보,
registry token, 전체 환경변수는 artifact와 로그에 기록하지 않는다.

## 실패와 rollback

- 포트가 열렸지만 application workload가 실패하면 readiness 성공으로 간주하지
  않고 `product_failure`로 중단한다.
- pull rate limit, registry 인증, mirror 연결 실패는 `infrastructure_failure`로
  분류하고 재시도 횟수와 최종 원인을 남긴다. 실패한 family가 있으면 publish는
  진행하지 않는다.
- timeout은 항목별 bounded timeout을 사용하고, 진단 수집 후 다음 family로
  넘어가지 않는다. 전체 gate를 fail-closed로 종료한다.
- manifest/source drift가 발견되면 실행보다 먼저 static contract가 실패한다.
- release rollback은 이전 artifact를 성공으로 재사용하지 않는다. 새 commit에서
  gate를 다시 통과해야 하며, 이전 결과는 traceability artifact로만 보존한다.

## 호환성 및 운영 경계

- 기존 `PropertyExportingServer` property namespace, endpoint, credential,
  lifecycle을 그대로 사용한다.
- HTTP family는 `/httpbin/get` 200을 workload로 사용하고, provider별 추가
  endpoint는 manifest에 명시한다.
- K3s는 privileged runner와 별도 nightly/release preflight lane으로 유지한다.
- CI PR gate는 변경 family만 실행하지만, release gate는 release manifest에
  포함된 모든 required family를 실행한다.
- 개발 버전 배포는 이 Slot의 stable publish 차단 규칙을 우회하지 않으며,
  release workflow가 사용하는 동일한 summary schema를 소비한다.

## 수용 기준

- [ ] 52개 변경 family가 manifest에 1:1로 등록되고 source/README/Nightly
      pre-pull과 일치한다.
- [ ] 변경된 family를 선택하는 PR required job이 추가되고 `max-parallel: 1`
      순차 실행을 보장한다.
- [ ] 모든 항목이 startup과 application-level workload를 별도로 판정한다.
- [ ] Docker/K3s 실패 진단 artifact와 failure category가 summary에 기록된다.
- [ ] registry auth/mirror/retry 설정이 로그에 secret을 남기지 않고 rate limit을
      검증 면제 사유로 허용하지 않는다.
- [ ] stable release workflow가 required summary 성공 없이는 publish하지 않는다.
- [ ] static manifest contract, `actionlint`, 대상 Gradle compile/test,
      Testcontainers 순차 검증, `git diff --check`가 통과한다.
- [ ] 기존 public API와 property/endpoint/credential/lifecycle 테스트가
      회귀 없이 통과한다.

## DoD와 중단 조건

Slot 4 DoD는 PR exact head에서 required checks가 모두 성공하고, manifest
coverage가 `52/52`, product/infrastructure failure가 `0`, stable publish
prerequisite가 live read-back된 상태다. 하나라도 누락되거나 Docker/K3s
증거가 `PENDING`이면 merge-ready로 보고하지 않고 해당 family를 복구한다.

현재 Epic #1418은 Slot 3 완료 후 `3/4`이며, 이 설계가 완료되어도 #1337의
구현·CI·release 검증이 끝날 때까지 Epic은 `PENDING`이다.

## 근거 원본

- GitHub Issue #1337, #1331, merged PR #1332
- `.github/workflows/ci.yml`
- `.github/workflows/nightly-tests.yml`
- `.github/workflows/release.yml`
- `scripts/test_testcontainers_contract.py`
- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/BluetapeHttpServer.kt`
- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/http/BluetapeWebfluxServer.kt`
- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/K3sServer.kt`
- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/infra/ZookeeperServerSupport.kt`
