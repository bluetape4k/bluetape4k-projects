---
title: 저장소 지도
description: 등록 모듈을 Build, Learn, Apply 탐색에 맞게 분류한 지도입니다.
---

# 저장소 지도

매뉴얼 inventory는 Gradle `subprojects`에서 생성합니다. 디렉터리 이름은 탐색을 위한 metadata이며 복사된 진실 원천이 아닙니다.

| 경로 | 매뉴얼 그룹 | 주요 용도 |
| --- | --- | --- |
| `bluetape4k/` | Foundation | core type, annotation, logging, coroutine, BOM |
| `cache/` | Caching | cache 계약과 backend |
| `data/` | Data | JDBC, R2DBC, Hibernate, MongoDB, Cassandra |
| `infra/` | Infrastructure | Redis, messaging, resilience, metric, tracing, search |
| `io/` | I/O | codec, serialization, HTTP, gRPC, Protobuf, Vert.x |
| `ktor/` | Web | Ktor server 기반과 테스트 |
| `spring-boot/` | Spring | Spring Boot 통합 모듈과 demo |
| `testing/` | Testing | assertion, JUnit 지원, container, mock server |
| `utils/` | Utilities | domain 및 algorithm utility |
| `virtualthread/` | Concurrency | virtual thread API와 JDK 구현 |
| `examples/` | Learning | 실행 예제, 비게시 |
| `benchmark/` | Experiments | 재현 가능한 성능 실험, 비게시 |

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Bluetape4k Projects 아키텍처

[![Bluetape4k Projects 아키텍처](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-en-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-en-diagram-01.svg)

_배포본 README: [`README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/README.ko.md)_

### Bluetape4k 프레임워크 개요

[![Bluetape4k 프레임워크 개요](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-overview-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/root-readme-overview-01.svg)

_배포본 README: [`README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/README.ko.md)_

### ServiceLoader provider, 우선순위 정렬, platform fallback을 사용하는 VirtualThreads 런타임 선택 흐름

[![ServiceLoader provider, 우선순위 정렬, platform fallback을 사용하는 VirtualThreads 런타임 선택 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-01.svg)

_배포본 README: [`virtualthread/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/README.ko.md)_

### 런타임 파사드, provider 인터페이스, JDK 구현체, scope 계약, TaskContext를 보여주는 가상 스레드 클래스 구조

[![런타임 파사드, provider 인터페이스, JDK 구현체, scope 계약, TaskContext를 보여주는 가상 스레드 클래스 구조](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-diagram-02.svg)

_배포본 README: [`virtualthread/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/README.ko.md)_

### VirtualThreadRuntime 발견, 지원 여부 필터링, 우선순위 정렬, executor 위임을 보여주는 ServiceLoader 선택 시퀀스

[![VirtualThreadRuntime 발견, 지원 여부 필터링, 우선순위 정렬, executor 위임을 보여주는 ServiceLoader 선택 시퀀스](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/virtualthread-sequence-01.svg)

_배포본 README: [`virtualthread/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/virtualthread/README.ko.md)_

<!-- release-readme-diagrams:end -->
