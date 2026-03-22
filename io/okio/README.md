# Module bluetape4k-okio

## 개요

`bluetape4k-okio`는 Square의 [Okio](https://square.github.io/okio/) 라이브러리를 기반으로 한 고성능 I/O 확장 모듈입니다.
Okio의 `Source`/`Sink` 추상화 위에 압축, 암호화, Base64 인코딩, NIO 채널 통합, Kotlin Coroutines 비동기 I/O 등을 제공합니다.

## 주요 기능

### 1. Buffer / ByteString 유틸리티

Okio `Buffer`와 `ByteString` 생성을 위한 팩토리 함수와 확장 함수를 제공합니다.

```kotlin
import io.bluetape4k.okio.*

// Buffer 생성
val buffer = bufferOf("Hello, Okio!")
val buffer2 = bufferOf(byteArrayOf(1, 2, 3))
val buffer3 = bufferOf(inputStream)

// ByteString 생성
val byteString = byteStringOf("Hello")
val byteString2 = byteStringOf(byteArrayOf(1, 2, 3))
```

### 2. Source / Sink 확장

`InputStream`/`OutputStream`을 Okio `Source`/`Sink`로 변환하는 어댑터를 제공합니다.

```kotlin
import io.bluetape4k.okio.*

// InputStream → Source
val source = inputStream.asSource()

// OutputStream → Sink
val sink = outputStream.asSink()
```

### 3. NIO 채널 지원

Java NIO `ReadableByteChannel`/`WritableByteChannel`/`FileChannel`을 Okio와 통합합니다.

```kotlin
import io.bluetape4k.okio.channels.*

// ByteChannel → Source/Sink
val source = readableByteChannel.asSource()
val sink = writableByteChannel.asSink()

// FileChannel → Source/Sink
val fileSource = FileChannelSource(fileChannel)
val fileSink = FileChannelSink(fileChannel)
```

### 4. 압축 스트림

`bluetape4k-io`의 `Compressor`/`StreamingCompressor`를 Okio Sink/Source로 래핑하여 스트리밍 압축/해제를 지원합니다.

```kotlin
import io.bluetape4k.okio.compress.*
import io.bluetape4k.io.compressor.Compressors

// 압축 Sink (close 시점에 압축 확정)
val compressSink = sink.asCompressSink(Compressors.LZ4)
compressSink.use { cs ->
    cs.write(buffer, buffer.size)
}

// 복원 Source
val decompressSource = source.asDecompressSource(Compressors.LZ4)
decompressSource.use { ds ->
    ds.read(outputBuffer, Long.MAX_VALUE)
}

// StreamingCompressor 사용 (대용량 스트리밍)
val streamingSink = sink.asCompressSink(Compressors.Streaming.Zstd)
val streamingSource = source.asDecompressSource(Compressors.Streaming.Zstd)
```

**주의사항:**
- `CompressableSink`는 `close()` 시점에 압축 결과가 확정됩니다. 반드시 `close()` 또는 `use {}`를 사용하세요.
- `StreamingCompressSink`도 footer/finalize 기록을 위해 `close()`가 필요합니다.

### 5. Tink 암호화 (권장)

Google Tink AEAD 기반 암호화 Sink/Source를 제공합니다.

```kotlin
import io.bluetape4k.okio.tink.*
import io.bluetape4k.tink.encrypt.TinkEncryptors

// 암호화 Sink
val encryptSink = sink.asTinkEncryptSink(TinkEncryptors.AES256_GCM)
encryptSink.write(buffer, buffer.size)

// 복호화 Source
val decryptSource = source.asTinkDecryptSource(TinkEncryptors.AES256_GCM)
val result = Buffer()
decryptSource.read(result, Long.MAX_VALUE)
```

**암호화 + 압축 조합:**

```kotlin
// 압축 → 암호화
val combinedSink = sink
    .asTinkEncryptSink(TinkEncryptors.AES256_GCM)
    .asCompressSink(Compressors.Zstd)

combinedSink.use { it.write(buffer, buffer.size) }
```

### 6. Base64 인코딩/디코딩

Okio Sink/Source 기반 Base64 인코딩/디코딩을 제공합니다.

```kotlin
import io.bluetape4k.okio.base64.*

// Base64 인코딩 Sink
val encodeSink = ApacheBase64Sink(delegate)
encodeSink.write(buffer, buffer.size)

// Base64 디코딩 Source
val decodeSource = ApacheBase64Source(delegate)
decodeSource.read(outputBuffer, Long.MAX_VALUE)
```

### 7. Kotlin Coroutines 비동기 I/O

Okio Source/Sink를 Kotlin Coroutines `suspend` 함수로 래핑하여 비동기 I/O를 제공합니다.

```kotlin
import io.bluetape4k.okio.coroutines.*
import java.nio.file.Paths

// Suspended 파일 읽기
suspend fun readFileAsync(path: String): ByteArray {
    val source = SuspendedFileChannelSource(Paths.get(path))
    val buffer = Buffer()
    source.use { it.readAll(buffer) }
    return buffer.readByteArray()
}

// Suspended 파일 쓰기
suspend fun writeFileAsync(path: String, data: ByteArray) {
    val sink = SuspendedFileChannelSink(Paths.get(path))
    val buffer = Buffer().write(data)
    sink.use {
        it.write(buffer)
        it.flush()
    }
}

// Suspended Socket 통신
val socketSource = SuspendedSocketChannelSource(socketChannel)
val socketSink = SuspendedSocketChannelSink(socketChannel)
```

**Suspended Pipe (생산자-소비자 패턴):**

```kotlin
import io.bluetape4k.okio.coroutines.*

val pipe = SuspendedPipe()

// 생산자
launch {
    pipe.sink.use { sink ->
        sink.write(Buffer().writeUtf8("Hello"))
        sink.flush()
    }
}

// 소비자
launch {
    pipe.source.use { source ->
        val buffer = Buffer()
        source.read(buffer, Long.MAX_VALUE)
    }
}
```

## 의존성 추가

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-okio:${version}")

    // 필수 (자동 포함)
    // io.github.bluetape4k:bluetape4k-io
    // com.squareup.okio:okio

    // 선택적 의존성 (필요한 기능에 따라 추가)
    implementation("io.github.bluetape4k:bluetape4k-tink:${version}")        // Tink 암호화
    implementation("io.github.bluetape4k:bluetape4k-coroutines:${version}")  // Coroutines 비동기 I/O
    implementation("commons-codec:commons-codec:1.17.0")                     // Base64
}
```

## 모듈 구조

```
io.bluetape4k.okio
├── BufferSupport.kt            # Buffer 팩토리 (bufferOf)
├── ByteStringSupport.kt        # ByteString 팩토리 (byteStringOf)
├── SinkSupport.kt              # Sink 확장 함수
├── SourceSupport.kt            # Source 확장 함수
├── InputStreamSource.kt        # InputStream → Source 어댑터
├── OutputStreamSink.kt         # OutputStream → Sink 어댑터
├── channels/                   # NIO 채널 통합
│   ├── FileChannelSink.kt
│   ├── FileChannelSource.kt
│   ├── ByteChannelSink.kt
│   └── ByteChannelSource.kt
├── compress/                   # 압축 스트림
│   ├── CompressableSink.kt     # Compressor 기반 압축 Sink
│   ├── DecompressableSource.kt # Compressor 기반 복원 Source
│   ├── SinkWithCompressor.kt   # 레거시 호환 압축 Sink
│   ├── SourceWithCompressor.kt # 레거시 호환 복원 Source
│   └── Compressable.kt         # 압축 인터페이스
├── tink/                       # Tink AEAD 암호화 (권장)
│   ├── TinkEncryptSink.kt
│   └── TinkDecryptSource.kt
├── base64/                     # Base64 인코딩/디코딩
│   ├── ApacheBase64Sink.kt
│   ├── ApacheBase64Source.kt
│   ├── OkioBase64Sink.kt
│   └── OkioBase64Source.kt
└── coroutines/                 # Kotlin Coroutines 비동기 I/O
    ├── SuspendedSource.kt
    ├── SuspendedSink.kt
    ├── SuspendedFileChannelSource.kt
    ├── SuspendedFileChannelSink.kt
    ├── SuspendedSocketChannelSource.kt
    ├── SuspendedSocketChannelSink.kt
    ├── SuspendedPipe.kt
    └── [Buffered 구현체 등]
