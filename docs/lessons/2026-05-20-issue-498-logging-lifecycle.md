# 이슈 #498 KLoggingChannel Lifecycle 교훈

## 배경

`KLoggingChannel`은 test와 reloadable application을 위한 explicit lifecycle ownership이 필요했다.
기존 test suite는 no-throw logging call만 검증했고 delivery 또는 collector shutdown을 증명하지 않았다.

## 결정

`KLoggingChannel`은 이제 `AutoCloseable`을 구현하고, shared default runtime scope/shutdown hook 하나를
사용하며, suspend cleanup boundary를 위해 `closeAndJoin()`을 expose한다. Custom scope는 caller-owned로
유지한다.

## 결과

Test는 이제 Logback event를 capture하고, level/message/error delivery를 assert하며, collector
cancellation과 post-close event dropping을 검증한다. Collector는 `MutableSharedFlow` subscription이
establish되기 전에 첫 event를 drop하지 않도록 `UNDISPATCHED`로 시작한다.

## 검증

- 변경된 Kotlin file에 IDE imports optimized.
- IDE diagnostics: index ready, build error 없음. File이 IDE에서 열려 있지 않아 per-file fresh analysis는 unavailable.
- `./gradlew :bluetape4k-logging:compileKotlin :bluetape4k-logging:compileTestKotlin --no-configuration-cache`
- `./gradlew :bluetape4k-logging:test --tests 'io.bluetape4k.logging.coroutines.KLoggingChannelTest' --no-configuration-cache`
- `./gradlew :bluetape4k-logging:test --no-configuration-cache`
- `git diff --check`

## 향후 가드

Async logging test에서는 real appender를 붙이고 emitted event를 assert한다. `Thread.sleep` drain test를
피하고, collector job 또는 observable output으로 lifecycle state를 증명한다.
