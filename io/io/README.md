# Module bluetape4k-io

## 개요

`bluetape4k-io`는 Kotlin 기반의 고성능 I/O 유틸리티 라이브러리입니다. 파일 처리, 압축, 직렬화, 비동기 I/O 등 다양한 I/O 작업을 간편하고 효율적으로 처리할 수 있는 기능을 제공합니다.

## 주요 기능

### 1. 압축 (Compressor)

다양한 압축 알고리즘을 통일된 인터페이스로 제공합니다.

**지원 알고리즘:**

- **LZ4**: 초고속 압축/해제 (실시간 처리에 적합)
- **Snappy**: 빠른 압축 속도 (Google 개발)
- **Zstd**: 높은 압축률과 빠른 속도의 균형
- **GZip**: 범용적인 압축 (호환성 우수)
- **Deflate**: GZip의 기반 알고리즘
- **BZip2**: 높은 압축률 (속도는 느림)

```kotlin
import io.bluetape4k.io.compressor.Compressors

// 기본 사용
val plainData = "Hello, World!".toByteArray()
val compressed = Compressors.LZ4.compress(plainData)
val decompressed = Compressors.LZ4.decompress(compressed)

// 문자열 직접 압축 (Base64 인코딩됨)
val compressedStr = Compressors.Zstd.compress("Large text data...")
val originalStr = Compressors.Zstd.decompress(compressedStr)

// ByteBuffer 지원
val buffer = ByteBuffer.wrap(plainData)
val compressedBuffer = Compressors.Snappy.compress(buffer)

// InputStream 지원
val inputStream = File("large-file.txt").inputStream()
val compressedStream = Compressors.GZip.compress(inputStream)
```

**압축 알고리즘 선택 가이드:**

- **실시간 처리**: LZ4, Snappy (압축률 < 속도)
- **네트워크 전송**: Zstd, GZip (속도 + 압축률 균형)
- **저장 공간 최적화**: BZip2, Zstd (압축률 > 속도)

### 2. 직렬화 (BinarySerializer)

객체를 바이너리로 직렬화/역직렬화하는 다양한 구현체를 제공합니다.

**지원 직렬화:**

- **Jdk**: Java 표준 직렬화 (호환성 최고)
- **Kryo**: 빠르고 효율적인 바이너리 직렬화
- **Fory**: Apache Fory 기반 Kotlin 최적화 직렬화
- **Compressable**: 직렬화 + 압축 조합 (예: LZ4Kryo, ZstdFory)

```kotlin
import io.bluetape4k.io.serializer.BinarySerializers

data class User(val id: Long, val name: String, val email: String)

// Kryo 직렬화 (빠른 속도)
val serializer = BinarySerializers.Kryo
val user = User(1L, "John Doe", "john@example.com")
val bytes = serializer.serialize(user)
val restored = serializer.deserialize<User>(bytes)

// 직렬화 + 압축 (저장 공간 절약)
val compressedSerializer = BinarySerializers.LZ4Kryo
val compressedBytes = compressedSerializer.serialize(user)
// 원본보다 50-70% 작은 크기

// Fory 직렬화 (최신, 고성능)
val forySerializer = BinarySerializers.Fory
val foryBytes = forySerializer.serialize(user)
```

**직렬화 방식 선택 가이드:**

- **호환성 우선**: Jdk (모든 Java 환경)
- **성능 우선**: Kryo, Fory (3-10배 빠름)
- **저장 공간 절약**: LZ4Kryo, ZstdFory (압축 포함)

### 3. Okio 통합 (비동기 I/O)

Square의 Okio 라이브러리를 Kotlin Coroutines와 통합하여 비동기 I/O를 제공합니다.

**주요 기능:**

- Suspended Source/Sink (Coroutine 기반)
- File/Socket Channel 지원
- 암호화/복호화 스트림
- Base64 인코딩/디코딩
- 압축 스트림

```kotlin
import io.bluetape4k.io.okio.coroutines.*
import okio.Buffer
import java.nio.file.Paths

// Suspended 파일 읽기
suspend fun readFileAsync(path: String): ByteArray {
    val source = SuspendedFileChannelSource(Paths.get(path))
    val buffer = Buffer()
    source.use {
        it.readAll(buffer)
    }
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

// 압축 스트림
import io . bluetape4k . io . okio . compress . *
        import io . bluetape4k . io . compressor . Compressors

val sink = /* ... */
val compressedSink = CompressableSink(sink, Compressors.Zstd)
compressedSink.write(buffer)

// 암호화 스트림
import io . bluetape4k . io . okio . cipher . *
        import javax . crypto . Cipher

val cipher = /* Cipher 초기화 */
val encryptedSink = CipherSink(sink, cipher)
encryptedSink.write(buffer)
```

### 4. 파일 유틸리티 (FileSupport)

파일 처리를 위한 편리한 확장 함수들을 제공합니다.

```kotlin
import io.bluetape4k.io.*
import java.io.File
import java.nio.file.Paths

// 비동기 파일 복사
val source = File("source.txt")
val target = File("target.txt")
source.copyToAsync(target).thenAccept {
    println("Copy completed: ${it.absolutePath}")
}

// 비동기 파일 이동
source.moveAsync(target).thenAccept {
    println("Move completed")
}

// 비동기 파일 읽기
val path = Paths.get("large-file.txt")
path.readAllBytesAsync().thenAccept { bytes ->
    println("Read ${bytes.size} bytes")
}

// 비동기 파일 쓰기
val lines = listOf("Line 1", "Line 2", "Line 3")
path.writeLinesAsync(lines).thenAccept { bytesWritten ->
    println("Wrote $bytesWritten bytes")
}

// 라인 단위 스트리밍 (메모리 효율적)
File("huge-file.txt").readLineSequence().forEach { line ->
    processLine(line)
}

// 디렉토리 생성
val dir = createDirectory("temp/sub/folder")

// 임시 디렉토리 (자동 삭제)
val tempDir = createTempDirectory(deleteAtExit = true)

// 디렉토리 재귀 삭제
File("temp").deleteDirectoryRecursively()
```

