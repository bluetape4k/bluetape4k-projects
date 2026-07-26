# #756 Fory/FastFory raw codec allocation 후속 설계 명세

**상태:** 구현 계획 작성 전 검토

-

**이슈:** [#756 Reduce Redis codec allocation with ByteBuffer and ByteBuf paths](https://github.com/bluetape4k/bluetape4k-projects/issues/756)
- **마일스톤:** `1.12.0`
- **브랜치:** `feat/issue-756-fory-codec-followup`
- **기준 커밋:** `137d87cfeb6fe9dc45b727daf8c1e81e35a9babf`
- **유형:** Type A — 여러 모듈의 직렬화 경계, ownership 계약, 측정 증거를 함께 바꾸는 기능 작업
- **선행 작업:** PR #1072 (`Lettuce` JDK/Kryo/Jackson2/Jackson3 bounded `ByteBuf` writer)

## 1. 문제와 목표

PR #1072는 출력 스트림 경로가 검증된 JDK, Kryo, Jackson2, Jackson3에 한정하여 `Lettuce`의 payload-sized handoff `ByteArray`를 제거했다. 당시 Fory/FastFory는 직접 출력 계약의 안전성과 이득이 확인되지 않아, allocation 표와 chart에서 명시적으로 fallback 상태로 남겼다.

Fory 1.3.0의 `ThreadSafeFory`는 `serialize(OutputStream, Object)`, `serialize(MemoryBuffer, Object)`, `deserialize(ByteBuffer)`를 제공한다. 따라서 raw Fory/FastFory 경로는 Fory가 호출자 저장소를 교체할 수 있는 `MemoryBuffer` view가 아니라,
**bounded absolute-index `ByteBuf` writer와 단일 NIO 읽기 view**를 사용하면 복사 경계를 줄일 후보가 된다.

이 후속 slice의 목표는 다음과 같다.

1. raw Fory/FastFory의 warmed steady-state 성공 경로에서 반환·handoff용 payload-sized `ByteArray`를 없앨 수 있는 안전한 경계를 제공한다.
2. Fory/FastFory의 binary wire format, `FastFory`의 기존 비대칭 fallback, caller `ByteBuf` 상태와 Redisson ownership 계약을 보존한다.
3. 이득은 새 독립 two-run benchmark가 재현 가능한 allocation 증거로 확인한 셀에만 문서화한다.

Fory 1.3.0의 stream API도 내부 reusable heap `MemoryBuffer`에 먼저 직렬화한 뒤 `OutputStream.write(byte[], ...)`를 호출한다. 따라서 이는 전체 codec의 zero-copy, 내부 payload buffer 제거, throughput 개선 약속이 아니다. cold start와 내부 buffer growth는 별도 allocation으로 측정하며, direct path를 사용할 수 없는 buffer·예외·fallback은 기존 `ByteArray` 호환 경로를 그대로 사용한다.

## 2. 범위

### 포함

| 대상                                                  | 변경 방향                                                                                                                                                                |
|-------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `io/io` `ForyBinarySerializer`                        | Fory `OutputStream` API를 이용하는 handoff-array 제거 후보를 추가하고, 기존 `ByteArray` 경로와의 wire parity 및 writer 실패 계약을 검증한다.                             |
| `infra/lettuce` raw `fory()` / `fastFory()`           | 이미 검증된 `BoundedByteBufOutputStream` dispatch를 통해 직접 출력 후보가 되게 한다.                                                                                     |
| `infra/redisson` `ForyCodec` / `FastForyCodec` encode | owned output storage 때문에 allocation 이득이 불명확하므로 production 변경 전에 독립 feasibility gate를 수행한다. gate를 통과한 경우에만 bounded writer 후보를 구현한다. |
| `infra/redisson` `ForyCodec` / `FastForyCodec` decode | 단일 NIO buffer일 때만 읽기 전용 duplicate view로 direct decode를 시도하고, 그 외에는 현행 copied fallback을 유지한다.                                                   |
| 검증과 증거                                           | io, Lettuce, Redisson 계약 테스트와 Fory 전용 독립 two-run allocation evidence를 추가한다.                                                                               |
| 문서화                                                | 검증된 결과만 한국어/영어 README와 chart에 반영하며, 불확정·fallback 셀은 그대로 표시한다.                                                                               |

### 제외

| 제외 대상                                                                                                | 이유                                                                                            |
|----------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| GZip/LZ4/Zstd/Snappy 등 compression wrapper와 compressed Fory/FastFory codec                             | #755 compressor 작업과 lifecycle·buffer 교체 위험이 겹친다. 이 slice는 raw codec 경계만 다룬다. |
| Fory 의존성 버전, `ThreadSafeFory` lifecycle/ownership 정책, serialization mode 변경                     | format·운영 범위를 넓히고 별도 호환성 검토가 필요하다.                                          |
| Fory/FastFory wire format, `CompatibleMode`/`SCHEMA_CONSISTENT` 선택, Redisson fallback 정책의 의미 변경 | 기존 caller 호환 계약이다.                                                                      |
| 새 모듈·새 외부 의존성·범용 buffer abstraction                                                           | 좁은 성능 slice에 필요한 것보다 표면적이 크다.                                                  |
| 기존 #1072 JMH artifact 또는 16-cell validator 수정                                                      | Fory 전용 증거는 독립 artifact로 만들어 기존 증거의 의미를 바꾸지 않는다.                       |

## 3. 대안과 결정

| 대안                                                                                                            | 결정 | 근거                                                                                                                 |
|-----------------------------------------------------------------------------------------------------------------|------|----------------------------------------------------------------------------------------------------------------------|
| A. raw Fory/FastFory의 serializer·Lettuce·Redisson decode를 구현하고 Redisson encode는 feasibility gate 뒤 결정 | 채택 | #756의 실제 복사 경계를 검증하면서 owned output storage가 필요한 encode에 근거 없는 allocation 주장을 만들지 않는다. |
| B. `ForyBinarySerializer`와 Lettuce만 변경                                                                      | 기각 | Redisson decode 복사를 남기고 encode feasibility조차 검증하지 않는다.                                                |
| C. raw와 compression wrapper를 한 번에 변경                                                                     | 기각 | #755와 동일한 compressor lifecycle 문제를 섞어 rollback·원인 분리를 어렵게 만든다.                                   |
| D. writable NIO `ByteBuffer` view를 Fory 출력 대상으로 제공                                                     | 기각 | Netty buffer aliasing·growth·commit 계약이 불명확하며 #1072의 안전 경계를 약화한다.                                  |
| E. `MemoryBuffer`를 caller `ByteBuf` 위에 직접 구성                                                             | 기각 | Fory가 저장소를 대체하며 성장할 수 있어 caller writerIndex와 ownership을 안전하게 commit하기 어렵다.                 |

## 4. 설계

### 4.1 공통 원칙

`BinarySerializer`의 기존 ABI와 default implementation은 바꾸지 않는다. Fory가 stream 경로를 제공할 수 있다는 사실만 `ForyBinarySerializer` 내부 구현으로 노출한다. 모든 direct path는 성공 시에만 결과를 commit한다. 안전한 direct view가 불가능한 decode와 Redisson codec fallback은 기존 byte-array compatibility path로 돌아가며, Lettuce stream serialization 자체의 실패는 새 fallback을 만들지 않고 기존 encode 호출과 같은 실패로 전파한다.

stream 출력과 기존 `deserializeFrom(ByteBuffer)` 경로는 생성자에 주입된
**동일한** `ThreadSafeFory` 인스턴스만 사용한다. global/default serializer를 다시 조회하거나 Fory를 재구성하지 않으며, `CompatibleMode`, class-registration allowlist, ref tracking 등 주입된 보안·wire 설정을 바꾸지 않는다. writable `serializeTo(ByteBuffer)`는 이번 범위에서 직접화하지 않고 allocating compatibility fallback으로 유지한다.

직렬화 결과의 byte-for-byte parity는 입력 fixture별로 기존 `fory.serialize(graph)` 결과와 비교한다. decode 성공만으로는 wire parity를 증명하지 않는다.

### 4.2 `ForyBinarySerializer`의 bounded `OutputStream` 경로

실제 Lettuce 호출 seam인 `ForyBinarySerializer.serializeBinaryToStream(graph, target)`가 Fory의 `serialize(OutputStream, Object)`를 호출하는 후보가 된다. `io/io`에는 Netty 의존성을 추가하지 않는다. serializer-local counting/no-close/no-flush adapter가 generic caller `OutputStream`만 synchronous borrow하고 호출 뒤 retain하지 않으며, exact byte count와 destination failure provenance를 기록한다.

null은 기존 `serialize(null)`과 같이 0 bytes를 반환하고 target을 호출하지 않는다. backend failure는 기존 `serialize`와 같은 `BinarySerializationException` 계층·cause 의미를 유지하고, destination failure는 established direct-stream `BufferFailurePolicy`에 따라 identity/cause를 복원한다. cancellation과 `Error`는 JDK/Kryo direct stream과 같은 control-failure policy를 따른다. Fory가 내부적으로 호출하는 `flush()`는 adapter에서 흡수해 caller target으로 전달하지 않으며 `close()`도 전달하지 않는다.

이 override가 stream/byte-array exact wire, exact byte count, null, backend/destination/cancellation/fatal failure taxonomy를 모두 복원하지 못하면 direct capability를 제거하고 기존 allocating default를 유지한다.

### 4.3 Lettuce raw Fory/FastFory

`LettuceBinaryCodecs.fory()`와 `fastFory()`는 raw serializer가 `serializeBinaryToStream`을 제공하면 기존 `LettuceBinaryCodec`의 bounded writer dispatch를 사용한다. 다음은 변하지 않는다.

- `io/io` adapter는 byte count와 failure provenance만 담당하고, absolute-index write·snapshot·writerIndex commit은 `infra/lettuce`의 기존 `BoundedByteBufOutputStream`만 담당한다.
- compressed `fory`/`fastFory` factory는 `CompressableBinarySerializer`의 byte-array compatibility 경로를 계속 사용한다.
- heap/direct target의 writer-index rollback과 `ByteBuf` reader/writer index, mark, refCnt 관찰 가능 상태를 보존한다. target serialization 실패에는 새 byte-array fallback을 추가하지 않는다.
- Lettuce FastFory에는 새 fallback이나 cross-mode decode를 추가하지 않는다.

### 4.4 Redisson raw Fory/FastFory encode feasibility gate

Redisson encode는 caller-owned target이 아니라 새 owned `ByteBuf`를 반환해야 한다. Fory stream API도 내부 buffer에서 output으로 복사하므로, 기존 `serialize() -> wrappedBuffer(byte[])`를 `serialize(OutputStream) -> fresh ByteBuf`로 바꿔도 payload-sized storage allocation 자체는 남는다. 따라서 production code보다 feasibility benchmark와 ownership test를 먼저 수행한다.

candidate는 `Unpooled.buffer(256, Int.MAX_VALUE)`로 allocator·initial/max-capacity와 growth policy를 고정하고, baseline은 현행 `serialize() -> Unpooled.wrappedBuffer(bytes)` 그대로다. benchmark iteration마다 양쪽 반환 buffer를 exact-once release하고, candidate의 capacity growth 횟수·최종 capacity·heap/direct 종류를 raw evidence에 남긴다. 이 비교가 §7의 allocation·throughput gate를 통과하지 못하면 Redisson encode production 변경은 하지 않는다.

gate를 통과해 구현할 경우 Redisson은 Lettuce 모듈에 의존하지 않고 `infra/redisson` 내부에 fresh owned buffer 전용의 작은 bounded adapter를 둔다. codec 수준에서는
**현재 byte-array 경로의 예외 정규화와 fallback 관찰 동작을 그대로
유지**한다. 현재 `AbstractBinarySerializer`가 underlying cancellation/`Error`까지 `BinarySerializationException`으로 정규화한 뒤 `ForyCodec`의 `Exception` 또는 `FastForyCodec`의 `RuntimeException` catch domain이 fallback하는 동작도 이 slice에서 바꾸지 않는다. fatal handling 현대화는 별도 보안 변경이다.

실패 provenance는 다음처럼 분리한다.

- Fory backend semantic/control failure: current byte-array path와 같은 throwable normalization 뒤 partial buffer를 release하고 같은 primary를 재시도하지 않으며 기존 Kryo5/Fory fallback encoder를 한 번 호출한다.
- candidate-only setup/adapter/destination failure: partial buffer를 release하고 현행 same-mode byte-array path를 한 번 수행한다. 그 compatibility path의 기존 catch domain과 fallback 동작을 그대로 적용한다.
- cleanup failure: 기존 semantic failure가 있으면 suppress하고, 단독 cleanup failure는 전파한다.

codec fallback log는 기존 semantic failure에서만 기존 info/debug level과 비민감 내용을 유지한다. adapter failure 자체는 fallback log를 만들지 않는다.

candidate buffer는 성공해 ownership을 이전하기 전 caller에게 escape하지 않는다. 실패 시 written range zeroization은 현행 serializer buffer와 같은 정책으로 요구하지 않지만 readable range로 노출하지 않고 release한다. success-transfer flag와 `finally`를 사용해 candidate는 실패 시 exact-once release하고, 성공 반환 buffer는 재-release하지 않는다.

encode/decode fallback 자체가 실패하면 **fallback failure가 현행처럼 terminal exception identity/type/cause로
전파**된다. primary semantic failure는 기존 level로 log되지만 terminal failure를 대체하지 않는다. candidate cleanup failure는 terminal failure가 있으면 suppressed되고, 단독이면 전파된다.

### 4.5 Redisson raw Fory/FastFory decode

decode의 direct 후보는 `nioBufferCount() == 1`이고 정확한 readable range의 NIO view를 안전하게 만들 수 있는 경우뿐이다. `readerIndex`와 `readableBytes`에서 얻은 view는 synchronous borrowed read-only slice로 만들며 Fory가 호출 종료 뒤 retain하지 않는다. 이 lifetime이 증명되지 않으면 copied path를 사용한다. 원본 `ByteBuf`의 reader/writer index, marked index, refCnt를 변경하지 않고 prefix/suffix sentinel을 노출하지 않는다.

view 생성·precondition이 실패하면 `ByteBufUtil.getBytes(...)`로 copy한 뒤 같은 primary decoder를 **한
번** 호출하고, 그 실패에만 기존 fallback codec을 적용한다. direct primary decoder가 실제 호출된 뒤 실패하면 same primary를 copied bytes로 재시도하지 않고, current byte-array path와 같은 throwable normalization 뒤 fallback codec에 전달할 bytes만 한 번 copy한다. 이는 malformed payload의 중복 parsing을 막는다. control/fatal failure까지 기존 codec fallback 횟수와 exception/log 관찰 동작을 맞추지 못하면 Redisson direct decode candidate 전체를 제거한다.

`ForyCodec`은 현행 `Exception`, `FastForyCodec`은 현행 `RuntimeException` catch domain에서만 각각 Kryo5/Fory fallback decoder를 호출한다. direct Fory decode가 성공했다는 이유로 Fory가 FastFory payload를 읽는 방향의 호환성을 새로 만들지 않는다.

### 4.6 소유권과 rollback

| 상황                                    | 결과                                                                                                        |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Lettuce direct serialize 성공           | target writerIndex만 실제 payload 길이만큼 전진한다.                                                        |
| Lettuce direct serialize 실패           | target writerIndex는 시작 값이며, 기존 encode 호출과 동일하게 실패를 전파한다.                              |
| Redisson direct encode 성공             | 반환한 fresh buffer의 ownership은 현행 encode 반환값과 동일하게 caller에게 있다.                            |
| Redisson direct encode backend 실패     | candidate buffer를 release하고 same-mode 재시도 없이 기존 codec fallback 규칙을 적용한다.                   |
| Redisson direct encode destination 실패 | candidate buffer를 release하고 현행 same-mode byte-array encode 1회 뒤 기존 codec fallback 규칙을 적용한다. |
| Redisson direct decode 성공/실패        | 입력 buffer의 index, marks, refCnt는 관찰 가능한 변경이 없다.                                               |
| direct path 불가                        | allocation 최적화 주장 없이 현행 byte-array compatibility path를 사용한다.                                  |

이 작업에는 feature flag나 per-call dispatch telemetry가 없다. hot path에 새 log/metric을 추가하지 않고 기존 codec fallback 로그의 level·횟수·비민감 정보 계약만 유지한다. 운영 확인 수단은 artifact version/hash와 committed benchmark evidence다.

출시 전 wire parity, ownership, exception taxonomy, allocation 또는 throughput gate가 실패하면 해당 direct candidate를 revert한다. 출시 전 rollback은 candidate commit 제거다. 출시 후 rollback은 release checklist가 미리 고정한 `io.github.bluetape4k:bluetape4k-io`, `io.github.bluetape4k:bluetape4k-lettuce`, `io.github.bluetape4k:bluetape4k-redisson`의 exact known-good version과 JAR SHA-256으로 복귀하는 것이다. release executor가 rollback owner이며, 이전↔신규 artifact 교차 decode fixture와 rollback 후 Redis smoke test를 재실행한다.

## 5. 호환성 계약

1. 기존 Fory payload는 Fory mode에서 계속 decode된다.
2. FastFory는 `SCHEMA_CONSISTENT` mode를 유지한다.
3. Redisson의 기존 비대칭성은 유지한다. FastFory decoder는 Fory payload를 fallback으로 읽을 수 있지만, Fory decoder가 FastFory payload를 읽을 수 있게 만들지 않는다.
4. fallback을 예외가 아닌 정상 fast path로 승격하지 않는다. raw direct 후보의 실패 조건은 기존 fallback 의미를 넓히지 않는다.
5. public factory 이름, serializer ABI, artifact/module 좌표, dependency version은 변경하지 않는다.
6. 안전하게 direct view를 만들 수 없는 입력은 copied compatibility route를 선택한다.
7. stream output/direct decode path는 주입된 동일 `ThreadSafeFory`의 mode·registration·reference 정책을 유지하며 writable `serializeTo(ByteBuffer)`는 allocating fallback이다.
8. Redisson의 기본 Fory codec은 신뢰된 Redis payload용 기존 보안 모델을 유지하며, 이 작업이 secure deserialization 설정을 새로 제공한다고 문서화하지 않는다.

## 6. 테스트 전략

### 6.1 `io/io`

- Fory/FastFory 각각에서 stream emission과 `byte[]` emission의 byte-for-byte parity를 고정한다.
- null, 정상 graph, nested graph, Fory 예외, target overflow를 검증한다.
- Fory 내부 `flush()`/`close()`가 caller `OutputStream`으로 전달되지 않는지 검증한다.
- generic stream의 exact count, no-flush/no-close, failure provenance를 검증한다.
- `secureFory`의 등록 타입 성공과 미등록 타입 거부를 byte-array, stream, direct `ByteBuffer`에서 같은 예외 taxonomy로 검증한다.
- backend, destination, cancellation, fatal failure의 identity/cause를 JDK/Kryo의 established direct-stream policy와 비교한다.

### 6.2 `infra/lettuce`

- raw `fory()`/`fastFory()`를 heap/direct target에서 round trip과 byte parity로 검증한다.
- hostile target (여유 공간 부족, non-expandable 범위 등)에서 writerIndex rollback과 기존 예외 전파를 검증한다.
- `BoundedByteBufOutputStream`의 absolute-index write, snapshot, commit-on-success와 failure no-commit을 검증한다.
- compressed Fory/FastFory가 이 변경의 direct candidate가 아님을 regression test/KDoc scope로 고정한다.
- 기존 `FastForyCompatibilityTest` 및 binary codec buffer contract를 유지·확장한다.

### 6.3 `infra/redisson`

- encode feasibility gate를 통과한 경우에만 Fory/FastFory direct encode 결과와 기존 byte-array 결과의 payload parity 및 round trip을 검증한다.
- single-NIO heap/direct input의 direct decode와 composite/non-NIO input의 fallback decode를 분리 검증한다.
- readerIndex, writerIndex, marked reader/writer index, refCnt가 decode 전후 유지되는지 검증한다.
- direct encode 예외에서 candidate buffer가 release되고 기존 Kryo5/Fory fallback 및 기존 예외 semantics가 유지되는지 검증한다.
- direct encode feasibility candidate의 success-transfer/exact-once release를 direct failure + baseline success, direct failure + baseline/fallback failure, fatal failure로 나눠 검증한다.
- view precondition failure와 direct primary failure를 분리해 primary/fallback 호출 횟수, catch domain, log 횟수, bounded-range sentinel 비노출을 검증한다.
- injected fallback failure로 Fory/FastFory encode/decode 각각 fallback terminal identity/type/cause, primary log count, candidate exact-once release를 검증한다.
- public constructor/ABI는 유지하되 internal serializer factory와 output-buffer factory seam으로 semantic/destination/control/fatal failure와 exact interaction을 결정적으로 주입한다. copy-constructor는 같은 runtime 설정을 보존한다.
- touched test는 `runCatching`/non-null smoke assertion 대신 bluetape4k assertions, `assertFailsWith`, exact interaction/refCnt assertion을 사용한다.
- `FastForyCompatibilityTest`의 비대칭 mode contract를 회귀 검증한다.

Testcontainers가 필요한 Redis integration path는 모듈·worktree 간 병렬 실행하지 않는다.

## 7. Benchmark와 evidence

기존 #1072 evidence runner/16-cell validator는 수정하지 않는다. 이 slice는 두 모듈의 독립 benchmark source set을 유지한다. `infra/lettuce`에는 고정 4 pair/8 method JAR·runner, `infra/redisson`에는 아래 disposition에 따라 6 pair/12 method 또는 8 pair/16 method JAR·runner를 만든다. repository-level `issue-756-fory-codec-followup` aggregate manifest/validator가 두 artifact를 묶어 검증하며 benchmark-only cross-module dependency나 새 module은 추가하지 않는다.

Redisson encode gate는 두 단계다.

1. **Non-promotable feasibility
   probe:** benchmark-local candidate로 현행 encode 대비 방향성과 ownership/capacity를 canonical profile과 동일한 독립 2회 (`probe-a`, `probe-b`) 측정으로 확인한다. 이 수치는 README/chart claim에 사용할 수 없다.
2. probe가 유망할 때만 production codec path를 구현한다. 구현 뒤 이전 probe를 폐기하고 실제 production path로 fresh canonical A/B를 실행한다. documentation 승격은 이 canonical 결과만 사용한다.

probe는 두 run 모두 preflight/wire/ownership/release 검증을 통과하고, allocation point reduction이 5% 이상이며 `candidate score + scoreError < baseline score - scoreError`이고, throughput delta가 `>-20%`일 때만 `implemented`로 진행한다. 누락·NaN·interval overlap·한 run 기준 실패·capacity/refCnt drift·throughput `<=-20%` 중 하나라도 있으면 `rejected`다.

aggregate manifest는 `encodeDisposition=rejected|implemented`를 고정한다. `rejected`이면 canonical matrix는 encode를 제외한 정확히 10 pair/20 methods이고 feasibility raw evidence만 별도 첨부한다. `implemented`이면 actual codec encode를 포함한 정확히 12 pair/24 methods다. validator는 disposition과 cardinality가 맞지 않으면 실패한다.

측정 후보와 conditional canonical cardinality는 다음과 같다.

| Pair 범위                | Mode           | Buffer shape                                                           | pair 수 | 승격 가능 여부                                                           |
|--------------------------|----------------|------------------------------------------------------------------------|--------:|--------------------------------------------------------------------------|
| Lettuce serialize        | Fory, FastFory | heap target, direct target                                             |       4 | 가능                                                                     |
| Redisson decode          | Fory, FastFory | single-NIO heap, single-NIO direct                                     |       4 | 가능                                                                     |
| Redisson decode fallback | Fory, FastFory | composite                                                              |       2 | 불가 — fallback overhead 확인 전용                                       |
| Redisson encode          | Fory, FastFory | 현행 wrapped byte array 대 fresh `Unpooled.buffer(256, Int.MAX_VALUE)` |       2 | probe는 승격 불가, implemented disposition의 production path만 승격 가능 |

각 pair는 baseline byte-array route와 candidate route를 같은 fixture·payload·동일 process 환경에서 일대일 비교한다. cold/internal-buffer-growth probe는 timed acceptance matrix 밖에서 별도 수행하고, canonical matrix는 충분히 warmed 상태에서 측정한다. primary metric은 `gc.alloc.rate.norm` (bytes/op)이며 throughput은 diagnostic metric이다.

probe와 canonical profile은 `1 thread`, `2 forks`, `3 x 1s warmup`, `5 x 1s measurement`, throughput `ops/ms`, GC profiler, `-Xms1g -Xmx1g -XX:+UseG1GC`로 고정한다. payload는 기존 `Issue756BenchmarkData(id=756L, name="lettuce-buffer-codec", description="A".repeat(96))` 하나를 두 모듈이 공유하며 source manifest에 payload SHA-256을 고정한다. raw authority 경로는 `docs/benchmarks/raw/issue-756-fory-followup/{feasibility,lettuce,redisson}/{probe-a,probe-b,canonical-a,canonical-b}` 중 단계에 맞는 leaf이고 aggregate manifest/report는 `docs/benchmarks/raw/issue-756-fory-followup/`에 둔다.

실행 전 fail-closed preflight는 다음을 모두 확인한다.

- disposition별 module-local exact 8+12 또는 8+16 method set와 aggregate 20/24-method pair mapping
- serialize baseline의 `serialize()` 1회/candidate의 `serializeBinaryToStream()` 1회 및 반대 path 0회
- decode byte-array baseline의 `deserialize(byte[])` 1회; single-NIO candidate의 `deserializeFrom(ByteBuffer)` 1회와 array path 0회; composite candidate의 copied array path 1회와 direct path 0회
- fallback은 선언된 primary failure 뒤에만 1회 호출되며 이 dispatch identity를 exact timed method와 binding
- exact wire/count parity, 동일 주입 Fory identity/mode/registration, fixture/payload hash
- Lettuce target allocator와 heap/direct 실제 class, Redisson allocator·initial/max capacity·growth·release identity
- decode input index/marks/refCnt 및 sentinel 불변, composite fallback의 non-promotable 분류
- clean working tree, exact commit/tree, benchmark JAR/classpath/source/executable hash, JDK/Gradle/JMH argv

각 module-local canonical run A/B의 raw JMH JSON은 append-only authority이며 aggregate derived table은 이를 재생성한 결과다. 생성 owner와 독립 validator owner를 분리한다. 어느 한쪽 benchmark source, fixture, allocator 설정, JVM/JMH argv, executable hash 또는 timed production path가 바뀌면 두 모듈의 두 run을 전부 무효화하고 다시 측정한다. measurement SHA에서 delivery SHA까지 허용되는 변경은 docs/chart/validation artifact뿐이며 validator가 ancestry와 changed-path allowlist를 확인한다.

candidate를 README/chart의
**accepted** 셀로 승격하려면 두 canonical run 모두에서 allocation point reduction이 5% 이상이고 `candidate score + scoreError < baseline score - scoreError`이며 throughput delta가 `>-20%`여야 한다. 누락·NaN·interval overlap·한 run 기준 실패·parity 실패·fallback-only 셀은 `rejected`, `fallback`, 또는 `inconclusive`로 남기며 수치를 일반화하지 않는다.

## 8. 문서와 chart 원칙

코드·테스트·evidence가 승인된 뒤에만 README와 chart를 갱신한다.

- `ForyBinarySerializer`, `LettuceBinaryCodecs.fory()/fastFory()`, Redisson `ForyCodec`/`FastForyCodec` KDoc를 실제 accepted 경로와 동기화한다. Redisson KDoc에는 등록 없는 기본 Fory (`requireClassRegistration(false)`)를 사용하므로 trusted Redis payload 전용이며 untrusted input의 secure deserialization 경계를 제공하지 않는다고 경고한다.
- KDoc는 raw-only 범위, Fory 내부 buffer/copy가 남아 zero-copy가 아님, caller migration이 필요 없음, gate 탈락 후보는 fallback/inconclusive임을 명시한다.
- Lettuce의 FastFory 무-fallback과 Redisson의 FastFory→Fory 비대칭 fallback을 transport별로 분리해 설명하며 서로의 동작으로 일반화하지 않는다.
- 한국어/영어 문서는 같은 codec matrix와 같은 수치·caveat를 유지한다.
- Fory/FastFory는 검증 전에는 allocation comparison에 없는 이유와 fallback 상태를 명시한다.
- accepted 셀만 새 chart에 표시한다. 빠진 셀을 0, 동일, zero-copy로 추정하지 않는다.
- chart source와 PNG/SVG output, renderer 명령, rendered inspection은 함께 version control 한다.
- compression wrapper는 별도 #755 slice임을 명시해 raw codec 결과를 compressor 결과로 오해하지 않게 한다.
- feature flag와 runtime direct-dispatch telemetry가 없으며 version/hash와 committed evidence가 운영 확인 수단임을 명시한다.

## 9. 구현 순서와 완료 기준

1. Fory generic stream parity와 failure taxonomy를 `io/io`에서 먼저 고정한다.
2. raw Lettuce Fory/FastFory를 기존 bounded dispatch에 연결하고 target-contract tests를 통과시킨다.
3. Redisson safe decode view와 fallback-state tests를 추가한다.
4. Redisson encode feasibility preflight를 실행하고 gate 통과 시에만 internal bounded writer와 ownership/fallback tests를 추가한다.
5. 영향을 받은 모듈 test, detekt, diff check를 순차 실행한다.
6. 독립 two-run benchmark artifact를 생성·검증한다.
7. accepted evidence만 README/chart에 반영하고 diagram/document validation을 실행한다.

다음 모두가 충족되어야 구현 완료다.

- [ ] Fory/FastFory raw payload byte parity와 mode compatibility가 기존 fixture에서 유지된다.
- [ ] direct 후보의 실패가 caller writerIndex·input buffer state·Redisson ownership을 손상시키지 않는다.
- [ ] composite/non-NIO와 direct decode 실패가 copied compatibility path로 안전하게 돌아간다.
- [ ] FastFory→Fory fallback 비대칭성과 Fory→FastFory 비호환성이 그대로다.
- [ ] 새 benchmark artifact가 두 canonical run, hash binding, validator를 포함한다.
- [ ] serializer/Lettuce/Redisson KDoc와 한국어/영어 README/chart가 accepted evidence, transport별 fallback, raw-only 범위를 동일하게 반영한다.
- [ ] P0/P1 없는 spec/plan review, targeted tests, module tests, relevant detekt, `git diff --check`를 모두 통과한다.
- [ ] 이전↔신규 artifact 교차 decode와 rollback 후 Redis smoke 절차가 release evidence에 포함된다.
- [ ] #756은 모든 in-scope candidate가 `accepted`, 문서화된 `inconclusive`, 또는 문서화된 `rejected` 중 하나의 terminal disposition을 갖고 code/KDoc/README/chart와 일치할 때 close한다. #755 compression 작업은 별도 issue 상태로 추적한다.

## 10. 열린 구현 확인 항목

다음은 설계의 미결정이 아니라 구현 전 반드시 증명할 가설이다. 하나라도 성립하지 않으면 direct candidate를 축소하거나 제거한다.

1. Fory 1.3.0 `serialize(OutputStream, Object)`가 `byte[] serialize(Object)`와 fixture별 exact payload parity를 제공하는가?
2. Fory가 stream을 flush/close하거나 partial output을 남길 때 bounded writer의 no-commit contract가 유지되는가?
3. Redisson의 current allocator로 만든 fresh buffer에서 direct stream write가 fallback/exception 시 leak 없이 폐기되는가?
4. Fory `deserialize(ByteBuffer)`가 read-only duplicate NIO view를 소비하면서 original `ByteBuf` state를 바꾸지 않는가?
5. direct path의 allocation 이득이 two-run acceptance threshold를 충족하는가?

이 항목은 구현 계획에서 명시적인 task와 test/benchmark evidence로 전부 닫는다.
