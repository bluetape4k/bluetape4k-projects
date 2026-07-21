# Issue #755 caller-owned ByteBuffer compressor 설계

- 상태: 대안 1 선택 승인, 최종 명세 검토 완료, 구현 전 승인 대기
- 대상 issue: `#755 Add caller-owned ByteBuffer compressor APIs for lower GC pressure`
- 대상 milestone: `1.12.0`
- 대상 모듈: `:bluetape4k-io`
- 기준 branch/commit: `origin/develop@a065a8e88cf246975660c68df2dd78dfb5b6dc4d`
- feature branch: `feat/issue-755-bytebuffer-compressor`

## 1. 문제와 목표

현재 `Compressor`의 핵심 공개 계약은 `ByteArray`이다. 기존
`compress(ByteBuffer): ByteBuffer`와 `decompress(ByteBuffer): ByteBuffer`도 내부에서
remaining bytes를 `ByteArray`로 변환한 뒤 새 결과 배열을 감싸므로, 이미 caller-owned
buffer나 buffer pool을 사용하는 파이프라인에서도 payload 크기의 중간 배열이 생긴다.

이 작업의 목표는 caller가 공급한 `ByteBuffer`에 압축 또는 복원 결과를 기록하는 공개
API를 추가하고, 실제 codec API가 지원하는 경로에서 payload 크기의 중간 `ByteArray`
할당을 제거하는 것이다. 처리량 향상은 목표나 승인 조건이 아니다. 성능 주장은
`gc.alloc.rate.norm`으로 확인된 할당 감소에만 한정한다.

## 2. 범위

### 2.1 포함

- `Compressor`에 다음 Java-callable 기본 메서드를 추가한다.

```kotlin
fun compress(source: ByteBuffer, target: ByteBuffer): Int
fun decompress(source: ByteBuffer, target: ByteBuffer): Int
```

- 동일 erased signature의 다른 interface default와 충돌하지 않는 기존 `Compressor` 구현체는
  기본 메서드로 source/binary compatibility를 유지한다.
- `LZ4Compressor`, `DeflateCompressor`, `SnappyCompressor`, `ZstdCompressor`는 지원되는
  저장형 조합에서 저할당 override를 제공한다.
- heap, direct, sliced, read-only, non-zero position, bounded limit, too-small target 계약을
  직접 테스트한다.
- 기존 wire와 신규 API가 서로 압축·복원되는 교차 호환성을 검증한다.
- 대표 codec의 기존 `ByteArray` 경로와 신규 caller-owned 경로를 같은 조건에서 측정한다.
- 공개 KDoc와 `io/io/README.md`, `io/io/README.ko.md`에 지원 행렬과 제한을 기록한다.

### 2.2 제외

- 기존 `compress(ByteBuffer): ByteBuffer`와 `decompress(ByteBuffer): ByteBuffer`의 동작,
  반환형 또는 position 소비 정책 변경
- `StreamingCompressor`의 채널/Okio API 추가
- Netty `ByteBuf` overload
- 새로운 압축 codec 또는 dependency 도입
- 압축 wire format 변경
- stream-only codec을 저할당 경로로 홍보하는 것
- 처리량 개선 목표 또는 기존 same-condition throughput 결론 재평가
- #756의 Redis codec 통합 및 `CompressableBinarySerializer` 연결

## 3. 현재 근거

### 3.1 저장소 근거

- `Compressor.kt`는 `ByteArray` 추상 메서드와 `ByteBuffer` allocating adapter를 제공한다.
- `AbstractCompressor.kt`는 null/empty 처리와 `ByteArray` 구현 경계를 소유한다.
- `ByteBufferInputStream`은 caller source의 duplicate view를 stream input으로 사용할 수 있다.
- `ByteBufferOutputStream.fixed`는 caller target의 현재 limit을 hard bound로 사용하고
  초과 기록을 `BufferOverflowException`으로 보고한다.
- `BinarySerializer.serializeTo`는 이미 caller-owned target에 대해 다음 관례를 사용한다.
  성공 시 position만 전진하고, 실패 시 position을 복원하며, 실패 전 덮어쓴 byte는
  복구하지 않는다.
- 변경 전 `./gradlew :bluetape4k-io:test`는 1,109 tests PASS이다.

### 3.2 dependency API 근거

현재 resolved dependency source와 JDK 21 API를 직접 확인했다.

| Codec | 확인된 API | 제약 |
|---|---|---|
| LZ4 1.11.0 | absolute-offset `compress(ByteBuffer, ...)`, `decompress(ByteBuffer, ...)` | heap/direct 및 slice 지원, absolute API는 position/limit을 변경하지 않음 |
| Snappy 1.1.10.8 | `compress(ByteBuffer, ByteBuffer)`, `uncompress(ByteBuffer, ByteBuffer)` | direct→direct만 지원하고 output limit을 변경함 |
| Snappy 1.1.10.8 | offset 기반 `byte[]` API | array-backed heap→heap 지원 |
| zstd-jni 1.5.7-11 | direct-buffer 및 byte-array offset API | direct→direct 또는 array-backed heap→heap만 직접 지원 |
| JDK 21 Deflater/Inflater | `setInput(ByteBuffer)`, `deflate(ByteBuffer)`, `inflate(ByteBuffer)` | heap/direct buffer를 처리하지만 출력 크기를 사전 확정할 수 없음 |
| JDK/Apache framed codecs | stream API | buffer-native API가 아니므로 이번 범위에서는 compatibility fallback |

## 4. 검토한 대안

### 4.1 선택: `Compressor` 기본 메서드와 backend override

모든 구현체가 동일한 공개 API를 가지며, 기본 메서드는 기존 `ByteArray` 동작으로
호환성을 유지한다. 실제 codec이 buffer/offset API를 제공할 때만 override한다.

