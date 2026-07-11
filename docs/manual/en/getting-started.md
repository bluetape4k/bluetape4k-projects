---
title: Getting started
description: Select and install a bluetape4k-projects module, then move from guide to manual to runnable example.
---

# Getting started

## 1. Import the BOM

Use the repository version published for your application and let the BOM align bluetape4k artifacts.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
}
```

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

The repository uses Java 21 and Kotlin 2.3. Framework-specific manuals state narrower Spring Boot, Ktor, database, container, or native requirements.
