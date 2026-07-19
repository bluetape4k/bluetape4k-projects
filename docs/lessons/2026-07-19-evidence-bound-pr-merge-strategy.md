# Evidence-bound PR의 merge 전략

## Context

Issue #757 allocation evidence는 measurement commit, rollback identity, delivery commit을 immutable Git SHA와 hash chain으로 기록한다. 기능 branch를 최신 `develop` 위로 rebase하면서 소스 tree는 같았지만 delivery commit SHA가 바뀌었고, committed manifest의 delivery provenance가 현재 branch ancestry와 어긋났다.

## Root Cause

일반 PR에서는 rebase나 squash가 변경 내용을 유지하면서 history를 정리하는 방법일 수 있다. 그러나 evidence-bound PR에서는 commit identity 자체가 검증 입력이다. rebase, amend, squash는 동일한 tree라도 authenticated commit SHA를 새로 만들기 때문에 기존 Git ancestry를 끊는다. Strict `validate-committed`는 committed manifest/evidence semantics와 `delivery.git_commit`의 `HEAD` ancestry만 검증하며, measurement/rollback SHA의 Git-object reachability나 ancestry까지 증명하지 않는다.

## Decision

Evidence가 commit SHA에 bind된 뒤 branch base를 갱신할 때는 `origin/develop`을 merge commit으로 병합한다. 이 정책은 이미 branch ancestor인 authenticated commits의 graph를 보존한다. 이전 history rewrite에서 사라진 measurement/rollback ancestry를 merge commit이 복구한다고 주장해서는 안 된다. GitHub에서 최종 PR을 병합할 때도 merge-commit 방식만 사용한다. rebase-merge와 squash-merge는 남아 있는 authenticated delivery ancestry를 다시 쓰므로 금지한다.

이미 tree-equivalent rebase가 완료된 경우에는 일반 JSON 편집 대신 감사 CLI를 사용한다.

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  rebind-rebased-delivery \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --rebased-commit <full-rebased-commit-sha>
```

이 복구는 working manifest가 committed `HEAD`와 동일하고, 이전 delivery commit과 rebased candidate의 전체 tree가 같으며, candidate가 현재 `HEAD`의 ancestor일 때만 허용한다. 이 historical rebase 복구가 다시 세우는 ancestry는 delivery commit뿐이다. Measurement/rollback SHA는 immutable manifest/hash-chain identity로 남지만 Git-object reachability는 검증하지 않는다. 조건이 하나라도 실패하거나 measurement/rollback의 executable full ancestry가 필요한데 명시적인 retained anchor가 없으면 provenance 보존을 주장하지 않고 fresh evidence lifecycle을 수행한다.

## Verification

- `rebind-rebased-delivery` 전후 JSON 구조 diff는 `delivery.git_commit`과 `report_input_sha256` 두 필드로 제한한다.
- Raw measurement, verdict/reason, rollback, file path/hash는 변경하지 않는다.
- Allocation report는 `render-report`로만 재생성하고 semantic diff가 `Delivery commit` 한 줄인지 확인한다.
- Commit 전 `validate-report`, `git diff --check`, 전체 Python evidence test를 실행한다.
- Commit 후 strict `validate-committed`와 `validate-report`를 다시 실행해 committed bytes, evidence semantics, delivery ancestry를 검증한다. 이 결과를 measurement/rollback ancestry 증명으로 확대 해석하지 않는다.

## Future Agent Guidance

Evidence-bound branch를 동기화할 때 history 정리를 기본값으로 가정하지 않는다. 먼저 manifest와 rollback artifact가 어떤 commit SHA를 identity로 기록하고, 그 SHA 중 무엇이 실제 branch ancestor인지 구분한다. Merge commit은 이미 존재하는 ancestry만 보존한다. force-push, rebase, amend, squash를 사용하지 않는다. 불가피하게 이미 rebase된 상태를 인수했다면 tree equivalence와 delivery ancestry를 CLI로 증명할 수 있을 때만 재바인딩하고, measurement/rollback ancestry까지 복구됐다고 보고하지 않는다. 그 full ancestry가 필요하고 retained anchor가 없다면 fresh evidence를 수집하며, 수동 hash 계산이나 raw artifact 수정으로 검증을 우회하지 않는다.
