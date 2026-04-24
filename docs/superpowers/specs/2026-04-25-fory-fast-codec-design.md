# FastFory Codec 추가 — 설계 스펙

- **Issue**: #113
- **Branch/worktree**: `.worktrees/fory-fast-codec`
- **Date**: 2026-04-25
- **Approach**: A (각 모듈에 개별 클래스/심볼 추가, 추상화 레이어 없음) — 사용자 확정
- **Status**: Draft (user review pending)

## 1. 목표 및 범위

### 1.1 목표
이미 구현된 `ForyBinarySerializer.fast()`(SCHEMA_CONSISTENT + refTracking=false, ~+70% throughput)를 다음 3개 상위 모듈의 공개 API에 배선(wire)하여, 사용자가 `BinarySerializers.FastFory`, `RedissonCodecs.FastFory`, `LettuceBinaryCodecs.fastFory()` 형태로 즉시 사용할 수 있도록 한다.

> **명명 규칙 결정**: `{압축코덱}FastFory` 형식 사용 (예: `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`). 기존 `{압축코덱}Fory` 패턴과 일관성 유지하며 Fast 수식어를 Fory 앞에 위치.

### 1.2 Issue #113 완료 기준 매핑
| Issue 요구사항 | 본 스펙 대응 |
|---|---|
| io/io `BinarySerializers` FastFory 계열 val 추가 | §5.1 — `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GZipFastFory` 5개 lazy val |
| infra/redisson `FastForyCodec` 신규 클래스 | §5.2 — `FastForyCodec`, `ForyCodec` 바이트 단위 복제, **fallback=ForyCodec**, 2 생성자 |
| infra/redisson `RedissonCodecs` FastFory val 추가 | §5.2 — `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GzipFastFory` + 각 `*Composite` (10개) |
| infra/lettuce `LettuceBinaryCodecs` fastFory 팩토리 | §5.3 — `fastFory()`, `lz4FastFory()`, `zstdFastFory()`, `snappyFastFory()`, `gzipFastFory()` (5개) |
| 성능 향상 검증 | §8 — JMH 벤치마크 확장, 수치 목표 없이 측정·기록 |

### 1.3 비범위 (Out of Scope)
- `ForyBinarySerializer.fast()` 구현 변경 — **이미 구현 완료**, 본 작업은 배선만.
- Cache 모듈(cache-redisson, cache-lettuce) 공개 API 변경 — JMH 벤치마크 없음, 기존 codec 경유.
- 기본 Fory codec 대체/Deprecation — 기본값 유지, FastFory는 opt-in.
- 스키마 진화 지원 — SCHEMA_CONSISTENT 특성상 원천 불가.

## 2. 위험 및 실패 모드

### R1. 와이어 포맷 비호환으로 인한 운영 데이터 유실 (CRITICAL)
**현상**: 기존 `Fory`(COMPATIBLE) 포맷으로 기록된 Redis/캐시 데이터를 `FastFory`(SCHEMA_CONSISTENT)로 읽으면 deserialize 오류 또는 잘못된 객체 복원.
**근거**: `ForyBinarySerializer.kt` L80-83 KDoc — "SCHEMA_CONSISTENT 포맷은 기본 COMPATIBLE 포맷과 호환되지 않아".
**완화책**:
- 모든 새 심볼(`BinarySerializers.FastFory`, `RedissonCodecs.FastFory`, `LettuceBinaryCodecs.fastFory()`)의 KDoc에 §6의 경고 문구 한국어로 명시.
- 로컬라이즈된 통합 교차 검증 테스트(§7.2)로 비호환을 실제 바이트 비교로 고정.
- **비대칭 계약**: `FastForyCodec`은 구 Fory(COMPATIBLE) 바이트를 fallback으로 읽을 수 있음. 반대(`ForyCodec`이 FastFory 바이트 읽기)는 불가 — KDoc에 명시. §R2 참고.