```

## 클래스 구조

### Sink / Source 어댑터 계층

Okio의 `Sink`/`Source` 추상화 위에 압축, 암호화, Base64 인코딩 등을 데코레이터 패턴으로 제공합니다.

```mermaid
classDiagram
    class Sink {
        <<interface, okio>>
        +write(source: Buffer, byteCount: Long)
        +flush()
        +close()
    }

    class Source {
        <<interface, okio>>
        +read(sink: Buffer, byteCount: Long) Long
        +close()
    }

    class ForwardingSink {
        <<okio>>
        #delegate: Sink
    }

    class ForwardingSource {
        <<okio>>
        #delegate: Source
    }

    class InputStreamSource {
        -input: InputStream
        +read(sink: Buffer, byteCount: Long) Long
    }

    class OutputStreamSink {
        -output: OutputStream
        +write(source: Buffer, byteCount: Long)
    }

    class CompressableSink {
        -plainBuffer: Buffer
        -compressor: Compressor
        +write(source: Buffer, byteCount: Long)
        +close()
    }

    class StreamingCompressSink {
        -compressingStream: OutputStream
        -compressor: StreamingCompressor
    }

    class DecompressableSource {
        -decodedBuffer: Buffer
        -compressor: Compressor
        -ensureDecoded()
    }

    class StreamingDecompressSource {
        -decompressingStream: InputStream
        -compressor: StreamingCompressor
    }

    class TinkEncryptSink {
        -encryptor: TinkEncryptor
        +write(source: Buffer, byteCount: Long)
    }

    class TinkDecryptSource {
        -encryptor: TinkEncryptor
        -decryptedBuffer: Buffer
        -ensureDecrypted()
    }

    class AbstractBase64Sink {
        <<abstract>>
        #getEncodedBuffer(ByteString) Buffer
    }

    class AbstractBase64Source {
        <<abstract>>
        #decodeBase64Bytes(String) ByteString
    }

    class ApacheBase64Sink
    class OkioBase64Sink
    class ApacheBase64Source
    class OkioBase64Source

    Sink <|.. ForwardingSink
    Source <|.. ForwardingSource
    Source <|.. InputStreamSource
    Sink <|.. OutputStreamSink

    ForwardingSink <|-- CompressableSink
    CompressableSink <|-- StreamingCompressSink
    ForwardingSource <|-- DecompressableSource
    DecompressableSource <|-- StreamingDecompressSource

    ForwardingSink <|-- TinkEncryptSink
    ForwardingSource <|-- TinkDecryptSource

    ForwardingSink <|-- AbstractBase64Sink
    AbstractBase64Sink <|-- ApacheBase64Sink
    AbstractBase64Sink <|-- OkioBase64Sink
    ForwardingSource <|-- AbstractBase64Source
    AbstractBase64Source <|-- ApacheBase64Source
    AbstractBase64Source <|-- OkioBase64Source
