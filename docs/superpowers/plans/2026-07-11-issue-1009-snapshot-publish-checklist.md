# Issue #1009 스냅샷 게시 체크리스트

## Release 식별 정보

| Field               | Value                                                                  |
|---------------------|------------------------------------------------------------------------|
| 흐름                | AWS consumer validation으로 이어지는 routine replacement snapshot       |
| Producer            | `d8974d2688b543a35a0fc307cffb1b5e04257036`의 `bluetape4k-projects`    |
| Branch              | `test/issue-1009-floci-non-reusable`                                   |
| Version             | `1.11.1-SNAPSHOT`                                                      |
| 변경 artifact       | `io.github.bluetape4k:bluetape4k-testcontainers`                       |
| 게시 surface        | root `nmcpPublishAggregationToCentralPortalSnapshots` workflow task    |
| Consumer            | `bluetape4k-aws`; catalog가 이미 `1.11.1-SNAPSHOT`을 선택한다.        |
| 권한                | 현재 snapshot을 사용해 진행하라는 2026-07-11 사용자 지시             |

## Topology

`bluetape4k-projects`(snapshot producer) -> `bluetape4k-aws`(consumer validation).
이 흐름에는 catalog ref, 안정 release, tag 또는 Maven Central release가
포함되지 않는다.

## 사전 확인

- [x] PRE-01: Routine snapshot selected at version `1.11.1-SNAPSHOT`.
- [x] PRE-02: Issue #1009와 PR #1010이 열려 있고 `debop`에 할당되어 있으며,
  milestone `1.12.0`이 활성 상태다. closeout은 snapshot 범위 밖이다.
- [x] PRE-03: 이 routine snapshot에는 N/A이며 안정 changelog는 게시하지 않는다.
- [x] PRE-04: local full module이 test 449개, 실패 0개, skip 25개로 통과했다.
  CI run `29125650184`도 정확한 producer SHA에서 통과했다.
- [x] PRE-05: `baseVersion=1.11.1`이고 `snapshotVersion=`은 비어 있으며,
  workflow는 `-PsnapshotVersion=-SNAPSHOT`을 전달한다.
- [x] PRE-06: 내부 release reference를 추가하지 않으므로 N/A다.
- [x] PRE-07: 생성된 `bluetape4k-testcontainers` POM에 예상 coordinate와
  Apache-2.0 license가 있고 누락된 dependency version과 dependency SNAPSHOT
  reference가 0개다. Kotlin의 선택적 strict POM checker는 기존 developer
  organization metadata 누락을 보고하지만, 선언된 snapshot workflow는 그
  checker를 실행하지 않으며 Central은 run `29117088678`에서 동일한 POM
  shape를 수락했다.
- [x] PRE-08: snapshot run `29117088678`이 같은 branch에서 signing과 게시를
  성공적으로 완료했다. replacement dispatch는 새 secret-shape 검사를 위해
  workflow가 선언한 `diagnoseSigning` input을 활성화한다.
- [x] PRE-09: 변경 artifact matrix는
  `io.github.bluetape4k:bluetape4k-testcontainers:1.11.1-SNAPSHOT`이다. root
  aggregation이 변경되지 않은 module을 다시 게시할 수 있지만 consumer
  검증은 이 변경 coordinate에 고정한다.
- [x] PRE-10: `d8974d268`의 `.github/workflows/publish-snapshot.yml`은 선택적
  boolean input `diagnoseSigning`만 선언하며 dispatch도 이 input만 사용한다.

## Dispatch hold

- [x] Branch SHA `d8974d2688b543a35a0fc307cffb1b5e04257036` is pushed to `origin`.
- [x] CI run `29125650184` completed successfully for the exact SHA.
- [x] dispatch 전에 workflow schema, branch SHA, issue/PR 상태와 현재 Central
  metadata(`1.11.1-20260710.195152-13`)를 갱신했다.
- [x] replacement workflow run `29126253922`가 `d8974d268`에서 signing
  diagnostics green으로 성공했다.
- [x] Central metadata가 `1.11.1-20260710.220125-14`로 진행되었고 timestamp가
  붙은 `bluetape4k-testcontainers` POM이 HTTP 200을 반환했다.
- [x] AWS가 정확한 `20260710.220125-14` snapshot을 resolve했고
  `S3KtorClientFlociTest`가 통과했다.

## 범위 제외

- Stable release, tag, GitHub Release, and milestone closure.
- Removing repository test mutexes before consumer validation.
- Repairing the repository-wide developer organization POM metadata gap.
