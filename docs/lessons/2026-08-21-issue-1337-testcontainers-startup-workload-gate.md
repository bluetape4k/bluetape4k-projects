# Issue #1337 Testcontainers startup·workload gate lesson

## 배경

Issue #1331/#1332에서 52개 Docker image tag를 고정했지만, 기존 CI는
`testing/testcontainers` 전체 startup/workload를 PR 필수 경로에서 실행하지 않았습니다.
Nightly matrix는 여러 shard를 병렬 실행해 개별 family의 readiness와 registry 장애를
stable release 증거로 재사용하기 어려웠습니다. K3s는 API·pod workload 준비와 port open을
구분해야 했고, Docker Hub pull rate limit도 실제 환경 실패로 재현되었습니다.

## 결정

- 52개 family를 `scripts/testcontainers_image_gate_manifest.json`에 선언하고 source,
  test class, image/tag, readiness, workload, diagnostics를 하나의 digest로 묶었습니다.
- Python 표준 라이브러리 runner가 family를 순차 실행하고 `success`, `product_failure`,
  `infrastructure_failure`, `blocked`를 분리합니다.
- PR CI는 changed scope, Nightly와 안정 버전 배포는 full scope를 사용합니다.
- stable publish는 `52/52`, `release_gate=true`, 모든 실패 분류 0을 선행조건으로 합니다.
- Docker/K3s 진단은 bounded·secret-free artifact로 남기고 rate limit을 면제하지 않습니다.
- public Kotlin API와 기존 wrapper readiness 구현은 변경하지 않습니다.

## 구현 증거

- manifest 52개와 Kotlin/EN/KO README/test parity 정적 검증: PASS
- runner 단위 테스트(성공, 제품 실패, 인프라 retry, timeout, redaction, zero-exit 차단): PASS
- PR CI changed gate, Nightly full sequential lane, release publish dependency: PASS
- `actionlint` 세 workflow: PASS
- 실제 Docker/K3s full matrix와 hosted release dispatch `52/52`: PENDING

## 배운 점

1. 고정된 image tag 목록은 pull/start/workload 성공을 증명하지 않습니다. 실행 증거와
   태그 parity를 같은 manifest digest에 연결해야 release 판단이 재현됩니다.
2. 병렬 Testcontainers matrix의 green 결과만으로는 family별 readiness와 실패 원인을
   구분할 수 없습니다. 무거운 Docker lane은 별도 순차 경계와 bounded 진단이 필요합니다.
3. exit code 0만으로 성공을 선언하면 결과가 누락된 fake/조기 종료를 통과시킬 수 있습니다.
   runner는 Gradle `BUILD SUCCESSFUL` 증거가 없으면 `blocked`로 닫습니다.
4. registry rate limit은 제품 실패가 아니지만 release를 허용할 근거도 아닙니다. 인프라
   실패로 분류하고 인증·mirror·owner/time-bound를 보존해야 합니다.

## 후속 위험

- hosted runner의 Docker Hub 인증·mirror 설정이 실제로 제공되지 않으면 full gate가
  `infrastructure_failure`가 됩니다. 이 상태를 retry로 덮지 않고 release hold로 유지합니다.
- K3s privileged runner와 Curator session readiness의 실제 소요 시간이 기존 timeout과
  맞지 않을 수 있습니다. 첫 Nightly 결과에서 elapsed time과 진단량을 확인합니다.
- 52개 family를 개별 Gradle invocation으로 실행하므로 Nightly 시간이 길어질 수 있습니다.
  실행 증거가 안정된 뒤에만 grouping 최적화를 검토하며, 병렬화로 순차 계약을 약화하지 않습니다.

## 문서 SPW 감사

- SPW-01 source/evidence lock: PASS — #1337/#1331/#1332와 현재 52개 source/readme/test를 대조했습니다.
- SPW-02 contract completeness: PASS — manifest schema, 분류, artifact, release prerequisite를 고정했습니다.
- SPW-03 Korean technical register/naturalness: PASS — 한국어 문서 terminology audit 결과가 0건입니다.
- SPW-04 traceability: PASS — issue 요구사항을 manifest, runner, CI/Nightly/release, runbook으로 연결했습니다.
- SPW-05 final Markdown read-back: PASS — 생성 문서를 마지막 줄까지 읽고 명령·경로·PENDING 범위를 확인했습니다.
