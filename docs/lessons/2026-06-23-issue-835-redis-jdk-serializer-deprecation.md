# Redis JDK serializer deprecation 교훈 (#835, 2026-06-23)

관련 이슈: #835
영향 module: `:bluetape4k-spring-boot-redis`

## L1: wrapper constant는 하위 보안 신호를 보존해야 한다

`BinarySerializers.Jdk`에는 이미 JDK deserialization RCE risk에 대한 deprecation
warning이 있었지만, Redis-facing convenience constant는 그 warning을 반복하지 않았다.
public module boundary는 감싸는 lower-level API의 security guidance를 숨기면 안 된다.
특히 Redis 같은 persistence/network boundary에서는 더 그렇다.

Redis constant에는 이제 명시적인 deprecation annotation과 replacement guidance가
붙었고, reflection contract test는 future JDK Redis constant가 warning 없이 다시
도입되지 못하게 막는다.

## L2: README serializer table에는 capability뿐 아니라 status가 필요하다

serializer matrix가 JDK, Kryo, Fory를 동등하게 나열하면 implementation이 기술적으로
맞더라도 unsafe default를 정상 선택지처럼 보이게 만들 수 있다. deserialization risk가
contract 일부라면 user-facing table은 recommended serializer와 trusted-data-only legacy
choice를 구분해야 한다.
