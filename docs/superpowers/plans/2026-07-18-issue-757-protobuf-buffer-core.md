# Issue #757 Protobuf Buffer Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `$subagent-driven-development` (recommended) or `$executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add caller-owned Protobuf `ByteBuffer` APIs, route strict serializer and contiguous Redisson decode through lower-copy paths, and prove benchmark completeness plus allocation claims with two fail-closed runs.

**Architecture:** Keep `ByteArray` and trusted fallback behavior intact while adding a shared loader-scoped Protobuf message resolver and explicit buffer overrides. Redisson uses a bounded NIO view only for single-component inputs and retains an isolated copied compatibility path for composite and trusted fallback inputs. The existing protobuf benchmark module becomes a thread-confined allocation harness with a module-local validator and exact-head evidence protocol.

**Tech Stack:** Kotlin 2.3, Java 21 `ByteBuffer`, Protobuf 4.35.1 `Any`/`CodedOutputStream`, Netty `ByteBuf`, Redisson codec APIs, JUnit 5, kotlinx-benchmark/JMH, Python 3 `unittest`.

---

## File Map

**Create**

- `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufMessageClassResolver.kt` — loader-identity cache and `Message` assignability gate shared by serializer and Redisson.
- `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MessageSupportByteBufferTest.kt` — public helper buffer contract.
- `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerByteBufferTest.kt` — serializer buffer dispatch, exception, fallback, and state contract.
- `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupport.kt` — deterministic fixture and reset/semantic validation.
- `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkMetadata.kt` — pinned-JAR metadata entrypoint for observed payload, matrix, and config identity.
- `benchmark/protobuf-codec-benchmark/src/test/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupportTest.kt` — repeated invocation and matrix contract.
- `benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py` — expected-method, metric, identity, and two-run verdict gate.
- `benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py` — fail-closed validator fixtures.
- `benchmark/protobuf-codec-benchmark/scripts/run-evidence.py` — collision-safe environment capture, pinned-JAR execution, logging, validation, and atomic evidence promotion.
- `benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py` — runner no-clobber, provenance, clean-tree, and promotion fixtures.
- `docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md` — measured conclusion and limitations.
- `docs/benchmarks/raw/issue-757/run-<UTC>/environment.json` — normalized run identity.
- `docs/benchmarks/raw/issue-757/run-<UTC>/argv.json` — exact recorded JMH launcher arguments and timestamps.
- `docs/benchmarks/raw/issue-757/run-<UTC>/run.log` — combined launcher stdout/stderr and explicit exit status.
- `docs/benchmarks/raw/issue-757/run-<UTC>/jmh.json` — raw JMH output for each fresh run.
- `docs/benchmarks/raw/issue-757/run-<UTC>/summary.csv` — validated per-run metrics.
- `docs/benchmarks/raw/issue-757/run-<UTC>/validation.json` — machine-readable validation result.
- `docs/benchmarks/raw/issue-757/comparison.csv` — two-run comparison and verdicts.
- `docs/benchmarks/raw/issue-757/validation.json` — comparison validation and identity verdict.
- `docs/benchmarks/raw/issue-757/delivery-manifest.json` — committed relative-path/hash authority for clean-checkout report and delivery validation.

**Move**

- `benchmark/protobuf-codec-benchmark/src/benchmark/proto/protobuf/benchmark-message.proto` → `benchmark/protobuf-codec-benchmark/src/main/proto/protobuf/benchmark-message.proto` so the fixture and its unit tests share one generated payload type.

**Modify**

- `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt`
- `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt`
- `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt`
- `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt`
- `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecTest.kt`
- `benchmark/protobuf-codec-benchmark/build.gradle.kts`
- `benchmark/protobuf-codec-benchmark/src/benchmark/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmark.kt`
- `benchmark/protobuf-codec-benchmark/README.md`
- `benchmark/protobuf-codec-benchmark/README.ko.md`
- `io/protobuf/README.md`
- `io/protobuf/README.ko.md`
- `docs/benchmarks/README.md`
- `CHANGELOG.md`

## Guardrails

- Do not edit `LettuceProtobufCodecs.kt`, Kafka, compressor wrappers, Gradle module registration, dependency catalogs, release, or publishing surfaces.
- Preserve `packMessage(ByteArray)`, `unpackMessage(ByteArray)`, `serialize`, and `deserialize` wire and source/binary compatibility.
- Do not call any path zero-copy; `Any.pack`, `ByteString`, parse, and unpack may still allocate.
- Execute Testcontainers-backed modules sequentially. This slice needs only in-memory protobuf/Netty tests.
- Before evidence collection, commit every implementation/documentation change and require a clean working tree including untracked files.
- Roll back in dependency order: evidence/report wording and validator expectations first, Redisson contiguous dispatch second, serializer overrides third. Retain the approved public helper/resolver unless correctness or security fails; before release they may be reverted only with the MessageSupport, serializer security, and full protobuf suites rerun. After publication, public API removal requires a separate compatibility/deprecation decision rather than this performance rollback.
- After any rollback, rerun the directly affected targeted tests, `:bluetape4k-protobuf:test`, benchmark fixture/validator tests, benchmark compile/jar, and both fresh evidence runs from the new clean head.

Evidence invalidation is fail-closed:

| Post-promotion change | Required lifecycle |
|---|---|
| production dispatch, JMH fixture/method matrix, canonical config/payload, metadata emitter, dependency/JAR input, JVM profile | invalidate the manifest; build a fresh JAR; collect two fresh runs; compare, promote/replace, verify, regenerate report/manifest; rerun Performance plus every affected review lens |
| validator/runner metric parsing, verdict semantics, identity rules | fresh runs unless a reviewer proves metric inputs and semantics are byte-for-byte unchanged; otherwise no revalidation shortcut |
| report renderer or delivery-contract wording only | run `validate-committed`, keep and revalidate the unchanged delivery manifest, regenerate/validate only the report, then rerun Operator/Caller review; if the manifest schema or authority contract changed, use the validator/runner row instead; raw metrics remain immutable |
| promoted raw evidence, environment, comparison, validation, or rollback archive | treat as tamper; restore exact committed bytes or run the full fresh-evidence lifecycle |

Initial promotion remains no-clobber. A legitimate post-promotion replacement uses `run-evidence.py replace-promoted --state <new-state> --expected-manifest <tracked-old-manifest> --destination docs/benchmarks/raw/issue-757 --backup-root .omx/tmp/issue-757/evidence-backups`. It verifies the tracked old directory against its manifest, builds/verifies the complete new sibling directory, atomically moves the old canonical directory to a unique ignored backup and the new directory into place, restores the old directory on any failure, and records the one exact backup path in state. Git history plus the ignored backup preserves recovery. Only after the replacement manifest is committed at `HEAD`, `git show HEAD:docs/benchmarks/raw/issue-757/delivery-manifest.json` is byte-identical to the working file, and `validate-committed` succeeds may `cleanup-replacement-backup` remove that state-recorded backup; it rejects an unverified manifest/head, a path outside the configured backup root, or any path other than the recorded backup. Fixtures cover old-manifest drift, destination collision, failure between both renames, successful restore, cleanup refusal, exact single-backup cleanup, and clean-checkout validation of the replacement.

### Task 1: Add the loader-scoped message resolver security boundary

**Files:**

- Create: `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufMessageClassResolver.kt`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt`

- [ ] **Step 1: Write failing tests for non-Message classes and loader isolation**

Add these helpers and tests to `ProtobufSerializerSecurityTest`:

```kotlin
private class CountingClassLoader(parent: ClassLoader): ClassLoader(parent) {
    var targetLoads: Int = 0
        private set

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name == TestMessage::class.java.name) targetLoads++
        return super.loadClass(name, resolve)
    }
}

@Test
fun `allowlisted non message class is a terminal security failure`() {
    val crafted = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/java.lang.String")
        .build()
        .toByteArray()
    val serializer = ProtobufSerializer(allowedClassPrefixes = setOf("java.lang."))

    val failure = assertFailsWith<BinarySerializationException> {
        serializer.deserialize<kotlin.Any>(crafted)
    }

    generateSequence(failure.cause) { it.cause }
        .filterIsInstance<SecurityException>()
        .first()
        .message
        .orEmpty()
        .contains("does not implement com.google.protobuf.Message")
        .shouldBeTrue()
}

@Test
fun `message class cache is isolated by effective class loader identity`() {
    val serializer = ProtobufSerializer()
    val bytes = serializer.serialize(testMessage { id = 7L; name = "loader" })
    val original = Thread.currentThread().contextClassLoader
    val first = CountingClassLoader(original)
    val second = CountingClassLoader(original)

    try {
        Thread.currentThread().contextClassLoader = first
        serializer.deserialize<TestMessage>(bytes).shouldNotBeNull()
        serializer.deserialize<TestMessage>(bytes).shouldNotBeNull()
        Thread.currentThread().contextClassLoader = second
        serializer.deserialize<TestMessage>(bytes).shouldNotBeNull()
    } finally {
        Thread.currentThread().contextClassLoader = original
    }

    first.targetLoads shouldBeEqualTo 1
    second.targetLoads shouldBeEqualTo 1
}
```

- [ ] **Step 2: Run the security tests and verify RED**

Run:

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.ProtobufSerializerSecurityTest" \
  --no-configuration-cache
```

Expected: FAIL because the existing class-name-only companion cache bypasses the second loader and the unchecked cast does not enforce `Message` assignability before caching.

- [ ] **Step 3: Implement the shared resolver**

Create `ProtobufMessageClassResolver.kt`:

```kotlin
package io.bluetape4k.protobuf.serializers

import com.google.protobuf.Message
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

internal class ProtobufMessageClassResolver {
    private companion object {
        const val MAX_CLASSES_PER_LOADER = 256
    }

    private val staleLoaders = ReferenceQueue<ClassLoader>()
    private val loaderCaches = ConcurrentHashMap<LoaderKey, LoaderBucket>()
    private val bootstrapCache = LoaderBucket()

    fun resolve(className: String, classLoader: ClassLoader?): Class<out Message> {
        expungeStaleLoaders()
        val cache = if (classLoader == null) {
            bootstrapCache
        } else {
            loaderCaches.computeIfAbsent(LoaderKey(classLoader, staleLoaders)) { LoaderBucket() }
        }
        return cache.resolve(className) { loadMessageClass(className, classLoader) }
    }

    private fun loadMessageClass(className: String, classLoader: ClassLoader?): Class<out Message> {
        val resolved = Class.forName(className, false, classLoader)
        if (!Message::class.java.isAssignableFrom(resolved)) {
            throw SecurityException(
                "Resolved Protobuf class $className does not implement ${Message::class.java.name}."
            )
        }
        return resolved.asSubclass(Message::class.java)
    }

    private fun expungeStaleLoaders() {
        while (true) {
            val stale = staleLoaders.poll() as? LoaderKey ?: return
            loaderCaches.remove(stale)
        }
    }

    private class LoaderBucket {
        private val classes = LinkedHashMap<String, WeakReference<Class<out Message>>>()

        @Synchronized
        fun resolve(
            className: String,
            loader: () -> Class<out Message>,
        ): Class<out Message> {
            classes[className]?.get()?.let { return it }
            classes.entries.removeIf { it.value.get() == null }
            val resolved = loader()
            if (classes.size >= MAX_CLASSES_PER_LOADER) classes.clear()
            classes[className] = WeakReference(resolved)
            return resolved
        }

        @Synchronized fun size(): Int = classes.size
        @Synchronized fun seedForTest(entries: Map<String, Class<out Message>>) {
            entries.forEach { (name, type) ->
                if (classes.size >= MAX_CLASSES_PER_LOADER) classes.clear()
                classes[name] = WeakReference(type)
            }
        }
    }

    private class LoaderKey(
        classLoader: ClassLoader,
        queue: ReferenceQueue<ClassLoader>,
    ): WeakReference<ClassLoader>(classLoader, queue) {
        private val identityHash = System.identityHashCode(classLoader)

        override fun hashCode(): Int = identityHash

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LoaderKey || identityHash != other.identityHash) return false
            val left = get() ?: return false
            return left === other.get()
        }
    }
}
```

Add these package-internal test seams to the resolver with the exact signatures below; they are not public API and exist only to make eviction/queue behavior deterministic:

```kotlin
internal fun cacheSizeForTest(classLoader: ClassLoader?): Int
internal fun seedCacheForTest(
    classLoader: ClassLoader?,
    entries: Map<String, Class<out Message>>,
)
internal fun expungeStaleLoadersForTest()
internal fun loaderBucketCountForTest(): Int
internal fun clearAndEnqueueLoaderKeyForTest(classLoader: ClassLoader): Boolean
```

Implement those seams exactly against the real map entries:

```kotlin
internal fun cacheSizeForTest(classLoader: ClassLoader?): Int =
    if (classLoader == null) bootstrapCache.size()
    else loaderCaches.entries.singleOrNull { it.key.get() === classLoader }?.value?.size() ?: 0

internal fun seedCacheForTest(
    classLoader: ClassLoader?,
    entries: Map<String, Class<out Message>>,
) {
    val bucket = if (classLoader == null) bootstrapCache else
        loaderCaches.computeIfAbsent(LoaderKey(classLoader, staleLoaders)) { LoaderBucket() }
    bucket.seedForTest(entries)
}

internal fun expungeStaleLoadersForTest() = expungeStaleLoaders()
internal fun loaderBucketCountForTest(): Int = loaderCaches.size

internal fun clearAndEnqueueLoaderKeyForTest(classLoader: ClassLoader): Boolean {
    val key = loaderCaches.keys.singleOrNull { it.get() === classLoader } ?: return false
    key.clear()
    return key.enqueue()
}
```

Add this child-first fixture and deterministic lifecycle tests:

```kotlin
private class ChildFirstSingleClassLoader(
    parent: ClassLoader,
    private val targetName: String,
    private val targetBytes: ByteArray,
): ClassLoader(parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        synchronized(getClassLoadingLock(name)) {
            val loaded = findLoadedClass(name)
            val type = loaded ?: if (name == targetName) {
                defineClass(name, targetBytes, 0, targetBytes.size)
            } else {
                super.loadClass(name, false)
            }
            if (resolve) resolveClass(type)
            type
        }
}

private fun classBytes(type: Class<*>): ByteArray =
    checkNotNull(type.getResourceAsStream("/${type.name.replace('.', '/')}.class")).use { it.readBytes() }

@Test
fun `child defined same name classes stay isolated by loader identity`() {
    val target = TestMessage::class.java
    val first = ChildFirstSingleClassLoader(target.classLoader, target.name, classBytes(target))
    val second = ChildFirstSingleClassLoader(target.classLoader, target.name, classBytes(target))
    val resolver = ProtobufMessageClassResolver()
    val firstType = resolver.resolve(target.name, first)
    val secondType = resolver.resolve(target.name, second)
    (firstType === secondType) shouldBeEqualTo false
    firstType.classLoader shouldBeEqualTo first
    secondType.classLoader shouldBeEqualTo second
}

@Test
fun `loader bucket cap and stale key cleanup are deterministic`() {
    val loader = CountingClassLoader(TestMessage::class.java.classLoader)
    val resolver = ProtobufMessageClassResolver()
    resolver.seedCacheForTest(
        loader,
        (0..256).associate { "synthetic.Message$it" to TestMessage::class.java },
    )
    resolver.cacheSizeForTest(loader) shouldBeEqualTo 1
    val buckets = resolver.loaderBucketCountForTest()
    resolver.clearAndEnqueueLoaderKeyForTest(loader) shouldBeEqualTo true
    resolver.expungeStaleLoadersForTest()
    resolver.loaderBucketCountForTest() shouldBeEqualTo buckets - 1
}
```

`seedCacheForTest` delegates to the same synchronized `LoaderBucket.seedForTest` cap logic used by production insertion. Test the hard cap by seeding `MAX_CLASSES_PER_LOADER + 1` distinct synthetic names all pointing to `TestMessage::class.java` and asserting the bucket size is `1`, then resolve the real class successfully. For stale-loader cleanup, obtain/create a loader bucket, explicitly clear and enqueue its weak `LoaderKey` through a package-internal resolver test helper, call `expungeStaleLoadersForTest`, and assert the bucket count drops without relying on `System.gc()`. A bounded GC reclamation test may remain disabled/tagged as auxiliary evidence, but it is not a required build gate.

Replace `ProtobufSerializer`'s companion cache with an instance field:

```kotlin
private val messageClassResolver = ProtobufMessageClassResolver()
```

Resolve with the effective loader only after allowlist validation:

```kotlin
val classLoader = Thread.currentThread().contextClassLoader ?: ProtobufSerializer::class.java.classLoader
val clazz = messageClassResolver.resolve(className, classLoader)
```

Extend the loader tests with a null-TCCL case that restores the original loader in `finally` and still decodes `TestMessage`. Add a child-defined same-binary-name loader fixture to prove loader identity isolation. Use the deterministic weak-key enqueue seam and hard-cap seed seam described above as the required lifecycle checks; do not make GC timing a build requirement. Keep the counting-loader assertion as routing evidence, not as unloadability proof.

- [ ] **Step 4: Run the security tests and verify GREEN**

Run the Step 2 command.

Expected: PASS, including one resolution per distinct loader identity and terminal rejection of `java.lang.String`.

- [ ] **Step 5: Commit the resolver boundary**

```bash
git add \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufMessageClassResolver.kt \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt \
  io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt
git commit -m "Isolate protobuf class resolution across caller class loaders" \
  -m "Constraint: Any type URLs must be allowlisted and resolve without class initialization
Rejected: Keep a companion cache keyed only by class name | it crosses class-loader trust boundaries
Confidence: high
Scope-risk: moderate
Directive: Validate Message assignability before every cache insertion or unchecked use
Tested: ProtobufSerializerSecurityTest
Not-tested: Redisson adoption follows in a separate task"
```

### Task 2: Add caller-owned MessageSupport ByteBuffer APIs

