# Cache 모듈 일관성 리팩토링 설계

**날짜**: 2026-03-18
**상태**: 승인됨

## 목표

Cache 모듈(cache-core, cache-lettuce, cache-hazelcast, cache-redisson) 간의 일관성을 개선한다.

- 직렬화: 팩토리 API는 `LettuceBinaryCodec`으로 통일, 내부는 `BinarySerializer` 유지 (Lettuce 계열)
- 팩토리 네이밍 통일: `nearJCache()` / `suspendNearJCache()` (JCache 기반), `nearCache()` /
  `suspendNearCache()` (NearCacheOperations 기반)
- 파라미터 축소 및 DSL 도입
- 미사용 파라미터 실제 활용

## 핵심 설계 결정

### LettuceBinaryCodec ↔ BinarySerializer 브릿지 전략

`LettuceBinaryCodec.serializer`가 `private`이므로 `public`으로 변경하여 외부에서도 접근 가능하게 한다.

- **팩토리 API** (`LettuceCaches`, `LettuceSuspendCacheManager`): `LettuceBinaryCodec<V>` 파라미터를 받음
- **내부 구현** (`LettuceJCache`, `LettuceCacheConfig`): `codec.serializer`로 `BinarySerializer`를 추출하여 사용
- `LettuceJCache`는 `LettuceMap<ByteArray>` 기반이므로 `ByteArray` 직렬화 경로를 유지

변경 대상: `LettuceBinaryCodec.kt`의 `private val serializer` → `val serializer` (public)

## 단계별 변경 사항

### 단계 1: NearJCacheConfig Builder DSL (cache-core)

**대상 파일:**

- `infra/cache-core/.../nearcache/jcache/NearJCacheConfigBuilder.kt` (신규)
- `infra/cache-core/.../nearcache/jcache/NearJCacheConfig.kt` (톱레벨 DSL 함수 추가)

**변경 내용:**

- `NearJCacheConfigBuilder<K, V>` 클래스 추가
    - `var cacheManagerFactory: Factory<CacheManager> = NearJCacheConfig.CaffeineCacheManagerFactory`
    - `var cacheName: String = "near-jcache-" + Base58.randomString(8)`
    - `var frontCacheConfiguration: MutableConfiguration<K, V> = getDefaultFrontCacheConfiguration()`
    - `var isSynchronous: Boolean = false`
    - `var syncRemoteTimeout: Long = NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT`
    - `fun build(): NearJCacheConfig<K, V>`
- 톱레벨 DSL 함수: `nearJCacheConfig<K, V> { ... }` 추가

### 단계 2: HazelcastCaches 파라미터 축소 (cache-hazelcast)

**대상 파일:**

- `infra/cache-hazelcast/.../HazelcastCaches.kt`
- 관련 테스트 파일 전체

**변경 내용:**

- `nearJCache(frontCache, hazelcastInstance, configuration, nearCacheCfg)` (4파라미터)
  → `nearJCache(hazelcastInstance, block: NearJCacheConfigBuilder.() -> Unit)` (2파라미터, DSL)
  → `nearJCache(hazelcastInstance, config: NearJCacheConfig)` (2파라미터, 객체)
- `suspendNearJCache()` 동일 패턴 적용
- **front cache 자동 생성**: `NearJCache.invoke(nearCacheCfg, backCache)` companion 함수 활용
    - `NearJCacheConfig.cacheManagerFactory`로 front CacheManager 생성
    - `NearJCacheConfig.frontCacheConfiguration`으로 front cache 설정
    - 리스너 등록도 companion 내부에서 처리
- 기존 4파라미터 함수 제거 (내부 라이브러리, 외부 사용자 없음 확인됨)
- **테스트 코드 수정**: 새 시그니처에 맞게 변경

### 단계 3: LettuceBinaryCodec 접근성 변경 + LettuceJCache 통일 (cache-lettuce)

**대상 파일:**

- `infra/lettuce/.../codec/LettuceBinaryCodec.kt` (`private` → `internal`)
- `infra/cache-lettuce/.../jcache/LettuceJCache.kt`
- `infra/cache-lettuce/.../jcache/LettuceCacheConfig.kt`
- `infra/cache-lettuce/.../jcache/LettuceJCaching.kt`
- 관련 테스트 파일 전체

**변경 내용:**

- `LettuceBinaryCodec.serializer`: `private` → `public` (`val serializer`)
- `LettuceJCache` 생성자: `serializer: BinarySerializer` → `codec: LettuceBinaryCodec<V>`
    - 내부에서 `codec.serializer`를 통해 `BinarySerializer` 접근 (ByteArray 직렬화 경로 유지)
    - 기본값: `LettuceBinaryCodecs.lz4Fory()`
