# 이슈 #834 Redis serializer runtime dependency

issue #834는 `bluetape4k-spring-boot-redis`가 `RedisBinarySerializers.LZ4Fory`와
`LZ4Kryo`를 즉시 사용할 수 있는 serializer 선택지로 문서화했지만, 해당 선택지를
뒷받침하는 codec/compressor library가 module에서는 `compileOnly`였음을 드러냈다.

## 결정

- Fory, Kryo, LZ4, Zstd, Snappy를 `bluetape4k-spring-boot-redis`의 runtime dependency로
  publish한다.
- 기본 module test는 유지하되, module의 `compileOnly` test classpath를 상속하지 않는
  별도 `consumerRuntimeTest` source set을 추가한다.
- README locale set에 표시된 serializer matrix를 사용할 때 consumer가 별도
  codec/compressor dependency를 추가하지 않아도 된다고 문서화한다.

## 교훈

- `compileOnly`를 확장한 module test classpath는 consumer runtime gap을 숨길 수 있다.
  published runtime contract에는 `testImplementation`이 아니라 `runtimeClasspath` 기반
  source set을 추가한다.
- named serializer constant를 사용하는 README example은 runtime contract다. 필요한
  runtime dependency를 publish하거나 optional dependency를 명시적으로 문서화해야 한다.
- 이미 열린 PR과 같은 module file을 건드리는 follow-up issue에는 stacked PR이 적합하다.

## 검증

- RED: `./gradlew :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache`는
  `org.apache.fory.ThreadSafeFory`와 `net.jpountz.lz4.LZ4Factory` 누락으로 실패했다.
- GREEN: 문서화된 codec/compressor library를 `runtimeOnly`로 옮긴 뒤 같은 task가
  consumer-runtime test 2개와 함께 통과했다.
- `./gradlew :bluetape4k-spring-boot-redis:dependencies --configuration runtimeClasspath`에서
  Fory, Kryo, LZ4, Zstd, Snappy를 확인했다.
- `./gradlew :bluetape4k-spring-boot-redis:compileKotlin :bluetape4k-spring-boot-redis:compileTestKotlin :bluetape4k-spring-boot-redis:test :bluetape4k-spring-boot-redis:consumerRuntimeTest --no-build-cache`가 통과했다.
- test XML totals: default Redis tests 83 tests, consumer runtime tests 2 tests,
  failures/errors 0.
- `git diff --check`가 통과했다.
