# bt4k Version Catalog Consumption

## 배경

`bluetape4k-projects`는 centrally governed version을 local catalog에 직접 들고 있었다. Fory가 한 예다.
Approved version은 이미 `bluetape4k-dependencies` published catalog에 있었지만, projects repository는
이를 consume하기 위해 여전히 local catalog edit이 필요했다.

## 결정

`io.github.bluetape4k:bluetape4k-version-catalog`를 `bt4k` Gradle version catalog로 import하고,
centrally governed dependency version의 source로 사용한다. Project-local coordinate에는 local `libs`
alias를 유지하되, direct Fory version은 `libs`에서 제거한다.

## 결과

`libs.fory.kotlin`은 이제 versionless다. Managed Fory version은 `bt4kVersion("fory-kotlin")` catalog
lookup을 통한 dependency management에서 공급되므로 future Fory version change는
`bluetape4k-dependencies`에서 시작한다.

## 검증

- `./gradlew help --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-io:dependencyInsight --configuration compileClasspath --dependency org.apache.fory:fory-kotlin --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-io:compileKotlin --no-daemon --no-configuration-cache`
- `./gradlew compileKotlin --no-daemon --no-configuration-cache`

## 향후 가이드

Version이 `bluetape4k-dependencies`에서 govern된다면 `bluetape4k-projects` local version edit 대신
`bt4k` catalog에서 읽는다. Publishing-cycle implication을 검토하기 전에는 이 repository에 전체
`bluetape4k-dependencies` BOM을 import하지 않는다.
