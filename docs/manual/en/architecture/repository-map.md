---
title: Repository map
description: How the registered modules are grouped for Build, Learn, and Apply navigation.
---

# Repository map

The manual inventory is generated from Gradle `subprojects`; directory names are navigation metadata, not a copied source of truth.

| Path | Manual group | Primary use |
| --- | --- | --- |
| `bluetape4k/` | Foundation | Core types, annotations, logging, coroutines, and BOM |
| `cache/` | Caching | Cache contracts and backends |
| `data/` | Data | JDBC, R2DBC, Hibernate, MongoDB, and Cassandra |
| `infra/` | Infrastructure | Redis, messaging, resilience, metrics, tracing, and search |
| `io/` | I/O | Codecs, serialization, HTTP, gRPC, Protobuf, and Vert.x |
| `ktor/` | Web | Ktor server foundations and testing |
| `spring-boot/` | Spring | Spring Boot integration modules and demos |
| `testing/` | Testing | Assertions, JUnit support, containers, and mock servers |
| `utils/` | Utilities | Domain and algorithmic utilities |
| `virtualthread/` | Concurrency | Virtual-thread APIs and JDK implementations |
| `examples/` | Learning | Runnable examples; not published |
| `benchmark/` | Experiments | Reproducible performance investigations; not published |

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### Module Structure diagram

[![Module Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-en-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-en-diagram-01.svg)

_Release README: [`README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/README.md)_

### Bluetape4k framework overview diagram

[![Bluetape4k framework overview diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-overview-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-overview-01.svg)

_Release README: [`README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/README.md)_

### VirtualThreads runtime selection flow using ServiceLoader providers, priority sorting, and platform fallback

[![VirtualThreads runtime selection flow using ServiceLoader providers, priority sorting, and platform fallback](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-01.svg)

_Release README: [`virtualthread/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/README.md)_

### Virtual thread class structure for runtime facades, provider interfaces, JDK implementations, scope contracts, and TaskContext

[![Virtual thread class structure for runtime facades, provider interfaces, JDK implementations, scope contracts, and TaskContext](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-02.svg)

_Release README: [`virtualthread/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/README.md)_

### ServiceLoader selection sequence for VirtualThreadRuntime discovery, support filtering, priority sorting, and executor delegation

[![ServiceLoader selection sequence for VirtualThreadRuntime discovery, support filtering, priority sorting, and executor delegation](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-sequence-01.svg)

_Release README: [`virtualthread/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/README.md)_

<!-- release-readme-diagrams:end -->
