# Issue #1039 Serializer Allocation Benchmark Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a standalone, reproducible serializer allocation benchmark that permits only two-run, allocation-backed ByteBuffer claims and documents every optimized and fallback boundary.

**Architecture:** Create `benchmark/serializer-benchmark` using the repository `kotlinx-benchmark` JMH pattern. Keep payload, comparison adapters, and validation support inside the benchmark-only module; measure binary, JSON, and Avro serialization/deserialization separately; derive claim decisions from two raw JMH GC-profiler runs with a tested Python summarizer. Publish measured numbers only in the central benchmark report and link representative English/Korean module documentation to it.

**Tech
Stack:** Kotlin 2.3, Java 21, Gradle 9.6, `kotlinx-benchmark`, JMH GC profiler, JUnit 5/Kluent, Python 3 standard library, existing bluetape4k serializer modules.

---

## Preconditions And Execution Contract

- Worktree: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-issue-754-allocation-proof`
- Branch: `feat/issue-754-allocation-proof`
- Base: `origin/develop@09402f87752412031266059547be7a2f6351268d`
- Approved spec: `docs/superpowers/specs/2026-07-18-issue-1039-serializer-allocation-benchmark-design.md`
- Workflow: Type A (`bluetape-full-feature`)
- Required implementation skills: `test-driven-development`, `bluetape-kotlin-patterns`, and `bluetape-writer` for English/Korean documentation.
- Benchmark, native, real-service, Testcontainers, and other heavyweight commands run sequentially. This scope has benchmark commands but no native, service, or Testcontainers command.
- Stop immediately if an implementation would change an existing `ByteArray` API, wire/security behavior, serializer registration, or any #755-#758 surface.
- Pull request delivery targets repository `bluetape4k/bluetape4k-projects`, base `develop`, and head `feat/issue-754-allocation-proof`. Merge remains outside implementation authority until a fresh exact-head approval.

## File And Responsibility Map

### New benchmark module

- `benchmark/serializer-benchmark/build.gradle.kts` — benchmark source set, JMH target, serializer project dependencies, and test dependencies.
- `benchmark/serializer-benchmark/README.md` — English purpose, commands, matrix, and evidence link.
- `benchmark/serializer-benchmark/README.ko.md` — Korean parity document.
- `benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkPayload.kt` — deterministic cross-backend payload and semantic comparison.
- `benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupport.kt` — compatibility-default adapters, buffer preparation, and fixture validation outside timed methods.
- `benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupportTest.kt` — payload, adapter, buffer, path-label, and round-trip contracts.
- `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/BinarySerializerAllocationBenchmark.kt` — JDK, Kryo, and Fory allocation cells.
- `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/JsonSerializerAllocationBenchmark.kt` — Jackson 2, Jackson 3, and Fastjson2 allocation cells.
- `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/AvroSerializerAllocationBenchmark.kt` — Avro reflect allocation cells.
- `benchmark/serializer-benchmark/scripts/summarize-jmh.py` — validate two runs, extract allocation/GC/throughput fields, apply eligibility and 5% claim rules, and write compact CSV/Markdown fragments.
- `benchmark/serializer-benchmark/scripts/test_summarize_jmh.py` — Python standard-library unit tests for extraction, missing metrics, fallback exclusion, disagreement, and threshold decisions.

### Evidence and public documentation

- `docs/benchmarks/raw/issue-1039/run-*/jmh.json` — raw JMH JSON from each fresh evidence run.
- `docs/benchmarks/raw/issue-1039/run-*/summary.csv` — compact per-run metric rows.
- `docs/benchmarks/raw/issue-1039/run-*/environment.txt` — exact commit, OS, CPU, memory, JDK, Gradle, and JMH command.
- `docs/benchmarks/raw/issue-1039/comparison.csv` — two-run comparison and claim verdicts.
- `docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md` — numeric source of truth.
- `docs/benchmarks/README.md` — report index entry.
- `CHANGELOG.md` — milestone-facing summary without duplicating numeric tables.
- `io/io/README.md`, `io/io/README.ko.md` — core binary capability and measured-cell summary.
- `io/jackson2/README.md`, `io/jackson2/README.ko.md` — Jackson 2 contract and evidence link.
- `io/jackson3/README.md`, `io/jackson3/README.ko.md` — Jackson 3 contract and evidence link.
- `io/fastjson2/README.md`, `io/fastjson2/README.ko.md` — JSONB array-backed optimization and direct/read-only fallback.
- `io/avro/README.md`, `io/avro/README.ko.md` — measured reflect scope and unmeasured generic/specific limits.
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializer.kt` — fallback and caller-owned buffer KDoc.
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt` — measured optimized-cell KDoc link.
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializer.kt` — measured optimized-cell KDoc link.
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializer.kt` — input-only claim and output fallback KDoc.
- `io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt` — JSON compatibility-default KDoc.
- `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt` — Jackson 2 measured-cell limitation.
- `io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt` — Jackson 3 measured-cell limitation.
- `io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt` — output/direct/read-only fallback limits.
- `io/avro/src/main/kotlin/io/bluetape4k/avro/AvroReflectSerializer.kt` — reflect interface evidence boundary.
- `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializer.kt` — measured reflect implementation note.
- `docs/lessons/2026-07-18-issue-1039-serializer-allocation-proof.md` — durable benchmark and claim-gating lesson required by Type A.

## Traceability Map

| Spec requirement                                       | Plan tasks  |
|--------------------------------------------------------|-------------|
| Standalone `kotlinx-benchmark` module and registration | 1, 7, 10    |
| Deterministic equivalent payload and untimed setup     | 2, 3, 4, 5  |
| ByteArray/default/optimized matrix                     | 2, 3, 4, 5  |
| Separate serialization/deserialization                 | 3, 4, 5     |
| Two fresh GC-profiler runs and raw artifacts           | 6, 7, 8     |
| 5% same-direction claim rule                           | 6, 8        |
| Fallback ergonomic-only exclusion                      | 2, 4, 6, 9  |
| Position/limit/overflow/rollback and semantic proof    | 2, 7, 9, 10 |
| Public KDoc, EN/KO README parity, changelog            | 9, 10       |
| ABI, affected tests, Detekt, proportional build        | 10          |
| #755-#758 and release exclusions                       | 9, 10, 12   |
| P0/P1 convergence, lesson, exact-head PR               | 10, 11, 12  |

## Type A Step 3-R Plan Review Record

The detailed plan was reviewed in the main session because the available native subagent surface cannot carry the required installed role identifier. The six independent lenses converge as follows:

- Performance: primary evidence is normalized allocation (`gc.alloc.rate.norm`), with throughput explicitly diagnostic; two fresh runs and the same-direction 5% threshold prevent single-run claims.
- Stability: setup validates semantic round trips, bounded position/limit behavior, overflow, source-state preservation, and exact 40-cell smoke coverage before evidence is accepted.
- Security: benchmark configuration and documentation do not change production serializer registration, wire formats, security defaults, or ownership rules.
- Operations: raw JSON, compact CSV, environment metadata, exact commands, and immutable run IDs make failures diagnosable and reruns distinguishable.
- Developer/API: the benchmark-only module avoids production dependency and ABI expansion; compatibility defaults and optimized paths remain visibly separate.
- User/caller: English/Korean documentation states caller-owned buffer guidance, fallback limits, inconclusive results, and deferred #755-#758 scope.

Result: P0=0, P1=0. Runtime-generated run IDs and measured verdicts are intentionally unresolved until Tasks 8-9; their commands and fail-closed gates are fixed here.

### Task 1: Register The Standalone Benchmark Module

**Complexity:** Medium **Depends on:** Approved spec **Write scope:** `benchmark/serializer-benchmark/build.gradle.kts`
**Rollback/Rerun:** Remove only the benchmark module shell if registration or non-publication guards fail, then rerun Steps 1-4 before committing.

**Files:**

- Create: `benchmark/serializer-benchmark/build.gradle.kts`

- [ ] **Step 1: Prove the module is absent**

Run:

```bash
./gradlew :serializer-benchmark:tasks --all --no-configuration-cache
```

Expected: FAIL with `project 'serializer-benchmark' not found`.

- [ ] **Step 2: Add the repository-standard benchmark build**

Create the build with this structure:

```kotlin
plugins {
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlinx.benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

sourceSets {
    create("benchmark")
}

kotlin {
    target {
        compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
    }
}

configurations {
    named("benchmarkImplementation") {
        extendsFrom(
            configurations.getByName("implementation"),
            configurations.getByName("compileOnly"),
            configurations.getByName("testImplementation"),
        )
    }
    named("benchmarkRuntimeOnly") {
        extendsFrom(
            configurations.getByName("runtimeOnly"),
            configurations.getByName("testRuntimeOnly"),
        )
    }
}

benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
    }
}