### R2. Redisson FastForyCodec fallback 체인 — 비대칭 계약 (HIGH)
**비대칭 계약 (Codex 리뷰 확정)**:
- `FastForyCodec`이 **Fory 바이트를 읽을 때**: FastFory decode 실패 → Fory fallback 성공 (COMPATIBLE 바이트를 Fory가 읽을 수 있음) ✅
- `ForyCodec`이 **FastFory 바이트를 읽을 때**: Fory decode 실패 → Kryo5 fallback도 실패 → 오류 ❌
- **결론**: `FastForyCodec`은 구 Fory 데이터를 읽을 수 있다. 반대 방향은 불가. 이 비대칭 계약을 KDoc과 테스트에 명시.
- encode-side fallback: FastFory encode 실패(직렬화 불가 타입) 시에도 Fory로 재인코드. 단, 해당 바이트는 Fory 형식이므로 이후 ForyCodec(decoder)만 읽을 수 있음.

**근거**: `ForyCodec.kt` L52-74 encoder/decoder try/catch → fallback 구조. 사용자 결정: 체인 FastFory → Fory → Kryo5.
**완화책**:
- `FastForyCodec` fallback = `RedissonCodecs.Fory` (ForyCodec 인스턴스). ForyCodec은 이미 fallback=Kryo5를 가지므로 체인 자동 구성.
- `RedissonCodecs.kt`에서 `FastFory`는 반드시 `Fory` 선언 **이후**에 위치 (lazy 초기화 순환 방지).
- §7.2 방향 A 테스트로 비대칭 계약 명시적 검증.

### R3. 벤치마크가 캐시 실사용 시나리오 미반영 위험 (HIGH)
**현상**: JMH 벤치마크에 작은 String/단일 Int만 넣으면 FastFory의 실질 이득(1KB+ DTO graph)이 드러나지 않거나 반대로 과대 평가.
**근거**: Step 1-R — 기존 `RedissonCodecBenchmark.kt`, `LettuceCodecBenchmark.kt`의 값 분포 재사용 필요.
**완화책**:
- 기존 벤치마크 파일의 테스트 값 분포(예: DTO fixture)를 **그대로 재사용**하고, codec 필드만 FastFory 계열로 교체.
- 기존 Fory 벤치마크 메서드와 나란히 @Benchmark 추가 → 직접 비교 가능.

### R4. `fast()` pool thread-safety 가정 검증 (MEDIUM)
**현상**: `FastFory`는 `buildThreadSafeForyPool(min=2, max=2*cores, 30min)`로 pool된 ThreadSafeFory이나, 고동시성 부하에서 pool 고갈 시 블로킹/타임아웃 동작이 ForyCodec(Default pool)과 동일한지 미검증.
**근거**: `ForyBinarySerializer.kt` L51-67 — FastFory pool 파라미터가 DefaultFory(L29-45)와 동일 구조임을 확인.
**완화책**: pool 파라미터는 DefaultFory와 동일 — 별도 검증 불요. 스펙에 "DefaultFory와 동일 pool 설정 사용" 기재로 가정 명시화.

### R5. SnappyFastFory 범위 일관성 (LOW)
**현상**: io/io와 lettuce는 Snappy 조합을 제공하나, Issue #113 원문에서 Redisson 쪽 Snappy 언급이 명확하지 않음. 모듈 간 대칭성 저하.
**근거**: `RedissonCodecs.kt` L167-180 — 기존에 `SnappyFory` + `SnappyForyComposite`가 이미 존재.
**결정**: **Redisson에도 `SnappyFastFory` + `SnappyFastForyComposite`를 추가**하여 기존 Fory 계열과 1:1 대칭 유지. (기존 대칭을 깨면 미래 유지보수 비용 증가)

## 3. 아키텍처 결정 (접근법 A 내부)

### D1. FastForyCodec: ForyCodec 상속 vs 완전 복제 → **완전 복제**
- **복제 선택 근거**:
  - Step 1-R: 사용자 지침 "바이트 단위 복제".
  - `ForyCodec.fory` (L50) 필드가 `private val` — 상속해도 교체 불가, override 포인트 없음.
  - BaseCodec의 `getValueEncoder`/`getValueDecoder`는 open이지만, encoder/decoder가 람다로 캡처된 `private val fory`를 참조 → 상속 시 생성자에서 fory를 재주입하는 path가 깔끔하지 않음.
  - 복제 → 두 codec 각자 독립 진화 가능, 실패 격리, 로그 메시지 차별화(`FastForyCodec` 명시) 가능.
