# Issue #757 Lettuce Protobuf Buffer Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** uncompressed strict/trusted Lettuce Protobuf factory가 caller-owned `ByteBuf`에 bounded absolute-index 방식으로 직접 기록하도록 하되, 기존 wire/security/failure/ABI 계약을 유지하고 두 번의 fresh JMH evidence로 유지 또는 제거 결정을 내린다.

**Architecture:** generic `LettuceBinaryCodec`에는 source-level extension point 하나만 열고, 그에 따라 활성화되는 세 compiler bridge의 JVM overrideability를 additive ABI cost로 명시한다. ABI gate는 immutable authority baseline과 clean detached candidate measurement-source에서 같은 toolchain으로 만든 paired `javap -p -s`/`javap -p -v` raw output을 payload manifest 하나로 검증한다. `LettuceProtobufCodecs` 내부 private subtype은 `ProtoAny.pack` 결과를 bounded `OutputStream` adapter와 `CodedOutputStream`으로 target에 기록한다. 성공 후에만 `writerIndex`를 한 번 commit하며 NIO view, retain/release, payload-sized final `ByteArray` handoff를 사용하지 않는다. benchmark와 evidence validator는 heap/direct copied/optimized matrix를 검증하고 `retained-accepted`, `retained-inconclusive`, `rejected-after-regression` 중 하나를 결정한다. tracked payload/root는 measurement-source만 bind하고 final head는 external verifier가 확인하는 non-cyclic DAG를 사용한다.

**Tech Stack:** Kotlin 2.3, Java 21, Gradle, Lettuce 7.6, Netty 4.2, Protobuf Java 4.35, JUnit 5, MockK, Testcontainers, kotlinx-benchmark/JMH, Python `unittest`.

---

## 0. 실행 계약

- **승인 기준 명세:** `docs/superpowers/specs/2026-07-20-issue-757-lettuce-protobuf-buffer-design.md`
- **작업 위치:** `.worktrees/feat-issue-757-lettuce-protobuf-buffer`
- **브랜치:** `feat/issue-757-lettuce-protobuf-buffer`
- **PR target:** `bluetape4k/bluetape4k-projects`, base `develop`, head `feat/issue-757-lettuce-protobuf-buffer`
- **범위:** `infra/lettuce`, `io/protobuf`, 기존 `benchmark/protobuf-codec-benchmark`, issue #757 evidence/docs만 변경한다.
- **금지:** 새 module, external/production dependency, generic serializer SPI, compressed/custom-prefix/decode optimization, settings/catalog/release/tag/publish 변경.
- **허용 dependency:** benchmark module의 existing project `:bluetape4k-lettuce`에 대한 `implementation` dependency만 추가한다.
- **테스트 순서:** unit/compile은 병렬화할 수 있지만 Redis Testcontainers와 canonical JMH는 다른 container/heavy work와 병렬 실행하지 않는다.
- **shell fail-closed:** 각 `bash` fence는 독립된 단일 `bash` process에서 실행하며 첫 줄
  `set -euo pipefail`을 필수로 한다. 앞선 실패를 후속 성공으로 덮지 않는다. 반드시 수행할
  cleanup은 다음 command에 의존하지 않고 status-preserving helper `finally`에서 실행하며 원래
  exit/signal status를 재전파하고 cleanup failure는 failure로 승격한다.
- **커밋:** 각 task는 작은 Lore commit으로 끝낸다. intent line은 why를 쓰고 `Constraint`, `Rejected`, `Confidence`, `Scope-risk`, `Directive`, `Tested`, `Not-tested` trailer를 실제 결과에 맞게 기록한다.
- **중단점:** Task 12에서 PR을 exact-head merge-ready로 보고한 뒤 fresh merge 승인을 기다린다. merge, release, publish, tag, branch/worktree 삭제는 이 계획의 자동 실행 범위가 아니다.

## 1. Task map

| Task | 복잡도 | 의존 | 독립 write scope | 핵심 검증 | terminal/rollback |
|---|---:|---|---|---|---|
| 1. ABI extension point | H | 없음 | `infra/lettuce` codec/tests, shared detached-root helper | manifest-bound paired `javap`, transition/compile-negative fixtures | rejected면 canonical rollback |
| 2. bounded writer | H | 1 | `LettuceProtobufCodecs.kt`, new unit test | heap/direct/composite/wrapped/failure/resource | rejected면 subtype/test 제거 |
| 3. factory/security/integration | H | 2 | existing Protobuf Lettuce tests | strict/trusted/fallback/wire/Redis | retained/rejected 공통 compatibility |
| 4. ABI/source compatibility proof | H | 2-3 | Java/Kotlin fixtures, evidence commands | descriptor/raw flags/reflection/source compile | 허용 diff 외 blocker |
| 5. Lettuce JMH matrix | H | 3 | benchmark Kotlin/Gradle/tests | four cells, semantic/reset/ownership | candidate measurement 준비 |
| 6. fail-closed verdict | H | 5 | validator와 Python tests | score/error/unit/matrix/terminal | deterministic precedence |
| 7. archive-aware evidence | H | 6 | runner와 Python tests | fenced immutable generation, manifest DAG | active pointer만 교체 |
| 8. candidate exact-head gate | H | 1-7 | 검증/commit only | clean detached ABI/JAR/test receipt | immutable measurement-source |
| 9. two-run measurement | H | 8 | build evidence only | two sequential canonical runs | terminal 선택 |
| 10. terminal finalization | H | 9 | source/tests/rollback contract | retained keep 또는 rejected rollback-source | pre-release source shape 결정 |
| 11. promotion/docs | H | 10 | evidence/report/docs/KDoc/changelog | immutable generation/pointer/report parity | non-closing rejected 지원 |
| 12. final review/PR | H | 11 | lesson/review/PR metadata | external exact-head DAG, six-lens review | merge approval에서 정지 |

## Task 1: 최소 ABI extension point를 TDD로 연다

**Files:**

- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecTargetTest.kt`
- Create: `infra/lettuce/src/test/java/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecJavaCompatibilityFixture.java`
- Create: `infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py`
- Create: `infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py`
- Create: `benchmark/protobuf-codec-benchmark/scripts/issue757_detached_roots.py`
- Create: `benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py`

**Skills:** `test-driven-development`, `bluetape-kotlin-patterns`.

초기 extension implementation과 validator는 이미 commits `d1ab81f40`, `9442a9f60`,
`31c867bdd`에 존재한다. 이 task는 그 이력을 처음부터 다시 쓰지 않는다. 승인된 compiler
bridge 명세에 맞춰 기존 test를 먼저 실패하게 확장하고, 구현과 evidence contract를
수정한 뒤 Task 1 spec/code-quality review를 다시 수렴시킨다.

- [ ] **Step 1: manifest/transition/compile-negative RED를 기존 test에 추가한다.**

validator는 CLI에서 baseline/candidate raw file 네 개를 따로 받지 않고
`--manifest PATH` 하나만 받는다. fixture payload는 role별 commit/tree, distinct canonical
checkout/build root, pre/post clean receipt, FQCN, standalone class/JAR/exact JAR entry,
structural/verbose raw path/hash, classfile generation 전후 hash와 pinned Kotlin/Gradle/JDK/
`$JAVA_HOME/bin/java`/`$JAVA_HOME/bin/javap` path·version·SHA-256을 모두 포함한다.

RED matrix는 다음 exact transition과 all-other exact equality를 한 field씩 변형한다.

| target | baseline | retained candidate |
|---|---|---|
| class | `0x0031 ACC_PUBLIC, ACC_FINAL, ACC_SUPER` | `0x0021 ACC_PUBLIC, ACC_SUPER` |
| ordinary source method 7개 | 각각 `0x0001 ACC_PUBLIC` | 각각 `0x0011 ACC_PUBLIC, ACC_FINAL` |
| target `encodeValue(Object, ByteBuf)` | `0x0001 ACC_PUBLIC` | `0x0001 ACC_PUBLIC` |
| compiler bridge 3개 | 각각 `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | 각각 `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` |

wrong baseline commit/tree(`4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88`/
`086f83baa7eec0cd68e68fff132542ef6db0f200`), wrong role/root/hash/toolchain,
shared/dirty/stale checkout, missing/duplicate/wrong-path JAR entry, same-classfile mismatch,
truncated raw input, class/ordinary/target/bridge의 baseline 또는 candidate flag mutation,
bridge `ACC_PUBLIC`/`ACC_BRIDGE`/`ACC_SYNTHETIC` 제거, unexpected `ACC_FINAL`/extra flag,
fourth bridge/member와 descriptor/access drift를 모두 non-zero로 고정한다. `rejected`는 raw
member final flag를 포함한 immutable baseline exact equality만 허용한다. payload ->
class/JAR/raw, result -> payload, tracked root -> payload/result 방향만 허용하고 payload의
result/root/final-head 참조와 root self-hash를 거부한다.

별도 compile-negative fixture는 Kotlin/Java subclass가 source key method
`encodeKey(String)`, `encodeKey(String, ByteBuf)`, `decodeKey(ByteBuffer)`와 JVM에서 보이는
erased/raw/generic bridge signature `encodeKey(Object)`, `encodeKey(Object, ByteBuf)`,
`decodeKey(ByteBuffer): Object`를 각각 override하려는 source를 독립 compilation unit으로
컴파일한다. 모든 case가 실패해야 하며 하나라도 성공하면 spec을 reopen한다. 기존 caller와
target `encodeValue(V, ByteBuf?)` override positive fixture는 계속 성공해야 한다.

```bash
set -euo pipefail
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py
./gradlew :bluetape4k-lettuce:test \
  --tests "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest" \
  --rerun-tasks --no-configuration-cache
```

Expected: 기존 two-file validator 또는 잘못된 bridge 가정 때문에 새 fixture가 실패한다.

- [ ] **Step 2: manifest-only validator와 exact raw flag parser를 GREEN으로 만든다.**

structural normalization은 encoding/line ending/비의미적 whitespace/member order만
canonicalize하고 class/member kind, FQCN/name/descriptor/access/finality를 보존한다. verbose
normalization은 body noise만 제외하고 numeric access bitmask 전체를 보존한다. capture 단계는
raw output byte stream을 수정하지 않고 hash하며, validate 단계는 manifest path/hash/role,
authority, toolchain, clean detached root, JAR-entry/classfile와 generation 전후 same-classfile
binding을 검증한 뒤 mode별 transition을 판정한다.

```bash
set -euo pipefail
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py
```

- [ ] **Step 3: Kotlin source/KDoc를 exact retained ABI에 맞춘다.**

class와 target overload는 `open`을 유지한다. target 외 ordinary source method 일곱 개는
명시적 `final`로 잠그고 public constructor/property/descriptors를 보존한다. class KDoc는
지원 source extension seam이 target overload 하나뿐이며 세 synthetic bridge가 source API는
아니지만 class 개방으로 JVM override 가능해지는 장기 ABI cost임을 기록한다. target KDoc는
null no-op, caller ownership, success-only index commit, failure propagation과 subclass
compatibility 책임을 영어로 기록한다. 실패 시 `writerIndex`는 commit되지 않지만 capacity는
이미 증가했고 attempted absolute range가 부분 수정됐을 수 있으므로 caller가 그 range를
clear/reinitialize하거나 buffer를 폐기해야 하며 안전한 재사용을 가정하면 안 된다는
failure-aftercare를 명시한다. Task 11의 source-to-doc validator가 KDoc contract를 기계적으로
찾을 수 있도록 class/target KDoc에는 exact English terms `sole supported source extension seam`,
`synthetic bridge`, `caller-owned`, `success-only`, `writerIndex`, `capacity`, `attempted range`,
`clear or reinitialize`, `discard`를 포함한다.

- [ ] **Step 4: Kotlin/Java positive와 compile-negative GREEN을 확인한다.**

```bash
set -euo pipefail
./gradlew :bluetape4k-lettuce:test \
  --tests "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest" \
  --rerun-tasks --no-configuration-cache
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py
```

- [ ] **Step 5: Task 1 amendment Lore commit을 만든다.** Intent:
`Bind the Lettuce extension ABI to compiler bridge reality`. 기존 commits를 squash하거나
가짜 RED/GREEN history로 재작성하지 않는다.

- [ ] **Step 6: authority baseline과 committed candidate를 서로 다른 clean detached root에서 capture한다.**

```bash
set -euo pipefail
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py capture-paired \
  --repository-root "$PWD" \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --baseline-revision 4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88 \
  --baseline-tree 086f83baa7eec0cd68e68fff132542ef6db0f200 \
  --candidate-revision HEAD \
  --mode retained \
  --selection-state .omx/evidence/issue-757-lettuce/abi-selection.json
ISSUE757_ABI_MANIFEST=$(python3 \
  infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py selected-manifest \
  --selection-state .omx/evidence/issue-757-lettuce/abi-selection.json)
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py validate \
  --manifest "$ISSUE757_ABI_MANIFEST"
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py cleanup-detached \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --selection-state .omx/evidence/issue-757-lettuce/abi-selection.json
```

capture는 각 role에 exact `$JAVA_HOME/bin/javap`로 paired `-p -s`/`-p -v`를 실행하고
`baseline.struct.txt`, `candidate.struct.txt`, `baseline.verbose.txt`,
`candidate.verbose.txt` raw bytes와 hash를 보존한다. Expected: 위 numeric transition만
허용되고 payload/result DAG에 final delivery head는 없다. manifest와 raw `javap` files는
registered worktree 밖의 receipt-owned evidence child에 먼저 봉인한다. `capture-paired`와 Task 7
runner는 `issue757_detached_roots.py`의 동일 primitive를 사용한다. 두 consumer는 repo root에서
exact relative path를 resolve하고 모든 ancestor non-symlink/regular-file/hash를 확인한 뒤
`importlib.util.spec_from_file_location`으로 같은 module을 load한다. module의 versioned public API는
`begin_attempt`, `materialize_worktree`, `materialize_build_root`, `seal_artifact`,
`cleanup_selected`이며 validator와 runner 양쪽에서 import/contract test를 실행한다.

primitive는 authorized evidence root 아래 existing journal directory에 immutable phase -1 intent를
no-clobber 생성·fsync한다. intent에는 attempt ID, planned unique staging-parent/final-parent path,
planned exact child paths, expected role/revision/tree, repository common-dir와 evidence-root
device+inode를 기록하고 selection state를 intent path/hash에 CAS한 뒤에야 staging parent 생성을
시도한다. staging parent에 single-link regular owner marker를 만들고 file/directory를 fsync한 뒤
staging device+inode, marker path/hash를 immutable `parent-prepared` phase로 append·CAS한다. 그
record가 selected된 뒤에만 platform atomic no-replace rename으로 staging을 planned final parent에
publish하고 evidence-root directory를 fsync한다. published parent의 동일 device+inode/marker와
final path를 phase 0으로 append·CAS한다.

intent-only recovery는 planned staging/final path가 없으면 no-op receipt를 남긴다. markerless path,
foreign empty directory, helper-marker substitution 또는 recorded inode/marker가 없는 path는 절대
삭제하지 않고 fail-closed journal/manual-recovery 대상으로 남긴다. `parent-prepared` 이후 recovery는
recorded exact inode+marker가 staging 또는 no-replace-published final path 중 정확히 한 곳에 있을
때만 cleanup 또는 phase 0 recovery-append를 수행한다. foreign final path가 있으면 rename 자체가
실패하고 그 path를 제거하지 않는다. 각 add/register 성공 직후 actual checkout device+inode, sidecar
owner record, resolved commit/tree, admin gitdir와 registry mapping을 새 phase record로 append·fsync하고
selection을 CAS한다. build root도 생성 직후 path/device+inode/sidecar owner record를 새 phase로
append한다. 이전 phase는 rewrite하지 않는다.

모든 destructive action 직전에 latest valid phase와 phase 0 parent proof를 함께 사용해 raw ancestor
non-symlink, exact containment, parent/root device+inode, sidecar marker type/link-count/hash,
role/commit/tree와 gitdir registry mapping을 다시 검증한다. add/register 직후 phase append 전 crash는
phase 0의 exact planned child, owned parent proof, expected commit/tree/common-dir와 live registry
mapping을 reconcile해 ownership이 전부 일치할 때만 다음 phase를 recovery-append한 뒤 cleanup한다.
gap/drift나 foreign mapping이면 제거하지 않고 fail-closed한다. registered root는 exact
unregister/remove 뒤 registry와 filesystem absence를 증명하고, 부분 생성된 unregistered root는
phase 0 parent proof, planned child와 actual inode/contents가 helper-owned partial shape와 일치할 때만
제거한다. 하나라도 drift하면 foreign data를 건드리지 않고 journal을 보존한다.

in-process `finally`는 원래 exit/signal status를 보존해 cleanup 성공 뒤 재전파하고 cleanup 실패는
원래 결과와 무관하게 step failure로 만든다. SIGKILL/host crash 뒤 `cleanup-detached`는 broad
scan/prune 없이 selection state가 bind한 exact journal만 읽어 같은 검사를 idempotent하게
반복한다. crash before/after add/register/unregister/remove, partial failed add, root/ancestor
replacement, wrong marker/inode/link count, foreign registered worktree, removal failure와 interrupt,
phase -1 직후 crash, staging mkdir/marker/`parent-prepared`/no-replace rename/phase 0 각 경계 crash,
foreign empty final path와 helper-marker/inode substitution
fixture는 foreign root 무변경, exact owned root 수거, registry/filesystem postcondition, 원래 status
보존과 sealed manifest/raw evidence 생존을 확인한다.

- [ ] **Step 7: Task 1 spec-compliance와 code-quality review를 분리해 재수렴한다.** 두 review
모두 최신 committed candidate와 payload manifest를 검사한다. P0/P1은 수정 후 affected
review를 반복하고 P2/P3은 수정 또는 명시적 disposition을 남긴다. 두 review가 P0=0,
P1=0이 되기 전에는 Task 2로 진행하지 않는다.

## Task 2: private bounded absolute-index writer를 TDD로 구현한다

**Files:**

- Modify: `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt`
- Create: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufByteBufCodecTest.kt`

**Skills:** `test-driven-development`, `bluetape-kotlin-patterns`.

- [ ] **Step 1: direct path RED matrix를 작성한다.**

다음 target을 parameterized fixture로 만든다.

```kotlin
enum class TargetKind { HEAP, DIRECT, COMPOSITE, WRAPPED, SLICED }

data class TargetFixture(
    val kind: TargetKind,
    val buffer: ByteBuf,
    val release: () -> Unit,
)
```

각 fixture에서 non-zero prefix, reader/writer mark, `refCnt`, exact writer delta, generic codec wire equality, 양방향 decode를 검증한다. direct path가 아직 없어 payload-sized copied path를 구별하는 reflection/private subtype assertion이 RED여야 한다.

fixture마다 allocation owner와 derived view의 release 책임을 따로 기록한다. codec call 뒤
`refCnt` 보존을 assertion하고, 이후 assertion이 실패해도 `finally`에서 owner를 정확히 한
번 release해 최종 `refCnt == 0`을 확인한다. slice/composite component를 owner와 중복
release하지 않는다.

- [ ] **Step 2: bounded adapter 실패 tests를 먼저 추가한다.**

테스트는 factory 반환 객체의 private nested class constructor와 private writer interface를
reflection으로 찾고 `Proxy`로 두 번째 parameter를 교체한다. visibility를
`internal`/public으로 넓히지 않는다. stored Kotlin function type은 primitive `Int`를
boxing하므로 사용하지 않는다.

```kotlin
val constructor = codec.javaClass.declaredConstructors.single { candidate ->
    !candidate.isSynthetic &&
        candidate.parameterCount == 2 &&
        candidate.parameterTypes[0] == ProtobufSerializer::class.java &&
        candidate.parameterTypes[1].declaredMethods.single().name == "write"
}.apply { isAccessible = true }
val writerType = constructor.parameterTypes[1]
val shortWriter = Proxy.newProxyInstance(
    writerType.classLoader,
    arrayOf(writerType),
) { proxy, method, arguments ->
    when (method.name) {
        "write" -> {
            requireNotNull(arguments)
            val target = arguments[1] as ByteBuf
            val start = arguments[2] as Int
            target.setByte(start, 1)
            1
        }
        "toString" -> "ShortPackedAnyWriter"
        "hashCode" -> System.identityHashCode(proxy)
        "equals" -> proxy === arguments?.singleOrNull()
        else -> error("Unexpected writer method: ${method.name}")
    }
}
val injected = constructor.newInstance(serializer, shortWriter) as LettuceBinaryCodec<Any>
```

포함할 failure cases:

- partial absolute write 후 exception
- `size - 1` short-success
- max capacity 부족
- read-only target의 `ReadOnlyBufferException`
- released target의 Netty reference-count exception
- null target은 mock `ProtoMessage`의 어떤 method도 호출하지 않는 no-op
- `ProtoAny.pack` 실패의 기존 `BinarySerializationException` message/cause
- `nioBufferCount()==1`이지만 `nioBuffer*` 호출 시 실패하는 spy target

partial/short/exception case는 failure 전 `writerIndex`가 유지되더라도 capacity 증가와 attempted
absolute range의 부분 변경은 rollback되지 않음을 assertion한다. 같은 buffer를 성공 payload로
재사용하려면 caller가 그 range를 명시적으로 clear/reinitialize해야 하며, 그렇지 않은 fixture는
buffer를 폐기한다. 이 failure-aftercare가 target KDoc와 README/manual에 같은 의미로 나타나는지
문서 검사에서도 확인한다.

각 실패에서 reader/writer indices, reset으로 확인 가능한 marks, `refCnt`를 검증하고 capacity/attempted bytes는 rollback assertion에서 제외한다.

- [ ] **Step 3: RED를 실행한다.**

```bash
set -euo pipefail
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest" \
  --rerun-tasks --no-configuration-cache
