---
title: bluetape4k-projects Manual
description: Task-oriented manuals for every registered library, example, and benchmark module.
---

# bluetape4k-projects Manual

This manual is the detailed companion to the repository README. It follows the live Gradle project model and gives every registered module an English and Korean page with the same evidence contract.

## Core capabilities

- **Kotlin foundation:** [Core](modules/bluetape4k-core.md), [logging](modules/bluetape4k-logging.md), and serialization modules provide the shared types, validation, diagnostics, and wire-format building blocks used across the ecosystem.
- **Coroutines and execution models:** [Coroutines](modules/bluetape4k-coroutines.md), virtual-thread modules, and reactive adapters make blocking, asynchronous, and suspending boundaries explicit.
- **Data access:** [JDBC](modules/bluetape4k-jdbc.md), R2DBC, Hibernate, MongoDB, and Cassandra modules add Kotlin-friendly repositories, batching, transactions, and test support.
- **Distributed infrastructure:** [Redis](modules/bluetape4k-redis.md), Kafka, messaging, search, caching, resilience, metrics, and tracing modules cover common service infrastructure without hiding its failure boundaries.
- **Web applications:** [Ktor core](modules/bluetape4k-ktor-core.md), Spring Boot, HTTP client, gRPC, and OpenAPI modules connect the shared libraries to application runtimes.
- **Testing and utilities:** [Assertions](modules/bluetape4k-assertions.md), JUnit 5, Testcontainers, mock servers, time, ID, money, workflow, and other focused utilities shorten repeatable application and test code.

## Choose a path

- **Build:** Start with [Getting started](getting-started.md), then choose a foundation, data, infrastructure, web, or utility module.
- **Learn:** Use example manuals as runnable lessons and follow their links back to the production modules they demonstrate.
- **Apply:** Use benchmark and operations sections to evaluate constraints before adopting a pattern.

## How pages are organized

Every module page explains the problem, selection criteria, dependency coordinates, core concepts, task-oriented APIs, integration patterns, configuration, failure behavior, operations, testing, workshops, limitations, and source evidence.

See the [repository map](architecture/repository-map.md) for the module groups and the [getting started guide](getting-started.md) for the first dependency and code path.