장점:

- 기존 factory 반환형과 Java/Kotlin 호출 방식이 유지된다.
- serializer 계층의 `serializeTo` 계약과 일관된다.
- 새로운 구현체가 기존 `ByteArray` 메서드만 구현해도 계속 동작한다.
- backend별 최적화를 독립적으로 추가하거나 철회할 수 있다.

단점:

- API 존재 자체가 저할당을 보장하지 않는다.
- 호출자는 문서의 지원 행렬과 측정 범위를 확인해야 한다.

이 단점은 KDoc, README, benchmark report에서 `compatibility fallback`과 `optimized
override`를 명시적으로 구분해 완화한다.

### 4.2 기각: `BufferCompressor : Compressor` capability interface

실제 buffer-native codec만 별도 interface를 구현하면 capability는 더 명확하다. 그러나
현재 factory와 대부분의 caller가 `Compressor`로 다루므로 type check/cast가 필요하고,
기존 serializer 및 Redis 연결부에서 두 계층을 계속 전파해야 한다. Java caller에게도
불필요한 분기가 생긴다.

### 4.3 기각: top-level extension 함수만 추가

ABI 영향은 작지만 interface dispatch를 사용할 수 없어 backend override가 불가능하다.
Java에서는 자연스러운 instance API가 아니며, 결국 codec type 분기 또는 별도 registry가
필요하다.

## 5. 공개 API 계약

### 5.1 공통 상태 계약

두 신규 메서드는 `[source.position(), source.limit())`를 입력으로 읽고
`[target.position(), target.limit())`에 결과를 기록한다.

성공 시:

- 반환값은 target에 기록한 정확한 byte 수이다.
- source의 `position`, `limit`, `mark`, `capacity`, `byteOrder`는 보존한다.
- target의 `position`만 반환값만큼 증가한다.
- target의 `limit`, `capacity`, `byteOrder`는 보존한다.
- target의 기존 mark는 정상적인 JDK position 이동 규칙을 따른다.

실패 시:

- source의 observable state를 보존한다.
- target position은 호출 시작 값으로 복원한다.
- target limit, capacity, byteOrder를 보존한다.
- codec이 실패 전에 target에 기록한 byte의 원상 복구는 보장하지 않는다.
- 실패는 target을 poison하지 않는다. overflow 뒤에는 같은 target과 더 작은 valid
  source로, corrupt-input 또는 injected codec failure 뒤에는 같은 target과 valid source로
  재호출할 수 있다. 재호출은 시작 position부터 결과 전체를 다시 기록하며 stale partial
  byte를 결과 범위에 포함하지 않는다.

caller는 호출 중 각 source와 target을 단일 thread에 한정해야 한다. bluetape4k 내장
`Compressors` singleton과 신규 helper/optimized override는 동시 호출할 수 있어야 하며,
mutable cursor와 JDK/native codec resource는 호출별 local state여야 한다. 외부
`Compressor` 구현체의 default method thread-safety는 그 구현체의 기존 `ByteArray` operation
계약을 그대로 따른다.

source와 target의 underlying byte range는 겹치지 않아야 한다. 동일 객체는
`IllegalArgumentException`으로 거부한다. 두 buffer가 array-backed이고 같은 backing
array를 노출하면 `arrayOffset() + position()`과 remaining range로 overlap을 탐지해 codec
호출 전에 `IllegalArgumentException`으로 거부한다. JDK API로 탐지할 수 없는 direct 또는
read-only alias는 caller precondition이며 지원하지 않는다.

### 5.2 preflight 순서

신규 두 메서드의 `source`와 `target`은 Kotlin non-null parameter다. Java caller가 어느
인자에든 null을 전달하면 buffer 상태 검사 전에 raw `NullPointerException`이 발생하며 예외
message는 계약하지 않는다. 둘 다 null인 경우에도 같은 예외 타입만 계약한다.

모든 non-null 경로는 다음 순서를 유지한다.

1. target이 read-only면 `ReadOnlyBufferException`을 던진다.
2. source와 target이 동일 객체이거나 탐지 가능한 array range가 겹치면
   `IllegalArgumentException`을 던진다.
3. source remaining이 0이면 `0`을 반환한다.
4. backend가 exact 결과 크기를 알거나 안전상 full bound가 필수이면
   `initialTargetRemaining`을 검사한다.
5. absolute/offset API 또는 필요한 경우에만 duplicate view에서 codec을 실행한다.
6. 성공한 기록량만 원본 target position에 commit한다.

read-only target은 alias, empty-source, codec 초기화, 압축 또는 복원보다 먼저 거부한다.
따라서 same-object+read-only와 empty+read-only는 `ReadOnlyBufferException`, writable
same-object+empty는 `IllegalArgumentException`이다.

### 5.3 overflow와 실패 정규화

- 기본 fallback은 결과 `ByteArray` 크기를 확인한 뒤 기록하므로 작은 target을 raw
  `BufferOverflowException`으로 보고하고 byte를 기록하지 않는다.
- exact 크기를 아는 decompression 경로와 full bound가 native 안전성에 필수인 Snappy
  compression은 codec 호출 전에 `BufferOverflowException`으로 실패한다.
- destination length를 안전하게 받는 LZ4/Zstd compression은 4-byte header를 제외한
  `payloadCapacity = initialTargetRemaining - 4`를 codec에 전달한다. backend의
  destination-too-small 결과만 raw
  `BufferOverflowException`으로 정규화한다.
- Deflate처럼 출력 크기를 사전 확정할 수 없는 경로는 duplicate target에 기록하다
  공간이 소진되면 `BufferOverflowException`으로 실패한다. 원본 target position은
  복구하지만 이미 덮어쓴 byte는 unspecified이다.
