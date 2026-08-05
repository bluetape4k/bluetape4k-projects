# WIP - bluetape4k-projects

- 스냅샷: 2026-08-03 KST
- 범위: `1.12.0` 전체 Bluetape 배포 전 사전 준비와 신규 이슈 유입 대기
- 열린 이슈 수: 2개

## 현재 상태

Live GitHub 상태:

- `1.12.0`: 열린 이슈 0개, 문서 준비 PR [#1310](https://github.com/bluetape4k/bluetape4k-projects/pull/1310) 1개
- `backlog`: 열린 이슈 2개
- repository 전체: 열린 이슈 2개
- release 구현 PR: 열린 PR 0개 (`#1310`은 이 상태를 기록하는 문서 PR)
- milestone `1.12.0`: 추가 이슈가 발견될 수 있으므로 배포 준비가 끝날 때까지 OPEN 유지

`bluetape4k-projects`의 현재 milestone 작업은 모두 정리됐다. 이 문서는
repository의 release-note 사전 준비 상태를 기록하며, 전체 Bluetape 배포나
Maven Central publication 완료를 선언하지 않는다.

## 배포 전 준비 상태

| 항목 | 상태 | 근거 / 다음 조치 |
|---|---|---|
| `1.12.0` 이슈 | READY | OPEN 0건. 새 결함이나 누락이 확인되면 milestone에 추가한다. |
| release 구현 PR | READY | OPEN 0건. 문서 준비 PR `#1310`은 merge 대기 중이며, 새 이슈 구현 PR이 생기면 다시 검증한다. |
| `CHANGELOG.md` | PREPARED | `1.12.0` 주요 변경, 마이그레이션, 성능·정책 경계를 반영한다. |
| milestone 종료 | HOLD | 혹시 모를 추가 이슈를 받을 수 있도록 OPEN 상태를 유지한다. |
| release / publish | NOT STARTED | 별도 Type P preflight에서 버전, 태그, catalog/BOM, consumer 범위를 고정한 뒤 진행한다. |

## 신규 이슈 처리 원칙

- 배포 전 검증에서 재현 가능한 결함, 문서 누락, 호환성 위험이 확인되면
  `1.12.0` milestone에 추가하고 이 문서의 상태를 다시 갱신한다.
- caller evidence가 없는 API 후보나 장기 설계 항목은 release blocker로
  승격하지 않는다. 구체적인 production caller와 검증 가능한 계약이 생기면
  별도 이슈로 재개한다.
- milestone을 닫는 행위는 전체 Bluetape 배포 준비가 끝난 뒤 별도 판단한다.

## Backlog

| 이슈 | 상태 | 메모 |
|---|---|---|
| [#767](https://github.com/bluetape4k/bluetape4k-projects/issues/767) | HOLD | Owned money API 도입과 Moneta compatibility 경계는 별도 설계·마이그레이션 작업이다. |
| [#1070](https://github.com/bluetape4k/bluetape4k-projects/issues/1070) | HOLD | Event Sourcing 및 projection primitive 수요를 검증하는 장기 설계 항목이다. |

두 backlog 이슈는 현재 `1.12.0` release blocker가 아니다.

## 다음 단계

1. 전체 Bluetape repository의 배포 준비 상태를 확인한다.
2. 새 `1.12.0` 이슈가 생기면 구현·검증하고 이 문서를 다시 갱신한다.
3. 추가 이슈가 없고 consumer 범위가 고정되면 Type P release/publish
   preflight를 시작한다.
4. milestone 종료는 release/publish 결정과 분리해 명시적으로 승인받는다.
