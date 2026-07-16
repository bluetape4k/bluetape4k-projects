---
title: bluetape4k-projects 매뉴얼
description: 등록된 모든 라이브러리, 예제, 벤치마크 모듈을 위한 작업 중심 매뉴얼입니다.
---

# bluetape4k-projects 매뉴얼

이 매뉴얼은 저장소 README를 대체하는 문서가 아니라 더 깊게 설명하는 동반 문서입니다. 실제 Gradle 프로젝트 모델을 기준으로 모든 등록 모듈에 동일한 근거 계약을 가진 영문·한글 페이지를 제공합니다.

## 핵심 기능

- **Kotlin 기반 기능:** [Core](modules/bluetape4k-core.md), [로깅](modules/bluetape4k-logging.md), 직렬화 모듈에서 생태계 전반이 공유하는 타입, 검증, 진단, 데이터 형식 기능을 제공합니다.
- **코루틴과 실행 모델:** [Coroutines](modules/bluetape4k-coroutines.md), 가상 스레드, 리액티브 어댑터를 이용해 블로킹·비동기·일시 중단 경계를 분명하게 나눌 수 있습니다.
- **데이터 접근:** [JDBC](modules/bluetape4k-jdbc.md), R2DBC, Hibernate, MongoDB, Cassandra 모듈에서 Kotlin에 맞춘 저장소, 배치, 트랜잭션, 테스트 기능을 제공합니다.
- **분산 인프라:** [Redis](modules/bluetape4k-redis.md), Kafka, 메시징, 검색, 캐시, 회복성, 메트릭, 추적 모듈로 서비스 인프라를 구성하되 각 기술의 실패 경계는 그대로 드러냅니다.
- **웹 애플리케이션:** [Ktor Core](modules/bluetape4k-ktor-core.md), Spring Boot, HTTP 클라이언트, gRPC, OpenAPI 모듈로 공통 라이브러리를 애플리케이션 런타임에 연결합니다.
- **테스트와 유틸리티:** [Assertions](modules/bluetape4k-assertions.md), JUnit 5, Testcontainers, 모의 서버와 시간, ID, 금액, 워크플로 같은 목적별 유틸리티로 반복되는 애플리케이션·테스트 코드를 줄입니다.

## 탐색 경로

- **Build:** [시작하기](getting-started.md)에서 출발해 foundation, data, infrastructure, web, utility 모듈을 선택합니다.
- **Learn:** example 매뉴얼을 실행 가능한 수업처럼 사용하고, 예제가 보여 주는 production 모듈로 이동합니다.
- **Apply:** benchmark와 operations 절을 통해 제약과 운영 조건을 검토한 뒤 패턴을 적용합니다.

## 페이지 구성

모든 모듈 페이지는 문제, 선택 기준, 의존성 좌표, 핵심 개념, 작업별 API, 통합 패턴, 설정, 실패 동작, 운영, 테스트, 워크숍, 제한 사항, 소스 근거를 설명합니다.

전체 분류는 [저장소 지도](architecture/repository-map.md), 첫 의존성과 코드 경로는 [시작하기](getting-started.md)를 참고하세요.