**Files:**

- Create: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MessageSupportByteBufferTest.kt`
- Modify: `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt`

- [ ] **Step 1: Write failing public contract tests**

Create `MessageSupportByteBufferTest.kt` with heap/direct/sliced/read-only and state assertions:

```kotlin
package io.bluetape4k.protobuf

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException

class MessageSupportByteBufferTest {
    private val message = testMessage { id = 42L; name = "buffer" }
    private val wire = packMessage(message)

    @Test
    fun `packMessageTo writes identical wire at a non zero position`() {
        listOf(ByteBuffer.allocate(wire.size + 8), ByteBuffer.allocateDirect(wire.size + 8)).forEach { target ->
            target.order(ByteOrder.LITTLE_ENDIAN)
            val originalLimit = target.limit()
            val originalCapacity = target.capacity()
            val originalOrder = target.order()
            target.position(3)
            val written = packMessageTo(message, target)
            written shouldBeEqualTo wire.size
            target.position() shouldBeEqualTo 3 + wire.size
            target.limit() shouldBeEqualTo originalLimit
            target.capacity() shouldBeEqualTo originalCapacity
            target.order() shouldBeEqualTo originalOrder
            val actual = ByteArray(written)
            target.duplicate().apply { position(3); limit(3 + written) }.get(actual)
            actual.contentEquals(wire) shouldBeEqualTo true
        }
    }

    @Test
    fun `packMessageTo succeeds with exact heap and direct capacity`() {
        listOf(ByteBuffer.allocate(wire.size), ByteBuffer.allocateDirect(wire.size)).forEach { target ->
            packMessageTo(message, target) shouldBeEqualTo wire.size
            target.position() shouldBeEqualTo wire.size
            val actual = ByteArray(wire.size)
            target.duplicate().flip().get(actual)
            actual.contentEquals(wire) shouldBeEqualTo true
        }
    }

    @Test
    fun `empty heap direct and sliced sources decode as null without state drift`() {
        val sources = listOf(
            ByteBuffer.allocate(0),
            ByteBuffer.allocateDirect(0),
            ByteBuffer.allocate(4).apply { position(2); limit(2) }.slice(),
        )
        sources.forEach { source ->
            val position = source.position()
            val limit = source.limit()
            unpackMessage<TestMessage>(source).shouldBeNull()
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
        }
    }

    @Test
    fun `non preflight failure restores position but does not promise content rollback`() {
        val target = ByteBuffer.allocate(wire.size + 4).apply { position(2) }
        val packed = ProtoAny.pack(message)

        assertFailsWith<IllegalStateException> {
            writePackedAnyTo(packed, target) { _, output ->
                output.put(0x5A)
                throw IllegalStateException("injected write failure")
            }
        }

        target.position() shouldBeEqualTo 2
        target.get(2) shouldBeEqualTo 0x5A.toByte()
    }

    @Test
    fun `packMessageTo rejects read only and undersized targets before writing`() {
        val readOnly = ByteBuffer.allocate(wire.size + 4).apply { position(2) }.asReadOnlyBuffer()
        val readOnlyPosition = readOnly.position()
        val readOnlyLimit = readOnly.limit()
        assertFailsWith<ReadOnlyBufferException> { packMessageTo(message, readOnly) }
        readOnly.position() shouldBeEqualTo readOnlyPosition
        readOnly.limit() shouldBeEqualTo readOnlyLimit
        val undersized = ByteBuffer.allocate(wire.size - 1).apply { put(0, 0x5A) }
        val start = undersized.position()
        assertFailsWith<BufferOverflowException> { packMessageTo(message, undersized) }
        undersized.position() shouldBeEqualTo start
        undersized.get(0) shouldBeEqualTo 0x5A.toByte()
    }

    @Test
    fun `unpackMessage reads only the bounded range without consuming the source`() {
        val storage = ByteBuffer.allocateDirect(wire.size + 6)
        storage.position(3).put(wire).flip().position(3)
        val source = storage.slice().apply { limit(wire.size) }.asReadOnlyBuffer()
            .order(ByteOrder.LITTLE_ENDIAN)
        val position = source.position()
        val limit = source.limit()
        val order = source.order()
        source.mark()

        unpackMessage<TestMessage>(source) shouldBeEqualTo message
        unpackMessage<NestedMessage>(source).shouldBeNull()
        source.position() shouldBeEqualTo position
        source.limit() shouldBeEqualTo limit
        source.order() shouldBeEqualTo order
        source.reset()
    }

    @Test
    fun `malformed sources preserve position limit mark and order`() {
        val malformed = byteArrayOf(0x0A, 0x7F, 0x01)
        val sources = listOf(
            ByteBuffer.wrap(malformed),
            ByteBuffer.allocateDirect(malformed.size).apply { put(malformed).flip() },
            ByteBuffer.wrap(byteArrayOf(9) + malformed + byteArrayOf(8))
                .apply { position(1); limit(1 + malformed.size) }
                .slice()
                .asReadOnlyBuffer(),
        )
        sources.forEach { source ->
            source.order(ByteOrder.LITTLE_ENDIAN)
            val position = source.position()
            val limit = source.limit()
            val order = source.order()
            source.mark()
            assertFailsWith<com.google.protobuf.InvalidProtocolBufferException> {
                unpackMessage<TestMessage>(source)
            }
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
        }
    }
}
```

- [ ] **Step 2: Run the new test and verify RED**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.MessageSupportByteBufferTest" \
  --no-configuration-cache
```

Expected: compilation FAIL with unresolved `packMessageTo` and `unpackMessage(ByteBuffer)`.

- [ ] **Step 3: Implement `packMessageTo` and `unpackMessage(ByteBuffer)`**

Add the imports and complete functions to `MessageSupport.kt`:

```kotlin
import com.google.protobuf.CodedOutputStream
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

fun <T: Message> packMessageTo(message: T, target: ByteBuffer): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    val packed = ProtoAny.pack(message)
    return writePackedAnyTo(packed, target) { value, outputBuffer ->
        val output = CodedOutputStream.newInstance(outputBuffer)
        value.writeTo(output)
        output.flush()
    }
}

internal inline fun writePackedAnyTo(
    packed: ProtoAny,
    target: ByteBuffer,
    write: (ProtoAny, ByteBuffer) -> Unit,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    if (packed.serializedSize > target.remaining()) throw BufferOverflowException()
    val start = target.position()
    return try {
        write(packed, target)
        (target.position() - start).also { written ->
            check(written == packed.serializedSize) {
                "Expected ${packed.serializedSize} bytes but wrote $written."
            }
        }
    } catch (failure: Throwable) {
        target.position(start)
        throw failure
    }
}

inline fun <reified T: Message> unpackMessage(source: ByteBuffer): T? {
    val any = ProtoAny.parseFrom(source.duplicate())
    return if (any.isA<T>()) any.unpack() else null
}
```

Add KDoc that states non-zero position, limit preservation, raw preflight failures, position-only rollback, caller ownership/thread confinement, and that decode retains no view. It must explicitly say that bytes touched before a non-preflight failure are unspecified and the caller must clear, fully reinitialize, or discard the attempted range before reuse.

- [ ] **Step 4: Run helper and existing wire tests**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.MessageSupportByteBufferTest" \
  --tests "io.bluetape4k.protobuf.MessageSupportTest" \
  --no-configuration-cache
```

Expected: PASS with ByteArray/ByteBuffer wire equality and preserved input state.

- [ ] **Step 5: Commit the public helper APIs**

```bash
git add \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt \
  io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MessageSupportByteBufferTest.kt
git commit -m "Let protobuf callers reuse bounded buffers without a final array copy" \
  -m "Constraint: Preserve Any wire bytes and the BinarySerializer buffer state contract
Rejected: Add a separate size API | exact sizing would duplicate Any.pack work for normal callers
Confidence: high
Scope-risk: moderate
Directive: Do not describe this path as zero-copy
Tested: MessageSupportByteBufferTest and MessageSupportTest
Not-tested: Serializer and Redis dispatch are covered by later tasks"
```

### Task 3: Override ProtobufSerializer buffer dispatch without weakening fallback security

**Files:**

- Create: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerByteBufferTest.kt`
- Modify: `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt`

- [ ] **Step 1: Write failing serializer buffer tests**

Create tests that prove optimized Protobuf wire equality, empty input, preserved source state, raw preflight exceptions, repeated target reuse, and constructor/factory fallback parity:

```kotlin
package io.bluetape4k.protobuf.serializers

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

class ProtobufSerializerByteBufferTest {
    private val serializer = ProtobufSerializer()
    private val message = testMessage { id = 42L; name = "serializer-buffer" }
    private val wire = serializer.serialize(message)

    data class FallbackValue(val id: Int): Serializable

    @Test
    fun `ProtobufSerializer declares both buffer SPI overrides`() {
        ProtobufSerializer::class.java.getDeclaredMethod(
            "serializeTo",
            kotlin.Any::class.java,
            ByteBuffer::class.java,
        )
        ProtobufSerializer::class.java.getDeclaredMethod(
            "deserializeFrom",
            ByteBuffer::class.java,
        )
    }

    @Test
    fun `serializeTo reuses heap and direct targets with identical wire bytes`() {
        listOf(ByteBuffer.allocate(wire.size + 16), ByteBuffer.allocateDirect(wire.size + 16)).forEach { target ->
            repeat(100) {
                target.clear().position(5)
                serializer.serializeTo(message, target) shouldBeEqualTo wire.size
                val actual = ByteArray(wire.size)
                target.duplicate().apply { position(5); limit(5 + wire.size) }.get(actual)
                actual.contentEquals(wire) shouldBeEqualTo true
            }
        }
    }

    @Test
    fun `deserializeFrom supports bounded heap direct sliced and read only sources`() {
        val sources = listOf(
            ByteBuffer.wrap(wire),
            ByteBuffer.allocateDirect(wire.size).apply { put(wire).flip() },
            ByteBuffer.wrap(byteArrayOf(1, 2) + wire + byteArrayOf(3)).apply { position(2); limit(2 + wire.size) }.slice(),
            ByteBuffer.wrap(wire).asReadOnlyBuffer(),
        )
        sources.forEach { source ->
            source.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            val position = source.position()
            val limit = source.limit()
            val order = source.order()
            source.mark()
            serializer.deserializeFrom<TestMessage>(source) shouldBeEqualTo message
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
        }
        val emptySources = listOf(
            ByteBuffer.allocate(0),
            ByteBuffer.allocateDirect(0),
            ByteBuffer.allocate(2).apply { position(1); limit(1) }.slice(),
        )
        emptySources.forEach {
            serializer.deserializeFrom<TestMessage>(it).shouldBeNull()
        }
    }

    @Test
    fun `serializeTo keeps raw overflow and target position`() {
        val target = ByteBuffer.allocate(wire.size - 1)
        assertFailsWith<BufferOverflowException> { serializer.serializeTo(message, target) }
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `serializeTo null writes zero bytes and preserves writable target state`() {
        val target = ByteBuffer.allocate(16).apply { position(5) }
        val limit = target.limit()
        serializer.serializeTo(null, target) shouldBeEqualTo 0
        target.position() shouldBeEqualTo 5
        target.limit() shouldBeEqualTo limit
    }

    @Test
    fun `serializeTo rejects every read only target before serialization dispatch`() {
        val fallback = RecordingFallbackSerializer()
        val trusted = ProtobufSerializer(fallback = fallback)
        val cases = listOf<Pair<ProtobufSerializer, kotlin.Any?>>(
            serializer to message,
            serializer to null,
            serializer to FallbackValue(1),
            trusted to FallbackValue(2),
        )

        cases.forEach { (subject, value) ->
            val target = ByteBuffer.allocate(64).asReadOnlyBuffer()
            assertFailsWith<ReadOnlyBufferException> { subject.serializeTo(value, target) }
            target.position() shouldBeEqualTo 0
        }
        fallback.serializeCalls shouldBeEqualTo 0
    }

    @Test
    fun `public constructor and factory keep the same trusted fallback behavior`() {
        val value = FallbackValue(7)
        val serializers = listOf(
            ProtobufSerializer(fallback = BinarySerializers.Kryo),
            ProtobufSerializer.trustedInternalProtobuf(BinarySerializers.Kryo),
        )
        serializers.forEach { trusted ->
            val bytes = trusted.serialize(value)
            val target = ByteBuffer.allocate(bytes.size)
            trusted.serializeTo(value, target) shouldBeEqualTo bytes.size
            target.position() shouldBeEqualTo bytes.size
            val source = target.duplicate().apply { flip() }
            val targetBytes = ByteArray(source.remaining()).also { source.duplicate().get(it) }
            targetBytes.contentEquals(bytes) shouldBeEqualTo true
            trusted.deserializeFrom<FallbackValue>(source) shouldBeEqualTo value
        }
    }
}
```

`RecordingFallbackSerializer` implements `BinarySerializer`, increments `serializeCalls` before any encode work, and otherwise returns a fixed byte array. The four table rows are required: Protobuf message, null, strict non-message, and trusted fallback value. All must throw the raw `ReadOnlyBufferException` before `ProtoAny.pack`, strict validation, or fallback dispatch.

- [ ] **Step 2: Run the serializer tests and verify RED**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.ProtobufSerializerByteBufferTest" \
  --tests "io.bluetape4k.protobuf.serializers.ProtobufSerializerSecurityTest" \
  --no-configuration-cache
```

Expected: the declared-method reflection test fails before the overrides exist.

- [ ] **Step 3: Refactor Protobuf decode once, then override both buffer methods**

Use these complete private operations in `ProtobufSerializer`:

```kotlin
private val messageClassResolver = ProtobufMessageClassResolver()

private fun decodeProtobuf(source: ByteBuffer): Any {
    val any = ProtoAny.parseFrom(source)
    val className = any.typeUrl.substringAfterLast("/")
    validateClassName(className)
    val classLoader =
        Thread.currentThread().contextClassLoader ?: ProtobufSerializer::class.java.classLoader
    val clazz = messageClassResolver.resolve(className, classLoader)
    return any.unpack(clazz)
}

private fun validateClassName(className: String) {
    if (className.isBlank() || allowedClassPrefixes.none { className.matchesAllowedPrefix(it) }) {
        val cause = IllegalArgumentException(
            "Untrusted Protobuf class: $className. Add the package to allowedClassPrefixes."
        )
        throw SecurityException("Blocked Protobuf deserialization: ${cause.message}", cause)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T: Any> decodeWithTrustedFallback(
    source: ByteBuffer,
    fallbackBytes: () -> ByteArray,
): T? = try {
    decodeProtobuf(source) as? T
} catch (failure: SecurityException) {
    throw failure
} catch (failure: Error) {
    throw failure
} catch (failure: Throwable) {
    val trustedFallback = fallback
        ?: throw SecurityException(
            "Payload is not Protobuf Any and no trusted fallback serializer is configured.",
            failure,
        )
    log.debug(failure) {
        "Protobuf deserialization failed; delegating to the trusted fallback serializer."
    }
    trustedFallback.deserialize(fallbackBytes())
}
```

Replace `doDeserialize` and add overrides:

```kotlin
override fun <T: Any> doDeserialize(bytes: ByteArray): T? =
    decodeWithTrustedFallback(ByteBuffer.wrap(bytes)) { bytes }

override fun serializeTo(graph: Any?, target: ByteBuffer): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    if (graph == null) return 0
    val start = target.position()
    return try {
        if (graph is ProtoMessage) {
            packMessageTo(graph, target)
        } else {
            val bytes = serialize(graph)
            if (bytes.size > target.remaining()) throw BufferOverflowException()
            target.put(bytes)
            bytes.size
        }
    } catch (failure: Throwable) {
        target.position(start)
        when (failure) {
            is ReadOnlyBufferException,
            is BufferOverflowException,
            is BinarySerializationException,
            is Error -> throw failure
            else -> throw BinarySerializationException(
                "Fail to serialize. graphType=${graph.javaClass.name}",
                failure,
            )
        }
    }
}

override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
    if (!source.hasRemaining()) return null
    val size = source.remaining()
    return try {
        decodeWithTrustedFallback(source.duplicate()) {
            ByteArray(size).also { source.duplicate().get(it) }
        }
    } catch (failure: Throwable) {
        when (failure) {
            is Error -> throw failure
            else -> throw BinarySerializationException(
                "Fail to deserialize. bytesSize=$size",
                failure,
            )
        }
    }
}
```

Keep `doSerialize` and the existing public constructors/factory unchanged. Update KDoc with strict default, explicit trusted opt-in, buffer recovery, and lower-copy claim limits.

Clarify preflight ordering in KDoc/tests: read-only rejection always happens before Protobuf or fallback serialization; overflow is raw and happens before any target write, but trusted fallback serialization may run first because its encoded size is not otherwise known. Assert fallback call count `1`, target position unchanged, and raw `BufferOverflowException` for an undersized writable trusted-fallback target.

Add exact regression assertions for both `deserialize(bytes)` and `deserializeFrom(buffer)`: strict malformed protobuf or class resolution/unpack failure preserves the existing outer `BinarySerializationException` and the existing no-fallback `SecurityException` compatibility wrapper/cause chain. Trusted mode may call fallback for those compatibility failures. Allowlist and non-`Message` security failures remain terminal and never reach fallback.

Use this exception matrix as the test oracle; `N` is the exact bounded input size for both entry points:

| Failure | `deserialize(ByteArray)` | `deserializeFrom(ByteBuffer)` |
|---|---|---|
| strict malformed protobuf | outer `BinarySerializationException("Fail to deserialize. bytesSize=N")`; cause depth 1 is `SecurityException("Payload is not Protobuf Any and no trusted fallback serializer is configured.")`; cause depth 2 is the original `InvalidProtocolBufferException` | identical outer type/message and cause chain |
| strict allowed class not found | same outer and depth-1 messages; cause depth 2 is the original `ClassNotFoundException` | identical |
| strict protobuf `Any.unpack` type/payload failure | same outer and depth-1 messages; cause depth 2 is the original unpack `InvalidProtocolBufferException` | identical |
| terminal allowlist/non-`Message` rejection | same outer message; cause depth 1 is `SecurityException`; fallback call count is zero | identical |
| trusted fallback serializer failure | same outer message; cause depth 1 is the fallback's `BinarySerializationException`; its cause remains the backend failure at depth 2 | identical |
| JVM `Error` | preserve the inherited `AbstractBinarySerializer` contract: outer `BinarySerializationException("Fail to deserialize. bytesSize=N")`, cause depth 1 is the same `Error` instance | preserve the buffer SPI contract: rethrow the same `Error` instance without wrapping |

Construct each payload deterministically, assert exact `message`, cause class, cause depth, and object identity for `Error`. Except for the documented raw-buffer `Error` classification, `deserializeFrom` must reproduce the same wrapper depth as `deserialize(ByteArray)`. This requires `deserializeFrom` to wrap a fallback `BinarySerializationException` once more; do not special-case it for direct rethrow.

Keep the null-TCCL regression in the Task 3 GREEN run. The complete `decodeProtobuf` implementation above must use the same effective loader expression as Task 1 and may never reinterpret null TCCL as bootstrap loading.

- [ ] **Step 4: Add terminal security regression coverage to the buffer entry point**

Extend `ProtobufSerializerSecurityTest` with a fallback spy and exercise both public decode entry points:

```kotlin
private class RecordingFallbackSerializer: BinarySerializer {
    var deserializeCalls: Int = 0
        private set

