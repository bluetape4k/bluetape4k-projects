# Issue #1337 Testcontainers 이미지 family gate 운영 runbook

## 목적

이 runbook은 52개 Docker 기반 Testcontainers 서버 family의 startup,
애플리케이션 readiness, 대표 workload를 같은 기준으로 실행하고 실패 원인을
재현 가능한 증거로 남기기 위한 운영 계약입니다. source of truth는
[`scripts/testcontainers_image_gate_manifest.json`](../../scripts/testcontainers_image_gate_manifest.json)이며,
Kotlin wrapper·테스트·EN/KO README와 정적 parity 검증을 함께 통과해야 합니다.

대상 workflow는 다음과 같습니다.

| 단계 | scope | 성공 조건 | artifact |
|---|---|---|---|
| PR CI | `changed` | 변경 family 모두 `success` 또는 변경 없음 `skipped` | `testcontainers-image-gate-*` |
| Nightly | `full` 또는 `testcontainers` | 선택 family 전체 `success`, 순차 실행 | `nightly-testcontainers-image-gate-*` |
| 안정 버전 배포 | `full` | `52/52`, `release_gate=true`, 제품·인프라·차단 0 | `release-testcontainers-image-gate-*` |

## 실행 명령

저장소 루트에서 실행합니다. Docker Hub 인증과 mirror는 환경변수 또는 CI secret으로
전달하고 명령행에 credential을 넣지 않습니다.

```bash
# 변경 family 확인
python3 scripts/run_testcontainers_image_gate.py \
  --scope changed \
  --report-dir build/reports/testcontainers-image-gate \
  --max-attempts 2 \
  --timeout-minutes 30

# 전체 family 확인
./gradlew :bluetape4k-mock-web-server:jibDockerBuild \
  :bluetape4k-mock-webflux-server:jibDockerBuild \
  --no-configuration-cache
python3 scripts/run_testcontainers_image_gate.py \
  --scope full \
  --report-dir build/reports/testcontainers-image-gate \
  --max-attempts 2 \
  --timeout-minutes 30
```

`--max-attempts`는 runner에서 최대 3회로 제한합니다. 각 family는 하나씩 실행하며,
`BUILD SUCCESSFUL` 출력이 없거나 Gradle 결과가 누락되면 성공으로 처리하지 않습니다.

## Manifest 변경 절차

1. `testing/testcontainers/src/main/kotlin/**`의 `IMAGE`/`TAG`, 대응 `*Test.kt`, EN/KO README를 함께 확인합니다.
2. manifest 항목의 `source`, `testSource`, `image`, `tag`, `testPattern`, `readiness`, `workload`, `diagnostics`, `releaseRequired`를 갱신합니다.
3. 정적 계약과 실행기 단위 테스트를 실행합니다.

```bash
python3 -m unittest \
  scripts/test_testcontainers_contract.py \
  scripts/test_testcontainers_image_gate.py \
  scripts/test_run_testcontainers_image_gate.py -v
```

4. `git diff --check`와 workflow `actionlint`를 통과시키고, manifest digest와 변경 family를 PR DoD에 기록합니다.
5. 이미지 태그를 바꿀 때는 기존 호환성 fixture를 일반 최신 태그로 치환하지 않습니다. 실제 startup/workload 결과와 rollback identity를 함께 남깁니다.

## 결과 artifact 계약

`summary.json`은 다음 필드를 포함합니다.

| 필드 | 의미 |
|---|---|
| `manifest_digest` | 실행한 manifest의 SHA-256 |
| `selected` / `coverage` | 선택 family 수와 성공 수 (`success/selected`) |
| `success` | startup·readiness·workload 실행이 성공한 family 수 |
| `product_failure` | 테스트 assertion 또는 제품 동작 실패 수 |
| `infrastructure_failure` | registry, Docker daemon, timeout, K3s 환경 실패 수 |
| `blocked` | manifest/결과 누락 또는 prerequisite 미충족 수 |
| `release_gate` | 안정 버전 배포 허용 여부 |
| `results` | family별 image/tag, test pattern, 시도 결과, 진단 경로 |

실패 family마다 `<id>.json`이 생성되며 `summary.md`에는 분류별 count와 stable
release gate가 표시됩니다. stdout/stderr, command, Docker/K3s 진단은 bounded
redaction을 거치며 password/token/secret/authorization/API key/Bearer 값은
`<redacted>`로 치환됩니다.

## 실패 분류와 조치

| 분류 | 대표 신호 | 조치 | gate 처리 |
|---|---|---|---|
| `product_failure` | JUnit assertion, 예상 응답/연결 계약 불일치 | 해당 wrapper와 대표 workload를 먼저 수정·재검증 | 실패 |
| `infrastructure_failure` | `TOOMANYREQUESTS`, pull/auth 오류, daemon 연결 거부, timeout | Docker/registry/K3s 진단과 owner/time-bound를 기록하고 bounded retry | 실패 |
| `blocked` | manifest drift, test 결과 누락, `BUILD SUCCESSFUL` 증거 없음 | source/manifest/runner prerequisite를 복구한 뒤 재실행 | 실패 |
| `success` | 각 family test가 `BUILD SUCCESSFUL`로 종료 | artifact와 digest를 보존 | 통과 |

rate limit은 면제 사유가 아닙니다. 인증 또는 mirror를 구성할 수 없으면
`infrastructure_failure`로 남기고 안정 버전 배포를 중단합니다. 재시도 횟수를 늘려
실패를 숨기지 않습니다.

## K3s 진단

`K3sServer` 실패 시 runner가 Docker inspect/logs/events와 함께 다음 bounded
진단을 수집합니다.

```bash
kubectl get pods --all-namespaces -o wide
kubectl get events --all-namespaces --sort-by=.lastTimestamp
kubectl logs --all-namespaces --all-containers --tail=200
```

컨테이너 port open만으로 readiness를 판정하지 않습니다. K3s API와 pod/workload가
준비되지 않으면 `infrastructure_failure` 또는 `product_failure` 원인을 보존합니다.
ZooKeeper는 port open이 아니라 Curator session readiness를 사용합니다.

## Rollback과 재실행

- runner/manifest 변경이 문제이면 해당 child commit을 bounded revert하고 기존
  `scripts/test_testcontainers_contract.py`, Nightly matrix, release workflow를
  복원한 뒤 static contract를 먼저 통과시킵니다.
- 이미지 tag 변경 문제이면 이전에 검증된 tag와 manifest digest를 rollback identity로
  지정하고 full gate를 다시 통과시킵니다. 무제한 retry나 gate skip은 정상 rollback이
  아닙니다.
- release gate 실패 시 `publish`는 실행되지 않습니다. 새 배포를 시도하기 전에
  실패 artifact, exact tag SHA, owner, 종료 시각, forward-fix를 기록합니다.
- 이미 Maven Central publish가 시작됐다면 추가 publish를 중단하고 release owner가
  tag와 artifact identity를 확인합니다. 이 runbook은 외부 artifact 삭제를 수행하지
  않습니다.

## 현재 검증 상태

이 문서 작성 시점에는 manifest/source/README/test parity, runner 단위 테스트,
workflow 정적 검증은 통과했습니다. 실제 Docker Hub pull, K3s workload, hosted
Nightly, release dispatch의 `52/52` 결과는 아직 실행 전이므로 `PENDING`입니다.
그 결과가 확보되기 전에는 PR을 merge-ready 또는 stable publish-ready로 표시하지
않습니다.
