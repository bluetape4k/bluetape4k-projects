# bluetape4k-okio DAEAD 청크 스트리밍 암호화 설계

Issue: #240

## 배경

`bluetape4k-okio`의 기존 `TinkEncryptSink`는 `write(source, byteCount)` 호출 단위로 `TinkEncryptor.encrypt()`를 수행한다. 반면 `TinkDecryptSource`는 첫 `read()`에서 delegate `Source` 전체를 `Buffer`에 적재한 뒤 단일 ciphertext로 복호화한다.

이 구조는 두 가지 문제가 있다.

- 대용량 입력에서 복호화가 전체 암호문을 메모리에 올리므로 OOM 위험이 있다.
- 여러 번의 `write()` 호출로 생성된 암호문은 여러 ciphertext가 이어진 형태인데, 기존 `TinkDecryptSource`는 이를 하나의 ciphertext로 복호화하려 하므로 구조적으로 호환되지 않는다.

Issue #240은 기존 API 호환성을 유지하면서 DAEAD (AES-SIV) 기반 청크형 wire format을 새로 제공해 실제 스트리밍 복호화를 가능하게 하는 작업이다.

## 요구사항

- `io/okio` 모듈에 신규 클래스 `DaeadChunkEncryptSink`, `DaeadChunkDecryptSource`를 추가한다.
- wire format은 각 청크마다 `[8-byte big-endian ciphertext_len][ciphertext_len bytes ciphertext]`를 반복한다.
- 기본 평문 청크 크기는 `64 * 1024` bytes다.
- 암호화 Sink는 입력을 `chunkSize` 단위로 나누어 `TinkDeterministicAead.encryptDeterministically()`로 암호화한다.
- 복호화 Source는 헤더와 해당 ciphertext만 읽고 복호화한 뒤 caller가 요청한 만큼 반환한다.
- 복호화는 전체 delegate Source를 메모리에 적재하지 않는다.
- 기존 `TinkEncryptSink` / `TinkDecryptSource`와 `asTinkEncryptSink` / `asTinkDecryptSource`는 호환성을 위해 유지한다.
- 공개 API에는 한국어 KDoc을 작성하고 DAEAD 결정성의 패턴 노출 위험을 명시한다.
- README.md / README.ko.md에 기존 Tink 어댑터와 DAEAD 청크 어댑터의 차이, `close()` 필요성, wire format 호환성 제약을 기록한다.

## Step 1-R 연구 요약

- 기존 `TinkDecryptSource`는 `ensureDecrypted()`에서 delegate Source를 EOF까지 읽고 `encryptor.decrypt(encryptedBuffer.readByteArray())`를 한 번만 호출한다.
- 기존 `TinkEncryptSink`는 `write()` 호출마다 요청 구간만 암호화하므로 write 호출 경계가 암호문 경계가 된다. 그러나 길이 prefix가 없어 복호화 쪽에서 경계를 복원할 수 없다.
- `io/okio/build.gradle.kts`는 `bluetape4k-tink`를 `compileOnly`로 사용하고 테스트에서는 `testImplementation` 확장으로 접근한다. 새 클래스도 같은 모듈/패키지에서 구현할 수 있다.
- `TinkDeterministicAead`는 `encryptDeterministically(plaintext, associatedData)` / `decryptDeterministically(ciphertext, associatedData)`를 제공하고 기본 associated data는 빈 배열이다.
- Google Tink 공식 문서는 Deterministic AEAD가 동일 plaintext에 동일 ciphertext를 생성하며 AES256_SIV key type을 권장한다고 설명한다. 또한 associated data는 인증되지만 암호화되지 않고, 결정성 때문에 동일 평문 패턴 노출 위험이 있다.
- Tink 공식 문서는 Deterministic AEAD plaintext/associated data 길이가 `0..2^32 bytes` 범위라고 설명하고, 메시지당 1MB 미만일 때 많은 메시지 암호화에 대한 안정성 기준을 제시한다. 기본 64KB 청크는 이 기준 안에 있다.
- `io/okio` 테스트는 `Buffer`, bluetape4k-assertions, `assertFailsWith`, helper `readAllTo` 패턴을 사용한다.

## 설계 옵션

### 옵션 A: 신규 DAEAD 청크 Sink/Source 추가

