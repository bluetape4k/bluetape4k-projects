# Issue #1839 inline fallback review

## Review 범위와 근거

대상은 `b0b1720220e7f2e3432683e75082538b2602f4fa` 기준 [Issue #1839](https://github.com/bluetape4k/bluetape4k-projects/issues/1839) 변경이다. `.github/workflows/ci.yml`, `.github/workflows/nightly-tests.yml`, `scripts/verify-catalog-workflows.py`, `tests/test_catalog_workflows.py`의 source patch SHA-256은 `81540fcf51fde60c8be1e567ce8a44e3ec66ae5ca0273ca66a698d720b9fb4bc`다. 검토 근거는 네 source 파일 diff, 2026-10-24 testlog, issue 본문과 milestone, [실패 run #39408775102](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/39408775102), [임시 PR #1838](https://github.com/bluetape4k/bluetape4k-projects/pull/1838), `docs/lessons/2026-10-24-issue-1839-mongodb-runner-kernel.md`다.

native code, architecture, writer review lane은 모두 report 전에 tool-level `usage limit` 오류로 종료됐다. 오류 응답은 사용자 계정의 전체 usage 상태를 입증하지 않는다. 아래 내용은 main-session inline fallback이며 독립 reviewer attestation이 아니다. 요청된 모델은 `gpt-6-luna max`였고, 이 세션의 실제 runtime 모델 provenance는 독립적으로 확인할 수 없었다.

## Verdict

Inline verdict: PASS; P0=0, P1=0.

- Inline code review: PASS.
- Inline architecture review: WATCH — workflow scope와 `ubuntu-24.04` 배치는 Issue #1839의 임시 `ubuntu-22.04` pin 제거 및 지원 runner 이전 요구와 맞는다. 실제 hosted runner kernel과 MongoDB 테스트가 확인될 때까지 runtime 호환성은 증명되지 않았다.
- Inline writer review: PASS — 새 lesson과 testlog가 과거 실행 kernel의 불확실성, 변경 경로, 검증 결과 및 미완료 경계를 구분한다.
- Public Kotlin API/ABI: N/A — 이번 diff는 GitHub Actions workflow, Python workflow verifier/test, 한국어 문서만 변경한다.

## 검토 결과

### 코드 및 workflow

- PR CI의 Test/Data는 MongoDB와 데이터 테스트를 같은 job에서 실행하므로 `ubuntu-24.04`를 사용한다. Spring Boot에서는 `batch-nosql`만 해당 runner로 보낸다. 진단은 대상 테스트보다 앞에서 `ImageOS`, `ImageVersion`, `/etc/os-release`, `uname -r`를 출력한다. 근거: `.github/workflows/ci.yml:1390-1417`, `.github/workflows/ci.yml:1474-1529`.
- Nightly에서는 Data `nosql`, Spring Boot `batch-nosql`, Testcontainers `storage-cache`, image-gate shard만 MongoDB 실행 runner와 kernel 진단을 받는다. 다른 Data group은 기존 runner를 유지한다. 근거: `.github/workflows/nightly-tests.yml:994-1045`, `.github/workflows/nightly-tests.yml:1085-1137`, `.github/workflows/nightly-tests.yml:1189-1230`, `.github/workflows/nightly-tests.yml:1283-1320`.
- verifier는 MongoDB task와 matrix group의 연결, runner 조건, 진단 순서, retry 입력, shell 및 실패 전달을 고정한다. Nightly plan의 Python heredoc과 matrix output 연결, scope별 필수 결과도 검사한다. 별도 regression tests는 주석 decoy, skip/failure masking, 위험 환경 변수, YAML alias/merge, Gradle `-x`/`--dry-run`, 누락 image-gate shard 및 skipped coverage/status 변형을 거부한다.
- 새로운 runtime dependency나 Kotlin API/ABI 변경은 없다. verifier는 workflow 텍스트와 허용된 Python plan AST를 정적으로 검사하므로 실제 hosted kernel이나 MongoDB 프로세스의 호환성을 자체 증명하지 않는다.

### P2 — hosted runner/kernel 검증 대기

위치: `.github/workflows/ci.yml:1392`, `.github/workflows/ci.yml:1476`, `.github/workflows/nightly-tests.yml:996`, `.github/workflows/nightly-tests.yml:1087`.

Issue #1839가 인용한 과거 실행은 MongoDB kernel `6.19+` fatal log를 남겼지만 정확한 `uname -r`를 보관하지 않았다. `ubuntu-24.04` label은 실제 kernel 번호를 고정하지 않는다. 이번 workflow는 exact-head hosted CI에서 runner/image/kernel을 출력하도록 한다. PR `Test / Data`와 `Test / Spring Boot (batch-nosql)`의 실행 kernel 및 MongoDB 테스트가 모두 확인될 때까지 이 P2는 미해결이며, issue 완료와 merge-ready를 막는다. kernel이 `6.19+`이거나 테스트가 실패하면 그 hosted 환경에서 통과하는 지원 runner로 조정하고 exact-head 검증을 반복한다.

## 검증 및 범위 제한

2026-10-24 testlog는 이 source patch에서 128개 `test_catalog_workflows.py` 테스트, catalog workflow verifier, Ruff, Python compile, 두 workflow의 `actionlint`, `git diff --check`가 통과했다고 기록한다. 한국어 문서 terminology audit 결과는 `findings=[]`다. 이 inline review turn에서는 테스트를 다시 실행하지 않았다. PR exact-head CI와 실제 runner/kernel 로그는 아직 없다.

Issue #1839 live read-back은 `OPEN`, milestone `2.1.0`, assignee `debop`, labels `bug`/`ci`다. 별도로 확인한 Issue #1832는 `CLOSED`지만 Dependabot alert #235는 `open`, `fixed_at=null`이다. 이 live 상태는 기존 #1832 lesson의 날짜가 붙은 기록을 확인하는 데만 사용했으며 #1839의 runner 판정 근거로 사용하지 않았다.

## Writer DoD

- SPW-01 — PASS: 대상 독자, 목적, source patch, issue 상태, native reviewer 오류와 실제 runner/kernel 미확인 범위를 적었다.
- SPW-02 — PASS: 범위, verdict, file/line 근거, severity, disposition, validation, gaps를 포함했다.
- SPW-03 — PASS: 한국어 문장을 사용하고 `gpt-6-luna max`, issue 번호, runner label, command, checksum 등 정확한 token은 유지했다.
- SPW-04 — PASS: workflow diff, verifier/tests, testlog, lesson 및 live issue 상태를 대조했다. 과거 실행의 실제 kernel을 단정하지 않았다.
- SPW-05 — PASS: commit 전 최종 Markdown read-back과 링크·heading·코드 token 대조를 마쳤다. Hosted runner/kernel 확인은 PENDING이다.