```

Expected: private optimized subtype/writer가 없어 실패한다.

- [ ] **Step 4: private subtype과 bounded writer를 최소 구현한다.**

세 declaration은 모두 `object LettuceProtobufCodecs` 안의 private nested declarations로
유지한다.

```kotlin
object LettuceProtobufCodecs {
    private fun interface PackedAnyWriter {
        fun write(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int
    }

    private object AbsolutePackedAnyWriter: PackedAnyWriter {
        override fun write(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int =
            writePackedAny(packed, target, start, end)
    }

    private class DirectProtobufLettuceCodec<V: Any> private constructor(
        serializer: ProtobufSerializer,
        private val writer: PackedAnyWriter = AbsolutePackedAnyWriter,
    ): LettuceBinaryCodec<V>(serializer) {
        companion object {
            fun <V: Any> create(serializer: ProtobufSerializer): LettuceBinaryCodec<V> =
                DirectProtobufLettuceCodec(serializer)
        }

        override fun encodeValue(value: V, target: ByteBuf?) {
            if (target == null) return
            if (value !is ProtoMessage) return super.encodeValue(value, target)

            val packed = try {
                ProtoAny.pack(value)
            } catch (failure: Throwable) {
                throw BinarySerializationException(
                    "Fail to serialize. graphType=${value.javaClass.name}",
                    failure,
                )
            }
            val size = packed.serializedSize
            val start = target.writerIndex()
            target.ensureWritable(size)
            val written = writer.write(packed, target, start, start + size)
            check(written == size) { "Packed Any writer wrote $written bytes, expected $size" }
            target.writerIndex(start + size)
        }
    }
}
```

`writePackedAny`는 NIO API를 호출하지 않고 bounded `OutputStream`을 사용한다.

```kotlin
private fun writePackedAny(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int {
    val output = BoundedByteBufOutputStream(target, start, end)
    val coded = CodedOutputStream.newInstance(output, 0)
    packed.writeTo(coded)
    coded.flush()
    return output.written
}
```

adapter의 `write(Int)`와 `write(ByteArray, offset, length)`는 Java array bounds와 `[start,end)`를 먼저 검사한 뒤 `setByte`/`setBytes`만 호출한다. target index, mark, ref-count를 직접 변경하거나 target/writer를 field 밖으로 escape시키지 않는다.

- [ ] **Step 5: factory dispatch를 uncompressed strict/trusted에만 연결한다.**

```kotlin
private val strictSerializer: ProtobufSerializer by lazy { ProtobufSerializer() }
private val trustedInternalSerializer: ProtobufSerializer by lazy {
    ProtobufSerializer.trustedInternalProtobuf()
}

fun <V: Any> protobuf(): LettuceBinaryCodec<V> =
    DirectProtobufLettuceCodec.create(strictSerializer)

fun <V: Any> trustedInternalProtobuf(): LettuceBinaryCodec<V> =
    DirectProtobufLettuceCodec.create(trustedInternalSerializer)
```

compressed factories는 기존 generic `LettuceBinaryCodec(CompressableBinarySerializer(...))` 그대로 둔다.
같은 task에서 factory KDoc에 uncompressed optimized target 범위, payload-sized handoff 제거,
zero-copy 비보장, compressed/custom-prefix compatibility path를 영어로 기록한다. source-to-doc
validator가 찾을 수 있도록 exact English terms `direct ByteBuf target`, `unchanged ByteBuffer`,
`caller-owned`, `failure-aftercare`, `trusted-internal`, `compressed`, `custom-prefix`를 포함한다.

- [ ] **Step 6: GREEN, static check, no-NIO scan.**

```bash
set -euo pipefail
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest" \
  --rerun-tasks --no-configuration-cache
if rg -n "nioBuffer|nioBuffers|internal class Direct|public class Direct" \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt; then
  exit 1
fi
git diff --check
```

Expected: tests PASS; production direct writer에는 NIO call과 새 public/internal seam이 없다.
`javap -c -p`로 writer invocation descriptor가 `(ProtoAny, ByteBuf, int, int)int`이며 hot call
주변에 `Integer.valueOf`/`intValue` boxing이 없음을 확인한다.

- [ ] **Step 7: Lore commit.** Intent: `Write packed Protobuf bytes without a detached buffer view`.

## Task 3: factory, security, wire, Redis compatibility를 고정한다

**Files:**

- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecsTest.kt`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufByteBufCodecTest.kt`

**Skills:** `test-driven-development`, `bluetape-kotlin-patterns`. Testcontainers는 sequential.

- [ ] **Step 1: strict/trusted/compressed factory type assertions를 추가한다.**

reflection으로 uncompressed strict/trusted 두 factory만 같은 private optimized subtype인지
확인한다. Gzip, Deflate, LZ4, Snappy, Zstd의 five compressed pairs/ten factories는 모두
generic codec인지 검증한다. private subtype에 public/protected constructor가 없고
externally accessible nested class가 없음을 확인한다.

- [ ] **Step 2: security/wire control tests를 추가한다.**

```kotlin
val oldCodec = LettuceBinaryCodec<Any>(ProtobufSerializer())
val newCodec = LettuceProtobufCodecs.protobuf<Any>()

// old -> new and new -> old wire compatibility
newCodec.decodeValue(oldCodec.encodeValue(message)) shouldBeEqualTo message
oldCodec.decodeValue(newCodec.encodeValue(message)) shouldBeEqualTo message
```

함께 검증할 항목:

- strict non-Protobuf exception type/message/cause
- trusted `ProtoMessage` direct path와 trusted non-Protobuf Kryo fallback bytes
- default allowlist 밖 decode는 계속 차단
- default strict와 trusted factory가 같은 well-formed outside-prefix `Any`를 거부하고,
  trusted profile도 이 `SecurityException`을 Kryo fallback으로 보내지 않음
- pre-existing/crafted out-of-prefix `typeUrl`과 malformed payload가 기존 exception
  type/message/cause chain을 유지함. counting/trap `ClassLoader`로 unauthorized class의
  load 시도가 0회인지 확인하며, 잘못된 payload가 decode에 성공하지 못한 것만으로
  security gate를 대체하지 않음
- caller-created `ProtobufSerializer(allowedClassPrefixes=...)`를 감싼 generic
  `LettuceBinaryCodec`은 private optimized subtype이 아니며 명시적으로 허용한 custom
  prefix만 decode함
- `encodeValue(value): ByteBuffer`, decode, key encode, estimate 불변
- repeated target invocation에서 prefix/stale byte/index/ref-count drift 없음

- [ ] **Step 3: focused unit tests를 실행한다.**

```bash
set -euo pipefail
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest" \
  --rerun-tasks --no-configuration-cache
```

- [ ] **Step 4: Redis integration을 단독 실행한다.**

```bash
set -euo pipefail
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufCodecsTest" \
  --rerun-tasks --no-configuration-cache
```

Expected: strict/trusted Redis SET/GET/HSET round trip PASS. 동시에 다른 Testcontainers suite를 실행하지 않는다.

- [ ] **Step 5: Lore commit.** Intent: `Preserve Lettuce Protobuf trust and wire contracts`.

## Task 4: source/binary compatibility proof를 완성한다

**Files:**

- Modify: `infra/lettuce/src/test/java/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecJavaCompatibilityFixture.java`
- Create: `io/protobuf/src/test/java/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecJavaCompatibilityFixture.java`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufByteBufCodecTest.kt`

- [ ] **Step 1: Java/Kotlin caller fixture를 compile/runtime test로 고정한다.**

Java fixture는 `LettuceProtobufCodecs.INSTANCE.protobuf()`와 `trustedInternalProtobuf()`를 기존 반환형으로 받으며 target overload를 호출한다. Kotlin test는 Java fixture를 실행해 null target no-op과 wire bytes를 확인한다.

- [ ] **Step 2: reflection ABI assertions를 추가한다.**

```kotlin
val clazz = LettuceBinaryCodec::class.java
Modifier.isPublic(clazz.modifiers) shouldBeEqualTo true
Modifier.isFinal(clazz.modifiers) shouldBeEqualTo false
clazz.getDeclaredConstructor(BinarySerializer::class.java)
clazz.getMethod("encodeValue", Any::class.java, ByteBuf::class.java)
```

`serializer` getter와 기존 public method descriptors가 남아 있는지 확인한다.
ordinary source method 일곱 개가 source/reflection 관점에서 final인지, target overload만
source override 가능한지 확인한다. 세 compiler bridge는 reflection만으로 raw numeric
flags를 추론하지 않고 Task 1의 paired verbose proof에 맡긴다. private optimized subtype은
public/protected constructor가 없고 class directory/JAR/reflection의 외부 접근 surface에
노출되지 않아야 한다.

- [ ] **Step 3: positive caller와 negative override fixture를 함께 실행한다.**

```bash
set -euo pipefail
./gradlew :bluetape4k-lettuce:test :bluetape4k-protobuf:compileTestJava \
  :bluetape4k-protobuf:test --no-configuration-cache
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
```

Kotlin/Java 기존 caller와 target override fixture는 compile/runtime success여야 한다. source
key method 및 erased/raw/generic bridge override fixture는 각각 compile failure여야 한다.

- [ ] **Step 4: latest committed candidate의 manifest-bound ABI proof를 재생성한다.**

Task 1 Step 6의 detached capture를 latest committed candidate에 다시 실행하고 validator에는
`payload-manifest.json` 하나만 전달한다. structural raw output은 constructor/전체 member set과
descriptor를, verbose raw output은 class `0x0031 -> 0x0021`, ordinary method 일곱 개
`0x0001 -> 0x0011`, target `0x0001 -> 0x0001`, bridge 세 개 `0x1041 -> 0x1041` 및
all-other exact equality를 증명해야 한다. wrong authority, toolchain drift, raw hash drift,
JAR-entry/standalone class mismatch는 blocker다.

- [ ] **Step 5: Lore commit.** Intent: `Prove existing Lettuce codec callers remain compatible`.

## Task 5: Lettuce heap/direct JMH matrix를 TDD로 추가한다

**Files:**

- Modify: `benchmark/protobuf-codec-benchmark/build.gradle.kts`
- Modify: `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupport.kt`
- Modify: `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkMetadata.kt`
- Modify: `benchmark/protobuf-codec-benchmark/src/benchmark/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmark.kt`
- Modify: `benchmark/protobuf-codec-benchmark/src/test/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupportTest.kt`

**Skills:** `test-driven-development`; benchmark hazard gate 적용.

- [ ] **Step 1: benchmark task 이름을 현재 Gradle에서 확인한다.**

```bash
set -euo pipefail
./gradlew :protobuf-codec-benchmark:tasks --all --no-configuration-cache | \
  rg "benchmarkBenchmark(Compile|Jar|Run|$)"
```

- [ ] **Step 2: exact four-cell matrix RED test를 쓴다.**

```kotlin
val lettuceMethods = setOf(
    "lettuceEncodeHeapCopied",
    "lettuceEncodeHeapOptimized",
    "lettuceEncodeDirectCopied",
    "lettuceEncodeDirectOptimized",
)
```

fixture test는 동일 payload/prefix/start/final capacity/max capacity, output length, wire bytes, decoded message, writer delta, `refCnt`, repeated reset을 검증한다. copied는 `LettuceBinaryCodec(ProtobufSerializer())`, optimized는 production factory를 사용한다.

- [ ] **Step 3: benchmark-only project dependency를 추가한다.**

```kotlin
dependencies {
    implementation(project(":bluetape4k-protobuf"))
    implementation(project(":bluetape4k-redisson"))
    implementation(project(":bluetape4k-lettuce"))
}
```

- [ ] **Step 4: fixture와 benchmark methods를 구현한다.**

trial setup에서 heap/direct `ByteBuf`를 final capacity까지 미리 확장하고 prefix를 쓴다. invocation setup은 allocation 없이 index와 prefix를 복원한다. timed method는 codec call 후 writer index와 encoded range의 first/last-byte checksum만 `Blackhole`에 전달한다.

```kotlin
@Benchmark
fun lettuceEncodeHeapCopied(blackhole: Blackhole) =
    fixture.lettuceEncodeHeapCopied(blackhole)

@Benchmark
fun lettuceEncodeHeapOptimized(blackhole: Blackhole) =
    fixture.lettuceEncodeHeapOptimized(blackhole)
```

direct pair도 동일하게 추가하고 trial teardown에서 fixture-owned buffers를 정확히 한 번 release한다.

- [ ] **Step 5: metadata identity를 갱신한다.**

matrix version을 `issue-757-lettuce-v2`로 올리고 allocator class, heap/direct capacity/maxCapacity, canonical prefix/reader/writer index, expanded method set을 `config_json`/SHA에 포함한다.

- [ ] **Step 6: tests/compile/JAR smoke.**

```bash
set -euo pipefail
./gradlew :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkCompile \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache
```

- [ ] **Step 7: Lore commit.** Intent: `Compare Lettuce copied and direct writes under one payload contract`.

## Task 6: allocation uncertainty와 terminal precedence를 fail-closed로 만든다

**Files:**

- Modify: `benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py`
- Modify: `benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py`

**Skills:** `test-driven-development`.

- [ ] **Step 1: malformed metric RED fixtures를 추가한다.**

complete fixture에 `secondaryMetrics["gc.alloc.rate.norm"].scoreError`를 추가하고 missing score/error, wrong unit, zero/negative score, negative error, `NaN`, `Infinity`, duplicate/missing/unexpected Lettuce method를 각각 non-zero failure로 고정한다.

- [ ] **Step 2: mixed-cell verdict RED fixtures를 추가한다.**

다음을 모두 test한다.

- one accepted + one inconclusive → `retained-accepted`, accepted cell만 positive
- both inconclusive → `retained-inconclusive`
- one regression + one accepted/inconclusive → `rejected-after-regression`
- relative 5%는 통과하지만 absolute 8 B/op 미달 → inconclusive
- uncertainty interval overlap → inconclusive
- error field가 바뀐 두 run identity mismatch → failure가 아니라 metric-specific comparison input; execution identity는 동일해야 함
- raw JMH `scoreError`가 per-run summary CSV와 comparison CSV를 거쳐 verdict formula까지
  byte-for-byte 전달됨
- allocator class, heap/direct capacity·maxCapacity, canonical reader/writer index 중 하나가
  다르면 config/two-run identity failure

- [ ] **Step 3: RED를 실행한다.**

```bash
set -euo pipefail
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
```

- [ ] **Step 4: metric model과 formula를 구현한다.**

```python
rows[method] = {
    "throughput": validate_positive_finite(primary["score"], ...),
    "allocation": validate_positive_finite(allocation["score"], ...),
    "allocation_error": validate_non_negative_finite(allocation["scoreError"], ...),
    ...,
}
```

`SUMMARY_FIELDS`에 `allocation_error_bytes_per_operation`을 추가하고 summary reader가 이를
finite/non-negative float로 복원한다. `COMPARISON_FIELDS`에는 run A/B candidate와 baseline
각각의 allocation error를 추가한다. compare formula는 raw JMH를 다시 읽지 않고 이
state-bound summary fields만 사용하며, end-to-end fixture가 raw JSON → summary CSV →
comparison CSV → validation verdict를 검증한다.

`CONFIG_KEYS`와 metadata type validation에는 allocator class, heap/direct capacity와
maxCapacity, canonical reader/writer index를 추가한다. copied/optimized pair가 같은 target
fixture identity를 공유하는지 Kotlin fixture와 Python config validator 양쪽에서 확인한다.

positive formula:

```python
candidate <= baseline * 0.95
baseline - candidate >= 8.0
candidate + candidate_error < baseline - baseline_error
```

regression formula는 대칭이다. `ROLLBACK_DISPATCH_CELLS`에 `lettuce_encode`와 heap/direct optimized cells를 추가하고 delivery terminal은 regression 우선, 다음 accepted, 나머지 inconclusive로 집계한다. compare validation과 runner state에 canonical `delivery_terminal` field를 기록한다.

- [ ] **Step 5: Python tests와 Gradle fixture를 GREEN으로 만든다.**

```bash
set -euo pipefail
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
./gradlew :protobuf-codec-benchmark:test --no-configuration-cache
```

- [ ] **Step 6: Lore commit.** Intent: `Reject ambiguous Lettuce allocation evidence`.

## Task 7: promoted evidence replacement를 immutable/non-recursive로 만든다

**Files:**

- Modify: `benchmark/protobuf-codec-benchmark/scripts/run-evidence.py`
- Modify: `benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py`
- Modify: `infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py`
- Modify: `infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py`

**Skills:** `test-driven-development`. 이 task는 파일 lifecycle 테스트만 수행하고 live promoted tree를 아직 바꾸지 않는다.

- [ ] **Step 1: repeated replacement RED fixtures를 추가한다.**

temporary repo fixture에서 다음을 검증한다.

- 기존 active generation regular files가 `archive/<old-delivery-commit>/`에 한 번 복사됨
- 기존 `archive/` subtree를 새 archive 아래 재귀 복사하지 않음
- 이전 manifest의 `superseded_evidence` path/hash가 carry-forward됨
- new archive entry는 relative path, file hashes, file-set hash를 가짐
- symlink, missing/extra file, nested `archive/**/archive`, hash drift, destination collision은 실패
- legacy rollback archive는 해당 generation에 한 번만 포함됨
- validation/generation publish 또는 pointer rename 전 실패는 old active pointer를 보존하고,
  pointer rename 뒤 parent fsync 실패/crash는 old 또는 exact intended new pointer만 허용하며
  restart가 new를 durable completion으로 재채택함
- fully validated staging만 한 번도 존재하지 않은 `generations/<generation-id>`에 platform
  atomic no-replace로 publish되며 existing/non-empty target은 삭제/교체 없이 실패함
- exclusive promotion lock과 monotonically increasing fencing token이 한 promoter만 publish하게
  하고 stale token과 previous-pointer CAS drift는 old pointer를 유지한 채 실패함
- lock symlink/replacement/inode drift, partial token staging, missing/extra/hash-broken token
  generation, stale owner/PID reuse, token replay와 crash 뒤 ambiguous token allocation을 거부함
- staged file/directory fsync, generation rename, generation parent fsync, pointer temp
  write/flush/fsync, pointer atomic rename, pointer parent fsync 각 interruption에서 old 또는 new
  pointer가 온전하고 재시작 후 durable함
- generation rename 뒤 crash한 old owner를 새 process가 새 recovery token으로 인수해 exact
  immutable generation/old receipt를 검증하고 pointer 단계만 재개함
- exact owner/token/root-hash receipt의 idempotent resume만 unreferenced immutable generation을
  재사용하고, cleanup은 own abandoned staging만 대상으로 하며 generation은 삭제하지 않음
- `lettuce_encode`가 parser/decision ordering/cell mapping/source predicate에 없으면 실패
- candidate commit에서 `lettuce_encode` rollback을 준비하고 subtype과 `open` ABI를 제거한
  committed head에서만 finalize가 성공함
- post-release recovery는 published retained JAR와 recovery JAR의 public ABI를 exact-equal로
  보존하고 dispatch-only rollback만 허용하며 recovery checklist phase 누락을 거부함
- recovery template schema drift, mutable/rewritten phase, missing previous-phase hash, approval의
  planned artifact/target/command/threshold binding drift와 actual artifact digest mismatch를 거부함
- distinct pinned GitHub reviewer의 live rollback approval marker가 없거나 runner actor와 같거나
  edited/deleted/duplicate이고 contract/candidate/governed path/approver/attempt binding이 다르면
  production mutation 전에 실패함
- rollback/recovery approval policy가 없거나 rewrite/rebound됐거나 expected reviewer, runner actor,
  repository/issue, marker schema, approval kind, binding rule 또는 policy receipt hash가 drift하면
  contract/proposal 생성 전과 approval import/verify/mutation 직전에 각각 실패함
- policy input review 뒤 pin 전 byte replacement와 approved SHA mismatch를 거부하고, receipt temp
  fsync/publish/parent fsync/selection temp fsync/rename/parent fsync 각 crash 경계에서 selected state가
  absent/old/exact intended 중 하나로만 복구되며 exact orphan receipt만 adopt함
- candidate verification command 하나라도 실패하면 receipt가 생성되지 않음
- canonical run 직전 environment preflight receipt가 없거나 stale, 다른 state/JAR identity,
  active Gradle/container/heavy-work snapshot이면 run을 시작하지 않음
- receipt의 commit/tree/JAR/log hash drift 또는 rollback bundle 누락을 거부함
- receipt/output/log root의 symlink, traversal, absolute/duplicate/extra file, unauthorized root,
  output collision을 거부함
- unrelated committed manifest로 다른 evidence destination replacement를 승인할 수 없음
- raw argument 또는 ancestor component가 symlink인 manifest/destination/backup root를
  `resolve()` 전에 거부함
- carried path/file key의 absolute path, `..`, duplicate, non-canonical separator,
  `archive/<validated-commit>/` 밖 경로를 거부함
- payload -> class/JAR/raw, result -> payload, root -> payload/result의 non-cyclic DAG만 허용하고
  payload/root의 self/final-head reference, wrong JAR entry와 same-classfile mismatch를 거부함
- GitHub handoff body symlink/overwrite/hash drift, active generation/final head/repo/base/head/terminal
  mismatch와 post-create PR/comment live body 또는 evidence identity mismatch를 거부함
- PR `author.login`/head repository owner/name과 comment `user.login`이 sealed authenticated actor/
  repository와 다르면 reconcile, outcome record와 live verification 모두 거부함
- PR/comment mutation 성공 직후 crash/응답 유실 재시도에서 stable intent marker로 exact live
  object를 한 번만 adopt하고 conflict/duplicate는 실패함
- retained external verifier는 measurement-source ancestry와 exact docs/evidence allowlist를,
  rejected verifier는 measurement-source -> rollback-source -> final head ancestry와 canonical
  rollback contract를 검증하고 prefix/wildcard 또는 input drift를 거부함

- [ ] **Step 2: archive model helper를 구현한다.**

```python
def active_generation_files(root):
    return sorted(
        path for path in root.rglob("*")
        if path.is_file() and "archive" not in path.relative_to(root).parts
    )
```

실제 구현은 symlink-free regular file 검증을 재사용하고, manifest에 다음 shape를 기록한다.

```json
{
  "superseded_evidence": [
    {
      "delivery_commit": "0123456789abcdef0123456789abcdef01234567",
      "path": "archive/0123456789abcdef0123456789abcdef01234567",
      "files": {"relative/file": "sha256"},
      "file_set_sha256": "sha256"
    }
  ]
}
```

- [ ] **Step 3: immutable generation과 active pointer lifecycle을 구현한다.**

기존 active pointer가 가리키는 committed generation을 먼저 validate하고, same-filesystem
sibling staging에 새 state evidence, 직전 active file set archive와 carried entries를 만든다.
payload/root DAG, semantic/archive/file-set/permission 검증과 모든 staged file/directory fsync가
통과한 뒤에만 새 immutable generation으로 publish한다. `verify-promoted`,
`validate-committed`, report hash input도 `superseded_evidence`, generation ID/root hash와 active
pointer identity를 포함한다.

pre-mutation authorization guard는 raw path를 resolve하기 전에 모든 ancestor의 symlink를
거부하고 다음 invariant를 요구한다.

```python
destination_root == repo_root / "docs/benchmarks/raw/issue-757"
generations_root == destination_root / "generations"
active_pointer == destination_root / "active-generation.json"
lock_path == repo_root / ".omx/evidence/issue-757-lettuce/promotion.lock"
token_root == repo_root / ".omx/evidence/issue-757-lettuce/promotion-tokens"
```

각 `superseded_evidence.path`는 `archive/<40-or-64-hex-validated-commit>` canonical relative
directory이고, file key도 그 subtree 아래의 unique canonical relative regular file이어야
한다. source, staging, generation, pointer는 tracked destination root 아래에,
lock/token/owner receipt는 ignored runtime authority root
`.omx/evidence/issue-757-lettuce/` 바로 아래에 남는지 복사와 rename 직전에 다시 확인한다.
lock/token/owner receipt는 generation file set, manifest, archive, commit 대상에 포함하지 않으며
runtime authority root 밖 path, symlink ancestor, nested path를 거부한다.

promoter는 generation reservation 전에 canonical `promotion.lock`을
`O_CREAT|O_RDWR|O_NOFOLLOW`로 열고 regular-file, link-count, device/inode를 `fstat`한 뒤 OS-level
exclusive lock을 획득한다. fixed lock inode는 replace/delete/truncate하지 않는다. 같은 ignored
runtime root의 `promotion-tokens/`는 삭제하지 않는 immutable token generation root다. lock
아래에서 canonical `token-<20-digit-sequence>.json` regular files의 연속 sequence/hash chain을
검증하고, next token content에 previous hash, owner UUID, process start identity, nonce와 input
root hash를 기록한다. unique sibling temp를 write/flush/fsync한 뒤 platform atomic no-replace로
아직 없는 canonical token path에 rename하고 token-root parent를 fsync해야 token이 발급된다.
rename 전 crash의 owned temp만 정리할 수 있고, rename 뒤 parent fsync 전 crash는 exact valid
token file을 재검증해 committed boundary로 재채택한다. 기존 token generation은 truncate,
replace/delete하지 않는다. lock/token-root device/inode를 단계마다 재검증해 symlink/replacement,
PID reuse, stale owner, replay와 partial/hash-broken generation을 거부한다. fencing token은 모든
staging/generation/pointer/resume/cleanup receipt에 bind되고
pointer parent fsync까지 lock을 유지한다. generation ID는 immutable input/file-set root
hash에서 결정하되 target은 반드시 non-existing이어야 한다. Linux는
`renameat2(RENAME_NOREPLACE)`, macOS는 `renamex_np(RENAME_EXCL)`처럼 atomic no-replace를
보장하는 primitive만 사용하고 미지원 platform은 fail-closed한다. generation rename 뒤
`generations/` parent를 fsync한다. active pointer는 previous generation/root hash를 CAS하고,
같은 directory의 unique temp를 write/flush/fsync한 뒤 atomic rename과 parent fsync로 한
파일만 교체한다. Git commit/tree가 generation과 pointer의 최종 authority이며 pointer는
아직 없는 final commit/tree를 기록하지 않는다.

같은 runner에서 rejected terminal이 실행 가능하도록 다음 mapping과 removal proof도 추가한다.

```python
DISPATCH_ORDER = (
    "serializer_encode",
    "serializer_decode",
    "redisson_contiguous",
    "lettuce_encode",
)
DISPATCH_CELLS["lettuce_encode"] = (
    "lettuceEncodeHeapOptimized",
    "lettuceEncodeDirectOptimized",
)
DISPATCH_SOURCE_PATHS["lettuce_encode"] = (
    "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/"
    "LettuceProtobufCodecs.kt"
)
```

`verify_dispatch_removal`의 Lettuce case는 pre-release rejected rollback-source/final head에서
private optimized subtype/writer가 없고 두 uncompressed factory가 generic codec으로
복원됐는지 확인한다. `LettuceBinaryCodec`은 class-open, ordinary-method-final을 포함한
retained-only ABI delta가 전부 사라지고 immutable authority baseline과 raw flags까지
exact-equal이어야 한다. validator는 그 rollback-source의 manifest 하나를 `rejected` mode로
검증한다. predicate는 source substring이 아니라 canonical rollback contract의 exact governed
path, pre/post blob SHA-256, required deletion, class/JAR/reflection subtype absence와 generic
compatibility/security receipt를 모두 검사한다. archived candidate measurement-source
commit/tree/JAR/ABI/JMH identity는 rollback bundle에 유지한다. 이 규칙은 publish 전 rejected
terminal 전용이며 post-release recovery에서는 published retained ABI를 제거하지 않는다.

별도 `published-retained-vs-recovery` ABI mode는 Maven에서 가져온 실제 published GAV/version,
JAR SHA-256, release commit/tree/repository/ref를 baseline authority로 요구한다. clean detached
recovery JAR는 class-open, target-open, ordinary-method-final과 bridge `0x1041`을 포함한 published
retained class/member descriptor/raw flags와 exact-equal이어야 하고, diff allowlist는 uncompressed
strict/trusted factory의 optimized subtype dispatch를 generic `LettuceBinaryCodec`으로 되돌리는
path/blob set만 허용한다. optimized subtype removal은 허용하지만 published public ABI 제거,
local build를 published authority로 대체, pre-release rejected contract 재사용은 실패다.

이 mode의 소유자는 Task 1 ABI validator다. parser에
`validate --manifest FILE`의 mode enum으로 추가하고 manifest 하나만 입력받는다. manifest는
`mode`, `published_baseline`의 repository URL/GAV/version/JAR path+SHA-256/class entry/release
commit+tree+ref와
immutable Maven retrieval receipt path+SHA-256,
`planned_recovery` coordinate/expected digest/command/target, recovery commit+tree/JAR
path+SHA-256/class entry, 양쪽 exact toolchain/`javap` raw path+hash,
dispatch-only contract path+hash, pre-dispatch checklist receipt path+hash를 필수로 갖는다.
`run-evidence.py verify-published-recovery`는 `--pre-dispatch-selection-state FILE`만 authority로
받아 CAS-selected final pre-dispatch receipt path/hash를 내부 resolve하고 그 exact
approval receipt의 별도
`published_baseline` object에서 approved repository URL/GAV/expected digest/release identity를
읽고 canonical artifact URL을
직접 계산해 authorized evidence root로 no-clobber fetch한다. caller-provided local JAR path/hash와
`file:` URL은 받지 않는다. redirect는 같은 approved repository authority 안에서만 허용하고
request/final URL, HTTP status, response identity, GAV, fetch timestamp, downloaded regular-file
path/hash와 approval receipt hash를 immutable retrieval receipt에 기록한다. fresh explicit user
gate 뒤 flow runner와 다른 authenticated GitHub reviewer가 #757에 게시한 exact approval marker를
`import-github-approval`이 read-only로 가져온 recovery approval receipt는 incident ID/root와 flow
run ID, approver identity, immutable pre-dispatch proposal path/hash, `published_baseline`과
`planned_recovery` 전체 identity, connect/read timeout, maximum artifact bytes를 bind한다. expected
reviewer는 proposal 생성 전에 별도 no-clobber approval policy receipt에 미리 pin하고 live comment
author는 그 receipt에 함께 pin한 current `gh api user` runner actor와 달라야 한다. policy receipt는
repository/issue, approval kind, expected reviewer, runner actor, marker schema version과 exact
proposal binding rule을 bind한다. checklist/runner는 approval comment를 게시하거나 receipt를 자체
발급할 수 없고 `verify-recovery-approval`이 comment URL/ID/node ID/author/created-at/body hash를
live GitHub에서 다시 조회해 issuer authority, self-approval 금지, exact no-clobber receipt
path/hash와 모든 binding을 fetch 및 dispatch 직전에 재검증한다.

approval marker body는 `issue757-approval:v1`, `kind`, exact contract/proposal SHA-256, incident 또는
rollback preparation ID, flow run ID와 `approve` decision만 허용하는 canonical JSON code fence다.
comment `created_at`은 contract/proposal receipt보다 늦고 `updated_at == created_at`이어야 하며 exact
marker가 pinned reviewer에게서 하나만 존재해야 한다. runner가 사용하는 authenticated GitHub actor,
contract/proposal preparer 또는 중복 reviewer comment는 authority가 아니며, comment edit/delete/live
drift는 import·재검증·mutation을 모두 fail-closed한다.

fetch는 approved connect/read timeout과 `Content-Length` upper bound 및 streaming byte cap을
모두 적용한다. authorized evidence root의 opened directory inode를 고정하고 relative
`dirfd` + `O_NOFOLLOW|O_CREAT|O_EXCL`로 owned temporary regular file을 만든 뒤 `fstat`한다.
stream 완료 후 file과 directory를 fsync하고 같은 pinned parent 안에서 atomic no-replace로
final artifact/retrieval receipt를 publish한다. parent inode/ownership replacement, symlink/hardlink,
oversized/slow/infinite response, partial read 또는 digest mismatch는 selection을 바꾸지 않고
owned temp만 bounded cleanup한 뒤 recovery build 전에 실패한다. 이어 clean detached recovery
build를 별도 root에서 capture하고 같은 class entry/toolchain으로 manifest를 만든 뒤 validator를
호출해 selection receipt를 emit한다. exact command shape는 다음과 같다.

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  pin-github-approval-policy \
  --kind recovery \
  --input FILE \
  --expected-input-sha256 SHA256 \
  --gh-binary "$(command -v gh)" \
  --selection-state FILE
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  validate-recovery-checklist \
  --phase pre-dispatch-proposal \
  --input FILE \
  --approval-policy-selection-state FILE \
  --selection-state FILE
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  import-github-approval \
  --kind recovery \
  --pre-dispatch-proposal FILE \
  --repository bluetape4k/bluetape4k-projects \
  --issue 757 \
  --gh-binary "$(command -v gh)" \
  --approval-selection-state FILE
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  verify-recovery-approval \
  --pre-dispatch-proposal FILE \
  --gh-binary "$(command -v gh)" \
  --approval-selection-state FILE
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  finalize-recovery-pre-dispatch \
  --pre-dispatch-proposal FILE \
  --gh-binary "$(command -v gh)" \
  --approval-selection-state FILE \
  --pre-dispatch-selection-state FILE
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  verify-published-recovery \
  --evidence-root PATH \
  --recovery-source REV \
  --dispatch-contract FILE \
  --pre-dispatch-selection-state FILE \
  --gh-binary "$(command -v gh)" \
  --approval-selection-state FILE \
  --abi-manifest OUTPUT \
  --selection-state FILE
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py validate \
  --manifest OUTPUT
# Run only after the separately approved deployment and its confirmation exist.
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  validate-recovery-checklist \
  --phase post-dispatch \
  --input FILE \
  --pre-dispatch-selection-state FILE \
  --selection-state FILE
# Run only after the pinned observation window and close evidence are complete.
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  validate-recovery-checklist \
  --phase close \
  --input FILE \
  --post-dispatch-selection-state FILE \
  --selection-state FILE
```

ABI validator/runner fixtures는 missing/wrong published baseline authority, published baseline과
planned recovery identity alias/swap/self-comparison, local JAR/path/hash input,
non-HTTPS/file URL, cross-authority redirect, retrieval receipt/approval/digest drift,
missing/forged/self-issued/wrong-incident recovery approval, current runner actor와 같은 author,
missing/changed/rebound approval policy, unpinned/wrong reviewer, policy runner actor/repository/issue/
marker schema/binding-rule drift, pinned `gh` binary path/hash drift, edited/deleted/duplicate GitHub
marker와 live comment drift,
proposal/approval/final receipt identity drift,
approval-before-proposal, proposal rewrite, double-finalize conflict, crash before/after final receipt selection,
unapproved timeout/byte cap, oversized `Content-Length`, oversized/slow/infinite streaming body,
partial-download cleanup, parent inode replacement와 symlink/hardlink/no-replace race,
wrong class entry/JAR/raw/toolchain,
recovery public flag/descriptor drift, dispatch-contract/checklist receipt drift와 local-build
substitution을 각각 reject하고 exact retained ABI + dispatch-only diff만 accept한다.

`validate-recovery-checklist --phase template|pre-dispatch-proposal|pre-dispatch|post-dispatch|close`를 추가한다.
template은 required schema와 phase transition shape만 검증하고 incident 값이나 approval을
성공으로 간주하지 않는다. 각 실제 phase는 unique incident root 아래 immutable no-clobber
generation/receipt로 기록하고 selection state를 atomic CAS한다. post-dispatch는 exact
pre-dispatch receipt hash를, close는 exact post-dispatch receipt hash를 참조하며 이전 generation을
rewrite하지 않는다. CLI는 post-dispatch에 `--pre-dispatch-selection-state FILE`, close에
`--post-dispatch-selection-state FILE`을 필수로 받아 selected previous receipt path/hash를 input의
previous-phase identity와 exact 비교한다. `pin-github-approval-policy --kind recovery`는 사용자가
먼저 검토한 canonical
policy input의 repository/issue, expected distinct reviewer, current runner actor, validated absolute
regular-file `gh` path/SHA-256, marker schema version, approval kind와 proposal binding rule을 immutable
no-clobber receipt로 만들고 selection state를
atomic CAS한다. input/receipt rewrite, conflicting second selection과 runner actor/reviewer 동일성은
거부한다. `pre-dispatch-proposal`은 selected policy receipt path/hash를 요구하고 fresh approval 없이
아래 전체 payload를 immutable
proposal path/hash로 먼저 고정한다. `import-github-approval`은 그 exact proposal hash를 포함해
distinct pinned reviewer가 게시한 GitHub approval marker를 가져오고,
`finalize-recovery-pre-dispatch`는 proposal과 verified approval이 exact-equal일 때만
final pre-dispatch receipt를 no-clobber 생성해 selection state를 CAS한다. crash/retry는 기존
proposal/approval/final receipt를 hash로 adopt할 수 있지만 rewrite 또는 conflicting second
selection은 거부한다. final pre-dispatch는
reopened #757, supported release/GAV, deployment target, rollback/escalation owner/contact,
baseline/recovery observation window와 최소 duration, Redis encode/SET, serialization/decode,
Netty ref-count, allocation/GC, round-trip metric별 threshold/query/dashboard/aggregation/sample/
missing-data policy와 서로 다른 두 identity object를 요구한다. `published_baseline`은 approved
HTTPS repository URL, GAV/version, expected JAR digest, release commit/tree/ref를 갖고,
`planned_recovery`는 coordinate, expected digest, command/change request, target/environment/region을
갖는다. 두 object는 alias, swap 또는 self-comparison할 수 없다. fresh approval receipt는
pre-dispatch proposal path/hash와 두 object 각각의 전체 identity, metric threshold/query/window set을
비롯해 incident/flow/approver identity와 fetch timeout/maximum artifact bytes를 exact bind하며,
independent `import-github-approval`/`verify-recovery-approval` lifecycle과 fresh explicit user
gate를 통과해야 한다. preparer/checklist/runner actor와 approval comment author가 같으면
금지하며 distinct approved reviewer가 없으면 workflow를 blocked로 유지한다. post-dispatch는 actual
deployed coordinate/digest/command/target을 오직 approved
`planned_recovery`와 exact 비교하고, executed change ID와 rollout start/end 및 deployment
confirmation을 추가로 요구한다. published baseline fetch와 ABI 비교는 오직 approved
`published_baseline`을 사용하며 actual recovery identity를 baseline으로 재사용하지 않는다. close는 pinned
window의 metric pass, `published-retained-vs-recovery` result, refreshed compatibility/wire/security/
Redis/allocation/promoted-tree evidence와 consumer impact link를 요구한다. phase를 건너뛰거나
field/query/owner/approval/digest가 없거나 두 identity가 alias/swap/self-comparison이면 dispatch,
observation start 또는 issue close를 각각
fail-closed한다. release/tag/publish/redeploy는 이 tooling이 실행하지 않으며 별도 승인을 유지한다.

candidate 기능 검증을 manifest-bound evidence로 만들기 위해 runner에
`verify-candidate --evidence-root PATH --selection-state FILE` command를
추가한다. 이 command는 clean
committed HEAD에서 Task 8의 focused tests, Lettuce full build, Protobuf full build,
benchmark/Python/static checks를 순차 실행하고 command argv, exit code, bounded log
file/hash, commit, tree, benchmark JAR SHA-256, validator-consumed ABI payload/result/root
hash와 retained ABI validator result를 canonical receipt에 기록한다. 하나라도
실패하면 receipt를 emit하지 않는다. `selected-receipt --selection-state FILE`은 atomic
selection state가 가리키는 exact receipt path/hash만 반환하고,
`resolve-jar --verification-receipt FILE`은 그 receipt 내부 exact detached JAR path/hash를 유일한
JAR authority로 state에 bind한다. current worktree build directory를 검색하거나 별도 jar path를
받지 않는다. promotion은 receipt와 logs를 regular files로 복사하고 delivery manifest에
path/hash를 기록한다.
rollback preparation/bundle은 candidate receipt 전체를 immutable archive에 포함하며 final
rejected manifest가 그 archive path/hash를 재검증한다.

detached verification runner는 shared primitive의 phase 0을 selection state에 CAS한 뒤에만 첫
checkout/build side effect를 수행하고, 각 add/register/build-root 성공 뒤 actual inode/sidecar
marker/commit/tree/admin-gitdir mapping을 append-only phase로 fsync·CAS한다. 성공/실패 모두 bounded logs,
raw ABI, manifest/result와 이후 단계가 필요한 benchmark JAR를 authorized evidence child로
no-clobber seal·fsync하고 receipt는 checkout/build path가 아니라 이 evidence-owned copy만
참조한다. `finally`에서는 각 destructive action 직전에 raw ancestor non-symlink, containment,
device+inode, marker type/link-count/hash, role/commit/tree와 registry gitdir mapping을 재검증한다.
일치한 registered root만 exact unregister/remove한 뒤 registry/filesystem absence를 증명하고,
partial unregistered root/build root는 marker와 inode ownership이 일치할 때만 제거해 cleanup
receipt를 append한다. mismatch는 foreign root를 그대로 두고 fail-closed한다. immutable evidence child,
selected receipt/JAR와 rollback archive는 삭제하지 않는다. interrupt/crash 뒤
`cleanup-detached --evidence-root PATH --selection-state FILE`은 broad scan 없이 attempt journal의
owned marker/path/inode와 worktree registration/gitdir mapping을 재검증해 exact orphan root만
idempotent하게 수거하고 cleanup receipt를 emit한다. original exit/signal status는 cleanup 뒤
재전파하고 cleanup failure는 step failure로 승격한다. 성공·test failure·build failure·seal 전/후
crash·register/unregister/remove 경계 crash·partial add·cleanup failure·interrupt·rerun과 foreign
root replacement fixtures는 registered worktree/build root가 남지 않고 foreign data와 sealed
evidence가 계속 검증됨을 확인한다.
selection state당 unselected failed attempt는 최대 8개, 각 bounded log/evidence cap 이내로
제한하며 cap 도달 시 자동 삭제하지 않고 archive/prune에 대한 별도 explicit approval 전까지
새 attempt를 fail-closed한다.

verification output은 repo의 exact authorized root `.omx/evidence/issue-757-lettuce/` 바로
아래 unique no-clobber directory만 허용한다. raw output-root, receipt, log path와 모든
ancestor를 `resolve()` 전에 symlink 검사한다. receipt의 log paths는 receipt directory
기준 canonical relative path이며 absolute path, `..`, separator ambiguity, duplicate,
unexpected/extra file을 거부한다. `resolve-jar`/`validate-verification`은 receipt directory에서
exact expected command/log file set을 재구성해 regular-file type, successful exit code, hash,
commit/tree/JAR identity와 authorized-root containment를 모두 확인한다. promotion과 rollback은
이 validated file set만 fixed collision-free relative destination으로 복사한다.
`validate-verification --authority-revision REV`는 현재 repo에서 `REV`의 commit/tree를 resolve해
receipt의 measurement-source 또는 rollback-source와 exact-equal인지 fail-closed로 비교한다.

runner가 `candidate-{commit}-{run-id}` 또는 `final-rejected-{commit}-{run-id}` unique direct
child를 생성하고 성공 후에만 selection state를 새 receipt path/hash/attempt ID로 atomic
교체한다. 실패한 attempt directory/log는 보존하되 selection state는 바꾸지 않는다. retry
fixture는 첫 attempt를 삭제하지 않고 두 번째 attempt와 rollback root가 충돌 없이 성공함을
검증한다. 각 `prepare-rollback-contract` 호출은 candidate attempt ID에 별도 no-clobber
preparation run ID를 더한 unique rollback root를 발급하고 exact root/contract path/hash를
rollback selection state에 atomic CAS로 기록한다. `record-rollback`과 `finalize-rollback`은
그 selected preparation handle만 받으며 exact bundle path/hash를 같은 state에 기록한다.
downstream은 broad `find`나 candidate attempt ID 기반 경로 재구성을 사용하지 않고 이 state
handles만 사용한다.

`verify-rollback-source --contract FILE --evidence-root PATH --selection-state FILE`은 제거 후
clean detached committed rollback-source에서 direct-writer 전용 tests를 N/A로 제외하고
generic Lettuce/Protobuf full builds, benchmark/Python/static checks, manifest-only rejected ABI
validator, `verify_dispatch_removal`과 subtype class/JAR/reflection absence를 순차 실행해 별도
receipt를 만든다. candidate measurement-source ancestry와 contract exact path/blob/deletion
set도 독립 resolve하며 contract에 rollback-source/final head/self hash가 있으면 거부한다.
post-removal `resolve-jar`도 rollback verification receipt 내부 exact detached JAR path/hash만
current identity로 bind하고 finalized rollback bundle을 통해 candidate receipt와 negative
evidence를 함께 authenticate한다.

`pin-github-approval-policy --kind rollback|recovery --input FILE --expected-input-sha256 SHA256
--gh-binary PATH --selection-state FILE`은 사용자가 검토하고 fresh approval한 exact SHA-256을
caller-computed hash와 구분된 필수 입력으로 받아 canonical JSON input bytes와 먼저 비교한다. 일치한
input의 repository/issue, expected distinct GitHub reviewer login, current authenticated runner actor,
validated absolute regular-file `gh` binary path/SHA-256, marker schema version, approval kind와
contract/proposal binding rule을 exact bind한 no-clobber policy
receipt를 만들고 selection state를 atomic CAS한다. input과 receipt는
regular-file/path/hash/containment 검증을 통과해야 하며 existing identical receipt만 retry에서 adopt할
수 있다. reviewer와 runner가 같거나 approved input hash/field/hash가 drift하거나 conflicting second
selection이면 fail-closed한다. receipt는 pinned parent의 temp write/flush/fsync, atomic no-replace
publish와 parent fsync가 끝난 뒤에만 selection temp write/fsync, CAS rename과 selection parent fsync를
수행한다. 각 crash 경계에서 selection은 absent/old/exact intended receipt만 허용하며 receipt publish
뒤 CAS 전 orphan은 exact approved hash와 full identity 재검증 후에만 retry가 adopt한다.
`prepare-rollback-contract --approval-policy-selection-state FILE`은 selection state가 가리키는 sole
selected rollback policy receipt path/hash와 selection-state path/hash 및 전체 identity를 contract에
포함해야 하며, recovery
`validate-recovery-checklist --phase pre-dispatch-proposal --approval-policy-selection-state FILE`도
selected recovery policy receipt 전체를 proposal에 포함해야 한다. 이후 import/verify/finalize 단계는
policy selection state/receipt와 live runner actor, repository/issue, reviewer, approved input SHA-256,
pinned `gh` binary path/hash, marker schema 및 binding rule을 매번
재검증하며 policy 변경이나 다른 contract/proposal로의 rebound를 거부한다.

`import-github-approval`, `verify-rollback-approval`, `verify-recovery-approval`, `record-rollback`,
`finalize-rollback`, `finalize-recovery-pre-dispatch`, `verify-published-recovery`와 live approval을
재검증한 뒤 fetch/mutation을 수행하는 command는 모두 `--gh-binary PATH`를 필수로 받고 policy
receipt에 pinned된 absolute
regular-file path/SHA-256과 exact-equal인지 확인한 뒤 그 binary만 직접 실행한다. implicit PATH lookup,
shell re-resolution과 changed binary는 network query나 mutation 전에 거부한다.

`import-github-approval --kind rollback --contract FILE`은 기계적으로 선택된
`rejected-after-regression` terminal과 contract approval policy를 먼저 검증하고, 별도의 fresh
user approval 뒤 flow runner와 다른 pinned GitHub reviewer가 #757에 게시한 exact contract marker를
read-only로 가져온다. no-clobber approval receipt는 live comment URL/ID/node ID/author/created-at/body
hash와 contract SHA-256,
candidate commit/tree, governed path/pre/post/deletion set hash, candidate attempt ID, rollback
preparation run ID, approver identity, approval timestamp와 flow run ID를 bind하고 approval
selection state에 atomic CAS로 기록한다. expected reviewer와 current authenticated `gh` runner
actor는 contract가 참조하는 selected approval policy receipt에 미리 pin하고 comment author가
runner actor와 같으면 거부한다. contract preparer/runner는
comment를 게시하거나 approval receipt를 자체 발급할 수 없다. distinct approved reviewer가
없으면 workflow를 blocked로 유지한다. `verify-rollback-approval`은 source mutation 전에 live
GitHub comment와 exact receipt path/hash 및 모든 binding을
재검증하며, `record-rollback`, B2 mutation guard와 `finalize-rollback`은 validated approval
receipt 없이는 fail-closed한다.

`record-environment-gate --state FILE --output-root DIR`은 각 canonical run 직전에 active Gradle
task/daemon build, container suite와 다른 declared heavy work가 없음을 process/container snapshot과
timestamp로 기록한다. runner가 no-clobber environment run ID/path를 발급해 state/JAR/authority
identity와 receipt hash를 state에 atomic CAS로 기록하고 exact receipt path를 stdout으로 반환한다.
`run`은 그 fresh exact environment receipt를 요구하며 사용한 path/hash를 state/run manifest에
기록한다. receipt 생성 뒤 identity나 environment가 바뀌면 fail-closed하고, 재시도와 두 canonical
run은 각각 새 unique receipt를 사용하며 기존 receipt를 덮어쓰거나 삭제하지 않는다.

`publish-generation`은 immutable generation 검증과 publish 뒤 active pointer CAS까지 한
fenced transaction으로 실행한다. pointer rename 전 실패는 old pointer를 유지한다. pointer
rename 뒤 parent fsync 실패/crash는 current pointer가 old 또는 exact intended generation/root인
경우만 허용한다. restart는 lock 아래에서 old면 CAS/rename을 계속하고, exact intended new면
pointer parent를 fsync한 뒤 append-only completion/adoption receipt를 기록해 성공으로 재채택한다.
그 밖의 pointer state는 fail-closed한다. generation
rename 뒤 pointer 전 단계에서 실패한 경우 새 generation은 verified unreferenced state로
남긴다. same-process resume는 canonical lock inode와 immutable token root의 current token
아래에서 exact owner UUID/process-start/nonce/token/root-hash receipt를 다시 검증한다.
old process가 종료됐거나 lease가 만료된 cross-process takeover는 lock 아래에서 old owner의
process-start identity가 더 이상 live가 아님을 확인하고, 새 recovery token generation이 old
token/receipt/generation ID/root hash/expected previous pointer를 참조한다. 새 owner는 immutable
generation 전체를 재검증한다. current pointer가 expected previous이면 CAS/rename을 진행하고,
exact intended generation/root이면 parent fsync와 append-only completion/adoption receipt만
수행해 generation rename 없이 성공을 인수한다. live old owner, unknown receipt, root drift,
previous와 exact intended new 이외의 pointer, token gap/replay는 takeover를 거부한다.
unrestricted restore/delete command와 immutable generation cleanup은 만들지 않는다.

successful manifest에는 measurement-source 또는 rollback-source authority, generation ID/root
hash, previous active generation/file-set hash를 기록하되 final delivery commit/tree는 기록하지
않는다. `cleanup-owned-staging --receipt FILE --lock FILE`은 lock/token/owner/age/root를 검증한
뒤 canonical lock inode/current token 아래에서 자신의 abandoned partial staging만 삭제한다.
cross-process cleanup은 pointer takeover와 분리하며 새 recovery token이 old owner death/lease,
staging receipt와 exact root를 bind한 경우에만 old partial staging을 대상으로 한다.
PID만 같고 owner UUID/process-start/nonce가 다른 receipt는 stale로 거부한다. active staging,
immutable generation, 다른
owner/hash의 tree, directory scan/glob/latest selection은 cleanup 대상이 아니다.

final authority는 tracked payload에 되쓰기하지 않는다. 대신
`verify-final-head --active-pointer FILE --head REV --terminal retained|rejected`를 추가한다.
retained는 external `REV`가 manifest-bound measurement-source의 descendant이고 그 이후
production/build/test/benchmark input이 root의 exact docs/evidence allowlist 외에는 같음을
검증한다. rejected는 candidate measurement-source -> canonical rollback-source -> `REV`
ancestry와 contract를 검증한 뒤 rollback-source 이후 같은 drift를 거부한다. prefix/wildcard
allowlist, rewritten/non-ancestor authority, wrong/missing rollback-source와 tracked payload/root의
final-head reference는 모두 실패다. tree-equivalent rebase가 authority commit을 바꿨다면
payload hash를 직접 rebind하지 않고 새 immutable authority generation과 필요한
verification/measurement lifecycle을 만든다.

`select-github-handoff`는 final external head 검증 뒤 `gh api user`의 authenticated actor,
target repo, exact head repository owner/name, kind/final-head와 comment의 경우 PR URL을 canonical
selection key로 계산한다. authorized ignored root의 deterministic selection
state를 먼저 읽어 exact identity가 이미 선택됐으면 기존 sealed/unsealed handoff root와 stable
intent ID를 반환하고, 없을 때만 unique no-clobber directory를 만든 뒤 외부 mutation 전에
selection을 durable atomic CAS로 고정한다. 다른 identity로 existing selection을 덮어쓰거나
broad scan/latest selection을 하지 않는다.
`seal-github-handoff`는 body의 exact hidden intent marker, regular body hash, active
generation/root/terminal, source authority, final commit/tree, repo/base/head와 선택적 PR URL을
immutable manifest/hash에 bind한다. `verify-github-handoff`는 GitHub mutation 직전에
symlink-free file set과 live local identities를 재검증한다.

`execute-github-mutation`은 validated exact `gh` binary를 child process로 소유하고 fresh live
query, `seal-github-pre-mutation`, `reconcile-github-mutation`, create/adopt, immediate post-query,
`record-github-outcome`과 live verification을 한 process 안에서 순차 수행한다. shell/caller에게
중간 JSON이나 create decision을 반환하지 않는다. 각 external mutation 바로 전에 다시 live
query하고 freshness/remote/PR head를 재검증한 뒤 다음 subprocess call로 mutation하며, 이
외부 API의 비원자적 경계를 receipt에 명시하고 mutation 직후 drift도 fail-closed outcome으로
기록한다. response loss/cold restart는 stable intent marker와 live re-query로 adopt-recovery한다.

내부 `seal-github-pre-mutation`은 fresh `gh` live bundle을 입력받아
local HEAD, live remote branch ref, repo/base/head repository+branch+SHA, authenticated actor와
existing PR/comment objects를 sealed handoff와 exact 비교한다. PR create/adopt bundle은 all-state
PR list를, issue-comment bundle은 exact PR view와 comments를 포함한다. receipt는 canonical input
hash/timestamp와 handoff manifest hash를 no-clobber bind하며 freshness 초과, remote/final SHA drift,
PR URL/head/body drift를 mutation 전에 거부한다.

`reconcile-github-mutation --handoff-root DIR --input - --decision-root DIR
--decision-selection FILE --pre-mutation-receipt FILE`은 mutation 전에
같은 receipt가 bind한 all-state PR 또는 issue comment live bundle을 검사한다. decision은 exact
pre-mutation receipt path/hash를 포함한다. exact intent marker, body hash,
authenticated `author.login` 또는 `user.login`, PR head repository owner/name,
repo/base/head/final SHA/terminal/evidence identity가 일치하는 object가 정확히 하나면 `adopt`,
없으면 `create`, marker가 중복되거나 일부만 일치하면 fail-closed하고 immutable no-clobber
decision generation을 만든 뒤 selection을 CAS한다. 기존 selected `create` 뒤 exact live marker가
발견되면 append-only `adopt-recovery` decision을 추가하고 selection을 전진시킨다. 없으면 기존
create decision을 idempotently 재사용한다. create 성공 뒤 응답 유실/crash가 나도 cold restart가
deterministic handoff selection과 live marker를 찾아 adopt한다. `record-github-outcome`은 verified
live URL/number/body/head, sealed authenticated actor, PR head repository owner/name와
intent/manifest hash를
no-clobber outcome receipt에 쓰고 selection state를 CAS한다. existing exact receipt 재사용만
idempotent하다. `verify-live-pr`/`verify-live-comment`는 `gh`가 다시 조회한 JSON을 sealed body,
terminal/evidence text, URL/number, exact authenticated `author.login` 또는 `user.login`, PR head
repository owner/name와 head/base에 비교하며 mismatch면 후속 mutation을 중단한다.

- [ ] **Step 4: tests를 실행한다.**

`test_run_evidence.py`의 shell-harness fixture는 이 plan의 모든 multi-command `bash` fence가
첫 command로 `set -euo pipefail`을 갖는지 파싱한다. synthetic Gradle/Python/validator/`gh` command
각각을 중간에 실패시키고 뒤의 command가 성공해도 block exit가 non-zero인지 검증한다.
status-preserving detached cleanup harness는 cleanup이 항상 실행되며 original non-zero 또는
cleanup failure가 최종 status로 남는지 확인한다. 각 fence를 clean bash environment로 분리해
모든 `ISSUE757_*` reference가 같은 fence에서 먼저 assignment/re-hydration됐는지 dataflow
검사하고 undeclared external 또는 cross-fence shell memory 의존을 거부한다.

```bash
set -euo pipefail
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
git diff --check
```

- [ ] **Step 5: Lore commit.** Intent: `Keep repeated benchmark evidence replacement auditable`.

## Task 8: candidate measurement exact head를 검증·커밋한다

**Files:** candidate source/test/benchmark files only; promoted docs/evidence는 아직 변경하지 않는다.

- [ ] **Step 1: candidate source를 먼저 Lore commit한다.**

```bash
set -euo pipefail
git diff --check
test -z "$(git status --porcelain)"
```

remaining source changes를 commit한다. Intent:
`Make the Lettuce allocation candidate reproducible`. 이 commit/tree가 immutable
measurement-source authority다. 이후 build, ABI capture, functional receipt와 JMH는 별도 clean
detached checkout에서 이 exact authority에 귀속한다. tracked payload/root는 아직 없는 final
delivery head/tree를 기록하지 않는다.

- [ ] **Step 2: candidate verification receipt command를 실행한다.**

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-candidate \
  --measurement-source HEAD \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json \
  --abi-baseline-commit 4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88 \
  --abi-baseline-tree 086f83baa7eec0cd68e68fff132542ef6db0f200
```

runner는 baseline과 measurement-source를 서로 다른 새 detached checkout/empty build root에
materialize한다. pre/post tracked/non-ignored clean, stale ignored output absence, root
distinctness와 symlink containment를 확인하고 같은 Kotlin compiler/Gradle/JDK/exact
`$JAVA_HOME/bin/javap` binary로 Task 1의 paired structural/verbose raw output과 payload manifest를
capture한다. JAR exact entry를 추출해 standalone class와 SHA-256이 같은지 확인한 뒤 validator에
manifest 하나만 전달한다. 이어 다른 container/heavy work가 없음을 확인하고 아래 argv를
exact measurement-source checkout에서 순차 실행한다. container-capable Gradle invocation은
모두 `--no-parallel`이다.

```text
./gradlew :bluetape4k-lettuce:test --tests io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:test --tests io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:test --tests io.bluetape4k.protobuf.serializers.redis.LettuceProtobufCodecsTest --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :bluetape4k-lettuce:clean :bluetape4k-lettuce:build --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:clean :bluetape4k-protobuf:build --no-parallel --no-configuration-cache
./gradlew :protobuf-codec-benchmark:clean :protobuf-codec-benchmark:test :protobuf-codec-benchmark:benchmarkBenchmarkCompile :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
./gradlew :bluetape4k-lettuce:detekt :bluetape4k-protobuf:detekt --no-configuration-cache
```

- [ ] **Step 3: receipt와 clean exact head를 검증한다.**

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-verification \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json \
  --authority-revision HEAD
test -z "$(git status --porcelain)"
ISSUE757_RECEIPT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  selected-receipt --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json)
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py inspect-verification \
  --verification-receipt "$ISSUE757_RECEIPT" \
  --field benchmark_jar_path
```

Expected: receipt의 measurement-source commit/tree, ABI payload/result/root, class/JAR/raw-output,
toolchain, benchmark JAR와 command/log hashes가 detached authority와 일치한다. 현재 branch HEAD는
measurement-source와 exact-equal이어야 한다. 이 receipt는 rejected terminal에서도 immutable
negative evidence identity이며 final head를 기록하지 않는다.

## Task 9: canonical JMH를 두 번 순차 실행하고 terminal을 선택한다

**Files:** `benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/**` ignored runtime evidence.

- [ ] **Step 1: 환경 gate.** 각 canonical run 직전에 다른 heavy work, Gradle daemon build,
  container suite가 없는지 runner로 확인하고 exact state/JAR identity에 묶인 fresh environment
  receipt를 만들 수 있을 때만 진행한다.

- [ ] **Step 2: pinned JAR state를 만든다.**

```bash
set -euo pipefail
ISSUE757_RECEIPT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  selected-receipt \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json)
test -f "$ISSUE757_RECEIPT"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --verification-receipt "$ISSUE757_RECEIPT"
```

- [ ] **Step 3: canonical profile을 서로 다른 run ID로 두 번 순차 실행한다.**

```bash
set -euo pipefail
ISSUE757_RUN1_ENV=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py record-environment-gate \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence)
test -f "$ISSUE757_RUN1_ENV"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --profile canonical \
  --environment-receipt "$ISSUE757_RUN1_ENV" \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence
ISSUE757_RUN2_ENV=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py record-environment-gate \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence)
test -f "$ISSUE757_RUN2_ENV"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --profile canonical \
  --environment-receipt "$ISSUE757_RUN2_ENV" \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence
```

- [ ] **Step 4: compare/validation을 실행한다.**

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py compare \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --output benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/comparison.csv \
  --validation benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/validation.json
```

- [ ] **Step 5: terminal을 기계적으로 선택한다.**

1. Lettuce eligible cell 하나라도 두 run 모두 symmetric regression → `rejected-after-regression`.
2. 그 외 Lettuce cell 하나라도 두 run 모두 acceptance → `retained-accepted`; positive text는 해당 cell만.
3. 그 외 → `retained-inconclusive`; positive text 금지.

runner state와 comparison validation의 `delivery_terminal`이 같은지 확인한다. committed
source of truth는 후속 manifest/report이며 수동 terminal 파일을 만들지 않는다.

## Task 10: 선택된 terminal로 final source를 만든다

### Branch A — `retained-accepted` / `retained-inconclusive`

- [ ] Task 2-4 production subtype, `open` extension point, direct-writer tests를 유지한다.
- [ ] Task 8 candidate receipt를 `validate-verification`으로 다시 확인한다. production 또는
  benchmark input이 바뀌었다면 기존 receipt를 재사용하지 않고 source를 commit한 뒤 새
  receipt와 Task 9 two-run lifecycle을 처음부터 수행한다.
- [ ] accepted는 accepted cell만 positive, inconclusive는 neutral-only metadata를 사용한다.

### Branch B — `rejected-after-regression`

- [ ] **Step B0: rollback reviewer policy를 계약보다 먼저 고정한다.** runner는 사용자에게
  `kind=rollback`, repository `bluetape4k/bluetape4k-projects`, issue `757`, expected distinct
  GitHub reviewer login, current authenticated runner actor, validated absolute regular-file `gh`
  path/SHA-256, marker schema `issue757-approval:v1`과 exact contract binding rule을 담은 canonical
  `.omx/evidence/issue-757-lettuce/rollback-approval-policy-input.json`의 전체 내용과 SHA-256을
  제시한다. 이 exact policy input에 대한 fresh explicit user approval을 받은 뒤에만 다음 command를
  실행하고, 사용자가 승인한 64자리 lowercase SHA-256을 command stdin에 그대로 입력한다. command가
  자체 계산한 현재 file hash를 승인 값 대신 사용해서는 안 된다. expected reviewer가 runner actor와
  같거나 distinct reviewer를 지정할 수 없으면 source
  mutation 없이 blocked로 멈춘다.

```bash
set -euo pipefail
IFS= read -r ISSUE757_APPROVED_POLICY_SHA256
case "$ISSUE757_APPROVED_POLICY_SHA256" in
  (*[!0-9a-f]*|'') exit 1 ;;
esac
test "${#ISSUE757_APPROVED_POLICY_SHA256}" -eq 64
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  pin-github-approval-policy \
  --kind rollback \
  --input .omx/evidence/issue-757-lettuce/rollback-approval-policy-input.json \
  --expected-input-sha256 "$ISSUE757_APPROVED_POLICY_SHA256" \
  --gh-binary "$(command -v gh)" \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-approval-policy.json
ISSUE757_APPROVAL_POLICY=$(python3 \
  benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-github-approval-policy --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-approval-policy.json \
  --field policy_receipt_path)
test -f "$ISSUE757_APPROVAL_POLICY"
```

- [ ] **Step B1: candidate authority와 canonical rollback contract를 immutable하게 고정한다.**

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py prepare-rollback-contract \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --dispatch lettuce_encode \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --approval-policy-selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-approval-policy.json \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json
ISSUE757_ROLLBACK_ROOT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-selection --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json \
  --field rollback_root)
ISSUE757_CONTRACT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-selection --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json \
  --field contract_path)
test -d "$ISSUE757_ROLLBACK_ROOT"
test -f "$ISSUE757_CONTRACT"
```

contract는 candidate measurement-source commit/tree와 governed implementation/test exact path
set, 각 path의 pre/post blob SHA-256 또는 required deletion, approval policy selection-state path/hash,
selected receipt path/hash와 approved input SHA-256, repository/issue/reviewer/runner actor/marker
schema/binding rule 및 pinned `gh` binary path/hash identity를 bind한다.
contract는 자기 hash,
아직 없는 rollback-source/final head/tree를 포함하지 않는다. approval receipt와 contract
path/hash는 root index가 production rollback allowlist와 분리해 exact 열거한다.

- [ ] **Step B1a: exact rollback contract의 fresh approval을 받는다.** contract SHA-256,
  candidate commit/tree, governed path set과 measured regression terminal을 사용자에게 제시하고
  fresh explicit approval을 받은 뒤 contract에 pinned된 distinct GitHub reviewer가 #757에 exact
  approval marker를 게시해야 한다. runner는 marker를 게시하지 않으며 다음 command는 live comment를
  가져와 검증할 뿐이다. reviewer가 current `gh api user`와 같거나 marker가 없으면 blocked로 멈춘다.

```bash
set -euo pipefail
ISSUE757_CONTRACT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-selection --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json \
  --field contract_path)
test -f "$ISSUE757_CONTRACT"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py import-github-approval \
  --kind rollback \
  --contract "$ISSUE757_CONTRACT" \
  --repository bluetape4k/bluetape4k-projects \
  --issue 757 \
  --gh-binary "$(command -v gh)" \
  --approval-selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-approval.json
ISSUE757_APPROVAL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-approval --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-approval.json \
  --field approval_receipt_path)
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-rollback-approval \
  --contract "$ISSUE757_CONTRACT" \
  --gh-binary "$(command -v gh)" \
  --approval-receipt "$ISSUE757_APPROVAL"
```

- [ ] **Step B2: contract와 exact 일치하도록 pre-release source를 rollback한다.** optimized
subtype/dispatch, private writer seam, retained-only class-open/ordinary-method-final ABI delta와
implementation-only tests를 제거하고 target/factory KDoc를 generic copied behavior로 복원한다.
rejected source-to-doc validation을 위해 target KDoc에 exact term `generic copied behavior`와 공통
failure-aftercare contract markers를 유지하고 factory KDoc에도 `generic copied behavior` 및 공통
factory contract markers를 유지한다.
generic factory/codec compatibility/security tests, benchmark/validator/archive code, candidate
negative evidence identity는 유지한다. governed set 밖 production/build/test path를 바꾸지 않는다.
첫 파일 mutation 직전에 같은 pinned `--gh-binary "$(command -v gh)"`를 전달해
`verify-rollback-approval`을 다시 실행하며 receipt 또는 binary/comment identity가 바뀌면 아무
파일도 수정하지 않는다.

- [ ] **Step B3: immutable clean rollback-source Lore commit을 만든다.** Intent:
`Remove the Lettuce dispatch after confirmed allocation regression`. `Rejected` trailer에 measured
direct dispatch와 공식/두 run을 기록한다. candidate measurement-source는 이 commit의
ancestor여야 한다.

- [ ] **Step B4: rollback-source를 detached build로 검증하고 bundle을 finalize한다.**

```bash
set -euo pipefail
ISSUE757_CONTRACT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-selection --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json \
  --field contract_path)
ISSUE757_APPROVAL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-approval --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-approval.json \
  --field approval_receipt_path)
test -f "$ISSUE757_CONTRACT"
test -f "$ISSUE757_APPROVAL"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-rollback-source \
  --contract "$ISSUE757_CONTRACT" \
  --rollback-source HEAD \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-verification.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-verification \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-verification.json \
  --authority-revision HEAD
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py finalize-rollback \
  --contract "$ISSUE757_CONTRACT" \
  --approval-receipt "$ISSUE757_APPROVAL" \
  --gh-binary "$(command -v gh)" \
  --rollback-selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json \
  --verification-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-verification.json
```

Expected: rollback-source가 candidate descendant이고 contract exact path/blob/deletion set과
일치한다. relevant ABI는 immutable baseline과 raw flags까지 exact-equal이며 optimized subtype은
class directory/JAR/reflection에서 absent다. generic compatibility/security와 crafted decode
ClassLoader tests가 통과한다.

- [ ] **Step B5: finalized bundle로 별도 post-removal state를 만들고 canonical run 두 번을 다시 수집한다.**

```bash
set -euo pipefail
ISSUE757_BUNDLE=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-rollback-selection --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-selection.json \
  --field rollback_bundle_path)
test -f "$ISSUE757_BUNDLE"
ISSUE757_ROLLBACK_RECEIPT=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  selected-receipt \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-rollback-verification.json)
test -f "$ISSUE757_ROLLBACK_RECEIPT"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --rollback-bundle "$ISSUE757_BUNDLE" \
  --verification-receipt "$ISSUE757_ROLLBACK_RECEIPT"
ISSUE757_POST_RUN1_ENV=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py record-environment-gate \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal)
test -f "$ISSUE757_POST_RUN1_ENV"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --profile canonical \
  --environment-receipt "$ISSUE757_POST_RUN1_ENV" \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal
ISSUE757_POST_RUN2_ENV=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py record-environment-gate \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal)
test -f "$ISSUE757_POST_RUN2_ENV"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --profile canonical \
  --environment-receipt "$ISSUE757_POST_RUN2_ENV" \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py compare \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --output benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/comparison.csv \
  --validation benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/validation.json
```

final comparison에서 removed Lettuce cells는 ineligible이고 candidate archive가 authenticated
chain에 남아야 한다. delivery terminal은 post-removal comparison으로 다시 선택하지 않고
authenticated rollback bundle의 `rejected-after-regression` 결정을 유지한다.

### 공통 finalization

- [ ] `git status --short`가 clean인지 확인한다. retained의 immutable source authority는 Task 8
  measurement-source이고, rejected의 immutable source authority는 candidate measurement-source의
  descendant인 B3 rollback-source다. docs/evidence final head는 아직 정해지지 않으며 tracked
  payload/root에 기록하지 않는다. evidence promotion 전에 source/build/test/benchmark input
  change가 있으면 해당 terminal의 verification/two-run lifecycle을 새 authority에서 다시
  수행한다.

## Task 11: evidence를 archive-aware promote하고 문서를 동기화한다

**Files:**

- Modify: `docs/benchmarks/raw/issue-757/**`
- Modify: `docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md`
- Modify: `docs/benchmarks/README.md`
- Modify: `benchmark/protobuf-codec-benchmark/README.md`
- Modify: `benchmark/protobuf-codec-benchmark/README.ko.md`
- Modify: `docs/manual/en/modules/protobuf-codec-benchmark.md`
- Modify: `docs/manual/ko/modules/protobuf-codec-benchmark.md`
- Modify: `io/protobuf/README.md`
- Modify: `io/protobuf/README.ko.md`
- Modify: `docs/manual/en/modules/bluetape4k-protobuf.md`
- Modify: `docs/manual/ko/modules/bluetape4k-protobuf.md`
- Modify: `infra/lettuce/README.md`
- Modify: `infra/lettuce/README.ko.md`
- Modify: `docs/manual/en/modules/bluetape4k-lettuce/codecs-and-serialization.md`
- Modify: `docs/manual/ko/modules/bluetape4k-lettuce/codecs-and-serialization.md`
- Modify: `docs/security/serialization-trust-profiles.md`
- Create: `docs/operations/issue-757-lettuce-protobuf-recovery.md`
- Create: `docs/operations/templates/issue-757-lettuce-protobuf-recovery.json`
- Modify: `CHANGELOG.md`

**Skills:** `bluetape-writer` for paired docs. Public KDoc/README English, locale counterpart Korean.

- [ ] **Step 1: fully validated staging을 새 immutable generation으로 publish한다.**

terminal에 맞는 verified state를 먼저 고정한다.

```bash
set -euo pipefail
ISSUE757_TERMINAL=$(python3 -c \
  'import json; print(json.load(open("benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json"))["delivery_terminal"])')
case "$ISSUE757_TERMINAL" in
  retained-accepted|retained-inconclusive)
    ISSUE757_STATE=benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json
    ;;
  rejected-after-regression)
    ISSUE757_STATE=benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json
    ;;
  *) exit 1 ;;
esac
test -f "$ISSUE757_STATE"
```

```bash
set -euo pipefail
ISSUE757_TERMINAL=$(python3 -c \
  'import json; print(json.load(open("benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json"))["delivery_terminal"])')
case "$ISSUE757_TERMINAL" in
  retained-accepted|retained-inconclusive)
    ISSUE757_STATE=benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json
    ;;
  rejected-after-regression)
    ISSUE757_STATE=benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json
    ;;
  *) exit 1 ;;
esac
test -f "$ISSUE757_STATE"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py publish-generation \
  --state "$ISSUE757_STATE" \
  --destination-root docs/benchmarks/raw/issue-757 \
  --generations-root docs/benchmarks/raw/issue-757/generations \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --lock .omx/evidence/issue-757-lettuce/promotion.lock \
  --token-root .omx/evidence/issue-757-lettuce/promotion-tokens
```

`publish-generation` success는 lock/fencing token, payload/root/semantic/archive/file-set 검증,
staged file/directory fsync, platform atomic no-replace generation rename, generation-parent fsync,
previous-pointer CAS, pointer temp fsync/rename/parent fsync가 모두 통과했다는 증거다. non-zero면
`validate-committed --active-pointer ...`로 current pointer를 확인한다. old면 downstream을
중단하고 exact receipt로 재개하며, exact intended new면 lock 아래 parent fsync와 completion
adoption을 수행한 뒤 success receipt를 얻어야만 downstream을 계속한다. 다른 pointer는
fail-closed한다. Expected: 이전 active generation regular files만 새 generation의
`archive/<old-delivery-commit>/`에 한 번 보존되고 기존 archive는 재귀 복제되지 않는다.
immutable old/new generation은 삭제하지 않는다.

- [ ] **Step 2: manifest에서 report를 재생성·검증한다.**

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py render-report \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --output docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
```

- [ ] **Step 3: terminal별 문구를 적용한다.**

- retained-accepted: accepted cell의 B/op만 manifest-derived positive claim.
- retained-inconclusive: direct behavior는 설명하되 allocation improvement 문구 없음.
- rejected: candidate regression과 dispatch removal을 neutral/negative로 기록; direct-write 완료, `Closes #757`, #898 완료 문구 금지.

README는 숫자를 복제하지 않고 report를 링크한다. Task 1-2에서 측정 전에 고정한 KDoc와
일치하도록 custom-prefix/compressed/decode/fallback compatibility path를 설명하되 production
Kotlin source는 이 task에서 수정하지 않는다.

English/Korean README와 manual pair에 동일한 실행 의미의 예제를 넣는다.

`infra/lettuce/README.md`/`README.ko.md`와 Lettuce
`codecs-and-serialization.md` 영·한 pair에는 public `LettuceBinaryCodec`의 sole supported source
extension seam이 nullable target-taking overload 하나뿐임을 기록한다. ordinary methods는 final이며
class open으로 JVM synthetic bridge overrideability가 생기는 ABI caveat, caller-owned target,
success-only index commit, failure 시 capacity/attempted range aftercare와 retained/rejected terminal별
seam 존재 여부를 KDoc와 동일하게 설명한다.

- default allowlist 안 message의 기존 zero-argument Kotlin factory 사용
- trusted-internal opt-in warning과 shared/untrusted boundary에서 사용 금지
- custom prefix `ProtobufSerializer(allowedClassPrefixes=...)`를 generic
  `LettuceBinaryCodec`으로 감싸는 caller-owned compatibility path
- unchanged compressed factory 사용
- Java의 `LettuceProtobufCodecs.INSTANCE.protobuf()` 호출
- target-taking Lettuce `ByteBuf` direct path와 unchanged single-argument `ByteBuffer`
  encode/decode path 구분
- target encode 실패 시 index commit이 없더라도 capacity와 attempted range는 rollback되지 않아
  caller가 range를 clear/reinitialize하거나 buffer를 폐기해야 하는 failure-aftercare
- retained terminal은 caller migration 없음, rejected terminal은 generic copied behavior 유지

outside-default-prefix `MyMessage`가 zero-argument factory로 곧바로 round-trip하는 예시는
금지한다.

`docs/operations/issue-757-lettuce-protobuf-recovery.md`에는 pre-release rejected rollback과
post-release dispatch-only recovery를 별도 절로 분리한다. 후자는 published ABI 제거와
pre-release contract 재사용을 금지하고, `published-retained-vs-recovery` command, #757 reopen,
published Maven authority, pre/post/close checklist field, metric/owner/approval/digest/observation
gate, 별도 redeploy 승인과 recovery close 조건을 exact command 예제와 함께 기록한다. recovery
첫 단계는 repository/issue, expected distinct reviewer, current runner actor, validated absolute
regular-file `gh` path/SHA-256, marker schema와 proposal binding rule을 담은 exact policy input 및
SHA-256을 사용자에게 제시하고 fresh explicit approval을
받은 뒤 그 approved SHA를 `--expected-input-sha256`에, validated `gh` path를 `--gh-binary`에 전달해
`pin-github-approval-policy --kind recovery`로 immutable policy receipt를 고정하는 것이다.
그 selected receipt를 `--approval-policy-selection-state`로 bind하기 전에는
`--phase pre-dispatch-proposal`을 실행하지 않는다.
post-release 절 바로 앞에는 machine anchor `<!-- issue-757-post-release-recovery -->`를 두고 모든
실행 예제는 `bash` fence와 `set -euo pipefail`로 작성한다. Task 11 validator는 이 anchor 이후
명령의 policy pin → proposal → import → live verify → finalize → published fetch 순서, 필수
`--expected-input-sha256`/`--gh-binary`/`--approval-policy-selection-state`, 별도 deploy gate 뒤
post-dispatch의 `--pre-dispatch-selection-state`, observation gate 뒤 close의
`--post-dispatch-selection-state`와 전체 순서/shell syntax를 검사한다.
post-release `bash` fence는 strict-mode 선언, canonical direct `python3 run-evidence.py` lifecycle과
ABI validator invocation만 허용하고 `source`, `trap`, 추가 `set`, wrapper 또는 임의 command를
금지한다. external GitHub/redeploy action은 executable fence 밖의 승인 gate prose로 유지한다.
recovery 문서 전체의 fence는 container에 중첩하지 않은 top-level `bash`만 허용하고, fence 밖
본문은 validator에 고정한 exact heading, exact 한국어 문장 또는 지정 machine anchor만 허용한다.
pre-release 절에는 executable fence를 두지 않고 Task 10 contract를 prose로 참조한다. blockquote/list
container prefix는 반복 정규화하며 indented code, command-shaped plaintext와 승인되지 않은 문구를
거부한다. 운영 문구 변경은 validator의 exact prose identity와 함께 review한다.
동반 JSON template은 `validate-recovery-checklist --phase template`이 pre/post/close required
schema와 phase transition을 검증할 수 있게 하며 실제 incident 값/approval/digest를 선기록하지
않는다.

- [ ] **Step 4: locale/doc parity와 placeholders를 검사한다.**

```bash
set -euo pipefail
if rg -n 'TO''DO|T''BD|FIX''ME|PLACE''HOLDER' \
  infra/lettuce io/protobuf benchmark/protobuf-codec-benchmark docs/manual docs/benchmarks docs/operations CHANGELOG.md; then
  exit 1
fi
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  validate-recovery-checklist \
  --phase template \
  --checklist docs/operations/templates/issue-757-lettuce-protobuf-recovery.json
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/sync_navigation_metadata.rb --check
ruby scripts/manual/export_manifest.rb --check
python3 - <<'PY'
import re
import shlex
import subprocess
from pathlib import Path

def read_utf8_exact(path):
    return path.read_bytes().decode("utf-8")

pairs = [
    (
        Path("infra/lettuce/README.md"),
        Path("infra/lettuce/README.ko.md"),
        ("LettuceBinaryCodec", "encodeValue", "ByteBuf", "writerIndex",
         "synthetic", "bridge"),
        True,
        ("kotlin",),
    ),
    (
        Path("docs/manual/en/modules/bluetape4k-lettuce/codecs-and-serialization.md"),
        Path("docs/manual/ko/modules/bluetape4k-lettuce/codecs-and-serialization.md"),
        ("LettuceBinaryCodec", "encodeValue", "ByteBuf", "writerIndex",
         "synthetic", "bridge"),
        False,
        ("kotlin",),
    ),
    (
        Path("io/protobuf/README.md"),
        Path("io/protobuf/README.ko.md"),
        ("LettuceProtobufCodecs", "trustedInternalProtobuf", "allowedClassPrefixes",
         "LettuceBinaryCodec", "ByteBuf", "ByteBuffer", "gzipProtobuf", "INSTANCE"),
        True,
        ("kotlin", "java"),
    ),
    (
        Path("docs/manual/en/modules/bluetape4k-protobuf.md"),
        Path("docs/manual/ko/modules/bluetape4k-protobuf.md"),
        ("LettuceProtobufCodecs", "trustedInternalProtobuf", "allowedClassPrefixes",
         "LettuceBinaryCodec", "ByteBuf", "ByteBuffer", "gzipProtobuf", "INSTANCE"),
        False,
        ("kotlin", "java"),
    ),
    (
        Path("benchmark/protobuf-codec-benchmark/README.md"),
        Path("benchmark/protobuf-codec-benchmark/README.ko.md"),
        ("lettuceEncodeHeapCopied", "lettuceEncodeHeapOptimized",
         "lettuceEncodeDirectCopied", "lettuceEncodeDirectOptimized",
         "gc.alloc.rate.norm", "scoreError", "retained-inconclusive"),
        True,
        ("bash",),
    ),
    (
        Path("docs/manual/en/modules/protobuf-codec-benchmark.md"),
        Path("docs/manual/ko/modules/protobuf-codec-benchmark.md"),
        ("lettuceEncodeHeapCopied", "lettuceEncodeHeapOptimized",
         "lettuceEncodeDirectCopied", "lettuceEncodeDirectOptimized",
         "gc.alloc.rate.norm", "scoreError", "retained-inconclusive"),
        False,
        ("bash",),
    ),
]
def fenced_examples(markdown):
    assert all(
        character in "\n\t"
        or (
            ord(character) >= 0x20
            and ord(character) != 0x7F
            and character not in "\u0085\u2028\u2029"
        )
        for character in markdown
    )
    examples = []
    current = None
    for raw in markdown.split("\n"):
        if current is not None:
            closing = re.fullmatch(r"(?P<marker>`{3,}|~{3,})[ \t]*", raw)
            if (
                closing is not None
                and set(closing.group("marker")) == {current[0]}
                and len(closing.group("marker")) >= current[1]
            ):
                examples.append((current[2], "\n".join(current[3])))
                current = None
                continue
            current[3].append(raw)
            continue
        opening = re.fullmatch(
            r"(?P<marker>`{3,}|~{3,})(?P<info>.*)",
            raw,
        )
        if re.search(r"`{3,}|~{3,}", raw):
            assert opening is not None
        if opening is None:
            continue
        marker = opening.group("marker")
        info = opening.group("info").strip()
        if marker[0] == "`":
            assert "`" not in info
        language = info.split(maxsplit=1)[0].lower() if info else ""
        language = {
            "kt": "kotlin",
            "sh": "bash",
            "shell": "bash",
        }.get(language, language)
        assert language != "kts"
        current = [marker[0], len(marker), language, []]
    assert current is None
    return examples

assert fenced_examples("~~~~java title=Example\ncall();\n~~~~") == [
    ("java", "call();"),
]
assert fenced_examples("```kt\ncall()\n```") == [("kotlin", "call()")]
assert fenced_examples("```bash\n\necho ok\n```") == [("bash", "\necho ok")]

def must_reject_fenced_examples(markdown):
    try:
        fenced_examples(markdown)
    except AssertionError:
        return
    raise AssertionError("non-top-level documentation fence was accepted")

must_reject_fenced_examples("> ```java\n> wrong();\n> ```")
must_reject_fenced_examples("- ~~~bash\n  wrong\n  ~~~")
must_reject_fenced_examples("- Example:\n  ```java\n  wrong();\n  ```")
must_reject_fenced_examples("    ```java\nwrong();\n    ```")

for english, korean, required, reciprocal_readme, required_languages in pairs:
    left, right = read_utf8_exact(english), read_utf8_exact(korean)
    assert left.count("\n## ") == right.count("\n## ")
    assert left.count("\n### ") == right.count("\n### ")
    assert all(token in left and token in right for token in required)
    if reciprocal_readme:
        assert "](./README.ko.md)" in left
        assert "](./README.md)" in right
    left_examples = fenced_examples(left)
    right_examples = fenced_examples(right)
    assert left_examples and left_examples == right_examples
    assert all(body.strip() for _, body in left_examples)
    assert set(required_languages).issubset(
        {language for language, _ in left_examples}
    )

codec_source = Path(
    "infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/"
    "LettuceBinaryCodec.kt"
)
codec_source = read_utf8_exact(codec_source)
lettuce_docs = [
    read_utf8_exact(Path("infra/lettuce/README.md")),
    read_utf8_exact(Path("infra/lettuce/README.ko.md")),
    read_utf8_exact(Path("docs/manual/en/modules/bluetape4k-lettuce/codecs-and-serialization.md")),
    read_utf8_exact(Path("docs/manual/ko/modules/bluetape4k-lettuce/codecs-and-serialization.md")),
]
common_aftercare = (
    "caller-owned",
    "writerIndex",
    "capacity",
    "attempted range",
    "clear or reinitialize",
    "discard",
)
assert all(token in codec_source for token in common_aftercare)
assert all(all(token in doc for token in common_aftercare) for doc in lettuce_docs)
if "open class LettuceBinaryCodec" in codec_source:
    retained_contract = (
        "sole supported source extension seam",
        "synthetic bridge",
        "success-only",
    )
    assert all(token in codec_source for token in retained_contract)
    assert all(all(token in doc for token in retained_contract) for doc in lettuce_docs)
else:
    assert "generic copied behavior" in codec_source
    assert all(
        "generic copied behavior" in doc and "rejected-after-regression" in doc
        for doc in lettuce_docs
    )

factory_source = Path(
    "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/"
    "LettuceProtobufCodecs.kt"
)
factory_source = read_utf8_exact(factory_source)
protobuf_docs = [
    read_utf8_exact(Path("io/protobuf/README.md")),
    read_utf8_exact(Path("io/protobuf/README.ko.md")),
    read_utf8_exact(Path("docs/manual/en/modules/bluetape4k-protobuf.md")),
    read_utf8_exact(Path("docs/manual/ko/modules/bluetape4k-protobuf.md")),
]
java_factory_statement = "LettuceProtobufCodecs.INSTANCE.protobuf();"

def java_code_only(source):
    output = []
    index = 0
    state = "code"
    while index < len(source):
        character = source[index]
        if state == "code":
            if source.startswith("//", index):
                output.extend((" ", " "))
                index += 2
                state = "line-comment"
                continue
            if source.startswith("/*", index):
                output.extend((" ", " "))
                index += 2
                state = "block-comment"
                continue
            if source.startswith('\"\"\"', index):
                output.extend((" ", " ", " "))
                index += 3
                state = "text-block"
                continue
            if character == '\"':
                output.append(" ")
                index += 1
                state = "string"
                continue
            if character == "'":
                output.append(" ")
                index += 1
                state = "char"
                continue
            output.append(character)
            index += 1
            continue
        if state == "line-comment":
            output.append("\n" if character == "\n" else " ")
            index += 1
            if character == "\n":
                state = "code"
            continue
        if state == "block-comment":
            if source.startswith("*/", index):
                output.extend((" ", " "))
                index += 2
                state = "code"
            else:
                output.append("\n" if character == "\n" else " ")
                index += 1
            continue
        if state == "text-block":
            if source.startswith('\"\"\"', index):
                output.extend((" ", " ", " "))
                index += 3
                state = "code"
            else:
                output.append("\n" if character == "\n" else " ")
                index += 1
            continue
        if character == "\\" and index + 1 < len(source):
            output.extend((" ", " "))
            index += 2
            continue
        output.append("\n" if character == "\n" else " ")
        index += 1
        if (state == "string" and character == '\"') or (
            state == "char" and character == "'"
        ):
            state = "code"
    assert state in {"code", "line-comment"}
    return "".join(output)

def validated_java_code(source):
    assert '\"\"\"' not in source
    assert re.search(r"\\u+[0-9A-Fa-f]{4}", source) is None
    return java_code_only(source)

def must_reject_java_code(source):
    try:
        validated_java_code(source)
    except AssertionError:
        return
    raise AssertionError("unsafe Java documentation example was accepted")

must_reject_java_code(
    'String ignored = \"\"\"\n' + java_factory_statement + '\n\"\"\";'
)
must_reject_java_code(
    r"\u002f\u002a" + java_factory_statement + r"\u002a\u002f"
)
assert all(
    any(
        language == "java"
        and java_factory_statement in {
            line.strip(" \t") for line in validated_java_code(body).split("\n")
        }
        for language, body in fenced_examples(doc)
    )
    for doc in protobuf_docs
)

def combined_executable_code(markdown, language):
    bodies = [
        body
        for example_language, body in fenced_examples(markdown)
        if example_language == language
    ]
    assert bodies
    if language in {"kotlin", "java"}:
        assert all('\"\"\"' not in body for body in bodies)
        if language == "kotlin":
            assert all("/*" not in body and "*/" not in body for body in bodies)
            assert all(not body.lstrip(" \t\n").startswith("#!") for body in bodies)
        return "\n".join(java_code_only(body) for body in bodies)
    assert language == "bash"
    for body in bodies:
        subprocess.run(["bash", "-n"], input=body, text=True, check=True)
    executable_lines = [
        line
        for body in bodies
        for line in body.split("\n")
        if line.strip(" \t") and not line.lstrip(" \t").startswith("#")
    ]
    assert executable_lines
    return "\n".join(executable_lines)

assert all(
    re.search(r"(?<![\w.])LettuceBinaryCodec\s*\(", combined_executable_code(doc, "kotlin"))
    for doc in lettuce_docs
)
protobuf_kotlin_patterns = (
    r"(?<![\w.])LettuceProtobufCodecs\.protobuf\(\)",
    r"(?<![\w.])LettuceProtobufCodecs\.trustedInternalProtobuf\(\)",
    r"(?<![\w.])LettuceProtobufCodecs\.gzipProtobuf\(\)",
    r"(?<![\w.])LettuceBinaryCodec\s*\(",
    r"\ballowedClassPrefixes\b",
)
assert all(
    all(
        re.search(pattern, combined_executable_code(doc, "kotlin"))
        for pattern in protobuf_kotlin_patterns
    )
    for doc in protobuf_docs
)
benchmark_docs = [
    read_utf8_exact(Path("benchmark/protobuf-codec-benchmark/README.md")),
    read_utf8_exact(Path("benchmark/protobuf-codec-benchmark/README.ko.md")),
    read_utf8_exact(Path("docs/manual/en/modules/protobuf-codec-benchmark.md")),
    read_utf8_exact(Path("docs/manual/ko/modules/protobuf-codec-benchmark.md")),
]
benchmark_methods = (
    "lettuceEncodeHeapCopied",
    "lettuceEncodeHeapOptimized",
    "lettuceEncodeDirectCopied",
    "lettuceEncodeDirectOptimized",
)
canonical_benchmark_command = (
    "python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run "
    "--state FILE --profile canonical --environment-receipt FILE "
    "--output-root PATH"
)
assert all(
    any(
        language == "bash" and body == canonical_benchmark_command
        for language, body in fenced_examples(doc)
    )
    and all(method in doc for method in benchmark_methods)
    and "--concurrent-heavy-work" not in doc
    for doc in benchmark_docs
)
factory_contract = (
    "ByteBuf",
    "unchanged ByteBuffer",
    "caller-owned",
    "failure-aftercare",
    "trusted-internal",
    "compressed",
    "custom-prefix",
)
assert all(token in factory_source for token in factory_contract)
assert all(all(token in doc for token in factory_contract) for doc in protobuf_docs)
if "DirectProtobufLettuceCodec.create" in factory_source:
    assert "direct ByteBuf target" in factory_source
    assert all(
        "direct ByteBuf target" in doc
        and "retained-accepted" in doc
        and "retained-inconclusive" in doc
        for doc in protobuf_docs
    )
else:
    assert "generic copied behavior" in factory_source
    assert all(
        "generic copied behavior" in doc and "rejected-after-regression" in doc
        for doc in protobuf_docs
    )

runner_script = "benchmark/protobuf-codec-benchmark/scripts/run-evidence.py"
abi_validator_script = "infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py"
recovery_doc = read_utf8_exact(
    Path("docs/operations/issue-757-lettuce-protobuf-recovery.md")
)
anchor = "<!-- issue-757-post-release-recovery -->"
assert recovery_doc.count(anchor) == 1
post_release = recovery_doc.split(anchor, 1)[1]
bash_blocks = re.findall(r"```bash\n(.*?)\n```", recovery_doc, flags=re.DOTALL)
assert bash_blocks
for block in bash_blocks:
    lines = block.split("\n")
    assert lines and lines[0] == "set -euo pipefail"
    subprocess.run(["bash", "-n"], input=block, text=True, check=True)

def validated_post_release_bash_blocks(markdown, require_complete=False):
    blocks = []
    current = None
    fence_char = None
    fence_length = 0
    approved_headings = {
        "# Issue 757 Lettuce Protobuf 복구 운영 가이드",
        "## 릴리스 전 rejected rollback",
        "## 릴리스 후 dispatch-only recovery",
        "### 승인 정책",
        "### 배포 전 검증",
        "### 배포 후 관찰",
        "### 종료 조건",
    }
    approved_prose = {
        "릴리스 전 rejected terminal은 Task 10의 immutable rollback contract와 별도 GitHub approval receipt만 사용한다.",
        "릴리스 전 rollback contract를 릴리스 후 recovery에 재사용하지 않는다.",
        "릴리스 후 recovery는 published public ABI를 제거하지 않고 dispatch-only change만 허용한다.",
        "#757을 reopen하고 incident, release, consumer, owner를 기록한다.",
        "정책 input과 SHA-256은 fresh user approval 뒤에만 pin한다.",
        "Runner와 reviewer는 서로 다른 GitHub identity여야 한다.",
        "외부 GitHub comment와 redeploy는 문서 명령이 수행하지 않으며 별도 승인 gate를 유지한다.",
        "Published baseline은 approved HTTPS Maven authority의 GAV, digest, release commit, tree, ref로 고정한다.",
        "Planned recovery는 coordinate, digest, command, target, environment, region으로 고정한다.",
        "Artifact fetch는 approved timeout과 byte cap, same-authority redirect, digest 검증을 적용한다.",
        "배포 전 proposal, approval, final receipt는 immutable no-clobber selection으로 연결한다.",
        "배포 후 actual identity는 approved planned recovery와 exact-equal이어야 한다.",
        "Observation window는 post-dispatch receipt가 성공한 뒤에만 시작한다.",
        "Close는 pinned metrics, retained ABI, compatibility, wire, security, Redis, allocation evidence가 모두 통과해야 한다.",
        "Missing data 또는 identity drift는 fail-closed한다.",
        "동반 JSON template은 실제 incident 값이나 approval을 미리 채우지 않는다.",
        "Template 검증은 schema만 확인하며 실행 승인이 아니다.",
        "Release, tag, publish, redeploy는 별도 승인을 유지한다.",
    }
    approved_outside_sequence = (
        "# Issue 757 Lettuce Protobuf 복구 운영 가이드",
        "## 릴리스 전 rejected rollback",
        "릴리스 전 rejected terminal은 Task 10의 immutable rollback contract와 별도 GitHub approval receipt만 사용한다.",
        "릴리스 전 rollback contract를 릴리스 후 recovery에 재사용하지 않는다.",
        anchor,
        "## 릴리스 후 dispatch-only recovery",
        "릴리스 후 recovery는 published public ABI를 제거하지 않고 dispatch-only change만 허용한다.",
        "#757을 reopen하고 incident, release, consumer, owner를 기록한다.",
        "### 승인 정책",
        "정책 input과 SHA-256은 fresh user approval 뒤에만 pin한다.",
        "Runner와 reviewer는 서로 다른 GitHub identity여야 한다.",
        "외부 GitHub comment와 redeploy는 문서 명령이 수행하지 않으며 별도 승인 gate를 유지한다.",
        "### 배포 전 검증",
        "Published baseline은 approved HTTPS Maven authority의 GAV, digest, release commit, tree, ref로 고정한다.",
        "Planned recovery는 coordinate, digest, command, target, environment, region으로 고정한다.",
        "Artifact fetch는 approved timeout과 byte cap, same-authority redirect, digest 검증을 적용한다.",
        "배포 전 proposal, approval, final receipt는 immutable no-clobber selection으로 연결한다.",
        "<bash-fence-pre-dispatch>",
        "### 배포 후 관찰",
        "배포 후 actual identity는 approved planned recovery와 exact-equal이어야 한다.",
        "<bash-fence-post-dispatch>",
        "Observation window는 post-dispatch receipt가 성공한 뒤에만 시작한다.",
        "### 종료 조건",
        "Close는 pinned metrics, retained ABI, compatibility, wire, security, Redis, allocation evidence가 모두 통과해야 한다.",
        "Missing data 또는 identity drift는 fail-closed한다.",
        "동반 JSON template은 실제 incident 값이나 approval을 미리 채우지 않는다.",
        "Template 검증은 schema만 확인하며 실행 승인이 아니다.",
        "Release, tag, publish, redeploy는 별도 승인을 유지한다.",
        "<bash-fence-close>",
    )
    seen_headings = set()
    seen_prose = set()
    seen_outside = []
    forbidden_plain = re.compile(
        r"^\s*(timeout|xargs|sudo|dash|command|env|builtin|nohup|python\d*|ruby|node|perl|java|git|"
        r"gh|curl|wget|bash|sh|zsh|"
        r"source|eval|exec|trap|rm|mv|cp|chmod|chown|touch|make|gradle|docker|kubectl|helm|ssh|scp|"
        r"rsync|jq|sed|awk|grep|rg|cat|printf|echo|true|false|\./|/)\b"
    )

    def strip_containers(line):
        value = line
        while True:
            previous = value
            value = re.sub(r"^\s{0,3}>\s?", "", value)
            value = re.sub(r"^\s{0,3}(?:[-*+]|\d+[.)])\s+", "", value)
            if value == previous:
                return value

    for line in markdown.split("\n"):
        fence = re.fullmatch(r"(?P<marker>`{3,}|~{3,})(?P<info>.*)", line)
        if re.search(r"`{3,}|~{3,}", line):
            assert fence is not None
        if fence:
            marker = fence.group("marker")
            info = fence.group("info")
            if current is None:
                assert info == "bash"
                current = []
                fence_char = marker[0]
                fence_length = len(marker)
            else:
                assert not info
                assert set(marker) == {fence_char} and len(marker) >= fence_length
                blocks.append("\n".join(current))
                fence_tokens = (
                    "<bash-fence-pre-dispatch>",
                    "<bash-fence-post-dispatch>",
                    "<bash-fence-close>",
                )
                assert len(blocks) <= len(fence_tokens)
                seen_outside.append(fence_tokens[len(blocks) - 1])
                current = None
                fence_char = None
                fence_length = 0
            continue
        if current is not None:
            current.append(line)
            continue
        assert not line.startswith(("    ", "\t"))
        assert runner_script not in line
        assert "<pre" not in line.lower() and "<code" not in line.lower()
        executable_candidate = strip_containers(line)
        assert not executable_candidate.startswith(("    ", "\t"))
        assert not forbidden_plain.match(executable_candidate)
        assert not re.match(r"^\s*set\s+[+-]", executable_candidate)
        if not executable_candidate:
            continue
        if re.match(r"^#{1,6}\s", executable_candidate):
            assert executable_candidate in approved_headings
            seen_headings.add(executable_candidate)
            seen_outside.append(executable_candidate)
            continue
        if executable_candidate == anchor:
            seen_outside.append(executable_candidate)
            continue
        assert executable_candidate in approved_prose
        seen_prose.add(executable_candidate)
        seen_outside.append(executable_candidate)
    if require_complete:
        assert seen_headings == approved_headings
        assert seen_prose == approved_prose
        assert tuple(seen_outside) == approved_outside_sequence
    assert current is None and blocks
    return blocks

pre_release = recovery_doc.split(anchor, 1)[0]
assert not re.search(r"`{3,}|~{3,}", pre_release)
recovery_bash_blocks = validated_post_release_bash_blocks(recovery_doc, require_complete=True)
for block in recovery_bash_blocks:
    lines = block.split("\n")
    assert lines and lines[0] == "set -euo pipefail"
    subprocess.run(["bash", "-n"], input=block, text=True, check=True)
post_release_bash_blocks = validated_post_release_bash_blocks(post_release)
for block in post_release_bash_blocks:
    lines = block.split("\n")
    assert lines and lines[0] == "set -euo pipefail"
    subprocess.run(["bash", "-n"], input=block, text=True, check=True)

def logical_argv(block):
    current = []
    for raw in block.split("\n"):
        assert all(
            (character in " \t") or (0x21 <= ord(character) <= 0x7E)
            for character in raw
        )
        line = raw.strip(" \t")
        if not line or line.startswith("#"):
            assert not current
            continue
        assert not re.search(r"\\[ \t]+$", raw)
        continued = raw.endswith("\\")
        current.append(line[:-1].rstrip() if continued else line)
        if not continued:
            command_text = " ".join(current)
            allowed_gh_lookup = '"$(command -v gh)"'
            assert command_text.count(allowed_gh_lookup) <= 1
            literal_text = command_text.replace(allowed_gh_lookup, "GH_BINARY")
            assert not any(token in literal_text for token in ("$", "`", "*", "?", "[", "]", "{", "}", "~"))
            lexer = shlex.shlex(command_text, posix=True, punctuation_chars=True)
            lexer.whitespace_split = True
            lexer.commenters = ""
            yield list(lexer)
            current = []
    assert not current

expected_commands = (
    "pin-github-approval-policy",
    "validate-recovery-checklist",
    "import-github-approval",
    "verify-recovery-approval",
    "finalize-recovery-pre-dispatch",
    "verify-published-recovery",
)
expected_lifecycle = (
    "pin-github-approval-policy",
    "validate-recovery-checklist:pre-dispatch-proposal",
    "import-github-approval",
    "verify-recovery-approval",
    "finalize-recovery-pre-dispatch",
    "verify-published-recovery",
    "abi-validator",
    "validate-recovery-checklist:post-dispatch",
    "validate-recovery-checklist:close",
)
allowed_runner_commands = set(expected_commands)
expected_block_lifecycle = (
    expected_lifecycle[:7],
    expected_lifecycle[7:8],
    expected_lifecycle[8:],
)

def extract_records(markdown):
    records = []
    blocks = validated_post_release_bash_blocks(markdown)
    assert len(blocks) == len(expected_block_lifecycle)
    for block_index, block in enumerate(blocks):
        block_records = []
        lines = block.split("\n")
        assert lines and lines[0] == "set -euo pipefail"
        subprocess.run(["bash", "-n"], input=block, text=True, check=True)
        parsed = list(logical_argv(block))
        strict = ["set", "-euo", "pipefail"]
        assert parsed and parsed[0] == strict
        assert sum(argv == strict for argv in parsed) == 1
        for argv in parsed[1:]:
            assert argv[0] not in {"alias", "unalias", "function"}
            assert argv[0] != "python3()"
            assert not any(
                token.startswith(("PATH=", "BASH_ENV=", "ENV="))
                for token in argv
            )
            script_positions = [
                index for index, token in enumerate(argv)
                if token.endswith("run-evidence.py")
            ]
            if script_positions:
                assert len(script_positions) == 1
                index = script_positions[0]
                assert argv[0] == "python3"
                assert index == 1 and argv[1] == runner_script
                assert index + 1 < len(argv)
                assert argv[index + 1] in allowed_runner_commands
                block_records.append((argv[index + 1], argv[index + 2:]))
                continue
            assert argv == [
                "python3", abi_validator_script, "validate", "--manifest", "OUTPUT",
            ]
            block_records.append(("abi-validator", argv[2:]))
        block_lifecycle = []
        for command, argv in block_records:
            if command == "validate-recovery-checklist":
                assert argv.count("--phase") == 1
                phase_index = argv.index("--phase")
                assert phase_index + 1 < len(argv)
                block_lifecycle.append(command + ":" + argv[phase_index + 1])
            else:
                block_lifecycle.append(command)
        assert tuple(block_lifecycle) == expected_block_lifecycle[block_index]
        records.extend(block_records)
    return records

def require_flags(argv, *flags):
    for flag in flags:
        assert argv.count(flag) == 1
        index = argv.index(flag)
        assert index + 1 < len(argv) and not argv[index + 1].startswith("--")

def flag_value(argv, flag):
    require_flags(argv, flag)
    return argv[argv.index(flag) + 1]

def require_exact_flags(argv, flags):
    assert len(argv) == len(flags) * 2
    assert tuple(argv[::2]) == flags
    assert all(value and not value.startswith("--") for value in argv[1::2])
    values_by_flag = {
        "--kind": {"recovery"},
        "--phase": {"pre-dispatch-proposal", "post-dispatch", "close"},
        "--repository": {"bluetape4k/bluetape4k-projects"},
        "--issue": {"757"},
        "--input": {"FILE"},
        "--expected-input-sha256": {"SHA256"},
        "--gh-binary": {"$(command -v gh)"},
        "--selection-state": {"FILE"},
        "--approval-policy-selection-state": {"FILE"},
        "--pre-dispatch-proposal": {"FILE"},
        "--approval-selection-state": {"FILE"},
        "--pre-dispatch-selection-state": {"FILE"},
        "--post-dispatch-selection-state": {"FILE"},
        "--evidence-root": {"PATH"},
        "--recovery-source": {"REV"},
        "--dispatch-contract": {"FILE"},
        "--abi-manifest": {"OUTPUT"},
    }
    for flag, value in zip(argv[::2], argv[1::2]):
        assert value in values_by_flag[flag]
    assert not ({"&&", "||", ";", "|", "&"} & set(argv))

def validate_recovery_records(records):
    lifecycle = []
    for command, argv in records:
        if command == "validate-recovery-checklist":
            phase = flag_value(argv, "--phase")
            assert phase in {"pre-dispatch-proposal", "post-dispatch", "close"}
            lifecycle.append((f"{command}:{phase}", argv))
        elif command in expected_commands or command == "abi-validator":
            lifecycle.append((command, argv))
    assert tuple(command for command, _ in lifecycle) == expected_lifecycle
    commands = {command: argv for command, argv in lifecycle}
    require_exact_flags(
        commands["pin-github-approval-policy"], (
        "--kind", "--input", "--expected-input-sha256", "--gh-binary", "--selection-state",
        ),
    )
    assert flag_value(commands["pin-github-approval-policy"], "--kind") == "recovery"
    require_exact_flags(
        commands["validate-recovery-checklist:pre-dispatch-proposal"], (
        "--phase", "--input", "--approval-policy-selection-state", "--selection-state",
        ),
    )
    require_exact_flags(
        commands["import-github-approval"], (
        "--kind", "--pre-dispatch-proposal", "--repository", "--issue", "--gh-binary",
        "--approval-selection-state",
        ),
    )
    assert flag_value(commands["import-github-approval"], "--kind") == "recovery"
    assert flag_value(
        commands["import-github-approval"], "--repository"
    ) == "bluetape4k/bluetape4k-projects"
    assert flag_value(commands["import-github-approval"], "--issue") == "757"
    require_exact_flags(
        commands["verify-recovery-approval"], (
        "--pre-dispatch-proposal", "--gh-binary", "--approval-selection-state",
        ),
    )
    require_exact_flags(
        commands["finalize-recovery-pre-dispatch"], (
        "--pre-dispatch-proposal", "--gh-binary", "--approval-selection-state",
        "--pre-dispatch-selection-state",
        ),
    )
    require_exact_flags(
        commands["verify-published-recovery"], (
        "--evidence-root", "--recovery-source", "--dispatch-contract", "--pre-dispatch-selection-state",
        "--gh-binary", "--approval-selection-state", "--abi-manifest", "--selection-state",
        ),
    )
    assert commands["abi-validator"] == ["validate", "--manifest", "OUTPUT"]
    require_exact_flags(
        commands["validate-recovery-checklist:post-dispatch"], (
        "--phase", "--input", "--pre-dispatch-selection-state", "--selection-state",
        ),
    )
    require_exact_flags(
        commands["validate-recovery-checklist:close"], (
        "--phase", "--input", "--post-dispatch-selection-state", "--selection-state",
        ),
    )
    return records

recovery_records = validate_recovery_records(extract_records(post_release))

def clone(records):
    return [(command, list(argv)) for command, argv in records]

def remove_flag(record, flag):
    command, argv = record
    index = argv.index(flag)
    del argv[index:index + 2]
    return command, argv

def must_reject(records):
    try:
        validate_recovery_records(records)
    except (AssertionError, KeyError, ValueError):
        return
    raise AssertionError("adversarial recovery runbook was accepted")

def must_reject_markdown(markdown):
    try:
        validate_recovery_records(extract_records(markdown))
    except (AssertionError, KeyError, ValueError):
        return
    raise AssertionError("adversarial recovery Markdown was accepted")

def must_reject_recovery_doc(markdown):
    try:
        validated_post_release_bash_blocks(markdown, require_complete=True)
    except AssertionError:
        return
    raise AssertionError("adversarial recovery document was accepted")

must_reject([])  # prose-only tokens, commands outside fences, or commands only before anchor
bad = clone(recovery_records)
bad[0] = remove_flag(bad[0], "--expected-input-sha256")
must_reject(bad)
bad = clone(recovery_records)
bad[3] = remove_flag(bad[3], "--gh-binary")
bad[0][1].extend(("--gh-binary", "DUPLICATE"))
must_reject(bad)
bad = clone(recovery_records)
bad[0], bad[1] = bad[1], bad[0]
must_reject(bad)
bad = clone(recovery_records)
bad.insert(3, (bad[2][0], list(bad[2][1])))
must_reject(bad)
bad = clone(recovery_records)
del bad[6]
must_reject(bad)
bad = clone(recovery_records)
bad.insert(6, (bad[6][0], list(bad[6][1])))
must_reject(bad)
bad = clone(recovery_records)
bad[5], bad[6] = bad[6], bad[5]
must_reject(bad)
bad = clone(recovery_records)
bad[7] = remove_flag(bad[7], "--pre-dispatch-selection-state")
must_reject(bad)
bad = clone(recovery_records)
bad[8] = remove_flag(bad[8], "--post-dispatch-selection-state")
must_reject(bad)
must_reject_markdown(post_release.replace(runner_script, f"echo python3 {runner_script}"))
must_reject_markdown(post_release.replace(runner_script, f"printf %s python3 {runner_script}"))
must_reject_markdown(post_release.replace("python3 " + runner_script, "false && python3 " + runner_script))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state FILE || true", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state FILE||true", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state FILE;true", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state FILE 2>/dev/null", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state FILE --dry-run yes", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", '  --selection-state "$(printf FILE)"', 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state `printf FILE`", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state $ISSUE757_STATE", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", "  --selection-state FILE*", 1))
must_reject_markdown(post_release.replace("  --expected-input-sha256 SHA256", "  --expected-input-sha256 FILE", 1))
must_reject_markdown(post_release.replace('  --gh-binary "$(command -v gh)"', "  --gh-binary FILE", 1))
must_reject_markdown(post_release.replace("  --selection-state FILE", '  --selection-state "$(command -v gh)"', 1))
must_reject_markdown(
    post_release.replace(
        "  --pre-dispatch-selection-state FILE",
        "  --pre-dispatch-selection-state FILE||true",
        1,
    )
)
must_reject_markdown("```bash\nset -euo pipefail\npython3() { :; }\n```\n" + post_release)
must_reject_markdown("```bash\nset -euo pipefail\nalias python3=true\n```\n" + post_release)
must_reject_markdown("```bash\nset -euo pipefail\nsource ./unsafe.sh\n```\n" + post_release)
must_reject_markdown("```bash\nset -euo pipefail\ntrap true ERR\n```\n" + post_release)
must_reject_markdown("```bash\nset -euo pipefail\nset +e\n```\n" + post_release)
continued_runner = "python3 " + runner_script + " \\\n"
assert continued_runner in post_release
must_reject_markdown(post_release.replace(continued_runner, continued_runner + "\n", 1))
must_reject_markdown(post_release.replace(continued_runner, continued_runner + "# gap\n", 1))
must_reject_markdown(
    post_release.replace(continued_runner, continued_runner.replace("\\\n", "\\   \n"), 1)
)
must_reject_markdown(
    post_release.replace(continued_runner, continued_runner.replace("\\\n", "\\\t\n"), 1)
)
must_reject_markdown(post_release.replace("FILE", "FILE\u00a0", 1))
must_reject_markdown(post_release.replace("FILE", "FILE\v", 1))
must_reject_markdown(post_release.replace("FILE", "FILE\x1c", 1))
must_reject_markdown(post_release.replace("FILE", "FILE\u0085", 1))
must_reject_markdown(post_release.replace("FILE", "FILE\u2028", 1))
must_reject_markdown(post_release.replace("FILE", "FILE\r", 1))
must_reject_markdown(post_release.replace("FILE", "FILE\r\n", 1))
must_reject_markdown("```sh\ntrue\n```\n" + post_release)
must_reject_markdown("~~~sh\ntrue\n~~~\n" + post_release)
must_reject_markdown("~~~bash\ncommand gh issue comment 757\n~~~\n" + post_release)
must_reject_markdown("   ~~~bash\n   timeout 1 gh issue comment 757\n   ~~~\n" + post_release)
must_reject_markdown("> ~~~bash\n> command gh issue comment 757\n> ~~~\n" + post_release)
must_reject_markdown("- ~~~bash\n  timeout 1 gh issue comment 757\n  ~~~\n" + post_release)
must_reject_markdown("    python3 unsafe.py\n" + post_release)
must_reject_markdown("gh issue comment 757\n" + post_release)
must_reject_markdown("command gh issue comment 757\n" + post_release)
must_reject_markdown("timeout 1 gh issue comment 757\n" + post_release)
must_reject_markdown("- timeout 1 gh issue comment 757\n" + post_release)
must_reject_markdown("> > command gh issue comment 757\n" + post_release)
must_reject_markdown("- > command gh issue comment 757\n" + post_release)
must_reject_markdown("1. > timeout 1 gh issue comment 757\n" + post_release)
must_reject_markdown("- - command gh issue comment 757\n" + post_release)
must_reject_markdown("nice gh issue comment 757\n" + post_release)
must_reject_markdown(":; gh issue comment 757\n" + post_release)
must_reject_markdown(">     nice gh issue comment 757\n" + post_release)
must_reject_markdown("nice gh issue comment 757 --body 승인.\n" + post_release)
must_reject_markdown(":; gh issue comment 757 --body 안내.\n" + post_release)
must_reject_markdown("- nice gh issue comment 757 --body 승인.\n" + post_release)
must_reject_markdown("> :; gh issue comment 757 --body 안내.\n" + post_release)
must_reject_markdown("touch FILE\n" + post_release)
must_reject_markdown("./unsafe.sh\n" + post_release)
must_reject_markdown("<pre><code>gh issue comment 757</code></pre>\n" + post_release)
must_reject_recovery_doc(
    recovery_doc.replace(anchor, "~~~bash\ngh issue comment 757\n~~~\n" + anchor, 1)
)
must_reject_recovery_doc(
    recovery_doc.replace(anchor, "> ~~~bash\n> gh issue comment 757\n> ~~~\n" + anchor, 1)
)
first_prose = approved_outside_sequence[2]
second_prose = approved_outside_sequence[3]
must_reject_recovery_doc(
    recovery_doc.replace(first_prose, first_prose + "\n" + first_prose, 1)
)
must_reject_recovery_doc(
    recovery_doc.replace(
        first_prose + "\n" + second_prose,
        second_prose + "\n" + first_prose,
        1,
    )
)
assert "approve-rollback" not in recovery_doc
assert "approve-recovery" not in recovery_doc
PY
git diff --check
```

- [ ] **Step 5: immutable generation, pointer, docs/evidence Lore commit.** Intent:
`Bind the Lettuce delivery claim to reproducible evidence`. tracked Git tree가 generation, pointer,
report의 최종 authority가 된다. payload/root에는 이 commit/tree를 되쓰지 않는다.

- [ ] **Step 6: committed active generation과 non-cyclic authority DAG를 검증한다.**

```bash
set -euo pipefail
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json
ISSUE757_TERMINAL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py read-active \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --field delivery_terminal)
case "$ISSUE757_TERMINAL" in
  retained-accepted|retained-inconclusive|rejected-after-regression) ;;
  *) exit 1 ;;
