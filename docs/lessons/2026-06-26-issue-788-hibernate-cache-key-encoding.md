# Hibernate cache key encoding 교훈 (2026-06-26)

관련 이슈: #788
영향 module: `:bluetape4k-hibernate-cache-lettuce`

## L1: cache key collision claim에는 public-path collision test가 필요하다

### 문제

`LettuceNearCacheStorageAccess`는 delimiter join과 `toString()`으로 Hibernate key를
normalize했다. 이 방식은 `toString()` 값이 같을 때 natural-id arity, array/scalar boundary,
custom identifier state를 지웠다. README는 Redis key collision prevention을 주장했지만,
test는 readable key fragment가 존재하는지만 확인했다.

### 교훈

cache bridge가 key collision resistance를 주장한다면 public storage path를 adversarial key로
cover한다.

- delimiter가 들어 있는 단일 natural-id value와 composite natural-id array
- scalar identifier text와 object-array identifier text
- `toString()` output은 같지만 serialized state가 다른 custom identifier

### 향후 가드

모든 component가 length-prefixed이거나 type 정보를 보존하지 않는 한 readable delimiter
string을 distributed cache key로 쓰지 않는다. raw key가 user value나 Hibernate-disassembled
object를 포함할 수 있으면 versioned canonical digest key를 우선한다.
