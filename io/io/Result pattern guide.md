# Result 패턴 파일 유틸리티 (tryXXXX)

## 개요

`FileSupportResult.kt`는 기존 nullable 반환 파일 유틸리티의 **안전한 대안**을 제공합니다. Go 언어 스타일의 `tryXXXX` 네이밍으로 `Result<T>` 반환을 명시합니다.

## 기본 사용법

### 1. 디렉토리 생성

```kotlin
import io.bluetape4k.io.*

// 기존 방식 (null 반환)
val dir = createDirectory("/tmp/data")
if (dir != null) {
    println("Created: ${dir.absolutePath}")
}

// Result 방식 (실패 원인 파악 가능)
tryCreateDirectory("/tmp/data").fold(
    onSuccess = { dir ->
        println("Created: ${dir.absolutePath}")
    },
    onFailure = { error ->
        logger.error("Failed to create directory", error)
        // IOException 상세 정보 활용 가능
    }
)
```

### 2. 파일 읽기/쓰기

```kotlin
import java.nio.file.Paths

val path = Paths.get("config.json")

// 파일 읽기
tryReadAllBytes(path).fold(
    onSuccess = { bytes -> processData(bytes) },
    onFailure = { error -> handleError(error) }
)

// 파일 쓰기
val data = "Hello, World!".toByteArray()
path.tryWriteBytes(data).fold(
    onSuccess = { bytesWritten -> println("Wrote $bytesWritten bytes") },
    onFailure = { error -> logger.error("Write failed", error) }
)
```

## 함수형 스타일

### map / mapCatching

```kotlin
// 파일 읽기 -> 파싱 -> 변환 체인
val result = path.tryReadAllBytes()
    .map { bytes -> bytes.decodeToString() }
    .map { json -> parseJson(json) }
    .map { config -> config.databaseUrl }

val dbUrl = result.getOrElse("default-db-url")
```

### getOrElse / getOrDefault

```kotlin
// 실패 시 기본값 반환
val config = path.tryReadAllBytes()
    .map { it.decodeToString() }
    .getOrElse { "{}" }

// 실패 시 null 반환
val data = path.tryReadAllBytes().getOrNull()
```

### recoverCatching

```kotlin
// 실패 시 복구 시도
val content = path.tryReadAllBytes()
    .recoverCatching { error ->
        // 백업 파일에서 읽기 시도
        Paths.get("backup.txt").tryReadAllBytes().getOrThrow()
    }
    .getOrElse { ByteArray(0) }
```

## 비동기 작업

### CompletableFuture + Result

```kotlin
import kotlinx.coroutines.future.await

// 파일 복사 (비동기)
suspend fun copyFileAsync(source: File, target: File) {
    val result = source.tryCopyToAsync(target).await()

    result.fold(
        onSuccess = { file ->
            println("Copied to: ${file.absolutePath}")
        },
        onFailure = { error ->
            logger.error("Copy failed", error)
        }
    )
}

// 파일 읽기 (비동기)
suspend fun readFileAsync(path: Path): ByteArray? {
    return path.tryReadAllBytesAsync()
        .await()
        .getOrNull()
}
```

### 병렬 처리

```kotlin
import kotlinx.coroutines.*

suspend fun processMultipleFiles(paths: List<Path>) = coroutineScope {
    val results = paths.map { path ->
        async {
            path.tryReadAllBytesAsync().await()
        }
    }.awaitAll()

    // 성공한 것만 처리
    results.forEach { result ->
        result.onSuccess { bytes ->
            processData(bytes)
        }
    }
}
```

## 실전 패턴

### 1. 설정 파일 로딩

```kotlin
data class AppConfig(val dbUrl: String, val apiKey: String)

fun loadConfig(path: String): Result<AppConfig> {
    return tryCreateFile(path)
        .mapCatching { file ->
            file.readText()
        }
        .mapCatching { json ->
            parseJson<AppConfig>(json)
        }
}

// 사용
val config = loadConfig("/etc/app/config.json").fold(
    onSuccess = { it },
    onFailure = {
        logger.warn("Failed to load config, using defaults")
        AppConfig("localhost", "default-key")
    }
)
```

### 2. 안전한 파일 삭제

```kotlin
fun cleanupTempFiles(tempDir: File): Result<Int> = runCatching {
    var deletedCount = 0

    tempDir.listFiles()?.forEach { file ->
        file.tryDeleteIfExists().onSuccess {
            if (it) deletedCount++
        }
    }

    deletedCount
}

// 사용
cleanupTempFiles(File("/tmp/app")).fold(
    onSuccess = { count -> println("Deleted $count files") },
    onFailure = { error -> logger.error("Cleanup failed", error) }
)
```