- `LettuceCacheConfig`: `serializer: BinarySerializer` → `codec: LettuceBinaryCodec<*>` 변경 (또는 추가)
- `LettuceJCaching.getOrCreate()`: codec 파라미터 전달 경로 수정
- **테스트 코드 수정**

### 단계 4: LettuceSuspendCacheManager/LettuceCacheManager 파라미터 활용 (cache-lettuce)

**대상 파일:**

- `infra/cache-lettuce/.../jcache/LettuceSuspendCacheManager.kt`
- `infra/cache-lettuce/.../jcache/LettuceCacheManager.kt`
- 관련 테스트 파일 전체

**변경 내용:**

- 생성자 `codec` 파라미터: `LettuceBinaryCodec<Any>?` → `LettuceBinaryCodec<Any> = LettuceBinaryCodecs.lz4Fory()`
- `getOrCreate()` 메서드에서 `ttlSeconds`, `codec`를 `LettuceCacheConfig`에 설정하여 내부 JCache 생성에 전달
- 개별 캐시별 override 가능 (파라미터 → 생성자 기본값 fallback)
- **테스트 코드 수정**

### 단계 5: LettuceCaches 팩토리 함수 추가 (cache-lettuce)

**대상 파일:**

- `infra/cache-lettuce/.../LettuceCaches.kt`
- 관련 테스트 파일

**변경 내용:**

- 기존 `jcache()`: `serializer: BinarySerializer` → `codec: LettuceBinaryCodec<V> = LettuceBinaryCodecs.lz4Fory()`
- 신규 `suspendJCache()`: `LettuceSuspendJCache<V>` 생성
- 신규 `nearJCache(redisClient, block)` / `nearJCache(redisClient, config)`: `NearJCache<K, V>` 생성
- 신규 `suspendNearJCache(redisClient, block)` / `suspendNearJCache(redisClient, config)`: `SuspendNearJCache<K, V>` 생성
- 모든 기본 codec: `LettuceBinaryCodecs.lz4Fory()`
- **테스트 코드 추가**

### 단계 6: RedissonCaches 네이밍 통일 (cache-redisson)

**대상 파일:**

- `infra/cache-redisson/.../RedissonCaches.kt`
- 관련 테스트 파일 전체

**변경 내용:**

- `nearCacheOps()` → `nearCache()` 이름 변경
- `suspendNearCacheOps()` → `suspendNearCache()` 이름 변경
- JCache 기반은 이미 네이밍 테이블에 따라 `nearJCache()`로 구분되므로 오버로드 충돌 없음
    - `nearJCache(...)` → `NearJCache<K, V>` 반환 (JCache 기반)
    - `nearCache(...)` → `NearCacheOperations<V>` 반환 (RLocalCachedMap 기반)
- 기존 JCache 기반 `nearCache()` 함수명도 `nearJCache()`로 변경하여 네이밍 테이블과 일치시킴
- **테스트 코드 수정**

## 네이밍 규칙 (3개 모듈 통일)

| 유형                            | 함수명                   | 반환 타입                           |
|-------------------------------|-----------------------|---------------------------------|
| JCache                        | `jcache()`            | `JCache<K, V>`                  |
| Suspend JCache                | `suspendJCache()`     | `SuspendJCache<K, V>`           |
| JCache 기반 NearCache (동기)      | `nearJCache()`        | `NearJCache<K, V>`              |
| JCache 기반 NearCache (suspend) | `suspendNearJCache()` | `SuspendNearJCache<K, V>`       |
| NearCacheOperations (동기)      | `nearCache()`         | `NearCacheOperations<V>`        |
| NearCacheOperations (suspend) | `suspendNearCache()`  | `SuspendNearCacheOperations<V>` |

## 기본값 규칙

| 항목               | 기본값                             |
|------------------|---------------------------------|
| Lettuce Codec    | `LettuceBinaryCodecs.lz4Fory()` |
| Redisson Codec   | `RedissonCodecs.LZ4Fory`        |
| NearJCacheConfig | Caffeine front cache, 30분 접근 만료 |

## 범위 외

- `NearCacheOperations` / `SuspendNearCacheOperations` 인터페이스 자체 변경 없음
- Hazelcast/Redisson의 NearCacheOperations 기반 팩토리 (`nearCache()`/`suspendNearCache()`) 시그니처는 유지
- Lettuce NearCacheConfig (`LettuceNearCacheConfig`) 관련 변경 없음
