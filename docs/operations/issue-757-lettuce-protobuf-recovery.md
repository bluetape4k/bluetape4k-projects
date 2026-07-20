# Issue #757 Lettuce Protobuf 복구 가이드

이 문서는 issue #757의 retained Lettuce Protobuf target-write 경로를 되돌리거나 운영 중 격리할 때 필요한
증거와 승인 경계를 정의한다. 릴리스 전 소스 rollback과 릴리스 후 dispatch-only recovery는 서로 다른 절차다.

## 릴리스 전 regression rollback

canonical JMH 두 실행에서 Lettuce candidate가 `regressed`로 판정되면 direct target dispatch와 그 전용 테스트를
같이 제거하고, 기록된 rollback bundle을 새 measurement authority에 결합한다. 이미 accepted로 승격된 raw
evidence를 임의로 덮어쓰지 않는다. `run-evidence.py record-rollback`과 `finalize-rollback`이 만든 인증된 lineage만
사용하고, 변경된 JAR로 canonical A/B를 다시 실행한다. 이 단계는 게시된 Maven artifact를 변경하지 않는다.

<!-- issue-757-post-release-recovery -->
## 릴리스 후 dispatch-only recovery

릴리스 전 rollback contract를 릴리스 후 recovery에 재사용하지 않는다.
릴리스 후 recovery는 published public ABI를 제거하지 않고 dispatch-only change만 허용한다.

먼저 #757을 reopen하고 incident, affected release/GAV, consumer, environment, region, owner/contact를 기록한다.
Published baseline과 planned recovery의 repository, GAV, digest, commit, tree, command, target을 동반 JSON 문서에
고정한다. Planned recovery는 coordinate, digest, command, target, environment, region으로 고정한다.

별도 배포 승인 전에 다음 read-only 검증을 수행한다. 첫 명령은 현재 retained ABI evidence를 검증하고, 두 번째
명령은 현재 운영 checklist가 JSON 문법과 필수 최상위 구조를 유지하는지 확인한다.

```bash
set -euo pipefail
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py validate \
  --manifest .omx/evidence/issue-757-abi-v1/payload-manifest.json
python3 -m json.tool \
  docs/operations/templates/issue-757-lettuce-protobuf-recovery.json >/dev/null
```

Distinct reviewer의 fresh approval은 planned recovery digest와 exact command를 대상으로 받아야 한다. 승인 전에는
artifact publish, workflow dispatch, 배포, Redis 데이터 변경을 실행하지 않는다. 배포 후 actual identity는 approved
planned recovery와 exact-equal이어야 한다.

관측 window에는 최소한 encode failure rate, Redis command failure rate, allocation, latency, affected consumer의
decode 성공률을 기록한다. 사전에 고정한 threshold를 모두 만족하고 consumer impact가 해소된 뒤에만 close 단계로
진행한다. Close 기록에는 published baseline/recovery digest, 배포 결과, 관측 query/window/threshold, owner,
reviewer approval, #757 링크를 포함한다. ABI가 달라졌거나 actual identity가 계획과 다르면 즉시 중단하고 별도
release/redeploy 승인을 받는다.
