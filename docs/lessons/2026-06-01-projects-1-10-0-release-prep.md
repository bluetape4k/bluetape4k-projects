# projects 1.10.0 release prep

## 배경

남은 milestone issue가 완료됐고 repository release scope와 무관함이 확인된 뒤,
`bluetape4k-projects`에는 1.10.0 stable release gate 준비가 필요했다.

## 결정

release metadata만 준비한다. `baseVersion=1.10.0`을 설정하고 `snapshotVersion=`은 비워
두며, release-prep PR을 만들기 전에 curated `CHANGELOG.md` section을 추가한다.

## 결과

release-prep branch는 Ktor module family, performance work, cancellation fix, release
guard, Zip Slip hardening에서 나온 1.10.0 user-facing change를 기록한다.

## 검증

- worktree가 깨끗한 `origin/develop`에서 시작했음을 확인했다.
- `release.yml`이 `version`, optional `diagnoseSigning`, optional `catalogRef`를
  요구함을 확인했다.
- stable release dispatch를 위해 `snapshotVersion=`이 비어 있음을 확인했다.

## 다음 번

release-prep PR이 CI를 통과하고, merge된 release state에 대한 Nightly/snapshot
validation이 최신이며, 정확한 `1.10.0` BOM과 대표 module POM에 대한 Maven Central
verification이 준비되기 전에는 stable release를 dispatch하지 않는다.