dependencies {
    implementation(project(":bluetape4k-io"))
    implementation(project(":bluetape4k-json"))
    implementation(project(":bluetape4k-jackson2"))
    implementation(project(":bluetape4k-jackson3"))
    implementation(project(":bluetape4k-fastjson2"))
    implementation(project(":bluetape4k-avro"))

    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime)
    add("benchmarkImplementation", libs.kotlinx.benchmark.runtime.jvm)
    add("benchmarkImplementation", libs.jmh.core)
    add("benchmarkRuntimeOnly", libs.logback.classic)
}
```

- [ ] **Step 3: Verify auto-registration and generated task names**

Run:

```bash
./gradlew projects :serializer-benchmark:tasks --all --no-configuration-cache \
  | tee /tmp/issue-1039-serializer-benchmark-tasks.txt
rg -q "project ':serializer-benchmark'" /tmp/issue-1039-serializer-benchmark-tasks.txt
rg -q '^benchmarkBenchmarkCompile' /tmp/issue-1039-serializer-benchmark-tasks.txt
rg -q '^benchmarkBenchmarkJar' /tmp/issue-1039-serializer-benchmark-tasks.txt
```

Expected: PASS; the project and both generated tasks are present.

- [ ] **Step 4: Verify non-publication and coverage exclusion rules apply**

Run:

```bash
rg -n 'includeModules\("benchmark", false, false\)' settings.gradle.kts
rg -n 'name.endsWith\("-benchmark"\)' build.gradle.kts
rg -n 'sourceDir.startsWith\("benchmark/"\)' build.gradle.kts
```

Expected: all three existing guards are present; no BOM/catalog or publishing edit is needed.

- [ ] **Step 5: Commit the module shell**

```bash
git add benchmark/serializer-benchmark/build.gradle.kts
git commit -m "Isolate serializer allocation evidence from production modules" \
  -m "Constraint: Cross-backend benchmark dependencies must stay under benchmark/" \
  -m "Rejected: Add Jackson and Avro dependencies to io/io | that reverses production module boundaries" \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Directive: Keep serializer-benchmark excluded from publication and Kover aggregation" \
  -m "Tested: ./gradlew projects :serializer-benchmark:tasks --all --no-configuration-cache" \
  -m "Not-tested: Benchmark source does not exist yet"
```

### Task 2: Lock Payload, Fallback, And Buffer Contracts

**Complexity:** High **Depends on:** Task 1 **Write scope:** benchmark module main/test Kotlin sources **Required
skills:** `test-driven-development`, `bluetape-kotlin-patterns`
**Rollback/Rerun:** Revert only fixture/payload changes that violate a contract; rerun the full support test and every affected serializer contract test.

**Files:**

- Create: `benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkPayload.kt`
- Create: `benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupport.kt`
- Create: `benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/SerializerBenchmarkSupportTest.kt`

- [ ] **Step 1: Write failing payload and adapter contract tests**

The tests must cover these concrete cases:

```kotlin
class SerializerBenchmarkSupportTest {
    @Test
    fun `payload is deterministic and semantically comparable`() {
        val first = SerializerBenchmarkPayload.sample()
        val second = SerializerBenchmarkPayload.sample()
        first shouldBeSemanticallyEqualTo second
        first.payload shouldNotBeSameInstanceAs second.payload
    }

    @Test
    fun `binary compatibility adapter executes the interface default buffer path`() {
        val delegate = RecordingBinarySerializer()
        val adapter = CompatibilityBinarySerializer(delegate)
        val target = ByteBuffer.allocateDirect(256)
        adapter.serializeTo(SerializerBenchmarkPayload.sample(), target) shouldBeGreaterThan 0
        delegate.byteArraySerializeCalls shouldBeEqualTo 1
    }

    @Test
    fun `buffer validation rejects overflow before evidence`() {
        val fixture = SerializerBenchmarkFixture(JdkBinarySerializer())
        assertThrows<BufferOverflowException> {
            fixture.validateTarget(ByteBuffer.allocate(1))
        }
    }

    @Test
    fun `all production fixtures round trip without changing source state`() {
        serializerFixtures().forEach { fixture ->
            val source = fixture.precomputedOptimizedSource()
            val position = source.position()
            val limit = source.limit()
            fixture.deserializeOptimized(source) shouldBeSemanticallyEqualTo fixture.payload
            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
        }
    }
}
```

Use recording fakes only for dispatch proof; production fixtures must use default JDK, Kryo, Fory, Jackson 2, Jackson 3, Fastjson2, and `DefaultAvroReflectSerializer` configurations.

- [ ] **Step 2: Run the tests and observe RED**

Run:

```bash
./gradlew :serializer-benchmark:test --tests '*SerializerBenchmarkSupportTest' --no-configuration-cache
```

Expected: FAIL because payload, adapters, and fixtures are undefined.

- [ ] **Step 3: Implement the deterministic payload**

Use a Java-serializable Kotlin class with default construction and mutable fields so JDK, JSON, and Avro reflect paths consume one logical shape:

```kotlin
data class SerializerBenchmarkPayload @JvmOverloads constructor(
    var id: Int = 42,
    var name: String = "bluetape4k-serializer-allocation",
    var timestamp: Long = 1_700_000_000_000L,
    var tags: List<String> = listOf("byte-array", "byte-buffer", "allocation"),
    var payload: ByteArray = ByteArray(1024) { index -> (index * 31).toByte() },
): java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L

        fun sample(): SerializerBenchmarkPayload = SerializerBenchmarkPayload()
    }
}