- codec 고유의 corrupt-input, dictionary, native 또는 I/O 실패는 기존 예외 의미를
  보존한다. 단순 target 부족을 codec 오류로 노출하지 않는다.
- `Error`와 cancellation 계열은 identity를 보존해 그대로 전파한다.

### 5.4 기존 API와의 관계

- `compress(ByteArray?)`와 `decompress(ByteArray?)`는 변경하지 않는다.
- 기존 single-argument `ByteBuffer` overload는 source compatibility와 기존 position
  동작을 위해 변경하지 않는다.
- 신규 two-argument overload만 source-preserving caller-owned 계약을 제공한다.
- interface default method는 새 구현체와 기존 외부 구현체에 allocating compatibility
  fallback을 제공한다. 외부 구현체의 동시 호출 보장은 기존 `ByteArray` operation보다
  강화하지 않는다.

## 6. 내부 구조

### 6.1 기본 fallback helper

`compressor` package에 internal helper를 둔다. helper는 다음 책임만 가진다.

- read-only, same-object, 탐지 가능한 array overlap, empty-input preflight
- source duplicate에서 remaining bytes 복사
- 기존 `ByteArray` operation 호출
- exact output-size overflow 검사
- target position commit/rollback
- fatal failure identity 보존

이 helper는 low-allocation으로 문서화하지 않는다. `Compressor` 기본 메서드는 각각
기존 `compress(ByteArray?)`와 `decompress(ByteArray?)`를 lambda로 전달한다.

### 6.2 optimized operation wrapper

backend override가 상태 계약을 반복 구현하지 않도록 internal wrapper를 둔다.

- 원본 source/target state capture
- absolute/offset 경로는 캡처한 position/remaining을 원본 buffer와 함께 사용하고 view를
  할당하지 않음
- Snappy direct 및 JDK Deflate처럼 position/limit을 변경하는 경로만 duplicate view 생성
- backend operation 실행
- 반환 기록량 검증: `0 <= written <= initialTargetRemaining`
- 성공 시 원본 target position commit
- 실패 시 원본 target position 복구
- 모든 backend length는 capacity가 아니라 `initialTargetRemaining`으로 제한하며,
  array-backed offset은 `arrayOffset() + position()`으로 계산
- backend가 duplicate limit을 변경해도 원본 limit은 영향받지 않음

wrapper는 codec 선택이나 wire 처리 책임을 갖지 않는다.

header-prefixed codec은 wrapper에 전체 기록량을 반환한다. LZ4와 Zstd compression은
`initialTargetRemaining >= 4`를 먼저 확인하고,
`payloadCapacity = initialTargetRemaining - 4`만 native codec에 전달한다. 성공 시
`totalWritten = 4 + payloadWritten`을 검증하고 target position 및 public 반환값에
commit한다.

## 7. backend별 설계

### 7.1 LZ4

- heap/direct/sliced/read-only source와 writable heap/direct/sliced target을 처리한다.
- 기존 wire `[original size: 4-byte big-endian][compressed payload]`를 유지한다.
- header는 target byte order와 무관하게 big-endian byte로 기록한다.
- compression은 header를 제외한 `initialTargetRemaining - 4`를 destination length로 전달한다.
  LZ4가 destination-too-small을 보고할 때만 `BufferOverflowException`으로 정규화한다.
- decompression은 source duplicate에서 4-byte header를 읽고 기존 256 MiB 제한을
  적용한다. target remaining이 원본 크기보다 작으면 codec 호출 전에 실패한다.
- LZ4 absolute-offset API는 caller buffer position을 변경하지 않는다.
- `LZ4FastDecompressor` 반환값은 기록량이 아니라 compressed payload의 consumed byte다.
  성공은 `consumed == compressedPayloadRemaining`이어야 하며, target commit 양은 header의
  `declaredOriginalSize`다. trailing 또는 truncated payload는 성공으로 처리하지 않는다.

### 7.2 Snappy

- direct→direct는 Snappy ByteBuffer API를 duplicate view에서 사용한다.
- writable array-backed heap→heap은 offset 기반 byte-array API를 사용한다.
- mixed storage, read-only heap source, 또는 array를 노출하지 않는 heap source는 기본
  fallback으로 내려간다.
- compression target은 `Snappy.maxCompressedLength(source.remaining())` 이상이어야 한다.
- decompression은 정확한 source range에서 `Snappy.uncompressedLength`로 exact size를 확인하고
  기존 256 MiB 제한을 적용한다.
- heap과 direct optimized decompression 모두 native crash 위험을 줄이기 위해 codec이
  제공하는 range-aware compressed-buffer validation을 먼저 수행한다. invalid/truncated
  input은 `SnappyException`으로 보고하며 native decompression을 실행하지 않는다.
- Snappy가 output duplicate의 limit을 바꾸더라도 원본 target limit은 보존한다.

### 7.3 Zstd

- direct→direct는 zstd-jni direct-buffer offset API를 사용한다.
- writable array-backed heap→heap은 byte-array offset API를 사용한다.
- 그 밖의 조합은 기본 fallback을 사용한다.
- 기존 `[original size: 4-byte big-endian][compressed payload]` wire를 유지한다.
- compression은 header를 제외한 `payloadCapacity = initialTargetRemaining - 4`를 destination
  length로 전달하고,
  zstd의 destination-too-small error만 `BufferOverflowException`으로 정규화한다.
- decompression은 header의 exact size와 기존 256 MiB 제한을 먼저 검증한다.
- native 반환 code는 `Zstd.isError`로 검사한다. compression의 destination-too-small만 raw
  `BufferOverflowException`으로 정규화하고, 그 밖의 error code는 `Zstd.getErrorName(code)`를
  message에 포함한 정확한 `IllegalStateException`으로 변환하며 cause는 두지 않는다. JNI가
  직접 던진 `ZstdException`과 fatal throwable은 identity를 보존해 그대로 전파한다.
