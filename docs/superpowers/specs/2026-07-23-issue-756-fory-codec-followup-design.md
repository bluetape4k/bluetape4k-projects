# #756 Fory/FastFory raw codec allocation 후속 설계 명세

**상태:** 구현 계획 작성 전 검토

**이슈:** [#756 Reduce Redis codec allocation with ByteBuffer and ByteBuf paths](https://github.com/bluetape4k/bluetape4k-projects/issues/756)  
**마일스톤:** `1.12.0`  
**브랜치:** `feat/issue-756-fory-codec-followup`  
**기준 커밋:** `137d87cfeb6fe9dc45b727daf8c1e81e35a9babf`  
**유형:** Type A — 여러 모듈의 직렬화 경계, ownership 계약, 측정 증거를 함께 바꾸는 기능 작업  
**선행 작업:** PR #1072 (`Lettuce` JDK/Kryo/Jackson2/Jackson3 bounded `ByteBuf` writer)

## 1. 문제와 목표

PR #1072는 출력 스트림 경로가 검증된 JDK, Kryo, Jackson2, Jackson3에 한정하여 `Lettuce`의 payload-sized handoff `ByteArray`를 제거했다. 당시 Fory/FastFory는 직접 출력 계약의 안전성과 이득이 확인되지 않아, allocation 표와 chart에서 명시적으로 fallback 상태로 남겼다.

Fory 1.3.0의 `ThreadSafeFory`는 `serialize(OutputStream, Object)`, `serialize(MemoryBuffer, Object)`, `deserialize(ByteBuffer)`를 제공한다. 따라서 raw Fory/FastFory 경로는 Fory가 호출자 저장소를 교체할 수 있는 `MemoryBuffer` view가 아니라, **bounded absolute-index `ByteBuf` writer와 단일 NIO 읽기 view**를 사용하면 복사 경계를 줄일 후보가 된다.

이 후속 slice의 목표는 다음과 같다.

1. raw Fory/FastFory의 성공 경로에서만 payload-sized 중간 `ByteArray`를 없앨 수 있는 안전한 경계를 제공한다.
2. Fory/FastFory의 binary wire format, `FastFory`의 기존 비대칭 fallback, caller `ByteBuf` 상태와 Redisson ownership 계약을 보존한다.
3. 이득은 새 독립 two-run benchmark가 재현 가능한 allocation 증거로 확인한 셀에만 문서화한다.

이는 전체 codec의 무조건적 zero-copy 보장이나 throughput 개선 약속이 아니다. direct path를 사용할 수 없는 buffer·예외·fallback은 기존 `ByteArray` 호환 경로를 그대로 사용한다.

## 2. 범위

### 포함

| 대상 | 변경 방향 |
|---|---|
| `io/io` `ForyBinarySerializer` | Fory `OutputStream` API를 이용하는 직접 출력 후보를 추가하고, 기존 `ByteArray` 경로와의 wire parity 및 writer 실패 계약을 검증한다. |
| `infra/lettuce` raw `fory()` / `fastFory()` | 이미 검증된 `BoundedByteBufOutputStream` dispatch를 통해 직접 출력 후보가 되게 한다. |
| `infra/redisson` `ForyCodec` / `FastForyCodec` encode | 새로 생성한 owned `ByteBuf`에 bounded writer를 사용한 direct encode 후보를 추가한다. |
| `infra/redisson` `ForyCodec` / `FastForyCodec` decode | 단일 NIO buffer일 때만 읽기 전용 duplicate view로 direct decode를 시도하고, 그 외에는 현행 copied fallback을 유지한다. |
| 검증과 증거 | io, Lettuce, Redisson 계약 테스트와 Fory 전용 독립 two-run allocation evidence를 추가한다. |
| 문서화 | 검증된 결과만 한국어/영어 README와 chart에 반영하며, 불확정·fallback 셀은 그대로 표시한다. |

### 제외

| 제외 대상 | 이유 |
|---|---|
| GZip/LZ4/Zstd/Snappy 등 compression wrapper와 compressed Fory/FastFory codec | #755 compressor 작업과 lifecycle·buffer 교체 위험이 겹친다. 이 slice는 raw codec 경계만 다룬다. |
| Fory 의존성 버전, `ThreadSafeFory` lifecycle/ownership 정책, serialization mode 변경 | format·운영 범위를 넓히고 별도 호환성 검토가 필요하다. |
| Fory/FastFory wire format, `CompatibleMode`/`SCHEMA_CONSISTENT` 선택, Redisson fallback 정책의 의미 변경 | 기존 caller 호환 계약이다. |
| 새 모듈·새 외부 의존성·범용 buffer abstraction | 좁은 성능 slice에 필요한 것보다 표면적이 크다. |
| 기존 #1072 JMH artifact 또는 16-cell validator 수정 | Fory 전용 증거는 독립 artifact로 만들어 기존 증거의 의미를 바꾸지 않는다. |

## 3. 대안과 결정

| 대안 | 결정 | 근거 |
|---|---|---|
| A. raw Fory/FastFory를 serializer·Lettuce·Redisson 경계까지 함께 최적화 | 채택 | #756이 지목한 Fory/FastFory의 실제 복사 경계를 끝까지 검증할 수 있다. |
| B. `ForyBinarySerializer`와 Lettuce만 변경 | 기각 | Redisson `ForyCodec`/`FastForyCodec`의 encode/decode `ByteArray` 복사를 그대로 남긴다. |
| C. raw와 compression wrapper를 한 번에 변경 | 기각 | #755와 동일한 compressor lifecycle 문제를 섞어 rollback·원인 분리를 어렵게 만든다. |
| D. writable NIO `ByteBuffer` view를 Fory 출력 대상으로 제공 | 기각 | Netty buffer aliasing·growth·commit 계약이 불명확하며 #1072의 안전 경계를 약화한다. |
| E. `MemoryBuffer`를 caller `ByteBuf` 위에 직접 구성 | 기각 | Fory가 저장소를 대체하며 성장할 수 있어 caller writerIndex와 ownership을 안전하게 commit하기 어렵다. |

## 4. 설계

### 4.1 공통 원칙

`BinarySerializer`의 기존 ABI와 default implementation은 바꾸지 않는다. Fory가 직접 경로를 제공할 수 있다는 사실만 `ForyBinarySerializer` 내부 구현으로 노출한다. 모든 direct path는 성공 시에만 결과를 commit한다. 안전한 direct view가 불가능한 decode와 Redisson codec fallback은 기존 byte-array compatibility path로 돌아가며, Lettuce stream serialization 자체의 실패는 새 fallback을 만들지 않고 기존 encode 호출과 같은 실패로 전파한다.

직렬화 결과의 byte-for-byte parity는 입력 fixture별로 기존 `fory.serialize(graph)` 결과와 비교한다. decode 성공만으로는 wire parity를 증명하지 않는다.

### 4.2 `ForyBinarySerializer`의 bounded `OutputStream` 경로

`ForyBinarySerializer.serializeTo(graph, target)`는 Fory의 `serialize(OutputStream, Object)`를 호출하는 후보가 된다. target은 다음 성질을 가진 writer여야 한다.

- 시작 writerIndex와 writable bound를 고정한 absolute-index write만 수행한다.
- Fory가 `close()` 또는 `flush()`를 호출하더라도 adapter가 caller `ByteBuf`를 release하거나 상태를 commit하지 않는다.
- overflow, Fory 예외, partial write에서는 caller writerIndex를 바꾸지 않는다.
- 성공할 때만 실제 작성 byte 수만큼 writerIndex를 한 번 commit한다.
- 이미 검증된 `Lettuce` `BoundedByteBufOutputStream`은 그대로 재사용한다.

이 override가 Fory의 실제 stream emission과 byte-array emission의 parity, 예외, null, writer lifecycle을 만족하지 못하면 direct capability를 주장하지 않고 기존 구현을 유지한다.

### 4.3 Lettuce raw Fory/FastFory

`LettuceBinaryCodecs.fory()`와 `fastFory()`는 raw serializer가 `serializeTo`를 제공하면 기존 `LettuceBinaryCodec`의 bounded writer dispatch를 사용한다. 다음은 변하지 않는다.

- compressed `fory`/`fastFory` factory는 `CompressableBinarySerializer`의 byte-array compatibility 경로를 계속 사용한다.
- heap/direct target의 writer-index rollback과 `ByteBuf` reader/writer index, mark, refCnt 관찰 가능 상태를 보존한다. target serialization 실패에는 새 byte-array fallback을 추가하지 않는다.
- Lettuce FastFory에는 새 fallback이나 cross-mode decode를 추가하지 않는다.

### 4.4 Redisson raw Fory/FastFory encode

Redisson은 Lettuce 모듈에 의존하지 않는다. 따라서 `infra/redisson` 내부에 fresh owned output buffer 전용의 작은 bounded `OutputStream` adapter를 둔다. 이는 재사용 가능한 공용 abstraction으로 승격하지 않는다.

encode는 direct stream path를 먼저 시도한다. 성공한 경우 정확히 written range만 readable 한 새 `ByteBuf`를 반환한다. direct stream path가 실패하면 partial buffer를 release하고, **기존과 같은** Fory `byte[]` encode 또는 codec-specific fallback encoder를 수행한다. fallback이 새 buffer를 반환할 때까지 direct candidate buffer의 ownership을 caller에게 넘기지 않는다.

`ForyCodec`의 Kryo5 fallback과 `FastForyCodec`의 Fory fallback의 순서·조건·예외 의미는 변경하지 않는다.

### 4.5 Redisson raw Fory/FastFory decode

decode의 direct 후보는 `nioBufferCount() == 1`인 readable 범위뿐이다. `readerIndex`와 `readableBytes`에서 얻은 NIO view는 read-only duplicate로 만들어 Fory에 넘긴다. 원본 `ByteBuf`의 reader/writer index, marked index, refCnt를 변경하지 않는다.

composite/non-NIO buffer, direct decode 예외, view 생성 불가는 현행 `ByteBufUtil.getBytes(...)` 기반 compatibility path로 이동한다. FastFory는 direct FastFory decode 실패 뒤에도 기존과 같이 copied bytes를 Fory fallback decoder에 전달한다. direct Fory decode가 성공했다는 이유로 Fory가 FastFory payload를 읽는 방향의 호환성을 새로 만들지 않는다.

### 4.6 소유권과 rollback

| 상황 | 결과 |
|---|---|
| Lettuce direct serialize 성공 | target writerIndex만 실제 payload 길이만큼 전진한다. |
| Lettuce direct serialize 실패 | target writerIndex는 시작 값이며, 기존 encode 호출과 동일하게 실패를 전파한다. |
| Redisson direct encode 성공 | 반환한 fresh buffer의 ownership은 현행 encode 반환값과 동일하게 caller에게 있다. |
| Redisson direct encode 실패 | candidate buffer를 release하고 fallback 결과만 반환한다. |
| Redisson direct decode 성공/실패 | 입력 buffer의 index, marks, refCnt는 관찰 가능한 변경이 없다. |
| direct path 불가 | allocation 최적화 주장 없이 현행 byte-array compatibility path를 사용한다. |

이 작업에는 feature flag나 migration이 없다. rollback은 이전 artifact로 되돌리는 것이며, 문제 발생 시 direct candidate를 제거해 원래 byte-array path만 남긴다.

## 5. 호환성 계약

1. 기존 Fory payload는 Fory mode에서 계속 decode된다.
2. FastFory는 `SCHEMA_CONSISTENT` mode를 유지한다.
3. Redisson의 기존 비대칭성은 유지한다. FastFory decoder는 Fory payload를 fallback으로 읽을 수 있지만, Fory decoder가 FastFory payload를 읽을 수 있게 만들지 않는다.
4. fallback을 예외가 아닌 정상 fast path로 승격하지 않는다. raw direct 후보의 실패 조건은 기존 fallback 의미를 넓히지 않는다.
5. public factory 이름, serializer ABI, artifact/module 좌표, dependency version은 변경하지 않는다.
6. 안전하게 direct view를 만들 수 없는 입력은 copied compatibility route를 선택한다.

## 6. 테스트 전략

### 6.1 `io/io`

- Fory/FastFory 각각에서 stream emission과 `byte[]` emission의 byte-for-byte parity를 고정한다.
- null, 정상 graph, nested graph, Fory 예외, target overflow를 검증한다.
- `close()`/`flush()`가 underlying `ByteBuf` lifecycle을 바꾸지 않는지 검증한다.
- 실패한 stream write가 caller writerIndex를 commit하지 않는지 검증한다.

### 6.2 `infra/lettuce`

- raw `fory()`/`fastFory()`를 heap/direct target에서 round trip과 byte parity로 검증한다.
- hostile target(여유 공간 부족, non-expandable 범위 등)에서 writerIndex rollback과 기존 예외 전파를 검증한다.
- compressed Fory/FastFory가 이 변경의 direct candidate가 아님을 regression test/KDoc scope로 고정한다.
- 기존 `FastForyCompatibilityTest` 및 binary codec buffer contract를 유지·확장한다.

### 6.3 `infra/redisson`

- Fory/FastFory 각각의 direct encode 결과와 기존 byte-array 결과의 payload parity 및 round trip을 검증한다.
- single-NIO heap/direct input의 direct decode와 composite/non-NIO input의 fallback decode를 분리 검증한다.
- readerIndex, writerIndex, marked reader/writer index, refCnt가 decode 전후 유지되는지 검증한다.
- direct encode 예외에서 candidate buffer가 release되고 기존 Kryo5/Fory fallback 및 기존 예외 semantics가 유지되는지 검증한다.
- `FastForyCompatibilityTest`의 비대칭 mode contract를 회귀 검증한다.

Testcontainers가 필요한 Redis integration path는 모듈·worktree 간 병렬 실행하지 않는다.

## 7. Benchmark와 evidence

기존 #1072 evidence runner/16-cell validator는 수정하지 않는다. 이 slice는 `issue-756-fory-codec-followup` 전용 source manifest, runner, raw data, validation report를 추가한다.

측정 대상은 raw Fory/FastFory의 실제 transport 경계이며, 최소한 다음을 분리한다.

| 축 | 셀 |
|---|---|
| Serializer mode | Fory, FastFory |
| Integration | Lettuce direct-target serialize, Redisson owned-buffer encode, Redisson single-NIO decode |
| Input/buffer shape | Lettuce heap/direct target; Redisson single-NIO heap/direct와 composite fallback |
| Run | canonical run A, canonical run B |

각 셀은 baseline byte-array route와 candidate route를 같은 fixture·동일 환경에서 비교한다. primary metric은 `gc.alloc.rate.norm` (bytes/op)이며 throughput은 diagnostic metric이다. source hash, executable hash, JDK/Gradle/JMH 환경, raw result, derived table, validator 결과를 모두 artifact에 고정한다.

candidate를 README/chart의 **accepted** 셀로 승격하려면 두 canonical run 모두에서 allocation 감소가 5% 이상이고, throughput이 baseline 대비 20%보다 크게 악화되지 않아야 한다. 측정 불안정·parity 실패·fallback-only 셀은 `fallback` 또는 `inconclusive`로 남기며 수치를 일반화하지 않는다.

## 8. 문서와 chart 원칙

코드·테스트·evidence가 승인된 뒤에만 README와 chart를 갱신한다.

- 한국어/영어 문서는 같은 codec matrix와 같은 수치·caveat를 유지한다.
- Fory/FastFory는 검증 전에는 allocation comparison에 없는 이유와 fallback 상태를 명시한다.
- accepted 셀만 새 chart에 표시한다. 빠진 셀을 0, 동일, zero-copy로 추정하지 않는다.
- chart source와 PNG/SVG output, renderer 명령, rendered inspection은 함께 version control 한다.
- compression wrapper는 별도 #755 slice임을 명시해 raw codec 결과를 compressor 결과로 오해하지 않게 한다.

## 9. 구현 순서와 완료 기준

1. Fory stream parity와 bounded writer failure contract를 `io/io`에서 먼저 고정한다.
2. raw Lettuce Fory/FastFory를 기존 bounded dispatch에 연결하고 target-contract tests를 통과시킨다.
3. Redisson internal bounded writer와 safe decode view를 추가하고 ownership/fallback tests를 통과시킨다.
4. 영향을 받은 모듈 test, detekt, diff check를 순차 실행한다.
5. 독립 two-run benchmark artifact를 생성·검증한다.
6. accepted evidence만 README/chart에 반영하고 diagram/document validation을 실행한다.

다음 모두가 충족되어야 구현 완료다.

- [ ] Fory/FastFory raw payload byte parity와 mode compatibility가 기존 fixture에서 유지된다.
- [ ] direct 후보의 실패가 caller writerIndex·input buffer state·Redisson ownership을 손상시키지 않는다.
- [ ] composite/non-NIO와 direct decode 실패가 copied compatibility path로 안전하게 돌아간다.
- [ ] FastFory→Fory fallback 비대칭성과 Fory→FastFory 비호환성이 그대로다.
- [ ] 새 benchmark artifact가 두 canonical run, hash binding, validator를 포함한다.
- [ ] README/chart은 accepted evidence만 반영하며 compression 결과와 혼동되지 않는다.
- [ ] P0/P1 없는 spec/plan review, targeted tests, module tests, relevant detekt, `git diff --check`를 모두 통과한다.

## 10. 열린 구현 확인 항목

다음은 설계의 미결정이 아니라 구현 전 반드시 증명할 가설이다. 하나라도 성립하지 않으면 direct candidate를 축소하거나 제거한다.

1. Fory 1.3.0 `serialize(OutputStream, Object)`가 `byte[] serialize(Object)`와 fixture별 exact payload parity를 제공하는가?
2. Fory가 stream을 flush/close하거나 partial output을 남길 때 bounded writer의 no-commit contract가 유지되는가?
3. Redisson의 current allocator로 만든 fresh buffer에서 direct stream write가 fallback/exception 시 leak 없이 폐기되는가?
4. Fory `deserialize(ByteBuffer)`가 read-only duplicate NIO view를 소비하면서 original `ByteBuf` state를 바꾸지 않는가?
5. direct path의 allocation 이득이 two-run acceptance threshold를 충족하는가?

이 항목은 구현 계획에서 명시적인 task와 test/benchmark evidence로 전부 닫는다.
