# WIP - bluetape4k-projects

- 기준일: 2026-09-01 KST
- 범위: `2.0.0` 정식 배포 준비
- milestone: `2.0.0` (열림 0 / 닫힘 261)
- 열린 release blocker: 없음

## 현재 상태

`2.0.0` milestone의 이슈와 PR은 모두 닫혔다. PR
[#1591](https://github.com/bluetape4k/bluetape4k-projects/pull/1591)에서 stable
tag의 immutable SHA 해석, exact-head Full Nightly 검증, 외부 Central manual의
fail-closed 계약과 영문/한글 문서 동등성 검증을 통합했다.

현재 `develop`의 검증 기준 SHA는
`15c6e640409528d44ec35b580e7d0403cc46448a`이며, 해당 SHA의 post-merge CI
[run 33503042621](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/33503042621)은
26개 job이 모두 성공했다. `baseVersion=2.0.0`, `snapshotVersion=` 상태이며
`2.0.0` tag와 stable publication은 아직 생성하거나 실행하지 않았다.

## 배포 전 준비 상태

| 항목 | 상태 | 근거 / 다음 조치 |
|---|---|---|
| `2.0.0` milestone 이슈 | READY | 열린 이슈 0개, 닫힌 이슈 261개다. |
| release 문서 | IN PROGRESS | 이 문서와 `CHANGELOG.md`를 최종화한 뒤 문서 PR을 병합한다. README/README.ko의 2.0.0, Java 25, 배포 명령은 현재 계약과 일치한다. |
| `develop` post-merge CI | VERIFIED | SHA `15c6e640409528d44ec35b580e7d0403cc46448a`의 26개 job이 모두 성공했다. |
| exact-head Full Nightly | PENDING | 문서 PR 병합 후 확정된 최종 `develop` SHA에서 실행한다. |
| stable tag / publication | HOLD | 최종 Full Nightly 성공과 별도 승인 전에는 `2.0.0` tag를 만들거나 배포하지 않는다. |
| Central / GitHub Release | HOLD | Maven Central publication 검증 후 별도 권한으로 진행한다. |

## SHA 및 배포 순서 원칙

- 배포 준비 중인 mutable `develop` SHA를 문서나 workflow에 반복해서 고정하지
  않는다. 문서와 이슈 정리가 모두 끝난 뒤의 최종 head만 배포 후보로 삼는다.
- 문서 PR 병합처럼 source tree가 변경되면 이전 Nightly 결과를 재사용하지 않고
  새 exact-head Full Nightly를 실행한다.
- stable tag가 가리키는 commit은 immutable release source identity다. release
  workflow는 tag를 실행 시점에 SHA로 해석하고 publication 전 다시 검증한다.
- `2.0.0` tag 생성, publication, GitHub Release, milestone 종료는 각각의 검증과
  권한 경계를 유지한다.

## Backlog

| 이슈 | 상태 | 메모 |
|---|---|---|
| [#767](https://github.com/bluetape4k/bluetape4k-projects/issues/767) | HOLD | Owned money API 도입과 Moneta compatibility 경계는 별도 설계·마이그레이션 작업이다. |

#767은 현재 `2.0.0` release blocker가 아니다.

## 다음 단계

1. 이 문서와 `CHANGELOG.md`를 포함한 문서 PR을 병합한다.
2. 최종 `develop` head에서 exact-head Full Nightly를 실행하고 성공 상태를
   검증한다.
3. 별도 승인 후 최종 head에 `2.0.0` tag를 만들고 stable release workflow를
   실행한다.
4. Maven Central의 전체 publication 행렬을 검증한 뒤 GitHub Release와
   `2.0.0` milestone을 별도 권한으로 마무리한다.
