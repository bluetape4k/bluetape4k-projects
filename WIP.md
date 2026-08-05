# WIP - bluetape4k-projects

- 스냅샷: 2026-08-05 KST
- 범위: `1.12.1` BOM publication inventory 복구 릴리스
- 열린 release blocker: [#1313](https://github.com/bluetape4k/bluetape4k-projects/issues/1313)

## 현재 상태

`1.12.0`은 Maven Central에 게시됐지만 BOM에 실제로 게시되지 않는 benchmark와
application-only 모듈 제약이 포함됐다. Central artifact는 불변이므로 #1313에서
공통 publication 분류기와 생성 POM 인벤토리 검증을 추가하고 `1.12.1`로 복구한다.

## 배포 전 준비 상태

| 항목 | 상태 | 근거 / 다음 조치 |
|---|---|---|
| `1.12.1` 이슈 | IN PROGRESS | #1313 구현과 전체 검증을 진행한다. |
| publication inventory | VERIFIED LOCALLY | 생성된 75개 POM과 BOM 제약 집합이 일치한다. |
| `CHANGELOG.md` | PREPARED | `1.12.1` 복구 사유와 검증 게이트를 기록한다. |
| milestone 종료 | HOLD | Central의 전체 아티팩트 행렬이 HTTP 200이 된 뒤 닫는다. |
| release / publish | HOLD | PR merge, Nightly, snapshot 검증 후 `1.12.1`을 게시한다. |

## 신규 이슈 처리 원칙

- 배포 전 검증에서 재현 가능한 결함, 문서 누락, 호환성 위험이 확인되면
  `1.12.1` milestone에 추가하고 이 문서의 상태를 다시 갱신한다.
- caller evidence가 없는 API 후보나 장기 설계 항목은 release blocker로
  승격하지 않는다. 구체적인 production caller와 검증 가능한 계약이 생기면
  별도 이슈로 재개한다.
- milestone을 닫는 행위는 전체 Bluetape 배포 준비가 끝난 뒤 별도 판단한다.

## Backlog

| 이슈 | 상태 | 메모 |
|---|---|---|
| [#767](https://github.com/bluetape4k/bluetape4k-projects/issues/767) | HOLD | Owned money API 도입과 Moneta compatibility 경계는 별도 설계·마이그레이션 작업이다. |
| [#1070](https://github.com/bluetape4k/bluetape4k-projects/issues/1070) | HOLD | Event Sourcing 및 projection primitive 수요를 검증하는 장기 설계 항목이다. |

두 backlog 이슈는 현재 `1.12.1` release blocker가 아니다.

## 다음 단계

1. 전체 Bluetape repository의 배포 준비 상태를 확인한다.
2. #1313 PR을 merge하고 exact-head Nightly를 통과시킨다.
3. `1.12.1-SNAPSHOT`의 전체 POM 행렬을 검증한 뒤 stable을 게시한다.
4. Central 전체 행렬과 GitHub Release를 확인한 뒤 `1.12.0`, `1.12.1`
   milestone을 정리한다.