infix fun SerializerBenchmarkPayload?.shouldBeSemanticallyEqualTo(expected: SerializerBenchmarkPayload?) {
    requireNotNull(this)
    requireNotNull(expected)
    id shouldBeEqualTo expected.id
    name shouldBeEqualTo expected.name
    timestamp shouldBeEqualTo expected.timestamp
    tags shouldBeEqualTo expected.tags
    payload shouldBeEqualTo expected.payload
}
```

Keep test assertions in test sources if production assertion imports would leak into `main`; the payload itself contains no test-library dependency.

- [ ] **Step 4: Implement compatibility adapters and fixtures**

`CompatibilityBinarySerializer`, `CompatibilityJsonSerializer`, and
`CompatibilityAvroReflectSerializer` implement only existing ByteArray methods and deliberately inherit public interface buffer defaults. `SerializerBenchmarkFixture`
must:

```kotlin
interface SerializerBenchmarkFixture {
    val name: String
    val payload: SerializerBenchmarkPayload
    val claimEligibleSerialize: Boolean
    val claimEligibleDeserialize: Boolean

    fun serializeByteArray(): ByteArray
    fun serializeCompatibility(target: ByteBuffer): Int
    fun serializeOptimized(target: ByteBuffer): Int
    fun deserializeByteArray(): SerializerBenchmarkPayload?
    fun deserializeCompatibility(source: ByteBuffer): SerializerBenchmarkPayload?
    fun deserializeOptimized(source: ByteBuffer): SerializerBenchmarkPayload?
    fun precomputedOptimizedSource(): ByteBuffer
    fun validate()
}
```

All target buffers are allocated once with direct capacity `max(wireSize * 2, 4096)`.
`precomputedOptimizedSource()` returns a direct source for JDK, Kryo, Fory, Jackson 2, Jackson 3, and Avro, but a writable array-backed heap source for Fastjson2. Fastjson direct and read-only sources are exposed only as explicitly labeled fallback controls.
`validate()` checks semantic equality, exact written length, source state preservation, successful non-zero-position writes, and overflow failure before benchmarks compile.

- [ ] **Step 5: Run GREEN and affected serializer contract tests**

Run sequentially:

```bash
./gradlew :serializer-benchmark:test --tests '*SerializerBenchmarkSupportTest' --no-configuration-cache
./gradlew :bluetape4k-io:test --tests '*CoreBinarySerializerByteBufferTest*' --no-configuration-cache
./gradlew :bluetape4k-jackson2:test --tests '*JacksonSerializerByteBufferTest' --no-configuration-cache
./gradlew :bluetape4k-jackson3:test --tests '*JacksonSerializerByteBufferTest' --no-configuration-cache
./gradlew :bluetape4k-fastjson2:test --tests '*FastjsonSerializerByteBufferTest' --no-configuration-cache
./gradlew :bluetape4k-avro:test --tests '*AvroSerializerByteBufferContractTest' --no-configuration-cache
```

Expected: all commands PASS. The existing Avro reflect tests already establish support for mutable Kotlin data classes with defaults and nested/list fields, so failure here is an implementation defect to diagnose rather than a payload-type branch in this plan.

- [ ] **Step 6: Commit the locked contract**

```bash
git add benchmark/serializer-benchmark/src/main benchmark/serializer-benchmark/src/test
git commit -m "Make serializer allocation cells comparable before timing them" \
  -m "Constraint: Every backend must use one deterministic payload and untimed validation" \
  -m "Rejected: Validate decoded values inside JMH methods | assertion work would pollute allocation metrics" \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Directive: Compatibility adapters must inherit interface defaults and remain claim-ineligible" \
  -m "Tested: serializer benchmark support tests and affected ByteBuffer contract tests" \
  -m "Not-tested: JMH cells are added in later tasks"
```

### Task 3: Add Core Binary Allocation Cells

**Complexity:** High **Depends on:** Task 2 **Write scope:** binary benchmark source only
**Rollback/Rerun:** Revert only the binary benchmark source if the matrix or capability labels are wrong, then rerun benchmark compile and affected contracts.

**Files:**

- Create: `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/BinarySerializerAllocationBenchmark.kt`

- [ ] **Step 1: Add a compile-failing benchmark declaration**

Create the class with imports and one call to the not-yet-declared fixture state, then run compile to observe RED:

```kotlin
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
open class BinarySerializerAllocationBenchmark {
    private lateinit var jdk: BinaryBenchmarkState

    @Setup(Level.Trial)
    fun setup() {
        jdk = BinaryBenchmarkState.jdk()
        jdk.validate()
    }
}
```

Run:

```bash
./gradlew :serializer-benchmark:compileBenchmarkKotlin --no-configuration-cache
```

Expected: FAIL because `BinaryBenchmarkState` is undefined.

- [ ] **Step 2: Implement JDK and Kryo comparison methods**

Use a prevalidated `@State(Scope.Thread)` fixture and these exact method names:

```text
jdkSerializeByteArray
jdkSerializeCompatibility
jdkSerializeOptimized
jdkDeserializeByteArray
jdkDeserializeCompatibility
jdkDeserializeOptimized
kryoSerializeByteArray
kryoSerializeCompatibility
kryoSerializeOptimized
kryoDeserializeByteArray
kryoDeserializeCompatibility
kryoDeserializeOptimized
```

Every serialization buffer method resets the preallocated direct target before the timed call. Every deserialization method uses a precomputed input. A complete method follows this shape:

```kotlin
@Benchmark
fun jdkSerializeOptimized(blackhole: Blackhole) {
    jdk.resetTarget()
    blackhole.consume(jdk.optimized.serializeTo(jdk.payload, jdk.target))
}

