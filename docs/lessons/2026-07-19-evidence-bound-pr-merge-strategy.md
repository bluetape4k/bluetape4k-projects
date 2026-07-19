# Evidence-bound PR의 merge 전략

## Context

Issue #757 allocation evidence는 measurement commit, rollback lineage, delivery commit을 immutable Git SHA와 hash chain으로 인증한다. 기능 branch를 최신 `develop` 위로 rebase하면서 소스 tree는 같았지만 delivery commit SHA가 바뀌었고, committed manifest의 provenance가 현재 branch lineage와 어긋났다.

## Root Cause

일반 PR에서는 rebase나 squash가 변경 내용을 유지하면서 history를 정리하는 방법일 수 있다. 그러나 evidence-bound PR에서는 commit identity 자체가 검증 입력이다. rebase, amend, squash는 동일한 tree라도 authenticated commit SHA를 새로 만들기 때문에 manifest, report input, rollback preparation/finalization이 가리키는 delivery lineage를 끊는다.

## Decision

Evidence가 commit SHA에 bind된 뒤 branch base를 갱신할 때는 `origin/develop`을 merge commit으로 병합한다. GitHub에서 최종 PR을 병합할 때도 merge-commit 방식만 사용한다. rebase-merge와 squash-merge는 authenticated delivery lineage를 다시 쓰므로 금지한다.

이미 tree-equivalent rebase가 완료된 경우에는 일반 JSON 편집 대신 감사 CLI를 사용한다.

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  rebind-rebased-delivery \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --rebased-commit <full-rebased-commit-sha>
```

이 복구는 working manifest가 committed `HEAD`와 동일하고, 이전 delivery commit과 rebased candidate의 전체 tree가 같으며, candidate가 현재 `HEAD`의 ancestor일 때만 허용한다. 조건이 하나라도 실패하면 provenance를 다시 쓰지 않고 fresh evidence lifecycle을 수행한다.

## Verification

- `rebind-rebased-delivery` 전후 JSON 구조 diff는 `delivery.git_commit`과 `report_input_sha256` 두 필드로 제한한다.
- Raw measurement, verdict/reason, rollback, file path/hash는 변경하지 않는다.
- Allocation report는 `render-report`로만 재생성하고 semantic diff가 `Delivery commit` 한 줄인지 확인한다.
- Commit 전 `validate-report`, `git diff --check`, 전체 Python evidence test를 실행한다.
- Commit 후 strict `validate-committed`와 `validate-report`를 다시 실행해 committed bytes와 lineage를 검증한다.

## Future Agent Guidance

Evidence-bound branch를 동기화할 때 history 정리를 기본값으로 가정하지 않는다. 먼저 manifest와 rollback artifact가 commit SHA를 인증하는지 확인한다. 인증한다면 merge commit으로 ancestry를 보존하고, force-push, rebase, amend, squash를 사용하지 않는다. 불가피하게 이미 rebase된 상태를 인수했다면 tree equivalence와 ancestry를 CLI로 증명할 수 있을 때만 재바인딩하고, 수동 hash 계산이나 raw artifact 수정으로 검증을 우회하지 않는다.