    override fun serialize(graph: Any?): ByteArray = byteArrayOf(1)

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserialize(bytes: ByteArray?): T? {
        deserializeCalls++
        return "unexpected-fallback" as T
    }
}

@Test
fun `terminal security failures never reach trusted fallback`() {
    val blocked = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/io.bluetape4kevil.Blocked")
        .build()
        .toByteArray()
    val nonMessage = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/java.lang.String")
        .build()
        .toByteArray()
    val fallback = RecordingFallbackSerializer()
    val trusted = ProtobufSerializer(
        fallback = fallback,
        allowedClassPrefixes = setOf("io.bluetape4k.", "java.lang."),
    )

    listOf(blocked, nonMessage).forEach { crafted ->
        val byteArrayFailure = assertFailsWith<BinarySerializationException> {
            trusted.deserialize<kotlin.Any>(crafted)
        }
        val bufferFailure = assertFailsWith<BinarySerializationException> {
            trusted.deserializeFrom<kotlin.Any>(ByteBuffer.wrap(crafted))
        }
        listOf(byteArrayFailure, bufferFailure).forEach { failure ->
            generateSequence(failure.cause) { it.cause }
                .any { it is SecurityException }
                .shouldBeTrue()
        }
    }

    fallback.deserializeCalls shouldBeEqualTo 0
}

@Test
fun `allowlist rejection preserves the historical wrapper and cause depth`() {
    val blocked = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/io.bluetape4kevil.Blocked")
        .build()
        .toByteArray()
    val invocations = listOf<() -> Unit>(
        { ProtobufSerializer().deserialize<kotlin.Any>(blocked) },
        { ProtobufSerializer().deserializeFrom<kotlin.Any>(ByteBuffer.wrap(blocked)) },
    )

    invocations.forEach { invoke ->
        val failure = assertFailsWith<BinarySerializationException> { invoke() }
        failure.message shouldBeEqualTo "Fail to deserialize. bytesSize=${blocked.size}"
        val security = failure.causeAt(1)
        (security is SecurityException) shouldBeEqualTo true
        security.message.orEmpty()
            .startsWith("Blocked Protobuf deserialization: Untrusted Protobuf class:") shouldBeEqualTo true
        (failure.causeAt(2) is IllegalArgumentException) shouldBeEqualTo true
    }
}
```

Add these executable helpers/cases beside the terminal test:

```kotlin
private fun Throwable.causeAt(depth: Int): Throwable =
    generateSequence(this) { it.cause }.drop(depth).first()

private inline fun <T> withContextLoader(loader: ClassLoader, block: () -> T): T {
    val thread = Thread.currentThread()
    val original = thread.contextClassLoader
    return try {
        thread.contextClassLoader = loader
        block()
    } finally {
        thread.contextClassLoader = original
    }
}

private class ForcedFailureClassLoader(
    parent: ClassLoader,
    private val target: String,
    private val failure: () -> Throwable,
): ClassLoader(parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name == target) throw failure()
        return super.loadClass(name, resolve)
    }
}

@Test
fun `strict compatibility failures keep the same two entrypoint cause chain`() {
    val malformed = byteArrayOf(0x80.toByte())
    val missing = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/io.bluetape4k.missing.MissingMessage")
        .build().toByteArray()
    val unpack = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/${TestMessage::class.java.name}")
        .setValue(com.google.protobuf.ByteString.copyFrom(byteArrayOf(0x80.toByte())))
        .build().toByteArray()
    val cases = listOf(
        malformed to com.google.protobuf.InvalidProtocolBufferException::class.java,
        missing to ClassNotFoundException::class.java,
        unpack to com.google.protobuf.InvalidProtocolBufferException::class.java,
    )
    cases.forEach { (bytes, originalType) ->
        val failures = listOf(
            assertFailsWith<BinarySerializationException> { ProtobufSerializer().deserialize<Any>(bytes) },
            assertFailsWith<BinarySerializationException> {
                ProtobufSerializer().deserializeFrom<Any>(ByteBuffer.wrap(bytes))
            },
        )
        failures.forEach { failure ->
            failure.message shouldBeEqualTo "Fail to deserialize. bytesSize=${bytes.size}"
            (failure.causeAt(1) is SecurityException) shouldBeEqualTo true
            originalType.isInstance(failure.causeAt(2)) shouldBeEqualTo true
        }
    }
}

@Test
fun `class loading Errors are terminal and never reach fallback`() {
    listOf<LinkageError>(NoClassDefFoundError("forced"), ExceptionInInitializerError("forced")).forEach { sentinel ->
        val fallback = RecordingFallbackSerializer()
        val serializer = ProtobufSerializer(fallback = fallback)
        val bytes = serializer.serialize(testMessage { id = 1L })
        val loader = ForcedFailureClassLoader(
            TestMessage::class.java.classLoader,
            TestMessage::class.java.name,
        ) { sentinel }
        withContextLoader(loader) {
            val byteArrayFailure = assertFailsWith<BinarySerializationException> {
                serializer.deserialize<TestMessage>(bytes)
            }
            (byteArrayFailure.cause === sentinel) shouldBeEqualTo true
            val bufferFailure = assertFailsWith<LinkageError> {
                serializer.deserializeFrom<TestMessage>(ByteBuffer.wrap(bytes))
            }
            (bufferFailure === sentinel) shouldBeEqualTo true
        }
        fallback.deserializeCalls shouldBeEqualTo 0
    }
}
```

Add the trusted compatibility and fallback-backend cases as executable code, importing `AbstractBinarySerializer`:

```kotlin
private data class TrustedFallbackSentinel(val id: Int)

private class ReturningTrustedFallback(
    private val result: TrustedFallbackSentinel,
): BinarySerializer {
    var deserializeCalls: Int = 0
        private set

    override fun serialize(graph: kotlin.Any?): ByteArray = byteArrayOf(1)

    @Suppress("UNCHECKED_CAST")
    override fun <T: kotlin.Any> deserialize(bytes: ByteArray?): T? {
        deserializeCalls++
        return result as T
    }
}

private class ThrowingBackendFallback(
    val backendFailure: Throwable,
): AbstractBinarySerializer() {
    override fun doSerialize(graph: kotlin.Any): ByteArray =
        throw UnsupportedOperationException("not used")

    override fun <T: kotlin.Any> doDeserialize(bytes: ByteArray): T? =
        throw backendFailure
}

private fun trustedCompatibilityPayloads(): List<ByteArray> {
    val malformed = byteArrayOf(0x80.toByte())
    val missing = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/io.bluetape4k.missing.MissingMessage")
        .build().toByteArray()
    val unpack = Any.newBuilder()
        .setTypeUrl("type.googleapis.com/${TestMessage::class.java.name}")
        .setValue(com.google.protobuf.ByteString.copyFrom(byteArrayOf(0x80.toByte())))
        .build().toByteArray()
    return listOf(malformed, missing, unpack)
}

@Test
fun `trusted compatibility failures invoke fallback exactly once per entrypoint`() {
    trustedCompatibilityPayloads().forEachIndexed { index, bytes ->
        val sentinel = TrustedFallbackSentinel(index)
        val invocations = listOf<(ProtobufSerializer) -> TrustedFallbackSentinel?>(
            { it.deserialize<TrustedFallbackSentinel>(bytes) },
            { it.deserializeFrom<TrustedFallbackSentinel>(ByteBuffer.wrap(bytes)) },
        )
        invocations.forEach { invoke ->
            val fallback = ReturningTrustedFallback(sentinel)
            val serializer = ProtobufSerializer(fallback = fallback)

            invoke(serializer) shouldBeEqualTo sentinel
            fallback.deserializeCalls shouldBeEqualTo 1
        }
    }
}

@Test
fun `trusted fallback backend failure keeps the same two entrypoint cause chain`() {
    val bytes = byteArrayOf(0x80.toByte())
    val invocations = listOf<(ProtobufSerializer) -> Unit>(
        { it.deserialize<TrustedFallbackSentinel>(bytes) },
        { it.deserializeFrom<TrustedFallbackSentinel>(ByteBuffer.wrap(bytes)) },
    )
    invocations.forEach { invoke ->
        val backendFailure = IllegalStateException("fallback-backend")
        val fallback = ThrowingBackendFallback(backendFailure)
        val failure = assertFailsWith<BinarySerializationException> {
            invoke(ProtobufSerializer(fallback = fallback))
        }

        failure.message shouldBeEqualTo "Fail to deserialize. bytesSize=${bytes.size}"
        (failure.causeAt(1) is BinarySerializationException) shouldBeEqualTo true
        (failure.causeAt(2) === backendFailure) shouldBeEqualTo true
    }
}
```

Together with `terminal security failures never reach trusted fallback`, these tests are the required executable matrix; no prose-only case may satisfy Step 4.

- [ ] **Step 5: Run serializer and security tests GREEN**

Run the Step 2 command plus existing `ProtobufSerializerTest`.

Expected: PASS; strict exceptions remain wrapped, trusted fallback remains compatible, and repeated buffer calls do not drift.

- [ ] **Step 6: Commit serializer dispatch**

```bash
git add \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt \
  io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerByteBufferTest.kt \
  io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt
git commit -m "Route protobuf serializer success paths through caller buffers" \
  -m "Constraint: Strict exceptions and trusted historical payloads must remain compatible
Rejected: Optimize trusted fallback | its isolation copy is a deliberate compatibility boundary
Confidence: high
Scope-risk: moderate
Directive: Keep allowlist and non-Message failures terminal before fallback
Tested: ProtobufSerializer buffer, compatibility, and security tests
Not-tested: Redisson decode and allocation evidence follow in later tasks"
```

### Task 4: Route contiguous Redisson decode through a bounded NIO view

**Files:**

- Modify: `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecTest.kt`

- [ ] **Step 1: Write failing contiguous/composite/resource tests**

First make the existing test helper release the encoded buffer it owns so the new ref-count assertions run on a leak-free harness:

```kotlin
@Suppress("UNCHECKED_CAST")
private fun <T> Codec.verifyCodec(origin: T) {
    val buf = valueEncoder.encode(origin)
    try {
        val actual = valueDecoder.decode(buf, State()) as? T
        actual shouldBeEqualTo origin
    } finally {
        buf.release()
    }
}
```

Add a fallback codec that records the temporary buffer and can throw:

```kotlin
private class TrackingFallbackCodec(
    private val result: Any? = null,
    private val failure: Throwable? = null,
): BaseCodec() {
    var seen: ByteBuf? = null
        private set

    private val decoder = Decoder<Any> { input, _ ->
        seen = input
        failure?.let { throw it }
        checkNotNull(result)
    }

    override fun getValueEncoder(): Encoder = Encoder { Unpooled.EMPTY_BUFFER }
    override fun getValueDecoder(): Decoder<Any> = decoder
}
```

Use one state assertion helper around every decoder success and failure case:

```kotlin
private inline fun <T> ByteBuf.withPreservedDecoderState(block: () -> T): T {
    val originalReaderIndex = readerIndex()
    val originalWriterIndex = writerIndex()
    val originalRefCnt = refCnt()
    return try {
        block()
    } finally {
        readerIndex() shouldBeEqualTo originalReaderIndex
        writerIndex() shouldBeEqualTo originalWriterIndex
        refCnt() shouldBeEqualTo originalRefCnt
    }
}
```

Add a bounded-view probe, importing `io.netty.buffer.SwappedByteBuf` and `java.nio.ByteBuffer`. The test must fail against the current `getBytes(copy = true)` implementation because that path never asks the input for the one bounded NIO view:

```kotlin
private class TrackingNioViewByteBuf(delegate: ByteBuf): SwappedByteBuf(delegate) {
    var boundedNioCalls: Int = 0
        private set

    override fun nioBuffer(index: Int, length: Int): ByteBuffer {
        boundedNioCalls++
        index shouldBeEqualTo readerIndex()
        length shouldBeEqualTo readableBytes()
        return super.nioBuffer(index, length)
    }
}

@Test
fun `strict contiguous heap and direct decode use one bounded nio view`() {
    val codec = RedissonProtobufCodec()
    val origin = newSimpleMessage()
    val encoded = codec.valueEncoder.encode(origin)
    val wire = try {
        ByteArray(encoded.readableBytes()).also {
            encoded.getBytes(encoded.readerIndex(), it)
        }
    } finally {
        encoded.release()
    }
    val delegates = listOf(
        Unpooled.wrappedBuffer(byteArrayOf(0x11, 0x22) + wire + byteArrayOf(0x33)),
        Unpooled.directBuffer(wire.size + 3).apply {
            writeByte(0x11).writeByte(0x22).writeBytes(wire).writeByte(0x33)
        },
    )

    delegates.forEach { delegate ->
        delegate.setIndex(2, 2 + wire.size)
        val tracked = TrackingNioViewByteBuf(delegate)
        val decoded = try {
            tracked.withPreservedDecoderState {
                codec.valueDecoder.decode(tracked, State())
            }
        } finally {
            tracked.release()
        }
        tracked.boundedNioCalls shouldBeEqualTo 1
        decoded shouldBeEqualTo origin
    }
}
```

Apply it to contiguous success, malformed/truncated Any, blocked prefix, allowlisted non-`Message`, composite compatibility, trusted fallback success, and trusted fallback failure. The test that owns each input must release it in an outer `finally`; after release, assert the decoded protobuf message fields remain readable so no returned value retains the NIO view.

Add tests:

```kotlin
@Test
fun `strict decode preserves contiguous input indices and reference count`() {
    val codec = RedissonProtobufCodec()
    val origin = newSimpleMessage()
    val encoded = codec.valueEncoder.encode(origin)
    try {
        val readerIndex = encoded.readerIndex()
        val writerIndex = encoded.writerIndex()
        val refCnt = encoded.refCnt()
        codec.valueDecoder.decode(encoded, State()) shouldBeEqualTo origin
        encoded.readerIndex() shouldBeEqualTo readerIndex
        encoded.writerIndex() shouldBeEqualTo writerIndex
        encoded.refCnt() shouldBeEqualTo refCnt
    } finally {
        encoded.release()
    }
}

@Test
fun `composite input uses the compatibility copy and still decodes`() {
    val codec = RedissonProtobufCodec()
    val origin = newSimpleMessage()
        val encoded = codec.valueEncoder.encode(origin)
        val bytes = try {
            ByteArray(encoded.readableBytes()).also {
                encoded.getBytes(encoded.readerIndex(), it)
            }
        } finally {
            encoded.release()
        }
    val split = bytes.size / 2
    val composite = Unpooled.compositeBuffer().addComponents(
        true,
        Unpooled.wrappedBuffer(bytes, 0, split),
        Unpooled.wrappedBuffer(bytes, split, bytes.size - split),
    )
    try {
        codec.valueDecoder.decode(composite, State()) shouldBeEqualTo origin
    } finally {
        composite.release()
    }
}

@Test
fun `trusted fallback releases its isolated copy on success and failure`() {
    listOf(
        TrackingFallbackCodec(result = "fallback"),
        TrackingFallbackCodec(failure = IllegalStateException("boom")),
    ).forEach { fallback ->
        val codec = RedissonProtobufCodec(fallback)
        val input = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
        try {
            runCatching { codec.valueDecoder.decode(input, State()) }
            fallback.seen!!.refCnt() shouldBeEqualTo 0
            input.refCnt() shouldBeEqualTo 1
        } finally {
            input.release()
        }
    }
}
```

- [ ] **Step 2: Run Redisson tests and verify RED**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodecTest" \
  --no-configuration-cache
```

Expected: fallback copy remains unreleased, and the bounded-view probe reports zero `nioBuffer` calls because strict decode still performs `getBytes(copy = true)`.

- [ ] **Step 3: Implement contiguous view, composite copy, shared resolution, and exact fallback cleanup**

Replace the class-name cache with:

```kotlin
private val messageClassResolver = ProtobufMessageClassResolver()
```

Add these operations:

