# WIP - bluetape4k-projects

- 기준일: 2026-09-01 KST
- 범위: `2.0.0` 정식 배포 준비
- milestone: `2.0.0` (실시간 상태는 GitHub milestone을 기준으로 확인)
- release blocker: tag 생성 직전에 GitHub의 열린 이슈와 PR을 다시 확인

## 현재 상태

`baseVersion=2.0.0`, `snapshotVersion=` 상태이며 release 준비에 필요한 source
변경을 모두 마친 뒤 `develop` head를 한 번만 최종 후보로 선택한다. WIP에는
mutable `develop` SHA나 과거 CI run ID를 현재 기준처럼 기록하지 않는다.

PR [#1591](https://github.com/bluetape4k/bluetape4k-projects/pull/1591)은 stable
tag의 immutable SHA 해석, exact-head Full Nightly 검증, 외부 Central manual의
fail-closed 계약과 영문/한글 문서 동등성 검증을 통합했다. PR
[#1595](https://github.com/bluetape4k/bluetape4k-projects/pull/1595)는 image gate의
일시적 `infrastructure_failure`만 최대 두 번 시도하고 `product_failure`는 즉시
종료하는 bounded retry 경계를 고정했다.

## 배포 전 준비 상태

| 항목 | 상태 | 근거 / 다음 조치 |
|---|---|---|
| `2.0.0` milestone 이슈 | GATE | tag 생성 직전에 live GitHub 상태에서 열린 release blocker가 0개인지 확인한다. |
| release 문서 | READY | WIP, CHANGELOG, README/README.ko의 2.0.0, Java 25, 배포 명령을 source freeze 전에 최종화한다. |
| `develop` post-merge CI | GATE | source freeze를 만든 마지막 PR의 merge SHA에서 CI와 Dependency Submission 성공을 확인한다. |
| exact-head Full Nightly | GATE | source freeze 뒤 최종 `develop` head에서 한 번 실행하고 `publish_eligible=true`를 확인한다. |
| stable tag / publication | GATE | Full Nightly가 증명한 exact head에 signed annotated `2.0.0` tag를 만든다. |
| Central / GitHub Release | GATE | release workflow 성공 뒤 Maven Central 전체 publication과 GitHub Release를 공개 상태에서 확인한다. |

## SHA 및 배포 순서 원칙

- 배포 준비 중인 mutable `develop` SHA를 문서나 workflow에 반복해서 고정하지
  않는다. 문서와 이슈 정리가 모두 끝난 뒤의 최종 head만 배포 후보로 삼는다.
- 최종 후보 SHA와 Full Nightly run ID는 GitHub Actions와 release attestation에
  남기고, 다음 source 변경을 유발하는 WIP 최신화 대상으로 삼지 않는다.
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

1. release 준비 source 변경을 모두 병합하고 source freeze를 선언한다.
2. 마지막 merge SHA의 post-merge CI와 Dependency Submission을 확인한다.
3. 같은 SHA에서 exact-head Full Nightly를 실행해 `publish_eligible=true`를
   검증한다.
4. 검증된 exact head에 signed annotated `2.0.0` tag를 만들고 release
   workflow를 실행한다.
5. Maven Central 전체 publication과 GitHub Release를 공개 상태에서 확인한 뒤
   `2.0.0` milestone을 마무리하고 downstream handoff를 시작한다.