@Benchmark
fun jdkDeserializeOptimized(blackhole: Blackhole) {
    blackhole.consume(jdk.optimized.deserializeFrom<SerializerBenchmarkPayload>(jdk.directSource))
}
```

- [ ] **Step 3: Add Fory's asymmetric cells**

Use these exact method names:

```text
forySerializeByteArray
forySerializeFallback
foryDeserializeByteArray
foryDeserializeOptimized
```

Fory output calls the production `serializeTo` fallback and remains claim-ineligible. Fory optimized input uses a precomputed direct bounded source. Do not add an optimized-output label.

- [ ] **Step 4: Compile and inspect generated JMH methods**

Run:

```bash
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
find benchmark/serializer-benchmark/build/generated -type f -name '*BinarySerializerAllocationBenchmark*' -print
```

Expected: PASS and generated JMH sources include all 16 method names.

- [ ] **Step 5: Commit binary cells**

```bash
git add benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/BinarySerializerAllocationBenchmark.kt
git commit -m "Separate core serializer allocation paths by capability" \
  -m "Constraint: JDK and Kryo support optimized input/output while Fory output remains fallback" \
  -m "Rejected: Label every ByteBuffer method optimized | production Fory output still allocates a ByteArray" \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Directive: Keep serialize and deserialize cells separate and reset only preallocated targets" \
  -m "Tested: :serializer-benchmark:benchmarkBenchmarkCompile" \
  -m "Not-tested: Allocation evidence runs remain pending"
```

### Task 4: Add JSON Allocation Cells

**Complexity:** High **Depends on:** Task 2 **Write scope:** JSON benchmark source only
**Rollback/Rerun:** Revert only the JSON benchmark source if dispatch or labels are wrong, then rerun benchmark compile and Jackson/Fastjson contracts.

**Files:**

- Create: `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/JsonSerializerAllocationBenchmark.kt`

- [ ] **Step 1: Write a compile-failing Jackson state**

Declare `JsonSerializerAllocationBenchmark` with `@State(Scope.Thread)`,
`Mode.Throughput`, and a missing `Jackson2BenchmarkState`; compile and observe the undefined-state failure.

Run:

```bash
./gradlew :serializer-benchmark:compileBenchmarkKotlin --no-configuration-cache
```

Expected: FAIL for `Jackson2BenchmarkState`.

- [ ] **Step 2: Implement Jackson 2 and Jackson 3 cells**

Alias imports to avoid class-name ambiguity:

```kotlin
import io.bluetape4k.jackson.JacksonSerializer as Jackson2Serializer
import io.bluetape4k.jackson3.JacksonSerializer as Jackson3Serializer
```

Implement these exact method names for each version:

```text
jackson2SerializeByteArray
jackson2SerializeCompatibility
jackson2SerializeOptimized
jackson2DeserializeByteArray
jackson2DeserializeCompatibility
jackson2DeserializeOptimized
jackson3SerializeByteArray
jackson3SerializeCompatibility
jackson3SerializeOptimized
jackson3DeserializeByteArray
jackson3DeserializeCompatibility
jackson3DeserializeOptimized
```

Use the default mapper for each version. Compatibility cells use
`CompatibilityJsonSerializer`; optimized cells use the concrete serializer.

- [ ] **Step 3: Implement Fastjson2 capability cells**

Use these exact method names:

```text
fastjsonSerializeByteArray
fastjsonSerializeFallback
fastjsonDeserializeByteArray
fastjsonDeserializeOptimizedHeap
fastjsonDeserializeFallbackDirect
fastjsonDeserializeFallbackReadOnly
```

The optimized input is a writable array-backed buffer. The direct and read-only inputs are separate compatibility controls. All Fastjson output buffer cells are claim-ineligible because production calls `JSONB.toBytes` first.

- [ ] **Step 4: Compile and verify the JSON method matrix**

Run:

```bash
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
find benchmark/serializer-benchmark/build/generated -type f -name '*JsonSerializerAllocationBenchmark*' -print
```

Expected: PASS and generated sources cover all 18 method names.

- [ ] **Step 5: Commit JSON cells**

```bash
git add benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/JsonSerializerAllocationBenchmark.kt
git commit -m "Keep JSON allocation claims aligned with real buffer dispatch" \
  -m "Constraint: Jackson streams are optimized while Fastjson2 output and non-array input remain fallback" \
  -m "Rejected: Compare unlike Fastjson buffer kinds as one candidate | array-backed and direct paths have different capabilities" \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Directive: Never attribute Fastjson2 direct or read-only input scores to the array-backed optimization" \
  -m "Tested: :serializer-benchmark:benchmarkBenchmarkCompile" \
  -m "Not-tested: Allocation evidence runs remain pending"
```

### Task 5: Add Avro Reflect Allocation Cells

**Complexity:** Medium **Depends on:** Task 2 **Write scope:** Avro benchmark source only
**Rollback/Rerun:** Revert only the Avro benchmark source if reflect scope or cell labels are wrong, then rerun benchmark compile and the Avro contract test.

**Files:**

- Create: `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/AvroSerializerAllocationBenchmark.kt`

- [ ] **Step 1: Write the compile-failing Avro benchmark state**

Declare the JMH class and reference an undefined `AvroReflectBenchmarkState`.

Run:

```bash
./gradlew :serializer-benchmark:compileBenchmarkKotlin --no-configuration-cache
```

Expected: FAIL for the undefined state.

- [ ] **Step 2: Implement the six measured reflect cells**

Use default `DefaultAvroReflectSerializer()` configuration and exact methods:

```text
avroReflectSerializeByteArray
avroReflectSerializeCompatibility
avroReflectSerializeOptimized
avroReflectDeserializeByteArray
avroReflectDeserializeCompatibility
avroReflectDeserializeOptimized
```

The setup serializes and decodes once, checks semantics, and validates that the optimized direct target capacity is sufficient. Generic, specific, and list APIs must not appear in this benchmark class or receive allocation claims.

- [ ] **Step 3: Compile and run the Avro contract test**

Run sequentially:

```bash
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
./gradlew :bluetape4k-avro:test --tests '*AvroSerializerByteBufferContractTest' --no-configuration-cache
```

Expected: both commands PASS.

- [ ] **Step 4: Commit Avro cells**

```bash
git add benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/AvroSerializerAllocationBenchmark.kt
git commit -m "Measure Avro allocation only where reflect paths were exercised" \
  -m "Constraint: Issue #1039 uses reflect as the representative Avro allocation cell" \
  -m "Rejected: Generalize reflect results to generic, specific, and list serializers | those cells are unmeasured" \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Directive: Keep unmeasured Avro variants out of allocation-reduction wording" \
  -m "Tested: serializer benchmark compile and Avro ByteBuffer contract test" \
  -m "Not-tested: Allocation evidence runs remain pending"
