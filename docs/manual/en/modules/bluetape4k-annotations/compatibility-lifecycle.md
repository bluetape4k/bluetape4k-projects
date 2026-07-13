---
title: Compatibility lifecycle
description: Manage marker declarations and downstream opt-ins as APIs stabilize, become obsolete, and are eventually removed.
manualId: bluetape4k-annotations
chapterId: compatibility-lifecycle
---

# Compatibility lifecycle

A marker is more than a label for the current release. It defines the compatibility review required before stabilization or removal. Downstream source may also refer to the marker annotation class directly, so the marker itself has a lifecycle beyond any one marked API.

## Experimental to stable

Before stabilization, verify:

- Public signatures and generic types have settled
- Exceptions and failure behavior are documented
- Thread-safety and resource ownership are explicit
- Representative examples and regression tests exist
- Source and binary compatibility can be maintained across expected minor releases

After that review, remove the marker from the API declaration. Downstream `@OptIn` sites can then be removed.

## Beta is not a softer Experimental

`Beta` emits a warning, but it should still represent a settled direction. Keep `Experimental` while large signature changes or removal remain plausible. Use `Beta` when only minor source, binary, or behavior changes are expected.

## Removing obsolete APIs

`BluetapeObsoleteApi` blocks new use unless the caller explicitly opts in. It does not provide a replacement or migration message.

```kotlin
@Deprecated(
    message = "Use decodeV2 instead.",
    replaceWith = ReplaceWith("decodeV2(bytes)"),
)
@BluetapeObsoleteApi
fun decodeLegacy(bytes: ByteArray): Value = TODO()
```

Combine it with `@Deprecated` when callers need an IDE-visible migration path. Delete the API only after applying the project's major-version policy and checking downstream migration.

## Marker classes outlive marked declarations

Do not immediately remove `BluetapeExperimentalApi` from the artifact after every marked API stabilizes. Downstream code may still name it in `@OptIn(BluetapeExperimentalApi::class)`. Removing the marker from an API and deleting the marker annotation class are separate compatibility changes.

Delete a marker class intentionally in a major version and include it in migration notes.

## Upgrade audit

```bash
rg -n "@OptIn\(|Bluetape(Experimental|Beta|Internal|Delicate|Obsolete|Implementation)Api" src
```

For each result, ask:

1. Does the marked API still have the same status?
2. Is a more stable replacement available?
3. Can the opt-in scope be reduced to a function?
4. Is the restriction documented for Java callers?
5. Is a compiler option silently accepting new uses?

## Sources

- [README compatibility note](../../../../../bluetape4k/annotations/README.md)
- [Marker type-name stability test](../../../../../bluetape4k/annotations/src/test/kotlin/io/bluetape4k/annotations/BluetapeApiMarkersTest.kt)
- [Obsolete marker contract](../../../../../bluetape4k/annotations/src/main/kotlin/io/bluetape4k/annotations/BluetapeObsoleteApi.kt)
