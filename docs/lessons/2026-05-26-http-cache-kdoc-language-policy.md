# HTTP Cache KDoc Language Policy Cleanup

**Date**: 2026-05-26
**Issue**: #633
**Branch**: docs/issue-633-http-cache-kdoc-english

## Problem

`CachingHttpClientBuilder.kt` and `CachingHttpAsyncClientBuilder.kt` had newly added English
KDoc overloads next to older Korean KDoc on the original no-arg cache builder functions.
This violated the contributor documentation policy that meaningfully edited KDoc should stay
in English.

## Solution

Converted the remaining Korean KDoc for the original classic and async HTTP cache builders to
English, matching the style of the new parameterized overloads.

## Validation

- Confirmed no Korean text remains in the two affected builder files.
- Ran `./gradlew :bluetape4k-http:compileKotlin`.

## Lessons

1. When adding overloads with English KDoc, scan adjacent overloads in the same file for older
   localized KDoc before opening the PR.
2. Use the real Gradle module path from `./gradlew projects`; source path `io/http` maps to
   `:bluetape4k-http`, not `:io:http`.
