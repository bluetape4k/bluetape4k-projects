# 2026-05-24 1.9.2 Post-Release Development Line

## 배경

`bluetape4k-projects` `1.9.1`은 release되었지만 repository에는 IO HTTP patch lane을 위한 active
`1.9.2` milestone이 남아 있었다. Stale PR 하나는 `develop`을 바로 `1.10.0`으로 옮기려 했고, 이는
active patch milestone을 건너뛰게 된다.

## 결정

Committed base version을 `1.9.1`에서 `1.9.2`로 이동한다. `gradle.properties`의 `snapshotVersion=`은
비워 둔다. SNAPSHOT publishing은 `-PsnapshotVersion=-SNAPSHOT`을 전달해야 한다.

## 결과

`develop`은 active `1.9.2` patch milestone과 정렬되고, `1.10.0`은 Ktor module-family minor line을 위해
reserved 상태로 남는다.

## 검증

- `./gradlew properties --no-configuration-cache --no-daemon --quiet`
- `./gradlew properties -PsnapshotVersion=-SNAPSHOT --no-configuration-cache --no-daemon --quiet`
- `git diff --check`

## 향후 가드

Active patch milestone이 다음 release를 소유하고 있다면 그 milestone이 명시적으로 deferred 또는
closed되기 전에는 새 minor development line을 열지 않는다.
