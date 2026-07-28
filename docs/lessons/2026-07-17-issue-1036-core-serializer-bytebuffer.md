# 이슈 #1036: Core serializer ByteBuffer path

## 배경

`BinarySerializer`는 이미 호환 가능한 ByteBuffer default를 노출했지만, 그 default는
모든 operation을 새로 할당한 ByteArray를 통해 staging했다. JDK, Kryo, Fory는 서로
다른 buffer capability와 lifecycle constraint를 가지므로 하나의 shared optimization
strategy로는 기존 계약을 모두 보존할 수 없었다.

## 결정

- JDK input/output에는 fixed ByteBuffer-backed stream을 사용하고, ByteArray path와 같은 configured 또는 global `ObjectInputFilter`를 적용한다.
- Kryo `ByteBufferInput`과 `ByteBufferOutput`은 caller의 bounded slice에 bind하되, adapter를 global pool에 반환하기 전에 caller storage를 detach하는 scoped provider method를 통해서만 노출한다. 이 path는 serializer가 소유한 default, secure, fast pool에만 적용한다.
- Externally supplied Kryo pool은 custom serializer가 array-backed `Input`과 `Output` access에 의존할 수 있으므로 ByteArray compatibility path에 남긴다.
- Fory는 native ByteBuffer input overload를 사용하고, backend가 output storage를 grow하거나 detach할 수 있으므로 compatibility ByteArray output path를 유지한다.
- 외부에서 확장 가능한 `AbstractBinarySerializer`의 protected surface를 늘리지 않고 새 failure/cleanup helper는 internal로 유지한다.
- Raw overflow, fatal error, configured registration, wire bytes, caller position/limit/order behavior를 보존한다.

## 발견 / 실패

Open base class에 protected convenience method를 추가하는 일은 source-local처럼
보였지만 외부 subclass와 JVM signature conflict를 만들 수 있었다. Direct pooled
adapter obtain/release method도 미래 호출자가 caller buffer를 detach하지 않고 adapter를
반환할 수 있게 했다. 마지막으로 failed graph를 log하면 arbitrary `toString()` 구현을
호출해 payload content를 노출할 수 있었다. Kryo의 ByteBuffer adapter는 `getBuffer()`를
throw하도록 override하므로 external pool에 적용하면 그 외에는 유효한 custom serializer가
깨질 수 있었다.

## 결과

JDK와 Kryo는 owned configuration이 fixed caller-owned buffer를 안전하게 지원하는
곳에서 compatibility ByteArray staging path를 우회한다. External Kryo pool은 기존
array-backed behavior를 유지한다. Fory는 지원하지 않는 output behavior를 주장하지 않고
input copy만 피한다. 기존 ByteArray entry point와 serializer configuration은 변경되지
않는다.

## 검증

- Core serializer ByteBuffer suite: 18 tests passed.
- Full `:bluetape4k-io:test` suite: 1055 tests passed.
- Wire parity, exact capacity, overflow, cursor rollback, source-state
  preservation, security filters, registration, pooling, retry, and mixed
  concurrency paths are covered.
- ABI verification is run from the clean committed head before PR publication.

## 향후 방지책

ABI analysis 없이 public extensible base class에 convenience member를 추가하지 않는다.
Pooled adapter는 scoped resource로 다루고 재사용 전에 caller-owned storage를 detach한다.
Externally supplied serializer implementation이 해당 capability를 명시적으로 선언하지
않는 한 backend-specific adapter를 넘기지 않는다. Resolved dependency version 기준으로
backend behavior를 검증하고, allocation claim은 #1039가 반복 측정을 기록할 때까지
미룬다.
