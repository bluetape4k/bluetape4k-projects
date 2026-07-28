# Snapshot Version Parameterization

배경: Central Portal release를 위해 `-SNAPSHOT` 제거만 목적으로 `gradle.properties`를 수정해서는 안 된다.

결정: `snapshotVersion=`은 기본적으로 비워 두고, `publish-snapshot.yml`이
`-PsnapshotVersion=-SNAPSHOT`을 전달하도록 한다.

결과: `develop`은 release-ready 상태를 유지하고, snapshot publishing은 workflow command에서
명시적으로만 수행된다.

검증: `actionlint .github/workflows/publish-snapshot.yml`.

향후 가드: `gradle.properties`의 기본값으로 `snapshotVersion=-SNAPSHOT`을 다시 도입하지 않는다.
