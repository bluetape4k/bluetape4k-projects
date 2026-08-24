# Issue #1486 구현 계획 독립 리뷰 기록

## 1. 범위와 판정

- **대상 계획**:
  `docs/superpowers/plans/2026-08-24-issue-1486-ignite2-arm64-image-gate-plan.md`
- **대상 단계**: Type A Full Feature Step 3-R
- **검토 관점**: stability/performance, operator/security, developer/API,
  user/caller
- **구현 상태**: production source, Gradle dependency, workflow runtime은 아직
  변경하지 않았다. 계획과 이미 승인된 설계·리뷰 산출물만 검토했다.

초기 검토는 P0=0, P1=2로 BLOCK이었다. 유효한 P1은 parent/child가 같은
manifest 파일을 넓게 소유한 점과 Release workflow의 exact `needs`/checkout
ref 계약이 계획에 고정되지 않은 점이었다. 이전 설계의 5분/317분 수치 지적은
현재 설계의 6분/318분 기준과 맞지 않는 stale evidence로 판정했다.

## 2. 교정 전 검토 결과

| 관점 | P0 | P1 | P2/P3 | 최초 판정 | 핵심 finding |
|---|---:|---:|---:|---|---|
| Stability/performance | 0 | 1 | 3 | BLOCK | manifest ownership 경계, 실제 8 MiB/setup wall-clock fixture와 ARM evidence가 아직 구현 전 |
| Operator/security | 0 | 1 | 3 | BLOCK | Release `resolve-version` dependency/ref 및 release artifact literal이 계획에 불충분하게 고정됨 |
| Developer/API | 0 | 0 | 4 | WATCH | TDD RED 순서, custom no-tag, exact argv, manifest subtree는 교정 필요 |
| User/caller | 0 | 0 | 1 | WATCH | 실제 native runtime은 첫 Nightly/Release까지 미검증 |
| **통합 최초** | **0** | **2** | **11** | **BLOCK** | P1 교정 후 재검토 필요 |

## 3. 반영한 교정

1. Parent는 `scripts/testcontainers_image_gate_manifest.json`을 read-only로
   두고, child만 JSON pointer `/families/*` 중 `families[id=ignite2]` object
   hunk를 수정하도록 plan에 고정했다. exact-head diff ownership 검사를 통합
   단계에 추가했다.
2. 기준선 `3/3 skipped`는 Task 0 증거로 분리했다. dependency와 test를 추가하고
   `@Disabled`를 제거한 뒤 production source를 바꾸기 전에 실제 assertion
   failure를 관찰하는 별도 RED 순서를 고정했다.
3. canonical no-tag, custom no-tag fail-fast, custom explicit-tag unknown-arch
   허용, `DockerImageName` overload의 tagless 동작을 테스트·KDoc·README 계약으로
   분리했다.
4. x64/arm64 runner CLI와 arm64 최종 Gradle argv를 전체 literal로 고정하고,
   arbitrary task/JVM property/Gradle option을 정적 fixture에서 거부하도록 했다.
5. Release x64/arm64 gate는 `needs.resolve-version.outputs.ref`를 checkout에
   사용하고, `publish.needs`를 정확히
   `[resolve-version, testcontainers-manifest-contract, testcontainers-image-gate,
   testcontainers-ignite2-arm64-image-gate]`로 보존하도록 plan에 추가했다.
   Release artifact 이름은
   `release-testcontainers-image-gate-${{ github.run_id }}`와
   `release-testcontainers-ignite2-arm64-image-gate-${{ github.run_id }}`로
   고정하고 두 artifact의 `if: always()`, `if-no-files-found: error`, 30일
   보존을 assert하도록 했다.
6. 52-family 최악 8 MiB artifact와 30분 setup slack을 포함한 wall-clock budget
   fixture를 최종 DoD의 필수 조건으로 유지했다. 해당 증거와 실제 ARM/native
   pull·startup·workload 증거 전에는 완료를 주장하지 않는다.

## 4. 최종 Step 3-R 결과

| 관점 | P0 | P1 | P2/P3 | 최종 판정 | 잔여 WATCH |
|---|---:|---:|---:|---|---|
| Stability/performance | 0 | 0 | 3 | PASS with WATCH | 8 MiB/setup wall-clock fixture와 실제 ARM queue/runtime |
| Operator/security | 0 | 0 | 2 | PASS with WATCH | mutable Docker tag 및 첫 Release native evidence |
| Developer/API | 0 | 0 | 1 | PASS with WATCH | custom-tag runtime fixture |
| User/caller | 0 | 0 | 1 | PASS with WATCH | 첫 Nightly/Release의 실제 workload |
| **통합** | **0** | **0** | **7** | **PASS with WATCH** | 구현·CI에서만 해소 가능한 증거를 PENDING으로 유지 |

### DoD

- [x] plan의 parent/child ownership, exact-head train, timeout authority가
      명시됐다.
- [x] TDD baseline/RED/GREEN 순서와 실패 분류 경계가 명시됐다.
- [x] runner/workflow exact argv, Release `resolve-version`/`needs`/artifact
      계약이 명시됐다.
- [x] P0=0, P1=0으로 수렴했다.
- [ ] synthetic 8 MiB/setup wall-clock fixture 실행 — 구현 후 PENDING
- [ ] 실제 ARM runner pull/startup/workload/cleanup evidence — 첫 Nightly/Release
      후 PENDING
- [ ] production code, workflow, Gradle 변경 및 테스트 — Step 3-R 이후 PENDING

**결론: PASS with WATCH — 계획을 commit하고 TDD 구현 단계로 진행한다.**
mutable tag, ARM queue 및 실제 runtime evidence는 범위상 WATCH이며, 해당
증거가 없는 상태에서 PR merge나 완료 판정을 하지 않는다.