## 의존성 추가

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-io:${version}")

    // 선택적 의존성 (필요한 것만 추가)

    // 압축 알고리즘
    implementation("org.lz4:lz4-java:1.8.0")              // LZ4
    implementation("org.xerial.snappy:snappy-java:1.1.10.8") // Snappy
    implementation("com.github.luben:zstd-jni:1.5.7-6")     // Zstd
    implementation("org.apache.commons:commons-compress:1.26.0") // BZip2, GZip

    // 직렬화
    implementation("com.esotericsoftware:kryo:5.6.2")     // Kryo
    implementation("org.apache.fury:fury-kotlin:0.14.1")     // Fory
}
```

### Maven

```xml

<dependency>
    <groupId>io.bluetape4k</groupId>
    <artifactId>bluetape4k-io</artifactId>
    <version>${bluetape4k.version}</version>
</dependency>

        <!-- 선택적 의존성 -->
<dependency>
<groupId>org.lz4</groupId>
<artifactId>lz4-java</artifactId>
<version>1.8.0</version>
</dependency>
```

## 성능 특성

### 압축 알고리즘 비교 (1MB 텍스트 기준)

| 알고리즘   | 압축 속도    | 해제 속도    | 압축률    | 용도      |
|--------|----------|----------|--------|---------|
| LZ4    | ~500MB/s | ~2GB/s   | 50-60% | 실시간 처리  |
| Snappy | ~400MB/s | ~1.5GB/s | 50-60% | 범용      |
| Zstd   | ~200MB/s | ~800MB/s | 65-75% | 균형 (추천) |
| GZip   | ~50MB/s  | ~200MB/s | 65-75% | 호환성     |
| BZip2  | ~10MB/s  | ~30MB/s  | 75-85% | 최대 압축   |

### 직렬화 방식 비교

| 방식   | 속도    | 크기   | 호환성   | 특징      |
|------|-------|------|-------|---------|
| Jdk  | 1x    | 100% | ⭐⭐⭐⭐⭐ | Java 표준 |
| Kryo | 3-5x  | 50%  | ⭐⭐⭐   | 빠르고 작음  |
| Fory | 5-10x | 40%  | ⭐⭐    | 최고 성능   |

## Coroutines 지원

모든 비동기 I/O 작업은 Kotlin Coroutines를 완벽하게 지원합니다.

```kotlin
import kotlinx.coroutines.*
import io.bluetape4k.io.okio.coroutines.*

suspend fun processLargeFile(path: String) = coroutineScope {
    val source = SuspendedFileChannelSource(Paths.get(path))
    val buffer = Buffer()

    source.use {
        // Non-blocking 읽기
        val bytesRead = it.read(buffer, 8192)
        processData(buffer.readByteArray())
    }
}

// 병렬 파일 처리
suspend fun processMultipleFiles(files: List<String>) = coroutineScope {
    files.map { path ->
        async(Dispatchers.IO) {
            processLargeFile(path)
        }
    }.awaitAll()
}
```

## Virtual Threads 지원 (Java 21+)

Virtual Threads를 활용한 경량 스레드 기반 비동기 처리를 지원합니다.

```kotlin
import io.bluetape4k.io.*

// Virtual Thread로 비동기 실행
val future = file.copyToAsync(target)
future.thenAccept { copiedFile ->
    println("Copied: ${copiedFile.name}")
}

// CompletableFuture 조합
val readFuture = path.readAllBytesAsync()
val writeFuture = readFuture.thenCompose { bytes ->
    processedPath.writeAsync(bytes)
}
```

## 테스트

프로젝트는 포괄적인 테스트를 포함합니다:

```bash
# 모든 테스트 실행
./gradlew :bluetape4k-io:test

# 벤치마크 실행 (압축/직렬화 성능 측정)
./gradlew :bluetape4k-io:benchmark
```

## 모듈 구조

```
io.bluetape4k.io
├── compressor/          # 압축 알고리즘
│   ├── Compressor.kt
│   ├── Compressors.kt
│   └── [각종 구현체]
├── serializer/          # 직렬화
│   ├── BinarySerializer.kt
│   ├── BinarySerializers.kt
│   └── [각종 구현체]
├── okio/               # Okio 통합
│   ├── coroutines/     # Suspended I/O
│   ├── channels/       # Channel 지원
│   ├── cipher/         # 암호화
│   ├── compress/       # 압축 스트림
│   └── jasypt/         # Jasypt 암호화
├── FileSupport.kt      # 파일 유틸리티
├── PathSupport.kt      # Path 유틸리티
└── [기타 확장 함수들]
```

## 라이선스

Apache License 2.0

## 참고

- [Okio Documentation](https://square.github.io/okio/)
- [Kryo Documentation](https://github.com/EsotericSoftware/kryo)
- [Apache Fory](https://fory.apache.org/)
- [LZ4 for Java](https://github.com/lz4/lz4-java)
