# Issue 492 Codec Trust Profiles Design

## Context

Issue #492 asks for public serialization trust profiles, explicit unsafe legacy
modes, Kafka secure/legacy examples, and representative tests proving safe
defaults plus explicit opt-in behavior.

The current code already has several security controls, but the vocabulary is
inconsistent:

- Kafka 3/4 Jackson codecs use `allowedTypePackages`; `emptySet()` denies all
  dynamic class loading and `ALLOW_ALL_TYPES_UNSAFE` explicitly restores the
  legacy allow-all mode.
- `ProtobufSerializer` uses allowlisted type URL prefixes by default.
- `RedissonProtobufCodec` reads `Any.typeUrl`, loads that class name directly,
  and falls back only when protobuf decoding fails. It lacks an allowlist API.
- Redisson Jackson/Fastjson codecs expose `allowedPackagePrefixes`, where
  `null` is the legacy trusted-internal behavior.
- `KryoBinarySerializer` and `ForyBinarySerializer` are trusted-internal binary
  serializers; their `secure` factories provide allowlisted variants.
- `JdkBinarySerializer` applies a default object input filter and remains
  deprecated for general use.

## Trust Profiles

Public docs and API should use these names:

| Profile | Meaning | Typical use |
|---|---|---|
| `TrustedInternal` | Deserializes only data written by the same trusted deployment boundary. | Private caches, in-memory queues, sealed internal Redis/Kafka deployments. |
| `AllowListedTypes` | Dynamic class/type loading is permitted only for configured package prefixes or filters. | Shared infrastructure, topic/cache boundaries, mixed producers. |
| `NoDynamicTypeLoading` | Caller supplies the target type statically; serialized data does not choose a class. | JSON value-type codecs and non-polymorphic APIs. |
| `UnsafeLegacyCompatibility` | Previous allow-all behavior is available only through an explicit unsafe name/configuration. | Temporary migration from older wire formats or trusted legacy deployments. |

## Scope

Implement a bounded API and documentation change:

1. Add a small public enum in `bluetape4k-io` to make the trust profile names
   stable and reusable across codec docs.
2. Harden `RedissonProtobufCodec` to match `ProtobufSerializer` by default:
   allow only `ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES`, expose
   `allowedClassPrefixes`, use Kryo5 fallback for non-Protobuf values, and let
   callers opt into legacy compatibility with an explicit unsafe constant.
3. Tighten `ProtobufSerializer` custom allowlist matching so package-prefix
   spoofing and blank prefixes are rejected consistently with the Redisson
   protobuf codec.
4. Keep existing Kafka runtime behavior. Fix Kafka4 README drift where it says
   `emptySet()` allows all even though the code denies all.
5. Add public docs describing profile defaults and migration choices.
6. Add representative tests for default-safe and explicit opt-in behavior using
   `RedissonProtobufCodec`, because it currently has the clearest gap.

## Non-Goals

- Do not rewrite every serializer to a common interface in this PR.
- Do not change Kafka wire format or `allowedTypePackages` semantics.
- Do not change Redisson Jackson/Fastjson defaults in this PR; document their
  trusted-internal default and existing allowlist option.
- Do not introduce new dependencies.

## Compatibility

`RedissonProtobufCodec` must preserve current default success for bluetape4k and
Google protobuf messages. Intended behavior changes are rejecting a protobuf
`Any.typeUrl` whose class name is outside the allowlist before class loading
occurs, rejecting spoofed/blank allowlist prefixes, and using Kryo5 instead of
JDK fallback for non-Protobuf payloads.

An explicit unsafe opt-in constant should be available for legacy deployments
that stored protobuf `Any` messages using application package names outside the
default prefixes.

## Acceptance Mapping

| Issue acceptance | Design response |
|---|---|
| Public docs describe codec trust profiles and defaults. | Add central codec trust profile docs and link/update affected README files. |
| Unsafe legacy modes explicit in API names/config. | Document `ALLOW_ALL_TYPES_UNSAFE` and add `RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE`. |
| At least Kafka docs include secure and legacy examples. | Update Kafka and Kafka4 README pairs with secure/legacy examples and corrected defaults. |
| Tests cover default-safe and explicit opt-in behavior for one representative codec. | Add Redisson protobuf tests for rejected untrusted type URL and explicit unsafe opt-in. |

## Step 2-R Session Review

External Claude/Codex CLI advisor passes are intentionally skipped by user
directive for this session. Review was performed in the current Codex session.

| Perspective | P0 | P1 | P2/P3 | Verdict |
|---|---:|---:|---|---|
| Developer | 0 | 0 | P2: avoid broad serializer interface in this PR. | Scope stays bounded. |
| Security | 0 | 0 | P2: document Redisson Jackson/Fastjson trusted-internal defaults. | Added to docs scope. |
| Ops/SRE | 0 | 0 | P3: migration rollback should be explicit. | Unsafe opt-in and fallback behavior recorded. |
| User/caller | 0 | 0 | P2: Kafka4 README drift must be fixed. | Included as required task. |

Step 2-R convergence: `P0 = 0`, `P1 = 0`.
