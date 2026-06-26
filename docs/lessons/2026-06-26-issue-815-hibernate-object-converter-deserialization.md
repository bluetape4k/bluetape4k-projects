# Issue 815 - Hibernate Object Converter Deserialization Boundary

## Context

Generic Hibernate object converters accepted `Any?` and deserialized database payloads without an explicit type boundary. That made persisted object columns a trust boundary whenever rows could be tampered with, imported, or shared across tenants.

## Decision

Keep the legacy converters for compatibility, but deprecate them as trusted-storage-only. Add typed converter bases that require a target class and serializer, then reject deserialized values that do not match the expected type. For Kryo and Fory paths, prefer secure serializer factories such as `KryoBinarySerializer.secure(...)` and `ForyBinarySerializer.secureFory(...)`.

## Outcome

The public API now gives callers a migration path from generic `Any?` converters to typed converter subclasses. Negative tests cover malformed payloads, unexpected JDK payload types, and secure Kryo/Fory disallowed payloads.

## Future Guidance

When adding persistence converters around binary serialization, make the trusted/untrusted boundary explicit in both API shape and README examples. Compatibility wrappers can remain, but they should not be the recommended path for persisted, externally mutable data.