- decompression 성공은 native 결과가 `declaredOriginalSize`와 정확히 같을 때만 commit한다.
  불일치는 안정적 message를 가진 정확한 `IllegalStateException`이며 cause는 두지 않는다.
  과대·과소 변조 header와 truncated payload를 성공으로 처리하지 않는다.

### 7.4 Deflate

- JDK 21 `Deflater`/`Inflater` ByteBuffer API를 duplicate source/target과 함께 사용한다.
- 기존 zlib wire를 유지한다.
- compression은 `finish()` 후 아래 상태표에 따라 `finished()`까지 진행한다.
- decompression은 아래 상태표로 `finished`, `needsDictionary`, `needsInput`, target
  exhaustion과 no-progress를 구분한다.
- invalid compressed data는 기존 Deflate caller가 이해할 수 있는 `ZipException` 계열로
  정규화하고 원인을 보존한다.
- `Deflater`/`Inflater`는 per-call로 생성하고 instance field나 singleton에 저장하지 않는다.
- internal factory seam으로 test engine을 주입할 수 있게 하되 public constructor와 factory
  API는 변경하지 않는다.
- `Deflater.end()`와 `Inflater.end()`는 모든 성공/실패 경로에서 정확히 한 번 실행한다.
  operation failure가 있으면 cleanup failure를 suppressed로 붙이고 원래 throwable identity를
  유지한다. operation failure가 없으면 cleanup failure 자체를 전파한다.

#### Deflate loop 상태표

각 iteration은 호출 전후 source position, target position과 반환 byte 수를 비교한다.
판정 순서는 표의 위에서 아래다.

| 조건 | compression | decompression |
|---|---|---|
| `finished()` | 기록량 commit | 기록량 commit |
| target remaining `0` | raw `BufferOverflowException` | raw `BufferOverflowException` |
| 반환량 `> 0` 또는 source position 전진 | 계속 | 계속 |
| `needsDictionary()` | N/A | cause 없이 안정적 메시지를 가진 `ZipException` |
| `needsInput()` | finish 이후 `IllegalStateException` | truncated input `ZipException` |
| 그 밖의 zero-progress | `IllegalStateException` | `ZipException` |

`finished()`를 target exhaustion보다 먼저 확인해 결과가 target limit에 정확히 맞는 성공을
overflow로 오판하지 않는다. 어떤 분기도 상태 변화 없이 loop로 돌아가지 않는다.
`Inflater.inflate`가 던진 `DataFormatException`은 cause로 연결한 `ZipException`으로
변환하지만, Boolean 상태인 `needsDictionary()`에는 존재하지 않는 cause를 만들지 않는다.

### 7.5 compatibility fallback codec

GZip, Apache GZip/Deflate/Zstd, BZip2, Block/Framed LZ4, Framed Snappy,
Zip 및 `StreamingCompressorAdapter`는 이번 작업에서 backend override를 갖지 않는다.
신규 API는 정상 동작하지만 기존 `ByteArray` 경로를 사용한다.

stream API로 caller target에 연결하는 것은 기술적으로 가능하지만 다음 이유로 이번
범위에서 기각한다.

- 각 codec의 close/finalization 실패와 target rollback 의미를 별도로 설계해야 한다.
- 일부 decompression 경로의 output bound가 서로 다르다.
- stream 내부 buffer 할당과 one-shot adapter를 제거하지 않으면 allocation 주장이
  불명확하다.

후속 최적화는 측정 근거와 codec별 안전 계약을 갖춘 별도 issue로 분리한다.

## 8. 호환성

### 8.1 source 및 binary compatibility

- interface에 concrete default method만 추가한다.
- 기존 abstract method, constructor, factory 반환형을 변경하지 않는다.
- 동일 erased signature의 다른 interface default를 함께 상속하지 않는 Kotlin/Java 기존
  caller와 구현체의 재컴파일 및 이전 compiled caller 실행을 검증한다.
- Java interface evolution의 표준 제한(JLS 13.5.7)에 따라 외부 구현체가 다른 interface의
  동일 default signature를 함께 상속하면 source compile conflict 또는 runtime
  `IncompatibleClassChangeError`가 날 수 있다. 이를 무조건 호환으로 주장하지 않는다.
- 신규 Java caller가 두 메서드를 instance method로 호출할 수 있어야 한다.
- 무형식 `compress(null)`은 기존 overload 집합에서도 ambiguous하므로 baseline-relative
  negative fixture로 유지한다. `compress((ByteArray) null)` 같은 기존 explicit-cast
  positive fixture와 신규 two-argument positive fixture가 계속 compile되는지 확인한다.

### 8.2 migration과 rollback

- 기존 caller는 migration할 필요가 없다. two-argument API는 reusable/pool target과
  optimized storage 조합을 이미 가진 caller만 GC 감소 목적으로 opt-in한다.
- fallback codec/storage 조합은 correctness-only다. allocation 감소가 목적이면 기존
  single-argument API를 그대로 사용하는 것이 기본 선택이다.
- 기존 single-argument ByteBuffer API는 일부 source position을 소비할 수 있지만 신규
  two-argument API는 source state를 보존한다. migration 시 이 차이를 명시한다.
- milestone `1.12.0` CHANGELOG/release note에 신규 opt-in API, optimized matrix, fallback 및
  source-position 차이와 표준 default-method 충돌 caveat를 기록한다.
- release 후 특정 optimized override에 결함이 발견되면 공개 default method와 wire contract는
  유지하고 해당 override만 compatibility fallback으로 되돌린 patch를 낸다. patch release에서
  공개 method를 제거하지 않는다.