- **복제 비용**: ~30줄 중복. DRY 위반이지만 codec 계층에서 추상화 도입은 A 범위 외.

### D2. 비호환 검증: 각 codec 테스트 vs 통합 교차 검증 파일 → **통합 교차 검증 파일 신설**
- **통합 파일 근거**: R1은 "FastFory로 쓰고 Fory로 읽기" 또는 그 역을 명시적으로 실패시켜야 하는 cross-codec 계약. 각 codec 단위 테스트에 분산시키면 한쪽만 유지보수되다 sync 깨질 위험.
- **위치**:
  - `io/io/src/test/kotlin/io/bluetape4k/io/serializer/FastForyCompatibilityTest.kt`
  - `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCompatibilityTest.kt` (fallback=ForyCodec 체인 — 방향 A에서 Fory fallback 성공 assert, 방향 B에서 Fory+Kryo5 모두 실패 assert)
  - `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt`
- 각 파일은 roundtrip PASS + cross-codec FAIL(or fallback) 2쌍 테스트만 유지.

### D3. 벤치마크: 기존 파일 확장 vs 신규 파일 분리 → **기존 파일 확장**
- **확장 근거**: R3의 값 분포 재사용이 유일한 공정 비교 조건. 파일 분리하면 fixture 중복 또는 drift 발생. @Benchmark 메서드 **4개** 추가(FastFory pure, LZ4FastFory, ZstdFastFory, GzipFastFory)로 기존 Fory와 즉시 비교.

## 4. 변경 파일 목록

### 4.1 신규 파일
1. `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt`
2. 교차 검증 테스트 3개:
   - `io/io/src/test/kotlin/io/bluetape4k/io/serializer/FastForyCompatibilityTest.kt`
   - `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCompatibilityTest.kt`
   - `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/FastForyCompatibilityTest.kt`

### 4.2 수정 파일 (프로덕션 3개)
1. `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializers.kt` — §5.1 심볼 추가
2. `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecs.kt` — §5.2 심볼 추가
3. `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt` — §5.3 팩토리 추가

### 4.2-T 수정 파일 (테스트 Registry 2개 — Codex 리뷰 추가)
1. `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecsTest.kt` — `getRedissonBinaryCodecs()` 파라미터에 FastFory **10종** 추가 (plain 5 + composite 5)
2. `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecTest.kt` — `getRedisCodecs()` 파라미터에 fastFory **5종** 추가

### 4.3 수정 파일 (벤치마크 2개)
1. `infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonCodecBenchmark.kt`
2. `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`

### 4.4 수정 파일 (README 6개)
- `io/io/README.md`, `io/io/README.ko.md`
- `infra/redisson/README.md`, `infra/redisson/README.ko.md`
- `infra/lettuce/README.md`, `infra/lettuce/README.ko.md`

## 5. API 설계

### 5.1 io/io `BinarySerializers`
```kotlin
/** SCHEMA_CONSISTENT + refTracking=false 최적화 Fory BinarySerializer.
 *  [Fory] 대비 ~+70% throughput. ⚠️ Fory와 와이어 포맷 비호환 — 휘발성 캐시 전용. */
val FastFory: ForyBinarySerializer by lazy { ForyBinarySerializer.fast() }

val LZ4FastFory: CompressableBinarySerializer by lazy {
    CompressableBinarySerializer(FastFory, Compressors.LZ4)
}
val ZstdFastFory: CompressableBinarySerializer by lazy {
    CompressableBinarySerializer(FastFory, Compressors.Zstd)
}
val SnappyFastFory: CompressableBinarySerializer by lazy {
    CompressableBinarySerializer(FastFory, Compressors.Snappy)
}
val GZipFastFory: CompressableBinarySerializer by lazy {
    CompressableBinarySerializer(FastFory, Compressors.GZip)
}
```

