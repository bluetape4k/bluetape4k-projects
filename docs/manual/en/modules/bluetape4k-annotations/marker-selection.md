---
title: Marker selection
description: Choose among the six markers by identifying maturity, public-boundary, operational, and implementation risks.
manualId: bluetape4k-annotations
chapterId: marker-selection
---

# Marker selection

Choosing by name alone tends to overload `Experimental` and `Internal`. Start with the unstable contract: can callers rely on the API, is the declaration public only for technical reasons, does safe use require specialized knowledge, or is only third-party implementation unstable?

![Bluetape API marker selection map](../../../assets/annotations/annotation-decision-map.svg)

## Check visibility first

Prefer `internal` or `private` when external code has no reason to access a declaration. A marker adds policy to a public declaration; it does not replace visibility. Consider a marker when inlining, framework integration, or another technical constraint requires public exposure.

## Contracts of the six markers

| Marker | Diagnostic | Use when | Caller accepts |
| --- | --- | --- | --- |
| `BluetapeExperimentalApi` | Error | The contract may change or disappear | No source or binary compatibility guarantee |
| `BluetapeBetaApi` | Warning | Stabilization is likely, with minor changes still possible | Minor source, binary, or behavior changes |
| `BluetapeInternalApi` | Error | The declaration is public only for technical reasons | It is not supported external API |
| `BluetapeDelicateApi` | Warning | Safe use requires lifecycle, concurrency, resource, or security knowledge | The documented contract and failure modes |
| `BluetapeObsoleteApi` | Error | The API remains only for migration or compatibility | No new use and possible removal |
| `BluetapeImplementationApi` | Warning | Calling is stable but third-party implementation is not | Changes to the implementation or subclassing contract |

`Experimental`, `Internal`, and `Obsolete` block compilation without opt-in. `Beta` and `Delicate` emit warnings. `Implementation` applies when code implements or subclasses a type marked through `@SubclassOptInRequired`, not when it merely calls the type.

## Common boundaries

### Experimental versus Beta

Use `Experimental` while removal or incompatible changes remain plausible. Use `Beta` after the direction is settled and only minor changes are expected. Do not choose `Beta` merely because a warning is more convenient than an error.

### Internal versus Delicate

`Internal` says external use is unsupported. `Delicate` says external use is permitted after the caller understands a demanding contract. The current Kafka binary codecs use `BluetapeDelicateApi` to flag serialization and type-safety boundaries.

### Obsolete versus Deprecated

`Obsolete` is an opt-in policy. Kotlin `@Deprecated` supplies a replacement and migration message to IDEs. They solve different problems and may be combined when an API needs both a hard usage boundary and a migration path.

## Combining markers

One marker usually communicates the owning contract best. Multiple markers can obscure what the caller is accepting. Combine them only when independent contracts genuinely overlap, and explain each contract separately in KDoc.

## Sources

- [Six marker declarations](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations)
- [Marker selection table in the README](../../../../../bluetape4k/annotations/README.md)
- [Delicate marker on Kafka codecs](../../../../../infra/kafka/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodecs.kt)