```

### Task 6: Test And Implement Evidence Summarization

**Complexity:** High **Depends on:** Tasks 3-5 **Write scope:** benchmark Python scripts only
**Rollback/Rerun:** Any parser or verdict failure invalidates derived evidence; fix the bounded scripts, rerun all unit tests, and regenerate every CSV.

**Files:**

- Create: `benchmark/serializer-benchmark/scripts/summarize-jmh.py`
- Create: `benchmark/serializer-benchmark/scripts/test_summarize_jmh.py`

- [ ] **Step 1: Write failing standard-library unit tests**

Use `unittest`, `tempfile`, and synthetic JMH JSON. Define these seven tests:

```text
test_extracts_gc_and_diagnostic_metrics
test_marks_two_runs_below_five_percent_inconclusive
test_accepts_two_claim_eligible_runs_at_or_above_five_percent
test_rejects_mixed_direction_runs
test_never_accepts_compatibility_or_fallback_cells
test_records_missing_allocation_count_as_na
test_fails_when_gc_alloc_rate_norm_is_missing
```

For the acceptance test, construct two ByteArray baselines at `1000.0 B/op`
and two optimized candidates at `940.0 B/op` and `930.0 B/op`; assert verdict
`accepted` and deltas `-6.0` and `-7.0`. For the sub-threshold test use
`960.0 B/op` in both runs and assert `inconclusive`. For mixed direction use
`940.0` then `1010.0` and assert `inconclusive`. Compatibility and fallback names must assert `eligible=false` regardless of their scores.

The synthetic JMH entries must use real result keys:
`primaryMetric`, `secondaryMetrics.gc.alloc.rate.norm`,
`secondaryMetrics.gc.alloc.rate`, and `secondaryMetrics.gc.count`.

- [ ] **Step 2: Run RED**

Run:

```bash
python3 -m unittest benchmark/serializer-benchmark/scripts/test_summarize_jmh.py -v
```

Expected: FAIL because `summarize-jmh.py` is absent.

- [ ] **Step 3: Implement the bounded parser and verdict policy**

The script interface is exact:

```bash
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input docs/benchmarks/raw/issue-1039/run-20260718T120000Z/jmh.json \
  --output docs/benchmarks/raw/issue-1039/run-20260718T120000Z/summary.csv

python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py compare \
  --run docs/benchmarks/raw/issue-1039/run-20260718T120000Z/summary.csv \
  --run docs/benchmarks/raw/issue-1039/run-20260718T123000Z/summary.csv \
  --output docs/benchmarks/raw/issue-1039/comparison.csv
```

Implement constants `CLAIM_THRESHOLD_PERCENT = 5.0` and
`INELIGIBLE_TOKENS = ("Compatibility", "Fallback")`. Define bounded functions named `load_jmh`, `extract_rows`, `baseline_name`, `claim_eligible`,
`compare_runs`, and `write_csv`. `load_jmh` accepts only a JSON top-level list;
`extract_rows` reads the primary throughput metric plus the three named GC secondary metrics; `baseline_name` maps `Optimized`, `OptimizedHeap`,
`Compatibility`, `Fallback`, `FallbackDirect`, and `FallbackReadOnly` suffixes to the same backend/direction `ByteArray` method; `claim_eligible` requires
`Optimized` and rejects both ineligible tokens; `compare_runs` requires exactly two unique run files and matching baselines; `write_csv` uses a fixed column order and UTF-8 with newline control.

The parser exits non-zero when raw JSON is malformed, the normalized allocation metric is missing, run count is not exactly two for comparison, or a candidate lacks a matching ByteArray baseline. It writes `eligible`, `run_1_delta_pct`,
`run_2_delta_pct`, and `verdict` columns; accepted verdict requires both deltas to be `<= -5.0`.

- [ ] **Step 4: Run GREEN and syntax checks**

Run:

```bash
python3 -m unittest benchmark/serializer-benchmark/scripts/test_summarize_jmh.py -v
python3 -m py_compile \
  benchmark/serializer-benchmark/scripts/summarize-jmh.py \
  benchmark/serializer-benchmark/scripts/test_summarize_jmh.py
```

Expected: all seven tests PASS; compilation exits 0.

- [ ] **Step 5: Commit evidence tooling**

```bash
git add benchmark/serializer-benchmark/scripts
git commit -m "Make allocation claims fail closed across two fresh runs" \
  -m "Constraint: Positive claims require gc.alloc.rate.norm and two independent deltas of at least five percent" \
  -m "Rejected: Infer claims manually from throughput tables | manual comparison is not reproducible or fail closed" \
  -m "Confidence: high" \
  -m "Scope-risk: moderate" \
  -m "Directive: Missing B/op, missing baselines, fallback labels, or mixed direction must remain inconclusive" \
  -m "Tested: Python unittest and py_compile" \
  -m "Not-tested: Real JMH JSON is generated after smoke validation"