### 5.2 infra/redisson
**`FastForyCodec.kt`** (신규, `ForyCodec` 바이트 단위 복제):
- `class FastForyCodec(private val fallbackCodec: Codec = RedissonCodecs.Fory) : BaseCodec()`
  - **fallback = `RedissonCodecs.Fory` (Kryo5 아님)** — 체인: FastFory → Fory → Kryo5
- 보조 생성자: `constructor(classLoader: ClassLoader)`, `constructor(classLoader: ClassLoader, codec: FastForyCodec)`
- `private val fory by lazy { BinarySerializers.FastFory }`
- encoder/decoder: try/catch → fallback 경로 (ForyCodec과 동일 구조, 로그 메시지만 "FastForyCodec"으로 교체)
- companion object: KLogging

**`RedissonCodecs.kt`** 추가 val:
```kotlin
val FastFory: Codec by lazy { FastForyCodec() }
val FastForyComposite: Codec by lazy { CompositeCodec(String, FastFory, FastFory) }
val LZ4FastFory: Codec by lazy { Lz4Codec(FastFory) }
val LZ4FastForyComposite: Codec by lazy { CompositeCodec(String, LZ4FastFory, LZ4FastFory) }
val ZstdFastFory: Codec by lazy { ZstdCodec(FastFory) }
val ZstdFastForyComposite: Codec by lazy { CompositeCodec(String, ZstdFastFory, ZstdFastFory) }
val SnappyFastFory: Codec by lazy { SnappyCodecV2(FastFory) }
val SnappyFastForyComposite: Codec by lazy { CompositeCodec(String, SnappyFastFory, SnappyFastFory) }
val GzipFastFory: Codec by lazy { GzipCodec(FastFory) }
val GzipFastForyComposite: Codec by lazy { CompositeCodec(String, GzipFastFory, GzipFastFory) }
```

### 5.3 infra/lettuce `LettuceBinaryCodecs`
```kotlin
fun <V: Any> fastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.FastFory)
fun <V: Any> lz4FastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4FastFory)
fun <V: Any> zstdFastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdFastFory)
fun <V: Any> snappyFastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyFastFory)
fun <V: Any> gzipFastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipFastFory)
```

## 6. KDoc 제약 문구 (한국어, 공통 블록)

모든 신규 FastFory 심볼 KDoc에 다음 경고 블록을 포함:

