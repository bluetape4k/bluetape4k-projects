# Changelog

모든 주요 변경 사항은 이 파일에 기록됩니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 따르며, 이 프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [1.12.0] — Unreleased

### Added

- Added an opt-in bounded-wait HTTP idempotency conformance fixture for JUnit 5,
  with identical Ktor and Spring MockMvc reference proofs. It validates observable
  HTTP behavior only; adopters still own atomic persistence, restart recovery,
  authorization, rate limiting, and external-side-effect idempotency
  ([#1055](https://github.com/bluetape4k/bluetape4k-projects/issues/1055)). Adoption
  is opt-in and can be rolled back by removing the fixture call or pinning the
  previous library version; changing the public policy requires client migration.

### Fixed

- Hibernate cache-key canonicalization in `bluetape4k-hibernate-cache-lettuce` no
  longer falls back to `toString()` or `hashCode()` bytes when a `Serializable`
  identifier contains a non-serializable nested member, or when two distinct
  non-serializable identifiers share the same textual representation. Both fallback
  branches have been removed; unsupported identifier graphs now fail closed so that
  cache reads miss, writes are ignored, and keyed eviction reports the
  canonicalization failure. This prevents cache poisoning and cross-identifier key
  collisions that were possible in all prior `1.12.0` pre-releases and earlier
  versions
  ([#1274](https://github.com/bluetape4k/bluetape4k-projects/issues/1274)).

  **Migration:** Any cache entries written under the old text/hashCode fallback keys
  are now unreachable. If you stored entries whose identifiers could not be fully
  serialized, flush those Redis keys before deploying this version to avoid stale
  data accumulating under the old key format. Going forward, all identifier objects
  (and every object reachable from them) must be fully `java.io.Serializable`; a
  non-serializable member anywhere in the graph will cause the cache operation to
  be skipped rather than silently collide.

### Performance

<!-- issue-755-migration:start -->
- Added opt-in caller-owned `ByteBuffer` compressor defaults with source-state
  preservation and target position commit/rollback contracts. Compatibility
  defaults may still allocate payload-sized arrays; codec-native paths are
  delivered and measured separately under #755. Existing callers do not need
  to migrate: unlike some existing one-argument `ByteBuffer` methods that may
  consume the source position, the new two-argument methods preserve all source
  state. Opt in only with reusable targets and an optimized storage pairing;
  fallback pairings are correctness-only. As with standard Java interface
  evolution, an implementation inheriting another erased-signature-equivalent
  default may require an explicit override and is not claimed conflict-free.
- Two reproducible canonical JMH GC-profiler runs accepted lower-allocation
  adoption for all LZ4 and Deflate storage pairings, matched heap/direct Zstd
  pairings, and Snappy direct compression. Snappy direct decompression remains
  adoption-ineligible because validation-first decoding regressed medium/large
  throughput by about 37-41% despite reducing allocation
  ([#1260](https://github.com/bluetape4k/bluetape4k-projects/issues/1260), [evidence](docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md)).
<!-- issue-755-migration:end -->
<!-- issue-755-rollback:start -->
- If a codec-native override proves defective, a patch keeps the public default
  methods and wire contract and reverts only that override to the compatibility
  fallback. Until that patch, use an existing allocating API or a documented
  fallback storage pairing; no runtime feature flag is provided.
<!-- issue-755-rollback:end -->
- Added caller-owned `ByteBuffer` APIs for Protobuf message packing and serializer encode while preserving the existing
  `ByteArray` APIs; serializer decode retains the inherited copied `ByteBuffer` compatibility path
  ([#757](https://github.com/bluetape4k/bluetape4k-projects/issues/757)).
- Added a lower-copy contiguous Redisson `ByteBuf` decode path that avoids the codec-owned `ByteArray` copy when a
  single NIO buffer is available; composite and trusted-fallback inputs r