esac
```

validator는 pointer가 가리키는 exact immutable generation/root hash, carried archive,
measurement-source 또는 rollback-source authority, ABI/JMH payload/result/root DAG와 report를
검증한다. immutable generation과 committed archive는 cleanup 대상이 아니다. owned partial
staging이 남은 경우에만 lock과 receipt를 지정해 `cleanup-owned-staging`을 별도로 실행한다.
Task 12의 PR title, issue comment, post-merge disposition도 이 validated active generation의
`delivery_terminal`만 사용한다.

## Task 12: final verification, review, lesson, push, PR

**Files:**

- Create: `docs/lessons/2026-07-20-issue-757-lettuce-protobuf-buffer.md`
- Create: `docs/review/issue-757-lettuce-protobuf-buffer-review.md`
- Create ignored handoff generations: `.omx/evidence/issue-757-lettuce/github-handoff-*/**`
- GitHub: issue #757 progress update and PR body in English.

**Skills:** `requesting-code-review`, `verification-before-completion`, `bluetape-writer`; PR review는 six independent lanes + main integration.

- [ ] **Step 1: final exact-head local verification.**

```bash
set -euo pipefail
ISSUE757_ACTIVE_TERMINAL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-active --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --field delivery_terminal)
case "$ISSUE757_ACTIVE_TERMINAL" in
  retained-accepted|retained-inconclusive) ISSUE757_FINAL_MODE=retained ;;
  rejected-after-regression) ISSUE757_FINAL_MODE=rejected ;;
  *) exit 1 ;;
esac
./gradlew :bluetape4k-lettuce:clean :bluetape4k-lettuce:build \
  --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:clean :bluetape4k-protobuf:build \
  --no-parallel --no-configuration-cache
./gradlew :protobuf-codec-benchmark:clean :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py \
  benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py \
  benchmark/protobuf-codec-benchmark/scripts/test_issue757_detached_roots.py
./gradlew :bluetape4k-lettuce:detekt :bluetape4k-protobuf:detekt --no-configuration-cache
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-final-head \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --head HEAD \
  --terminal "$ISSUE757_FINAL_MODE"
git diff --check
test -z "$(git status --porcelain)"
```

각 container-capable invocation 전에 다른 container/heavy work가 없음을 확인한다. evidence
generation commit의 정상 descendant로 exact allowlist의 docs/review/lesson만 추가된 경우에는
`verify-final-head`가 그 path/blob drift를 검증한다. retained는 HEAD가 immutable
measurement-source descendant인지, rejected는 candidate measurement-source -> canonical
rollback-source -> HEAD ancestry인지 검증한다. production/build/test/benchmark input 또는
authority commit/tree가 바뀌면 fresh detached verification/two-run/promotion lifecycle을 다시
수행한다. payload/root/manifest hash를 직접 수정하거나 final HEAD를 tracked payload에
되쓰지 않는다.

- [ ] **Step 2: six-lens plan-to-code review를 실행한다.** performance, stability, security, operator, developer/API, caller lanes를 독립 실행하고 main session에서 중복을 통합한다. 모든 finding은 수정 후 affected lane을 재검토하며 최종 조건은 각 관점 P0=P1=P2=P3=0이다.

review edit가 생기면 push 전에 다음 routing table을 적용하고 affected lane을 다시
검토한다.

| Tracked change after review | Required route |
|---|---|
| only `docs/review/**` and `docs/lessons/**` | 기존 retained measurement-source 또는 rejected B3 rollback-source SHA를 보존한다. commit 후 active generation/report와 terminal-specific external exact-head verifier만 재실행한다. B4/B5는 새 `HEAD`로 실행하지 않는다. |
| authority commit/tree의 rebase 또는 rewrite | old payload rebind 금지; Task 8부터 새 immutable authority generation 생성 |
| production, tests, runner, validator, Gradle/build metadata, benchmark, KDoc, evidence/report/README/manual/CHANGELOG를 포함한 그 밖의 모든 변경 | 기존 authority에 descendant input을 덧붙여 B4를 실행하지 않는다. retained는 새 candidate authority로 Task 8-9-11을 반복한다. rejected는 변경을 포함한 새 candidate authority에서 Task 8-9를 수행하고 regression이면 새 unique rollback preparation/B2/B3/B4/B5/Task 11 lifecycle을 수행한다. |

어느 route든 변경 후 affected review lane과 final exact-head gate를 다시 통과하지 않으면
push하지 않는다.

authority rewrite를 docs-only rebind로 처리하지 않는다. 새 authority가 필요하면 old
generation을 그대로 보존하고 Task 8부터 새 payload/result/root와 generation을 만든다. 모든
경로에서 commit 후 Task 12 Step 1과 affected review를 다시 통과하기 전에는 push하지 않는다.

- [ ] **Step 3: Korean review/lesson을 작성한다.** review에는 spec/plan mapping, terminal, ABI/wire/security/resource proof, commands/results, remaining risk를 기록한다. lesson에는 NIO view를 기각한 이유, absolute writer invariant, measurement/final-head split, evidence replacement 교훈을 기록한다.

validated active generation에서 terminal, measurement-source 또는 rollback-source,
tree/JAR/evidence identity를 읽는 English GitHub handoff는 tracked review/lesson commit 뒤에
no-clobber generation으로 만든다. PR body는 `Summary`, `Terminal`, `Compatibility`,
`Verification`, `Evidence`, `Excluded Scope`, `Issue Link`, 마지막 heading `DoD Status`를 갖고
`Closes #757`을 포함하지 않는다. progress comment는 같은 terminal/identity와 실제 PR URL을
기록하며 retained/rejected post-merge disposition을 명시한다.

- [ ] **Step 4: final Lore commit 후 clean/exact head를 확인한다.** Intent: `Make issue 757 review and recovery evidence durable`.

```bash
set -euo pipefail
git status --short
git rev-parse HEAD
git log -1 --format=fuller
ISSUE757_ACTIVE_TERMINAL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-active --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --field delivery_terminal)
case "$ISSUE757_ACTIVE_TERMINAL" in
  retained-accepted|retained-inconclusive) ISSUE757_FINAL_MODE=retained ;;
  rejected-after-regression) ISSUE757_FINAL_MODE=rejected ;;
  *) exit 1 ;;
esac
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-final-head \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --head HEAD \
  --terminal "$ISSUE757_FINAL_MODE"
test -z "$(git status --porcelain)"
```

이 external verifier가 commit 후 resolve한 HEAD/tree가 final exact-head authority다. retained는
measurement-source 이후, rejected는 rollback-source 이후 exact docs/evidence allowlist 밖
drift가 0이어야 하며 tracked payload/root는 이 HEAD/tree를 포함하지 않는다.

- [ ] **Step 4a: final head에 bind된 no-clobber PR handoff를 seal한다.**

```bash
set -euo pipefail
ISSUE757_GITHUB_ACTOR=$(gh api user --jq .login)
ISSUE757_HEAD_REPOSITORY=bluetape4k/bluetape4k-projects
test -n "$ISSUE757_GITHUB_ACTOR"
ISSUE757_PR_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind pr \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
test -d "$ISSUE757_PR_HANDOFF"
```

그 unique directory의 `intent-marker.txt` exact hidden marker를 포함해 `pr-body.md`를 영어로
작성한 뒤 다음을 실행한다.

```bash
set -euo pipefail
ISSUE757_GITHUB_ACTOR=$(gh api user --jq .login)
ISSUE757_HEAD_REPOSITORY=bluetape4k/bluetape4k-projects
ISSUE757_PR_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind pr \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
test -d "$ISSUE757_PR_HANDOFF"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py seal-github-handoff \
  --handoff-root "$ISSUE757_PR_HANDOFF" \
  --body "$ISSUE757_PR_HANDOFF/pr-body.md" \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY"
```

handoff manifest는 no-clobber regular-file body SHA-256, active generation/root/terminal,
measurement-source 또는 rollback-source, final commit/tree, authenticated actor와
repo/base/head branch/head repository owner/name을 bind한다.
symlink, traversal, overwrite와 identity drift는 실패다. GitHub mutation 직전에 같은 manifest를
다시 검증한다.

각 PR/comment mutation은 mutation 직전 live state bundle을 다시 조회해 no-clobber
pre-mutation receipt를 만든다. receipt는 authenticated actor, repository/base/head repository와
branch, live remote ref SHA, sealed final SHA, PR URL/head SHA/body identity와 exact live-query JSON
hash/timestamp를 bind하고 최대 freshness를 넘기면 거부한다. `reconcile-github-mutation` decision은
그 exact receipt path/hash를 포함해야 하며 create/adopt 뒤 같은 live fields를 다시 검증한다.
Step 5 이후 remote drift, PR outcome 이후 comment 직전 PR/remote drift fixture는 외부 mutation
command가 호출되지 않았음을 증명한다.

- [ ] **Step 5: branch를 push하고 exact remote head를 확인한다.**

```bash
set -euo pipefail
git push -u origin feat/issue-757-lettuce-protobuf-buffer
ISSUE757_LOCAL_HEAD=$(git rev-parse HEAD)
ISSUE757_REMOTE_HEAD=$(git ls-remote --heads origin \
  feat/issue-757-lettuce-protobuf-buffer | awk '{print $1}')
test -n "$ISSUE757_REMOTE_HEAD"
test "$ISSUE757_LOCAL_HEAD" = "$ISSUE757_REMOTE_HEAD"
test -z "$(git status --porcelain)"
```

- [ ] **Step 6: terminal-specific English PR을 생성한다.** repo/base/head를 정확히 지정하고
body의 마지막 `##` heading을 `## DoD Status`로 둔다. accepted만 measured positive wording을
허용하고 inconclusive는 neutral wording을 사용한다. 모든 terminal은 post-merge evidence
기록보다 먼저 issue를 auto-close하지 않도록 `Closes #757`을 쓰지 않는다. rejected는
non-closing evidence-only PR임을 명시한다.

```bash
set -euo pipefail
ISSUE757_GITHUB_ACTOR=$(gh api user --jq .login)
ISSUE757_HEAD_REPOSITORY=bluetape4k/bluetape4k-projects
ISSUE757_PR_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind pr \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
test -d "$ISSUE757_PR_HANDOFF"
ISSUE757_TERMINAL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py read-active \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --field delivery_terminal)
case "$ISSUE757_TERMINAL" in
  retained-accepted)
    ISSUE757_PR_TITLE="Optimize Lettuce Protobuf ByteBuf encoding"
    ;;
  retained-inconclusive)
    ISSUE757_PR_TITLE="Add Lettuce Protobuf direct ByteBuf encoding evidence"
    ;;
  rejected-after-regression)
    ISSUE757_PR_TITLE="Record Lettuce Protobuf ByteBuf allocation regression"
    ;;
  *) exit 1 ;;
esac
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-github-handoff \
  --handoff-root "$ISSUE757_PR_HANDOFF" \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY"
ISSUE757_PR_URL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  execute-github-mutation \
  --handoff-root "$ISSUE757_PR_HANDOFF" \
  --kind pr \
  --repository bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-757-lettuce-protobuf-buffer \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --title "$ISSUE757_PR_TITLE" \
  --gh-binary "$(command -v gh)" \
  --max-live-age-seconds 5)
test -n "$ISSUE757_PR_URL"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-github-outcome-live \
  --handoff-root "$ISSUE757_PR_HANDOFF" \
  --kind pr \
  --expected-url "$ISSUE757_PR_URL" \
  --gh-binary "$(command -v gh)"
```

- [ ] **Step 7: English #757 progress comment를 게시한다.** validated committed manifest의
`delivery_terminal`을 그대로 사용하며 comment에는 terminal, PR URL,
exact head, compatibility/test/evidence result, compressed/custom-prefix/decode exclusions를
기록한다. retained는 approved merge/post-merge closure 전까지 issue가 open임을, rejected는
issue와 #898 item을 open으로 유지함을 명시한다.

```bash
set -euo pipefail
ISSUE757_GITHUB_ACTOR=$(gh api user --jq .login)
ISSUE757_HEAD_REPOSITORY=bluetape4k/bluetape4k-projects
ISSUE757_PR_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind pr \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
ISSUE757_PR_URL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-github-outcome \
  --outcome-selection "$ISSUE757_PR_HANDOFF/outcome-selection.json" \
  --field live_url)
test -n "$ISSUE757_PR_URL"
ISSUE757_COMMENT_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind issue-comment \
  --pr-url "$ISSUE757_PR_URL" \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
test -d "$ISSUE757_COMMENT_HANDOFF"
```

unique directory의 `intent-marker.txt` exact hidden marker와 실제 PR URL을
`issue-comment.md`에 포함해 작성하고 seal한 뒤 게시한다.

```bash
set -euo pipefail
ISSUE757_GITHUB_ACTOR=$(gh api user --jq .login)
ISSUE757_HEAD_REPOSITORY=bluetape4k/bluetape4k-projects
ISSUE757_PR_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind pr \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
ISSUE757_PR_URL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  read-github-outcome \
  --outcome-selection "$ISSUE757_PR_HANDOFF/outcome-selection.json" \
  --field live_url)
ISSUE757_COMMENT_HANDOFF=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  select-github-handoff \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --kind issue-comment \
  --pr-url "$ISSUE757_PR_URL" \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --output-root .omx/evidence/issue-757-lettuce)
test -d "$ISSUE757_COMMENT_HANDOFF"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py seal-github-handoff \
  --handoff-root "$ISSUE757_COMMENT_HANDOFF" \
  --body "$ISSUE757_COMMENT_HANDOFF/issue-comment.md" \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --pr-url "$ISSUE757_PR_URL" \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-github-handoff \
  --handoff-root "$ISSUE757_COMMENT_HANDOFF" \
  --active-pointer docs/benchmarks/raw/issue-757/active-generation.json \
  --final-head HEAD \
  --pr-url "$ISSUE757_PR_URL" \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --head-repository "$ISSUE757_HEAD_REPOSITORY"
ISSUE757_COMMENT_URL=$(python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py \
  execute-github-mutation \
  --handoff-root "$ISSUE757_COMMENT_HANDOFF" \
  --kind issue-comment \
  --repository bluetape4k/bluetape4k-projects \
  --issue 757 \
  --pr-url "$ISSUE757_PR_URL" \
  --head feat/issue-757-lettuce-protobuf-buffer \
  --head-repository "$ISSUE757_HEAD_REPOSITORY" \
  --github-actor "$ISSUE757_GITHUB_ACTOR" \
  --gh-binary "$(command -v gh)" \
  --max-live-age-seconds 5)
test -n "$ISSUE757_COMMENT_URL"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-github-outcome-live \
  --handoff-root "$ISSUE757_COMMENT_HANDOFF" \
  --kind issue-comment \
  --expected-url "$ISSUE757_COMMENT_URL" \
  --gh-binary "$(command -v gh)"
```

- [ ] **Step 8: issue/PR live metadata와 exact-head CI/review를 확인한다.** milestone `1.12.0`, labels, assignee, unresolved threads, automated/human review artifact, local/remote/PR head equality를 확인한다. Step 6/7 live query 결과가 sealed body, terminal, evidence identity, repo/base/head/final SHA와 exact-equal인지 다시 확인한다.

- [ ] **Step 9: merge-ready 보고 후 정지한다.** merge는 fresh explicit user approval을 받은 뒤 별도 단계로 수행한다. post-merge issue #757/#898/#756 처리는 terminal별 명세를 따르고, release/tag/publish는 별도 승인 없이는 실행하지 않는다.

### Post-merge owner checklist (fresh merge approval 이후에만 실행)

- retained terminal: merged commit과 merged manifest/report를 다시 검증하고 #757에 PR/merge
  commit/final evidence를 영어로 기록한 뒤 issue를 close한다. #898의 #757 item을 완료하고
  #756에 Lettuce slice 완료와 compressed/custom-prefix/generic SPI 제외 범위를 링크한다.
- rejected terminal: #757과 #898 item을 open으로 유지하고 negative evidence, removed dispatch,
  남은 scope를 영어로 기록한다. #756에는 미완료 Lettuce slice와 제외 범위를 링크한다.
- 두 terminal 모두 milestone, labels, assignee를 live metadata에서 다시 확인한다.

### Post-release recovery handoff (실제 incident와 별도 승인 이후에만 실행)

- #757을 reopen하고 incident/release/consumer/owner를 기록한 뒤 operation guide의 JSON template을
  새 no-clobber incident checklist로 복사한다.
- approved HTTPS Maven authority에서 runner가 직접 no-clobber fetch한 immutable retrieval
  receipt의 GAV/JAR hash와 release commit/tree/ref를 authority로 고정하고
  `published-retained-vs-recovery` mode가 dispatch-only recovery JAR의 retained ABI exact equality를
  증명해야 한다. pre-release rejected rollback이나 local build baseline은 사용하지 않는다.
- 먼저 repository/issue, expected distinct reviewer, current runner actor, validated absolute
  regular-file `gh` path/SHA-256, marker schema와 proposal binding rule을 담은 exact recovery policy
  input 및 SHA-256을 사용자에게 제시해 fresh explicit
  approval을 받고 그 digest를 `--expected-input-sha256`에 전달해
  `pin-github-approval-policy --kind recovery --gh-binary PATH`로 immutable policy receipt를 만든다.
  selected policy receipt를 proposal에 bind한 `--phase pre-dispatch-proposal`로 immutable proposal을
  만들고, 그 exact proposal hash에 distinct pinned GitHub reviewer가 fresh redeploy marker를 게시하고
  live 검증한 뒤
  `finalize-recovery-pre-dispatch`가 selected final
  pre-dispatch receipt를 만들기 전에는 dispatch하지 않는다. crash/retry는 exact 기존
  proposal/approval/final receipt만 adopt하고 충돌하는 두 번째 선택을 거부한다. immutable
  proposal, approval과 final pre-dispatch receipt는 서로 alias될 수 없는
  `published_baseline` repository/GAV/digest/release commit/tree/ref와 `planned_recovery`
  coordinate/digest/command/target/environment/region, threshold/window set을 각각 bind한다.
  `import-github-approval`은 fresh explicit user gate 뒤 distinct pinned GitHub reviewer가 게시한
  exact marker를 read-only로 가져오고, `verify-recovery-approval`은 pinned `--gh-binary` identity로
  live comment author/ID/body와
  incident/flow, 두 identity, timeout/maximum-byte cap, pre-dispatch hash를 fetch 및 dispatch 직전에
  다시 검증한다. runner actor가 comment를 게시하거나 같은 author이면 허용하지 않는다.
  dispatch 뒤 actual coordinate/digest/command/target은 오직 approved `planned_recovery`와
  exact-equal인지 확인하고 change ID/timestamps/confirmation을 immutable post-dispatch receipt에
  기록한다. published JAR fetch와 retained ABI baseline은 오직 approved `published_baseline`에서
  resolve한다. `--phase post-dispatch`가 통과하기 전에는 observation window를 시작하지 않는다.
- pinned window/metrics와 refreshed wire/security/Redis/allocation/evidence가 모두 통과하고
  `--phase close`가 성공한 뒤 #757에 authority/result/consumer impact를 연결해야만 다시 close한다.
  optimized dispatch 재도입, release/tag/publish 또는 published ABI 제거는 각각 별도 설계·승인을
  요구한다.

## 2. 최종 spec-to-task traceability

| Spec/DoD | Plan task |
|---|---|
| evidence-correct open ABI, exact raw transition과 기존 descriptors | 1, 4, 10, 12 |
| immutable baseline, clean detached measurement-source, JAR-entry/toolchain binding | 1, 4, 8, 12 |
| compile-negative source/erased bridge와 crafted decode/ClassLoader trust boundary | 1, 3, 4, 10 |
| bounded absolute writer/no NIO | 2, 3 |
| null/read-only/max/released/partial/short-success/resource | 2 |
| failure-aftercare capacity/range ownership과 discard/reinitialize guidance | 1, 2, 11 |
| strict/trusted/fallback/allowlist/compressed compatibility | 2, 3, 11 |
| old/new wire와 Redis round trip | 3 |
| heap/direct copied/optimized benchmark | 5 |
| score/error/5%+8 B/op/uncertainty/terminal precedence | 6, 9 |
| non-cyclic payload/result/root와 external final-head authority | 1, 7, 8, 11, 12 |
| retained measurement-source와 rejected canonical rollback-source lineage | 7, 8, 9, 10, 12 |
| fenced no-replace generation, fsync, active pointer와 crash recovery | 7, 11 |
| published-retained dispatch-only recovery ABI/checklist/owner handoff | 7, 11, 12 |
| promoted generation/report/docs/locale/KDoc/changelog | 11 |
| module/static/full verification | 8, 12 |
| six-lens review, exact-head PR, merge gate | 12 |

## 3. 재실행 및 복구 규칙

- test failure는 `systematic-debugging`으로 root cause를 고정한 뒤 가장 작은 affected command부터 다시 실행한다.
- JMH run이 invalid/missing/identity mismatch면 해당 run ID를 재사용하거나 덮어쓰지 않고 새 run ID로 다시 두 run을 수집한다.
- candidate confirmed regression은 성능 문구만 제거하는 것으로 끝내지 않고 `lettuce_encode` rollback lifecycle과 subtype/open ABI 제거를 수행한다.
- evidence promotion 실패 시 모든 immutable generation을 보존한다. pointer rename 전 실패는
  old active pointer에서 exact receipt로 resume하고, rename 뒤 fsync 경계는 old 또는 exact
  intended new만 재검증해 new를 completion-adopt할 수 있다. owned staging 또는 unreferenced
  generation만 idempotent resume하며 committed generation/archive/raw evidence를 수동 삭제하지
  않는다.
- evidence manifest/hash는 직접 수정하지 않는다. runner/validator/external verifier의 검증된
  command로만 생성한다. authority rewrite는 rebind하지 않고 새 generation으로 재검증한다.
- GitHub handoff 중단은 Task 12 Step 4a부터 재개해 deterministic selection으로 기존 intent를
  재선택한다. preflight live reconciliation과 outcome selection 전에는 새 PR/comment를 만들지
  않고, exact marker object만 adopt하며 duplicate/conflict는 수동 수정 없이 fail-closed한다.
- Testcontainers/JMH는 timeout 또는 flaky retry가 성공해도 원인을 review artifact에 남긴다.
- scope 밖 문제는 현재 구현에 섞지 않고 follow-up issue 후보로 기록한다.

## 4. 구현 계획 독립 검토 수렴

distinct external approval authority와 Lettuce public docs parity amendment를 포함한 계획 본문은
SHA-256 `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee`에서
아래 여섯 관점의 fresh full review가 모두 all-zero로 수렴했다.

| 관점 | 검토 본문 SHA-256 | 최종 P0 | 최종 P1 | 최종 P2 | 최종 P3 | 판정 |
|---|---|---:|---:|---:|---:|---|
| Performance | `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee` | 0 | 0 | 0 | 0 | PASS |
| Stability | `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee` | 0 | 0 | 0 | 0 | PASS |
| Security | `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee` | 0 | 0 | 0 | 0 | PASS |
| Operator | `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee` | 0 | 0 | 0 | 0 | PASS |
| Developer/API | `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee` | 0 | 0 | 0 | 0 | PASS |
| Caller | `942f4e966b4ae1c4dec9932c541de82b206798aef921efc1fc4df6e9b5e609ee` | 0 | 0 | 0 | 0 | PASS |

최종 수렴 조건은 모든 관점 P0=P1=P2=P3=0이다.
