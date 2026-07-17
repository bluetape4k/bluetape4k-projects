# Issue 1038 Avro Serializer ByteBuffer Review

## Scope

- Route reflect, generic-record, specific-record, and specific-list OCF paths
  through caller-owned `ByteBuffer` streams.
- Preserve existing ByteArray APIs, schemas, codecs, OCF framing, null/empty
  policy, and caller buffer state.
- Keep benchmark and measured allocation claims assigned to issue #1039.

## Verifier Result

`PASS`: the implementation matches the approved issue #754 design and Slice 4
plan without expanding module, dependency, workflow, or release scope.

| Requirement | Implementation or evidence |
|---|---|
| Bypass complete ByteArray sibling staging | Concrete overrides use fixed output and duplicate-backed input streams; MockK verifies zero sibling calls. |
| Preserve OCF schema and codec behavior | Old/new cross-reading covers reflect, generic, specific, list, and null/deflate/snappy/zstd codecs. |
| Preserve caller-owned state | Heap/direct/sliced/read-only input and non-zero output positions retain position, limit, order, and mark contracts. |
| Preserve failure and cleanup policy | Raw target overflow, backend overflow, fatal identity, close/flush failure, rollback, retry, null, empty, and malformed paths are covered. |
| Preserve compatibility | The tracked ABI report passes legacy Java/Kotlin compilation, implementation loading, default dispatch, public-symbol, and fixture checks. |
| Exclude measured allocation claims | Only ByteArray sibling bypass and lower-copy routing are claimed; benchmarks remain in #1039. |

## Perspective Review

- Performance: explicit pre-close `flush()` calls were removed. Complete
  ByteArray sibling staging is bypassed, but measured allocation improvement is
  deliberately not claimed before #1039.
- Stability: fixed-target overflow is tagged only at the target stream boundary;
  backend `BufferOverflowException` retains the handled failure policy. Primary
  backend failures remain primary and suppressed cleanup failures are not
  promoted, as required by the approved design.
- Security: deserialization keeps caller-bounded views and caller-supplied
  schemas/classes. New SpecificRecord failure logging records only graph and
  failure types and never renders the caller datum.
- Ops: resources are owned by nested `use` scopes, caller position commits only
  after success, and failed calls remain reusable. The final tracked ABI report
  is bound to the reviewed implementation commit.
- Developer/API: no public signature changed in this slice. English override
  KDoc documents handled backend failures versus escaping target overflow and
  fatal errors.
- User/caller: null/empty behavior, exact-capacity output, heap/direct/sliced/
  read-only input, schema mismatch, codecs, and old/new cross-reading match the
  existing contract.

## Resolved Findings

- P1: stopped classifying every nested `BufferOverflowException` as target
  capacity failure; only fixed-target stream overflow now escapes raw.
- P1: removed caller datum rendering and full backend exception logging from the
  new SpecificRecord handled-failure path.
- P2: documented concrete handled-failure return policy in English KDoc.
- P3: removed redundant explicit writer flushes before `DataFileWriter.close()`.

## Dispositions

- Two stability/Ops lanes proposed promoting suppressed cleanup errors over a
  normal primary backend failure. This was rejected because the approved design
  requires retaining the primary failure and attaching cleanup failures as
  suppressed; the JSON predecessor records the same rule.
- Failed output content is unspecified even though position is restored. This is
  the inherited serializer contract.
- Avro still owns internal OCF block buffers. This slice proves routing and
  compatibility, not allocation counts or throughput.
- README, CHANGELOG, benchmark tables, and public migration recommendations are
  deferred to the approved Slice 5 issue #1039.

## Verification

- Targeted ByteBuffer contract suite: passing, including review regressions.
- Complete `:bluetape4k-avro:test`: 221 passing.
- `check-serializer-buffer-abi.sh --build-current`: passing at implementation
  commit `02abab67e` with refreshed tracked evidence.
- Root `detekt`: successful `NO-SOURCE`; the module has no separate detekt task.
- Kotlin compilation and complete tests are the static-analysis fallback because
  the available LSP diagnostic backend is TypeScript-only.
- Production unsafe-concurrency diff scan and `git diff --check`: passing.

## Final Gate

Final integrated result: `APPROVE`, P0=0, P1=0. Remaining P2 allocation evidence
is assigned to #1039 and does not block Slice 4 delivery.
