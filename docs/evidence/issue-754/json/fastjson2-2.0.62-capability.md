# Fastjson2 2.0.62 ByteBuffer Capability Evidence

## Resolved Artifact

- Coordinate: `com.alibaba.fastjson2:fastjson2:2.0.62`
- Sources JAR SHA-256: `712dba017b892c2e878614b38c94316ebdaab2a0f3104eb055a6ec9bb6b18619`
- Inspected source root: `.codex/lib-sources/fastjson2-2.0.62`
- Inspection date: 2026-07-17

The extraction directory is temporary review input and is removed before the PR. This document is the durable evidence.

## Source Findings

1. `com/alibaba/fastjson2/JSONB.java:1653-1670` implements `toBytes(Object)` with an internal
   `JSONWriterJSONB` and returns `writer.getBytes()`.
2. `com/alibaba/fastjson2/JSONWriterJSONB.java:1837-1839` implements `getBytes()` as
   `Arrays.copyOf(bytes, off)`. Public JSONB output therefore materializes a library-owned `byte[]`.
3. `com/alibaba/fastjson2/JSONReaderJSONB.java:106-137` implements the `InputStream` constructor by
   obtaining or allocating an internal `byte[]`, reading the stream into it, and growing it with
   `Arrays.copyOf` when required. Adapting a `ByteBuffer` to this constructor does not remove the input copy.
4. `com/alibaba/fastjson2/JSONB.java:1267-1323` exposes `byte[]`, offset, and length overloads for both
   `Class<T>` and `Type`. They construct `JSONReaderJSONB` over the supplied array range without first
   copying that range.
5. `com/alibaba/fastjson2/JSONReaderJSONB.java:752-791` handles JSONB typed-any metadata and checks
   `Feature.SupportAutoType`; without that feature it reads object/array payloads as data instead of
   resolving and instantiating the encoded class.

## Decision

Use the offset/length parser only when `ByteBuffer.hasArray()` is true. Pass
`array()`, `arrayOffset() + position()`, and `remaining()` with either the class token or
`reference<T>().type`. Use a bounded copy from `source.duplicate()` for direct or read-only input.

Keep `serializeTo` as an explicitly allocating compatibility path: call `JSONB.toBytes`, validate target
capacity, write through a duplicate, and commit the caller position only after success. No lower-copy or
zero-copy output claim is made.

All paths use Fastjson2's feature-free overloads. They do not enable `SupportAutoType`, install an AutoType
filter, or broaden the default reader context.

## Capability Matrix

| Operation / buffer kind | Path | Allocation statement |
|---|---|---|
| Deserialize writable heap buffer | backing `byte[]` + offset/length | Optimized: avoids the adapter-level range copy |
| Deserialize writable heap slice | backing `byte[]` + `arrayOffset()` + position | Optimized: avoids the adapter-level range copy |
| Deserialize direct buffer | duplicate into bounded `byte[]` | Compatibility fallback: one adapter-level copy |
| Deserialize read-only buffer | duplicate into bounded `byte[]` | Compatibility fallback: one adapter-level copy |
| Deserialize concrete reified generic | same cells with `reference<T>().type` | Preserves `List<Model>` and `Map<String, Model>` typing |
| Deserialize through `JsonSerializer` receiver | class-token path | Preserves existing raw generic limitation |
| Serialize to any writable target | `JSONB.toBytes` then duplicate `put` | Allocating compatibility fallback; no allocation reduction claim |

## Rejected Paths

- `JSONReaderJSONB(Context, InputStream)`: rejected because it reads into and may grow an internal array.
- Reflection into `JSONWriterJSONB` storage: rejected because it is not a stable public API and would weaken
  compatibility and security reviewability.
- Enabling AutoType to improve generic reconstruction: rejected because it changes the established security contract.