```

### NIO 채널 어댑터 계층

Java NIO `FileChannel`/`ByteChannel`을 Okio `Sink`/`Source`로 변환합니다.

```mermaid
classDiagram
    class Sink {
        <<interface, okio>>
    }
    class Source {
        <<interface, okio>>
    }

    class FileChannelSink {
        -channel: FileChannel
        +write(source: Buffer, byteCount: Long)
        +flush()
    }

    class FileChannelSource {
        -channel: FileChannel
        +read(sink: Buffer, byteCount: Long) Long
        +readAll(sink: Buffer) Long
    }

    class ByteChannelSink {
        -channel: ByteChannel
        +write(source: Buffer, byteCount: Long)
    }

    class ByteChannelSource {
        -channel: ByteChannel
        +read(sink: Buffer, byteCount: Long) Long
        +readAll(sink: Buffer) Long
    }

    Sink <|.. FileChannelSink
    Sink <|.. ByteChannelSink
    Source <|.. FileChannelSource
    Source <|.. ByteChannelSource
```

### Coroutines 비동기 I/O 계층

Kotlin Coroutines `suspend` 함수 기반 비동기 Sink/Source 추상화입니다.

```mermaid
classDiagram
    class SuspendedSink {
        <<interface>>
        +write(source: Buffer, byteCount: Long)*
        +flush()*
        +close()*
        +timeout() Timeout
    }

    class SuspendedSource {
        <<interface>>
        +read(sink: Buffer, byteCount: Long)* Long
        +close()*
        +timeout() Timeout
        +readAll(sink: Buffer)* Long
    }

    class BufferedSuspendedSink {
        <<interface>>
        +buffer: Buffer
        +write(byteString: ByteString)*
        +writeUtf8(string: String)*
        +writeByte(b: Int)*
        +writeInt(i: Int)*
        +writeLong(v: Long)*
        +emit()*
    }

    class BufferedSuspendedSource {
        <<interface>>
        +buffer: Buffer
        +exhausted()* Boolean
        +require(byteCount: Long)*
        +readByte()* Byte
        +readInt()* Int
        +readLong()* Long
        +readUtf8(byteCount: Long)* String
        +readAll()* ByteString
    }

    class RealBufferedSuspendedSink {
        -sink: SuspendedSink
        -closed: AtomicBoolean
    }

    class RealBufferedSuspendedSource {
        -source: SuspendedSource
        -closed: AtomicBoolean
    }

    class SuspendedFileChannelSink {
        -channel: AsynchronousFileChannel
        -position: Long
        -writeBuffer: ByteBuffer
    }

    class SuspendedFileChannelSource {
        -channel: AsynchronousFileChannel
        -position: Long
        -readBuffer: ByteBuffer
    }

    class SuspendedSocketChannelSink {
        -channel: AsynchronousSocketChannel
    }

    class SuspendedSocketChannelSource {
        -channel: AsynchronousSocketChannel
    }

    class SuspendedPipe {
        +sink: BufferedSuspendedSink
        +source: BufferedSuspendedSource
    }

    SuspendedSink <|-- BufferedSuspendedSink
    BufferedSuspendedSink <|.. RealBufferedSuspendedSink

    SuspendedSource <|-- BufferedSuspendedSource
    BufferedSuspendedSource <|.. RealBufferedSuspendedSource

    SuspendedSink <|.. SuspendedFileChannelSink
    SuspendedSink <|.. SuspendedSocketChannelSink
    SuspendedSource <|.. SuspendedFileChannelSource
    SuspendedSource <|.. SuspendedSocketChannelSource

    SuspendedPipe --> BufferedSuspendedSink
    SuspendedPipe --> BufferedSuspendedSource
