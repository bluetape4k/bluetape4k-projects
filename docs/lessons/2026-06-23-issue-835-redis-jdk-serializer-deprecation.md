# Lessons Learned - Redis JDK serializer deprecation (#835, 2026-06-23)

Related issue: #835
Affected module: `:bluetape4k-spring-boot-redis`

## L1: Wrapper constants must preserve lower-level security signals

`BinarySerializers.Jdk` already carried a deprecation warning for JDK
deserialization RCE risk, but the Redis-facing convenience constants did not
repeat that warning. Public module boundaries should not hide security guidance
from the lower-level API they wrap, especially at persistence or network
boundaries such as Redis.

The Redis constants now carry explicit deprecation annotations and replacement
guidance, and the reflection contract test checks that future JDK Redis
constants cannot be reintroduced without the warning.

## L2: README serializer tables need status, not only capability

A serializer matrix that lists JDK, Kryo, and Fory as peers can normalize unsafe
defaults even when the implementation is technically correct. User-facing tables
should distinguish recommended serializers from trusted-data-only legacy choices
when deserialization risk is part of the contract.