`DaeadChunkEncryptSink`와 `DaeadChunkDecryptSource`를 새로 추가하고 기존 AEAD 기반 `Tink*` 클래스는 그대로 둔다.

장점:

- 기존 API와 wire format을 깨지 않는다.
- 청크 길이 prefix로 복호화 경계를 명확히 복원한다.
- DAEAD 전용 보안/결정성 제약을 KDoc과 README에 별도로 설명할 수 있다.

단점:

- 사용자는 기존 `asTink*`와 새 `asDaeadChunk*`의 차이를 이해해야 한다.
- 기존 `TinkDecryptSource`의 전체로드 동작은 호환성 때문에 남는다.

### 옵션 B: 기존 TinkEncryptSink/TinkDecryptSource를 길이 prefix 포맷으로 변경

기존 클래스의 write format을 `[len][ciphertext]` 반복으로 바꾸고 복호화도 해당 포맷을 읽게 한다.

장점:

- API 이름은 그대로 유지된다.
- 기존 사용자 코드 변경량이 적다.

단점:

- 기존 단일 ciphertext 데이터와 호환되지 않는 breaking change다.
- `TinkEncryptor`는 일반 AEAD와 DAEAD를 모두 감싸므로 결정적 청크 포맷의 보안 특성을 명확히 분리하기 어렵다.

### 옵션 C: Tink Streaming AEAD로 전환

Tink의 Streaming AEAD primitive를 노출하거나 내부에서 사용한다.

장점:

- 스트리밍 암호화를 위해 설계된 primitive를 사용한다.
- 결정성 패턴 노출 문제가 없다.

단점:

- Issue #240은 DAEAD (AES-SIV) 기반 deterministic chunk format을 명시한다.
- `bluetape4k-tink`에는 현재 Streaming AEAD wrapper가 없다.
- 새 primitive 도입은 dependency/API 설계 범위가 넓어지고 이번 결함 수정 범위를 넘는다.

## 결정

옵션 A를 채택한다. 기존 `Tink*` API는 유지하고 `DaeadChunk*` API를 추가한다.

Rejected:

- 옵션 B: 기존 wire format을 깨고 기존 데이터를 읽을 수 없게 만든다.
- 옵션 C: Issue 요구인 DAEAD deterministic chunk format과 다르며 `bluetape4k-tink` 신규 primitive 설계가 필요하다.

## API 설계

```kotlin
class DaeadChunkEncryptSink(
    delegate: Sink,
    private val daead: TinkDeterministicAead,
    private val chunkSize: Int = DEFAULT_DAEAD_CHUNK_SIZE,
    private val associatedData: ByteArray = EMPTY_BYTES,
): ForwardingSink(delegate)

class DaeadChunkDecryptSource(
    delegate: Source,
    private val daead: TinkDeterministicAead,
    private val associatedData: ByteArray = EMPTY_BYTES,
): ForwardingSource(delegate)

const val DEFAULT_DAEAD_CHUNK_SIZE: Int = 64 * 1024

fun Sink.asDaeadChunkEncryptSink(
    daead: TinkDeterministicAead,
    chunkSize: Int = DEFAULT_DAEAD_CHUNK_SIZE,
    associatedData: ByteArray = EMPTY_BYTES,
): DaeadChunkEncryptSink

fun Source.asDaeadChunkDecryptSource(
    daead: TinkDeterministicAead,
    associatedData: ByteArray = EMPTY_BYTES,
): DaeadChunkDecryptSource
```

Issue에 제시된 확장 함수는 `associatedData` 기본값으로 그대로 충족한다.

## 내부 구현

### DaeadChunkEncryptSink

- 내부 `plainBuffer`에 입력을 누적한다.
- `write(source, byteCount)`는 `byteCount`를 검증하고 source에서 plainBuffer로 이동한다.
- plainBuffer가 `chunkSize` 이상이면 `chunkSize` 단위로 청크를 암호화해 delegate에 기록한다.
- `close()`는 남은 partial chunk를 암호화한 뒤 delegate를 닫는다.
- `flush()`는 이미 완성된 청크만 기록하고 partial chunk는 유지한 뒤 delegate flush를 호출한다.
- `chunkSize`는 양수여야 한다.
- `associatedData`는 생성 시 복사해 호출자가 전달한 `ByteArray`를 나중에 변경해도 암복호화 계약이 흔들리지 않게 한다.
- empty input은 chunk를 기록하지 않고 닫는다.

