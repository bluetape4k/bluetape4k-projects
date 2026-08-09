---
title: Getting started
description: Select and install a bluetape4k-projects module, then move from guide to manual to runnable example.
---

# Getting started

## 1. Import the BOM

Choose the published `bluetape4k-dependencies` version for your application and let the central BOM align every bluetape4k artifact.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
}
```

Here, `<version>` is the published version of `bluetape4k-dependencies`. The central BOM manages repository BOM versions, so consumers do not align them separately.

## 2. Add one task-focused module

Choose the smallest module that owns the capability. A module manual provides the exact artifact coordinate and compatibility notes.

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-core")
}
```

## 3. Follow the evidence trail

Read the guide for orientation, use the module manual for contracts and failure behavior, then run a linked example or workshop. Source and test links in each manual are the final authority when a README summary is ambiguous.

## 4. Validate the environment

The repository uses Java 25, Kotlin 2.4, and the Gradle 9.7.0 Wrapper. The tracked `.java-version` selects JDK 25; `virtualthread/jdk21` and its minimal API, logging, and testing dependency closure retain Java 21 compatibility. Framework-specific manuals state narrower Spring Boot, Ktor, database, container, or native requirements.