```

### 압축 팩토리 (Compressable)

`Compressable` 오브젝트를 통해 다양한 알고리즘의 압축/복원 Sink/Source를 편리하게 생성할 수 있습니다.

```mermaid
classDiagram
    class Compressable {
        <<object>>
    }

    class Sinks {
        <<object>>
        +compressableSink(Sink, Compressor) CompressableSink
        +compressableSink(Sink, StreamingCompressor) StreamingCompressSink
        +deflate(Sink) CompressableSink
        +gzip(Sink) CompressableSink
        +lz4(Sink) CompressableSink
        +snappy(Sink) CompressableSink
        +zstd(Sink) CompressableSink
        +bzip2(Sink) CompressableSink
    }

    class Sources {
        <<object>>
        +decompressableSource(Source, Compressor) DecompressableSource
        +decompressableSource(Source, StreamingCompressor) StreamingDecompressSource
        +deflate(Source) DecompressableSource
        +gzip(Source) DecompressableSource
        +lz4(Source) DecompressableSource
        +snappy(Source) DecompressableSource
        +zstd(Source) DecompressableSource
        +bzip2(Source) DecompressableSource
    }

    Compressable *-- Sinks
    Compressable *-- Sources
```

## 시퀀스 다이어그램

### 압축 Sink (One-Shot) — compress on close

`CompressableSink`는 모든 데이터를 내부 버퍼에 축적한 뒤, `close()` 시점에 한 번에 압축합니다.

```mermaid
sequenceDiagram
    participant C as 호출자
    participant CS as CompressableSink
    participant PB as plainBuffer
    participant Comp as Compressor
    participant D as delegate Sink

    C->>CS: write(data, byteCount)
    CS->>PB: write(data, byteCount)
    Note over PB: 내부 버퍼에 축적

    C->>CS: write(data2, byteCount2)
    CS->>PB: write(data2, byteCount2)

    C->>CS: close()
    CS->>PB: readByteArray()
    PB-->>CS: plainBytes
    CS->>Comp: compress(plainBytes)
    Comp-->>CS: compressedBytes
    CS->>D: write(compressedBytes)
    CS->>D: flush()
    CS->>D: close()