- runtime feature flag는 추가하지 않는다. patch 전 우회가 필요하면 기존 allocating API 또는
  문서화된 fallback storage 조합을 사용한다.

### 8.3 wire compatibility

각 optimized codec에 다음 두 방향을 검증한다.

1. 기존 `compress(ByteArray)` 결과를 신규 `decompress(source, target)`이 복원한다.
2. 신규 `compress(source, target)` 결과를 기존 `decompress(ByteArray)`가 복원한다.

LZ4/Zstd header byte order, Snappy raw format, Deflate zlib framing은 변경하지 않는다.
같은 입력의 compressed byte가 항상 byte-for-byte 동일하다고 일반화하지 않는다.
Deflate 같은 codec은 설정과 library 구현에 따라 유효한 wire가 달라질 수 있으므로
상호 복원 가능성을 계약으로 삼는다.

## 9. target sizing과 caller 재시도

이번 설계는 `maxCompressedSize` 또는 `requiredOutputSize` 공개 API를 추가하지 않는다.
codec마다 exact size를 알 수 있는 시점과 안전한 bound가 다르고, generic estimate가 잘못된
저할당 보장을 만들기 때문이다. `BufferOverflowException`도 필요 크기를 제공하지 않는다.

| 경로 | 구현이 아는 크기 | caller 계약 |
|---|---|---|
| LZ4/Zstd compression | native 호출 전 exact size는 모름 | 현재 target remaining을 사용하고 overflow 시 더 큰 target으로 전체 재시도 |
| Snappy compression | native 안전상 max bound 필요 | bound보다 작으면 preflight overflow; caller는 더 큰 target으로 재시도 |
| Deflate compression/decompression | 완료 전 exact size 모름 | bounded write 중 overflow; 더 큰 target으로 전체 재시도 |
| LZ4/Snappy/Zstd decompression | 구현 내부에서 declared/exact size 확인 | target이 작으면 overflow지만 필요 크기는 외부에 노출하지 않음 |
| compatibility fallback | 내부 결과 `ByteArray` 생성 후 exact size 확인 | target은 최종 write bound일 뿐 decompression resource bound가 아님; overflow에 exact size를 첨부하지 않고 전체 재시도 |

표준 retry는 source를 rewind하지 않는다. 신규 API가 source state를 보존하므로 caller는 같은
source와 새 target을 그대로 전달한다. target은 application이 정한 초기 pool 크기에서
시작해 overflow마다 두 배로 늘리되, untrusted input에 대해서는 application maximum을 넘기지
않고 실패해야 한다. 2배 증가가 overflow되거나 maximum을 넘으면 더 이상 재시도하지 않는다.
실패한 작은 target의 byte를 이어 붙이지 않고 새 target의 시작 position부터 결과 전체를
다시 기록한다.

## 10. 보안과 안정성

- LZ4, Snappy, Zstd의 256 MiB decompression 제한을 신규 경로에도 동일하게 적용한다.
- GZip 등 fallback은 기존 방어와 예외를 그대로 사용한다.
- optimized decompression에서 caller target remaining은 codec 실행 전 또는 실행 중 적용되는
  추가 hard output bound다.
- compatibility fallback의 target remaining은 전체 결과 `ByteArray`가 생성된 뒤 적용되는 최종
  write bound일 뿐 decompression memory/resource bound가 아니다. 작은 target만으로 untrusted
  fallback input의 대규모 할당이나 `OutOfMemoryError`를 방지한다고 보장하지 않는다.
- corrupt header, negative size, oversized declared size, truncated payload를 테스트한다.
- Snappy heap/direct optimized path는 invalid native input validation 없이 raw decompression을 호출하지
  않는다.
- 동일 객체와 탐지 가능한 array-backed overlap은 즉시 거부한다.
- no-progress loop, native error code 무시, cleanup 누락을 허용하지 않는다.
- 공유 `Compressors` singleton 호출 사이에 mutable buffer/codec state를 보관하지 않는다.
- buffer 또는 payload 내용을 log하지 않는다.

### 10.1 decompression 복합 실패 우선순위

공통 read-only/overlap/empty preflight 이후 backend별 순서는 다음과 같다.

| Backend | 판정 순서 |
|---|---|
| LZ4 | 4-byte header 구조 → declared size 범위 → target remaining → payload decode → consumed/trailing 검사 |
| Snappy | compressed range validation → uncompressed length/256 MiB → target remaining → native decode |
| Zstd | 4-byte header 구조 → declared size 범위 → target remaining → native decode/error → exact result 검사 |
| Deflate | stream traversal 중 `DataFormatException`/상태표/target exhaustion 순으로 먼저 관찰된 확정 상태 |
| fallback | 기존 ByteArray decompression과 security limit → exact result target overflow |

LZ4/Zstd는 header 구조와 declared-size 오류만 target overflow보다 우선한다. header가
유효하지만 payload가 corrupt한 상태에서 target까지 작으면 decode 전에
`BufferOverflowException`이 발생한다. Snappy는 compressed-range validation을 먼저 수행하므로
같은 복합 입력에서 `SnappyException`이 우선한다. Deflate는 output size를 사전 결정할 수
없으므로 corruption이 target exhaustion 전에 관찰되면 `ZipException`, target이 먼저
소진되면 `BufferOverflowException`이다. fallback은 기존 `ByteArray` decompression이 target
검사보다 먼저 실행되므로 기존 corrupt-input failure가 우선할 수 있다. 각 backend는
corrupt-payload+too-small-target compound fixture로 이 순서를 검증한다.

### 10.2 runtime diagnosis

