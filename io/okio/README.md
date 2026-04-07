# Module bluetape4k-okio

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-okio` is a high-performance I/O extension module built on Square's [Okio](https://square.github.io/okio/) library. On top of Okio's
`Source`/
`Sink` abstractions, it provides compression, encryption, Base64 encoding, NIO channel integration, and Kotlin Coroutines async I/O.

## Key Features

### 1. Buffer / ByteString Utilities

Factory functions and extension functions for creating Okio `Buffer` and `ByteString` instances.

```kotlin
import io.bluetape4k.okio.*

// Creating Buffers
val buffer = bufferOf("Hello, Okio!")
val buffer2 = bufferOf(byteArrayOf(1, 2, 3))
val buffer3 = bufferOf(inputStream)

// Creating ByteStrings
val byteString = byteStringOf("Hello")
val byteString2 = byteStringOf(byteArrayOf(1, 2, 3))
```

### 2. Source / Sink Extensions

Adapters to convert `InputStream`/`OutputStream` into Okio `Source`/`Sink`.

```kotlin
import io.bluetape4k.okio.*

// InputStream → Source
val source = inputStream.asSource()

// OutputStream → Sink
val sink = outputStream.asSink()
```

### 3. NIO Channel Support

Integrates Java NIO `ReadableByteChannel`/`WritableByteChannel`/`FileChannel` with Okio.

```kotlin
import io.bluetape4k.okio.channels.*

// ByteChannel → Source/Sink
val source = readableByteChannel.asSource()
val sink = writableByteChannel.asSink()

// FileChannel → Source/Sink
val fileSource = FileChannelSource(fileChannel)
val fileSink = FileChannelSink(fileChannel)
```

### 4. Compression Streams

Wraps `bluetape4k-io`'s `Compressor`/`StreamingCompressor` as Okio Sink/Source for streaming compression.

```kotlin
import io.bluetape4k.okio.compress.*
import io.bluetape4k.io.compressor.Compressors

// Compression Sink (compression is finalized on close)
val compressSink = sink.asCompressSink(Compressors.LZ4)
compressSink.use { cs ->
    cs.write(buffer, buffer.size)
}

// Decompression Source
val decompressSource = source.asDecompressSource(Compressors.LZ4)
decompressSource.use { ds ->
    ds.read(outputBuffer, Long.MAX_VALUE)
}

// Using StreamingCompressor (for large-scale streaming)
val streamingSink = sink.asCompressSink(Compressors.Streaming.Zstd)
val streamingSource = source.asDecompressSource(Compressors.Streaming.Zstd)
```

**Important notes:**

- `CompressableSink` finalizes compression at `close()`. Always use `close()` or `use {}`.
- `StreamingCompressSink` also requires `close()` to write the footer/finalize bytes.

### 5. Tink Encryption (Recommended)

Provides encryption Sink/Source based on Google Tink AEAD.

```kotlin
import io.bluetape4k.okio.tink.*
import io.bluetape4k.tink.encrypt.TinkEncryptors

// Encryption Sink
val encryptSink = sink.asTinkEncryptSink(TinkEncryptors.AES256_GCM)
encryptSink.write(buffer, buffer.size)

// Decryption Source
val decryptSource = source.asTinkDecryptSource(TinkEncryptors.AES256_GCM)
val result = Buffer()
decryptSource.read(result, Long.MAX_VALUE)
```

**Encryption + Compression combined:**

```kotlin
// Compress then encrypt
val combinedSink = sink
    .asTinkEncryptSink(TinkEncryptors.AES256_GCM)
    .asCompressSink(Compressors.Zstd)

combinedSink.use { it.write(buffer, buffer.size) }
```

### 6. Base64 Encoding/Decoding

Okio Sink/Source-based Base64 encoding and decoding.

```kotlin
import io.bluetape4k.okio.base64.*

// Base64 encoding Sink
val encodeSink = ApacheBase64Sink(delegate)
encodeSink.write(buffer, buffer.size)

// Base64 decoding Source
val decodeSource = ApacheBase64Source(delegate)
decodeSource.read(outputBuffer, Long.MAX_VALUE)
```

### 7. Kotlin Coroutines Async I/O

Wraps Okio Source/Sink as Kotlin Coroutines `suspend` functions for async I/O.

```kotlin
import io.bluetape4k.okio.coroutines.*
import java.nio.file.Paths

