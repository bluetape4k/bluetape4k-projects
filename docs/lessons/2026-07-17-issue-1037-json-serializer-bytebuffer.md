# Issue #1037: JSON serializer ByteBuffer paths

## Context

The shared `JsonSerializer` contract already exposed ByteBuffer defaults, but the
compatibility implementation staged data through a complete ByteArray. Jackson 2,
Jackson 3, and Fastjson2 expose materially different stream and array-range APIs,
so the JSON slice needed backend-specific paths without changing wire formats,
mapper/security configuration, exception policy, or caller-owned buffer state.

## Decision

- Stream Jackson 2 and Jackson 3 output through a fixed duplicate-backed
  `ByteBufferOutputStream`, and read input through a duplicate-backed
  `ByteBufferInputStream`.
- Create and close the Jackson generator explicitly through the configured
  `ObjectWriter`; mapper convenience methods catch `Exception`, not every fatal
  `Error`, so caller-owned stream cleanup cannot rely on them.
- Keep the generic Jackson ByteBuffer API as a top-level concrete-receiver
  extension. Adding a final member to the public open serializer classes could
  conflict with an existing subclass that already owns the same JVM signature.
- Use Fastjson2's array/offset/length JSONB parser only for writable array-backed
  input. Copy direct and read-only input, and retain `JSONB.toBytes` as the
  explicitly allocating output compatibility path.
- Traverse at most 64 cause links when preserving nested fatal errors or buffer
  overflow. Suppressed cleanup failures never replace the primary backend failure.
- Keep all JSONB readers feature-free and do not enable AutoType.

## Surprise / Failure

The first Jackson reified API was a member on an open class. It worked in local
callers but introduced a final JVM method that could break a legacy subclass with
the same erased signature. Moving it to an extension fixed the class ABI, but the
README and tests also had to distinguish that extension from the existing
`JsonSerializer.deserialize` extension through explicit imports and an alias.

Jackson's mapper-owned write path closes its generator for ordinary exceptions,
but the resolved Jackson 2 and Jackson 3 sources catch `Exception` rather than
`Error`. An explicit generator scope was required to guarantee cleanup on fatal
serialization failures.

Fastjson2 may replace a setter-thrown `Error` with a `ClassCastException`, so that
fixture could not prove adapter behavior. A registered JSONB `ObjectReader` that
throws an ordinary wrapper with the fatal error as its cause provided the valid
identity test. The same investigation exposed that scanning suppressed failures
would incorrectly promote cleanup errors over the primary parse failure.

## Outcome

Jackson 2 and Jackson 3 bypass the ByteArray compatibility path for fixed output
and bounded input while retaining mapper configuration and inherited data formats.
Fastjson2 avoids an input copy only where its public API supports an existing
array range; unsupported input and all output stay on documented compatibility
paths. Existing ByteArray entry points, JSON/JSONB wire bytes, raw interface
dispatch, and caller position/limit/mark/order contracts remain intact.

## Verification

- Jackson 2 ByteBuffer suite: 14 tests passed; external consumer import test:
  1 test passed; full module: 455 tests passed.
- Jackson 3 ByteBuffer suite: 16 tests passed; external consumer import test:
  1 test passed; full module: 456 tests passed.
- Fastjson2 ByteBuffer suite: 14 tests passed; full module: 180 tests passed.
- Legacy same-signature Jackson subclasses compile, while the reified extension
  still retains generic collection element types.
- Fastjson2 resolved-source evidence records version `2.0.62`, source locations,
  source JAR SHA-256, and the optimized/fallback matrix.
- Root `detekt` is `NO-SOURCE`; module compilation, complete module tests,
  README contract checks, unsafe-pattern scanning, and `git diff --check` provide
  the available static fallback.

## Future Guard

Do not add a convenience member to a public open class without checking erased
subclass signatures. When a backend owns a stream wrapper, inspect the resolved
source for fatal-error cleanup rather than assuming `use` at the outer stream is
sufficient. Preserve only fatal failures reachable through the primary cause
chain, never through suppressed cleanup failures. Make optimization claims per
backend cell and defer allocation claims until repeated benchmark evidence exists.
