# Fastjson2 2.0.62 ByteBuffer Capability Evidence

## Resolve된 Artifact

- Coordinate: `com.alibaba.fastjson2:fastjson2:2.0.62`
- Sources JAR SHA-256: `712dba017b892c2e878614b38c94316ebdaab2a0f3104eb055a6ec9bb6b18619`
- Inspected source root: `.codex/lib-sources/fastjson2-2.0.62`
- Inspection date: 2026-07-17

Extraction directory는 temporary review input이며 PR 전에 제거한다. 이 문서가 durable
evidence다.

## Source 발견 사항

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

## 결정

`ByteBuffer.hasArray()`가 true일 때만 offset/length parser를 사용한다. Class token 또는
`reference<T>().type`과 함께 `array()`, `arrayOffset() + position()`, `remaining()`을 전달한다.
Direct 또는 read-only input에는 `source.duplicate()`에서 bounded copy를 사용한다.

`serializeTo`는 명시적으로 allocation하는 compatibility path로 유지한다. `JSONB.toBytes`를 호출하고
target capacity를 검증하며 duplicate를 통해 write하고 성공 후에만 caller position을 commit한다.
Lower-copy 또는 zero-copy output claim은 하지 않는다.

모든 path는 Fastjson2의 feature-free overload를 사용한다. `SupportAutoType`을 enable하지 않고,
AutoType filter를 install하지 않으며, default reader context를 넓히지 않는다.

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

## 기각한 Path

- `JSONReaderJSONB(Context, InputStream)`: internal array로 읽고 이를 grow할 수 있으므로 기각했다.
- `JSONWriterJSONB` storage로 reflection: stable public API가 아니며 compatibility와 security
  reviewability를 약화하므로 기각했다.
- Generic reconstruction 개선을 위한 AutoType enable: 기존 security contract를 바꾸므로 기각했다.