```

### Task 7: Compile And Smoke The Exact Benchmark Matrix

**Complexity:** Medium **Depends on:** Tasks 3-6 **Write scope:** no production writes; temporary build output only
**Heavy-command limit:** one benchmark process at a time
**Rollback/Rerun:** A compile, cell-count, semantic, profiler, or parser failure blocks evidence generation; return to the owning task and rerun the full smoke.

- [ ] **Step 1: Run module tests and benchmark compilation**

Run sequentially:

```bash
./gradlew :serializer-benchmark:test --no-configuration-cache
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
./gradlew :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache
```

Expected: all commands PASS.

- [ ] **Step 2: Resolve exactly one generated JMH jar**

Run:

```zsh
jmh_jars=(benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar(N))
(( ${#jmh_jars} == 1 ))
print -r -- "$jmh_jars[1]"
```

Expected: exactly one jar path.

- [ ] **Step 3: Run the smoke protocol**

Run:

```bash
JMH_JAR=$(find benchmark/serializer-benchmark/build/benchmarks/benchmark/jars -type f -name '*-JMH.jar' -print -quit)
java -jar "$JMH_JAR" '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc \
  -rf json -rff /tmp/issue-1039-jmh-smoke.json
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input /tmp/issue-1039-jmh-smoke.json \
  --output /tmp/issue-1039-jmh-smoke.csv
```

Expected: every declared cell completes, JSON parses, and every row contains
`gc.alloc.rate.norm`. Any overflow, decode mismatch, missing cell, or profiler failure blocks evidence runs.

- [ ] **Step 4: Confirm the smoke artifact contains the exact method counts**

Run:

```bash
python3 - <<'PY'
import json
from pathlib import Path
entries = json.loads(Path('/tmp/issue-1039-jmh-smoke.json').read_text())
names = {entry['benchmark'].rsplit('.', 1)[-1] for entry in entries}
expected = 16 + 18 + 6
assert len(names) == expected, (len(names), expected, sorted(names))
print(f'benchmark_cells={len(names)}')
PY
```

Expected: `benchmark_cells=40`.

### Task 8: Produce Two Fresh Allocation Evidence Runs

**Complexity:** High **Depends on:** Task 7 **Write scope:** `docs/benchmarks/raw/issue-1039/`
**Heavy-command limit:** exactly one JMH process at a time; no concurrent Gradle or benchmark execution
**Rollback/Rerun:** Preserve an invalid run directory for diagnosis, mark it invalid in its environment metadata, and create a new run ID; never overwrite or count an invalid run among the two accepted fresh runs.

- [ ] **Step 1: Build the evidence jar from a clean benchmark output**

Run:

```bash
./gradlew :serializer-benchmark:clean :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache
JMH_JAR=$(find benchmark/serializer-benchmark/build/benchmarks/benchmark/jars -type f -name '*-JMH.jar' -print -quit)
test -f "$JMH_JAR"
git status --short
```

Expected: build PASS; status contains only planned source/doc changes and no unexpected generated file.

- [ ] **Step 2: Capture run 1 environment and raw JSON**

Run sequentially in `zsh`:

```bash
RUN_1="run-$(date -u +%Y%m%dT%H%M%SZ)"
RUN_1_DIR="docs/benchmarks/raw/issue-1039/$RUN_1"
JMH_JAR=$(find benchmark/serializer-benchmark/build/benchmarks/benchmark/jars -type f -name '*-JMH.jar' -print -quit)
test -f "$JMH_JAR"
mkdir -p "$RUN_1_DIR"
{
  echo "run_id=$RUN_1"
  echo "commit=$(git rev-parse HEAD)"
  sw_vers
  uname -a
  sysctl -n machdep.cpu.brand_string 2>/dev/null || true
  sysctl -n hw.memsize 2>/dev/null || true
  java -version 2>&1
  ./gradlew --version
  echo "jmh=-t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json"
} > "$RUN_1_DIR/environment.txt"
java -jar "$JMH_JAR" '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc \
  -rf json -rff "$RUN_1_DIR/jmh.json"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input "$RUN_1_DIR/jmh.json" --output "$RUN_1_DIR/summary.csv"
printf '%s\n' "$RUN_1" > /tmp/issue-1039-run-1-id
```

Expected: JMH exits 0 and the summarizer writes 40 rows.

- [ ] **Step 3: Capture independent run 2**

After run 1 finishes, execute the same command with a new timestamp:

```bash
RUN_2="run-$(date -u +%Y%m%dT%H%M%SZ)"
RUN_2_DIR="docs/benchmarks/raw/issue-1039/$RUN_2"
JMH_JAR=$(find benchmark/serializer-benchmark/build/benchmarks/benchmark/jars -type f -name '*-JMH.jar' -print -quit)
test -f "$JMH_JAR"
mkdir -p "$RUN_2_DIR"
{
  echo "run_id=$RUN_2"
  echo "commit=$(git rev-parse HEAD)"
  sw_vers
  uname -a
  sysctl -n machdep.cpu.brand_string 2>/dev/null || true
  sysctl -n hw.memsize 2>/dev/null || true
  java -version 2>&1
  ./gradlew --version
  echo "jmh=-t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json"
} > "$RUN_2_DIR/environment.txt"
java -jar "$JMH_JAR" '.*SerializerAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc \
  -rf json -rff "$RUN_2_DIR/jmh.json"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input "$RUN_2_DIR/jmh.json" --output "$RUN_2_DIR/summary.csv"
printf '%s\n' "$RUN_2" > /tmp/issue-1039-run-2-id
```

Expected: JMH exits 0, the summarizer writes 40 rows, and `RUN_2 != RUN_1`.

- [ ] **Step 4: Derive the two-run comparison**

Run:

```bash
RUN_1=$(cat /tmp/issue-1039-run-1-id)
RUN_2=$(cat /tmp/issue-1039-run-2-id)
test "$RUN_1" != "$RUN_2"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py compare \
  --run "docs/benchmarks/raw/issue-1039/$RUN_1/summary.csv" \
  --run "docs/benchmarks/raw/issue-1039/$RUN_2/summary.csv" \
  --output docs/benchmarks/raw/issue-1039/comparison.csv
```

Expected: PASS. Only rows marked `accepted` may support allocation-reduction prose.

- [ ] **Step 5: Validate raw artifact size and reviewability**

Run:

```bash
find docs/benchmarks/raw/issue-1039 -type f -print -exec wc -c {} \;
test -z "$(find docs/benchmarks/raw/issue-1039 -type f -size +2M -print)"
```

Expected: every artifact is at most 2 MiB. If raw JSON exceeds the limit, retain compact JSON/CSV plus a documented external artifact link; do not commit noisy profiler dumps.

### Task 9: Publish Evidence Without Broadening Claims

**Complexity:** High **Depends on:** Task 8 **Write
scope:** benchmark READMEs, benchmark docs/raw, KDoc, module README pairs, changelog **Required
skill:** `bluetape-writer`
**Rollback/Rerun:** If prose exceeds comparison evidence, downgrade it to inconclusive or ergonomic-only wording and rerun locale and claim-parity checks.

**Files:**

- Create: `benchmark/serializer-benchmark/README.md`
- Create: `benchmark/serializer-benchmark/README.ko.md`
- Create: `docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md`
- Modify: all KDoc, README, index, changelog, and raw paths listed in the file map.

- [ ] **Step 1: Write a failing documentation contract check**

Before documentation edits, run:

```bash
for readme in \
  benchmark/serializer-benchmark/README.md \
  benchmark/serializer-benchmark/README.ko.md; do
  test -f "$readme"
done
```

Expected: FAIL because benchmark README files do not exist.

- [ ] **Step 2: Write the benchmark report from committed comparison rows**

The report must contain these exact headings:

```markdown
# ByteBuffer Serializer Allocation Benchmark - 2026-07-18

## Scope
## Commands
## Run Conditions
## Raw Artifacts
## Allocation Results
## Diagnostic Throughput
## Claim Decisions
## Optimized And Fallback Matrix
## Limitations
## Follow-Up
```

Copy no number by hand from console output. Derive tables from committed
`summary.csv` and `comparison.csv`. Name both run IDs and state that only
`accepted` rows support reduction wording. Record allocations/op as `N/A` when the profiler does not expose a stable count. Mark charts `Not produced` because tables and raw JSON are the numeric source of truth.

- [ ] **Step 3: Write benchmark module README parity**

Both README files must include:

- purpose and non-goals;
- exact compile, jar, smoke, and evidence commands;
- the 40-cell backend/path matrix;
- `gc.alloc.rate.norm` as primary and throughput as diagnostic;
- two-run and 5% claim rule;
- fallback `ergonomic-only` label;
- caller-owned preallocated direct-buffer guidance;
- link to the central benchmark report;
- explicit #755-#758 deferral.

Do not duplicate result tables in both locales.

- [ ] **Step 4: Update representative module README pairs and public KDoc**

For every file listed in the file map, document the actual comparison verdict. Each README pair must include position, limit, overflow, rollback, Java/Kotlin usage, optimized/fallback table, evidence link, and limitations. KDoc links to the report but keeps API behavior authoritative.

Rules:

```text
accepted comparison row -> may say measured lower allocation on the named cell
inconclusive row -> say measured result was inconclusive; no reduction claim
compatibility/fallback row -> ergonomic-only; never say lower allocation
unmeasured backend variant -> no allocation claim
```

- [ ] **Step 5: Update benchmark index and changelog**

Add the report to `docs/benchmarks/README.md`. Add a `1.12.0` unreleased section to `CHANGELOG.md` only if one does not already exist; otherwise append to its performance/documentation section. Mention issue #1039 and the evidence link, not a copied numeric table.

- [ ] **Step 6: Validate locale and claim parity**

Run:

```bash
for module in io/io io/jackson2 io/jackson3 io/fastjson2 io/avro benchmark/serializer-benchmark; do
  test -f "$module/README.md"
  test -f "$module/README.ko.md"
  rg -qi 'ByteBuffer' "$module/README.md"
  rg -q 'ByteBuffer' "$module/README.ko.md"
  rg -qi 'fallback|compatibility' "$module/README.md"
  rg -q 'fallback|호환' "$module/README.ko.md"
  rg -qi 'allocation|evidence|benchmark' "$module/README.md"
  rg -q '할당|근거|벤치마크' "$module/README.ko.md"
done
rg -n '#755|#756|#757|#758' benchmark/serializer-benchmark/README.md benchmark/serializer-benchmark/README.ko.md docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md
```

Expected: every command PASS.

- [ ] **Step 7: Commit evidence and documentation**

```bash
git add \
  benchmark/serializer-benchmark/README.md \
  benchmark/serializer-benchmark/README.ko.md \
  docs/benchmarks/README.md \
  docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md \
  docs/benchmarks/raw/issue-1039 \
  CHANGELOG.md \
  io/io/README.md io/io/README.ko.md \
  io/jackson2/README.md io/jackson2/README.ko.md \
  io/jackson3/README.md io/jackson3/README.ko.md \
  io/fastjson2/README.md io/fastjson2/README.ko.md \
  io/avro/README.md io/avro/README.ko.md \
  io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializer.kt \
  io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt \
  io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializer.kt \
  io/io/src/main/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializer.kt \
  io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt \
  io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt \
  io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt \
  io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt \
  io/avro/src/main/kotlin/io/bluetape4k/avro/AvroReflectSerializer.kt \
  io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializer.kt
git commit -m "Document only serializer allocation reductions the evidence supports" \
  -m "Constraint: Public claims must match two fresh B/op runs and actual backend dispatch" \
  -m "Rejected: Copy full numeric tables into every README | duplicated numbers would drift from the benchmark report" \
  -m "Confidence: high" \
  -m "Scope-risk: broad" \
  -m "Directive: Keep fallback cells ergonomic-only and link all measured numbers to the central report" \
  -m "Tested: two fresh JMH GC runs, evidence summarizer, and EN/KO parity checks" \
  -m "Not-tested: Full proportional build runs in the next gate"
```

### Task 10: Run Proportional Verification And Repository Hazards

**Complexity:** High **Depends on:** Task 9 **Write scope:** no intended writes **Heavy-command
limit:** sequential Gradle invocations; no parallel benchmark or Testcontainers process
**Rollback/Rerun:** A failed gate returns to the task that owns the failing file or evidence; rerun that targeted gate, then repeat all Task 10 checks.

- [ ] **Step 1: Run benchmark and parser tests**

```bash
./gradlew :serializer-benchmark:test :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache
python3 -m unittest benchmark/serializer-benchmark/scripts/test_summarize_jmh.py -v
```

Expected: PASS.

- [ ] **Step 2: Run all affected module tests sequentially**

```bash
./gradlew :bluetape4k-io:test --no-configuration-cache
./gradlew :bluetape4k-json:test --no-configuration-cache
./gradlew :bluetape4k-jackson2:test --no-configuration-cache
./gradlew :bluetape4k-jackson3:test --no-configuration-cache
./gradlew :bluetape4k-fastjson2:test --no-configuration-cache
./gradlew :bluetape4k-avro:test --no-configuration-cache
```

Expected: every command PASS. Investigate any retry-only pass as a lifecycle or environment signal rather than erasing the first failure.

- [ ] **Step 3: Run serializer ABI proof**

```bash
bash scripts/check-serializer-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
```

Expected: PASS with current head recorded.

- [ ] **Step 4: Verify module registration and exclusion hazards**

```bash
./gradlew projects --no-configuration-cache
./gradlew :serializer-benchmark:tasks --all --no-configuration-cache
rg -n 'serializer-benchmark' README.md README.ko.md settings.gradle.kts build.gradle.kts .github/workflows || true
```

Inspect the result. Add root module-map or workflow path updates only when the existing generated/explicit registration chain requires them. Do not add a BOM constraint or publication job for the non-published benchmark.

- [ ] **Step 5: Run static and proportional build checks**

```bash
./gradlew \
  :serializer-benchmark:build \
  :bluetape4k-io:detekt \
  :bluetape4k-json:detekt \
  :bluetape4k-jackson2:detekt \
  :bluetape4k-jackson3:detekt \
  :bluetape4k-fastjson2:detekt \
  :bluetape4k-avro:detekt \
  --no-configuration-cache
./gradlew build -x test --no-configuration-cache
git diff --check origin/develop...HEAD
```

Expected: PASS with no new warnings attributable to changed files.

- [ ] **Step 6: Verify spec and claim traceability**

Check every `accepted` comparison row against every occurrence of allocation claim language:

```bash
rg -n -i 'lower allocation|reduced allocation|allocation reduction|할당.*감소|할당.*절감' \
  benchmark/serializer-benchmark \
  docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md \
  io/io io/jackson2 io/jackson3 io/fastjson2 io/avro CHANGELOG.md
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py compare \
  --run "docs/benchmarks/raw/issue-1039/$(cat /tmp/issue-1039-run-1-id)/summary.csv" \
  --run "docs/benchmarks/raw/issue-1039/$(cat /tmp/issue-1039-run-2-id)/summary.csv" \
  --output /tmp/issue-1039-comparison-recheck.csv
cmp docs/benchmarks/raw/issue-1039/comparison.csv /tmp/issue-1039-comparison-recheck.csv
```

Expected: every positive phrase maps to an `accepted` row and regenerated CSV is byte-identical.

### Task 11: Converge Review And Commit The Durable Lesson

**Complexity:** Medium **Depends on:** Task 10 **Write
scope:** `docs/lessons/2026-07-18-issue-1039-serializer-allocation-proof.md` and in-scope blocker fixes only
**Rollback/Rerun:** Any P0/P1 finding returns to the owning implementation task; after the fix, rerun affected verification and all six review lenses.

**Files:**

- Create: `docs/lessons/2026-07-18-issue-1039-serializer-allocation-proof.md`

- [ ] **Step 1: Run the Type A performance/stability scan and six review lenses**

Review the exact `origin/develop...HEAD` diff from performance, stability, security, Ops, developer/API, and user/caller perspectives. Normalize findings to P0/P1/P2/P3. P0/P1 blocks progression; fix and rerun affected commands and lenses. P2/P3 is fixed when cheap and in scope or recorded with a durable follow-up rationale.

- [ ] **Step 2: Write the lesson**

The lesson contains:

```markdown
# Issue #1039 Serializer Allocation Proof

## Context
## Decision
## What The Measurements Proved
## What They Did Not Prove
## Failure Or Surprise
## Verification Evidence
## Review Misses
## Future Guard
```

Record the exact two run IDs, accepted/inconclusive count, any profiler limitation, and the rule future changes must preserve.

- [ ] **Step 3: Commit the lesson and any converged fixes**

```bash
git add docs/lessons/2026-07-18-issue-1039-serializer-allocation-proof.md
git status --short
# Inspect every changed path, then stage only exact reviewed in-scope blocker fixes.
# Example: git add path/to/exact-reviewed-file
git diff --cached --check
git commit -m "Preserve the boundary between measured and fallback serializer claims" \
  -m "Constraint: Future documentation must remain reproducible from committed raw allocation evidence" \
  -m "Rejected: Treat a green benchmark run as sufficient | claim eligibility also requires functional and review gates" \
  -m "Confidence: high" \
  -m "Scope-risk: narrow" \
  -m "Directive: Re-run two fresh B/op measurements whenever optimized dispatch or benchmark payload changes" \
  -m "Tested: full issue #1039 verification matrix and P0/P1 convergence" \
  -m "Not-tested: GitHub CI runs after exact-head push"
```

- [ ] **Step 4: Confirm the converged local head**

```bash
git status --short --branch
git log -1 --format=full
git diff --stat origin/develop...HEAD
```

Expected: clean branch, Lore-compliant head, intended files only, P0=0, P1=0.

### Task 12: Deliver The Exact-Head Pull Request And Stop Before Merge

**Complexity:** Medium **Depends on:** Task 11 and common gates CG-01 through CG-10 **External side
effects:** push branch and create/update PR are authorized by the approved design; merge is not
**Rollback/Rerun:** A push, CI, metadata, or review failure returns to the owning task and exact-head verification; never merge, auto-merge, or delete the branch.

- [ ] **Step 1: Refresh issue metadata and PR authority**

```bash
gh issue view 1039 --repo bluetape4k/bluetape4k-projects \
  --json number,title,state,assignees,labels,milestone,url
git status --short --branch
git rev-parse HEAD
```

Expected: issue OPEN, milestone `1.12.0`, assignee `debop`, labels include
`enhancement`, `performance`, and `infra/io`; branch is clean.

- [ ] **Step 2: Push and verify exact remote head**

```bash
git push -u origin feat/issue-754-allocation-proof
LOCAL_HEAD=$(git rev-parse HEAD)
REMOTE_HEAD=$(git ls-remote --heads origin feat/issue-754-allocation-proof | awk '{print $1}')
test "$LOCAL_HEAD" = "$REMOTE_HEAD"
```

Expected: local and remote SHAs match exactly.

- [ ] **Step 3: Render the issue-linked PR body from current evidence**

Read the exact run IDs, local head, comparison verdict counts, and verification results. Create `/tmp/issue-1039-pr-body.md` with `apply_patch`; do not use shell redirection. The English body must contain these sections and no unresolved placeholder:

```markdown
## Why
## What
## Allocation Evidence
## Verification
## Scope Boundaries
Closes #1039
## DoD Status
```

The final Markdown heading must be `## DoD Status`. Include both fresh run IDs, accepted/inconclusive counts, exact head SHA, and the actual commands that passed.

Validate before PR creation:

```bash
RUN_1=$(cat /tmp/issue-1039-run-1-id)
RUN_2=$(cat /tmp/issue-1039-run-2-id)
HEAD_SHA=$(git rev-parse HEAD)
test -n "$RUN_1" && test -n "$RUN_2" && test "$RUN_1" != "$RUN_2"
rg -q "Closes #1039" /tmp/issue-1039-pr-body.md
test "$(rg '^## ' /tmp/issue-1039-pr-body.md | tail -1)" = "## DoD Status"
! rg -n 'TBD|TODO|FIXME|<[^>]+>' /tmp/issue-1039-pr-body.md
rg -q "$RUN_1" /tmp/issue-1039-pr-body.md
rg -q "$RUN_2" /tmp/issue-1039-pr-body.md
rg -q "$HEAD_SHA" /tmp/issue-1039-pr-body.md
```

- [ ] **Step 4: Create the issue-linked PR**

Create an English PR with base `develop`, head
`feat/issue-754-allocation-proof`, assignee `debop`, milestone `1.12.0`, and labels `enhancement`, `performance`, `infra/io`. The body explains why/what, lists both fresh run IDs and verification commands, closes #1039, and ends with the final heading `## DoD Status`.

```bash
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-754-allocation-proof \
  --title "Prove ByteBuffer serializer allocation reductions" \
  --assignee debop \
  --milestone 1.12.0 \
  --label enhancement \
  --label performance \
  --label infra/io \
  --body-file /tmp/issue-1039-pr-body.md
```

- [ ] **Step 5: Verify live PR metadata and body**

```bash
PR=$(gh pr view --repo bluetape4k/bluetape4k-projects --json number --jq .number)
gh pr view "$PR" --repo bluetape4k/bluetape4k-projects \
  --json number,url,state,headRefName,headRefOid,baseRefName,assignees,labels,milestone,body
```

Expected: metadata matches and the last Markdown `##` heading is exactly
`## DoD Status`.

- [ ] **Step 6: Wait for exact-head CI and reread reviews**

Use `ci-status` when available, otherwise:

```bash
gh pr checks "$PR" --repo bluetape4k/bluetape4k-projects --watch
gh pr view "$PR" --repo bluetape4k/bluetape4k-projects \
  --json headRefOid,mergeable,mergeStateStatus,reviews,reviewDecision,statusCheckRollup
gh api "repos/bluetape4k/bluetape4k-projects/pulls/$PR/comments"
```

Expected: required checks succeed on the exact remote head and no unresolved P0/P1 review blocker remains. A failure returns to the owning implementation task and reruns affected verification.

- [ ] **Step 7: Report merge readiness and stop**

Render the Type A and common-gate DoD with exact PR URL, head SHA, two run IDs, accepted/inconclusive claim counts, tests, ABI, Detekt, build, lesson, CI, and review evidence. Leave CG-16 through CG-18 pending and request a fresh merge decision. Do not enable auto-merge or run `gh pr merge`.

## Plan Completion Criteria

- Every acceptance criterion in the approved spec maps to a task and fresh command.
- The implementation contains no production serializer behavior change.
- The benchmark module is registered, non-published, and excluded from Kover.
- All 40 cells smoke successfully and both evidence runs complete sequentially.
- Positive claims are reproduced by the tested two-run, 5% B/op comparator.
- Fallback, unmeasured, and inconclusive cells contain no reduction wording.
- Affected tests, ABI, locale parity, Detekt, proportional build, P0/P1 review, lesson, exact-head PR, CI, and live review gates pass.
- The workflow stops at merge-ready state until fresh explicit approval.