// Suspended file read
suspend fun readFileAsync(path: String): ByteArray {
    val source = SuspendedFileChannelSource(Paths.get(path))
    val buffer = Buffer()
    source.use { it.readAll(buffer) }
    return buffer.readByteArray()
}

// Suspended file write
suspend fun writeFileAsync(path: String, data: ByteArray) {
    val sink = SuspendedFileChannelSink(Paths.get(path))
    val buffer = Buffer().write(data)
    sink.use {
        it.write(buffer)
        it.flush()
    }
}

// Suspended socket communication
val socketSource = SuspendedSocketChannelSource(socketChannel)
val socketSink = SuspendedSocketChannelSink(socketChannel)
```

**Suspended Pipe (producer-consumer pattern):**

```kotlin
import io.bluetape4k.okio.coroutines.*

val pipe = SuspendedPipe()

// Producer
launch {
    pipe.sink.use { sink ->
        sink.write(Buffer().writeUtf8("Hello"))
        sink.flush()
    }
}

// Consumer
launch {
    pipe.source.use { source ->
        val buffer = Buffer()
        source.read(buffer, Long.MAX_VALUE)
    }
}
```

## Adding the Dependency

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-okio:${version}")

    // Required (included automatically)
    // io.github.bluetape4k:bluetape4k-io
    // com.squareup.okio:okio

    // Optional (add based on features needed)
    implementation("io.github.bluetape4k:bluetape4k-tink:${version}")        // Tink encryption
    implementation("io.github.bluetape4k:bluetape4k-coroutines:${version}")  // Coroutines async I/O
    implementation("commons-codec:commons-codec:1.17.0")                     // Base64
}
```

## Module Structure

```
io.bluetape4k.okio
├── BufferSupport.kt            # Buffer factory (bufferOf)
├── ByteStringSupport.kt        # ByteString factory (byteStringOf)
├── SinkSupport.kt              # Sink extension functions
├── SourceSupport.kt            # Source extension functions
├── InputStreamSource.kt        # InputStream → Source adapter
├── OutputStreamSink.kt         # OutputStream → Sink adapter
├── channels/                   # NIO channel integration
│   ├── FileChannelSink.kt
│   ├── FileChannelSource.kt
│   ├── ByteChannelSink.kt
│   └── ByteChannelSource.kt
├── compress/                   # Compression streams
│   ├── CompressableSink.kt     # Compressor-based compression Sink
│   ├── DecompressableSource.kt # Compressor-based decompression Source
│   ├── SinkWithCompressor.kt   # Legacy-compatible compression Sink
│   ├── SourceWithCompressor.kt # Legacy-compatible decompression Source
│   └── Compressable.kt         # Compression interface
├── tink/                       # Tink AEAD encryption (recommended)
│   ├── TinkEncryptSink.kt
│   └── TinkDecryptSource.kt
├── base64/                     # Base64 encoding/decoding
│   ├── ApacheBase64Sink.kt
│   ├── ApacheBase64Source.kt
│   ├── OkioBase64Sink.kt
│   └── OkioBase64Source.kt
└── coroutines/                 # Kotlin Coroutines async I/O
    ├── SuspendedSource.kt
    ├── SuspendedSink.kt
    ├── SuspendedFileChannelSource.kt
    ├── SuspendedFileChannelSink.kt
    ├── SuspendedSocketChannelSource.kt
    ├── SuspendedSocketChannelSink.kt
    ├── SuspendedPipe.kt
    └── [Buffered implementations, etc.]
```

## Class Structure

### Sink / Source Adapter Hierarchy

Compression, encryption, and Base64 encoding are layered on top of Okio's `Sink`/
`Source` abstractions using the decorator pattern.

