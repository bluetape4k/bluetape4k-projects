# WIP - bluetape4k-projects

- 기준일: 2026-09-02 KST
- release: `2.0.0` 완료
- 현재 milestone: `2.0.0-post`

## 2.0.0 배포 결과

`2.0.0`은 배포 준비가 아니라 공개 완료 상태다.

| 항목 | 상태 | immutable 증거 |
|---|---|---|
| stable tag | DONE | signed annotated `2.0.0`, commit `8165a8989e0075e7c17c489bf3000bf41fef8232` |
| exact-head Full Nightly | DONE | [run 33522892818](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33522892818), tag commit과 동일 |
| Maven Central publication | DONE | [run 33537327623](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33537327623) |
| GitHub Release | DONE | [2.0.0](https://github.com/bluetape4k/bluetape4k-projects/releases/tag/2.0.0) |
| milestone | DONE | `2.0.0` milestone #13 closed |

release workflow의 dispatch branch SHA는 publication source identity가 아니다. 위
stable tag commit과 exact-head Nightly attestation이 공개 artifact의 source identity다.

## 2.0.0-post 진행 상태

완료된 release gate를 다시 실행하지 않는다. 아래 작업은 공개 artifact를 변경하지
않는 후속 정리다.

| 이슈 | 상태 | 범위 |
|---|---|---|
| [#1451](https://github.com/bluetape4k/bluetape4k-projects/issues/1451) | 진행 | 9개 consumer를 최종 Dependencies catalog SHA로 handoff |
| [#1601](https://github.com/bluetape4k/bluetape4k-projects/issues/1601) | 진행 | release catalog 기본값에서 숨은 repository variable 제거 |
| [#1602](https://github.com/bluetape4k/bluetape4k-projects/issues/1602) | 진행 | central manual 재현 명령 복구 |
| [#1603](https://github.com/bluetape4k/bluetape4k-projects/issues/1603) | 진행 | 이 문서의 release 완료 상태 반영 |
| [#1604](https://github.com/bluetape4k/bluetape4k-projects/issues/1604) | 진행 | 공개 metadata 언어 계약 통일 |
| [#1605](https://github.com/bluetape4k/bluetape4k-projects/issues/1605) | 진행 | Actions/Testcontainers 중복과 catalog 전환 병목 제거 |
| [#1600](https://github.com/bluetape4k/bluetape4k-projects/issues/1600) | 대기 | 2.0.0 이후 Ignite 2 지원 제거 |

## 다음 순서

1. final Dependencies catalog SHA를 Projects와 나머지 consumer의 checked-in 기본값에 반영한다.
2. repository variable을 release 입력 기본값으로 사용하지 않도록 제거한다.
3. CI/Nightly/release의 Testcontainers 증거를 inventory하고 exact-head당 한 canonical gate만 유지한다.
4. CodeQL compile에는 build cache를 사용하지 않는다.
5. 별도 source 변경으로 Ignite 2 지원을 제거하고 검증한다.

mutable `develop` SHA나 완료된 release run을 새 source freeze 대상으로 다시 고정하지
않는다. 후속 PR은 각 PR의 exact head CI만 검증한다.

상세 단일 책임과 측정값은
[`docs/operations/ecosystem-actions-gate-contract.md`](docs/operations/ecosystem-actions-gate-contract.md)에 기록한다.