### DaeadChunkDecryptSource

- 내부 `plainBuffer`에 현재 복호화된 청크의 남은 평문만 보관한다.
- `read(sink, byteCount)`는 `byteCount`를 검증하고, plainBuffer가 비어 있으면 다음 chunk 하나만 읽어 복호화한다.
- chunk header 8 bytes를 완전히 읽지 못하면 `EOFException`을 던진다. 단, header 첫 바이트도 읽기 전에 EOF면 정상 EOF다.
- ciphertext length는 `1..Int.MAX_VALUE` 범위로 제한한다. 0 또는 음수 길이는 손상된 입력으로 간주해 `IOException`을 던진다.
- ciphertext 본문을 완전히 읽지 못하면 `EOFException`을 던진다.
- 복호화 실패는 Tink 예외를 그대로 전파한다.
- 전체 파일을 읽지 않고 최대 `ciphertext_len + decrypted current chunk` 범위만 메모리에 둔다.
- `associatedData`는 생성 시 복사해 Source 수명 중 외부 변경 영향을 받지 않게 한다.

## 리스크와 대응

| Risk                                         | Impact                                             | Mitigation                                                               |
|----------------------------------------------|----------------------------------------------------|--------------------------------------------------------------------------|
| 기존 Tink API와 새 DAEAD API 혼동            | 잘못된 Source/Sink 조합으로 복호화 실패            | 클래스/KDoc/README에 wire format 비호환 명시                             |
| partial chunk가 `close()` 전에 기록되지 않음 | 사용자가 close 누락 시 데이터 손실                 | KDoc/README에 `use {}` 권장, 테스트로 close finalize 검증                |
| 손상된 length header                         | 과도한 메모리 할당 또는 무한 대기                  | 길이 범위 검증, truncated header/body EOF 테스트                         |
| 결정적 암호화 패턴 노출                      | 동일 청크 plaintext가 동일 ciphertext로 관찰 가능  | KDoc/README에 DAEAD 결정성 경고와 일반 암호화 필요 시 기존 AEAD API 안내 |
| associated data 불일치                       | 정상 데이터 복호화 실패                            | associatedData round-trip과 mismatch 실패 테스트                         |
| mutable associatedData 배열 변경             | 같은 Source/Sink 인스턴스의 인증 컨텍스트 변조     | 생성 시 `copyOf()`로 방어적 복사                                         |
| 청크 경계 결정성                             | 같은 chunkSize/input/key/AD는 같은 wire bytes 생성 | 테스트로 결정성 확인, 문서에 장단점 기록                                 |

## Acceptance Criteria

- `DaeadChunkEncryptSink`와 `DaeadChunkDecryptSource`가 `io.bluetape4k.okio.tink` 패키지에 추가된다.
- `asDaeadChunkEncryptSink` / `asDaeadChunkDecryptSource` 확장 함수가 제공된다.
- 기본 chunk size는 64KB이고, 양수가 아닌 chunk size는 거부된다.
- wire format은 각 청크를 8-byte big-endian ciphertext length와 ciphertext bytes로 기록한다.
- empty input, single chunk, multi chunk, small repeated writes, incremental reads가 round-trip 된다.
- 다중 `write()` 호출로 암호화한 데이터가 정상 복호화된다.
- truncated header, truncated ciphertext, invalid length, wrong associated data가 실패한다.
- 복호화 Source는 첫 `read()`에서 전체 ciphertext를 소비하지 않는다.
- README.md / README.ko.md가 새 DAEAD 청크 API와 제약을 설명한다.

## DoD

- `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:test --tests "io.bluetape4k.okio.tink.*"` 통과
- `./bin/repo-test-summary -- ./gradlew :bluetape4k-okio:compileKotlin :bluetape4k-okio:compileTestKotlin` 통과
- 공개 API 한국어 KDoc 작성
- README.md / README.ko.md 업데이트
- Step 6-R 6개 리뷰 티어 수행
- Lore commit trailer를 포함한 커밋 작성
- Issue #240을 닫는 PR 생성

## Draft Task List

1. DAEAD 청크 Sink/Source와 확장 함수 구현
2. DAEAD 청크 round-trip, 손상 입력, 스트리밍 동작 테스트 작성
3. README.md / README.ko.md에 사용 예시와 제약 업데이트
4. targeted compile/test, verifier, 리뷰 게이트 수행
