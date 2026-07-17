# Issue #1038: Avro serializer ByteBuffer paths

## Context

The Avro interfaces already exposed compatible `ByteBuffer` defaults, but those
defaults copied through complete ByteArrays before reading or writing OCF data.
Reflect, generic-record, specific-record, and list implementations needed direct
stream routing without changing schema, codec, sync-marker, framing, null/empty,
or caller-state behavior.

## Decision

- Write OCF data through a non-growing `ByteBufferOutputStream` over a duplicate
  of the caller target, then commit the caller position only after writer close
  succeeds.
- Read OCF data with `DataFileStream` over a duplicate-backed
  `ByteBufferInputStream`, preserving every caller-visible source property.
- Tag overflow only when it originates in the fixed target stream. A datum
  accessor may itself throw `BufferOverflowException`; that remains an ordinary
  handled backend failure rather than a false target-capacity signal.
- Preserve fatal errors found on the primary cause chain. Do not promote
  suppressed cleanup failures over the primary backend failure.
- Keep new handled-failure logs metadata-only so caller records are never
  rendered.

## Surprise / Failure

The first overflow classifier searched the full primary cause chain for any
`BufferOverflowException`. An independent Developer/API review showed that Avro
wraps datum-writer failures, so a record accessor could be misreported as a full
target buffer. A stream-bound signal separated target capacity from backend
behavior and a RED regression test locked the distinction.

The first SpecificRecord log copied the legacy `graph=$graph` diagnostic. That
could evaluate caller `toString()` and expose record data. The regression test
had to force both a real codec-close failure and ERROR-level lazy message
evaluation; a throwing `toString()` alone was insufficient because the logging
helper safely replaces message-render exceptions.

The ABI script also intentionally refuses dirty serializer paths. The reliable
sequence is implementation commit, clean-head ABI generation, evidence commit,
and regeneration after every review-driven source fix.

## Outcome

All four Avro families now bypass the allocating ByteArray sibling methods for
bounded buffer input and fixed output. OCF data cross-reads with legacy methods,
configured codecs remain authoritative, caller state is preserved, failed calls
remain reusable, and the compatibility report is bound to the reviewed code.

## Verification

- The focused contract suite covers direct/heap/sliced/read-only input,
  exact-capacity output, overflow provenance, rollback, fatal identity, cleanup
  failure, retry, schema mismatch, codecs, null/empty lists, malformed input,
  sibling bypass, and caller-safe logging.
- The complete Avro module reports 221 passing tests.
- Legacy Java/Kotlin callers, implementation loading, JVM default dispatch,
  public symbols, and frozen fixtures pass the exact-head ABI gate.
- Root detekt is `NO-SOURCE`; Kotlin compilation, full tests, unsafe-pattern
  scanning, and `git diff --check` provide the available static proof.

## Future Guard

Do not infer target capacity failure from an unqualified nested exception; mark
the resource boundary that produced it. Do not log a serializer datum to explain
a handled failure. Keep OCF comparisons semantic, and defer allocation or
throughput claims until #1039 supplies repeatable benchmark evidence.
