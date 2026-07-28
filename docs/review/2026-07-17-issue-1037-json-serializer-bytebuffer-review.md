# Issue 1037 JSON Serializer ByteBuffer 검토

## Scope

- Route Jackson 2 and Jackson 3 through duplicate-backed ByteBuffer streams.
- Use Fastjson2 JSONB array-range input where supported while retaining its
  allocating output compatibility path.
- Preserve ByteArray APIs, wire formats, mapper and AutoType security settings,
  exception policy, and caller-owned buffer state.
- Document the backend capability matrix and verify external Kotlin import use.

## Review Result

- Final integrated review: APPROVE, P0=0, P1=0.
- Performance: Jackson bypasses complete ByteArray staging for fixed output and
  bounded input. Fastjson2 bypasses input copying only for writable array-backed
  buffers; unsupported input and all output retain the documented fallback.
- Stability: output commits caller position only after success, failures restore
  position, explicit Jackson generator scopes close on fatal failures, and cause
  traversal is bounded to 64 links without promoting suppressed cleanup failures.
- Security: configured Jackson readers and writers remain authoritative;
  Fastjson2 JSONB readers remain feature-free and never enable AutoType.
- Ops: no module registration, Gradle configuration, publishing, release, or
  workflow surface changed. Resource ownership and fallback behavior are local
  to the three serializers.
- Developer/API: the open Jackson serializer classes gain no final public JVM
  method; concrete reified ByteBuffer decoding is a top-level extension. The
  final Fastjson2 serializer retains its concrete convenience member.
- User/caller: heap, direct, sliced, and read-only input behavior is covered.
  External-package tests prove that concrete and generic ByteBuffer extensions
  can be imported together through an explicit alias.

## Resolved Findings

- Moved the Jackson reified ByteBuffer method from the public open classes to
  top-level concrete-receiver extensions, preserving legacy subclass ABI.
- Replaced Jackson mapper-owned output calls with explicit configured-writer
  generator scopes so fatal serialization failures still close resources.
- Limited fatal and overflow classification to the primary cause chain and a
  maximum of 64 links; suppressed failures cannot replace the primary failure.
- Replaced a Fastjson2 setter fixture that rewrote the fatal cause with a
  registered JSONB `ObjectReader` fixture that preserves fatal-error identity.
- Updated all six READMEs with executable fixed-buffer examples, explicit
  imports, fallback wording, and caller-state guarantees.
- Added external consumer-package compile tests for the two Jackson extension
  import combinations.

## Dispositions

- Failed output restores position but may overwrite bytes in the writable range;
  this remains the documented shared serializer contract.
- Fastjson2 output remains allocating because its supported JSONB API returns a
  ByteArray; this slice does not claim a native fixed-buffer output path.
- The pre-existing Jackson 2 blank-prefix and array-validator behavior in
  `createTypedJsonMapper` is outside #1037 because this change does not alter the
  mapper or its AutoType acceptance boundary.
- Allocation rate and throughput measurements remain assigned to #1039; this
  slice reports only verified staging-path behavior.

## Verification

- Jackson 2 ByteBuffer suite: 14 passing; external consumer import test: 1
  passing; complete module: 455 passing.
- Jackson 3 ByteBuffer suite: 16 passing; external consumer import test: 1
  passing; complete module: 456 passing.
- Fastjson2 ByteBuffer suite: 14 passing; complete module: 180 passing.
- README contract and import parity, production unsafe-pattern scan, extracted
  source cleanup, and `git diff --check`: passing.
- Public-symbol inspection confirms that Jackson exposes the concrete reified
  decoder through `JacksonSerializerKt`, not as a class member, while Fastjson2
  retains the member on its final class.
- Root `detekt` is `NO-SOURCE`; complete module compilation and tests provide
  the available static fallback.

## 증거

- Fastjson2 resolved-source evidence records version `2.0.62`, source locations,
  source JAR SHA-256, and the optimized/fallback matrix.
- Exact-head aggregate and JSON-module ABI reports are generated after the main
  implementation commit so their producer commit and JAR hashes are auditable.