```

### 압축 Sink (Streaming) — compress incrementally

`StreamingCompressSink`는 데이터를 수신할 때마다 즉시 압축하여 대용량 스트리밍에 적합합니다.

```mermaid
sequenceDiagram
    participant C as 호출자
    participant SS as StreamingCompressSink
    participant CS as compressingStream
    participant D as delegate Sink

    Note over SS: 초기화 시 compressor.compressing(delegate.outputStream()) 생성

    C->>SS: write(data, byteCount)
    SS->>CS: write(bytes, 0, size)
    CS->>D: 압축된 청크 기록

    C->>SS: write(data2, byteCount2)
    SS->>CS: write(bytes, 0, size)
    CS->>D: 압축된 청크 기록

    C->>SS: close()
    SS->>CS: close()
    Note over CS: footer/finalize 기록
    CS->>D: 최종 청크 기록
    SS->>D: close()
```

### 복원 Source (One-Shot) — decompress on first read

`DecompressableSource`는 첫 번째 `read()` 호출 시 전체 데이터를 복원하고 캐싱합니다.

```mermaid
sequenceDiagram
    participant C as 호출자
    participant DS as DecompressableSource
    participant DB as decodedBuffer
    participant Comp as Compressor
    participant D as delegate Source

    C->>DS: read(sink, byteCount)
    alt 첫 번째 read (decodedReady == false)
        DS->>D: readByteArray()
        D-->>DS: compressedBytes
        DS->>Comp: decompress(compressedBytes)
        Comp-->>DS: plainBytes
        DS->>DB: write(plainBytes)
        Note over DS: decodedReady = true
    end
    DS->>DB: read(sink, min(byteCount, remaining))
    DB-->>C: 복원된 데이터
```

### Tink 암호화 + 압축 조합 흐름

`Sink` 데코레이터를 체이닝하여 압축 후 암호화를 적용합니다.

```mermaid
sequenceDiagram
    participant C as 호출자
    participant CS as CompressableSink
    participant ES as TinkEncryptSink
    participant D as delegate Sink

    Note over C: sink.asTinkEncryptSink(encryptor).asCompressSink(compressor)

    C->>CS: write(plainData)
    Note over CS: 내부 버퍼에 축적

    C->>CS: close()
    CS->>CS: compress(plainData)
    CS->>ES: write(compressedData)
    ES->>ES: encrypt(compressedData)
    ES->>D: write(encryptedData)
    ES->>D: flush()
    D-->>C: 압축 + 암호화된 데이터 기록 완료
```

### Coroutines 비동기 파일 I/O 흐름

`AsynchronousFileChannel`을 사용하여 논블로킹 파일 I/O를 수행합니다.

```mermaid
sequenceDiagram
    participant C as Coroutine
    participant BS as BufferedSuspendedSink
    participant RS as RealBufferedSuspendedSink
    participant FS as SuspendedFileChannelSink
    participant CH as AsynchronousFileChannel

    C->>BS: write(buffer) [suspend]
    BS->>RS: write(buffer) [suspend]
    RS->>RS: buffer에 축적
    C->>BS: emit() [suspend]
    BS->>RS: emit() [suspend]
    RS->>FS: write(buffer, byteCount) [suspend]
    FS->>CH: write(ByteBuffer, position)
    Note over FS,CH: CompletionHandler → suspendCoroutine 변환
    CH-->>FS: bytesWritten
    FS-->>RS: 완료
    RS-->>C: 완료

    C->>BS: close() [suspend]
    BS->>RS: close() [suspend]
    RS->>FS: flush() [suspend]
    FS->>CH: force(false)
    RS->>FS: close() [suspend]
    FS->>CH: close()
```

## 라이선스

Apache License 2.0

## 참고

- [Okio Documentation](https://square.github.io/okio/)
- [Google Tink](https://developers.google.com/tink)
- [bluetape4k-io](../io/README.md)
- [bluetape4k-tink](../tink/README.md)