```
⚠️ **와이어 포맷 경고**
- 이 codec은 `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec(`CompatibleMode.COMPATIBLE`)과 **와이어 포맷이 상호 비호환**합니다.
- **비대칭 호환성**: `FastForyCodec`(Redisson)은 구 Fory 데이터를 fallback으로 읽을 수 있습니다. 반대(`ForyCodec`으로 FastFory 데이터 읽기)는 불가합니다.
- io/lettuce 경로(`BinarySerializers`, `LettuceBinaryCodecs`)는 fallback이 없으므로 기존 Fory 데이터를 FastFory로 읽으면 역직렬화 오류가 발생합니다.
- **휘발성 캐시(Redis, 메모리 캐시) 전용** — 데이터베이스/파일 등 영속 저장에 사용하지 마십시오.
- **순환 참조 객체 불가** (refTracking=false).
- **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
- Redisson `FastForyCodec`은 구 Fory(COMPATIBLE) 바이트를 decode 시 FastFory 실패 → ForyCodec fallback으로 복구합니다. **반대 방향(ForyCodec이 FastFory 바이트 읽기)은 불가**합니다. io/lettuce 경로는 fallback이 없습니다.
```

## 7. 테스트 전략

### 7.1 Roundtrip 단위 테스트 (신규 3개)
- `FastForyCodec` roundtrip (`infra/redisson`) — 기존 `ForyCodecTest` 구조 재사용, codec만 교체.
- `LettuceBinaryCodecs.fastFory()/lz4FastFory()/zstdFastFory()/snappyFastFory()/gzipFastFory()` roundtrip — 기존 lettuce codec 테스트 구조 재사용.
- `BinarySerializers.FastFory/LZ4FastFory/ZstdFastFory/SnappyFastFory/GZipFastFory` roundtrip — 기존 Fory 계열 테스트 구조 재사용.

### 7.2 교차 검증 테스트 (R1 완화, D2 결정에 따라 통합 파일)
각 모듈당 `FastForyCompatibilityTest`. **테스트 방향 주의**:

- **방향 A (유효 테스트)**: Fory 인코드 바이트 → FastForyCodec decode
  - Fory(COMPATIBLE) 바이트를 FastFory(SCHEMA_CONSISTENT)로 decode 시도 → FastFory 실패 → Fory fallback 성공 (바이트가 COMPATIBLE 형식이므로 Fory가 읽을 수 있음)
  - io/io: `assertThrows`로 deserialize 실패 고정 (fallback 없음).
  - lettuce: io/io와 동일 (fallback 없음).
  - Redisson: roundtrip 성공 assert (FastFory 실패 → Fory fallback 성공 경로 검증).

- **방향 B (역방향, 실패 고정 테스트)**: FastFory 인코드 바이트 → ForyCodec decode
  - FastFory(SCHEMA_CONSISTENT) 바이트를 ForyCodec으로 decode → Fory 실패 → Kryo5 fallback도 실패 → 오류
  - 모든 모듈에서 `assertThrows` 또는 null 반환 등 실패 고정.

### 7.3 Fallback Roundtrip 검증 (R2 완화, Redisson only)
- **사용자 결정**: 검증은 roundtrip으로 충분. 로그 어설션 불필요.
- **⚠️ 순환 참조·직렬화 불가 타입 모두 사용 금지** (Codex 최종 확정):
  - 순환 참조 → `StackOverflowError` (Error, ForyCodec이 catch 안 함) → hang/crash 위험
  - "직렬화 불가 타입" → FastFory뿐 아니라 Fory/Kryo5도 실패할 수 있어 검증 불가
- **encode fallback은 코드 패리티(code-parity)만**: encode fallback(FastForyCodec encode 실패 → ForyCodec encode 재시도)은 ForyCodec과 동일한 try/catch 구조를 복제하는 것으로 충분하며, 테스트로 증명하지 않는다. 안전한 fixture 정의가 불가능하기 때문 (순환 참조→StackOverflowError, 직렬화 불가 타입→Fory/Kryo5도 실패 가능).
- **§7.2 방향 A는 decode fallback만 검증**한다. **§7.3은 §7.2 방향 A로 통합하고 별도 encode fallback 테스트는 작성하지 않는다.**

### 7.4 기존 파라미터화 Codec Registry 업데이트 (Codex 리뷰 추가)
- **`RedissonCodecsTest.getRedissonBinaryCodecs()`** — FastFory 계열 **10종** 추가 (plain 5: `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GzipFastFory` + composite 5).
- **`LettuceBinaryCodecTest.getRedisCodecs()`** — `fastFory()`, `lz4FastFory()`, `zstdFastFory()`, `snappyFastFory()`, `gzipFastFory()` **5개** 추가.
- 이 파일에 추가하지 않으면 신규 codec이 기존 넓은 regression 커버리지에서 제외됨.

### 7.5 test/resources
3개 대상 모듈(`io/io`, `infra/redisson`, `infra/lettuce`) 모두 `junit-platform.properties` + `logback-test.xml` 이미 존재 — 추가 작업 불필요.

## 8. 벤치마크 전략

### 8.1 Redisson (`RedissonCodecBenchmark.kt`)
- 기존 codec 필드 블록 근처에 `fastForyCodec`, `lz4FastForyCodec`, `zstdFastForyCodec`, `gzipFastForyCodec` 추가.
- 기존 `@Benchmark fun foryEncodeDecode()` 등의 패턴을 복제하여 FastFory 버전 4개 추가 (`fastForyEncodeDecode`, `lz4FastForyEncodeDecode`, `zstdFastForyEncodeDecode`, `gzipFastForyEncodeDecode`).
- 실행: `./gradlew :bluetape4k-redisson:benchmark`.
- **성공 기준**: 없음 (수치 목표 없이 측정·기록만) — 사용자 확정.

### 8.2 Lettuce (`LettuceCodecBenchmark.kt`)
- 동일 패턴. `fastForyCodec`, `lz4FastForyCodec`, `zstdFastForyCodec`, `gzipFastForyCodec` 필드 + @Benchmark 4개 (`fastForyEncodeDecode`, `lz4FastForyEncodeDecode`, `zstdFastForyEncodeDecode`, `gzipFastForyEncodeDecode`).
- 실행: `./gradlew :bluetape4k-lettuce:benchmark`.

### 8.3 결과 기록 (사용자 확정)
- 벤치마크 출력(ops/s)을 **마크다운 표**로 정리.
- PR description에 표 첨부.
- README.md / README.ko.md 각 모듈에도 벤치마크 결과 표 기록 (Fory vs FastFory vs LZ4/Zstd 변형 비교).
- 구체 수치는 하드웨어 의존이므로 "측정 환경" 명기.

## 9. README 업데이트 범위

각 모듈 README.md + README.ko.md에 추가:
- **io/io**: BinarySerializers 섹션에 FastFory 계열 **5종** (`FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GZipFastFory`) 추가 + 비호환 경고 박스.
- **infra/redisson**: Codec 표에 FastFory 계열 **10종** (plain 5 + composite 5) row 추가 + `forCache()` 팩토리 대안으로 언급(변경은 하지 않음).
- **infra/lettuce**: 팩토리 표에 fastFory 계열 **5개** (`fastFory()`, `lz4FastFory()`, `zstdFastFory()`, `snappyFastFory()`, `gzipFastFory()`) 추가 (Issue 원문의 `foryFast` 표기에서 `fastFory`로 변경 — 명명 규칙 확정에 따른 deviation).

Mermaid 다이어그램 변경은 불필요 (신규 심볼만, 구조 변화 없음).

## 10. bluetape4k-patterns 체크리스트

- [x] **KLogging** companion — FastForyCodec에 적용 (복제 원본에 이미 있음)
- [x] **Korean KDoc** — 모든 신규 val/fun에 한국어 KDoc + §6 경고 블록
- [N/A] **Serializable + serialVersionUID** — codec/serializer는 data 모델 아님
- [N/A] **Coroutines-first** — codec은 blocking I/O 경계 (Redisson/Lettuce API 자체가 blocking byte[] 왕복)
- [x] **requireNotBlank / require\*** — 신규 public API가 String 입력을 받지 않음 (해당 없음)
- [x] **atomicfu 클래스 레벨** — 사용 없음
- [x] **Magic literal 제거** — fallback codec은 상수 참조 `RedissonCodecs.Fory` (Kryo5 아님, Codex 리뷰 수정)
- [x] **DSL/확장** — 기존 팩토리 패턴 유지

## 11. 초안 Task List (Step 3 Plan 입력용)

### Phase 1 — io/io 기반
1. `BinarySerializers.kt`에 `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GZipFastFory` **5개** val 추가 + §6 KDoc.
2. `FastForyCompatibilityTest.kt` (io/io) 작성 — roundtrip PASS + Fory↔FastFory cross-codec FAIL.
3. `./gradlew :bluetape4k-io:test` 통과 확인.

### Phase 2 — infra/redisson
4. `FastForyCodec.kt` 신규 작성 (`ForyCodec` 바이트 단위 복제, fory→BinarySerializers.FastFory, fallback→RedissonCodecs.Fory, 로그 메시지 "FastForyCodec"으로 교체).
5. `RedissonCodecs.kt`에 `FastFory`, `LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`, `GzipFastFory` + 각 `*Composite` **10개** val 추가 + §6 KDoc.
6. `FastForyCompatibilityTest.kt` (infra/redisson) 작성 — 방향 A(Fory 바이트 → FastForyCodec decode 성공) + 방향 B(FastFory 바이트 → ForyCodec decode 실패). 별도 encode fallback 테스트 불필요 (§7.3 결정).
6a. `RedissonCodecsTest.getRedissonBinaryCodecs()` 파라미터에 FastFory **10종** 추가.
7. `RedissonCodecBenchmark.kt` 확장 — FastFory 계열 4개 codec 필드 + `@Benchmark` **4개** (`fastForyEncodeDecode`, `lz4FastForyEncodeDecode`, `zstdFastForyEncodeDecode`, `gzipFastForyEncodeDecode`).
8. `./gradlew :bluetape4k-redisson:test` 통과 확인.
8a. `docs/testlogs/2026-04.md` 맨 위에 테스트 결과 행 추가.
9. `./gradlew :bluetape4k-redisson:benchmark` 실행 → 마크다운 결과 기록.

### Phase 3 — infra/lettuce
10. `LettuceBinaryCodecs.kt`에 `fastFory()`, `lz4FastFory()`, `zstdFastFory()`, `snappyFastFory()`, `gzipFastFory()` 팩토리 **5개** + §6 KDoc.
11. `FastForyCompatibilityTest.kt` (infra/lettuce) 작성.
11a. `LettuceBinaryCodecTest.getRedisCodecs()` 파라미터에 fastFory **5종** 추가.
12. `LettuceCodecBenchmark.kt` 확장 — 4개 codec + `@Benchmark` **4개** (`fastForyEncodeDecode`, `lz4FastForyEncodeDecode`, `zstdFastForyEncodeDecode`, `gzipFastForyEncodeDecode`).
13. `./gradlew :bluetape4k-lettuce:test` 통과 확인.
13a. `docs/testlogs/2026-04.md` 맨 위에 테스트 결과 행 추가.
14. `./gradlew :bluetape4k-lettuce:benchmark` 실행 → 마크다운 결과 기록.

### Phase 4 — 문서화 / 마무리
15. `io/io/README.md` + `README.ko.md` 업데이트.
16. `infra/redisson/README.md` + `README.ko.md` 업데이트.
17. `infra/lettuce/README.md` + `README.ko.md` 업데이트.
18. `docs/superpowers/index/2026-04.md` 본 스펙 엔트리 추가 + `docs/superpowers/INDEX.md` 카운트 갱신.
19. `/wiki-update` 실행.
20. `./gradlew detekt` + 3개 모듈 전체 테스트 재확인.
21. PR 생성 전 CLAUDE.md "Before Creating a PR" 체크리스트 1회 통과.

## 12. 해결된 가정 · 미해결 질문

### 해결된 가정 (모두 Step 1-R 근거)
- `ForyBinarySerializer.fast()` 이미 구현됨 → 새 serializer 작성 불필요.
- `FastFory` pool 설정은 `DefaultFory`와 동일 구조 → thread-safety 별도 검증 불요.
- Redisson `ForyCodec`은 `BaseCodec` 상속 + 2개 보조 생성자 패턴 → 복제 시 동일.
- Kryo5Codec은 `RedissonCodecs.kt` L18에서 이미 import됨.

### 해결된 질문 (2026-04-25 사용자 확정)
1. **SnappyFastFory 전 모듈 추가** (Q1) → **추가 결정**. 3개 모듈 모두 Snappy 계열 포함, 대칭성 유지.
5. **GZipFastFory 전 모듈 추가** (Q1 후속) → **추가 결정**. Gzip 선호 개발자를 위해 3개 모듈 모두 GZip 계열 포함. 모듈별 관례 따름:
   - io/io: `GZipFastFory` (`GZipFory` 관례, `Compressors.GZip`)
   - Redisson: `GzipFastFory` (`GzipFory` 관례, `GzipCodec`)
   - Lettuce: `gzipFastFory()` 함수명, 내부 참조는 `BinarySerializers.GZipFastFory`
2. **벤치마크 성공 기준** (Q2) → **수치 목표 없음**. 측정 후 마크다운 표로 결과 기록, README에 반영.
3. **FastForyCodec fallback** (Q3) → **fallback = ForyCodec** (Kryo5 아님). 체인: FastFory → Fory → Kryo5. 검증은 roundtrip으로 충분.
4. **명명 규칙** (Q1 파생) → **`{압축}FastFory`** 형식 (`LZ4FastFory`, `ZstdFastFory`, `SnappyFastFory`). 기본 심볼: `FastFory`.

---

**다음 단계**: 사용자의 스펙 검토 → (승인 시) `writing-plans` 스킬로 구현 계획 작성.