이 synchronous library API는 신규 log나 metric을 방출하지 않고 payload/buffer 내용을 절대
기록하지 않는다. optimized/fallback 선택도 runtime telemetry로 노출하지 않으며 문서의
storage matrix로 판단한다.

caller가 진단을 남길 때 허용되는 privacy-safe field는 codec, operation, source/target
remaining, heap/direct/read-only/slice 분류, exception type/cause, bluetape4k version이다.
payload byte, 압축 전후 내용, backing array는 기록하지 않는다. raw
`BufferOverflowException`에 required size가 없다는 제한도 KDoc과 README에 기록한다.

## 11. 테스트 전략

### 11.1 공통 contract suite

모든 `Compressor` 구현체에 기본 API contract를 적용한다.

- heap/direct source와 target roundtrip
- non-zero position 및 bounded limit
- sliced source/target
- source state 보존
- 성공 시 target position-only commit
- Java null source, null target, 둘 다 null, null source+read-only target이 buffer preflight보다
  먼저 raw `NullPointerException`을 내는 runtime fixture
- 성공·overflow·codec failure 후 source/target mark의 `reset()` 가능 여부
- read-only target preflight와 same-object/empty 조합의 정확한 precedence
- too-small target position rollback
- overflow 후 더 작은 valid source, corrupt/injected failure 후 valid source로 같은 target 재시도
- pre-created `Error`, `CancellationException`, runtime codec failure의 `assertSame` identity
- 기존/new API wire interoperability
- same-object 및 같은 backing array의 부분/완전 overlap rejection
- `capacity > limit`인 heap/direct/slice target에서 limit 밖 sentinel byte 보존
- 각 bluetape4k 내장 shared `Compressors` singleton에서 서로 다른 buffer로 success, overflow,
  corrupt input을 병렬 반복하고 신규 helper/override의 호출별 state 격리
- backend별 corrupt-input+too-small target compound precedence

fallback codec도 correctness contract를 통과해야 하지만 allocation 감소 대상으로
분류하지 않는다.

### 11.2 optimized backend suite

- LZ4: heap/direct 모든 조합, header 경계, high-compression payload의
  `written=declaredOriginalSize`, consumed mismatch, trailing/truncated payload
- Snappy: heap→heap, direct→direct optimized; mixed 조합 fallback; heap/direct invalid input이
  native call 전에 거부됨
- Zstd: heap→heap, direct→direct optimized; mixed 조합 fallback; header 이후 native destination
  length가 `initialTargetRemaining - 4`임을 non-zero position, exact-limit/sentinel,
  `initialTargetRemaining == 4` 경계로 검증; heap/direct의 native error code는 exact
  `IllegalStateException` class/error-name/no-cause를, 직접 던진 `ZstdException`은 identity를,
  result와 declared size mismatch는 exact class/stable-message/no-cause를 검증; 과대·과소
  header, truncated payload
- Deflate: heap/direct 조합, incompressible payload, target exhaustion, corrupt input,
  dictionary/no-progress 상태, dictionary+zero-remaining target의 `BufferOverflowException`,
  exact-fit success, `end()` 1회, cleanup suppression
- codec 고유 256 MiB 제한과 corrupt/truncated input
- 압축률이 큰 fallback payload와 작은 target에서 target overflow 판정보다 내부 결과 할당이
  선행할 수 있음을 contract/documentation test로 고정
- target duplicate limit을 변경하는 backend가 원본 limit을 보존하는지 검증

### 11.3 ABI 및 caller fixture

- baseline은
  `a065a8e88cf246975660c68df2dd78dfb5b6dc4d`에서 생성한
  `bluetape4k-io-1.12.0.jar`이며 SHA-256은
  `34d280b0cb465ffca2a23a2aa57895cc3ba9c08ea18f57c706443b91a0eae6f1`이다.
- old Kotlin/Java implementor와 caller fixture는 이 baseline jar를 compile classpath로 사용하고
  Kotlin compiler `2.4.0`, language/api `2.3`, `-jvm-default=enable`, Java 21을 고정한다.
- fixture source, classfile-only fixture jar, provenance manifest와 SHA-256을
  `io/io/src/test/resources/abi/issue-755/`에 보존한다. fixture jar에는 baseline
  `Compressor` class를 포함하지 않는다.
- ABI runtime test는 보존된 implementor/caller classfile을 재컴파일하지 않고 신규 main
  classes와 함께 실행하며, 먼저 fixture jar와 provenance hash를 검증한다.
- 신규 Kotlin/Java caller compile 및 Java null runtime contract
- overload ambiguity fixture
- `javap` 또는 repository의 기존 ABI 검증 패턴으로 concrete JVM default method 확인

### 11.4 실행 순서

1. 신규 contract test의 RED 확인
2. helper/default method GREEN
3. codec별 RED/GREEN
4. targeted compressor tests
5. `:bluetape4k-io:test`
6. Kotlin compile 및 Detekt
7. ABI/caller fixture
8. benchmark smoke와 canonical allocation runs
9. `git diff --check` 및 문서 parity 검사

## 12. allocation 검증

기존 `SameConditionCompressorBenchmark`의 fixture와 `testBenchmarkJar` 실행 경로를
재사용한다. 신규 benchmark case는 최소 LZ4, Snappy, Zstd, Deflate에 대해 다음을
분리한다.

- heap/direct별로 같은 pre-created source를 사용하는 기존 single-argument `ByteBuffer`
  compress/decompress primary baseline
- 기존 `ByteArray` compress/decompress secondary baseline
- caller-owned heap target
- 지원되는 경우 caller-owned direct target
- unsupported storage 조합 fallback

