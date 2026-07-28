# 교훈 - 이슈 #746 Same-condition compressor benchmark

## 배경

issue #746에는 `bluetape-go`, `bluetape-rs`와 비교할 수 있는 durable
`bluetape4k-io` compressor-only matrix가 필요했다.

## 교훈

- normalized table은 target ecosystem 전반에 존재하는 compressor family로 제한한다.
  BZip2는 유용한 JVM context지만 common table에 섞지 않는다.
- `kotlinx-benchmark`에서는 command를 문서화하기 전에 generated task name을 검증한다.
  이 module은 `testBenchmark`, `testBenchmarkCompile`, `testBenchmarkJar`를 노출한다.
- 이 setup에서 `testBenchmark --args`는 JMH include-filter escape hatch가 아니다. 첫
  argument를 kotlinx runner input file로 처리한다. focused smoke run에는 generated JMH
  jar 실행을 예외로 문서화하면 사용할 수 있다.
- benchmark fat jar는 signed dependency metadata를 포함할 수 있다. direct JMH execution이
  signature verification에 실패하지 않도록 benchmark jar task에서 `META-INF/*.RSA`,
  `META-INF/*.DSA`, `META-INF/*.SF`를 제외한다.

## 가드

benchmark harness를 추가할 때는 Gradle-generated benchmark compile path와 적어도 하나의
runnable benchmark command를 모두 증명한다. runnable path가 direct JMH jar execution을
필요로 한다면 Gradle task가 충분하지 않은 이유를 기록하고 direct run을 좁게 유지한다.
