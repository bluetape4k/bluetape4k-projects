# Issue #1645 원자적 파일 교체 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-io`에 provider가 stream 종료와 sibling temporary file cleanup을 소유하는 `Path.writeAtomically` 공개 API를 추가한다.

**Architecture:** 공개 top-level extension은 stateless `AtomicFileWriter`에 위임한다. production operations는 JDK `Files`만 사용하고, internal operations seam은 단계별 실패와 move option을 결정적으로 검증한다. 일반 move fallback, 경로 sandbox, payload 검증, logging과 crash orphan 정리는 제공하지 않는다.

**Tech Stack:** Kotlin/JVM, Java NIO `Files`, JUnit 5, Java 25, Gradle, 기존 `bluetape4k-assertions`/JUnit 인프라

---

## 기준과 실행 경계

- 승인 사양: `docs/superpowers/specs/2026-09-06-issue-1645-atomic-file-replacement-design.md`
- provider 기준: `bluetape4k-projects@871f73aa8d464a01b9e71350e8220619c548684c`
- upstream 기준: `origin/develop@31ee966cf339f7b895284329c8fbd53638ab837c`
- 공개 API: `@file:JvmName("AtomicFileSupport")`와 `Path.writeAtomically((OutputStream) -> Unit): Long`
- 새 dependency, module, catalog alias, workflow, Kover 등록은 없다.
- `ATOMIC_MOVE`에서 기존 target 교체 여부는 provider 계약이다. 거부되면 예외를 전파하고 일반 move fallback을 사용하지 않는다.
- source와 parent는 신뢰된 호출자가 관리한다. byte/time/quota, checksum, domain validation, logging과 orphan cleanup은 호출자 책임이다.
- Projects merge와 `2.1.0-SNAPSHOT` 발행, Image #631 migration은 이 구현 계획의 외부 gate다.

## 파일 구조

| 경로 | 책임 |
|---|---|
| `io/io/src/main/kotlin/io/bluetape4k/io/AtomicFileSupport.kt` | 공개 extension, counting stream, JDK operations, failure/cleanup 정책 |
| `io/io/src/test/kotlin/io/bluetape4k/io/AtomicFileSupportTest.kt` | 실제 filesystem success, path, cleanup, concurrency 계약 |
| `io/io/src/test/kotlin/io/bluetape4k/io/AtomicFileFailurePolicyTest.kt` | 단계별 failure seam, close/move/cleanup 순서와 Throwable graph |
| `io/io/src/test/kotlin/io/bluetape4k/io/readme/AtomicFileReadmeKotlinContractTest.kt` | README Kotlin import, bounded copy와 coroutine 예제 compile/실행 |
| `io/io/src/test/java/io/bluetape4k/io/readme/AtomicFileSupportJavaContractTest.java` | README Java import, facade, checked exception, descriptor |
| `io/io/README.md` | 영어 사용법과 provider/ownership/운영 경계 |
| `io/io/README.ko.md` | 한국어 사용법과 영어 문서의 구조적 동등성 |
| `CHANGELOG.md` | 미출시 공개 API 추가 기록 |
| `docs/lessons/2026-09-06-issue-1645-atomic-file-replacement.md` | 재사용 가능한 provider·failure·운영 교훈 |
| `docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md` | gate와 RED/GREEN/검증 evidence |

## 위험, rollback, 재검토 지점

| 위험 | 계획상 방어 | 재검토 지점 |
|---|---|---|
| provider가 기존 target atomic replace를 거부 | 두 move option을 전달하되 예외를 그대로 전파하고 fallback 금지 | Task 2 failure seam과 실제 default provider test |
| callback·close·cleanup 동시 실패에서 primary 유실 | identity 기반 순환 검사 후 close, cleanup 순서로 suppressed 연결 | Task 2 Throwable graph test |
| commit 성공 뒤 cleanup lookup이 성공을 실패로 변경 | `committed=true` 뒤에는 `deleteIfExists`를 호출하지 않음 | Task 2 success seam |
| counting을 위해 target metadata 재조회 | provider-owned counting stream만 사용 | Task 1 mixed-write count와 operations surface 검토 |
| concurrent writer가 전역 직렬화되거나 partial target 생성 | lock 없는 stateless writer, barrier/latch 실제 filesystem test | Task 1 concurrency test |
| filesystem별 수치가 다른 microbenchmark를 공개 성능 계약으로 오해 | per-call writer 재사용, bounded contention/visibility stress만 acceptance로 사용하고 절대 throughput 수치는 주장하지 않음 | Task 1 stress test와 Task 4 fresh rerun |
| Java facade/checked exception drift | 실제 Java source test, reflection, `javap -s` | Task 3 ABI 검증 |
| 기존 target metadata를 보존한다고 오해 | KDoc와 README locale pair에 명시적 비보장 기록 | Task 3 문서 검토 |

발행 전 rollback은 branch의 lesson/review, README/CHANGELOG, implementation commit을
역순으로 모두 revert한다. merge 뒤에는 squash merge commit 하나를 되돌리는 corrective
PR을 만든다. `git revert` 기록으로 review/checklist 이력을 보존하며, 일부 commit만
되돌려 문서가 제거된 API를 계속 가리키는 상태를 허용하지 않는다. 이미 발행한 개발 버전
artifact는 삭제하거나 같은 좌표를 덮어썼다고 가정하지 않는다. 수정 artifact의 새 발행 근거가 생길
때까지 Image #631을 차단한다. revert 뒤에는
`rg -n 'writeAtomically|AtomicFileSupport' io/io/src io/io/README.md io/io/README.ko.md CHANGELOG.md`,
`./gradlew :bluetape4k-io:test --no-build-cache --no-configuration-cache --console=plain`,
`git diff --check`로 source와 문서 참조, module 회귀를 확인하고 checklist의 실제 상태를
다시 기록한다. 기존 API와 module registration은 변경하지 않으므로 소비자 rollback
migration은 필요 없다. Image migration은 remote `2.1.0-SNAPSHOT` provenance가
확인될 때까지 시작하지 않는다.

### Task 1: 공개 계약과 실제 filesystem 회귀를 RED로 고정