source와 target은 `@State(Scope.Thread)`인 신규 mutable benchmark state의 trial setup에서
worker thread별로 생성한다. `Scope.Benchmark` mutable buffer 공유는 금지하고 validator가
state scope를 fail closed로 검사한다. measured method는 source position/limit과 caller-owned
target position/limit을 reset한 뒤 codec 호출만 수행한다. 측정 본문에서
`allocate`, `allocateDirect`, `wrap` 또는 payload 복사를 수행하지 않는다. 1 KiB, 64 KiB,
512 KiB payload를 각각 측정해 감소분이 payload 크기와 함께 증가하는지 확인한다.

주 지표는 JMH GC profiler의 `gc.alloc.rate.norm` (`B/op`)이다. throughput은 진단용이며
승인 판단에 사용하지 않는다.

긍정적 allocation 주장은 다음 조건을 모두 만족할 때만 허용한다.

- 같은 payload, codec configuration, buffer kind, JVM 옵션을 사용한다.
- fork, warmup, measurement metadata를 보존한다.
- 두 번의 fresh canonical run이 모두 같은 방향이다.
- 각 run에서 optimized 경로가 대응 baseline보다 최소 5% 낮다.
- JMH error/confidence range가 대응 baseline과 겹치지 않는다.
- `gc.alloc.rate.norm`의 `B/op`를 기록하고 payload-size scaling과 optimized-path 코드 검사를
  함께 사용해 고정 view/wrapper 비용과 payload-sized allocation 제거를 구분한다.
- raw JSON과 요약 문서를 issue별 immutable 경로에 보존한다.

throughput은 개선 승인 조건이 아니지만 안정성 guard로 기록한다. 특히 validation pass가
추가되는 Snappy decompression을 포함해 matched baseline보다 20% 이상 낮은 throughput이
두 canonical run에서 모두 관찰되고 error range가 겹치지 않으면 allocation 결과를
승인 근거로 승격하기 전에 원인 분석과 설계 재검토를 수행한다.

조건을 만족하지 않으면 결과를 inconclusive 또는 no improvement로 기록한다. API의
correctness는 allocation 결과와 독립적이며, 측정 실패를 throughput 주장으로 대체하지
않는다.

### 12.1 evidence 소유권과 불변성

- canonical run은 `docs/benchmarks/raw/issue-755/run-<UTC>-<id>/`에 append-only로 저장한다.
  기존 run id를 덮어쓰거나 재사용하지 않는다.
- 고정 보고서는 `docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md`이며
  `docs/benchmarks/README.md`에서 연결한다.
- raw JMH JSON이 authority다. derived CSV와 보고서는 raw run id를 정확히 연결한다.
- 두 canonical run은 동일한 exact Git commit/tree와 benchmark JAR SHA-256을 사용한다.
- 각 run은 literal argv, JDK/JVM/GC, OS/CPU, dependency version, fork/warmup/measurement,
  metric/unit/error range, commit/tree, JAR hash를 보존한다.
- 구현/측정 owner가 artifact를 생성하고 독립 reviewer 또는 CI validator가 검증한다.
- validator는 `gc.alloc.rate.norm` metric key, `B/op` unit, error range, thread-local mutable
  state scope, commit/JAR/environment identity가 없거나 불일치하면 fail closed한다. 중복·덮어쓴
  run id와 fallback/ineligible cell의 positive promotion도 거부한다.
- rebase, benchmark source/fixture/config 또는 benchmark JAR 입력이 바뀌면 기존 canonical
  evidence는 무효이며 같은 새 artifact에서 두 run을 다시 수행한다.

## 13. 문서화

- 공개 KDoc은 영어로 작성한다.
- `io/io/README.md`와 `io/io/README.ko.md`는 동일한 예제와 지원 행렬을 유지한다.
- 기본 fallback도 동작하지만 payload-sized 배열을 할당할 수 있음을 명시한다.
- optimized 저장형 조합과 fallback 조합을 표로 제공한다.
- caller-owned target sizing, source preservation, position commit/rollback, overlap 금지,
  thread confinement을 예제 옆에 기록한다.
- benchmark report는 할당 결과만 주장하고 throughput을 약속하지 않는다.
- `CHANGELOG.md`와 milestone `1.12.0` release note에 opt-in API와 rollback 정책을 기록한다.
- runtime log/metric이 없고 fallback dispatch telemetry를 제공하지 않는다는 제한과
  privacy-safe caller diagnostics를 KDoc 및 양쪽 README에 기록한다.
- fallback decompression target은 최종 write bound일 뿐 untrusted input의 memory/resource
  bound가 아니라는 제한을 KDoc과 양쪽 README에 명시한다.

### 13.1 canonical Kotlin 예제

```kotlin
val source = ByteBuffer.wrap(payload)
val target = ByteBuffer.allocate(64 * 1024).apply { position(16) }
val start = target.position()
val written = compressor.compress(source, target)

val compressed = target.duplicate().apply {
    position(start)
    limit(start + written)
}.slice()
```

예제는 `source`를 rewind하지 않는다. 결과는 무조건 `flip()`하지 않고 호출 전 `start`와
반환 `written`으로 bounded view를 만든다. overflow가 나면 partial byte를 읽지 않고
application maximum 아래의 더 큰 새 target에 같은 source를 전달해 전체 작업을 재시도한다.

### 13.2 canonical Java 예제

```java
ByteBuffer source = ByteBuffer.wrap(payload);
ByteBuffer target = ByteBuffer.allocate(64 * 1024);
target.position(16);
int start = target.position();
int written = compressor.compress(source, target);

ByteBuffer compressed = target.duplicate();
compressed.position(start);
compressed.limit(start + written);
compressed = compressed.slice();
```

Kotlin/Java README 예제는 compile test에 포함하고 source position 불변, non-zero target
start, exact bounded result view를 assertion으로 검증한다. 기존 caller에게 blanket migration을
권하지 않으며 optimized matrix와 reusable target이 모두 있는 경우에만 이전을 안내한다.

