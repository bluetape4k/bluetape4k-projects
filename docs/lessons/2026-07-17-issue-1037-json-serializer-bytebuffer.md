# 이슈 #1037: JSON serializer ByteBuffer path

## 배경

Shared `JsonSerializer` 계약은 이미 ByteBuffer default를 노출했지만, compatibility
implementation은 complete ByteArray를 통해 data를 staging했다. Jackson 2, Jackson 3,
Fastjson2는 실질적으로 다른 stream 및 array-range API를 노출하므로 JSON slice에는 wire
format, mapper/security configuration, exception policy, caller-owned buffer state를
바꾸지 않는 backend-specific path가 필요했다.

## 결정

- Jackson 2와 Jackson 3 output은 fixed duplicate-backed `ByteBufferOutputStream`으로 stream하고, input은 duplicate-backed `ByteBufferInputStream`으로 읽는다.
- Configured `ObjectWriter`를 통해 Jackson generator를 명시적으로 만들고 닫는다. Mapper convenience method는 모든 fatal `Error`가 아니라 `Exception`을 잡으므로 caller-owned stream cleanup을 그 method에 의존할 수 없다.
- Generic Jackson ByteBuffer API는 top-level concrete-receiver extension으로 유지한다. Public open serializer class에 final member를 추가하면 같은 JVM signature를 이미 가진 기존 subclass와 충돌할 수 있다.
- Fastjson2의 array/offset/length JSONB parser는 writable array-backed input에만 사용한다. Direct/read-only input은 copy하고, `JSONB.toBytes`는 명시적으로 할당하는 output compatibility path로 유지한다.
- Nested fatal error나 buffer overflow를 보존할 때 cause link는 최대 64개까지만 순회한다. Suppressed cleanup failure는 primary backend failure를 대체하지 않는다.
- 모든 JSONB reader는 feature-free로 유지하고 AutoType을 활성화하지 않는다.

## 발견 / 실패

첫 Jackson reified API는 open class의 member였다. Local caller에서는 동작했지만 같은
erased signature를 가진 legacy subclass를 깨뜨릴 수 있는 final JVM method를 도입했다.
Extension으로 옮기자 class ABI는 고쳐졌지만, README와 test도 explicit import와 alias를
통해 그 extension을 기존 `JsonSerializer.deserialize` extension과 구분해야 했다.

Jackson의 mapper-owned write path는 ordinary exception에서 generator를 닫지만, resolved
Jackson 2/3 source는 `Error`가 아니라 `Exception`을 잡는다. Fatal serialization
failure에서 cleanup을 보장하려면 명시적인 generator scope가 필요했다.

Fastjson2는 setter가 던진 `Error`를 `ClassCastException`으로 바꿀 수 있어 해당 fixture로
adapter behavior를 증명할 수 없었다. Fatal error를 cause로 가진 ordinary wrapper를
던지는 registered JSONB `ObjectReader`가 유효한 identity test를 제공했다. 같은 조사에서
suppressed failure를 scan하면 cleanup error가 primary parse failure보다 잘못 승격될 수
있다는 점도 드러났다.

## 결과

Jackson 2와 Jackson 3는 mapper configuration과 inherited data format을 유지하면서
fixed output과 bounded input에서 ByteArray compatibility path를 우회한다. Fastjson2는
public API가 existing array range를 지원하는 경우에만 input copy를 피한다. 지원되지
않는 input과 모든 output은 문서화된 compatibility path에 남는다. 기존 ByteArray entry
point, JSON/JSONB wire bytes, raw interface dispatch, caller position/limit/mark/order
contract는 그대로 유지된다.

## 검증

- Jackson 2 ByteBuffer suite: 14 tests passed; external consumer import test:
  1 test passed; full module: 455 tests passed.
- Jackson 3 ByteBuffer suite: 16 tests passed; external consumer import test:
  1 test passed; full module: 456 tests passed.
- Fastjson2 ByteBuffer suite: 14 tests passed; full module: 180 tests passed.
- Legacy same-signature Jackson subclasses compile, while the reified extension
  still retains generic collection element types.
- Fastjson2 resolved-source evidence records version `2.0.62`, source locations,
  source JAR SHA-256, and the optimized/fallback matrix.
- Root `detekt` is `NO-SOURCE`; module compilation, complete module tests,
  README contract checks, unsafe-pattern scanning, and `git diff --check` provide
  the available static fallback.

## 향후 방지책

Erased subclass signature를 확인하지 않고 public open class에 convenience member를
추가하지 않는다. Backend가 stream wrapper를 소유하면 outer stream의 `use`만으로 충분할
것이라고 가정하지 말고 resolved source에서 fatal-error cleanup을 확인한다. Fatal
failure는 primary cause chain에서 reachable한 것만 보존하고, suppressed cleanup
failure를 통해 보존하지 않는다. Optimization claim은 backend cell별로 제한하고,
반복 benchmark evidence가 생길 때까지 allocation claim은 미룬다.