```mermaid
classDiagram
    class Sink {
        <<interface_okio>>
        +write(source: Buffer, byteCount: Long)
        +flush()
        +close()
    }

    class Source {
        <<interface_okio>>
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

### NIO Channel Adapter Hierarchy

Converts Java NIO `FileChannel`/`ByteChannel` to Okio `Sink`/`Source`.

```mermaid
classDiagram
    class Sink {
        <<interface_okio>>
    }
    class Source {
        <<interface_okio>>
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

### Coroutines Async I/O Hierarchy

Async Sink/Source abstraction based on Kotlin Coroutines `suspend` functions.

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

### Compression Factory (Compressable)

The
`Compressable` object provides a convenient way to create compression/decompression Sink/Source for various algorithms.

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

## Sequence Diagrams

### Compression Sink (One-Shot) — compress on close

`CompressableSink` accumulates all data in an internal buffer and compresses everything at `close()`.

```mermaid
sequenceDiagram
    participant C as Caller
    participant CS as CompressableSink
    participant PB as plainBuffer
    participant Comp as Compressor
    participant D as delegate Sink

    C->>CS: write(data, byteCount)
    CS->>PB: write(data, byteCount)
    Note over PB: Accumulated in internal buffer

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

### Compression Sink (Streaming) — compress incrementally

`StreamingCompressSink` compresses data immediately as it arrives, making it ideal for large-scale streaming.

```mermaid
sequenceDiagram
    participant C as Caller
    participant SS as StreamingCompressSink
    participant CS as compressingStream
    participant D as delegate Sink

    Note over SS: On init, creates compressor.compressing(delegate.outputStream())

    C->>SS: write(data, byteCount)
    SS->>CS: write(bytes, 0, size)
    CS->>D: Write compressed chunk

    C->>SS: write(data2, byteCount2)
    SS->>CS: write(bytes, 0, size)
    CS->>D: Write compressed chunk

    C->>SS: close()
    SS->>CS: close()
    Note over CS: Write footer/finalize
    CS->>D: Write final chunk
    SS->>D: close()
```

### Decompression Source (One-Shot) — decompress on first read

`DecompressableSource` decompresses and caches all data on the first `read()` call.

```mermaid
sequenceDiagram
    participant C as Caller
    participant DS as DecompressableSource
    participant DB as decodedBuffer
    participant Comp as Compressor
    participant D as delegate Source

    C->>DS: read(sink, byteCount)
    alt First read (decodedReady == false)
        DS->>D: readByteArray()
        D-->>DS: compressedBytes
        DS->>Comp: decompress(compressedBytes)
        Comp-->>DS: plainBytes
        DS->>DB: write(plainBytes)
        Note over DS: decodedReady = true
    end
    DS->>DB: read(sink, min(byteCount, remaining))
    DB-->>C: Decompressed data
```

### Tink Encryption + Compression Combined Flow

Compression followed by encryption using chained Sink decorators.

```mermaid
sequenceDiagram
    participant C as Caller
    participant CS as CompressableSink
    participant ES as TinkEncryptSink
    participant D as delegate Sink

    Note over C: sink.asTinkEncryptSink(encryptor).asCompressSink(compressor)

    C->>CS: write(plainData)
    Note over CS: Accumulated in internal buffer

    C->>CS: close()
    CS->>CS: compress(plainData)
    CS->>ES: write(compressedData)
    ES->>ES: encrypt(compressedData)
    ES->>D: write(encryptedData)
    ES->>D: flush()
    D-->>C: Compressed + encrypted data written
```

### Coroutines Async File I/O Flow

Non-blocking file I/O using `AsynchronousFileChannel`.

```mermaid
sequenceDiagram
    participant C as Coroutine
    participant BS as BufferedSuspendedSink
    participant RS as RealBufferedSuspendedSink
    participant FS as SuspendedFileChannelSink
    participant CH as AsynchronousFileChannel

    C->>BS: write(buffer) [suspend]
    BS->>RS: write(buffer) [suspend]
    RS->>RS: Accumulate in buffer
    C->>BS: emit() [suspend]
    BS->>RS: emit() [suspend]
    RS->>FS: write(buffer, byteCount) [suspend]
    FS->>CH: write(ByteBuffer, position)
    Note over FS,CH: CompletionHandler → suspendCoroutine conversion
    CH-->>FS: bytesWritten
    FS-->>RS: Complete
    RS-->>C: Complete

    C->>BS: close() [suspend]
    BS->>RS: close() [suspend]
    RS->>FS: flush() [suspend]
    FS->>CH: force(false)
    RS->>FS: close() [suspend]
    FS->>CH: close()
```

## License

Apache License 2.0

## References

- [Okio Documentation](https://square.github.io/okio/)
- [Google Tink](https://developers.google.com/tink)
- [bluetape4k-io](../io/README.md)
- [bluetape4k-tink](../tink/README.md)