### 3. 로그 파일 로테이션

```kotlin
suspend fun rotateLogFile(
    current: Path,
    archive: Path
): Result<Path> = runCatching {
    // 현재 로그 파일을 아카이브로 이동
    current.toFile()
        .tryMoveAsync(archive.toFile())
        .await()
        .getOrThrow()

    // 새 로그 파일 생성
    tryCreateFile(current.toString()).getOrThrow()

    current
}
```

### 4. 대용량 파일 스트리밍

```kotlin
fun processLargeFile(path: Path): Result<Long> = runCatching {
    var processedLines = 0L

    path.toFile().useLines { lines ->
        lines.forEach { line ->
            processLine(line)
            processedLines++
        }
    }

    processedLines
}
```

### 5. 트랜잭션 패턴

```kotlin
suspend fun atomicFileUpdate(
    path: Path,
    transform: (String) -> String
): Result<Unit> = runCatching {
    // 임시 파일에 쓰기
    val temp = Files.createTempFile("update", ".tmp")

    try {
        // 원본 읽기 -> 변환 -> 임시 파일에 쓰기
        val content = path.tryReadAllBytes().getOrThrow()
        val transformed = transform(content.decodeToString())
        temp.tryWriteBytes(transformed.toByteArray()).getOrThrow()

        // 임시 파일을 원본으로 이동 (atomic)
        temp.toFile()
            .tryMoveAsync(path.toFile(), overwrite = true)
            .await()
            .getOrThrow()
    } catch (e: Exception) {
        // 실패 시 임시 파일 삭제
        temp.toFile().tryDeleteIfExists()
        throw e
    }
}

// 사용
atomicFileUpdate(Paths.get("data.json")) { content ->
    updateJson(content)
}.fold(
    onSuccess = { println("Updated successfully") },
    onFailure = { error -> logger.error("Update failed, rollback complete", error) }
)
```

## 기존 API와 비교

| 기존 API                                   | Result API                                          | 차이점            |
|------------------------------------------|-----------------------------------------------------|----------------|
| `createDirectory(path): File?`           | `tryCreateDirectory(path): Result<File>`            | 실패 원인 파악 가능    |
| `readAllBytesAsync(path): CF<ByteArray>` | `tryReadAllBytesAsync(path): CF<Result<ByteArray>>` | 예외를 Result로 래핑 |
| `file.copyToAsync(target): CF<File>`     | `file.tryCopyToAsync(target): CF<Result<File>>`     | 안전한 비동기 처리     |

## 언제 사용할까?

### tryXXXX 사용 (Result 패턴)

- 실패 원인이 중요한 경우
- 에러 핸들링이 필요한 경우
- 함수형 체이닝을 활용하는 경우
- 로깅/모니터링이 필요한 경우

### 기존 API 사용 (nullable)

- 빠른 프로토타이핑
- 실패 원인이 중요하지 않은 경우
- Elvis 연산자로 충분한 경우

## 성능 고려사항

Result 패턴은 예외를 캡처하므로 약간의 오버헤드가 있습니다:

```kotlin
// 성능이 중요한 루프에서는 기존 API 사용
repeat(10_000) {
    val dir = createDirectory("/tmp/test-$it")  // 더 빠름
}

// 안정성이 중요한 경우 Result API 사용
val results = (0..10_000).map {
    tryCreateDirectory("/tmp/test-$it")  // 더 안전
}
```

## 마이그레이션 가이드

기존 코드를 점진적으로 마이그레이션할 수 있습니다:

```kotlin
// Before
val dir = createDirectory("/data")
    ?: throw IllegalStateException("Failed to create directory")

// After (Step 1: try 버전으로 변경)
val dir = tryCreateDirectory("/data")
    .getOrElse { throw IllegalStateException("Failed to create directory") }

// After (Step 2: 에러 핸들링 개선)
val dir = tryCreateDirectory("/data").fold(
    onSuccess = { it },
    onFailure = { error ->
        logger.error("Directory creation failed", error)
        throw IllegalStateException("Failed to create directory", error)
    }
)

// After (Step 3: 함수형 스타일)
val dir = tryCreateDirectory("/data")
    .recoverCatching { createDirectory("/fallback-data")!! }
    .getOrThrow()
```