```kotlin
private fun decodeProtobuf(buf: ByteBuf): Any {
    val any = if (buf.nioBufferCount() == 1) {
        AnyMessage.parseFrom(buf.nioBuffer(buf.readerIndex(), buf.readableBytes()))
    } else {
        AnyMessage.parseFrom(buf.getBytes(copy = true))
    }
    val className = any.typeUrl.substringAfterLast("/")
    validateClassName(className)
    val effectiveLoader =
        classLoader
            ?: Thread.currentThread().contextClassLoader
            ?: RedissonProtobufCodec::class.java.classLoader
    val clazz = messageClassResolver.resolve(className, effectiveLoader)
    return any.unpack(clazz)
}

private fun decodeFallback(buf: ByteBuf, state: State?, failure: Throwable): Any {
    val trustedFallback = fallbackCodec
        ?: throw SecurityException(
            "Payload is not Protobuf Any and no trusted fallback codec is configured.",
            failure,
        )
    log.debug(failure) {
        "Decoding: Protobuf 메시지가 아닙니다. fallbackCodec[$trustedFallback] 사용."
    }
    val copied = Unpooled.wrappedBuffer(buf.getBytes(copy = true))
    var operationFailure: Throwable? = null
    return try {
        trustedFallback.valueDecoder.decode(copied, state).also { result ->
            if (copied.refCnt() != 1) {
                throw SecurityException("Trusted fallback changed the owned input reference count.")
            }
            if (result is ByteBuf && result.references(copied)) {
                throw SecurityException("Trusted fallback returned a view of its temporary input.")
            }
        }
    } catch (caught: Throwable) {
        operationFailure = caught
        throw caught
    } finally {
        releaseOwnedBuffer(copied, operationFailure)
    }
}

internal fun releaseOwnedBuffer(copied: ByteBuf, operationFailure: Throwable?) {
    try {
        while (copied.refCnt() > 0) copied.release()
    } catch (cleanupFailure: Throwable) {
        operationFailure?.addSuppressed(cleanupFailure) ?: throw cleanupFailure
    }
}

private fun ByteBuf.references(root: ByteBuf): Boolean {
    val visited = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<ByteBuf, Boolean>()
    )
    var current: ByteBuf? = this
    while (current != null) {
        if (current === root) return true
        if (!visited.add(current)) return true
        current = current.unwrap()
    }
    return false
}
```

Replace the decoder body:

```kotlin
private val decoder: Decoder<Any> = Decoder { buf, state ->
    try {
        decodeProtobuf(buf)
    } catch (failure: SecurityException) {
        throw failure
    } catch (failure: Error) {
        throw failure
    } catch (failure: Throwable) {
        decodeFallback(buf, state, failure)
    }
}
```

The synchronous ownership check rejects direct retained/released/derived-view misuse and drains every remaining reference without masking the original operation failure. KDoc must also state the unavoidable trusted-codec precondition: a fallback result must not hide the temporary input inside an arbitrary object graph or transfer it to another thread.

Import `ProtobufMessageClassResolver`; remove `ConcurrentHashMap` and unchecked class casts. Preserve the #649 encode function byte-for-byte.

- [ ] **Step 4: Add a Redisson non-Message terminal test**

```kotlin
@Test
fun `allowlisted non message class never reaches fallback`() {
    val fallback = TrackingFallbackCodec(result = "must-not-run")
    val codec = RedissonProtobufCodec(fallback, setOf("java.lang."))
    val bytes = AnyMessage.newBuilder()
        .setTypeUrl("type.googleapis.com/java.lang.String")
        .build()
        .toByteArray()
    val input = Unpooled.wrappedBuffer(bytes)
    try {
        assertFailsWith<SecurityException> { codec.valueDecoder.decode(input, State()) }
        fallback.seen shouldBeEqualTo null
    } finally {
        input.release()
    }
}
```

Add the hostile codec and exact loop below:

```kotlin
private enum class HostileBehavior {
    RETAIN_AND_RETURN,
    EARLY_RELEASE_AND_RETURN,
    RETURN_SLICE,
    THROW_AFTER_RETAIN,
}

private class HostileFallbackCodec(
    private val behavior: HostileBehavior,
): BaseCodec() {
    val sentinel = IllegalStateException("throw-after-retain")
    var calls: Int = 0
        private set
    var seen: ByteBuf? = null
        private set

    override fun getValueEncoder(): Encoder = Encoder { Unpooled.EMPTY_BUFFER }
    override fun getValueDecoder(): Decoder<Any> = Decoder { input, _ ->
        calls++
        seen = input
        when (behavior) {
            HostileBehavior.RETAIN_AND_RETURN -> input.retain().let { "retained" }
            HostileBehavior.EARLY_RELEASE_AND_RETURN -> input.release().let { "released" }
            HostileBehavior.RETURN_SLICE -> input.slice()
            HostileBehavior.THROW_AFTER_RETAIN -> {
                input.retain()
                throw sentinel
            }
        }
    }
}

@Test
fun `hostile trusted fallback cannot escape or leak its isolated input`() {
    HostileBehavior.entries.forEach { behavior ->
        val fallback = HostileFallbackCodec(behavior)
        val codec = RedissonProtobufCodec(fallback)
        val callerInput = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
        val reader = callerInput.readerIndex()
        val writer = callerInput.writerIndex()
        val refs = callerInput.refCnt()
        try {
            val failure = assertFailsWith<Throwable> {
                codec.valueDecoder.decode(callerInput, State())
            }
            if (behavior == HostileBehavior.THROW_AFTER_RETAIN) {
                (failure === fallback.sentinel) shouldBeEqualTo true
            } else {
                (failure is SecurityException) shouldBeEqualTo true
            }
            fallback.calls shouldBeEqualTo 1
            fallback.seen!!.refCnt() shouldBeEqualTo 0
            callerInput.readerIndex() shouldBeEqualTo reader
            callerInput.writerIndex() shouldBeEqualTo writer
            callerInput.refCnt() shouldBeEqualTo refs
        } finally {
            callerInput.release()
        }
    }
}
```

Add an executable cleanup-failure seam test, importing `io.netty.buffer.SwappedByteBuf`:

```kotlin
private class ThrowOnFinalReleaseByteBuf(
    private val delegate: ByteBuf,
    val cleanupFailure: Throwable,
): SwappedByteBuf(delegate) {
    override fun release(): Boolean {
        if (refCnt() == 1) throw cleanupFailure
        return super.release()
    }

    fun forceReleaseDelegate() {
        while (delegate.refCnt() > 0) delegate.release()
    }
}

@Test
fun `cleanup failure is suppressed without replacing the operation failure`() {
    val operationFailure = IllegalStateException("operation")
    val cleanupFailure = IllegalStateException("cleanup")
    val owned = ThrowOnFinalReleaseByteBuf(
        Unpooled.buffer(1).writeByte(1),
        cleanupFailure,
    )
    try {
        releaseOwnedBuffer(owned, operationFailure)

        operationFailure.suppressed.size shouldBeEqualTo 1
        (operationFailure.suppressed.single() === cleanupFailure) shouldBeEqualTo true
        owned.refCnt() shouldBeEqualTo 1
    } finally {
        owned.forceReleaseDelegate()
    }
}
```

The `decodeFallback` catch must still throw the original operation sentinel; its `finally` only calls `releaseOwnedBuffer`, so the test above proves the cleanup exception is attached exactly once without becoming the primary failure. Add the same state assertions for malformed Any, blocked type URL, wrong `Message` type, composite compatibility, and successful trusted fallback; after releasing the test-owned input, assert the decoded protobuf value remains usable.

- [ ] **Step 5: Run Redisson and full protobuf tests GREEN**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodecTest" \
  --no-configuration-cache
./gradlew :bluetape4k-protobuf:test --no-configuration-cache
```

Expected: all tests PASS; input indices/refCnt remain unchanged and temporary fallback buffers reach refCnt 0.

- [ ] **Step 6: Commit the Redisson decode boundary**

```bash
git add \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt \
  io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecTest.kt
git commit -m "Avoid the explicit array handoff for contiguous Redisson protobuf input" \
  -m "Constraint: Framework-owned ByteBuf indices and reference counts must remain unchanged
Rejected: Treat every nioBuffer call as a view | composite buffers may merge-copy components
Confidence: high
Scope-risk: moderate
Directive: Keep composite and trusted fallback cells claim-ineligible
Tested: RedissonProtobufCodecTest and full protobuf module tests
Not-tested: Lettuce remains the second issue 757 pull request"
```

### Task 5: Rebuild the protobuf benchmark as a deterministic allocation matrix

**Files:**

- Move: `benchmark/protobuf-codec-benchmark/src/benchmark/proto/protobuf/benchmark-message.proto` → `benchmark/protobuf-codec-benchmark/src/main/proto/protobuf/benchmark-message.proto`
- Create: `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupport.kt`
- Create: `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkMetadata.kt`
- Create: `benchmark/protobuf-codec-benchmark/src/test/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupportTest.kt`
- Modify: `benchmark/protobuf-codec-benchmark/build.gradle.kts`
- Modify: `benchmark/protobuf-codec-benchmark/src/benchmark/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmark.kt`

- [ ] **Step 1: Move the canonical proto and wire main/test dependencies**

Move the proto without changing its schema. Change dependencies to:

```kotlin
dependencies {
    implementation(project(":bluetape4k-protobuf"))
    implementation(project(":bluetape4k-redisson"))
    testImplementation(project(":bluetape4k-junit5"))

    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
    add("benchmarkRuntimeOnly", libs.logback.classic)
}
```

Keep the existing configuration inheritance. Add the serializer benchmark's signature exclusion exactly:

```kotlin
tasks.matching { it.name.endsWith("BenchmarkJar") }.configureEach {
    this as org.gradle.jvm.tasks.Jar
    exclude("META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.SF")
}
```

- [ ] **Step 2: Write failing fixture state tests**

Create `ProtobufCodecBenchmarkSupportTest.kt`:

```kotlin
package io.bluetape4k.protobuf.benchmark

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ProtobufCodecBenchmarkSupportTest {
    @Test
    fun `fixture validates semantic equality and stable payload identity`() {
        ProtobufCodecBenchmarkFixture().validate()
    }

    @Test
    fun `one thousand invocation resets do not overflow or read stale bytes`() {
        val fixture = ProtobufCodecBenchmarkFixture()
        repeat(1_000) {
            fixture.resetInvocation()
            fixture.serializerEncodeHeap() shouldBeEqualTo fixture.wireSize
            fixture.serializerDecodeHeap() shouldBeEqualTo fixture.payload
        }
    }

    @Test
    fun `expected benchmark matrix remains exact`() {
        ProtobufBenchmarkMatrix.expectedMethods.size shouldBeEqualTo 13
        ProtobufBenchmarkMatrix.claimEligible.size shouldBeEqualTo 5
    }
}
```

- [ ] **Step 3: Run fixture tests and verify RED**

```bash
./gradlew :protobuf-codec-benchmark:test --no-configuration-cache
```

Expected: compilation FAIL until the support fixture and matrix exist.

- [ ] **Step 4: Implement the deterministic fixture and exact method set**

Create `ProtobufCodecBenchmarkSupport.kt` with these public contracts and complete matrix:

```kotlin
package io.bluetape4k.protobuf.benchmark

import com.google.protobuf.Any as ProtoAny
import com.google.protobuf.Message
import io.bluetape4k.protobuf.benchmark.messages.BenchmarkMessage
import io.bluetape4k.protobuf.benchmark.messages.benchmarkMessage
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.protobuf.serializers.redis.RedissonProtobufCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.handler.State
import java.io.Serializable
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object ProtobufBenchmarkMatrix {
    const val VERSION = "issue-757-v1"
    const val TARGET_HEADROOM = 32
    const val TARGET_START = 3
    const val PAYLOAD_IDENTITY = "BenchmarkMessage:id=42;payload=protobuf-payload-*128"
    val expectedMethods = setOf(
        "serializerEncodeByteArray",
        "serializerEncodeHeapOptimized",
        "serializerEncodeDirectOptimized",
        "serializerDecodeByteArray",
        "serializerDecodeHeapOptimized",
        "serializerDecodeDirectOptimized",
        "redissonDecodeCopiedByteArray",
        "redissonDecodeContiguousOptimized",
        "redissonDecodeCompositeCompatibility",
        "trustedFallbackEncodeByteArray",
        "trustedFallbackEncodeBufferCompatibility",
        "trustedFallbackDecodeByteArray",
        "trustedFallbackDecodeBufferCompatibility",
    )
    val claimEligible = expectedMethods.filterTo(mutableSetOf()) { it.endsWith("Optimized") }
}

data class FallbackPayload(val id: Long, val values: List<String>): Serializable

private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
        }
    }
    append('"')
}

private fun jsonStrings(values: List<String>): String =
    values.joinToString(prefix = "[", postfix = "]", separator = ",") { jsonString(it) }

private fun canonicalConfigJson(
    allowedPrefixes: List<String>,
    directCapacity: Int,
    directInitialPosition: Int,
    heapCapacity: Int,
    heapInitialPosition: Int,
    matrixVersion: String,
    methods: List<String>,
    payloadIdentity: String,
    payloadSha256: String,
    redissonCodecClass: String,
    serializerClass: String,
    targetHeadroom: Int,
    targetStart: Int,
): String = buildString {
    // Lexicographically sorted keys, no insignificant whitespace: identical to Python json.dumps below.
    append("{\"allowed_class_prefixes\":").append(jsonStrings(allowedPrefixes))
    append(",\"direct_capacity\":").append(directCapacity)
    append(",\"direct_initial_position\":").append(directInitialPosition)
    append(",\"heap_capacity\":").append(heapCapacity)
    append(",\"heap_initial_position\":").append(heapInitialPosition)
    append(",\"matrix_version\":").append(jsonString(matrixVersion))
    append(",\"methods\":").append(jsonStrings(methods))
    append(",\"payload_identity\":").append(jsonString(payloadIdentity))
    append(",\"payload_sha256\":").append(jsonString(payloadSha256))
    append(",\"redisson_codec_class\":").append(jsonString(redissonCodecClass))
    append(",\"serializer_class\":").append(jsonString(serializerClass))
    append(",\"target_headroom\":").append(targetHeadroom)
    append(",\"target_start\":").append(targetStart)
    append('}')
}

class ProtobufCodecBenchmarkFixture {
    val payload: BenchmarkMessage = benchmarkMessage {
        id = 42L
        payload = "protobuf-payload-".repeat(128)
    }
    private val serializer = ProtobufSerializer()
    private val trusted = ProtobufSerializer.trustedInternalProtobuf()
    private val redisson = RedissonProtobufCodec()
    private val redissonBaselineClasses = ConcurrentHashMap<String, Class<out Message>>()
    private val fallback = FallbackPayload(42L, List(64) { "value-$it" })
    private val wire = serializer.serialize(payload)
    private val fallbackWire = trusted.serialize(fallback)
    private val heapTarget = ByteBuffer.allocate(wire.size + ProtobufBenchmarkMatrix.TARGET_HEADROOM)
    private val directTarget = ByteBuffer.allocateDirect(wire.size + ProtobufBenchmarkMatrix.TARGET_HEADROOM)
    private val fallbackTarget = ByteBuffer.allocate(fallbackWire.size + ProtobufBenchmarkMatrix.TARGET_HEADROOM)
    private val heapSource = ByteBuffer.wrap(wire)
    private val directSource = ByteBuffer.allocateDirect(wire.size).apply { put(wire).flip() }
    private val fallbackSource = ByteBuffer.wrap(fallbackWire)
    private val redissonInput: ByteBuf = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(wire))

    val payloadSha256: String = MessageDigest.getInstance("SHA-256")
        .digest(wire)
        .joinToString("") { "%02x".format(it) }
    val wireSize: Int get() = wire.size
    val configIdentity: String = canonicalConfigJson(
        allowedPrefixes = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES.sorted(),
        directCapacity = directTarget.capacity(),
        directInitialPosition = ProtobufBenchmarkMatrix.TARGET_START,
        heapCapacity = heapTarget.capacity(),
        heapInitialPosition = ProtobufBenchmarkMatrix.TARGET_START,
        matrixVersion = ProtobufBenchmarkMatrix.VERSION,
        methods = ProtobufBenchmarkMatrix.expectedMethods.sorted(),
        payloadIdentity = ProtobufBenchmarkMatrix.PAYLOAD_IDENTITY,
        payloadSha256 = payloadSha256,
        redissonCodecClass = RedissonProtobufCodec::class.java.name,
        serializerClass = ProtobufSerializer::class.java.name,
        targetHeadroom = ProtobufBenchmarkMatrix.TARGET_HEADROOM,
        targetStart = ProtobufBenchmarkMatrix.TARGET_START,
    )
    val configSha256: String = MessageDigest.getInstance("SHA-256")
        .digest(configIdentity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    fun resetInvocation() {
        heapTarget.clear().position(ProtobufBenchmarkMatrix.TARGET_START)
        directTarget.clear().position(ProtobufBenchmarkMatrix.TARGET_START)
        fallbackTarget.clear().position(ProtobufBenchmarkMatrix.TARGET_START)
        heapSource.position(0).limit(wire.size)
        directSource.position(0).limit(wire.size)
        fallbackSource.position(0).limit(fallbackWire.size)
        redissonInput.setIndex(0, wire.size)
    }

    fun serializerEncodeByteArray(): ByteArray = serializer.serialize(payload)
    fun serializerEncodeHeap(): Int = serializer.serializeTo(payload, heapTarget)
    fun serializerEncodeDirect(): Int = serializer.serializeTo(payload, directTarget)
    fun serializerDecodeByteArray(): BenchmarkMessage? = serializer.deserialize(wire)
    fun serializerDecodeHeap(): BenchmarkMessage? = serializer.deserializeFrom(heapSource)
    fun serializerDecodeDirect(): BenchmarkMessage? = serializer.deserializeFrom(directSource)

    fun redissonDecodeCopied(): Any {
        val copied = ByteArray(redissonInput.readableBytes())
        redissonInput.getBytes(redissonInput.readerIndex(), copied)
        val any = ProtoAny.parseFrom(copied)
        val className = any.typeUrl.substringAfterLast("/")
        check(
            ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES.any { prefix ->
                className == prefix || className.startsWith(if (prefix.endsWith('.')) prefix else "$prefix.")
            }
        ) { "Unexpected benchmark Protobuf class: $className" }
        val clazz = redissonBaselineClasses.computeIfAbsent(className) {
            Class.forName(it, false, Thread.currentThread().contextClassLoader)
                .asSubclass(Message::class.java)
        }
        return any.unpack(clazz)
    }

    fun redissonDecodeContiguous(): Any = redisson.valueDecoder.decode(redissonInput, State())

    fun redissonDecodeComposite(): Any {
        val split = wire.size / 2
        val composite = Unpooled.compositeBuffer().addComponents(
            true,
            Unpooled.wrappedBuffer(wire, 0, split),
            Unpooled.wrappedBuffer(wire, split, wire.size - split),
        )
        return try {
            redisson.valueDecoder.decode(composite, State())
        } finally {
            composite.release()
        }
    }

    fun trustedEncodeByteArray(): ByteArray = trusted.serialize(fallback)
    fun trustedEncodeBuffer(): Int = trusted.serializeTo(fallback, fallbackTarget)
    fun trustedDecodeByteArray(): FallbackPayload? = trusted.deserialize(fallbackWire)
    fun trustedDecodeBuffer(): FallbackPayload? = trusted.deserializeFrom(fallbackSource)

    fun validate() {
        check(wire.isNotEmpty())
        check(payloadSha256.length == 64)
        check(configSha256.length == 64)
        resetInvocation()
        check(serializerEncodeHeap() == wire.size)
        resetInvocation()
        check(serializerEncodeDirect() == wire.size)
        resetInvocation()
        check(serializerDecodeHeap() == payload)
        resetInvocation()
        check(serializerDecodeDirect() == payload)
        resetInvocation()
        check(redissonDecodeContiguous() == payload)
        resetInvocation()
        check(trustedDecodeBuffer() == fallback)
    }
}
```

Create `ProtobufCodecBenchmarkMetadata.kt` as the only authority bridge from the built artifact:

```kotlin
package io.bluetape4k.protobuf.benchmark

