# Zip Slip canonical path validation

## Context

GitHub CodeQL reported `java/zipslip` in `ZipFileSupport.unzip` at the file
extraction sink.

## Decision

Resolve every ZIP entry through the destination directory's canonical file and
reject targets whose canonical path escapes that directory before opening the
output stream.

## Outcome

The extraction path check now guards the exact canonical file passed to
`FileOutputStream`, including sibling paths that share a textual prefix with
the destination directory name.

## Verification

- `./gradlew :bluetape4k-io:test --tests "io.bluetape4k.io.compressor.ZipFileSupportTest"`
- `git diff --check`

## Future guard

For archive extraction code, validate canonical target paths at the same
abstraction level as the file sink instead of relying only on normalized string
or path comparisons.
