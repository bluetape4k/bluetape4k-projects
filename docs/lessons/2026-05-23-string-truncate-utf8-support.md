# Lesson: Promote byte-safe UTF-8 truncation through bluetape4k-core first

**Date**: 2026-05-23
**Related**: bluetape4k-leader#270

## Context

`bluetape4k-leader` has an internal `String.truncateUtf8(maxBytes)` helper for
history error-message truncation. The helper is general-purpose and belongs in
the shared support package, but downstream repositories cannot safely consume it
until the public API is present in a published `bluetape4k-core` artifact.

## Decision

Add `io.bluetape4k.support.truncateUtf8(maxBytes)` to `bluetape4k-core` with
the same byte-boundary contract as the leader-internal implementation. Keep the
API small: no ellipsis, no grapheme-cluster guarantees, and no nullable receiver
overload.

## Follow-up

After this API is released in the BOM version consumed by `bluetape4k-leader`,
the leader repository can remove its internal copy and import the shared support
function without breaking CI on a missing Maven Central symbol.