**Files:**
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/AtomicFileSupportTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/readme/AtomicFileReadmeKotlinContractTest.kt`
- Create: `io/io/src/test/java/io/bluetape4k/io/readme/AtomicFileSupportJavaContractTest.java`

- [x] **Step 1: Kotlin 공개 계약 테스트 작성**

`AtomicFileSupportTest.kt`에 다음 완전한 test fixture를 작성한다.

```kotlin
package io.bluetape4k.io

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AtomicFileSupportTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `새 target을 교체하고 provider가 기록한 byte 수를 반환한다`() {
        val target = tempDir.resolve("nested/result.bin")
        val first = "hello".toByteArray()
        val second = " world".toByteArray()

        val written = target.writeAtomically { output ->
            output.write(first)
            output.write(second)
            output.write('!'.code)
        }

        assertEquals(first.size + second.size + 1L, written)
        assertArrayEquals("hello world!".toByteArray(), Files.readAllBytes(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `default provider에서 기존 target을 완전한 새 payload로 교체한다`() {
        val target = tempDir.resolve("replace.bin")
        Files.writeString(target, "old payload")

        target.writeAtomically { it.write("new".toByteArray()) }

        assertEquals("new", Files.readString(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `callback 실패는 같은 instance로 전파하고 기존 target과 cleanup을 보존한다`() {
        val target = tempDir.resolve("callback.bin")
        Files.writeString(target, "old")
        val expected = IllegalStateException("callback")

        val actual = assertThrows<IllegalStateException> {
            target.writeAtomically { output ->
                output.write("partial".toByteArray())
                throw expected
            }
        }

        assertSame(expected, actual)
        assertEquals("old", Files.readString(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `빈 receiver와 filesystem root를 거부한다`() {
        assertThrows<IllegalArgumentException> { Path.of("").writeAtomically {} }
        assertThrows<IllegalArgumentException> { tempDir.root.writeAtomically {} }
    }

    @Test
    fun `후속 실패에서도 자동 생성한 parent는 남는다`() {
        val target = tempDir.resolve("created/parent/result.bin")

        assertThrows<IllegalArgumentException> {
            target.writeAtomically { throw IllegalArgumentException("reject") }
        }

        assertTrue(Files.isDirectory(target.parent))
        assertFalse(Files.exists(target))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `서로 다른 target callback은 전역 직렬화 없이 동시에 진입한다`() {
        val entered = CountDownLatch(2)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val targets = listOf(tempDir.resolve("a.bin"), tempDir.resolve("b.bin"))
        val futures = targets.mapIndexed { index, target ->
            executor.submit<Long> {
                target.writeAtomically { output ->
                    entered.countDown()
                    assertTrue(release.await(5, TimeUnit.SECONDS))
                    output.write("payload-$index".toByteArray())
                }
            }
        }

        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            release.countDown()
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }

        targets.forEach(::assertNoSiblingTemps)
    }

    @Test
    fun `같은 target의 동시 write는 완전한 payload 하나만 남긴다`() {
        val target = tempDir.resolve("same.bin")
        val payloads = listOf(ByteArray(4096) { 0x11 }, ByteArray(8192) { 0x22 })
        val staged = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)
        val futures = payloads.map { payload ->
            executor.submit<Long> {
                target.writeAtomically { output ->
                    output.write(payload)
                    staged.await(5, TimeUnit.SECONDS)
                }
            }
        }

        try {
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }

        val actual = Files.readAllBytes(target)
        assertTrue(payloads.any(actual::contentEquals))
        assertNoSiblingTemps(target)
    }

    @Test
    fun `bounded contention 중 reader는 완전한 payload만 관측하고 temp가 남지 않는다`() {
        val target = tempDir.resolve("contended.bin")
        val payloads = (1..4).map { marker -> ByteArray(32 * 1024) { marker.toByte() } }
        Files.write(target, payloads.first())
        val reading = AtomicBoolean(true)
        val partialSizes = ConcurrentLinkedQueue<Int>()
        val executor = Executors.newFixedThreadPool(payloads.size + 1)
        val reader = executor.submit<Unit> {
            while (reading.get()) {
                val actual = Files.readAllBytes(target)
                if (payloads.none(actual::contentEquals)) partialSizes.add(actual.size)
            }
        }
        val writers = payloads.map { payload ->
            executor.submit<Unit> {
                repeat(12) {
                    target.writeAtomically { output ->
                        var offset = 0
                        while (offset < payload.size) {
                            val length = minOf(1024, payload.size - offset)
                            output.write(payload, offset, length)
                            offset += length
                        }
                    }
                }
            }
        }

        try {
            writers.forEach { it.get(15, TimeUnit.SECONDS) }
            reading.set(false)
            reader.get(5, TimeUnit.SECONDS)
            assertTrue(partialSizes.isEmpty(), "partial sizes=$partialSizes")
        } finally {
            reading.set(false)
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }

        assertTrue(payloads.any(Files.readAllBytes(target)::contentEquals))
        assertNoSiblingTemps(target)
    }

    private fun assertNoSiblingTemps(target: Path) {
        val prefix = ".${target.fileName}."
        Files.list(target.parent).use { siblings ->
            assertFalse(siblings.anyMatch { path ->
                val name = path.fileName.toString()
                name.startsWith(prefix) && name.endsWith(".tmp")
            })
        }
    }
}
```

- [x] **Step 2: README Kotlin 예제를 외부 package에서 그대로 compile하고 실행**

`AtomicFileReadmeKotlinContractTest.kt`는 README의 extension import, bounded copy와 coroutine
helper를 같은 형태로 포함한다. 한도 초과가 target을 commit하지 않는지와 coroutine helper가
byte 수와 payload를 보존하는지를 실행한다.

```kotlin
package io.bluetape4k.io.readme

import io.bluetape4k.io.writeAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class AtomicFileReadmeKotlinContractTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `bounded copy example은 한도 초과 target을 commit하지 않는다`() {
        val source = tempDir.resolve("oversized-input.bin")
        val destination = tempDir.resolve("bounded-output.bin")
        val maxPayloadBytes = 8L * 1024
        Files.write(source, ByteArray(maxPayloadBytes.toInt() + 1) { 0x2A })

        assertThrows<IllegalArgumentException> {
            destination.writeAtomically { output ->
                Files.newInputStream(source).use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        require(copied + read <= maxPayloadBytes) { "payload exceeds configured limit" }
                        output.write(buffer, 0, read)
                        copied += read
                    }
                }
            }
        }

        assertFalse(Files.exists(destination))
    }

    @Test
    fun `coroutine example은 dispatcher와 cancellation 검사를 호출자가 소유한다`() = runBlocking {
        val source = tempDir.resolve("coroutine-input.bin")
        val destination = tempDir.resolve("coroutine-output.bin")
        val payload = "coroutine-content".toByteArray()
        Files.write(source, payload)

        assertEquals(payload.size.toLong(), copyAtomically(source, destination))
        assertArrayEquals(payload, Files.readAllBytes(destination))
    }

    private suspend fun copyAtomically(source: Path, destination: Path): Long {
        val callerContext = currentCoroutineContext()
        return withContext(Dispatchers.IO) {
            destination.writeAtomically { output ->
                Files.newInputStream(source).use { input -> input.copyTo(output) }
                callerContext.ensureActive()
            }
        }
    }
}
```

- [x] **Step 3: Java caller와 facade 계약 테스트 작성**

`AtomicFileSupportJavaContractTest.java`를 외부 package에 다음 내용으로 작성한다. README와
같이 facade를 import하고 enclosing method가 checked `IOException`을 선언한다.

```java
package io.bluetape4k.io.readme;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.bluetape4k.io.AtomicFileSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFileSupportJavaContractTest {

    @TempDir
    Path tempDir;

    @Test
    void javaCallerUsesAtomicFileSupportFacade() throws IOException {
        Path target = tempDir.resolve("java.bin");
        byte[] payload = "java-caller".getBytes();

        long written = write(target, payload);

        assertEquals(payload.length, written);
        assertArrayEquals(payload, Files.readAllBytes(target));
    }

    private static long write(Path destination, byte[] payload) throws IOException {
        return AtomicFileSupport.writeAtomically(destination, output -> {
            try {
                output.write(payload);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
            return Unit.INSTANCE;
        });
    }

    @Test
    void facadeDeclaresTheExpectedDescriptorAndIOException() throws NoSuchMethodException {
        Method method = AtomicFileSupport.class.getMethod(
            "writeAtomically",
            Path.class,
            Function1.class
        );

        assertEquals(long.class, method.getReturnType());
        assertArrayEquals(new Class<?>[] {IOException.class}, method.getExceptionTypes());
    }
}
```

- [x] **Step 4: 신규 API 부재로 RED 확인**

Run:

```bash
./gradlew :bluetape4k-io:test \
  --tests "io.bluetape4k.io.AtomicFileSupportTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileReadmeKotlinContractTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileSupportJavaContractTest" \
  --no-build-cache --no-configuration-cache --console=plain
```

Expected: `AtomicFileSupport`, `writeAtomically` unresolved reference 또는 symbol-not-found로
`compileTestKotlin`/`compileTestJava`가 실패한다. 명령과 정확한 실패를 checklist RED
evidence에 기록하고 아직 commit하지 않는다.

### Task 2: 단계별 실패 테스트를 추가하고 최소 구현으로 GREEN 달성

**Files:**
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/AtomicFileFailurePolicyTest.kt`
- Create: `io/io/src/main/kotlin/io/bluetape4k/io/AtomicFileSupport.kt`
- Modify: `docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md`

- [x] **Step 1: internal operations와 failure topology 테스트 작성**

`AtomicFileFailurePolicyTest.kt`에 다음 fixture를 작성한다.

```kotlin
package io.bluetape4k.io

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.CopyOption
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CancellationException

class AtomicFileFailurePolicyTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `relative target은 absolute normalized parent로 해석한다`() {
        val requested = Path.of("build/atomic-file-support/../atomic-target.bin")
        val expected = IOException("stop after normalization")
        val operations = RecordingAtomicFileOperations().apply {
            createDirectoriesFailure = expected
        }

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(requested) {}
        }

        assertSame(expected, actual)
        assertEquals(requested.toAbsolutePath().normalize().parent, operations.createdDirectory)
        assertEquals(0, operations.tempCalls)
    }

    @Test
    fun `move 전에 stream을 닫고 정확한 atomic option만 전달한다`() {
        val operations = RecordingAtomicFileOperations()
        val target = tempDir.resolve("nested/../success.bin")

        val written = AtomicFileWriter(operations).write(target) { output ->
            output.write(byteArrayOf(1, 2))
            output.write(3)
        }

        assertEquals(3L, written)
        assertTrue(operations.closedBeforeMove)
        assertEquals(
            listOf(StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING),
            operations.moveOptions,
        )
        assertEquals(target.toAbsolutePath().normalize(), operations.lastMoveTarget)
        assertEquals(0, operations.deleteCalls)
    }

    @Test
    fun `parent 생성 실패 뒤에는 후속 단계와 writer를 실행하지 않는다`() {
        val expected = IOException("parent")
        val operations = RecordingAtomicFileOperations().apply { createDirectoriesFailure = expected }
        var writerCalled = false

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { writerCalled = true }
        }

        assertSame(expected, actual)
        assertFalse(writerCalled)
        assertEquals(0, operations.tempCalls)
        assertEquals(0, operations.openCalls)
        assertEquals(0, operations.moveCalls)
    }

    @Test
    fun `temp 생성 실패 뒤에는 stream과 writer를 실행하지 않는다`() {
        val expected = IOException("temp")
        val operations = RecordingAtomicFileOperations().apply { createTempFailure = expected }
        var writerCalled = false

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { writerCalled = true }
        }

        assertSame(expected, actual)
        assertFalse(writerCalled)
        assertEquals(0, operations.openCalls)
        assertEquals(0, operations.moveCalls)
        assertEquals(0, operations.deleteCalls)
    }

    @Test
    fun `stream open 실패는 staged file을 정리하고 writer와 move를 실행하지 않는다`() {
        val expected = IOException("open")
        val operations = RecordingAtomicFileOperations().apply { openFailure = expected }
        var writerCalled = false

        val actual = assertThrows<IOException> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { writerCalled = true }
        }

        assertSame(expected, actual)
        assertFalse(writerCalled)
        assertEquals(0, operations.moveCalls)
        assertEquals(1, operations.deleteCalls)
    }

    @Test
    fun `stream open의 unchecked failure identity를 보존하고 staged file을 정리한다`() {
        val expected = AssertionError("open")
        val operations = RecordingAtomicFileOperations().apply { openFailure = expected }

        val actual = assertThrows<AssertionError> {
            AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) {}
        }

        assertSame(expected, actual)
        assertEquals(0, operations.moveCalls)
        assertEquals(1, operations.deleteCalls)
    }

    @Test
    fun `callback close cleanup 실패의 identity와 suppressed 순서를 보존한다`() {
        val callback = IllegalStateException("callback")
        val close = IOException("close")
        val cleanup = IOException("cleanup")
        val operations = RecordingAtomicFileOperations().apply {
            closeFailure = close
            deleteFailure = cleanup
        }

        try {
            val actual = assertThrows<IllegalStateException> {
                AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { throw callback }
            }

            assertSame(callback, actual)
            assertArrayEquals(arrayOf(close, cleanup), actual.suppressed)
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `callback 성공 뒤 close 실패는 primary가 되고 staged file을 정리한다`() {
        val close = IOException("close")
        val cleanup = IOException("cleanup")
        val target = tempDir.resolve("close.bin")
        Files.writeString(target, "old")
        val operations = RecordingAtomicFileOperations().apply {
            closeFailure = close
            deleteFailure = cleanup
        }

        try {
            val actual = assertThrows<IOException> {
                AtomicFileWriter(operations).write(target) {
                    it.write("complete".toByteArray())
                }
            }

            assertSame(close, actual)
            assertSame(cleanup, actual.suppressed.single())
            assertEquals("old", Files.readString(target))
            assertEquals(0, operations.moveCalls)
            assertEquals(1, operations.deleteCalls)
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `같은 failure instance는 suppressed에 중복 연결하지 않는다`() {
        val primary = IOException("same")
        val operations = RecordingAtomicFileOperations().apply {
            closeFailure = primary
            deleteFailure = primary
        }

        try {
            val actual = assertThrows<IOException> {
                AtomicFileWriter(operations).write(tempDir.resolve("same-failure.bin")) { throw primary }
            }

            assertSame(primary, actual)
            assertTrue(actual.suppressed.isEmpty())
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `cleanup graph가 primary를 가리키면 순환 suppressed를 만들지 않는다`() {
        val callback = IllegalStateException("callback")
        val cleanup = IOException("cleanup", callback)
        val operations = RecordingAtomicFileOperations().apply { deleteFailure = cleanup }

        try {
            val actual = assertThrows<IllegalStateException> {
                AtomicFileWriter(operations).write(tempDir.resolve("target.bin")) { throw callback }
            }

            assertSame(callback, actual)
            assertTrue(actual.suppressed.isEmpty())
        } finally {
            operations.lastTemp?.let(Files::deleteIfExists)
        }
    }

    @Test
    fun `CancellationException과 Error primary identity를 유지한다`() {
        listOf<Throwable>(CancellationException("cancel"), AssertionError("fatal")).forEach { primary ->
            val cleanup = IOException("cleanup")
            val operations = RecordingAtomicFileOperations().apply { deleteFailure = cleanup }
            try {
                val actual = assertThrows<Throwable> {
                    AtomicFileWriter(operations).write(tempDir.resolve("${primary.javaClass.simpleName}.bin")) {
                        throw primary
                    }
                }
                assertSame(primary, actual)
                assertSame(cleanup, actual.suppressed.single())
            } finally {
                operations.lastTemp?.let(Files::deleteIfExists)
            }
        }
    }

    @Test
    fun `atomic move 거부는 fallback 없이 전파하고 기존 target을 보존한다`() {
        val target = tempDir.resolve("existing.bin")
        Files.writeString(target, "old")
        val failures = listOf<Throwable>(
            AtomicMoveNotSupportedException("staged", "target", "unsupported"),
            UnsupportedOperationException("unsupported option"),
            FileAlreadyExistsException(target.toString()),
            IOException("commit"),
        )

        failures.forEach { expected ->
            val operations = RecordingAtomicFileOperations().apply { moveFailure = expected }
            val actual = assertThrows<Throwable> {
                AtomicFileWriter(operations).write(target) { it.write("new".toByteArray()) }
            }

            assertSame(expected, actual)
            assertEquals("old", Files.readString(target))
            assertEquals(1, operations.moveCalls)
            assertEquals(1, operations.deleteCalls)
        }
    }

    private class RecordingAtomicFileOperations: AtomicFileOperations {
        var createDirectoriesFailure: Throwable? = null
        var createTempFailure: Throwable? = null
        var openFailure: Throwable? = null
        var closeFailure: Throwable? = null
        var moveFailure: Throwable? = null
        var deleteFailure: Throwable? = null
        var tempCalls = 0
        var openCalls = 0
        var moveCalls = 0
        var deleteCalls = 0
        var createdDirectory: Path? = null
        var lastTemp: Path? = null
        var lastMoveTarget: Path? = null
        var lastOutput: RecordingOutputStream? = null
        var closedBeforeMove = false
        var moveOptions: List<CopyOption> = emptyList()

        override fun createDirectories(directory: Path): Path {
            createdDirectory = directory
            createDirectoriesFailure?.let { throw it }
            return Files.createDirectories(directory)
        }

        override fun createTempFile(directory: Path, prefix: String, suffix: String): Path {
            tempCalls++
            createTempFailure?.let { throw it }
            return Files.createTempFile(directory, prefix, suffix).also { lastTemp = it }
        }

        override fun openOutput(path: Path): OutputStream {
            openCalls++
            openFailure?.let { throw it }
            return RecordingOutputStream(Files.newOutputStream(path)) { closeFailure }
                .also { lastOutput = it }
        }

        override fun move(source: Path, target: Path, vararg options: CopyOption): Path {
            moveCalls++
            closedBeforeMove = lastOutput?.closed == true
            lastMoveTarget = target
            moveOptions = options.toList()
            moveFailure?.let { throw it }
            return Files.move(source, target, *options)
        }

        override fun deleteIfExists(path: Path): Boolean {
            deleteCalls++
            deleteFailure?.let { throw it }
            return Files.deleteIfExists(path)
        }
    }

    private class RecordingOutputStream(
        delegate: OutputStream,
        private val failure: () -> Throwable?,
    ): FilterOutputStream(delegate) {
        var closed = false
            private set

        override fun close() {
            try {
                super.close()
            } finally {
                closed = true
            }
            failure()?.let { throw it }
        }
    }
}
```

- [x] **Step 2: compile 가능한 최소 skeleton으로 functional RED 확인**

`AtomicFileSupport.kt`에 facade와 seam만 만들고 writer는 의도적으로 실패시킨다.

```kotlin
@file:JvmName("AtomicFileSupport")

package io.bluetape4k.io

import java.io.IOException
import java.io.OutputStream
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path

@Throws(IOException::class)
fun Path.writeAtomically(writer: (OutputStream) -> Unit): Long =
    SystemAtomicFileWriter.write(this, writer)

private val SystemAtomicFileWriter = AtomicFileWriter(SystemAtomicFileOperations)

internal class AtomicFileWriter(
    private val operations: AtomicFileOperations,
) {
    fun write(target: Path, writer: (OutputStream) -> Unit): Long =
        throw UnsupportedOperationException("atomic file replacement is not implemented")
}

internal interface AtomicFileOperations {
    fun createDirectories(directory: Path): Path
    fun createTempFile(directory: Path, prefix: String, suffix: String): Path
    fun openOutput(path: Path): OutputStream
    fun move(source: Path, target: Path, vararg options: CopyOption): Path
    fun deleteIfExists(path: Path): Boolean
}

private object SystemAtomicFileOperations: AtomicFileOperations {
    override fun createDirectories(directory: Path): Path = Files.createDirectories(directory)
    override fun createTempFile(directory: Path, prefix: String, suffix: String): Path =
        Files.createTempFile(directory, prefix, suffix)
    override fun openOutput(path: Path): OutputStream = Files.newOutputStream(path)
    override fun move(source: Path, target: Path, vararg options: CopyOption): Path =
        Files.move(source, target, *options)
    override fun deleteIfExists(path: Path): Boolean = Files.deleteIfExists(path)
}
```

Run:

```bash
./gradlew :bluetape4k-io:test \
  --tests "io.bluetape4k.io.AtomicFileSupportTest" \
  --tests "io.bluetape4k.io.AtomicFileFailurePolicyTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileReadmeKotlinContractTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileSupportJavaContractTest" \
  --no-build-cache --no-configuration-cache --console=plain
```

Expected: source/test compilation은 성공하고 behavior tests가
`UnsupportedOperationException: atomic file replacement is not implemented`로 실패한다.
이 두 번째 RED를 기록한 뒤 skeleton을 최종 구현으로 교체한다.

- [x] **Step 3: stream-close-before-move와 failure-safe cleanup 최소 구현**

`AtomicFileSupport.kt` 전체를 다음 구현으로 교체한다.

```kotlin
@file:JvmName("AtomicFileSupport")

package io.bluetape4k.io

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

/**
 * [writer]가 기록한 내용을 같은 parent의 temporary file에 먼저 쓴 뒤 atomic move로
 * receiver를 교체하고, provider에 성공적으로 기록한 byte 수를 반환합니다.
 *
 * 이 함수는 blocking API입니다. [writer]는 전달받은 stream을 callback 실행 중에만
 * 사용하고 직접 닫거나 보관하지 않아야 합니다. 별도 buffering wrapper는 callback이
 * 반환되기 전에 flush해야 합니다. callback과 stream close가 성공한 뒤에만
 * [StandardCopyOption.ATOMIC_MOVE]를 시도합니다.
 * Coroutine 호출자는 dispatcher를 직접 선택하고, callback 밖에서 캡처한 context의
 * cancellation을 callback이 반환되기 전에 검사해야 합니다.
 *
 * `ATOMIC_MOVE`가 적용되면 다른 move option은 무시될 수 있으므로 기존 target 교체는
 * filesystem provider 계약을 따릅니다. atomic move 거부에는 일반 move fallback을
 * 수행하지 않습니다. callback, close 또는 commit의 unchecked exception,
 * [java.util.concurrent.CancellationException], [Error]는 identity를 유지하며 cleanup
 * 실패만 primary에 suppressed로 연결됩니다.
 *
 * 이 API는 접근이 제한된 신뢰 가능한 parent에서만 사용해야 합니다. 경로 정규화는
 * lexical 처리일 뿐 path sandbox, symlink, hard-link, mount 교체와 TOCTOU를 방어하지
 * 않습니다. 공격자가 제어하는 공유 writable directory에는 사용하지 말고, 그 방어가
 * 필요하면 secure directory handle 기반 API를 선택해야 합니다. 임시 파일명에는 target
 * basename이 포함되고 permission은 provider default이므로, 민감한 payload에는 opaque
 * basename과 private parent를 사용해야 합니다.
 *
 * 기존 target의 권한, owner, ACL, xattr, fsync durability, payload 제한, logging과 crash
 * orphan cleanup은 보장하지 않습니다. 호출자는 target/provider와 primary/suppressed
 * 실패를 관측하되 전체 경로, basename, 예외 메시지의 민감한 값은 가려서 기록해야 합니다.
 *
 * @receiver 유효한 파일명을 가진 target 경로
 * @param writer provider가 소유하는 [OutputStream]에 내용을 기록하는 동기 callback
 * @return callback이 provider stream에 성공적으로 기록한 byte 수
 * @throws IllegalArgumentException receiver에 파일명이 없거나 비어 있을 때
 * @throws IOException parent, temporary file, stream 또는 atomic move provider 호출이 실패할 때
 * @throws Throwable writer, close 또는 commit이 던진 unchecked failure의 identity를 보존할 때
 */
@Throws(IOException::class)
fun Path.writeAtomically(writer: (OutputStream) -> Unit): Long =
    SystemAtomicFileWriter.write(this, writer)

private val SystemAtomicFileWriter = AtomicFileWriter(SystemAtomicFileOperations)

internal class AtomicFileWriter(
    private val operations: AtomicFileOperations,
) {
    fun write(requestedTarget: Path, writer: (OutputStream) -> Unit): Long {
        val fileName = requestedTarget.fileName?.toString()
        require(!fileName.isNullOrEmpty()) { "Target path must have a file name" }

        val target = requestedTarget.toAbsolutePath().normalize()
        val parent = requireNotNull(target.parent) { "Normalized target must have a parent" }
        operations.createDirectories(parent)

        var staged: Path? = null
        var committed = false
        var primaryFailure: Throwable? = null
        try {
            staged = operations.createTempFile(parent, ".$fileName.", ".tmp")
            val output = CountingOutputStream(operations.openOutput(staged))
            val callbackFailure = try {
                writer(output)
                null
            } catch (failure: Throwable) {
                failure
            }

            close(output, callbackFailure)?.let { throw it }
            val written = output.count
            operations.move(
                staged,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            committed = true
            return written
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            if (!committed) {
                staged?.let { temporary ->
                    try {
                        operations.deleteIfExists(temporary)
                    } catch (cleanupFailure: Throwable) {
                        primaryFailure?.attachSuppressedSafely(cleanupFailure) ?: throw cleanupFailure
                    }
                }
            }
        }
    }

    private fun close(output: OutputStream, callbackFailure: Throwable?): Throwable? {
        try {
            output.close()
        } catch (closeFailure: Throwable) {
            if (callbackFailure == null) return closeFailure
            callbackFailure.attachSuppressedSafely(closeFailure)
        }
        return callbackFailure
    }
}

internal interface AtomicFileOperations {
    fun createDirectories(directory: Path): Path
    fun createTempFile(directory: Path, prefix: String, suffix: String): Path
    fun openOutput(path: Path): OutputStream
    fun move(source: Path, target: Path, vararg options: CopyOption): Path
    fun deleteIfExists(path: Path): Boolean
}

private object SystemAtomicFileOperations: AtomicFileOperations {
    override fun createDirectories(directory: Path): Path = Files.createDirectories(directory)

    override fun createTempFile(directory: Path, prefix: String, suffix: String): Path =
        Files.createTempFile(directory, prefix, suffix)

    override fun openOutput(path: Path): OutputStream = Files.newOutputStream(path)

    override fun move(source: Path, target: Path, vararg options: CopyOption): Path =
        Files.move(source, target, *options)

    override fun deleteIfExists(path: Path): Boolean = Files.deleteIfExists(path)
}

private class CountingOutputStream(delegate: OutputStream): FilterOutputStream(delegate) {
    var count: Long = 0L
        private set

    override fun write(value: Int) {
        out.write(value)
        count++
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        out.write(bytes, offset, length)
        count += length
    }
}

private fun Throwable.attachSuppressedSafely(secondary: Throwable) {
    if (this === secondary) return
    if (reaches(secondary) || secondary.reaches(this)) return
    if (suppressed.any { it === secondary }) return
    addSuppressed(secondary)
}

private fun Throwable.reaches(target: Throwable): Boolean {
    val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    val pending = ArrayDeque<Throwable>()
    pending.add(this)

    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        if (!visited.add(current)) continue
        if (current === target) return true
        current.cause?.let(pending::addLast)
        current.suppressed.forEach(pending::addLast)
    }
    return false
}
```

- [x] **Step 4: targeted GREEN 확인**

Run:

```bash
./gradlew :bluetape4k-io:test \
  --tests "io.bluetape4k.io.AtomicFileSupportTest" \
  --tests "io.bluetape4k.io.AtomicFileFailurePolicyTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileReadmeKotlinContractTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileSupportJavaContractTest" \
  --no-build-cache --no-configuration-cache --console=plain
```

Expected: 네 test class의 모든 test가 PASS하고 sibling temporary file이 남지 않는다.
실패 시 test expectation을 약화하지 않고 production lifecycle을 수정한다.

- [x] **Step 5: RED/GREEN evidence와 A-06 진행 상태 기록**

`docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md`에 첫 compile RED,
functional RED, targeted GREEN의 명령·실패 원인·test count를 기록한다.

- [ ] **Step 6: 첫 구현 commit**

```bash
git add \
  io/io/src/main/kotlin/io/bluetape4k/io/AtomicFileSupport.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/AtomicFileSupportTest.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/AtomicFileFailurePolicyTest.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/readme/AtomicFileReadmeKotlinContractTest.kt \
  io/io/src/test/java/io/bluetape4k/io/readme/AtomicFileSupportJavaContractTest.java \
  docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md
git commit
```

Commit intent: `파일 교체 실패에서 기존 target을 지키도록 한다`. Lore trailers에는
provider 제약, non-atomic fallback 거부, RED/GREEN 결과와 아직 실행하지 않은 broader
검증을 기록한다.

### Task 3: 공개 문서와 JVM 계약을 구현에 맞춘다

**Files:**
- Modify: `io/io/README.md:110-113,431-455,541-563`
- Modify: `io/io/README.ko.md:114-117,431-455,541-563`
- Modify: `CHANGELOG.md:82-90`

- [ ] **Step 1: README locale pair에 같은 구조의 계약과 예제 추가**

영어 README의 File Utilities 설명과 예제에 다음 내용을 추가한다.

````markdown
`Path.writeAtomically` writes through a provider-owned sibling temporary file and attempts
`ATOMIC_MOVE` only after the callback and stream close succeed. The callback borrows the
`OutputStream`; it must not close or retain it. Existing-target replacement, temporary-file
permissions, and file attributes follow the filesystem provider. The API does not guarantee
`fsync`, process-crash, or power-loss durability. Unsupported atomic replacement fails without a
non-atomic fallback.

This is a blocking call. It creates missing parents, rejects an empty or root target with
`IllegalArgumentException`, and returns the `Long` byte count accepted by its provider-owned
stream. A parent created by the call remains if a later stage fails. Callback, close, and commit
unchecked exceptions, `CancellationException`, and `Error`
keep their identity; only cleanup failures are attached as suppressed exceptions.
Coroutine callers own dispatcher selection and must check the context captured outside the
callback before the callback returns; cancellation after commit does not roll the replacement back.

Normalization is lexical; it does not provide a path sandbox or protect against symlink,
hard-link, mount-swap, or TOCTOU attacks. Use an opaque basename and an access-restricted private
parent for sensitive payloads. Do not use an attacker-controlled shared writable directory; use
a secure directory-handle API when that threat model applies. Enforce byte and time limits in
the caller, and redact full paths, basenames, and sensitive exception text when recording the
provider plus primary and suppressed cleanup failures.

A process crash can leave `.<basename>.*.tmp` files. The application operator owns cleanup:
monitor file count and allocated bytes in the private parent, exclude active writers, wait for a
configured retention interval, and then remove only matching stale entries. Never run an
unbounded glob deletion while writers are active. Redact the basename and parent in telemetry.

```kotlin
import io.bluetape4k.io.writeAtomically
import java.nio.file.Files
import java.nio.file.Path

val source = Path.of("input.bin")
val destination = Path.of("output.bin")
val maxPayloadBytes = 8L * 1024 * 1024
val bytes = destination.writeAtomically { output ->
    Files.newInputStream(source).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            require(copied + read <= maxPayloadBytes) { "payload exceeds configured limit" }
            output.write(buffer, 0, read)
            copied += read
        }
    }
}
```

```kotlin
import io.bluetape4k.io.writeAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

suspend fun copyAtomically(source: Path, destination: Path): Long {
    val callerContext = currentCoroutineContext()
    return withContext(Dispatchers.IO) {
        destination.writeAtomically { output ->
            Files.newInputStream(source).use { input -> input.copyTo(output) }
            callerContext.ensureActive()
        }
    }
}
```

Java calls the `AtomicFileSupport` facade with `Function1<OutputStream, Unit>`. The method declares
checked `IOException`; because `Function1` does not declare it, wrap callback I/O failures in
`UncheckedIOException` and return `Unit.INSTANCE`.

```java
import io.bluetape4k.io.AtomicFileSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import kotlin.Unit;

static long write(Path destination, byte[] payload) throws IOException {
    return AtomicFileSupport.writeAtomically(destination, output -> {
        try {
            output.write(payload);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return Unit.INSTANCE;
    });
}
```
````

한국어 README의 같은 위치에 구조적으로 동등한 내용을 추가한다.

````markdown
`Path.writeAtomically`는 provider가 소유하는 같은 parent의 임시 파일에 먼저 기록하고,
callback과 stream close가 성공한 뒤에만 `ATOMIC_MOVE`를 시도합니다. callback은
`OutputStream`을 빌려 쓰므로 직접 닫거나 보관하면 안 됩니다. 기존 target 교체,
임시 파일 permission과 파일 attribute는 filesystem provider 계약을 따릅니다. 이 API는
`fsync`와 process crash나 power loss 상황의 파일 durability를 보장하지 않습니다. atomic
replacement가 지원되지 않으면 일반 move fallback을 사용하지 않고 실패합니다.

이 함수는 blocking API입니다. 없는 parent를 생성하고, 빈 경로나 filesystem root를
`IllegalArgumentException`으로 거부하며, provider가 소유한 stream에 기록한 byte 수를
`Long`으로 반환합니다. 자동으로 생성한 parent는 이후 단계가 실패해도 rollback하지
않습니다. callback, close, commit이 던진 unchecked exception,
`CancellationException`, `Error`의 identity를 유지하고 cleanup 실패만 suppressed로
연결합니다.
Coroutine 호출자는 dispatcher를 직접 선택하고 callback 밖에서 캡처한 context의
cancellation을 callback 반환 전에 검사해야 합니다. commit 뒤 cancellation은 이미
끝난 교체를 되돌리지 않습니다.

경로 정규화는 lexical 처리일 뿐 path sandbox, symlink, hard-link, mount 교체와 TOCTOU를
방어하지 않습니다. 민감한 payload에는 opaque basename과 접근이 제한된 private parent를
사용하세요. 공격자가 제어하는 공유 writable directory에는 사용하지 말고, 이런 방어가
필요하면 secure directory handle 기반 API를 선택해야 합니다. 호출자가 byte/time 한도를
집행하고, provider와 primary/suppressed cleanup 실패를 기록할 때 전체 경로, basename,
예외 메시지의 민감한 값은 가려서 기록해야 합니다.

process crash 뒤에는 `.<basename>.*.tmp` 파일이 남을 수 있습니다. 애플리케이션 운영자가
private parent의 파일 수와 사용량을 감시하고, 활성 writer가 없음을 확인한 뒤 설정한
보존 시간이 지난 항목만 정리해야 합니다. writer가 실행 중일 때 무제한 glob 삭제를
실행하면 안 됩니다. telemetry에 기록할 때 basename과 parent는 가려야 합니다.

```kotlin
import io.bluetape4k.io.writeAtomically
import java.nio.file.Files
import java.nio.file.Path

val source = Path.of("input.bin")
val destination = Path.of("output.bin")
val maxPayloadBytes = 8L * 1024 * 1024
val bytes = destination.writeAtomically { output ->
    Files.newInputStream(source).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            require(copied + read <= maxPayloadBytes) { "payload exceeds configured limit" }
            output.write(buffer, 0, read)
            copied += read
        }
    }
}
```

```kotlin
import io.bluetape4k.io.writeAtomically
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

suspend fun copyAtomically(source: Path, destination: Path): Long {
    val callerContext = currentCoroutineContext()
    return withContext(Dispatchers.IO) {
        destination.writeAtomically { output ->
            Files.newInputStream(source).use { input -> input.copyTo(output) }
            callerContext.ensureActive()
        }
    }
}
```

Java에서는 `Function1<OutputStream, Unit>` 형태로 `AtomicFileSupport` facade를 호출합니다.
메서드는 checked `IOException`을 선언하지만 `Function1`은 이를 선언하지 않으므로,
callback의 I/O 실패는 `UncheckedIOException`으로 감싸고 `Unit.INSTANCE`를 반환합니다.

```java
import io.bluetape4k.io.AtomicFileSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import kotlin.Unit;

static long write(Path destination, byte[] payload) throws IOException {
    return AtomicFileSupport.writeAtomically(destination, output -> {
        try {
            output.write(payload);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return Unit.INSTANCE;
    });
}
```
````

두 README의 모듈 구조에 `AtomicFileSupport.kt`를 같은 위치와 의미로 추가한다.

- [ ] **Step 2: CHANGELOG 미출시 추가 항목 기록**

`CHANGELOG.md`의 첫 `### 추가` 아래에 다음 항목을 추가한다.

```markdown
- `bluetape4k-io`에 callback과 stream close가 성공한 뒤에만 sibling temporary file을
  atomic move하는 `Path.writeAtomically`를 추가했다. 지원하지 않는 atomic replacement는
  일반 move fallback을 사용하지 않으며, 기존 target 교체와 metadata는 filesystem
  provider 계약을 따른다
  ([#1645](https://github.com/bluetape4k/bluetape4k-projects/issues/1645)).
```

- [ ] **Step 3: README 예제와 Kotlin/Java facade 계약 확인**

Run:

```bash
./gradlew :bluetape4k-io:classes :bluetape4k-io:testClasses \
  :bluetape4k-io:test \
  --tests "io.bluetape4k.io.AtomicFileSupportTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileReadmeKotlinContractTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileSupportJavaContractTest" \
  --no-build-cache --no-configuration-cache --console=plain
javap -classpath io/io/build/classes/kotlin/main -s io.bluetape4k.io.AtomicFileSupport
```

Expected: README의 실제 bounded/coroutine Kotlin helper와 Java `write` method를 담은 외부
package test가 compile·실행되고, Gradle은 성공하며 `javap`에 다음 핵심 계약이 나타난다.

```text
public static final long writeAtomically(java.nio.file.Path, kotlin.jvm.functions.Function1<? super java.io.OutputStream, kotlin.Unit>) throws java.io.IOException;
descriptor: (Ljava/nio/file/Path;Lkotlin/jvm/functions/Function1;)J
```

- [ ] **Step 4: 문서·ABI commit**

```bash
git add io/io/README.md io/io/README.ko.md CHANGELOG.md
git commit
```

Commit intent: `원자적 파일 교체의 provider 경계를 호출자에게 알린다`. Lore trailers에는
metadata/durability 비보장과 README locale parity 검증을 기록한다.

### Task 4: module 전체 회귀와 정적 검증을 수렴한다

**Files:**
- Modify: `docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md`

- [ ] **Step 1: 새 계약 test를 clean 상태에서 재실행**

```bash
./gradlew :bluetape4k-io:cleanTest :bluetape4k-io:test \
  --tests "io.bluetape4k.io.AtomicFileSupportTest" \
  --tests "io.bluetape4k.io.AtomicFileFailurePolicyTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileReadmeKotlinContractTest" \
  --tests "io.bluetape4k.io.readme.AtomicFileSupportJavaContractTest" \
  --no-build-cache --no-configuration-cache --console=plain
```

Expected: 새 test 전체 PASS, failure/error/skip 0.

- [ ] **Step 2: `bluetape4k-io` 전체 회귀 실행**

```bash
./gradlew :bluetape4k-io:test \
  --rerun-tasks --no-build-cache --no-configuration-cache --console=plain
```

Expected: baseline 1,265개와 신규 test를 포함한 전체 suite PASS, failure/error 0.

- [ ] **Step 3: compile과 Detekt 실행**

```bash
./gradlew \
  :bluetape4k-io:compileKotlin \
  :bluetape4k-io:compileTestKotlin \
  :bluetape4k-io:compileTestJava \
  :bluetape4k-io:detekt \
  --no-build-cache --no-configuration-cache --console=plain
```

Expected: 모든 task 성공. Detekt가 `ignoreFailures`를 사용하는 경우 report의 신규 파일
finding도 직접 확인하고 exit code만으로 clean을 주장하지 않는다.

- [ ] **Step 4: 문서와 diff 검사**

```bash
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  io/io/README.ko.md CHANGELOG.md \
  docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md
git diff --check
git status --short
```

Expected: terminology findings 0, whitespace error 0, 승인 범위 밖 경로 0.

- [ ] **Step 5: checklist에 fresh evidence 기록**

Targeted/full-suite test count, compile/Detekt 결과, `javap` descriptor, terminology와 diff
결과를 A-06/A-07, CG-06..CG-08, SPW evidence에 반영한다. Full Nightly와 Image regression은
실행하지 않았으면 PASS로 표시하지 않는다.

### Task 5: final review, lesson과 provider PR 준비를 완료한다

**Files:**
- Create: `docs/lessons/2026-09-06-issue-1645-atomic-file-replacement.md`
- Create: `docs/superpowers/reviews/2026-09-06-issue-1645-atomic-file-replacement-code-review.md`
- Modify: `docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md`

- [ ] **Step 1: exact diff six-perspective code review 수행**

성능, 안정성, 보안, 운영, 개발자/API, 사용자/호출자 관점에 각각 exact local head와
`origin/develop...HEAD` diff를 제공한다. 각 결과는 file:line, P0..P3, 검증 공백을
분리한다. P0/P1은 source/test/docs를 수정하고 영향 관점을 다시 실행해 0으로 만든다.

- [ ] **Step 2: main integration에서 acceptance를 대조**

다음을 한 표로 기록한다.

```markdown
| Acceptance | Source/Test/Doc evidence | Result |
|---|---|---|
| sibling temp와 close-before-move | `AtomicFileSupport.kt`, failure seam test | PASS/FAIL |
| exact exception identity와 suppression | failure policy tests | PASS/FAIL |
| no fallback와 provider replace 경계 | operations tests, KDoc/README | PASS/FAIL |
| counting과 no metadata reread | counting test, operations surface | PASS/FAIL |
| path, concurrency, orphan/observability | filesystem tests, KDoc/README | PASS/FAIL |
| Kotlin/Java API와 ABI | Kotlin test, Java test, `javap` | PASS/FAIL |
```

모든 행이 fresh evidence로 PASS이고 known P0/P1이 0일 때만 A-08을 완료한다.

- [ ] **Step 3: 재사용 가능한 lesson 작성**

`docs/lessons/2026-09-06-issue-1645-atomic-file-replacement.md`에 다음 주제를 근거와 함께
기록한다.

```markdown
# 원자적 파일 교체에서 provider 계약과 실패 identity를 분리하는 법

## 문제와 원인
## ATOMIC_MOVE가 의미하는 범위
## callback-close-cleanup의 예외 순서
## commit 이후 cleanup lookup을 피하는 이유
## metadata, trusted parent, crash orphan과 관측 책임

- orphan 이름은 `.<basename>.*.tmp`이고 basename과 parent는 telemetry에서 redaction한다.
- 애플리케이션 운영자가 private parent의 파일 수와 사용량을 감시한다.
- 활성 writer가 없음을 확인하고 설정한 보존 시간이 지난 항목만 bounded batch로 정리한다.
- writer 실행 중의 무제한 glob 삭제를 금지한다.

## downstream coroutine/S3 adapter 적용 규칙
## 재사용 체크리스트
```

- [ ] **Step 4: lesson 색인·검색 검증**

```bash
gno update
gno embed --collection bluetape4k-docs
gno search "atomic file replacement provider cleanup" -c bluetape4k-docs
```

Expected: 새 lesson이 대표 결과에 나타난다. GNO 실패는 lesson source를 무효화하지 않지만
knowledge gate를 `PENDING`으로 두고 원인을 기록한다.

- [ ] **Step 5: final artifact commit**

```bash
git add \
  docs/lessons/2026-09-06-issue-1645-atomic-file-replacement.md \
  docs/superpowers/reviews/2026-09-06-issue-1645-atomic-file-replacement-code-review.md \
  docs/superpowers/checklists/2026-09-06-issue-1645-type-a.md
git commit
```

Commit intent: `provider별 파일 교체 위험이 다시 숨지 않도록 한다`. Lore trailers에는
six-perspective `P0=0/P1=0`, 전체 module test, ABI와 미실행 Full Nightly/Image regression을
정확히 기록한다.

- [ ] **Step 6: PR 전 exact-head 검증과 승인된 PR 생성**

```bash
git status --porcelain=v1
git log --oneline origin/develop..HEAD
git diff --check origin/develop...HEAD
git diff --stat origin/develop...HEAD
```

Expected: worktree clean, 승인된 파일만 존재하고 local exact head가 모든 검증 evidence와
일치한다.

`.omx/tmp/issue-1645-pr-body.md`를 다음 내용으로 만든다.

```markdown
## 문제

파일을 같은 parent의 임시 파일에 쓴 뒤 교체하는 lifecycle이 여러 저장소에 반복되지만,
callback, stream close, atomic move, cleanup 실패의 책임과 예외 순서를 공통 API로 검증할
수 없었습니다.

Closes #1645

## 변경 사항

- `Path.writeAtomically`가 sibling temporary file을 만들고 callback과 stream close가
  성공한 뒤에만 `ATOMIC_MOVE`를 시도합니다.
- callback, close, move, cleanup 실패의 identity와 suppressed 순서를 보존합니다.
- Kotlin/Java 호출 계약, 기존 target 교체, concurrency와 provider 경계를 테스트하고
  README locale pair와 CHANGELOG에 운영 제약을 기록합니다.

## 검증

- targeted atomic file tests
- `:bluetape4k-io:test`
- Kotlin/Java compile, Detekt, `javap`
- terminology audit, `git diff --check`

## 배포 경계

merge와 `2.1.0-SNAPSHOT` 발행은 이 PR 생성과 별도 승인 gate입니다. Image #631은
원격 snapshot provenance가 확인될 때까지 시작하지 않습니다.

## DoD Status

- [x] 설계·계획·TDD·module regression·ABI 검증 완료
- [x] six-perspective review에서 P0=0/P1=0
- [ ] exact-head CI와 live review/thread 수렴
- [ ] fresh merge 승인
- [ ] 별도 `2.1.0-SNAPSHOT` 발행 승인과 provenance
```

Run:

```bash
git push --set-upstream origin feat/issue-1645-atomic-file-replacement
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-1645-atomic-file-replacement \
  --title "파일 교체를 원자적 commit 경계로 묶는다" \
  --body-file .omx/tmp/issue-1645-pr-body.md
```

Expected: PR은 `develop <- feat/issue-1645-atomic-file-replacement`로 열리고 #1645를
연결한다. merge와 개발 버전 발행은 fresh approval 전까지 실행하지 않는다.

- [ ] **Step 7: remote/PR exact head와 live checks·thread를 read-back**

```bash
provider_branch=feat/issue-1645-atomic-file-replacement
local_head=$(git rev-parse HEAD)
remote_head=$(git ls-remote --heads origin "refs/heads/$provider_branch" | awk '{print $1}')
pr_json=$(gh pr view "$provider_branch" \
  --repo bluetape4k/bluetape4k-projects \
  --json number,url,state,baseRefName,headRefName,headRefOid,mergeable,mergeStateStatus,reviewDecision,statusCheckRollup)
pr_number=$(printf '%s' "$pr_json" | jq -r .number)
pr_head=$(printf '%s' "$pr_json" | jq -r .headRefOid)
test "$local_head" = "$remote_head"
test "$local_head" = "$pr_head"
test "$(printf '%s' "$pr_json" | jq -r .baseRefName)" = develop
gh pr checks "$provider_branch" --repo bluetape4k/bluetape4k-projects --required --watch
threads_json=$(gh api graphql \
  -f owner=bluetape4k -f name=bluetape4k-projects -F number="$pr_number" \
  -f query='query($owner:String!,$name:String!,$number:Int!){repository(owner:$owner,name:$name){pullRequest(number:$number){reviewThreads(first:100){nodes{isResolved}pageInfo{hasNextPage}}}}}')
test "$(printf '%s' "$threads_json" | jq -r '.data.repository.pullRequest.reviewThreads.pageInfo.hasNextPage')" = false
test "$(printf '%s' "$threads_json" | jq '[.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved == false)] | length')" -eq 0
date -u +%Y-%m-%dT%H:%M:%SZ
```

Expected: local, remote, PR `headRefOid`가 같고 CI가 terminal success이며 unresolved thread는
0이다. solo-maintainer 저장소에서 사람 review가 없으면 그 subgate만 `N/A`로 기록한다.
조회 시각, PR URL, exact SHA, check별 conclusion과 thread 수를 checklist에 기록한다.

- [ ] **Step 8: merge·개발 버전 발행 별도 승인 뒤 Image #631 unblock provenance 기록**

이 step은 merge와 publish의 fresh approval을 각각 받은 뒤에만 실행한다. 개발 버전 발행 workflow dispatch는
`gh workflow run publish-snapshot.yml --repo bluetape4k/bluetape4k-projects --ref develop`이며,
승인 전에는 실행하지 않는다. 발행 뒤 다음 read-only 검증을 수행한다.

```bash
gh run list \
  --repo bluetape4k/bluetape4k-projects \
  --workflow publish-snapshot.yml \
  --branch develop \
  --limit 5 \
  --json databaseId,headSha,status,conclusion,createdAt,updatedAt,url
curl --fail --silent --show-error \
  https://central.sonatype.com/repository/maven-snapshots/io/github/bluetape4k/bluetape4k-io/2.1.0-SNAPSHOT/maven-metadata.xml
cd /Users/debop/work/bluetape4k/bluetape4k-image
env -u BLUETAPE4K_DEPENDENCIES_CATALOG_PATH \
  ./gradlew --refresh-dependencies \
  :bluetape4k-images:dependencyInsight \
  --dependency io.github.bluetape4k:bluetape4k-io \
  --configuration compileClasspath \
  --no-build-cache --no-configuration-cache --console=plain
```

Expected: 성공한 publish run의 `headSha`가 승인·merge한 provider SHA와 같고, GAV
`io.github.bluetape4k:bluetape4k-io:2.1.0-SNAPSHOT`, 발행 시작/완료 시각, metadata의
timestamp/build number, 원격 repository에서 해석한 dependency version을 checklist에
기록한다. local catalog path override 없이 원격 artifact가 확인되기 전에는 Image #631을
계속 차단한다. 이미 발행한 개발 버전 artifact는 삭제하지 않으며, 잘못된 발행은 수정
commit과 새 발행 provenance로만 대체한다.

## Spec-to-task 추적성

| 사양 요구 | 계획 task |
|---|---|
| 공개 `Path.writeAtomically`와 counting stream | Task 1, Task 2 |
| parent/sibling temp, callback-close-commit 순서 | Task 1, Task 2 |
| provider 조건부 replace와 fail-closed no fallback | Task 2, Task 3 |
| callback/close/cleanup identity와 비순환 suppressed graph | Task 2 |
| empty/root, parent 잔존, success cleanup 미조회 | Task 1, Task 2 |
| different/same-target concurrency | Task 1, Task 4 |
| Kotlin facade, Java caller, `throws IOException`, ABI | Task 1, Task 3 |
| trusted parent, metadata/durability/orphan/observability 경계 | Task 3 |
| README locale pair와 CHANGELOG | Task 3, Task 4 |
| module regression, Detekt, exact diff | Task 4, Task 5 |
| six-perspective review, lesson, PR hold | Task 5 |
| Image migration provenance | Task 5의 외부 hold; Projects merge/publish 후 별도 Image 계획 |

## 계획 DoD

- 모든 사양 수용 기준이 concrete source/test/doc task에 연결된다.
- RED는 API 부재와 functional skeleton 실패 두 단계로 기록하고 GREEN은 targeted와 module
  전체에서 fresh하게 재현한다.
- 새 API는 실제 Kotlin/Java caller와 `javap` descriptor로 검증한다.
- provider 의미, metadata 비보존, trusted parent, orphan과 caller observability를 KDoc와
  locale pair에 같은 의미로 기록한다.
- exact diff six-perspective code review와 main integration에서 P0=0/P1=0을 달성한다.
- lesson과 checklist를 commit하고 PR exact-head gate를 통과한다.
- merge, `2.1.0-SNAPSHOT` 발행과 Image migration은 별도 승인·provenance gate로 남긴다.
