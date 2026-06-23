# Issue 844 - geo README examples

## Context

`bluetape4k-geo` consolidated the former geocode, geohash, and geoip2 modules,
but the English and Korean READMEs still documented the old
`io.bluetape4k.geo.*` package hierarchy and non-existent helper types.

The installation examples also exposed project-internal Gradle catalog symbols
such as `Libs.feign_core`, which consumers cannot use in their own builds.

## Decision

Rewrite the README examples against the current public API:

- `io.bluetape4k.geohash` factory and extension functions
- `io.bluetape4k.geocode.google.GoogleAddressFinder`
- `io.bluetape4k.geoip2.Geoip` plus `DatabaseReader` extension functions

The installation block now uses Maven coordinates and concrete versions from the
repository version catalog instead of internal `Libs.*` aliases.

## Follow-up Guard

Keep `README.md` and `README.ko.md` source-equivalent when geocode, geohash, or
geoip2 public entry points move. The `GeoReadmeContractTest` blocks the stale
package/API names that caused this issue.
