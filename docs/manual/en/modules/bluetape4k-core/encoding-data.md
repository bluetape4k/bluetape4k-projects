---
title: Encoding and data boundaries
description: Make format and failure explicit across byte, string, Base64, and hex conversions.
manualId: bluetape4k-core
chapterId: encoding-data
---

# Encoding and data boundaries

## Problem to solve

Moving binary data through text requires an explicit charset and wire format.

## Mental model

Encoding is not encryption. Base64 fits binary transport, while hex fits short identifiers and human inspection.

## Smallest API surface

Choose byte and string helpers whose names expose the format and specify a charset for text conversion.

## Complete example

Convert UTF-8 text to bytes, encode it as Base64, then decode and reconstruct the same UTF-8 text.

## Selection guide

Choose Base64 or hex from protocol requirements, size overhead, and human readability.

## Failure, cancellation, and lifecycle contract

Do not turn malformed input into an empty value. Never log a secret merely because it is encoded.

## Operations and diagnosis

Observe allocation and payload expansion for large conversions and switch to streaming when required.

## Source and representative tests

Use the [`encoding`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/codec) source and representative tests to confirm each public function.

## Next chapter and runnable workshop

See [Validation and invariants](./validation.md) for failure policy and [Recipes](./recipes.md) for composition.
