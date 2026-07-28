# 2026-05-22 1.9.0 Snapshot Version

배경: 1.9.0 milestone에는 남은 open issue가 없었고, 다음 단계는 `develop`에서 Maven Central SNAPSHOT을
publish하는 것이었다.

결정: Publish 전에 `baseVersion`을 `1.8.1`에서 `1.9.0`으로 올리고, `snapshotVersion=-SNAPSHOT`은
기본값으로 유지한다.

결과: Snapshot publishing은 정상 `publishAggregationToCentralSnapshots` workflow를 사용할 수 있고,
reproducible commit에서 `1.9.0-SNAPSHOT` artifact를 생성할 수 있다.

검증: Version bump가 `develop`에 도달한 뒤 Gradle project version과 GitHub Publish Snapshot workflow를 확인한다.

향후 가드: Command time에만 `-PbaseVersion`을 override해 1.9.0 release-candidate snapshot을 publish하지
말고, version source를 먼저 commit한다.