object ProtobufCodecBenchmarkMetadata {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.contentEquals(arrayOf("--json")))
        val fixture = ProtobufCodecBenchmarkFixture().also { it.validate() }
        println(
            buildString {
                append("{\"config_json\":").append(jsonString(fixture.configIdentity))
                append(",\"config_sha256\":").append(jsonString(fixture.configSha256))
                append(",\"matrix_version\":").append(jsonString(ProtobufBenchmarkMatrix.VERSION))
                append(",\"payload_sha256\":").append(jsonString(fixture.payloadSha256))
                append(",\"payload_size\":").append(fixture.wireSize)
                append(",\"schema_version\":1")
                append(",\"target_headroom\":").append(ProtobufBenchmarkMatrix.TARGET_HEADROOM)
                append(",\"target_start\":").append(ProtobufBenchmarkMatrix.TARGET_START)
                append('}')
            }
        )
    }
}
```

Make `jsonString` package-internal so both support and metadata use the same encoder. Fixture tests invoke `ProtobufCodecBenchmarkMetadata.main(arrayOf("--json"))`, capture stdout, parse the exact one-line JSON, and assert it matches fixture values. After the JMH JAR is built, run `java -cp "$JMH_JARS" io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmarkMetadata --json` and require one JSON line before method listing.

The copied Redisson control above performs the same parse, allowlist, warmed class-resolution, unpack, and result-consumption work as the production candidate. Its explicit `ByteArray` handoff is the only payload-sized copy under comparison. Keep that control inside this benchmark-only module; do not expose a production benchmark API.

- [ ] **Step 5: Replace the benchmark class with thread-confined 13-cell methods**

Use `@State(Scope.Thread)`, one trial setup, and invocation reset. Every timed method delegates exactly once and returns/consumes the result:

```kotlin
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
class ProtobufCodecBenchmark {
    private lateinit var fixture: ProtobufCodecBenchmarkFixture

    @Param(ProtobufBenchmarkMatrix.VERSION)
    lateinit var matrixVersion: String

    @Param("32")
    var targetHeadroom: Int = 0

    @Param("3")
    var targetStart: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        check(matrixVersion == ProtobufBenchmarkMatrix.VERSION)
        check(targetHeadroom == ProtobufBenchmarkMatrix.TARGET_HEADROOM)
        check(targetStart == ProtobufBenchmarkMatrix.TARGET_START)
        fixture = ProtobufCodecBenchmarkFixture().also { it.validate() }
    }

    @Setup(Level.Invocation)
    fun resetInvocation() = fixture.resetInvocation()

    @Benchmark fun serializerEncodeByteArray() = fixture.serializerEncodeByteArray()
    @Benchmark fun serializerEncodeHeapOptimized() = fixture.serializerEncodeHeap()
    @Benchmark fun serializerEncodeDirectOptimized() = fixture.serializerEncodeDirect()
    @Benchmark fun serializerDecodeByteArray() = fixture.serializerDecodeByteArray()
    @Benchmark fun serializerDecodeHeapOptimized() = fixture.serializerDecodeHeap()
    @Benchmark fun serializerDecodeDirectOptimized() = fixture.serializerDecodeDirect()
    @Benchmark fun redissonDecodeCopiedByteArray() = fixture.redissonDecodeCopied()
    @Benchmark fun redissonDecodeContiguousOptimized() = fixture.redissonDecodeContiguous()
    @Benchmark fun redissonDecodeCompositeCompatibility() = fixture.redissonDecodeComposite()
    @Benchmark fun trustedFallbackEncodeByteArray() = fixture.trustedEncodeByteArray()
    @Benchmark fun trustedFallbackEncodeBufferCompatibility() = fixture.trustedEncodeBuffer()
    @Benchmark fun trustedFallbackDecodeByteArray() = fixture.trustedDecodeByteArray()
    @Benchmark fun trustedFallbackDecodeBufferCompatibility() = fixture.trustedDecodeBuffer()
}
```

Import `org.openjdk.jmh.annotations.Level`, `org.openjdk.jmh.annotations.Param`, and `org.openjdk.jmh.annotations.Setup` explicitly. The JVM kotlinx-benchmark runtime aliases JMH annotations, but the explicit JMH imports make the `Level` and observed-parameter contracts unambiguous. The validator must read `matrixVersion`, `targetHeadroom`, and `targetStart` from every JMH result's `params` object and include them in its observed configuration fingerprint.

- [ ] **Step 6: Run fixture tests, compile, jar, and method-list smoke**

```bash
./gradlew :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkCompile \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache
JMH_JARS=$(find benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  -type f -name '*-JMH.jar' -print)
test "$(printf '%s\n' "$JMH_JARS" | sed '/^$/d' | wc -l | tr -d ' ')" = "1"
java -cp "$JMH_JARS" io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmarkMetadata --json
java -jar "$JMH_JARS" -l
```

Expected: tests and tasks PASS; list contains exactly the 13 methods in `ProtobufBenchmarkMatrix.expectedMethods`.

- [ ] **Step 7: Commit the deterministic harness**

```bash
git add benchmark/protobuf-codec-benchmark
git commit -m "Make protobuf allocation cells complete and state-stable" \
  -m "Constraint: Every claimed buffer cell needs an exact ByteArray baseline and repeated invocation safety
Rejected: Keep the existing throughput-only benchmark | it silently omitted a failing method
Confidence: high
Scope-risk: moderate
Directive: Treat composite and trusted fallback methods as claim-ineligible controls
Tested: Benchmark fixture tests, compile, jar, and exact method listing
Not-tested: Long GC-profiler evidence runs follow after validation tooling"
```

### Task 6: Add the fail-closed JMH validator and two-run verdict gate

**Files:**

- Create: `benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py`
- Create: `benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py`
- Create: `benchmark/protobuf-codec-benchmark/scripts/run-evidence.py`
- Create: `benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py`

- [ ] **Step 1: Write failing validator fixtures**

Cover complete success plus missing/unexpected method, wrong unit, NaN/infinity/negative/malformed score, duplicate run ID, identity mismatch, accepted/inconclusive/regressed/ineligible verdicts:

```python
class ValidateJmhTest(unittest.TestCase):
    def test_complete_two_run_matrix_is_accepted(self):
        comparison = validator.compare_runs(
            run("run-a", baseline=1000.0, candidate=940.0),
            run("run-b", baseline=1000.0, candidate=930.0),
        )
        self.assertEqual("accepted", comparison["serializerEncodeHeapOptimized"]["verdict"])

    def test_missing_and_unexpected_methods_fail(self):
        with self.assertRaisesRegex(ValueError, "missing=.*serializerEncodeByteArray"):
            validator.validate_methods([])
        with self.assertRaisesRegex(ValueError, "unexpected=.*driftedMethod"):
            validator.validate_methods(list(EXPECTED_METHODS) + ["driftedMethod"])

    def test_invalid_metrics_fail_closed(self):
        for score in (float("nan"), float("inf"), -1.0, "not-a-number"):
            with self.subTest(score=score):
                with self.assertRaises(ValueError):
                    validator.validate_score(score, "gc.alloc.rate.norm")

    def test_two_repeatable_five_percent_regressions_block(self):
        comparison = validator.compare_runs(
            run("run-a", baseline=1000.0, candidate=1060.0),
            run("run-b", baseline=1000.0, candidate=1050.0),
        )
        self.assertEqual("regressed", comparison["serializerEncodeHeapOptimized"]["verdict"])

    def test_identity_mismatch_prints_both_values(self):
        first = environment(run_id="run-a", tree_hash="aaa")
        second = environment(run_id="run-b", tree_hash="bbb")
        with self.assertRaisesRegex(ValueError, "tree_hash: aaa != bbb"):
            validator.validate_identity(first, second)
```

- [ ] **Step 2: Run Python tests and verify RED**

```bash
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
```

Expected: FAIL because `validate-jmh.py` does not exist.

- [ ] **Step 3: Implement the exact matrix and validation primitives**

`validate-jmh.py` must define these constants and verdict logic without importing another benchmark module:

```python
CLAIM_THRESHOLD_PERCENT = 5.0
EXPECTED_METHODS = {
    "serializerEncodeByteArray",
    "serializerEncodeHeapOptimized",
    "serializerEncodeDirectOptimized",
    "serializerDecodeByteArray",
    "serializerDecodeHeapOptimized",
    "serializerDecodeDirectOptimized",
    "redissonDecodeCopiedByteArray",
    "redissonDecodeContiguousOptimized",
    "redissonDecodeCompositeCompatibility",
    "trustedFallbackEncodeByteArray",
    "trustedFallbackEncodeBufferCompatibility",
    "trustedFallbackDecodeByteArray",
    "trustedFallbackDecodeBufferCompatibility",
}
BASELINES = {
    "serializerEncodeHeapOptimized": "serializerEncodeByteArray",
    "serializerEncodeDirectOptimized": "serializerEncodeByteArray",
    "serializerDecodeHeapOptimized": "serializerDecodeByteArray",
    "serializerDecodeDirectOptimized": "serializerDecodeByteArray",
    "redissonDecodeContiguousOptimized": "redissonDecodeCopiedByteArray",
}
IDENTITY_FIELDS = (
    "git_commit", "tree_hash", "os", "arch", "cpu", "jvm_vendor", "jvm_version",
    "gradle_version", "jmh_version", "jvm_args", "threads", "forks", "warmups",
    "measurements", "warmup_time", "measurement_time", "profiler", "payload_size",
    "payload_sha256", "config_sha256", "metadata_stdout_sha256", "benchmark_jar_sha256", "clean_status",
    "power_state", "concurrent_heavy_work",
)

CANONICAL_PROFILE = {
    "mode": "thrpt",
    "threads": 1,
    "forks": 2,
    "warmups": 3,
    "measurements": 5,
    "warmup_time": "1 s",
    "measurement_time": "1 s",
    "profiler": "gc",
    "exact_jvm_args": ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"],
}

def verdict(deltas, eligible):
    if not eligible:
        return "ineligible"
    if all(delta <= -CLAIM_THRESHOLD_PERCENT for delta in deltas):
        return "accepted"
    if all(delta >= CLAIM_THRESHOLD_PERCENT for delta in deltas):
        return "regressed"
    return "inconclusive"
