# Issue #1036: Core serializer ByteBuffer paths

## Context

`BinarySerializer` already exposed compatible ByteBuffer defaults, but those
defaults staged every operation through a newly allocated ByteArray. JDK, Kryo,
and Fory have different buffer capabilities and lifecycle constraints, so one
shared optimization strategy could not preserve every existing contract.

## Decision

- Use fixed ByteBuffer-backed streams for JDK input and output and apply the same
  configured or global `ObjectInputFilter` as the ByteArray path.
- Bind Kryo `ByteBufferInput` and `ByteBufferOutput` to the caller's bounded slice,
  but expose them only through scoped provider methods that detach caller storage
  before returning adapters to the global pool.
- Use Fory's native ByteBuffer input overload and retain the compatibility
  ByteArray output path because the backend may grow or detach output storage.
- Keep new failure and cleanup helpers internal instead of extending the
  protected surface of the externally extensible `AbstractBinarySerializer`.
- Preserve raw overflow, fatal errors, configured registration, wire bytes, and
  caller position/limit/order behavior.

## Surprise / Failure

Adding protected convenience methods to an open base class looked source-local
but could create JVM signature conflicts for external subclasses. Direct pooled
adapter obtain/release methods also allowed future callers to return adapters
without detaching caller buffers. Finally, logging a failed graph invoked
arbitrary `toString()` implementations and could expose payload contents.

## Outcome

JDK and Kryo now bypass the compatibility ByteArray staging path where their
backends safely support fixed caller-owned buffers. Fory avoids the input copy
without claiming unsupported output behavior. Existing ByteArray entry points
and serializer configuration remain unchanged.

## Verification

- Core serializer ByteBuffer suite: 18 tests passed.
- Full `:bluetape4k-io:test` suite: 1055 tests passed.
- Wire parity, exact capacity, overflow, cursor rollback, source-state
  preservation, security filters, registration, pooling, retry, and mixed
  concurrency paths are covered.
- ABI verification is run from the clean committed head before PR publication.

## Future Guard

Do not add convenience members to a public extensible base class without ABI
analysis. Treat pooled adapters as scoped resources and detach caller-owned
storage before reuse. Verify backend capabilities against the resolved dependency
version, and defer allocation claims until #1039 records repeated measurements.