## 14. 실패 모드와 대응

| 실패 모드 | 예방/탐지 | caller-visible 결과 |
|---|---|---|
| target이 read-only | codec 이전 preflight | raw `ReadOnlyBufferException` |
| target remaining 부족 | exact/bound preflight 또는 duplicate write bound | raw `BufferOverflowException`, position rollback |
| LZ4/Zstd 4-byte 미만 header | 기존 ByteArray 경로와 동일한 경계 검사 | `IndexOutOfBoundsException` |
| LZ4/Zstd negative/oversized header | header 및 256 MiB 검사 | `IllegalArgumentException` |
| LZ4 invalid/trailing/truncated payload | consumed와 payload range 비교 | `LZ4Exception` |
| Zstd invalid payload 또는 size mismatch | error code와 exact result 검사 | error name 또는 stable mismatch message를 가진 정확한 `IllegalStateException`, cause 없음; JNI가 직접 던진 `ZstdException`은 identity 보존 |
| Snappy invalid/truncated input | heap/direct range validation 선행 | `SnappyException`, native decompression 미실행 |
| Deflate invalid/truncated/dictionary input | loop 상태표와 cause 보존 | 충분한 target에서는 `ZipException`; dictionary와 zero-remaining target이 동시에 관찰되면 상태표 순서에 따라 `BufferOverflowException` |
| backend가 duplicate limit 변경 | 원본과 분리된 view에서 실행 | 원본 limit 보존 |
| backend failure 후 partial write | 원본 position rollback | bytes unspecified, 전체 재기록으로 retry |
| Deflate no-progress | 상태 분기와 progress assertion | bounded failure, 무한 loop 없음 |
| source/target 동일 객체 | identity preflight | `IllegalArgumentException` |
| 같은 array의 overlapping slice | range preflight | `IllegalArgumentException` |
| 탐지 불가능한 direct/read-only alias | 문서화된 caller precondition | 지원하지 않음; 문서에서 경고 |
| fallback을 저할당으로 오해 | KDoc/README/benchmark eligibility 표 | allocation claim 금지 |
| native/JDK resource cleanup 누락 | `finally`에서 `end`/close | 누수 없이 원래 failure 보존 |

## 15. acceptance criteria

1. `Compressor`가 두 caller-owned `ByteBuffer` 기본 메서드를 제공한다.
2. 동일 erased signature의 default 충돌이 없는 기존 외부 구현체와 Kotlin/Java caller의
   source/binary compatibility가 exact baseline fixture로 유지된다.
3. 모든 compressor가 공통 buffer correctness contract를 통과한다.
4. LZ4와 Deflate는 지원되는 heap/direct 조합에서 payload-sized intermediate
   `ByteArray` 없이 동작한다.
5. Snappy와 Zstd는 heap→heap 및 direct→direct에서 저할당 경로를 사용하고 나머지는
   명시적 fallback을 사용한다.
6. source state, target success commit, failure rollback, read-only 및 overflow 계약이
   direct test로 증명된다.
7. 기존 wire와 신규 API의 양방향 복원이 증명된다.
8. 기존 decompression 제한과 corrupt-input 방어가 신규 경로에도 적용된다.
9. allocation report가 codec/storage 조합별 eligible/ineligible 결과와 두 canonical
   run을 보존한다.
10. 영어/한국어 README와 KDoc이 지원 범위 및 제한을 정확히 설명한다.
11. canonical Kotlin/Java example과 bounded growth retry가 compile/contract test로 증명된다.
12. benchmark raw evidence의 commit/tree/JAR/environment identity와 append-only run이
    fail-closed validator로 증명된다.
13. fallback decompression의 target bound 한계가 압축률이 큰 payload/small-target contract
    test와 KDoc/양쪽 README에서 명시된다.

## 16. Definition of Done

- 설계와 구현 계획의 모든 acceptance criterion이 test/doc/benchmark task에 추적된다.
- targeted tests, 전체 `:bluetape4k-io:test`, compile, Detekt, ABI fixture,
  `git diff --check`가 성공한다.
- allocation evidence는 `gc.alloc.rate.norm`과 exact execution metadata를 포함한다.
- public API 및 문서가 fallback을 저할당으로 과장하지 않는다.
- 독립 spec/plan/code review의 최신 통합 결과가 P0=0, P1=0이다.
- PR exact head의 required CI와 review thread가 수렴한다.
- merge는 별도의 최신 사용자 승인을 받은 뒤에만 수행한다.

## 17. 독립 검토 수렴 기록

2026-07-21에 성능, 안정성/오류계약, 보안, 운영, developer/public API,
caller/user ergonomics의 여섯 관점을 서로 독립적으로 검토하고 main session에서 통합했다.

| 관점 | P0 | P1 | P2 | P3 | 최종 판정 |
|---|---:|---:|---:|---:|---|
| 성능 | 0 | 0 | 0 | 0 | PASS |
| 안정성/오류계약 | 0 | 0 | 0 | 0 | PASS |
| 보안 | 0 | 0 | 0 | 0 | PASS |
| 운영 | 0 | 0 | 0 | 0 | PASS |
| developer/public API | 0 | 0 | 0 | 0 | PASS |
| caller/user ergonomics | 0 | 0 | 0 | 0 | PASS |

검토 과정에서 발견된 header payload capacity, compound failure precedence, 외부 구현체
concurrency 및 default-method compatibility 경계, fallback decompression resource bound,
Zstd exception taxonomy, ABI 기준물, mutable benchmark state와 evidence schema 문제는 본문에
반영한 뒤 해당 관점에서 재검토했다. 구현 계획과 구현 review에서도 이 명세를 기준으로
P0=0, P1=0을 다시 확인한다.
