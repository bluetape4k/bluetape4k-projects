---
title: Selective dependency migration
description: Inventory umbrella usages and safely reduce the build to a Lettuce-only or Redisson-only coordinate.
manualId: bluetape4k-redis
chapterId: selective-dependency-migration
---

# Selective dependency migration

## Inventory real usage first

Replacing the umbrella is more than changing one dependency string. Inspect imports, Spring beans, reflection and configuration, test fixtures, and the runtime classpath.

```bash
rg 'io\.bluetape4k\.redis\.(lettuce|redisson)|io\.lettuce|org\.redisson' src
./gradlew :application:dependencies --configuration runtimeClasspath
```

Spring configuration can create a `RedissonClient` without application imports. Spring Data Redis can retain Lettuce as its default driver without direct bluetape4k Lettuce usage. Treat removal of bluetape4k client helpers and selection of the Spring Data driver as separate decisions.

## Reduce one boundary at a time

1. Restrict new code to the target client.
2. Find factories, beans, Codecs, and shutdown hooks for the client being removed.
3. Verify shared-key compatibility or allocate a new key prefix.
4. Pass contract and integration tests with the target client.
5. Replace the umbrella coordinate with the selective coordinate.
6. Inspect the runtime dependency report for the removed client and unused Codec runtimes.

A Lettuce-only build looks like this:

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-lettuce")
}
```

Use `bluetape4k-redisson` instead for a Redisson-only build. Do not attach separate versions to either coordinate.

## Separate Codec migration from dependency migration

Changing the client and stored format in one deployment makes decode failures difficult to diagnose. If old keys must survive, first test the exact wire format. When formats differ, use a new prefix and design dual writes, backfill, cutover, and expiration or deletion of old keys.

## Completion criteria

- Source imports and beans for the removed client are gone.
- Shutdown hooks and metrics no longer reference it.
- The Codec migration for shared Redis keys is verified.
- The runtime classpath no longer contains the artifact, or its remaining owner is recorded.
- Application integration tests pass against a real Redis instance.

Another library may still bring Lettuce or Redisson transitively. That does not prove a client instance is running, but dependency ownership still matters for security and capacity management.

## Release sources

- [`infra/redis/build.gradle.kts`](../../../../../infra/redis/build.gradle.kts)
- [`infra/lettuce/build.gradle.kts`](../../../../../infra/lettuce/build.gradle.kts)
- [`infra/redisson/build.gradle.kts`](../../../../../infra/redisson/build.gradle.kts)
- [Umbrella dependency contract](./umbrella-dependency-contract.md)
