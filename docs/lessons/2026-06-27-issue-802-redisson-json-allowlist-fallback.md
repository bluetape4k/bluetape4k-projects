# 이슈 802: Redisson JSON allowlist fallback 경계

## 배경

`Jackson3Codec`과 `Fastjson2Codec`은 `allowedPackagePrefixes`를 Redis trust-boundary
control로 설명했지만, malformed 또는 non-JSON payload는 여전히 Fory fallback
decoder로 떨어질 수 있었다. 이 때문에 문서화된 allowlist가 public API 계약보다
약해졌다.

## 결정

`allowedPackagePrefixes`가 설정되어 있으면 JSON codec decode fallback을 기본적으로
비활성화한다. `allowedPackagePrefixes = null`인 기존 trusted-internal 동작은 그대로
유지하고, 신뢰된 migration window가 필요한 호출자는 `allowFallbackDecode = true`를
명시적으로 설정해야 한다.

## 결과

- Allow-listed `Jackson3Codec`과 `Fastjson2Codec`은 이제 fallback-format binary payload를 `SecurityException`으로 거부한다.
- `RedissonCodecs.jackson3(...)`와 `RedissonCodecs.fastjson2(...)`는 안전한 기본값을 상속한다.
- README trust-profile 지침은 기본 거부 동작과 명시적 migration escape hatch를 문서화한다.

## 검증

- 구현 전 red test: allow-listed Jackson3/Fastjson2 binary fallback payload test는 exception이 던져지지 않아 실패했다.
- `./gradlew :bluetape4k-redisson:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-redisson:test --tests "io.bluetape4k.redis.redisson.codec.*CodecTest" --no-build-cache --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-redisson:test --no-daemon --no-configuration-cache`
- `git diff --check`

## 향후 방지책

모든 decode fallback path가 같은 type boundary를 검증하거나 기본 비활성화되어 있지
않다면 codec factory를 `AllowListedTypes`로 설명하지 않는다. Legacy migration path가
필요하면 API와 README trust-profile text에서 명시적으로 드러낸다.

## 동시성 helper gate

`MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`는 여기서 적용
대상이 아니었다. 이 변경은 shared mutable state, coroutine lifecycle behavior,
structured task scope behavior를 추가하지 않고 Redis codec trust boundary의
synchronous decode fallback behavior만 좁힌다.