```

Implement JSON loading, exact method-set comparison, `mode == "thrpt"`, primary `ops/s`, `gc.alloc.rate.norm == B/op`, finite non-negative numeric scores, unique run IDs, complete baseline pairs, exact `IDENTITY_FIELDS` equality, CSV output, diagnostic errors, and `validation.json` output. `run` must extract every observable JMH setting (`threads`, `forks`, warmup/measurement iteration counts and durations, JDK/VM identity, and the full fork `jvmArgs`) from every one of the 13 records, require internal consistency, compare those observed values to the manifest, and persist an observed-config SHA-256 in summary/validation. `compare` must match that full observed fingerprint across runs and enforce `CANONICAL_PROFILE`. The observed fork `jvmArgs` list must equal the exact ordered three-item list `-Xms1g`, `-Xmx1g`, `-XX:+UseG1GC`; launcher/framework command tokens live in `argv.json` and are not fork JVM args. Extra, missing, duplicate, or reordered fork args fail fixtures. A manifest cannot override a contradictory JMH record. Use Python standard library modules `argparse`, `csv`, `hashlib`, `json`, `math`, and `pathlib`.

`config_sha256` is computed from exactly these keys: `allowed_class_prefixes`, `direct_capacity`, `direct_initial_position`, `heap_capacity`, `heap_initial_position`, `matrix_version`, `methods`, `payload_identity`, `payload_sha256`, `redisson_codec_class`, `serializer_class`, `target_headroom`, and `target_start`. Lists are sorted, integer values remain JSON numbers, and the Python side serializes with `json.dumps(config, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")`. The Kotlin `canonicalConfigJson` above emits the same lexicographically ordered, whitespace-free UTF-8 bytes using actual fixture capacities/positions. Fixture tests assert the complete canonical JSON string and cross-check its SHA-256 against a fixed Python-produced fixture. Add validator fixtures for a change to every config field, manifest-vs-JMH threads/forks/iterations/durations/JVM args mismatches, `config_sha256` mismatch, JAR hash mismatch, and non-clean status.

CLI contract:

```text
validate-jmh.py run --jar /absolute/path/to/benchmark-JMH.jar --input jmh.json --environment environment.json --summary summary.csv --validation validation.json
validate-jmh.py compare --run run-a/summary.csv --run run-b/summary.csv --environment run-a/environment.json --environment run-b/environment.json --output comparison.csv --validation validation.json [--rollback-bundle rollback-bundle.json]
```

For `run`, resolve `--jar` to an absolute regular file, recompute its SHA-256, and compare it with `benchmark_jar_path` plus `benchmark_jar_sha256` in `environment.json`. Hash once before parsing JMH and once immediately before writing `validation.json`; fail if the path, hash, or file identity changed. This makes JAR provenance observed input rather than a self-asserted manifest field.

Implement `run-evidence.py` with Python standard library only. Its executable contract is:

```text
run-evidence.py resolve-jar --jar-dir build/benchmarks/benchmark/jars --state build/issue-757-evidence/jar.json [--rollback-bundle /absolute/rollback-bundle.json]
run-evidence.py run --state build/issue-757-evidence/jar.json --profile smoke|canonical --output-root build/issue-757-evidence [--run-id RUN_ID]
run-evidence.py compare --state build/issue-757-evidence/jar.json --output build/issue-757-evidence/comparison.csv --validation build/issue-757-evidence/validation.json
run-evidence.py record-rollback --state build/issue-757-evidence/jar.json --dispatch serializer_encode|serializer_decode|redisson_contiguous [--dispatch ...] --archive-root build/issue-757-rollback
run-evidence.py finalize-rollback --preparation build/issue-757-rollback/rollback-preparation-gN-<sha256>.json
run-evidence.py promote --state build/issue-757-evidence/jar.json --destination docs/benchmarks/raw/issue-757
run-evidence.py replace-promoted --state build/issue-757-evidence/jar.json --expected-manifest docs/benchmarks/raw/issue-757/delivery-manifest.json --destination docs/benchmarks/raw/issue-757 --backup-root .omx/tmp/issue-757/evidence-backups
run-evidence.py cleanup-replacement-backup --state build/issue-757-evidence/jar.json --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json --expected-head HEAD --backup-root .omx/tmp/issue-757/evidence-backups
run-evidence.py verify-promoted --state build/issue-757-evidence/jar.json --destination docs/benchmarks/raw/issue-757
run-evidence.py validate-committed --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
run-evidence.py render-report --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json --output docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
run-evidence.py validate-report --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
```

`resolve-jar` must find exactly one `*-JMH.jar`, canonicalize its absolute path, hash it, and atomically write a fail-if-exists state file. `run` must:

1. require an empty porcelain status including untracked files;
2. generate `run-YYYYMMDDTHHMMSS.ffffffZ-<8 random hex>` when `--run-id` is absent and reject an existing run directory;
3. create the staging directory before passing `-rff`;
4. execute `java -cp <pinned.jar> io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmarkMetadata --json`, require one valid JSON object, hash the exact stdout bytes, and atomically persist `metadata.json` plus `metadata_stdout_sha256`;
5. record git commit/tree, both clean-status observations, `uname`, architecture, CPU, Java/Gradle/JMH identity, full JMH argv, the observed metadata payload/config fields, power-source state, and an explicit operator-supplied `concurrent_heavy_work` declaration in a schema-versioned `environment.json` written through a sibling temporary file plus atomic rename;
6. after all environment capture and immediately before `Popen`, repeat the untracked-inclusive porcelain gate, persist its exact output/hash separately from the initial gate, rerun the pinned metadata entrypoint and require byte-identical stdout/hash, then recompute the pinned JAR hash;
7. persist `argv.json`, `started_at`, `ended_at`, stdout/stderr in `run.log`, and `exit_code` even on failure;
8. call `validate-jmh.py run --jar <pinned absolute path>` only after JMH exits 0; validator cross-checks metadata matrix version/headroom/start against all JMH params and environment, rejects any self-asserted mismatch, then the runner prints the exact recorded argv and artifact directory.

After each successful canonical validation, `run` atomically appends `{run_id, absolute_path, environment_sha256, summary_sha256, validation_sha256}` to the state file; it rejects a duplicate ID/path and more than two canonical runs. `compare` requires exactly two recorded canonical runs, passes their paths to the validator, and records `comparison_path`, `comparison_sha256`, `comparison_validation_path`, and `comparison_validation_sha256` in state. `promote` consumes only those state-bound paths/hashes and includes both comparison artifacts under the no-clobber atomic directory. `verify-promoted` checks every byte hash against state before semantic validation, re-runs both per-run validations and the comparison into separate scratch outputs, and compares their semantic JSON/CSV contents without overwriting the promoted originals. Only after all checks succeed, it atomically writes `promotion_status: "verified"` to ignored state and atomically emits `docs/benchmarks/raw/issue-757/delivery-manifest.json` with schema version, only canonical repo-relative promoted paths/hashes, measurement commit/tree, final verdicts/reasons, rollback archive/decision hashes, and report-input identity; absolute build/JAR paths are forbidden. Failure leaves status/manifest absent or non-verified. Repeating verification is idempotent only for the identical destination and hashes; a different/tampered destination fails. `validate-committed` reconstructs committed raw-result, environment, comparison, validation, bundle, and report-input semantics in a clean checkout without `build/` state; it verifies the recorded pinned-JAR hash chain established by `verify-promoted` but cannot reopen uncommitted JAR bytes. Fixtures prove clean-checkout reconstruction, success transition, failure non-transition, identical rerun, stale/different destination rejection, and missing/tampered/absolute-path manifest rejection. No delivery command depends on ignored state.

`record-rollback`은 코드 변경 전에 clean measurement HEAD/tree에서 실행한다. state-bound comparison에서 mapped cell 하나라도 `regressed`이면 해당 dispatch가 trigger되며, 모든 동시 trigger를 repeatable `--dispatch`로 정확히 전달해야 한다. 명령은 두 run과 comparison/validation 전체를 immutable archive에 복사하고 `rollback-preparation-g<generation>-<sha256>.json`을 no-clobber로 만든 뒤 state를 `prepared`, non-promotable로 전환한다. 각 decision의 `regressed_cells`는 실제 `regressed` subset이고 non-empty이며, `removed_cells`는 dispatch 전체 고정 mapping이다.

그 다음 symbol-scoped rollback을 적용해 commit하고 `finalize-rollback --preparation ...`을 실행한다. finalize는 clean descendant commit/tree와 canonical source-removal predicate, preparation/archive/predecessor lineage를 인증한 뒤에만 immutable `rollback-bundle-g<generation>-<sha256>.json`을 만든다. preparation은 bundle이 아니며 schema v1 bundle도 사용할 수 없다. rebase나 amend로 old/post commit lineage가 바뀌면 기존 preparation/finalization은 무효이므로 원래 clean measurement head에서 다시 준비해야 한다.

이후 fresh `resolve-jar --rollback-bundle <absolute finalized bundle path>`만 허용한다. `compare`, promotion, manifest, committed reconstruction은 모든 generation의 preparation, bundle, archive를 함께 인증한다. 최종 compare에서 각 decision의 전체 `removed_cells`는 `ineligible`/`removed_after_regression`이고, 실제 trigger subset인 `regressed_cells`와 혼동하지 않는다. retained cell의 새 regression은 promotion을 차단하고 다음 generation을 같은 prepare → rollback commit → finalize 순서로 시작한다.

`render-report` is the only writer for the allocation report. It consumes the verified committed delivery manifest and deterministically emits scope, separate measurement/delivery provenance, exact recorded commands, every per-run B/op/ops/s value, deltas, all verdicts/reasons, rollback decisions, compatibility controls, and limitations. Positive reduction language is emitted only for retained `accepted` cells; inconclusive, regressed, removed, baseline, and compatibility cells use non-positive fixed wording. `validate-report` regenerates into scratch and requires byte equality with the tracked report. Fixtures mutate every numeric/verdict/head field and inject forbidden positive language for each non-accepted status; all fail.

Power-state capture uses `pmset -g batt` on macOS when available and otherwise records `unknown` plus the failed command. `concurrent_heavy_work` is required as `absent` for canonical runs; the runner cannot infer it. All subprocess output used as identity is captured verbatim in `environment.json`, while normalized fields remain separately comparable. Runner fixtures include tampered/self-asserted metadata, changed metadata between capture/launch, metadata-vs-JMH params, and both initial/pre-launch dirty states.

Every `ValueError` must include the input path, both mismatch values when applicable, and a remediation hint. Exit non-zero by allowing the exception to reach `main` after printing one concise diagnostic to stderr.

Without a rollback bundle, all five `BASELINES` candidates are claim-eligible and a final `regressed` verdict blocks delivery. With an authenticated imported bundle, only candidates mapped to its removed dispatches become `ineligible` with exact reason `removed_after_regression`; all other candidates remain eligible. Validator fixtures reject a decision for a retained unit, a decision whose archived comparison was not regressed, an altered bundle/decision/archive hash, duplicate or conflicting dispatch decisions, incomplete chained lineage, and any attempt to mark an unrelated cell ineligible.

- [ ] **Step 4: Run all validator and evidence-runner fixtures GREEN**

```bash
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
```

Expected: all tests PASS, including `regressed`, invalid metric, manifest/JAR mismatch, JAR replacement, multiple-JAR, dirty-tree, duplicate-run-ID, failed-process logging, promotion collision, old-manifest drift, failure between the two replacement renames with exact restore, successful replacement, cleanup refusal, exact single-backup cleanup, and clean-checkout replacement validation fixtures.

- [ ] **Step 5: Commit the unit-validated fail-closed gate**

```bash
git add benchmark/protobuf-codec-benchmark/scripts
git commit -m "Fail closed when protobuf benchmark evidence is incomplete" \
  -m "Constraint: A zero JMH process exit cannot prove that every expected method produced metrics
Rejected: Reuse throughput output as allocation evidence | it lacks normalized allocation and completeness identity
Confidence: high
Scope-risk: narrow
Directive: Missing or invalid metrics are gate failures, never inconclusive results
Tested: Validator and evidence-runner unit fixtures
Not-tested: JMH smoke and two long fresh runs follow from a clean committed head"
```

- [ ] **Step 6: Exercise the validator against a short clean-head JMH smoke result**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --jar-dir benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-smoke/jar.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-smoke/jar.json \
  --profile smoke \
  --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-smoke
```

The smoke profile is fixed at `-t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc -rf json`; the validator's `run` mode validates schema, provenance, and metrics, while final `compare` mode enforces the canonical evidence profile.

Expected: all 13 methods present and validation status `passed`.
If the smoke fails, repair the scripts/tests, repeat Step 4, commit the repair with Lore trailers, and rerun the smoke from the new clean head. Do not bypass the runner clean-tree gate.
The smoke root/state is never reused for canonical evidence. Task 8 creates the distinct `build/issue-757-evidence/jar.json` only after its fresh clean/JAR build.

### Task 7: Document the public contract and prepare a clean implementation head

**Files:**

- Modify: `io/protobuf/README.md`
- Modify: `io/protobuf/README.ko.md`
- Modify: `benchmark/protobuf-codec-benchmark/README.md`
- Modify: `benchmark/protobuf-codec-benchmark/README.ko.md`
- Modify: `docs/benchmarks/README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update English/Korean Protobuf README sections in parity**

Both locale files must contain equivalent examples and boundaries:

```kotlin
val target = ByteBuffer.allocateDirect(4096).apply { position(8) }
val written = packMessageTo(message, target)
val source = target.duplicate().apply {
    position(8)
    limit(8 + written)
}
val decoded = unpackMessage<MyMessage>(source)
```

Also include the internal/test-only exact-capacity calculation without adding a new public size API:

```kotlin
val packed = com.google.protobuf.Any.pack(message)
val exactTarget = ByteBuffer.allocate(packed.serializedSize)
packMessageTo(message, exactTarget)
```

Explain that production callers should normally reuse an oversized buffer because exact sizing constructs the packed `Any` and defeats part of the intended allocation benefit. State that existing `ByteArray` callers do not need to migrate, reusable oversized caller buffers are the intended use case, strict is the default, composite Redisson input remains a copied compatibility path, and a failed partial write restores only `position`; contents may already be overwritten and callers must `clear()` and reinitialize or discard the buffer before reuse. Show that recovery explicitly in a `try/catch` example.

Use this executable recovery shape in both locales; `HEADER_SIZE` is the caller's own initialized prefix boundary:

```kotlin
try {
    serializer.serializeTo(message, target)
} catch (failure: Throwable) {
    target.clear()
    target.position(HEADER_SIZE)
    // Rewrite every caller-owned prefix byte before reuse, or discard target.
    throw failure
}
```

Document both trusted opt-in surfaces exactly: `ProtobufSerializer()` is the zero-argument strict default, while `ProtobufSerializer(fallback = nonNullSerializer)` and `ProtobufSerializer.trustedInternalProtobuf(...)` are trusted-store-only compatibility profiles. Neither is suitable for untrusted payloads, and neither changes the terminal allowlist/non-`Message` boundary.

Add one paired serializer example that reuses an oversized caller buffer through `ProtobufSerializer.serializeTo(message, target)` and decodes the bounded duplicate through `deserializeFrom<MyMessage>(source)`, while retaining the existing ByteArray example unchanged.

Replace the existing incorrect English/Korean claim that `RedissonProtobufCodec` uses Kryo5 fallback by default. Document `RedissonProtobufCodec()` and the allowlist-only constructor as strict, `RedissonProtobufCodec(fallbackCodec)` and `RedissonProtobufCodec.trustedInternal(...)` as trusted-store-only compatibility opt-ins, and `ALLOW_ALL_CLASSES_UNSAFE` as an allowlist migration escape hatch that does not activate fallback. The two locale files must use equivalent wording and examples.

- [ ] **Step 2: Replace throughput-only benchmark README claims with the exact allocation protocol**

Document the 13-cell method matrix, claim eligibility, short smoke command, canonical command, validator commands, two-run rule, ±5% verdicts, evidence paths, and no zero-copy/throughput guarantee. The canonical JMH arguments are exactly:

```text
-t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json
-jvmArgsAppend "-Xms1g -Xmx1g -XX:+UseG1GC"
```

- [ ] **Step 3: Update benchmark index and changelog without numeric claims**

Add a pending issue #757 report link to `docs/benchmarks/README.md`. Under the existing `1.12.0` section in `CHANGELOG.md`, describe the caller-owned APIs and contiguous Redisson lower-copy path, but do not state an allocation reduction until Task 8 accepts it.

- [ ] **Step 4: Verify locale parity, scope, and repository checks**

```bash
rg -n "ByteBuffer|serializeTo|deserializeFrom|strict|trustedInternal|RedissonProtobufCodec\(\)|ALLOW_ALL_CLASSES_UNSAFE|5%|gc.alloc.rate.norm|composite" \
  io/protobuf/README.md io/protobuf/README.ko.md \
  benchmark/protobuf-codec-benchmark/README.md benchmark/protobuf-codec-benchmark/README.ko.md
for file in io/protobuf/README.md io/protobuf/README.ko.md; do
  for token in 'serializeTo(message, target)' 'deserializeFrom<MyMessage>(source)' \
    'RedissonProtobufCodec()' 'trustedInternal' 'ALLOW_ALL_CLASSES_UNSAFE' \
    'target.clear()' 'HEADER_SIZE'; do
    rg -F "$token" "$file"
  done
done
test -z "$(rg -n 'uses Kryo5 as the fallback for non-Protobuf|fallback에는 Kryo5를 사용' \
  io/protobuf/README.md io/protobuf/README.ko.md || true)"
for file in benchmark/protobuf-codec-benchmark/README.md benchmark/protobuf-codec-benchmark/README.ko.md; do
  for token in 'gc.alloc.rate.norm' 'serializerEncodeHeapOptimized' \
    'redissonDecodeCompositeCompatibility' 'trustedFallbackDecodeBufferCompatibility' \
    '-Xms1g -Xmx1g -XX:+UseG1GC'; do
    rg -F -- "$token" "$file"
  done
done
git diff --check
./gradlew :bluetape4k-protobuf:test \
  :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkCompile \
  --no-configuration-cache
```

Expected: locale pairs contain the same contract topics; all targeted tests/compile pass; no whitespace errors.

- [ ] **Step 5: Commit documentation and confirm the measurement head is clean**

```bash
git add \
  io/protobuf/README.md io/protobuf/README.ko.md \
  benchmark/protobuf-codec-benchmark/README.md benchmark/protobuf-codec-benchmark/README.ko.md \
  docs/benchmarks/README.md CHANGELOG.md
git commit -m "Explain when protobuf callers should reuse buffer paths" \
  -m "Constraint: Allocation claims require measured exact-head evidence and cannot be inferred from API shape
Rejected: Publish the old throughput table as current guidance | it omitted a failing benchmark method
Confidence: high
Scope-risk: narrow
Directive: Keep English and Korean buffer, trust, and evidence guidance in parity
Tested: Locale contract search, protobuf tests, benchmark tests and compile, git diff check
Not-tested: Final allocation numbers are intentionally deferred to fresh runs"
repo-status
```

Expected: clean tree; branch is ahead of `origin/develop`; record `git rev-parse HEAD` as the measurement commit.

### Task 8: Collect two clean exact-head allocation runs and publish only supported conclusions

**Files:**

- Create: `docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md`
- Create: `docs/benchmarks/raw/issue-757/run-<UTC>/environment.json`
- Create: `docs/benchmarks/raw/issue-757/run-<UTC>/argv.json`
- Create: `docs/benchmarks/raw/issue-757/run-<UTC>/run.log`
- Create: `docs/benchmarks/raw/issue-757/run-<UTC>/jmh.json`
- Create: `docs/benchmarks/raw/issue-757/run-<UTC>/summary.csv`
- Create: `docs/benchmarks/raw/issue-757/run-<UTC>/validation.json`
- Create: `docs/benchmarks/raw/issue-757/comparison.csv`
- Modify conditionally: production dispatch files and `CHANGELOG.md` only if the symmetric regression rule requires rollback.

- [ ] **Step 1: Prove the measurement source is clean and capture immutable identity**

```bash
repo-status
git status --porcelain=v1 --untracked-files=all > /tmp/issue-757-clean-status.txt
test ! -s /tmp/issue-757-clean-status.txt
git rev-parse HEAD
git rev-parse 'HEAD^{tree}'
java -version
./gradlew --version
sysctl -n machdep.cpu.brand_string
uname -a
```

Expected: clean tree including untracked files. Stop if dirty; do not measure uncommitted implementation. The runner repeats this gate immediately before each launch and writes these command results into the manifest, so this shell inspection is orientation rather than the source of truth.

- [ ] **Step 2: Build one JMH jar before both runs**

```bash
./gradlew :protobuf-codec-benchmark:clean \
  :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache
```

Expected: PASS and exactly one runnable JMH jar under `benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars/`.

Resolve and pin the artifact once in an ignored, fail-if-exists state file:

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --jar-dir benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json
```

The state file contains the one absolute canonical path, SHA-256, and file identity. Do not rebuild or replace the jar between runs. Both the runner and validator recompute the hash; a stale, fabricated, multiple, or replaced artifact fails closed.

- [ ] **Step 3: Execute run 1 and run 2 sequentially in ignored build staging**

Run the canonical profile twice. The runner generates distinct collision-safe UTC IDs, creates each directory before `-rff`, captures environment/argv/log/exit status, invokes validation with the pinned `--jar`, and prints the resulting directory:

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-evidence
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-evidence
```

Expected: both validations PASS; run IDs differ; all 13 methods and required metrics are present; each directory contains `environment.json`, `argv.json`, `run.log`, `jmh.json`, `summary.csv`, and `validation.json`; the recorded exit code is 0; the tree remains clean because staging is ignored.

- [ ] **Step 4: Compare the two validated runs**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py compare \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --output benchmark/protobuf-codec-benchmark/build/issue-757-evidence/comparison.csv \
  --validation benchmark/protobuf-codec-benchmark/build/issue-757-evidence/validation.json
```

Expected: each eligible cell is exactly `accepted`, `inconclusive`, or `regressed`; compatibility/fallback cells are `ineligible`.

- [ ] **Step 5: Apply the symmetric go/no-go rule before writing claims**

- `accepted`: both runs are at most `-5%`; retain that dispatch and permit a narrowly worded reduction claim.
- `inconclusive`: mixed direction or absolute delta under `5%`; retain functionally correct dispatch but write no reduction claim.
- `regressed`: 두 run이 모두 `+5%` 이상이면 코드 변경 전에 모든 동시 trigger를 한 번의 `record-rollback`으로 prepare하고, rollback commit 후 `finalize-rollback`으로 bundle을 확정한 다음 finalized bundle을 import해 Task 8을 fresh state에서 다시 시작한다.
- `ineligible`: preserve compatibility data but never use it to justify a positive claim.

Rollback is dependency-ordered and symbol-scoped; do not use whole-commit `git revert` when a commit mixes retained public APIs with optimized dispatch:

| Regression cell | Remove/revert | Required tests | Documentation/evidence repair |
|---|---|---|---|
| serializer encode heap/direct | `ProtobufSerializer.serializeTo` ProtoMessage direct branch in `ProtobufSerializer.kt`; retain public `MessageSupport` APIs | `ProtobufSerializerByteBufferTest`, `ProtobufSerializerSecurityTest`, full protobuf suite | remove serializer encode reduction wording from both locale READMEs, benchmark README/report/index, and CHANGELOG; keep compatibility-only wording |
| serializer decode heap/direct | `ProtobufSerializer.deserializeFrom` direct parse branch; restore copied SPI compatibility dispatch | same serializer/security/full suite | remove serializer decode reduction wording from the same docs/KDoc surfaces |
| Redisson contiguous decode | contiguous `nioBuffer` branch in `RedissonProtobufCodec.Decoder`; retain isolated copied composite/fallback path | `RedissonProtobufCodecTest`, serializer/security/full suite | remove contiguous Redisson reduction wording from both locale READMEs, codec KDoc, benchmark report/index, and CHANGELOG |

Rollback 순서는 고정이다: (1) pre-change clean head에서 `record-rollback` preparation 생성, (2) staging을 obsolete 경로로 분리, (3) symbol-scoped code/docs rollback commit, (4) affected/전체 검증, (5) 같은 preparation으로 `finalize-rollback`, (6) finalized bundle을 import한 fresh JAR/state에서 새로운 run ID 두 개 수집. old metrics는 `regressed_cells` trigger 근거로만 사용하며 final performance evidence로 재사용하지 않는다. 전체 `removed_cells`는 최종 compare에서 `ineligible`/`removed_after_regression`이고 retained regression은 다음 generation을 시작하기 전에 promotion을 차단한다.

- [ ] **Step 6: Promote validated artifacts atomically only after both runs finish**

Run:

```bash
test ! -e docs/benchmarks/raw/issue-757
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py promote \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --destination docs/benchmarks/raw/issue-757
```

`promote` must fail if the destination, either destination run directory, or destination comparison file exists. It copies into a unique sibling temporary directory, checks the exact required file set and SHA-256 of every staged/promoted file, then atomically renames the complete temporary directory to `issue-757`; any failure removes only that newly created temporary directory. After promotion, rerun each per-run `validate-jmh.py run --jar <pinned path>` against the docs files and rerun `compare` from docs.

Do not hand-write numeric or verdict content. Task 8 Step 7 generates the report from the verified committed manifest; never transcribe commands manually and never copy the old 2026-06-19 throughput numbers as evidence.

- [ ] **Step 7: Validate evidence links and commit the measured result**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-promoted \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  --destination docs/benchmarks/raw/issue-757
python3 -c 'import json,pathlib,sys; d=json.load(open(sys.argv[1])); assert d["promotion_status"]=="verified"; assert pathlib.Path(d["promoted_destination"]).resolve()==pathlib.Path(sys.argv[2]).resolve(); assert d["verified_measurement_commit"] and d["verified_measurement_tree"]' \
  benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
  docs/benchmarks/raw/issue-757
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py render-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --output docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
rg -n "issue-757|comparison.csv|jmh.json|environment.json" \
  docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md \
  docs/benchmarks/README.md
git diff --check
git add docs/benchmarks CHANGELOG.md
git commit -m "Record exact-head protobuf buffer allocation evidence" \
  -m "Constraint: Positive claims require two independent five-percent B/op improvements
Rejected: Generalize from throughput or one run | neither proves normalized allocation reduction
Confidence: high
Scope-risk: narrow
Directive: Re-run both evidence passes after any production-dispatch change
Tested: Two validated GC-profiler runs, identity comparison, evidence links, git diff check
Not-tested: Results do not generalize beyond the recorded payload, machine, JDK, and buffer cells"
```

If this was a `replace-promoted` lifecycle and state contains `replacement_backup`, clean only that backup after the evidence commit is proved:

```bash
if python3 -c 'import json,sys; raise SystemExit(0 if json.load(open(sys.argv[1])).get("replacement_backup") else 1)' \
  benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json; then
  mkdir -p .omx/tmp/issue-757
  git show HEAD:docs/benchmarks/raw/issue-757/delivery-manifest.json \
    > .omx/tmp/issue-757/committed-delivery-manifest.json
  cmp .omx/tmp/issue-757/committed-delivery-manifest.json \
    docs/benchmarks/raw/issue-757/delivery-manifest.json
  python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
    --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
  python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py cleanup-replacement-backup \
    --state benchmark/protobuf-codec-benchmark/build/issue-757-evidence/jar.json \
    --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
    --expected-head HEAD --backup-root .omx/tmp/issue-757/evidence-backups
fi
```

### Task 9: Run final verification, independent review, and exact-head PR delivery

**Files:**

- Create: `docs/review/issue-757-protobuf-buffer-core-review.md`
- Modify: issue #757 DoD and PR metadata only after local verification passes.

- [ ] **Step 1: Run final local verification sequentially**

Create `.omx/tmp/issue-757/check_scope.py` with `apply_patch`. The ignored scratch checker must compare `git diff --name-only origin/develop...HEAD` against this exact allowlist plus the two dynamic prefixes `docs/benchmarks/raw/issue-757/` and `docs/review/issue-757-protobuf-buffer-core-review.md`:

```text
CHANGELOG.md
benchmark/protobuf-codec-benchmark/README.ko.md
benchmark/protobuf-codec-benchmark/README.md
benchmark/protobuf-codec-benchmark/build.gradle.kts
benchmark/protobuf-codec-benchmark/scripts/run-evidence.py
benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py
benchmark/protobuf-codec-benchmark/src/benchmark/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmark.kt
benchmark/protobuf-codec-benchmark/src/benchmark/proto/protobuf/benchmark-message.proto
benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkMetadata.kt
benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupport.kt
benchmark/protobuf-codec-benchmark/src/main/proto/protobuf/benchmark-message.proto
benchmark/protobuf-codec-benchmark/src/test/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupportTest.kt
docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
docs/benchmarks/README.md
docs/superpowers/plans/2026-07-18-issue-757-protobuf-buffer-core.md
docs/superpowers/specs/2026-07-18-issue-757-protobuf-buffer-core-design.md
io/protobuf/README.ko.md
io/protobuf/README.md
io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt
io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufMessageClassResolver.kt
io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializer.kt
io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodec.kt
io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/MessageSupportByteBufferTest.kt
io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerByteBufferTest.kt
io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/ProtobufSerializerSecurityTest.kt
io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/RedissonProtobufCodecTest.kt
```

The checker must also parse zero-context added lines from every changed `*.gradle.kts` and fail on any dependency/plugin declaration outside this exact approved set: `implementation(project(":bluetape4k-protobuf"))`, `implementation(project(":bluetape4k-redisson"))`, `testImplementation(project(":bluetape4k-junit5"))`, `add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)`, `add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)`, `add("benchmarkImplementation", libs.jmh.core)`, and `add("benchmarkRuntimeOnly", libs.logback.classic)`. It must print unexpected paths/lines and exit non-zero; added module directories, coordinates, aliases, plugins, settings, release, or publishing files are never implicitly allowed.

```bash
repo-status
./gradlew :bluetape4k-protobuf:test --no-configuration-cache
./gradlew :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkCompile \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py \
  benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
./gradlew :bluetape4k-protobuf:detekt --no-configuration-cache
./gradlew :bluetape4k-protobuf:build --no-configuration-cache
git diff --check
test -z "$(git status --porcelain=v1 --untracked-files=all)"
git diff --name-status origin/develop...HEAD
python3 .omx/tmp/issue-757/check_scope.py origin/develop HEAD
```

Expected: every command exits 0, the post-test tree is clean including untracked files, the complete `origin/develop...HEAD` name/status list is reviewed, and no module registration, dependency catalog/wrapper, release, tag, publishing, or workflow surface entered the diff. If Detekt task naming differs, discover with `./gradlew :bluetape4k-protobuf:tasks --all` and record the actual equivalent.

- [ ] **Step 2: Run independent six-lens review and repair until P0=0/P1=0**

Review exact `origin/develop...HEAD` independently for Performance, Stability/resource, Security, Operator/Ops, Developer/API, and User/caller lenses. Record evidence, fixes, explicit P2/P3 dispositions, test gaps, and final counts in `docs/review/issue-757-protobuf-buffer-core-review.md`. Re-run only affected lenses after repairs, then run the main integration pass.

The review artifact records the reviewed implementation commit/tree, all six P0/P1 counts, and the commands that produced them. Apply the Guardrails invalidation matrix to any code, test, benchmark, metadata, validator, runner, report, evidence, README, or CHANGELOG repair; rerun fresh evidence or committed-evidence regeneration plus every affected lens before continuing.

When repair requires fresh evidence and `docs/benchmarks/raw/issue-757` already exists from an earlier promotion, build and verify the fresh state first, then use `replace-promoted` instead of initial `promote`; immediately rerun `verify-promoted`, `validate-committed`, report generation/validation, and every affected lens.

- [ ] **Step 3: Commit review evidence if the review created or changed files**

```bash
git add docs/review/issue-757-protobuf-buffer-core-review.md
git commit -m "Record converged review evidence for protobuf buffer delivery" \
  -m "Constraint: The Type A delivery gate requires independent security, performance, stability, operations, API, and caller review
Confidence: high
Scope-risk: narrow
Directive: Re-open affected lenses after any post-review code change
Tested: Six-lens P0=0 and P1=0 convergence plus main integration review
Not-tested: Remote CI and review threads remain external gates"
```

Repeat the complete Step 1 post-test clean gate and scope-diff assertions after this commit. The only permitted new file relative to the previously reviewed implementation diff is `docs/review/issue-757-protobuf-buffer-core-review.md`; if anything else changed, reopen the affected review lens.

- [ ] **Step 4: Snapshot the live issue, gate branch drift, and prepare an idempotent PR**

```bash
git fetch origin --prune
git rev-parse HEAD
git rev-parse origin/develop
git merge-base --is-ancestor origin/develop HEAD
repo-status
mkdir -p .omx/tmp/issue-757
gh issue view 757 \
  --repo bluetape4k/bluetape4k-projects \
  --json number,title,body,milestone,labels,assignees,state,url \
  > .omx/tmp/issue-757/issue-before.json
test "$(python3 -c 'import json; d=json.load(open(".omx/tmp/issue-757/issue-before.json")); print(d["number"])')" = "757"
PR_NUMBER=$(gh pr list \
  --repo bluetape4k/bluetape4k-projects \
  --state open --base develop --head feat/issue-757-protobuf-buffer-core \
  --json number --jq 'if length == 0 then "" elif length == 1 then .[0].number else error("multiple open PRs for head") end')
```

If `origin/develop` is not an ancestor, stop delivery, rebase the feature branch onto the fetched head, resolve conflicts, and rerun Task 8 plus all of Task 9 from the new clean exact head. A base change invalidates every pre-rebase rollback bundle: create an isolated worktree from the rebased feature head, temporarily restore only each removed dispatch's exact symbol set there while retaining the rebased benchmark/evidence tooling, build a fresh pinned JAR, collect two fresh canonical runs, and record a new authenticated regression decision; then return to the rebased delivery worktree, keep those symbols removed, import the newly proved bundle, and collect the final two runs. Old-base bundle hashes or metrics cannot cross the rebase boundary. Do not push stale-base evidence.

Create `.omx/tmp/issue-757/delivery_common.py` and `.omx/tmp/issue-757/test_delivery_common.py` with `apply_patch`. The shared `load_delivery_contract(delivery_manifest, comparison, repo_root)` must:

1. load the committed manifest and its two relative environment paths, reject any absolute/escaping path, and require one identical `git_commit` (`measurement_head`) and `tree_hash`;
2. canonicalize and hash the supplied promoted comparison CSV plus its paired comparison-validation JSON; require their relative paths/hashes to equal the manifest and require the committed manifest's verified measurement identity;
3. require all five eligible and five ineligible verdicts;
4. define immutable `DISPATCH_LABELS = {"serializer_encode": "serializer encode buffer dispatch", "serializer_decode": "serializer decode buffer dispatch", "redisson_contiguous": "contiguous Redisson decode dispatch"}` and derive retained source state from the exact encode, decode, and Redisson symbols used below;
5. apply `{"serializer_encode": (heap encode, direct encode), "serializer_decode": (heap decode, direct decode), "redisson_contiguous": (Redisson contiguous)}` against manifest-bound rollback bundle generations, decision hashes, and archive hashes: retained units require no rollback decision and only final `accepted`/`inconclusive` mapped verdicts; removed units require an authentic imported pre-rollback decision naming a mapped `regressed` cell and require all mapped final cells to be `ineligible` with reason `removed_after_regression`; any final `regressed`, decision+retained, no-decision+removed, duplicate/conflicting decision, incomplete bundle lineage, or accepted/inconclusive+removed combination fails;
6. return measurement head/tree, all ten final verdicts, the three retained/removed states, and the exact archived regression cell set/reason per removed unit from one immutable result object.

Both delivery generators must import this function; neither may duplicate or weaken the mapping. Render “removed after repeatable allocation regression” only when the returned reason names the archived mapped regressed cells. Unit fixtures cover final-regressed delivery rejection, decision+retained, accepted+removed, inconclusive+removed, mixed sibling verdicts, valid decision+removed+final-ineligible, missing/altered decision archives, mismatched measurement commits/trees, missing cells, an ineligible cell with a wrong reason/verdict, tampered comparison CSV, tampered comparison validation, wrong promoted path, and unverified promotion state.

Create `.omx/tmp/issue-757/prepare-pr-body.py` with `apply_patch`. It uses the shared contract, renders all ten cells/verdicts plus each final dispatch state, and writes `.omx/tmp/issue-757/pr-body.md` with separate measurement and delivery heads. It must use non-closing `Part of #757` (never `Closes/Fixes/Resolves #757`), separate completed core/Redisson/evidence work from deferred Lettuce work, list validation commands, state that no zero-copy or throughput guarantee is made, and end with the final section `## DoD Status`. Run its fixture tests and validate before any remote mutation:

```python
import argparse
import pathlib

from delivery_common import DISPATCH_LABELS, ELIGIBLE, INELIGIBLE, load_delivery_contract

parser = argparse.ArgumentParser()
parser.add_argument("--comparison", required=True)
parser.add_argument("--delivery-head", required=True)
parser.add_argument("--delivery-manifest", required=True)
parser.add_argument("--output", required=True)
parser.add_argument("--repo-root", required=True)
args = parser.parse_args()
contract = load_delivery_contract(args.delivery_manifest, args.comparison, args.repo_root)
verdicts = contract.verdicts
dispatch = contract.dispatch
lines = [
    "# Issue #757 Protobuf buffer core",
    "",
    "Part of #757. Measurement head: `" + contract.measurement_head + "`; delivery head: `" + args.delivery_head + "`.",
    "",
    "## Completed",
    "",
    "- Caller-owned MessageSupport ByteBuffer APIs",
    *[
        f"- {DISPATCH_LABELS[name]}: retained" if retained else
        f"- {DISPATCH_LABELS[name]}: removed after repeatable allocation regression in {', '.join(contract.removal_reasons[name])}"
        for name, retained in dispatch.items()
    ],
    "- Two clean exact-head gc.alloc.rate.norm runs and fail-closed validation",
    "",
    "## Allocation verdicts",
    "",
    "| Cell | Verdict |",
    "|---|---|",
    *[f"| `{cell}` | `{verdicts[cell]}` |" for cell in (*ELIGIBLE, *INELIGIBLE)],
    "",
    "Composite Redisson and trusted-fallback compatibility cells are claim-ineligible controls.",
    "This is neither a zero-copy claim nor a throughput guarantee.",
    "",
    "## Validation",
    "",
    "- `./gradlew :bluetape4k-protobuf:test --no-configuration-cache`",
    "- `./gradlew :protobuf-codec-benchmark:test :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache`",
    "- `python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-promoted ...`",
    "",
    "## Deferred",
    "",
    "- Lettuce integration remains a separate follow-up slice.",
    "",
    "## DoD Status",
    "",
    "- [x] Core API, security, resource, benchmark, and evidence gates complete",
    "- [ ] Merge requires fresh explicit approval",
    "",
]
pathlib.Path(args.output).write_text("\n".join(lines))
```

```bash
PR_NUMBER=$(gh pr list \
  --repo bluetape4k/bluetape4k-projects \
  --state open --base develop --head feat/issue-757-protobuf-buffer-core \
  --json number --jq 'if length == 0 then "" elif length == 1 then .[0].number else error("multiple open PRs for head") end')
python3 .omx/tmp/issue-757/prepare-pr-body.py \
  --comparison docs/benchmarks/raw/issue-757/comparison.csv \
  --delivery-head "$(git rev-parse HEAD)" \
  --delivery-manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --repo-root "$(pwd)" \
  --output .omx/tmp/issue-757/pr-body.md
python3 -m unittest .omx/tmp/issue-757/test_delivery_common.py
rg -n '^#757|issue.?757|Lettuce|zero-copy|throughput|gc\.alloc\.rate\.norm|## DoD Status' \
  .omx/tmp/issue-757/pr-body.md
rg -F "$(git rev-parse HEAD)" .omx/tmp/issue-757/pr-body.md
for cell in serializerEncodeHeapOptimized serializerEncodeDirectOptimized \
  serializerDecodeHeapOptimized serializerDecodeDirectOptimized redissonDecodeContiguousOptimized \
  redissonDecodeCompositeCompatibility trustedFallbackEncodeByteArray \
  trustedFallbackEncodeBufferCompatibility trustedFallbackDecodeByteArray \
  trustedFallbackDecodeBufferCompatibility; do
  rg -F "$cell" .omx/tmp/issue-757/pr-body.md
done
test -z "$(rg -ni '\b(close[sd]?|fix(e[sd])?|resolve[sd]?)\s+#757\b' \
  .omx/tmp/issue-757/pr-body.md || true)"
test "$(rg -n '^## DoD Status$' .omx/tmp/issue-757/pr-body.md | wc -l | tr -d ' ')" = "1"
test "$(tail -n +$(rg -n '^## DoD Status$' .omx/tmp/issue-757/pr-body.md | cut -d: -f1) \
  .omx/tmp/issue-757/pr-body.md | rg '^## ' | wc -l | tr -d ' ')" = "1"
git push -u origin feat/issue-757-protobuf-buffer-core
if test -z "$PR_NUMBER"; then
  gh pr create \
    --repo bluetape4k/bluetape4k-projects \
    --base develop \
    --head feat/issue-757-protobuf-buffer-core \
    --title "Add lower-copy Protobuf buffer paths" \
    --body-file .omx/tmp/issue-757/pr-body.md
  PR_NUMBER=$(gh pr list \
    --repo bluetape4k/bluetape4k-projects \
    --state open --base develop --head feat/issue-757-protobuf-buffer-core \
    --json number --jq 'if length == 1 then .[0].number else error("expected one open PR") end')
else
  gh pr edit "$PR_NUMBER" \
    --repo bluetape4k/bluetape4k-projects \
    --body-file .omx/tmp/issue-757/pr-body.md
fi
test -n "$PR_NUMBER"
LOCAL_HEAD=$(git rev-parse HEAD)
python3 -c 'import json,os,pathlib,sys; p=pathlib.Path(sys.argv[1]); t=p.with_suffix(".tmp"); t.write_text(json.dumps({"repo":"bluetape4k/bluetape4k-projects","base":"develop","head_branch":"feat/issue-757-protobuf-buffer-core","number":int(sys.argv[2]),"local_head":sys.argv[3]},sort_keys=True)+"\n"); os.replace(t,p)' \
  .omx/tmp/issue-757/delivery.json "$PR_NUMBER" "$LOCAL_HEAD"
```

Use `apply_patch` to create `.omx/tmp/issue-757/update-issue.py` with this exact guarded transformation, then execute the commands below:

```python
import json
import pathlib
import re
import sys

from delivery_common import DISPATCH_LABELS, ELIGIBLE, INELIGIBLE, load_delivery_contract

before_path, output_path, pr_number, delivery_head, delivery_manifest, comparison_path, repo_root = sys.argv[1:]
before = json.loads(pathlib.Path(before_path).read_text())
body = before["body"]
dod = list(re.finditer(r"(?m)^## DoD Status\s*$", body))
h2 = list(re.finditer(r"(?m)^## .+$", body))
if len(dod) != 1 or not h2 or h2[-1].start() != dod[0].start():
    raise SystemExit("issue body must contain exactly one final ## DoD Status section")
contract = load_delivery_contract(delivery_manifest, comparison_path, repo_root)
evidence = contract.verdicts
dispatch = contract.dispatch
rows = ["## DoD Status", "", "- [x] Caller-owned MessageSupport ByteBuffer API implemented"]
rows.extend(
    f"- [x] {DISPATCH_LABELS[name]} retained" if retained else
    f"- [x] {DISPATCH_LABELS[name]} removed after repeatable allocation regression in {', '.join(contract.removal_reasons[name])}"
    for name, retained in dispatch.items()
)
rows.extend(f"- [x] allocation verdict `{cell}` = `{evidence[cell]}`" for cell in (*ELIGIBLE, *INELIGIBLE))
rows.append("- [x] composite and trusted-fallback compatibility cells remain claim-ineligible")
rows.extend([
    f"- [x] Two-run allocation evidence: measurement `{contract.measurement_head}` / `{contract.measurement_tree}`; delivery PR #{pr_number} at `{delivery_head}`",
    "- [ ] Lettuce integration deferred to a later PR under #757",
    "",
])
replacement = "\n".join(rows)
pathlib.Path(output_path).write_text(body[: dod[0].start()] + replacement)
```

```bash
PR_NUMBER=$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d=={**d,"repo":"bluetape4k/bluetape4k-projects","base":"develop","head_branch":"feat/issue-757-protobuf-buffer-core"}; print(d["number"])' \
  .omx/tmp/issue-757/delivery.json)
STATE_HEAD=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["local_head"])' \
  .omx/tmp/issue-757/delivery.json)
test "$STATE_HEAD" = "$(git rev-parse HEAD)"
gh issue view 757 \
  --repo bluetape4k/bluetape4k-projects \
  --json number,title,body,milestone,labels,assignees,state,url \
  > .omx/tmp/issue-757/issue-pre-edit.json
python3 -c 'import json,re,sys; old=json.load(open(sys.argv[1])); fresh=json.load(open(sys.argv[2])); dod=list(re.finditer(r"(?m)^## DoD Status\s*$",fresh["body"])); h2=list(re.finditer(r"(?m)^## .+$",fresh["body"])); assert len(dod)==1 and h2 and h2[-1].start()==dod[0].start(); print("issue snapshot unchanged" if old==fresh else "issue changed since orientation; regenerating from immediate pre-edit snapshot")' \
  .omx/tmp/issue-757/issue-before.json .omx/tmp/issue-757/issue-pre-edit.json
python3 -c 'import hashlib,json,pathlib,sys; d=json.load(open(sys.argv[1])); body=d["body"]; pathlib.Path(sys.argv[2]).write_text(body); pathlib.Path(sys.argv[3]).write_text(hashlib.sha256(body.encode()).hexdigest()+"\n")' \
  .omx/tmp/issue-757/issue-pre-edit.json \
  .omx/tmp/issue-757/issue-original.md \
  .omx/tmp/issue-757/issue-original.sha256
python3 .omx/tmp/issue-757/update-issue.py \
  .omx/tmp/issue-757/issue-pre-edit.json .omx/tmp/issue-757/issue-after-body.md \
  "$PR_NUMBER" "$(git rev-parse HEAD)" \
  docs/benchmarks/raw/issue-757/delivery-manifest.json \
  docs/benchmarks/raw/issue-757/comparison.csv "$(pwd)"
python3 -c 'import json,pathlib,re,sys; a=json.load(open(sys.argv[1])); body=pathlib.Path(sys.argv[2]).read_text(); prefix=lambda value: re.split(r"(?m)^## DoD Status\s*$",value,maxsplit=1)[0]; assert prefix(a["body"])==prefix(body); assert "Lettuce" in body and ("#"+sys.argv[3]) in body and sys.argv[4] in body; assert all(x in body for x in ("serializer encode buffer dispatch","serializer decode buffer dispatch","contiguous Redisson decode dispatch"))' \
  .omx/tmp/issue-757/issue-pre-edit.json .omx/tmp/issue-757/issue-after-body.md \
  "$PR_NUMBER" "$(git rev-parse HEAD)"
```

Step 4 only prepares and validates the proposed issue body. Do not mutate issue #757 until Step 5 has passed its first exact-head CI/review gate.

- [ ] **Step 5: Verify the pinned PR head, review state, CI, and stop at the merge gate**

Create `.omx/tmp/issue-757/check-pr.py` and `.omx/tmp/issue-757/test_check_pr.py` with `apply_patch`. The checker must page GraphQL `reviewThreads(first:100, after:$threadCursor)` and `reviews(first:100, after:$reviewCursor)` independently until both `pageInfo.hasNextPage` values are false, fail on GraphQL errors or missing nodes, and write `{thread_total, unresolved_threads, review_total, current_head_approvals}`. Count only review nodes whose state is exactly `APPROVED` and whose `commit.oid` equals the pinned head; dismissed, commented, changes-requested, missing-commit, and old-head reviews do not count. Fixtures must cover independent cursors with 101 threads plus 101 reviews, GraphQL errors, missing nodes, unresolved threads on the second page, old-head approvals, dismissed approvals, and current-head approvals. Run the unit tests before the first live checker invocation.

```bash
PR_NUMBER=$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["repo"]=="bluetape4k/bluetape4k-projects" and d["base"]=="develop" and d["head_branch"]=="feat/issue-757-protobuf-buffer-core"; print(d["number"])' \
  .omx/tmp/issue-757/delivery.json)
LOCAL_HEAD=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["local_head"])' \
  .omx/tmp/issue-757/delivery.json)
test "$LOCAL_HEAD" = "$(git rev-parse HEAD)"
python3 -m unittest .omx/tmp/issue-757/test_check_pr.py
git fetch origin --prune
git merge-base --is-ancestor origin/develop "$LOCAL_HEAD"
REMOTE_HEAD=$(git ls-remote origin refs/heads/feat/issue-757-protobuf-buffer-core | awk '{print $1}')
PR_HEAD=$(gh pr view "$PR_NUMBER" --repo bluetape4k/bluetape4k-projects --json headRefOid --jq .headRefOid)
test "$LOCAL_HEAD" = "$REMOTE_HEAD"
test "$LOCAL_HEAD" = "$PR_HEAD"
gh pr checks "$PR_NUMBER" --repo bluetape4k/bluetape4k-projects --required --watch
PR_JSON=$(gh pr view "$PR_NUMBER" --repo bluetape4k/bluetape4k-projects \
  --json number,url,headRefOid,mergeStateStatus,reviewDecision,statusCheckRollup)
test "$(printf '%s' "$PR_JSON" | jq -r .reviewDecision)" = "APPROVED"
test "$(printf '%s' "$PR_JSON" | jq -r .mergeStateStatus)" = "CLEAN"
python3 .omx/tmp/issue-757/check-pr.py \
  --repo bluetape4k/bluetape4k-projects --pr "$PR_NUMBER" --head "$LOCAL_HEAD" \
  --output .omx/tmp/issue-757/pr-review-state.json
test "$(jq .unresolved_threads .omx/tmp/issue-757/pr-review-state.json)" = "0"
test "$(jq .current_head_approvals .omx/tmp/issue-757/pr-review-state.json)" -ge 1
rg -n 'Performance.*P0=0.*P1=0|Stability.*P0=0.*P1=0|Security.*P0=0.*P1=0|Operator.*P0=0.*P1=0|Developer.*P0=0.*P1=0|Caller.*P0=0.*P1=0' \
  docs/review/issue-757-protobuf-buffer-core-review.md

# Mutate the issue only after the first exact-head CI/review gate succeeds.
gh issue view 757 --repo bluetape4k/bluetape4k-projects \
  --json number,title,body,milestone,labels,assignees,state,url \
  > .omx/tmp/issue-757/issue-pre-edit-final.json
python3 -c 'import hashlib,json,pathlib,re,sys; d=json.load(open(sys.argv[1])); dod=list(re.finditer(r"(?m)^## DoD Status\s*$",d["body"])); h2=list(re.finditer(r"(?m)^## .+$",d["body"])); assert len(dod)==1 and h2 and h2[-1].start()==dod[0].start(); pathlib.Path(sys.argv[2]).write_text(d["body"]); pathlib.Path(sys.argv[3]).write_text(hashlib.sha256(d["body"].encode()).hexdigest()+"\n")' \
  .omx/tmp/issue-757/issue-pre-edit-final.json \
  .omx/tmp/issue-757/issue-original-final.md \
  .omx/tmp/issue-757/issue-original-final.sha256
python3 .omx/tmp/issue-757/update-issue.py \
  .omx/tmp/issue-757/issue-pre-edit-final.json .omx/tmp/issue-757/issue-after-body-final.md \
  "$PR_NUMBER" "$LOCAL_HEAD" docs/benchmarks/raw/issue-757/delivery-manifest.json \
  docs/benchmarks/raw/issue-757/comparison.csv "$(pwd)"
python3 -c 'import hashlib,pathlib,sys; body=pathlib.Path(sys.argv[1]).read_text(); pathlib.Path(sys.argv[2]).write_text(hashlib.sha256(body.encode()).hexdigest()+"\n")' \
  .omx/tmp/issue-757/issue-after-body-final.md \
  .omx/tmp/issue-757/issue-agent-written-final.sha256

restore_issue_757() {
  gh issue view 757 --repo bluetape4k/bluetape4k-projects \
    --json number,title,body,milestone,labels,assignees,state,url \
    > .omx/tmp/issue-757/issue-live-before-restore.json
  if ! python3 -c 'import hashlib,json,pathlib,sys; live=json.load(open(sys.argv[1]))["body"]; expected=pathlib.Path(sys.argv[2]).read_text().strip(); assert hashlib.sha256(live.encode()).hexdigest()==expected' \
    .omx/tmp/issue-757/issue-live-before-restore.json \
    .omx/tmp/issue-757/issue-agent-written-final.sha256; then
    echo "issue #757 changed concurrently; automatic restore refused" >&2
    return 2
  fi
  gh issue edit 757 --repo bluetape4k/bluetape4k-projects \
    --body-file .omx/tmp/issue-757/issue-original-final.md
  gh issue view 757 --repo bluetape4k/bluetape4k-projects \
    --json number,title,body,milestone,labels,assignees,state,url \
    > .omx/tmp/issue-757/issue-restored-final.json
  python3 -c 'import hashlib,json,pathlib,sys; a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2])); expected=pathlib.Path(sys.argv[3]).read_text().strip(); assert hashlib.sha256(b["body"].encode()).hexdigest()==expected; keep=lambda d: (d["milestone"],d["labels"],d["assignees"],d["state"]); assert keep(a)==keep(b)' \
    .omx/tmp/issue-757/issue-pre-edit-final.json \
    .omx/tmp/issue-757/issue-restored-final.json \
    .omx/tmp/issue-757/issue-original-final.sha256
}

issue_restore_pending=0
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'status=$?; trap - EXIT INT TERM; if test "$issue_restore_pending" = 1; then restore_issue_757 || status=1; fi; exit "$status"' EXIT
gh issue edit 757 --repo bluetape4k/bluetape4k-projects \
  --body-file .omx/tmp/issue-757/issue-after-body-final.md
issue_restore_pending=1
gh issue view 757 --repo bluetape4k/bluetape4k-projects \
  --json number,title,body,milestone,labels,assignees,state,url \
  > .omx/tmp/issue-757/issue-after-final.json
if ! python3 -c 'import json,re,sys; a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2])); meta=lambda d: ((d["milestone"] or {}).get("title"),sorted(x["name"] for x in d["labels"]),sorted(x["login"] for x in d["assignees"]),d["state"]); prefix=lambda d: re.split(r"(?m)^## DoD Status\s*$",d["body"],maxsplit=1)[0]; assert meta(a)==meta(b),(meta(a),meta(b)); assert prefix(a)==prefix(b); assert "Lettuce" in b["body"] and ("#"+sys.argv[3]) in b["body"] and sys.argv[4] in b["body"]; assert all(x in b["body"] for x in ("serializer encode buffer dispatch","serializer decode buffer dispatch","contiguous Redisson decode dispatch"))' \
  .omx/tmp/issue-757/issue-pre-edit-final.json .omx/tmp/issue-757/issue-after-final.json \
  "$PR_NUMBER" "$LOCAL_HEAD"; then
  exit 1
fi
python3 -c 'import hashlib,json,pathlib,sys; body=json.load(open(sys.argv[1]))["body"]; pathlib.Path(sys.argv[2]).write_text(hashlib.sha256(body.encode()).hexdigest()+"\n")' \
  .omx/tmp/issue-757/issue-after-final.json \
  .omx/tmp/issue-757/issue-agent-written-final.sha256

# Any late CI/review/head drift restores the exact pre-edit issue body.
if ! {
  gh pr checks "$PR_NUMBER" --repo bluetape4k/bluetape4k-projects --required &&
  python3 .omx/tmp/issue-757/check-pr.py \
    --repo bluetape4k/bluetape4k-projects --pr "$PR_NUMBER" --head "$LOCAL_HEAD" \
    --output .omx/tmp/issue-757/pr-review-state-final.json &&
  test "$(jq .unresolved_threads .omx/tmp/issue-757/pr-review-state-final.json)" = "0" &&
  test "$(jq .current_head_approvals .omx/tmp/issue-757/pr-review-state-final.json)" -ge 1 &&
  git fetch origin --prune &&
  git merge-base --is-ancestor origin/develop "$LOCAL_HEAD" &&
  test "$(git ls-remote origin refs/heads/feat/issue-757-protobuf-buffer-core | awk '{print $1}')" = "$LOCAL_HEAD" &&
  FINAL_PR_JSON=$(gh pr view "$PR_NUMBER" --repo bluetape4k/bluetape4k-projects \
    --json number,url,headRefOid,mergeStateStatus,reviewDecision,statusCheckRollup) &&
  test "$(printf '%s' "$FINAL_PR_JSON" | jq -r .headRefOid)" = "$LOCAL_HEAD" &&
  test "$(printf '%s' "$FINAL_PR_JSON" | jq -r .reviewDecision)" = "APPROVED" &&
  test "$(printf '%s' "$FINAL_PR_JSON" | jq -r .mergeStateStatus)" = "CLEAN"
}; then
  exit 1
fi
issue_restore_pending=0
trap - EXIT INT TERM
```

Do not pass milestone/label/assignee flags. The final issue edit plus fetch/head/base/check/review recovery block is the last action before reporting merge-ready. EXIT/INT/TERM recovery restores only when the live body hash still equals the agent-written body; a concurrent human edit refuses automatic overwrite and becomes a reported blocker. If any late gate fails, restore and verify the exact pre-edit issue body when safe, discard the earlier CI/review sample, and restart Step 5 against the new pinned state. Expected: local, remote, and the pinned PR head match; freshly fetched `origin/develop` remains an ancestor; every required check exits success; aggregate review decision and merge state are `APPROVED`/`CLEAN`; all paginated review threads are resolved; at least one non-dismissed current-head approval exists; issue metadata and pre-DoD body remain unchanged; the six-lens artifact is P0=0/P1=0. Report the exact PR number/URL/head as merge-ready and stop. Do not merge, enable auto-merge, tag, release, publish, delete the branch, or remove the worktree without fresh explicit approval.

## Plan Acceptance Checklist

- Every design acceptance criterion maps to a task and fresh verification command.
- Tests precede production implementation in Tasks 1–6.
- Loader identity, assignability, fallback exception boundaries, partial writes, empty input, composite input, and temporary `ByteBuf` release have explicit tests.
- The benchmark matrix is exact and fail-closed; missing or invalid metrics cannot become inconclusive.
- Measurement occurs only from a clean implementation commit and stages both runs under ignored `build/` paths before docs are touched.
- Positive, inconclusive, regressed, and ineligible outcomes have distinct actions.
- Lettuce, Kafka, compression wrappers, releases, and merge remain outside this execution slice or behind separate gates.
