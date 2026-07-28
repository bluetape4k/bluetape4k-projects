# 이슈 #811: Cassandra mapper runtime dependency

## 배경

`bluetape4k-cassandra`는 public mapper extension signature에서 DataStax mapper runtime
type을 노출한다.

- `EntityHelper<T>` receiver와 parameter
- `NullSavingStrategy` parameter default

모듈은 `java-driver-mapper-runtime`을 `compileOnly`로 선언했고, 정규 test classpath는
`compileOnly`를 확장했다. 그래서 모듈 테스트는 통과했지만 문서화된
`bluetape4k-cassandra` 의존성만 사용하는 소비자는 mapper helper API를 컴파일할 수
없었다.

## 결정

Mapper helper를 `bluetape4k-cassandra` public API 계약의 일부로 다룬다.

- `libs.cassandra.java.driver.mapper.runtime`을 `compileOnly`에서 `api`로 승격한다.
- 정규 test classpath가 아니라 `main.output + runtimeClasspath`를 compile/runtime classpath로 사용하는 `consumerRuntimeTest` source set을 추가한다.
- 단일 `bluetape4k-cassandra` artifact가 `io.bluetape4k.cassandra.mapper`에 필요한 mapper runtime을 포함한다고 문서화한다.

## 검증

- RED: `./gradlew :bluetape4k-cassandra:compileConsumerRuntimeTestKotlin --no-build-cache --no-daemon --no-configuration-cache`가 unresolved `com.datastax.oss.driver.api.mapper.*`와 `Cannot access class 'EntityHelper'`로 실패했다.
- GREEN: mapper runtime을 `api`로 승격한 뒤 같은 compile task가 통과했다.
- `./gradlew :bluetape4k-cassandra:dependencies --configuration runtimeClasspath --no-daemon --no-configuration-cache`에서 `org.apache.cassandra:java-driver-mapper-runtime:4.19.2`가 확인되었다.
- `./gradlew :bluetape4k-cassandra:compileKotlin :bluetape4k-cassandra:compileTestKotlin :bluetape4k-cassandra:compileConsumerRuntimeTestKotlin --warning-mode all --no-daemon --no-configuration-cache`가 통과했다.
- `./gradlew :bluetape4k-cassandra:test :bluetape4k-cassandra:consumerRuntimeTest --no-build-cache --no-daemon --no-configuration-cache`가 178개 module test와 1개 consumer runtime test로 통과했다.
- `git diff --check`가 통과했다.

## 향후 지침

bluetape4k module이 public signature에서 third-party type을 노출하면 dependency를
export(`api`)하거나 API를 optional artifact 뒤로 이동해야 한다. `compileOnly`를
상속하는 정규 테스트는 충분한 증거가 아니므로 compile/runtime 계약을 검증하는
consumer-style source set을 추가한다.

## 동시성 helper gate

Shared mutable state, coroutine lifecycle, structured concurrency, virtual-thread
behavior는 변경되지 않았다. `MultithreadingTester`, `SuspendedJobTester`,
`StructuredTaskScopeTester`는 이 dependency metadata와 consumer compile-contract
수정에 적용되지 않는다.
