#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const OUT = join(process.cwd(), "docs/images/readme-diagrams");
const CAIROSVG = process.env.CAIROSVG ?? "cairosvg";
const RSVG = process.env.RSVG_CONVERT ?? "/opt/homebrew/bin/rsvg-convert";
const ONLY = new Set((process.env.DIAGRAM_ONLY ?? "").split(",").map((item) => item.trim()).filter(Boolean));

const colors = {
  blue: ["#EFF6FF", "#2563EB", "#1D4ED8"],
  green: ["#F0FDF4", "#16A34A", "#15803D"],
  teal: ["#F0FDFA", "#0D9488", "#0F766E"],
  amber: ["#FFF7ED", "#EA580C", "#C2410C"],
  pink: ["#FDF2F8", "#DB2777", "#BE185D"],
  purple: ["#FAF5FF", "#9333EA", "#7E22CE"],
  olive: ["#F7FEE7", "#65A30D", "#4D7C0F"],
  gray: ["#F9FAFB", "#6B7280", "#4B5563"],
};

const sourceModels = {
  "root-readme-overview-01": {
    intent: "Explain the repository-wide bluetape4k ecosystem by grouping modules into foundation, data/cache, integration/runtime, and applications/testing boundaries so README readers understand where to start.",
    evidence: ["README.md", "settings.gradle.kts", "AGENTS.md module groups"],
  },
  "root-readme-en-diagram-01": {
    intent: "Show the repository module structure as a stack of published module families, not a flat inventory, so readers can locate foundation, I/O, data, infrastructure, application, and utility modules.",
    evidence: ["README.md", "settings.gradle.kts", "AGENTS.md module groups"],
  },
  "bluetape4k-core-diagram-01": {
    intent: "Explain bluetape4k-core as the shared foundation: caller contracts and type helpers feed value models, collections, time, codec, functional, and concurrency utilities used by other modules.",
    evidence: ["bluetape4k/core/README.md", "bluetape4k/core/src/main/kotlin/io/bluetape4k"],
  },
  "bluetape4k-coroutines-diagram-01": {
    intent: "Explain bluetape4k-coroutines by grouping async primitives, Flow operators, scope/runtime helpers, Reactor bridges, and test support around coroutine-first APIs.",
    evidence: ["bluetape4k/coroutines/README.md", "bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines"],
  },
  "bluetape4k-coroutines-diagram-03": {
    intent: "Explain Flow extension taxonomy by showing how collection-style batching, concurrency, replay/subject helpers, and test collectors sit around the Flow receiver.",
    evidence: ["bluetape4k/coroutines/README.md", "bluetape4k/coroutines/src/main/kotlin/io/bluetape4k/coroutines/flow"],
  },
  "bluetape4k-logging-diagram-02": {
    intent: "Explain logging processing as a caller-to-logger path where Kotlin delegates, lazy message suppliers, MDC/context helpers, and backend logging calls remain separate responsibilities.",
    evidence: ["bluetape4k/logging/README.md", "bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging"],
  },
  "data-jdbc-diagram-01": {
    intent: "Explain JDBC extension APIs by showing caller SQL, DataSource/Connection helpers, statement/batch execution, ResultSet typed access, and domain mapping boundaries.",
    evidence: ["data/jdbc/README.md", "data/jdbc/src/main/kotlin/io/bluetape4k/jdbc"],
  },
  "data-r2dbc-diagram-01": {
    intent: "Explain R2DBC extension APIs by showing ConnectionFactory/DatabaseClient receivers, bind/query helpers, transaction/coroutine bridges, and Flow/suspend result boundaries.",
    evidence: ["data/r2dbc/README.md", "data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc"],
  },
  "io-feign-diagram-01": {
    intent: "Explain Feign + coroutines integration by separating public builder support, coroutine builder path, codec/request helpers, Vert.x transport adapters, and the remote HTTP runtime.",
    evidence: ["io/feign/README.md", "io/feign/src/main/kotlin/io/bluetape4k/feign"],
  },
  "io-grpc-diagram-02": {
    intent: "Explain gRPC component overview by separating server/client abstractions, managed channel support, in-process testing variants, interceptors, and concrete transport runtime.",
    evidence: ["io/grpc/README.md", "io/grpc/src/main/kotlin/io/bluetape4k/grpc"],
  },
  "io-http-diagram-01": {
    intent: "Explain multi-backend HTTP architecture by grouping the common facade/cross-cutting support and parallel HC5, OkHttp3, Vert.x, Ktor, and JDK client stacks.",
    evidence: ["io/http/README.md", "io/http/src/main/kotlin/io/bluetape4k/http"],
  },
  "testing-assertions-diagram-01": {
    intent: "Explain testing assertions as public assertion DSL families backed by coroutine assertions and a shared internal failure/message engine that raises AssertionError.",
    evidence: ["testing/assertions/README.md", "testing/assertions/src/main/kotlin/io/bluetape4k/assertions"],
  },
  "testing-mock-web-server-diagram-01": {
    intent: "Explain servlet mock web server request routing by separating Testcontainers entrypoints, HTTP/HTTPS port configuration, controllers, services, repositories, and fixture data.",
    evidence: ["testing/mock-web-server/README.md", "testing/mock-web-server/src/main/kotlin/io/bluetape4k/mockserver", "testing/mock-web-server/src/main/resources/application.yml"],
  },
  "testing-mock-webflux-server-diagram-01": {
    intent: "Explain WebFlux mock server routing by separating Testcontainers entrypoints, SmartLifecycle HTTPS setup, Reactor Netty runtime, controllers, services, repositories, and fixtures.",
    evidence: ["testing/mock-webflux-server/README.md", "testing/mock-webflux-server/src/main/kotlin/io/bluetape4k/mockwebflux", "testing/mock-webflux-server/src/main/resources/application.yml"],
  },
  "utils-javatimes-diagram-01": {
    intent: "Explain Java time utilities by grouping date/time DSL receivers, interval and period abstractions, calendar range operations, date arithmetic, and coroutine range flows.",
    evidence: ["utils/javatimes/README.md", "utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes"],
  },
  "utils-jwt-diagram-01": {
    intent: "Explain JWT create and verify flow by showing claims/header input, algorithm/key selection, signing, token output, parser verification, and decoded claims/error outcomes.",
    evidence: ["utils/jwt/README.md", "utils/jwt/src/main/kotlin/io/bluetape4k/jwt"],
  },
  "utils-money-diagram-02": {
    intent: "Explain currency operation flow by showing MonetaryAmount inputs, currency/exchange-rate support, arithmetic/conversion helpers, rounding, and formatted output.",
    evidence: ["utils/money/README.md", "utils/money/src/main/kotlin/io/bluetape4k/money"],
  },
  "utils-mutiny-diagram-02": {
    intent: "Explain Mutiny processing flow by showing Uni/Multi receivers, coroutine await/Flow bridges, retry/transform helpers, and terminal suspend/Flow outcomes.",
    evidence: ["utils/mutiny/README.md", "utils/mutiny/src/main/kotlin/io/bluetape4k/mutiny"],
  },
  "infra-kafka4-diagram-01": {
    intent: "Explain Kafka4 module dependency architecture by distinguishing the module-owned API/producer/consumer/serialization capabilities from external Kafka cluster and Spring/runtime boundaries.",
    evidence: ["infra/kafka4/README.md", "infra/kafka4/src/main/kotlin/io/bluetape4k/kafka4"],
  },
  "utils-geo-diagram-01": {
    intent: "Explain geo utilities by separating public geocode/geohash/geoip APIs from provider adapters, spatial models, MaxMind readers, and external datasets.",
    evidence: ["utils/geo/README.md", "utils/geo/src/main/kotlin/io/bluetape4k/geocode", "utils/geo/src/main/kotlin/io/bluetape4k/geohash", "utils/geo/src/main/kotlin/io/bluetape4k/geoip"],
  },
  "utils-science-diagram-01": {
    intent: "Explain science utilities as coordinate/projection helpers feeding geometry operations, file readers, Exposed persistence tables, repositories, and import services.",
    evidence: ["utils/science/README.md", "utils/science/src/main/kotlin/io/bluetape4k/science"],
  },
  "infra-kafka-diagram-02": {
    intent: "Explain the Kafka Streams topology as the reader writes it: source topic, stream creation, key grouping, materialized count table, stream conversion, produced output.",
    evidence: ["infra/kafka/README.md", "docs/superpowers/specs/2026-05-03-kafka4-module-design.md"],
  },
  "infra-redisson-diagram-02": {
    intent: "Explain Redisson batch and transaction processing from source: batch queues commands and executes once, while transaction actions branch to commit on success or rollback and rethrow on failure, with suspend variants awaiting async Redisson APIs.",
    evidence: [
      "infra/redisson/README.md",
      "infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/RedissonClientExtensions.kt",
      "infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/coroutines/RedissonClientCoroutine.kt",
    ],
  },
  "infra-micrometer-diagram-02": {
    intent: "Explain metric collection flow in bluetape4k-micrometer: application suspend/Flow, HTTP/cache, and event work enter helper families, then record through MeterRegistry or ObservationRegistry into timer metrics and observation signals.",
    evidence: [
      "infra/micrometer/README.md",
      "infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/TimerExtensions.kt",
      "infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/instrument/retrofit2/RetrofitCallMetricsCollector.kt",
      "infra/micrometer/src/main/kotlin/io/bluetape4k/micrometer/observation/events/EventTelemetryObservationSupport.kt",
    ],
  },
  "infra-redis-diagram-01": {
    intent: "Explain the Redis umbrella module dependency choice: bluetape4k-redis keeps the full Lettuce and Redisson bundle compatible, direct Lettuce/Redisson dependencies reduce footprint, and Spring Data Redis serializers stay in a separate module family.",
    evidence: [
      "infra/redis/README.md",
      "infra/lettuce/README.md",
      "infra/redisson/README.md",
      "spring-boot/redis/README.md",
    ],
  },
  "data-hibernate-reactive-diagram-03": {
    intent: "Compare Stage and Mutiny reactive session APIs by keeping each factory close to the session type it creates.",
    evidence: ["data/hibernate-reactive/README.md", "StageSessionFactoryExtensions.kt", "MutinySessionFactoryExtensions.kt"],
  },
  "infra-elasticsearch-diagram-02": {
    intent: "Show Elasticsearch client construction, transport/mapping support, and search/bulk runtime flows without tangent connector starts.",
    evidence: ["infra/elasticsearch/README.md", "ElasticsearchClients.kt", "ElasticsearchClientExtensions.kt"],
  },
  "spring-boot-core-diagram-03": {
    intent: "Explain how application code enters suspend RestClient helpers and how those helpers map to Spring RestClient plus HTTP exchange.",
    evidence: ["spring-boot/core/README.md", "RestClientCoroutinesExtensions.kt"],
  },
  "spring-boot-core-diagram-02": {
    intent: "Replace the generic WebFlux candy-chain with the actual coroutine WebFlux request model: HTTP enters WebFlux, controllers choose Default/IO/VT coroutine scopes, WebClient/WebTestClient helpers adapt request bodies, and Reactor/Flow bridges carry responses.",
    evidence: ["spring-boot/core/README.md", "spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller", "spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/WebClientExtensions.kt", "spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/WebTestClientExtensions.kt"],
  },
  "spring-boot-core-diagram-04": {
    intent: "Redraw from scratch as an optional Spring Boot + Retrofit2 integration map: Spring owns bean configuration, bluetape4k-retrofit2 owns Retrofit assembly, and selectable HTTP clients/converters/call adapters route to the external API.",
    evidence: ["spring-boot/core/README.md", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitSupport.kt", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients"],
  },
  "spring-boot-cassandra-diagram-02": {
    intent: "Replace the vertical candy-chain with a compact layered data-access map: repositories call coroutine extension families, those adapt Spring Data Cassandra APIs, and driver/session utilities reach the Cassandra cluster.",
    evidence: ["spring-boot/cassandra/README.md", "spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra"],
  },
  "spring-boot-hibernate-lettuce-diagram-01": {
    intent: "Redesign the UML placeholder as the actual auto-configuration component map: properties, Hibernate customizer, metrics binder, and actuator endpoint around LettuceNearCacheRegionFactory.",
    evidence: ["spring-boot/hibernate-lettuce/README.md", "spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce"],
  },
  "spring-boot-hibernate-lettuce-diagram-02": {
    intent: "Redesign activation as a decision flow: classpath/property conditions decide whether Hibernate customizer, metrics binder, and actuator endpoint are registered.",
    evidence: ["spring-boot/hibernate-lettuce/README.md", "LettuceNearCacheHibernateAutoConfiguration.kt", "LettuceNearCacheMetricsAutoConfiguration.kt", "LettuceNearCacheActuatorAutoConfiguration.kt"],
  },
  "spring-boot-hibernate-lettuce-demo-diagram-02": {
    intent: "Redesign the demo runtime flow from source: Product REST requests use Spring Data JPA/Hibernate 2LC; L1 Caffeine, L2 Redis, H2, cache management, and actuator stats have distinct paths.",
    evidence: ["spring-boot/hibernate-lettuce-demo/README.md", "spring-boot/hibernate-lettuce-demo/src/main/kotlin/io/bluetape4k/examples/cache/lettuce"],
  },
  "spring-boot-mongodb-diagram-01": {
    intent: "Redesign the class overview so Reactive Mongo operations extensions sit below their receiver, while Criteria/Query/Update DSL helpers stay grouped by responsibility.",
    evidence: ["spring-boot/mongodb/README.md", "spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb"],
  },
  "spring-boot-mongodb-diagram-02": {
    intent: "Redesign the coroutine extension flow around actual conversions: ReactiveMongoOperations returns Flux/Mono, extensions expose Flow/suspend results, and writes return Update/Delete metadata.",
    evidence: ["spring-boot/mongodb/README.md", "ReactiveMongoOperationsCoroutines.kt"],
  },
  "spring-boot-mongodb-diagram-03": {
    intent: "Redesign the Criteria/Query/Update DSL as a query-building flow, not a layered call chain.",
    evidence: ["spring-boot/mongodb/README.md", "CriteriaExtensions.kt", "QueryExtensions.kt", "UpdateExtensions.kt"],
  },
  "spring-boot-r2dbc-diagram-02": {
    intent: "Redesign R2DBC coroutine data flow around R2dbcEntityOperations and reactive CRUD operation specs converging on Flow/suspend APIs.",
    evidence: ["spring-boot/r2dbc/README.md", "spring-boot/r2dbc/src/main/kotlin/io/bluetape4k/spring/r2dbc/coroutines"],
  },
  "spring-boot-redis-diagram-02": {
    intent: "Redesign ReactiveRedisTemplate serialization flow around RedisSerializationContext, key/value/hash serializers, BinarySerializer/Compressor choices, and Redis wire bytes.",
    evidence: ["spring-boot/redis/README.md", "spring-boot/redis/src/main/kotlin/io/bluetape4k/spring/redis/serializer"],
  },
  "testing-junit5-diagram-01": {
    intent: "Redesign the Extension Component Overview as a capability map around JUnit lifecycle extension points instead of a fake sequential layered flow.",
    evidence: ["testing/junit5/README.md", "testing/junit5/src/main/kotlin/io/bluetape4k/junit5"],
  },
  "spring-boot-cassandra-demo-diagram-01": {
    intent: "Show the Cassandra demo request path from web entrypoint to coroutine service, repository DSL, CqlSession, and Cassandra cluster.",
    evidence: ["examples/spring-boot/cassandra-demo/README.md", "examples/spring-boot/cassandra-demo/src/main"],
  },
  "examples-spring-boot-observability-spring-boot-demo-architecture-01": {
    intent: "Explain the Spring Boot observability demo request path: controller delegates to OrderEventService, observeSpring wraps the HTTP boundary, EventTelemetry records publish/consume observations, and Actuator/OTLP expose metrics and optional traces.",
    evidence: [
      "examples/spring-boot/observability-spring-boot-demo/README.md",
      "examples/spring-boot/observability-spring-boot-demo/src/main/kotlin/io/bluetape4k/examples/spring/observability/ObservabilitySpringBootDemoApplication.kt",
      "examples/spring-boot/observability-spring-boot-demo/src/main/resources/application.yaml",
    ],
  },
  "io-csv-diagram-02": {
    intent: "Show real CSV/TSV processing flow: input source, sync reader/writer or suspend Flow path, lexer/writer engine, row mapping, and output.",
    evidence: ["io/csv/README.md", "io/csv/src/main/kotlin/io/bluetape4k/csv", "io/csv/src/main/kotlin/io/bluetape4k/csv/coroutines", "io/csv/src/main/kotlin/io/bluetape4k/csv/v2"],
  },
  "io-json-diagram-02": {
    intent: "Help README readers choose a JsonSerializer implementation by Jackson stack and binary JSONB needs, then converge on the common JsonSerializer API.",
    evidence: ["io/json/README.md", "io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt", "io/fastjson2", "io/jackson2", "io/jackson3"],
  },
  "io-netty-diagram-01": {
    intent: "Show the ByteBuf extension surface without a panoramic row: ByteBuf is the receiver, BitBuf wraps it, and utility extensions stay below their target types.",
    evidence: ["io/netty/README.md", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufExtensions.kt", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/BitBuf.kt", "io/netty/src/main/kotlin/io/bluetape4k/netty/util/ReferenceCountedSupport.kt"],
  },
  "io-netty-diagram-02": {
    intent: "Explain Smart encoding as a range-based decision flow: small values use fewer bytes, larger values switch to marker-bit encodings.",
    evidence: ["io/netty/README.md#smart-encoding", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufExtensions.kt", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/Smart.kt", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/USmart.kt"],
  },
  "io-netty-diagram-03": {
    intent: "Replace the unsupported channel-pipeline inventory with the actual module processing story: ByteBuf read/write families transform bytes through endian/offset/smart/string helpers.",
    evidence: ["io/netty/README.md", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufExtensions.kt", "io/netty/src/main/kotlin/io/bluetape4k/netty/buffer/ByteBufUtilSupport.kt"],
  },
  "io-protobuf-diagram-02": {
    intent: "Show the bidirectional conversion paths between Java/Kotlin domain types, protobuf well-known types, Any packing, and ByteArray serialization.",
    evidence: ["io/protobuf/README.md", "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DurationSupport.kt", "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/DateTimeSupport.kt", "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt", "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MessageSupport.kt"],
  },
  "io-retrofit2-diagram-01": {
    intent: "Show how retrofitOf assembles the public Retrofit surface: service interface, converter factories, result/reactive call adapters, and selectable HTTP call factories.",
    evidence: ["io/retrofit2/README.md", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/RetrofitSupport.kt", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients"],
  },
  "io-retrofit2-diagram-02": {
    intent: "Explain Result<T> integration as a request/response adaptation flow, not a layered inventory of Retrofit classes.",
    evidence: ["io/retrofit2/README.md#result-pattern-support", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCallAdapterFactory.kt", "io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/result/ResultCall.kt"],
  },
  "io-vertx-diagram-01": {
    intent: "Replace the unclear dependency structure with Vert.x module capability architecture: coroutine dispatcher helpers, Future resilience decorators, routing helpers, and SQL/MyBatis support around Vert.x core types.",
    evidence: ["io/vertx/README.md", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/CoroutineSupport.kt", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/VertxSupport.kt", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/resilience4j", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/sqlclient"],
  },
  "io-vertx-diagram-03": {
    intent: "Show the Vert.x SQL client class surface with SqlClient below the SQL helper families so query/render/row routes stay short and aligned.",
    evidence: ["io/vertx/README.md", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/sqlclient/SqlClientSupport.kt", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/sqlclient/PoolSupport.kt", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/sqlclient/mybatis/SqlClientExtensions.kt", "io/vertx/src/main/kotlin/io/bluetape4k/vertx/sqlclient/mybatis/MybatisRenderSupport.kt"],
  },
  "ktor-core-architecture-01": {
    intent: "Show Ktor core as a layered install/runtime architecture: application installs the baseline, config gates plugins, plugins expose JSON/errors/health, routes use request helpers.",
    evidence: ["ktor/core/README.md", "ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCore.kt", "ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kKtorCoreConfig.kt", "ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/Bluetape4kStatusPages.kt", "ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/KtorHealthRoutes.kt", "ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/KtorRequestParameters.kt"],
  },
  "ktor-observability-component-01": {
    intent: "Show Ktor observability installation as components around CallId, CallLogging, Micrometer, Prometheus routes, and optional OpenTelemetry tracing.",
    evidence: ["ktor/observability/README.md", "ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability"],
  },
  "ktor-testing-sequence-01": {
    intent: "Show the test helper sequence from testApplication through core installation, JSON client creation, request execution, and assertion/mock helpers.",
    evidence: ["ktor/testing/README.md", "ktor/testing/src/main/kotlin/io/bluetape4k/ktor/testing"],
  },
  "examples-ktor-idgenerator-ktor-demo-diagram-01": {
    intent: "Show the demo architecture from HTTP routes through validation and IdGeneratorRegistry to concrete UUID/ULID/KSUID/Snowflake/Flake generators.",
    evidence: ["examples/ktor/idgenerator-ktor-demo/README.md", "examples/ktor/idgenerator-ktor-demo/src/main"],
  },
  "examples-ktor-observability-ktor-demo-architecture-01": {
    intent: "Show the observability demo architecture: Ktor core + observability install, routes, service, EventTelemetry, and app-owned Prometheus/OpenTelemetry backends.",
    evidence: ["examples/ktor/observability-ktor-demo/README.md", "examples/ktor/observability-ktor-demo/src/main"],
  },
  "examples-ktor-observability-ktor-demo-sequence-01": {
    intent: "Show the observability demo request sequence with numbered publish/observe/response steps and a separate metrics scrape path.",
    evidence: ["examples/ktor/observability-ktor-demo/README.md", "examples/ktor/observability-ktor-demo/src/main/kotlin"],
  },
  "cache-hibernate-cache-lettuce-diagram-02": {
    intent: "Explain the Hibernate Lettuce cache activation path from application cache usage through Hibernate second-level cache configuration to Redis-backed Lettuce cache regions.",
    evidence: ["cache/hibernate/cache-lettuce/README.md", "cache/hibernate/cache-lettuce/src/main/kotlin"],
  },
  "data-cassandra-diagram-01": {
    intent: "Explain Cassandra data access responsibilities by separating repository code, CqlSession DSL helpers, statement builders, row/gettable mapping, and Cassandra cluster access.",
    evidence: ["data/cassandra/README.md", "data/cassandra/src/main/kotlin"],
  },
  "data-mongodb-diagram-03": {
    intent: "Explain MongoDB query and update DSL construction by showing Criteria, Query, Field, Update, and MongoOperations helper responsibilities around source-backed operations.",
    evidence: ["data/mongodb/README.md", "data/mongodb/src/main/kotlin"],
  },
  "examples-spring-boot-idgenerator-spring-boot-demo-diagram-01": {
    intent: "Show the Spring Boot IdGenerator demo path from HTTP endpoints through request validation, generator registry selection, concrete generator families, and response/error payloads.",
    evidence: ["examples/spring-boot/idgenerator-spring-boot-demo/README.md", "examples/spring-boot/idgenerator-spring-boot-demo/src/main"],
  },
  "infra-micrometer-diagram-03": {
    intent: "Explain Micrometer instrumentation components by grouping timers, observations, event telemetry, and framework collectors around the registry boundary.",
    evidence: ["infra/micrometer/README.md", "infra/micrometer/src/main/kotlin"],
  },
  "infra-opentelemetry-diagram-02": {
    intent: "Explain distributed trace propagation by showing incoming carrier extraction, context propagation, outbound injection, and exporter/runtime boundaries.",
    evidence: ["infra/opentelemetry/README.md", "infra/opentelemetry/src/main/kotlin"],
  },
  "io-avro-diagram-02": {
    intent: "Show Avro serializer decision routing from schema/record inputs through binary or JSON encoding branches to serialized bytes and decode outcomes.",
    evidence: ["io/avro/README.md", "io/avro/src/main/kotlin"],
  },
  "io-jackson2-diagram-02": {
    intent: "Explain Jackson2 serializer selection and conversion flow by separating ObjectMapper configuration, JSON/Smile/CBOR serializers, and ByteArray/String outputs.",
    evidence: ["io/jackson2/README.md", "io/jackson2/src/main/kotlin"],
  },
  "io-jackson3-diagram-01": {
    intent: "Explain Jackson3 module overview by grouping ObjectMapper setup, serializer implementations, Kotlin module support, and JSON-family byte/string boundaries.",
    evidence: ["io/jackson3/README.md", "io/jackson3/src/main/kotlin"],
  },
  "io-vertx-diagram-02": {
    intent: "Explain Vert.x Future processing flow from Future-producing work through coroutine await, retry/resilience decorators, and success or failure outcomes.",
    evidence: ["io/vertx/README.md", "io/vertx/src/main/kotlin"],
  },
  "testing-junit5-diagram-03": {
    intent: "Explain JUnit5 test utility flow by grouping extension context helpers, captured output, stress testers, and assertion/reporting outcomes.",
    evidence: ["testing/junit5/README.md", "testing/junit5/src/main/kotlin"],
  },
  "utils-idgenerators-diagram-01": {
    intent: "Explain id-generator module families by separating monotonic, time-sortable, random, and Snowflake-style generators with their configuration and output characteristics.",
    evidence: ["utils/idgenerators/README.md", "utils/idgenerators/src/main/kotlin"],
  },
  "utils-measured-diagram-02": {
    intent: "Explain Unit Composition Flow by showing Measure values, unit operators, inverse units, composed units, and conversion results.",
    evidence: ["utils/measured/README.md", "utils/measured/src/main/kotlin"],
  },
  "utils-rule-engine-diagram-01": {
    intent: "Explain the rule-engine module overview by grouping rules, facts, evaluators, execution context, and decision outcomes around the rule evaluation boundary.",
    evidence: ["utils/rule-engine/README.md", "utils/rule-engine/src/main/kotlin"],
  },
  "utils-rule-engine-diagram-06": {
    intent: "Explain rule-engine selection logic as a priority decision path from candidate rules through condition checks to selected action or no-match outcome.",
    evidence: ["utils/rule-engine/README.md", "utils/rule-engine/src/main/kotlin"],
    layout: "decision-flow",
  },
  "utils-rule-engine-diagram-07": {
    intent: "Explain rule-engine dispatch selection by showing ordered matcher evaluation, branch outcomes, and terminal execution or fallback behavior.",
    evidence: ["utils/rule-engine/README.md", "utils/rule-engine/src/main/kotlin"],
    layout: "decision-flow",
  },
  "utils-science-diagram-02": {
    intent: "Explain science utility flow from coordinate inputs through projection, geometry, repository/import helpers, and persisted or transformed outputs.",
    evidence: ["utils/science/README.md", "utils/science/src/main/kotlin"],
  },
  "utils-states-diagram-01": {
    intent: "Explain the states module overview by grouping state definitions, transition guards, state machine execution, and terminal outcomes.",
    evidence: ["utils/states/README.md", "utils/states/src/main/kotlin"],
  },
  "utils-states-diagram-03": {
    intent: "Explain state transition processing from current state and event through guard evaluation, transition action, and next or rejected state.",
    evidence: ["utils/states/README.md", "utils/states/src/main/kotlin"],
  },
  "utils-workflow-diagram-01": {
    intent: "Explain workflow definition architecture by separating workflow definition, tasks, transitions, execution context, policy, and terminal workflow outcomes.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
  },
  "utils-workflow-diagram-02": {
    intent: "Explain workflow execution flow from start state through task execution, transition evaluation, retry policy, and completed or failed result.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
    layout: "decision-flow",
  },
  "utils-workflow-diagram-03": {
    intent: "Explain workflow retry flow by showing failure detection, backoff, retry decision, repeated execution, and terminal failed/completed branches.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
    layout: "decision-flow",
  },
  "utils-workflow-diagram-04": {
    intent: "Explain workflow policy flow by showing condition evaluation, flow policy selection, task transition routing, and terminal state handling.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
    layout: "decision-flow",
  },
  "utils-workflow-diagram-05": {
    intent: "Explain workflow task orchestration by showing task inputs, action execution, output mapping, transition guards, and next-task routing.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
    layout: "decision-flow",
  },
  "utils-workflow-diagram-06": {
    intent: "Explain workflow failure handling by showing exception capture, retry/backoff policy, compensation or fallback branch, and terminal failure reporting.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
  },
  "utils-workflow-diagram-07": {
    intent: "Explain workflow decision routing by showing branch condition checks, selected transition, retry/failure alternatives, and final workflow outcome.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
  },
  "utils-workflow-diagram-08": {
    intent: "Explain workflow completion flow by showing final task result aggregation, transition closure, completion event, and observable outcome.",
    evidence: ["utils/workflow/README.md", "utils/workflow/src/main/kotlin"],
    layout: "decision-flow",
  },
  "virtualthread-api-diagram-02": {
    intent: "Explain virtual-thread runtime selection as a decision flow from requested execution model through platform support checks to virtual-thread or fallback executor outcomes.",
    evidence: ["virtualthread/api/README.md", "virtualthread/api/src/main/kotlin"],
    layout: "decision-flow",
  },
  "virtualthread-diagram-01": {
    intent: "Explain virtual-thread implementation selection by showing the caller request, availability checks, JDK runtime branch, and selected scheduler/executor behavior.",
    evidence: ["virtualthread/README.md", "virtualthread/src/main/kotlin"],
  },
};

function esc(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

function renderPng(svgPath, pngPath) {
  try {
    execFileSync(CAIROSVG, [svgPath, "-o", pngPath, "--scale", "2"], { stdio: "inherit" });
  } catch (error) {
    execFileSync(RSVG, ["--format", "png", "--output", pngPath, svgPath], { stdio: "inherit" });
  }
}

function base(width, height, title, subtitle, body, layout = "") {
  const layoutAttr = layout ? ` data-layout="${esc(layout)}"` : "";
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(title)}"${layoutAttr}>
<defs>
  <filter id="shadow" x="-8%" y="-8%" width="116%" height="116%"><feDropShadow dx="0" dy="6" stdDeviation="5" flood-color="#0F172A" flood-opacity="0.10"/></filter>
  <marker id="arrow" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="context-stroke"/></marker>
  <marker id="seqArrow-blue" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#1D4ED8"/></marker>
  <marker id="seqArrow-green" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#15803D"/></marker>
  <marker id="seqArrow-teal" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#0F766E"/></marker>
  <marker id="seqArrow-amber" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#C2410C"/></marker>
  <marker id="seqArrow-pink" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#BE185D"/></marker>
  <marker id="seqArrow-purple" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#7E22CE"/></marker>
  <marker id="seqArrow-gray" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="strokeWidth"><path d="M 0.5 0.5 L 4.5 2.5 L 0.5 4.5 Z" fill="#4B5563"/></marker>
  <style>
    svg{font-family:"Architects Daughter","Comic Mono","Comic Sans MS",ui-sans-serif,system-ui,sans-serif}
    .canvas{fill:#F8FAFC}.frame{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5;filter:url(#shadow)}
    .title{font-family:"Architects Daughter";font-size:44px;fill:#0F172A}.subtitle{font-family:"Comic Mono";font-size:16px;fill:#475569}
    .panel{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.5}.panelTitle{font-family:"Architects Daughter";font-size:24px;fill:#0F172A;paint-order:stroke;stroke:#fff;stroke-width:4px;stroke-linejoin:round}
    .card{filter:url(#shadow);stroke-width:1.7}.cardTitle{font-family:"Architects Daughter";font-size:23px;fill:#0F172A}.detail{font-family:"Comic Mono";font-size:13px;fill:#475569}
    .labelPill{fill:#FFFFFF;stroke:#CBD5E1;stroke-width:1.4}
    .route{fill:none;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round;marker-end:url(#arrow)}
    .seq,.seqReturn{fill:none;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round}.seqReturn{stroke-dasharray:8 7}
    .dashed{stroke-dasharray:8 7}.lifeline{fill:none;stroke-width:2.4;stroke-linecap:round;stroke-linejoin:round}
  </style>
</defs>
<rect class="canvas" width="${width}" height="${height}"/>
<rect class="frame" x="34" y="30" width="${width - 68}" height="${height - 60}" rx="8"/>
<text class="title" x="72" y="84">${esc(title)}</text>
<text class="subtitle" x="76" y="116">${esc(subtitle)}</text>
${body}
</svg>
`;
}

function card(id, x, y, w, h, title, details, color = "blue") {
  const [fill, stroke] = colors[color] || colors.gray;
  const lines = [title, ...(Array.isArray(details) ? details : [details].filter(Boolean))];
  const lineHeight = 18;
  const titleHeight = 25;
  const total = titleHeight + (lines.length - 1) * lineHeight;
  const start = y + h / 2 - total / 2 + 14;
  return `<g id="${esc(id)}">
  <rect class="card" x="${x}" y="${y}" width="${w}" height="${h}" rx="8" fill="${fill}" stroke="${stroke}"/>
  <text class="cardTitle" x="${x + w / 2}" y="${Math.round(start)}" text-anchor="middle" dominant-baseline="middle">${esc(title)}</text>
${lines.slice(1).map((line, index) => `  <text class="detail" x="${x + w / 2}" y="${Math.round(start + 30 + index * lineHeight)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`).join("\n")}
</g>`;
}

function diamond(id, x, y, w, h, title, details, color = "purple") {
  const [fill, stroke] = colors[color] || colors.gray;
  const cx = x + w / 2;
  const cy = y + h / 2;
  const points = `${cx},${y} ${x + w},${cy} ${cx},${y + h} ${x},${cy}`;
  const lines = [title, ...(Array.isArray(details) ? details : [details].filter(Boolean))];
  const lineHeight = 17;
  const total = 25 + (lines.length - 1) * lineHeight;
  const start = cy - total / 2 + 13;
  return `<g id="${esc(id)}">
  <polygon class="card card-shape" points="${points}" fill="${fill}" stroke="${stroke}"/>
  <text class="cardTitle" x="${cx}" y="${Math.round(start)}" text-anchor="middle" dominant-baseline="middle">${esc(title)}</text>
${lines.slice(1).map((line, index) => `  <text class="detail" x="${cx}" y="${Math.round(start + 29 + index * lineHeight)}" text-anchor="middle" dominant-baseline="middle">${esc(line)}</text>`).join("\n")}
</g>`;
}

function panel(x, y, w, h, title) {
  return `<g><rect class="panel" x="${x}" y="${y}" width="${w}" height="${h}" rx="8"/><text class="panelTitle" x="${x + 28}" y="${y + 36}" dominant-baseline="middle">${esc(title)}</text></g>`;
}

function labelPill(x, y, value, color = "gray", width = null) {
  const stroke = colors[color]?.[2] || colors.gray[2];
  const w = width ?? Math.max(54, value.length * 7 + 24);
  return `<g><rect class="labelPill" x="${x - w / 2}" y="${y - 13}" width="${w}" height="26" rx="8"/><text class="detail" x="${x}" y="${y + 1}" text-anchor="middle" dominant-baseline="middle" style="fill:${stroke}">${esc(value)}</text></g>`;
}

function route(from, to, d, color = "gray", dashed = false, label = null) {
  const stroke = colors[color]?.[2] || colors.gray[2];
  const path = `<path class="route${dashed ? " dashed" : ""}" data-from="${esc(from)}" data-to="${esc(to)}" d="${d}" stroke="${stroke}"/>`;
  return label ? `${path}\n${labelPill(label.x, label.y, label.text, color, label.width)}` : path;
}

function text(x, y, value, klass = "detail", anchor = "middle") {
  return `<text class="${klass}" x="${x}" y="${y}" text-anchor="${anchor}" dominant-baseline="middle">${esc(value)}</text>`;
}

function participant(id, x, y, w, title, color, lifeEnd) {
  const [fill, stroke] = colors[color] || colors.gray;
  return `<g id="${esc(id)}"><rect class="card" x="${x}" y="${y}" width="${w}" height="76" rx="10" fill="${fill}" stroke="${stroke}"/><text class="cardTitle" x="${x + w / 2}" y="${y + 40}" text-anchor="middle" dominant-baseline="middle">${esc(title)}</text><line class="lifeline dashed" x1="${x + w / 2}" y1="${y + 76}" x2="${x + w / 2}" y2="${lifeEnd}" stroke="${stroke}"/></g>`;
}

function seqMessage(id, fromId, toId, fromX, toX, y, label, number, color = "blue", dashed = false) {
  const stroke = colors[color]?.[2] || colors.gray[2];
  const mid = (fromX + toX) / 2;
  const width = Math.max(170, Math.min(360, label.length * 7 + 56));
  const klass = dashed ? "seqReturn" : "seq";
  return `<g id="${esc(id)}"><rect class="labelPill" x="${mid - width / 2}" y="${y - 14}" width="${width}" height="28" rx="8"/><circle cx="${mid - width / 2 + 18}" cy="${y}" r="11" fill="${stroke}"/><text class="detail" x="${mid - width / 2 + 18}" y="${y + 1}" text-anchor="middle" dominant-baseline="middle" style="fill:#fff;font-size:12px">${number}</text><text class="detail" x="${mid + 12}" y="${y + 1}" text-anchor="middle" dominant-baseline="middle">${esc(label)}</text><path class="${klass}" d="M${fromX} ${y + 28} L${toX} ${y + 28}" stroke="${stroke}" marker-end="url(#seqArrow-${color})"/></g>`;
}

function renderLayeredCapabilityMap(name, title, subtitle, rows, edges, options = {}) {
  const left = 82;
  const panelPadX = 64;
  const minNodeGap = options.minNodeGap ?? 42;
  const requiredWidth = Math.max(...rows.map((row) => {
    const totalW = row.nodes.reduce((sum, node) => sum + (node.w ?? row.cardWidth ?? 280), 0);
    return left * 2 + panelPadX * 2 + totalW + minNodeGap * Math.max(0, row.nodes.length - 1);
  }));
  const width = Math.max(options.width ?? 1500, Math.ceil(requiredWidth));
  const top = 160;
  const panelW = width - 164;
  const rowGap = options.rowGap ?? 84;
  const panelTitleBodyTop = 64;
  const panelBodyBottom = 22;
  const positions = new Map();
  const body = [];
  let y = top;

  for (const row of rows) {
    const cardH = row.cardHeight ?? 84;
    const panelH = row.height ?? Math.max(220, cardH + 150);
    body.push(panel(left, y, panelW, panelH, row.title));
    const totalW = row.nodes.reduce((sum, node) => sum + (node.w ?? row.cardWidth ?? 280), 0);
    const gap = row.nodes.length <= 1 ? 0 : Math.max(minNodeGap, (panelW - panelPadX * 2 - totalW) / (row.nodes.length - 1));
    let x = left + panelPadX + Math.max(0, (panelW - panelPadX * 2 - totalW - gap * (row.nodes.length - 1)) / 2);
    for (const node of row.nodes) {
      const w = node.w ?? row.cardWidth ?? 280;
      const h = node.h ?? cardH;
      const nx = Math.round(x);
      const bodyH = Math.max(1, panelH - panelTitleBodyTop - panelBodyBottom);
      const ny = Math.round(y + panelTitleBodyTop + (bodyH - h) / 2);
      positions.set(node.id, { ...node, x: nx, y: ny, w, h, rowY: y, panelH });
      body.push(card(node.id, nx, ny, w, h, node.title, node.details ?? [], node.color ?? "blue"));
      x += w + gap;
    }
    y += panelH + rowGap;
  }

  const portOverrides = buildDistributedPortOverrides(edges, positions);

  for (const [edgeIndex, edge] of edges.entries()) {
    const [from, to, color = "blue", dashed = false, label = null, ports = {}] = edge;
    const autoPorts = portOverrides.get(edgeKey(edgeIndex, from, to)) ?? {};
    body.push(autoRoute(positions, from, to, color, dashed, label, edgeIndex, { ...autoPorts, ...ports }));
  }

  const height = options.height ?? y + 44;
  write(name, base(width, height, title, subtitle, body.join("\n"), options.layout ?? ""), {
    nodes: [...positions.keys()],
    edges: edges.map(([from, to]) => [from, to]),
  });
}

function buildDistributedPortOverrides(edges, positions) {
  const bySource = new Map();
  const byTarget = new Map();
  const overrides = new Map();

  for (const [edgeIndex, edge] of edges.entries()) {
    const [from, to] = edge;
    const source = positions.get(from);
    const target = positions.get(to);
    if (!source || !target) continue;
    const sourceCy = source.y + source.h / 2;
    const targetCy = target.y + target.h / 2;
    if (Math.abs(sourceCy - targetCy) < 12) continue;
    const item = { edgeIndex, from, to, source, target };
    appendMapList(bySource, from, item);
    appendMapList(byTarget, to, item);
  }

  for (const items of bySource.values()) {
    if (items.length < 2) continue;
    const sorted = [...items].sort((a, b) => centerX(a.target) - centerX(b.target) || a.edgeIndex - b.edgeIndex);
    const slots = distributedPortSlots(sorted[0].source, sorted.length);
    sorted.forEach((item, index) => {
      const key = edgeKey(item.edgeIndex, item.from, item.to);
      overrides.set(key, { ...(overrides.get(key) ?? {}), fromX: slots[index] });
    });
  }

  for (const items of byTarget.values()) {
    if (items.length < 2) continue;
    const sorted = [...items].sort((a, b) => centerX(a.source) - centerX(b.source) || a.edgeIndex - b.edgeIndex);
    const slots = distributedPortSlots(sorted[0].target, sorted.length);
    sorted.forEach((item, index) => {
      const key = edgeKey(item.edgeIndex, item.from, item.to);
      overrides.set(key, { ...(overrides.get(key) ?? {}), toX: slots[index] });
    });
  }

  return overrides;
}

function appendMapList(map, key, item) {
  if (!map.has(key)) map.set(key, []);
  map.get(key).push(item);
}

function edgeKey(edgeIndex, from, to) {
  return `${edgeIndex}:${from}->${to}`;
}

function centerX(node) {
  return node.x + node.w / 2;
}

function distributedPortSlots(node, count) {
  const center = centerX(node);
  if (count <= 1) return [center];
  const margin = Math.min(74, Math.max(42, node.w * 0.18));
  const left = node.x + margin;
  const right = node.x + node.w - margin;
  const usable = Math.max(1, right - left);
  return Array.from({ length: count }, (_, index) => Math.round(left + (usable * index) / (count - 1)));
}

function autoRoute(positions, from, to, color = "blue", dashed = false, labelText = null, edgeIndex = 0, ports = {}) {
  const a = positions.get(from);
  const b = positions.get(to);
  if (!a || !b) throw new Error(`Unknown layered map route ${from} -> ${to}`);
  if (ports.path) {
    return route(from, to, ports.path, color, dashed, labelText ? {
      x: ports.labelX ?? (a.x + b.x + a.w + b.w) / 4,
      y: ports.labelY ?? (a.y + b.y + a.h + b.h) / 4,
      text: labelText,
      width: Math.max(70, labelText.length * 7 + 28),
    } : null);
  }
  const acx = a.x + a.w / 2;
  const acy = a.y + a.h / 2;
  const bcx = b.x + b.w / 2;
  const bcy = b.y + b.h / 2;
  const apx = ports.fromX ?? safeVerticalPortX(a, acx);
  const bpx = ports.toX ?? safeVerticalPortX(b, bcx);
  let d;
  let label;

  if (ports.direct) {
    if (Math.abs(acy - bcy) < 12) {
      d = acx < bcx
        ? `M${a.x + a.w} ${acy} L${b.x} ${bcy}`
        : `M${a.x} ${acy} L${b.x + b.w} ${bcy}`;
      label = labelText ? { x: (acx + bcx) / 2, y: acy - 28, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
    } else if (acy < bcy) {
      d = `M${apx} ${a.y + a.h} L${bpx} ${b.y}`;
      label = labelText ? { x: (apx + bpx) / 2, y: (a.y + a.h + b.y) / 2 - 16, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
    } else {
      d = `M${apx} ${a.y} L${bpx} ${b.y + b.h}`;
      label = labelText ? { x: (apx + bpx) / 2, y: (b.y + b.h + a.y) / 2 - 16, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
    }
    return route(from, to, d, color, dashed, label);
  }

  if (Math.abs(acy - bcy) < 12) {
    if (acx < bcx) {
      d = `M${a.x + a.w} ${acy} L${b.x} ${bcy}`;
    } else {
      d = `M${a.x} ${acy} L${b.x + b.w} ${bcy}`;
    }
    label = labelText ? { x: (acx + bcx) / 2, y: acy - 28, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
  } else if (acy < bcy) {
    if (Math.abs(apx - bpx) <= 18) {
      const commonX = Math.round((apx + bpx) / 2);
      d = `M${commonX} ${a.y + a.h} L${commonX} ${b.y}`;
      label = labelText ? { x: commonX + 42, y: (a.y + a.h + b.y) / 2, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
    } else {
      const laneY = Math.round(a.y + a.h + (b.y - (a.y + a.h)) / 2 + ((edgeIndex % 5) - 2) * 11);
      d = `M${apx} ${a.y + a.h} L${apx} ${laneY} L${bpx} ${laneY} L${bpx} ${b.y}`;
      label = labelText ? { x: (apx + bpx) / 2, y: laneY - 18, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
    }
  } else {
    const laneY = Math.round(b.y + b.h + (a.y - (b.y + b.h)) / 2 + ((edgeIndex % 5) - 2) * 11);
    d = `M${apx} ${a.y} L${apx} ${laneY} L${bpx} ${laneY} L${bpx} ${b.y + b.h}`;
    label = labelText ? { x: (apx + bpx) / 2, y: laneY - 18, text: labelText, width: Math.max(70, labelText.length * 7 + 28) } : null;
  }
  return route(from, to, d, color, dashed, label);
}

function safeVerticalPortX(node, preferred) {
  if (node.x < 360) return Math.min(node.x + node.w - 64, Math.max(node.x + 64, preferred + 72));
  return preferred;
}

function renderRootOverview() {
  renderLayeredCapabilityMap("root-readme-overview-01", "Bluetape4k Projects Overview", "The repository is organized as foundation libraries, runtime integrations, app frameworks, testing support, and examples.", [
    { title: "Foundation", nodes: [
      { id: "bom", title: "BOM / Gradle catalog", details: ["version alignment"], color: "gray", w: 260 },
      { id: "core", title: "core + logging", details: ["contracts, values, diagnostics"], color: "blue", w: 300 },
      { id: "coroutines", title: "coroutines", details: ["Flow, Deferred, scopes"], color: "green", w: 270 },
      { id: "utils", title: "utils", details: ["time, geo, money, states"], color: "amber", w: 290 },
    ] },
    { title: "Library families", nodes: [
      { id: "io", title: "I/O codecs and clients", details: ["HTTP, Feign, gRPC, JSON", "Netty, Okio, Protobuf"], color: "teal", w: 330 },
      { id: "data", title: "Data access", details: ["JDBC, R2DBC, MongoDB", "Cassandra, Hibernate"], color: "purple", w: 330 },
      { id: "cache", title: "Cache modules", details: ["JCache, NearCache", "Hazelcast, Lettuce, Redisson"], color: "pink", w: 340 },
      { id: "infra", title: "Infrastructure", details: ["Kafka, Redis, OTel", "Micrometer, Resilience4j"], color: "olive", w: 340 },
    ] },
    { title: "Application surfaces", nodes: [
      { id: "spring", title: "Spring Boot modules", details: ["auto-config and starters"], color: "green", w: 330 },
      { id: "ktor", title: "Ktor modules", details: ["core, observability, testing"], color: "blue", w: 310 },
      { id: "vt", title: "Virtual thread runtime", details: ["api, jdk21, jdk25"], color: "purple", w: 310 },
      { id: "testing", title: "Testing support", details: ["assertions, junit5", "mock servers, containers"], color: "amber", w: 340 },
    ] },
    { title: "Reader entrypoints", nodes: [
      { id: "examples", title: "Examples", details: ["end-to-end usage demos"], color: "teal", w: 330 },
      { id: "split", title: "Sibling ecosystem repos", details: ["exposed, r2dbc, workshop", "go, rs, image, javers"], color: "pink", w: 390 },
    ] },
  ], [
    ["bom", "io", "gray", true, null, { toX: 365 }],
    ["core", "io", "blue", false, null, { toX: 430 }],
    ["coroutines", "data", "green"],
    ["coroutines", "infra", "green"],
    ["utils", "cache", "amber"],
    ["io", "spring", "teal", false, null, { toX: 365 }],
    ["data", "spring", "purple", false, null, { toX: 415 }],
    ["infra", "ktor", "olive"],
    ["cache", "spring", "pink", false, null, { toX: 455 }],
    ["spring", "examples", "green", false, null, { toX: 365 }],
    ["ktor", "examples", "blue", false, null, { toX: 415 }],
    ["testing", "examples", "amber", false, null, { toX: 455 }],
    ["vt", "spring", "purple", true, null, { path: "M1058 892 L1058 866 L311 866 L311 892" }],
  ], { width: 1580 });
}

function renderRootModuleStructure() {
  renderLayeredCapabilityMap("root-readme-en-diagram-01", "Repository Module Structure", "Module families are arranged by dependency direction and reader navigation path.", [
    { title: "Shared base", nodes: [
      { id: "core", title: "bluetape4k/*", details: ["core, coroutines, logging, bom"], color: "blue", w: 390 },
      { id: "utils", title: "utils/*", details: ["domain utilities and workflows"], color: "amber", w: 360 },
    ] },
    { title: "Published library modules", nodes: [
      { id: "io", title: "io/*", details: ["serialization, codecs, HTTP clients"], color: "teal", w: 330 },
      { id: "data", title: "data/*", details: ["database adapters"], color: "purple", w: 300 },
      { id: "infra", title: "infra/*", details: ["messaging, metrics, tracing"], color: "olive", w: 330 },
      { id: "cache", title: "cache/*", details: ["cache APIs and backends"], color: "pink", w: 300 },
    ] },
    { title: "Framework and runtime modules", nodes: [
      { id: "spring", title: "spring-boot/*", details: ["Spring Boot integrations"], color: "green", w: 330 },
      { id: "ktor", title: "ktor/*", details: ["Ktor integrations"], color: "blue", w: 290 },
      { id: "vt", title: "virtualthread/*", details: ["runtime selection and APIs"], color: "purple", w: 330 },
      { id: "testing", title: "testing/*", details: ["test helpers and fixtures"], color: "amber", w: 310 },
    ] },
    { title: "Non-published demos", nodes: [
      { id: "examples", title: "examples/*", details: ["application demos using published modules"], color: "teal", w: 430 },
    ] },
  ], [
    ["core", "io", "blue", false, null, { direct: true }],
    ["core", "data", "blue", false, null, { direct: true }],
    ["core", "infra", "blue", false, null, { direct: true }],
    ["core", "cache", "blue", false, null, { direct: true }],
    ["utils", "io", "amber", true], ["io", "spring", "teal"], ["data", "spring", "purple"], ["infra", "ktor", "olive"],
    ["cache", "spring", "pink"], ["testing", "examples", "amber"], ["spring", "examples", "green"], ["ktor", "examples", "blue"],
  ], { width: 1580 });
}

function renderFoundationOverviews() {
  renderLayeredCapabilityMap("bluetape4k-core-diagram-01", "Bluetape4k Core architecture", "Core provides shared contracts and value/runtime helpers used by higher-level modules.", [
    { title: "Caller contracts", nodes: [
      { id: "require", title: "RequireSupport", details: ["typed preconditions"], color: "green" },
      { id: "types", title: "Type extensions", details: ["strings, numbers, reflection"], color: "teal" },
      { id: "codec", title: "Codec utilities", details: ["Base58, Base62, Hex"], color: "amber" },
    ] },
    { title: "Domain value model", nodes: [
      { id: "range", title: "Range model", details: ["open/closed ranges"], color: "pink" },
      { id: "collections", title: "Collection helpers", details: ["ring buffer, bounded stack"], color: "olive" },
      { id: "value", title: "ValueObject", details: ["stable equality contract"], color: "purple" },
    ] },
    { title: "Runtime helpers", nodes: [
      { id: "time", title: "Java time DSL", details: ["duration and temporal ranges"], color: "amber" },
      { id: "concurrent", title: "Concurrent helpers", details: ["Future and VT adapters"], color: "teal" },
      { id: "functional", title: "Functional helpers", details: ["currying, decorators"], color: "green" },
    ] },
  ], [
    ["require", "range", "green"], ["types", "collections", "teal"], ["codec", "value", "amber", true],
    ["range", "time", "pink"], ["collections", "concurrent", "olive"], ["value", "functional", "purple"],
  ]);

  renderLayeredCapabilityMap("bluetape4k-coroutines-diagram-01", "Bluetape4k Coroutines architecture", "Coroutine helpers are grouped by async primitives, Flow processing, runtime scope, and integration bridges.", [
    { title: "Async primitives", nodes: [
      { id: "deferredValue", title: "DeferredValue", details: ["map, flatMap, await"], color: "green", w: 300 },
      { id: "deferredOps", title: "Deferred helpers", details: ["zip, awaitAny, cancellation"], color: "teal", w: 320 },
    ] },
    { title: "Flow processing", nodes: [
      { id: "flowExt", title: "Flow extensions", details: ["chunked, windowed, mapParallel"], color: "amber", w: 330 },
      { id: "asyncFlow", title: "AsyncFlow", details: ["bounded parallel transform"], color: "pink", w: 300 },
      { id: "subjects", title: "Subject APIs", details: ["publish, replay, multicast"], color: "purple", w: 310 },
    ] },
    { title: "Runtime integration", nodes: [
      { id: "scopes", title: "Coroutine scopes", details: ["Default, IO, ThreadPool, VT"], color: "teal", w: 330 },
      { id: "reactor", title: "Reactor context bridge", details: ["ReactiveContext support"], color: "purple", w: 330 },
      { id: "tests", title: "Test support", details: ["Flow assertions, runTest helpers"], color: "olive", w: 330 },
    ] },
  ], [
    ["deferredValue", "flowExt", "green"], ["deferredOps", "subjects", "teal"],
    ["asyncFlow", "reactor", "pink"], ["subjects", "tests", "purple"],
  ]);

  renderLayeredCapabilityMap("bluetape4k-coroutines-diagram-03", "Flow extension taxonomy", "Flow helpers are grouped by the reader's operation intent instead of listed as an unconnected catalog.", [
    { title: "Receiver", nodes: [{ id: "flow", title: "Flow<T>", details: ["cold stream receiver"], color: "blue", w: 320 }] },
    { title: "Operation families", nodes: [
      { id: "batch", title: "Batch and window", details: ["chunked, windowed, sliding"], color: "green", w: 300 },
      { id: "parallel", title: "Parallel transform", details: ["mapParallel, flatMap"], color: "amber", w: 300 },
      { id: "replay", title: "Replay / subject", details: ["publish, replay, share"], color: "purple", w: 300 },
    ] },
    { title: "Terminal uses", nodes: [
      { id: "collect", title: "Collectors", details: ["toList, assertions"], color: "teal", w: 300 },
      { id: "testing", title: "Testing helpers", details: ["runTest and Flow assertions"], color: "pink", w: 330 },
    ] },
  ], [["flow", "parallel", "amber"], ["batch", "collect", "teal"], ["replay", "testing", "purple"]]);
}

function renderDataApiOverviews() {
  renderLayeredCapabilityMap("data-jdbc-diagram-01", "JDBC Extension API Overview", "The module helps callers move from SQL and JDBC resources to typed rows, batch execution, and domain mapping.", [
    { title: "Caller boundary", nodes: [
      { id: "sql", title: "SQL + parameters", details: ["query/update intent"], color: "blue", w: 300 },
      { id: "ds", title: "DataSource / Connection", details: ["resource receiver"], color: "green", w: 330 },
    ] },
    { title: "JDBC helper layer", nodes: [
      { id: "tx", title: "Transaction helpers", details: ["commit/rollback boundary"], color: "purple", w: 310 },
      { id: "stmt", title: "Statement helpers", details: ["prepare, bind, execute"], color: "teal", w: 300 },
      { id: "batch", title: "Batch helpers", details: ["batch update path"], color: "amber", w: 290 },
    ] },
    { title: "Result boundary", nodes: [
      { id: "rs", title: "ResultSet typed access", details: ["getAs, nullable reads"], color: "pink", w: 330 },
      { id: "domain", title: "Domain mapper", details: ["row -> model"], color: "olive", w: 290 },
      { id: "result", title: "Update counts", details: ["affected rows"], color: "gray", w: 270 },
    ] },
  ], [["sql", "stmt", "blue", false, null, { toX: 700 }], ["ds", "stmt", "green", false, null, { toX: 820 }], ["stmt", "rs", "teal"], ["stmt", "batch", "amber"], ["batch", "result", "amber"], ["tx", "stmt", "purple", true], ["rs", "domain", "pink"]]);

  renderR2dbcExtensionApiOverview();
}

function renderR2dbcExtensionApiOverview() {
  const body = [
    panel(82, 160, 1350, 180, "Caller boundary"),
    panel(82, 425, 1350, 230, "Extension families"),
    panel(82, 752, 1350, 220, "Reactive/coroutine result"),
    card("service", 140, 235, 330, 84, "Repository/service code", ["suspend and Flow callers"], "blue"),
    card("client", 585, 235, 320, 84, "DatabaseClient", ["Spring reactive SQL facade"], "purple"),
    card("factory", 1035, 235, 320, 84, "ConnectionFactory", ["R2DBC connection source"], "green"),
    card("bind", 170, 500, 300, 86, "Bind helpers", ["named/indexed params"], "teal"),
    card("query", 600, 500, 320, 86, "Query helpers", ["map rows, fetch one/all"], "amber"),
    card("tx", 1030, 500, 330, 86, "Transaction helpers", ["operator and connection scope"], "pink"),
    card("rows", 170, 836, 300, 86, "Rows updated", ["count and metadata"], "olive"),
    card("publisher", 585, 836, 300, 86, "Publisher results", ["Mono / Flux"], "purple"),
    card("coroutines", 1025, 836, 340, 86, "Coroutine results", ["Flow<T>, suspend T?"], "green"),
    route("service", "client", "M470 277 L585 277", "blue"),
    route("client", "bind", "M705 319 L705 390 L380 390 L380 500", "purple"),
    route("client", "query", "M745 319 L745 500", "amber"),
    route("factory", "tx", "M1195 319 L1195 500", "green"),
    route("bind", "query", "M470 543 L600 543", "teal"),
    route("query", "rows", "M680 586 L680 710 L450 710 L450 836", "amber"),
    route("query", "publisher", "M760 586 L760 836", "amber"),
    route("tx", "coroutines", "M1195 586 L1195 836", "pink"),
    route("publisher", "coroutines", "M885 879 L1025 879", "purple"),
  ].join("\n");
  write("data-r2dbc-diagram-01", base(1500, 1040, "R2DBC Extension API Overview", "The module adapts reactive database receivers into coroutine-friendly query, transaction, and result boundaries.", body), {
    nodes: ["service", "client", "factory", "bind", "query", "tx", "publisher", "coroutines", "rows"],
    edges: [["service", "client"], ["client", "bind"], ["client", "query"], ["factory", "tx"], ["bind", "query"], ["query", "publisher"], ["query", "rows"], ["tx", "coroutines"], ["publisher", "coroutines"]],
  });
}

function renderIoAndTestingOverviews() {
  renderLayeredCapabilityMap("io-feign-diagram-01", "Feign + Coroutines Integration Architecture", "Feign support is organized around builder APIs, coroutine-friendly services, codec/request helpers, and Vert.x transport adapters.", [
    { title: "Public API", nodes: [
      { id: "api", title: "User API interface", details: ["Feign target contract"], color: "blue", w: 300 },
      { id: "builder", title: "FeignBuilderSupport", details: ["base builder defaults"], color: "green", w: 330 },
      { id: "coroutineBuilder", title: "FeignCoroutineBuilderSupport", details: ["suspend service creation"], color: "teal", w: 380 },
    ] },
    { title: "Request and codec support", nodes: [
      { id: "request", title: "FeignRequestSupport", details: ["headers, target, request options"], color: "purple", w: 340 },
      { id: "codec", title: "Encoder / Decoder", details: ["JSON and body conversion"], color: "amber", w: 320 },
      { id: "result", title: "Coroutine result path", details: ["suspend call adaptation"], color: "pink", w: 320 },
    ] },
    { title: "Transport adapters", nodes: [
      { id: "vertx", title: "VertxHttpClient", details: ["blocking Feign client bridge"], color: "olive", w: 330 },
      { id: "remote", title: "Remote HTTP API", details: ["request/response runtime"], color: "blue", w: 310 },
      { id: "asyncVertx", title: "AsyncVertxHttpClient", details: ["async transport bridge"], color: "green", w: 340 },
    ] },
  ], [["api", "builder", "blue"], ["builder", "coroutineBuilder", "green"], ["builder", "request", "purple"], ["coroutineBuilder", "result", "teal"], ["request", "codec", "purple"], ["codec", "vertx", "amber"], ["codec", "asyncVertx", "amber"], ["vertx", "remote", "olive"], ["asyncVertx", "remote", "green"]]);

  renderLayeredCapabilityMap("io-grpc-diagram-02", "gRPC Component Overview", "Core server/client abstractions stay separate from managed channels, interceptors, and in-process test variants.", [
    { title: "Application boundary", nodes: [
      { id: "serverApp", title: "Service implementation", details: ["binds generated gRPC service"], color: "blue", w: 330 },
      { id: "clientApp", title: "Client caller", details: ["uses typed stub/client"], color: "green", w: 290 },
    ] },
    { title: "bluetape4k gRPC support", nodes: [
      { id: "server", title: "GrpcServer / AbstractGrpcServer", details: ["lifecycle and port management"], color: "teal", w: 390 },
      { id: "interceptor", title: "Interceptor support", details: ["metadata and call hooks"], color: "pink", w: 320 },
      { id: "client", title: "AbstractGrpcClient", details: ["stub/channel owner"], color: "amber", w: 320 },
      { id: "channel", title: "ManagedChannelSupport", details: ["channel creation/shutdown"], color: "purple", w: 360 },
    ] },
    { title: "Runtime and tests", nodes: [
      { id: "runtime", title: "gRPC transport runtime", details: ["server channel + client calls"], color: "blue", w: 360 },
      { id: "inprocess", title: "In-process variants", details: ["fast server/client tests"], color: "olive", w: 330 },
    ] },
  ], [["serverApp", "server", "blue"], ["clientApp", "client", "green"], ["client", "channel", "amber"], ["server", "runtime", "teal"], ["channel", "runtime", "purple"], ["interceptor", "server", "pink", true], ["interceptor", "client", "pink", true], ["server", "inprocess", "olive", false, null, { direct: true }], ["client", "inprocess", "olive", false, null, { direct: true }]]);

  renderLayeredCapabilityMap("io-http-diagram-01", "Multi-Backend HTTP Client Architecture", "The HTTP module exposes common request support while keeping backend client stacks and cross-cutting concerns explicit.", [
    { title: "Caller-facing API", nodes: [
      { id: "caller", title: "Application code", details: ["HTTP call intent"], color: "blue", w: 290 },
      { id: "support", title: "HTTP support facade", details: ["request, entity, protocol helpers"], color: "green", w: 360 },
      { id: "cross", title: "Cross-cutting helpers", details: ["auth, SSL, cache, routing"], color: "purple", w: 340 },
    ] },
    { title: "Backend client stacks", nodes: [
      { id: "hc5", title: "Apache HC5", details: ["classic and async"], color: "teal", w: 250 },
      { id: "okhttp", title: "OkHttp3", details: ["interceptors, client support"], color: "amber", w: 250 },
      { id: "vertx", title: "Vert.x WebClient", details: ["event-loop client"], color: "pink", w: 290 },
      { id: "ktor", title: "Ktor / JDK", details: ["Ktor client and JDK helpers"], color: "olive", w: 300 },
    ] },
    { title: "Runtime boundary", nodes: [
      { id: "wire", title: "HTTP wire request", details: ["headers, body, status"], color: "blue", w: 330 },
      { id: "remote", title: "Remote service", details: ["REST endpoint"], color: "green", w: 300 },
    ] },
  ], [["caller", "support", "blue"], ["support", "cross", "green"], ["support", "hc5", "teal"], ["support", "okhttp", "amber"], ["support", "vertx", "pink"], ["support", "ktor", "olive"], ["hc5", "wire", "teal"], ["okhttp", "wire", "amber"], ["vertx", "wire", "pink"], ["ktor", "wire", "olive"], ["wire", "remote", "blue"]]);

  renderLayeredCapabilityMap("testing-assertions-diagram-01", "Assertions Capability Architecture", "Public assertion DSLs stay reader-facing while coroutine assertions and the internal failure engine explain how errors are produced.", [
    { title: "Test code", nodes: [{ id: "test", title: "JUnit / Kotest test", details: ["calls assertion extensions"], color: "blue", w: 330 }] },
    { title: "Public assertion DSLs", nodes: [
      { id: "basic", title: "Basic assertions", details: ["strings, numbers, collections"], color: "green", w: 330 },
      { id: "domain", title: "Domain assertions", details: ["date/time, files, reflection"], color: "amber", w: 330 },
      { id: "coroutines", title: "Coroutine assertions", details: ["Flow/Turbine and suspend checks"], color: "purple", w: 350 },
    ] },
    { title: "Failure engine", nodes: [
      { id: "messages", title: "Messages", details: ["human-readable failure text"], color: "teal", w: 330 },
      { id: "failures", title: "Failures", details: ["throws AssertionError"], color: "pink", w: 310 },
    ] },
  ], [
    ["test", "basic", "blue"],
    ["test", "domain", "blue"],
    ["test", "coroutines", "blue"],
    ["basic", "messages", "green", false, null, { path: "M311 658 L311 892" }],
    ["domain", "messages", "amber", false, null, { path: "M740 658 L740 790 L383 790 L383 892" }],
    ["coroutines", "messages", "purple", false, null, { path: "M1179 658 L1179 762 L438 762 L438 892" }],
    ["messages", "failures", "teal"],
  ]);

  renderLayeredCapabilityMap("testing-mock-web-server-diagram-01", "Servlet Mock Web Server Routing Overview", "Requests enter Testcontainers-served HTTP/HTTPS ports, then route through servlet controllers, services, repositories, and fixture data.", [
    { title: "Container entrypoints", nodes: [
      { id: "tests", title: "Integration tests", details: ["Mock web server container"], color: "blue", w: 330 },
      { id: "ports", title: "Port configuration", details: ["HTTP 80, HTTPS 8443"], color: "purple", w: 330 },
    ] },
    { title: "Servlet application", nodes: [
      { id: "tomcat", title: "Tomcat servlet server", details: ["HTTP + HTTPS connectors"], color: "green", w: 330 },
      { id: "controllers", title: "Controllers", details: ["/admin, /httpbin", "/jsonplaceholder, /web"], color: "teal", w: 360 },
      { id: "service", title: "JsonplaceholderService", details: ["request semantics"], color: "amber", w: 330 },
    ] },
    { title: "State and fixtures", nodes: [
      { id: "repo", title: "InMemoryRepository", details: ["mutable test state"], color: "pink", w: 330 },
      { id: "fixtures", title: "Fixture resources", details: ["JSON/static responses"], color: "olive", w: 330 },
    ] },
  ], [["tests", "ports", "blue"], ["ports", "tomcat", "purple", false, null, { direct: true }], ["tomcat", "controllers", "green"], ["controllers", "service", "teal"], ["service", "repo", "amber", false, null, { direct: true }], ["service", "fixtures", "olive", false, null, { direct: true }]]);

  renderLayeredCapabilityMap("testing-mock-webflux-server-diagram-01", "WebFlux Mock Server Routing Overview", "The WebFlux server uses explicit lifecycle setup for HTTPS and Reactor Netty before routing requests to services and fixtures.", [
    { title: "Container entrypoints", nodes: [
      { id: "tests", title: "Integration tests", details: ["Mock WebFlux container"], color: "blue", w: 330 },
      { id: "ports", title: "Port configuration", details: ["HTTP 80, HTTPS 8443"], color: "purple", w: 330 },
    ] },
    { title: "Reactive runtime", nodes: [
      { id: "lifecycle", title: "HttpsServerLifecycle", details: ["SmartLifecycle-managed HTTPS"], color: "green", w: 360 },
      { id: "netty", title: "Reactor Netty server", details: ["non-blocking runtime"], color: "teal", w: 330 },
      { id: "controllers", title: "WebFlux controllers", details: ["suspend/reactive handlers"], color: "amber", w: 340 },
    ] },
    { title: "State and fixtures", nodes: [
      { id: "service", title: "JsonplaceholderService", details: ["reactive request semantics"], color: "pink", w: 340 },
      { id: "repo", title: "InMemoryRepository", details: ["test state + fixtures"], color: "olive", w: 330 },
    ] },
  ], [["tests", "ports", "blue"], ["ports", "lifecycle", "purple"], ["lifecycle", "netty", "green"], ["netty", "controllers", "teal"], ["controllers", "service", "amber"], ["service", "repo", "pink"]]);
}

function renderUtilityFlowOverviews() {
  renderLayeredCapabilityMap("utils-javatimes-diagram-01", "Java Time Feature Overview", "The module groups date/time DSLs by receiver type, interval abstractions, calendar ranges, and arithmetic/flow operations.", [
    { title: "Receivers", nodes: [
      { id: "temporal", title: "java.time receivers", details: ["Instant, LocalDate, ZonedDateTime"], color: "blue", w: 370 },
      { id: "period", title: "Period / Duration", details: ["typed amount helpers"], color: "green", w: 320 },
    ] },
    { title: "Utility families", nodes: [
      { id: "range", title: "Temporal ranges", details: ["date range and overlap"], color: "teal", w: 320 },
      { id: "calendar", title: "Calendar periods", details: ["quarter, month, week windows"], color: "amber", w: 340 },
      { id: "arithmetic", title: "Date arithmetic", details: ["add/diff/truncate helpers"], color: "purple", w: 330 },
    ] },
    { title: "Outputs", nodes: [
      { id: "flow", title: "Coroutine range Flow", details: ["iterate time windows"], color: "pink", w: 340 },
      { id: "formatted", title: "Formatted values", details: ["ISO and localized text"], color: "olive", w: 310 },
    ] },
  ], [
    ["temporal", "range", "blue"],
    ["period", "calendar", "green"],
    ["temporal", "arithmetic", "purple"],
    ["range", "flow", "teal", false, null, { path: "M306 658 L306 892" }],
    ["calendar", "flow", "amber", false, null, { path: "M745 658 L745 790 L386 790 L386 892" }],
    ["arithmetic", "formatted", "purple"],
  ]);

  renderLayeredCapabilityMap("utils-jwt-diagram-01", "JWT Create and Verify Flow", "The JWT helpers keep claim/header input, key/algorithm choice, signing, parser verification, and decoded outcome explicit.", [
    { title: "Creation", nodes: [
      { id: "claims", title: "Claims + headers", details: ["subject, issuer, expiration"], color: "blue" },
      { id: "sign", title: "JWT signer", details: ["build compact token"], color: "green" },
      { id: "alg", title: "Algorithm / key", details: ["HMAC or RSA/ECDSA key"], color: "purple" },
    ] },
    { title: "Transport", nodes: [{ id: "token", title: "Compact JWT token", details: ["header.payload.signature"], color: "amber", w: 360 }] },
    { title: "Verification", nodes: [
      { id: "decoded", title: "Decoded claims", details: ["trusted application input"], color: "green" },
      { id: "parser", title: "JWT parser", details: ["verify signature and claims"], color: "teal" },
      { id: "error", title: "Validation error", details: ["expired / invalid token"], color: "pink" },
    ] },
  ], [["claims", "sign", "blue", false, null, { direct: true }], ["alg", "sign", "purple", false, null, { direct: true }], ["sign", "token", "green"], ["token", "parser", "amber"], ["parser", "decoded", "teal", false, null, { direct: true }], ["parser", "error", "pink", true, null, { direct: true }]], { layout: "decision-flow" });

  renderLayeredCapabilityMap("utils-money-diagram-02", "Currency Operation Flow", "Money utilities convert caller amounts through currency/exchange/rounding support before returning monetary values or formatted text.", [
    { title: "Inputs", nodes: [
      { id: "amount", title: "MonetaryAmount", details: ["number + currency"], color: "blue" },
      { id: "currency", title: "CurrencyUnit", details: ["ISO code and scale"], color: "green" },
    ] },
    { title: "Operations", nodes: [
      { id: "math", title: "Arithmetic helpers", details: ["plus/minus/times/divide"], color: "teal" },
      { id: "convert", title: "Exchange conversion", details: ["rate provider boundary"], color: "amber" },
      { id: "round", title: "Rounding helpers", details: ["scale and monetary rounding"], color: "purple" },
    ] },
    { title: "Outputs", nodes: [
      { id: "money", title: "Monetary result", details: ["converted or rounded amount"], color: "pink" },
      { id: "format", title: "Formatted text", details: ["localized display"], color: "olive" },
    ] },
  ], [
    ["amount", "math", "blue"],
    ["currency", "convert", "green"],
    ["math", "money", "teal", false, null, { path: "M286 658 L286 892" }],
    ["convert", "money", "amber", false, null, { path: "M750 658 L750 786 L336 786 L336 892" }],
    ["round", "money", "purple", false, null, { path: "M1214 658 L1214 798 L386 798 L386 892" }],
    ["money", "format", "pink"],
  ]);

  renderLayeredCapabilityMap("utils-mutiny-diagram-02", "Mutiny Processing Flow", "Mutiny helpers show how Uni/Multi receivers cross into coroutine await, Flow conversion, and resilient terminal outcomes.", [
    { title: "Mutiny receivers", nodes: [
      { id: "uni", title: "Uni<T>", details: ["single async value"], color: "blue" },
      { id: "multi", title: "Multi<T>", details: ["streaming async values"], color: "green" },
    ] },
    { title: "Extension operations", nodes: [
      { id: "await", title: "await helpers", details: ["suspend result"], color: "teal" },
      { id: "flow", title: "Flow bridge", details: ["Multi -> Flow<T>"], color: "amber" },
      { id: "retry", title: "retry / transform", details: ["failure handling"], color: "purple" },
    ] },
    { title: "Coroutine outcomes", nodes: [
      { id: "suspend", title: "suspend T?", details: ["nullable or throwing result"], color: "pink" },
      { id: "flowOut", title: "Flow<T>", details: ["cold coroutine stream"], color: "olive" },
    ] },
  ], [["uni", "await", "blue"], ["multi", "flow", "green"], ["uni", "retry", "purple", true], ["multi", "retry", "purple", true], ["await", "suspend", "teal"], ["flow", "flowOut", "amber", false, null, { direct: true }], ["retry", "suspend", "purple", false, null, { direct: true }]]);
}

function renderWorkflowDiagrams() {
  renderWorkflowConceptOverview();
  renderWorkflowReportStates();
  renderWorkflowExecutionModel();
  renderSequentialFlow();
  renderParallelFlow();
  renderConditionalFlow();
  renderRepeatFlow();
  renderRetryFlow();
}

function renderWorkflowConceptOverview() {
  const body = [
    card("dsl", 120, 250, 360, 96, "Workflow DSL", ["workflow, sequential, parallel", "conditional, repeat, retry"], "blue"),
    card("work", 585, 195, 360, 96, "Work / SuspendWork", ["named units execute with context"], "green"),
    card("context", 585, 410, 360, 96, "WorkContext", ["shared mutable run state"], "amber"),
    card("flows", 1050, 250, 360, 96, "Flow implementations", ["sync, suspend, virtual thread"], "purple"),
    card("report", 585, 670, 360, 116, "WorkReport outcomes", ["Success, Failure, PartialSuccess", "Aborted and Cancelled stop immediately"], "teal"),
    route("dsl", "work", "M480 298 L585 243", "blue"),
    route("dsl", "context", "M480 298 L585 458", "amber", true),
    route("work", "flows", "M945 243 L1050 298", "green"),
    route("context", "flows", "M945 458 L1050 298", "amber"),
    route("flows", "report", "M1230 346 L1230 728 L945 728", "purple"),
    route("work", "report", "M765 291 L765 360 L520 360 L520 728 L585 728", "teal", true),
  ].join("\n");
  write("utils-workflow-diagram-01", base(1530, 910, "Workflow Concept Overview", "Work units execute with shared context, flow implementations choose orchestration semantics, and WorkReport controls the next step.", body), {
    nodes: ["dsl", "work", "context", "flows", "report"],
    edges: [["dsl", "work"], ["dsl", "context"], ["work", "flows"], ["context", "flows"], ["flows", "report"], ["work", "report"]],
  });
}

function renderWorkflowReportStates() {
  const body = [
    card("execute", 590, 170, 340, 78, "Work executes", ["returns one WorkReport"], "blue"),
    card("aborted", 120, 345, 300, 78, "Aborted", ["break flow immediately"], "amber"),
    card("failure", 590, 345, 340, 88, "Failure", ["strategy decides stop or continue"], "pink"),
    card("success", 1080, 345, 310, 78, "Success", ["continue to next work"], "green"),
    diamond("strategy", 585, 560, 350, 132, "ErrorStrategy?", ["STOP vs CONTINUE"], "teal"),
    card("cancelled", 120, 655, 300, 78, "Cancelled", ["timeout or external cancellation"], "purple"),
    card("terminal", 585, 790, 350, 78, "Return Failure", ["STOP exits immediately"], "pink"),
    card("partial", 1030, 780, 380, 92, "PartialSuccess", ["CONTINUE accumulates failures", "then returns after final work"], "olive"),
    route("execute", "aborted", "M590 209 L420 384", "amber", false, { x: 498, y: 286, text: "abort", width: 66 }),
    route("execute", "failure", "M760 248 L760 345", "pink", false, { x: 816, y: 298, text: "failure", width: 78 }),
    route("execute", "success", "M930 209 L1080 384", "green", false, { x: 1005, y: 286, text: "success", width: 82 }),
    route("execute", "cancelled", "M590 209 L80 209 L80 694 L120 694", "purple", true, { x: 160, y: 178, text: "cancel", width: 76 }),
    route("failure", "strategy", "M760 433 L760 560", "teal"),
    route("strategy", "terminal", "M760 692 L760 790", "pink", false, { x: 816, y: 742, text: "yes STOP", width: 88 }),
    route("strategy", "partial", "M935 626 L1030 826", "olive", false, { x: 1120, y: 704, text: "no CONTINUE", width: 126 }),
  ].join("\n");
  write("utils-workflow-diagram-02", base(1490, 950, "WorkReport State Flow", "Failure is the only state controlled by ErrorStrategy; Aborted and Cancelled bypass strategy and return immediately.", body, "decision-flow"), {
    nodes: ["execute", "success", "failure", "aborted", "cancelled", "strategy", "partial", "terminal"],
    edges: [["execute", "success"], ["execute", "failure"], ["execute", "aborted"], ["execute", "cancelled"], ["failure", "strategy"], ["strategy", "terminal"], ["strategy", "partial"]],
  });
}

function renderWorkflowExecutionModel() {
  const body = [
    card("caller", 120, 210, 320, 80, "Caller DSL", ["builds workflow definition"], "blue"),
    diamond("model", 570, 185, 350, 130, "Use sync model?", ["no branch uses suspend"], "purple"),
    card("sync", 1050, 160, 330, 82, "Sync WorkFlow", ["virtual-thread friendly work"], "green"),
    card("suspend", 1050, 295, 330, 82, "SuspendWorkFlow", ["coroutine cancellation aware"], "teal"),
    card("families", 250, 500, 410, 116, "Flow families", ["sequential, parallel, conditional", "repeat, retry"], "amber"),
    card("runtime", 860, 480, 420, 126, "Runtime primitives", ["StructuredTaskScope for sync parallel", "coroutineScope + delay for suspend"], "pink"),
    card("report", 565, 745, 360, 88, "WorkReport", ["unifies success, failure, stop states"], "olive"),
    route("caller", "model", "M440 250 L570 250", "blue"),
    route("model", "sync", "M920 238 L1050 201", "green", false, { x: 985, y: 196, text: "yes sync", width: 86 }),
    route("model", "suspend", "M920 250 L1050 336", "teal", false, { x: 995, y: 304, text: "no suspend", width: 104 }),
    route("model", "families", "M745 315 L745 438 L455 438 L455 500", "amber"),
    route("sync", "runtime", "M1380 201 L1430 201 L1430 543 L1280 543", "pink"),
    route("suspend", "runtime", "M1215 377 L1215 480", "teal"),
    route("families", "report", "M455 616 L455 700 L650 700 L650 745", "amber"),
    route("runtime", "report", "M1070 606 L1070 700 L840 700 L840 745", "pink"),
  ].join("\n");
  write("utils-workflow-diagram-03", base(1500, 940, "Workflow Execution Model", "The same DSL selects sync or suspend execution while flow families and runtime primitives converge on the shared WorkReport contract.", body, "decision-flow"), {
    nodes: ["caller", "model", "sync", "suspend", "families", "runtime", "report"],
    edges: [["caller", "model"], ["model", "sync"], ["model", "suspend"], ["model", "families"], ["sync", "runtime"], ["suspend", "runtime"], ["families", "report"], ["runtime", "report"]],
  });
}

function renderSequentialFlow() {
  const body = [
    card("start", 110, 255, 270, 76, "Start flow", ["initial WorkContext"], "blue"),
    card("work", 500, 245, 330, 96, "Execute next Work", ["exceptions become Failure"], "green"),
    diamond("failed", 955, 230, 300, 126, "Failure?", ["check WorkReport"], "pink"),
    card("next", 955, 470, 310, 82, "Advance to next work", ["or finish with Success"], "olive"),
    diamond("strategy", 500, 505, 360, 136, "ErrorStrategy?", ["STOP or CONTINUE"], "teal"),
    card("stop", 220, 735, 310, 76, "Return Failure", ["STOP exits immediately"], "pink"),
    card("accumulate", 770, 720, 360, 92, "Accumulate failure", ["continue later work", "final result becomes PartialSuccess"], "amber"),
    route("start", "work", "M380 283 L500 283", "blue"),
    route("work", "failed", "M830 293 L955 293", "green"),
    route("failed", "next", "M1105 356 L1105 470", "green"),
    route("failed", "strategy", "M1055 356 L1055 448 L680 448 L680 505", "pink"),
    route("strategy", "stop", "M500 573 L375 573 L375 735", "pink"),
    route("strategy", "accumulate", "M680 641 L680 685 L950 685 L950 720", "amber"),
    text(1240, 414, "no", "detail"),
    text(955, 392, "yes", "detail"),
    text(430, 654, "yes STOP", "detail"),
    text(1018, 666, "no CONTINUE", "detail"),
  ].join("\n");
  write("utils-workflow-diagram-04", base(1390, 930, "Sequential Flow Error Strategy", "Sequential flows run work in order; STOP returns the first failure while CONTINUE accumulates failures and reaches a partial result.", body, "decision-flow"), {
    nodes: ["start", "work", "failed", "next", "strategy", "accumulate", "stop"],
    edges: [["start", "work"], ["work", "failed"], ["failed", "next"], ["failed", "strategy"], ["strategy", "stop"], ["strategy", "accumulate"]],
  });
}

function renderParallelFlow() {
  const body = [
    card("start", 120, 230, 300, 78, "Parallel flow starts", ["shared WorkContext"], "blue"),
    diamond("policy", 555, 200, 340, 136, "ParallelPolicy?", ["ALL or ANY"], "purple"),
    card("forkAll", 1040, 155, 330, 88, "ALL: fail-fast scope", ["wait for every fork", "failure cancels siblings"], "green"),
    card("forkAny", 1040, 325, 330, 88, "ANY: first-success scope", ["first Success wins", "non-success keeps racing"], "teal"),
    card("timeout", 120, 520, 300, 80, "Timeout boundary", ["returns Cancelled"], "amber"),
    card("result", 555, 530, 340, 120, "Collected WorkReports", ["priority chooses outcome"], "pink"),
    card("success", 1040, 560, 330, 82, "Success result", ["all passed or first success"], "olive"),
    card("failure", 1040, 705, 330, 82, "Failure / Aborted", ["highest-priority stop state"], "pink"),
    route("start", "policy", "M420 269 L555 268", "blue"),
    route("policy", "forkAll", "M895 248 L1040 199", "green", false, { x: 970, y: 198, text: "ALL", width: 52 }),
    route("policy", "forkAny", "M895 268 L1040 369", "teal", false, { x: 970, y: 338, text: "ANY", width: 52 }),
    route("forkAll", "result", "M1370 199 L1420 199 L1420 470 L690 470 L690 530", "green"),
    route("forkAny", "result", "M1205 413 L1205 505 L780 505 L780 530", "teal"),
    route("start", "timeout", "M270 308 L270 520", "amber", true),
    route("timeout", "result", "M420 560 L555 596", "amber", true, { x: 490, y: 548, text: "cancel", width: 74 }),
    route("result", "success", "M895 590 L1040 601", "olive", false, { x: 965, y: 560, text: "success", width: 82 }),
    route("result", "failure", "M725 650 L725 746 L1040 746", "pink", false, { x: 875, y: 723, text: "failure/abort", width: 118 }),
  ].join("\n");
  write("utils-workflow-diagram-05", base(1490, 900, "Parallel Flow Policy", "ALL waits under fail-fast structured concurrency; ANY returns the first success while timeout and non-success reports still map to WorkReport outcomes.", body, "decision-flow"), {
    nodes: ["start", "policy", "forkAll", "forkAny", "timeout", "result", "success", "failure"],
    edges: [["start", "policy"], ["policy", "forkAll"], ["policy", "forkAny"], ["forkAll", "result"], ["forkAny", "result"], ["start", "timeout"], ["timeout", "result"], ["result", "success"], ["result", "failure"]],
  });
}

function renderConditionalFlow() {
  const body = [
    card("start", 145, 330, 290, 78, "Conditional flow", ["evaluate predicate"], "blue"),
    diamond("predicate", 560, 285, 350, 140, "Predicate true?", ["WorkContext -> Boolean"], "purple"),
    card("then", 1040, 220, 330, 86, "Then work", ["execute selected branch"], "green"),
    card("otherwise", 1040, 430, 330, 86, "Otherwise work", ["optional false branch"], "pink"),
    card("default", 610, 635, 330, 82, "Default Success", ["when otherwise is absent"], "amber"),
    card("report", 1370, 345, 260, 110, "Branch WorkReport", ["returned unchanged"], "teal"),
    route("start", "predicate", "M435 369 L560 355", "blue"),
    route("predicate", "then", "M910 355 L1040 263", "green", false, { x: 980, y: 285, text: "yes", width: 52 }),
    route("predicate", "otherwise", "M910 355 L1040 473", "pink", false, { x: 980, y: 430, text: "no", width: 48 }),
    route("predicate", "default", "M735 425 L735 635", "amber", true, { x: 792, y: 534, text: "no branch", width: 102 }),
    route("then", "report", "M1370 263 L1498 345", "green"),
    route("otherwise", "report", "M1370 473 L1498 455", "pink"),
    route("default", "report", "M940 676 L1500 676 L1500 455", "amber", true),
  ].join("\n");
  write("utils-workflow-diagram-06", base(1700, 850, "Conditional Flow Branching", "The predicate chooses exactly one branch; a missing otherwise branch is a successful no-op, not a hidden failure path.", body, "decision-flow"), {
    nodes: ["start", "predicate", "then", "otherwise", "default", "report"],
    edges: [["start", "predicate"], ["predicate", "then"], ["predicate", "otherwise"], ["predicate", "default"], ["then", "report"], ["otherwise", "report"], ["default", "report"]],
  });
}

function renderRepeatFlow() {
  const body = [
    card("start", 130, 245, 290, 76, "Repeat flow starts", ["iteration = 0"], "blue"),
    card("work", 560, 230, 330, 96, "Execute work", ["one iteration produces report"], "green"),
    diamond("terminal", 1030, 215, 320, 126, "Abort/cancel?", ["terminal states bypass loop"], "pink"),
    diamond("predicate", 565, 490, 320, 126, "Repeat predicate?", ["uses latest WorkReport"], "purple"),
    diamond("limit", 175, 490, 300, 126, "Max iterations?", ["safety guard"], "amber"),
    card("delay", 565, 715, 320, 78, "Repeat delay", ["suspend flow may wait"], "teal"),
    card("done", 1030, 520, 320, 82, "Return last report", ["predicate false or limit reached"], "olive"),
    route("start", "work", "M420 283 L560 278", "blue"),
    route("work", "terminal", "M890 278 L1030 278", "green"),
    route("terminal", "done", "M1190 341 L1190 520", "pink", false, { x: 1246, y: 430, text: "yes", width: 52 }),
    route("terminal", "predicate", "M1085 341 L1085 430 L725 430 L725 490", "green", false, { x: 1044, y: 386, text: "no", width: 48 }),
    route("predicate", "done", "M885 553 L1030 561", "olive", false, { x: 958, y: 520, text: "false", width: 64 }),
    route("predicate", "limit", "M565 553 L475 553", "amber", false, { x: 520, y: 520, text: "true", width: 58 }),
    route("limit", "done", "M400 616 L400 665 L1190 665 L1190 602", "amber", true, { x: 760, y: 640, text: "limit reached", width: 122 }),
    route("limit", "delay", "M250 616 L250 754 L565 754", "teal", false, { x: 445, y: 728, text: "room left", width: 100 }),
    route("delay", "work", "M885 754 L1390 754 L1390 190 L725 190 L725 230", "teal", true),
  ].join("\n");
  write("utils-workflow-diagram-07", base(1460, 940, "Repeat Flow State Loop", "Each iteration feeds the latest WorkReport into the repeat predicate; terminal reports and maxIterations stop the loop.", body, "decision-flow"), {
    nodes: ["start", "work", "terminal", "predicate", "limit", "delay", "done"],
    edges: [["start", "work"], ["work", "terminal"], ["terminal", "done"], ["terminal", "predicate"], ["predicate", "done"], ["predicate", "limit"], ["limit", "done"], ["limit", "delay"], ["delay", "work"]],
  });
}

function renderRetryFlow() {
  const body = [
    card("start", 120, 235, 300, 78, "Retry flow starts", ["attempt = 1"], "blue"),
    card("work", 560, 220, 330, 96, "Execute work", ["exceptions become Failure"], "green"),
    diamond("result", 1030, 200, 320, 136, "Success or stop?", ["Success, Aborted, Cancelled"], "purple"),
    card("success", 1030, 445, 320, 82, "Return terminal report", ["success/abort/cancel"], "olive"),
    diamond("attempts", 560, 525, 330, 136, "Attempts left?", ["maxAttempts includes first try"], "amber"),
    card("failure", 1010, 635, 340, 82, "Return Failure", ["last failed WorkReport"], "pink"),
    card("backoff", 160, 635, 320, 90, "Backoff delay", ["delay * multiplier", "bounded by maxDelay"], "teal"),
    route("start", "work", "M420 274 L560 268", "blue"),
    route("work", "result", "M890 268 L1030 268", "green"),
    route("result", "success", "M1300 336 L1190 445", "olive"),
    route("result", "attempts", "M1190 336 L1190 410 L725 410 L725 525", "pink"),
    text(1405, 378, "yes", "detail"),
    text(1000, 386, "failure", "detail"),
    route("attempts", "failure", "M890 593 L1010 676", "pink", false, { x: 950, y: 620, text: "no", width: 48 }),
    route("attempts", "backoff", "M560 593 L480 680", "teal", false, { x: 516, y: 624, text: "yes", width: 52 }),
    route("backoff", "work", "M320 635 L320 400 L725 400 L725 316", "teal", true, { x: 525, y: 379, text: "retry", width: 66 }),
  ].join("\n");
  write("utils-workflow-diagram-08", base(1460, 900, "Retry Flow With Backoff", "Only Failure enters the retry loop; Success, Aborted, and Cancelled return immediately, while maxAttempts bounds retry work.", body, "decision-flow"), {
    nodes: ["start", "work", "result", "attempts", "backoff", "failure", "success"],
    edges: [["start", "work"], ["work", "result"], ["result", "success"], ["result", "attempts"], ["attempts", "failure"], ["attempts", "backoff"], ["backoff", "work"]],
  });
}

function renderLoggingProcessingFlow() {
  const body = [
    card("caller", 120, 250, 300, 80, "Application code", ["calls logger extension"], "blue"),
    card("delegate", 520, 235, 340, 110, "KotlinLogging facade", ["KLogging / loggerOf", "lazy message lambdas"], "green"),
    card("context", 1000, 170, 340, 96, "MDC and coroutine context", ["correlation values", "scoped cleanup"], "purple"),
    card("supplier", 1000, 360, 340, 96, "Message supplier", ["evaluated only when enabled"], "amber"),
    diamond("enabled", 560, 520, 300, 128, "Level enabled?", ["trace/debug/info/warn/error"], "teal"),
    card("skip", 145, 690, 280, 76, "Skip allocation", ["supplier is not evaluated"], "gray"),
    card("backend", 600, 720, 340, 86, "SLF4J backend", ["Logback or bound logger"], "pink"),
    card("event", 1040, 710, 320, 96, "Log event", ["message, throwable, MDC fields"], "olive"),
    route("caller", "delegate", "M420 290 L520 290", "blue"),
    route("delegate", "context", "M860 275 L1000 218", "purple"),
    route("delegate", "supplier", "M860 290 L1000 408", "amber"),
    route("delegate", "enabled", "M690 345 L690 520", "teal"),
    route("enabled", "skip", "M560 584 L425 728", "gray", true, { x: 480, y: 640, text: "no", width: 48 }),
    route("enabled", "backend", "M710 648 L710 720", "pink", false, { x: 766, y: 686, text: "yes", width: 52 }),
    route("context", "event", "M1340 218 L1390 218 L1390 758 L1360 758", "purple"),
    route("supplier", "event", "M1170 456 L1170 710", "amber"),
    route("backend", "event", "M940 763 L1040 758", "pink"),
  ].join("\n");
  write("bluetape4k-logging-diagram-02", base(1480, 930, "Logging Processing Flow", "Logger extensions keep call sites small while lazy suppliers, MDC context, and backend emission stay distinct.", body, "decision-flow"), {
    nodes: ["caller", "delegate", "context", "supplier", "enabled", "skip", "backend", "event"],
    edges: [["caller", "delegate"], ["delegate", "context"], ["delegate", "supplier"], ["delegate", "enabled"], ["enabled", "skip"], ["enabled", "backend"], ["context", "event"], ["supplier", "event"], ["backend", "event"]],
  });
}

function renderJackson2ConversionFlow() {
  const body = [
    card("caller", 120, 250, 300, 78, "Caller code", ["serialize or deserialize"], "blue"),
    card("mapper", 520, 230, 360, 112, "Jackson2 ObjectMapper", ["Kotlin module", "JavaTime / binary modules"], "green"),
    diamond("format", 1000, 210, 320, 132, "Payload format?", ["JSON, Smile, CBOR"], "purple"),
    card("json", 1030, 430, 300, 80, "JSON serializer", ["string or UTF-8 bytes"], "teal"),
    card("binary", 1030, 575, 300, 92, "Smile / CBOR serializers", ["binary ByteArray payload"], "amber"),
    card("stringOut", 520, 660, 300, 80, "String output", ["readable JSON"], "olive"),
    card("bytesOut", 900, 760, 320, 80, "ByteArray output", ["JSON bytes or binary format"], "pink"),
    card("error", 145, 600, 300, 90, "Error policy", ["wrap Jackson failures", "preserve type context"], "gray"),
    route("caller", "mapper", "M420 289 L520 286", "blue"),
    route("mapper", "format", "M880 286 L1000 276", "green"),
    route("format", "json", "M1160 342 L1160 430", "teal"),
    route("format", "binary", "M1320 276 L1390 276 L1390 621 L1330 621", "amber"),
    text(1265, 386, "default", "detail"),
    text(1300, 455, "binary", "detail"),
    route("json", "stringOut", "M1030 470 L670 470 L670 660", "olive"),
    route("json", "bytesOut", "M1180 510 L1180 545 L860 545 L860 800 L900 800", "pink"),
    route("binary", "bytesOut", "M1180 667 L1180 760", "amber"),
    route("mapper", "error", "M610 342 L610 520 L295 520 L295 600", "gray", true),
  ].join("\n");
  write("io-jackson2-diagram-02", base(1500, 940, "Jackson2 Conversion Flow", "ObjectMapper configuration is shared; format choice selects JSON text or Smile/CBOR binary serializers before returning String or ByteArray payloads.", body, "decision-flow"), {
    nodes: ["caller", "mapper", "format", "json", "binary", "stringOut", "bytesOut", "error"],
    edges: [["caller", "mapper"], ["mapper", "format"], ["format", "json"], ["format", "binary"], ["json", "stringOut"], ["json", "bytesOut"], ["binary", "bytesOut"], ["mapper", "error"]],
  });
}

function renderVertxFutureProcessingFlow() {
  const body = [
    card("producer", 120, 250, 310, 78, "Vert.x async work", ["returns Future<T>"], "blue"),
    card("future", 540, 235, 330, 108, "Future<T>", ["callback-driven completion", "typed result or cause"], "green"),
    card("await", 1010, 210, 330, 86, "Coroutine await", ["suspend until complete"], "teal"),
    card("decorators", 1010, 395, 330, 96, "Retry / resilience decorators", ["recover, compose, timeout"], "amber"),
    card("complete", 560, 540, 320, 96, "Completion result", ["success value or failure cause"], "purple"),
    card("success", 980, 690, 330, 82, "Success result", ["resume caller with value"], "olive"),
    card("failure", 170, 690, 330, 82, "Failure cause", ["throw, recover, or map error"], "pink"),
    route("producer", "future", "M430 289 L540 289", "blue"),
    route("future", "await", "M870 289 L1010 253", "teal"),
    route("future", "decorators", "M870 300 L1010 443", "amber"),
    route("future", "complete", "M705 343 L705 540", "purple"),
    route("decorators", "complete", "M1175 491 L1175 520 L720 520 L720 540", "amber"),
    route("complete", "success", "M880 588 L940 588 L940 650 L1145 650 L1145 690", "olive"),
    route("complete", "failure", "M560 588 L500 588 L500 645 L335 645 L335 690", "pink"),
    text(1010, 668, "success", "detail"),
    text(410, 648, "failure", "detail"),
  ].join("\n");
  write("io-vertx-diagram-02", base(1460, 900, "Vert.x Future Processing Flow", "Vert.x Future helpers bridge callback completion into suspending code while retry and recovery decorators preserve explicit success and failure paths.", body, "decision-flow"), {
    nodes: ["producer", "future", "await", "decorators", "complete", "success", "failure"],
    edges: [["producer", "future"], ["future", "await"], ["future", "decorators"], ["future", "complete"], ["decorators", "complete"], ["complete", "success"], ["complete", "failure"]],
  });
}

function renderMicrometerComponentMap() {
  const body = [
    card("app", 120, 315, 310, 86, "Application work", ["suspend, Flow, HTTP, cache"], "blue"),
    card("helpers", 560, 220, 360, 118, "Instrumentation helpers", ["Timer extensions", "Observation and event telemetry"], "green"),
    card("collectors", 560, 520, 360, 104, "Framework collectors", ["Retrofit, cache, events"], "pink"),
    card("registries", 1030, 320, 350, 130, "Registry boundary", ["MeterRegistry metrics", "ObservationRegistry lifecycle"], "teal"),
    card("exporters", 1030, 665, 350, 86, "Metrics and trace signals", ["Prometheus / tracing bridge"], "olive"),
    route("app", "helpers", "M430 358 L560 279", "green"),
    route("app", "collectors", "M430 358 L560 572", "pink", true),
    route("helpers", "registries", "M920 279 L1030 385", "teal"),
    route("collectors", "registries", "M920 572 L1030 385", "pink"),
    route("registries", "exporters", "M1205 450 L1205 665", "olive"),
  ].join("\n");
  write("infra-micrometer-diagram-03", base(1420, 900, "Micrometer Instrumentation Component Map", "Timer helpers, observation helpers, and framework collectors feed explicit registry boundaries before metrics or trace signals leave the module.", body), {
    nodes: ["app", "helpers", "collectors", "registries", "exporters"],
    edges: [["app", "helpers"], ["app", "collectors"], ["helpers", "registries"], ["collectors", "registries"], ["registries", "exporters"]],
  });
}

function renderKafkaStreams() {
  const body = [
    card("inputTopic", 130, 260, 300, 82, "Input topic", ["keyed records"], "blue"),
    card("topology", 560, 220, 390, 142, "Streams topology DSL", ["stream -> groupByKey", "count(materialized)", "toStream -> to(output)"], "green"),
    card("serdeConfig", 1080, 180, 330, 110, "Serde configuration", ["Consumed, Grouped, Produced"], "purple"),
    card("stateStore", 1080, 410, 330, 104, "Materialized state store", ["count KTable backing store"], "amber"),
    card("outputTopic", 560, 660, 390, 82, "Output topic", ["processed stream result"], "olive"),
    route("inputTopic", "topology", "M430 301 L560 291", "blue"),
    route("topology", "serdeConfig", "M950 291 L1080 235", "purple"),
    route("topology", "stateStore", "M950 310 L1080 462", "amber"),
    route("topology", "outputTopic", "M755 362 L755 660", "green"),
    route("stateStore", "outputTopic", "M1080 462 L960 462 L960 701 L950 701", "amber", true),
  ].join("\n");
  write("infra-kafka-diagram-02", base(1500, 860, "Kafka Streams Topology Flow", "The diagram follows the DSL order and keeps Serde and materialized-store configuration tied to the topology step that consumes it.", body), {
    nodes: ["inputTopic", "topology", "serdeConfig", "stateStore", "outputTopic"],
    edges: [["inputTopic", "topology"], ["topology", "serdeConfig"], ["topology", "stateStore"], ["topology", "outputTopic"], ["stateStore", "outputTopic"]],
  });
}

function renderHibernateReactive() {
  const body = [
    panel(88, 165, 1244, 278, "CompletionStage API"),
    panel(88, 515, 1244, 300, "Mutiny Uni API"),
    card("stageFactory", 170, 245, 420, 128, "StageSessionFactory", ["withSession / withTransaction", "CompletionStage<T> contract"], "pink"),
    card("stageSession", 820, 245, 420, 128, "StageSession", ["find(cls, id): CompletionStage<T>", "reified findAs extension"], "blue"),
    card("mutinyFactory", 170, 605, 430, 146, "MutinySessionFactory", ["withSession / withTransaction", "suspending extension bridge"], "amber"),
    card("mutinySession", 820, 605, 420, 146, "MutinySession", ["find(cls, id): Uni<T>", "persist(entity): Uni<Void>"], "green"),
    card("bridge", 360, 865, 700, 112, "Coroutine extension bridge", ["suspending helpers adapt CompletionStage and Uni APIs", "without changing the session families"], "teal"),
    route("stageFactory", "stageSession", "M590 309 L820 309", "pink"),
    route("mutinyFactory", "mutinySession", "M600 678 L820 678", "amber"),
    route("stageFactory", "bridge", "M380 373 L380 455 L710 455 L710 865", "blue", true),
    route("mutinyFactory", "bridge", "M385 751 L385 828 L590 828 L590 865", "green", true),
  ].join("\n");
  write("data-hibernate-reactive-diagram-03", base(1420, 1040, "Reactive Session API Comparison", "Factories sit beside the session type they create; coroutine bridges stay as supporting extensions.", body), {
    nodes: ["stageFactory", "stageSession", "mutinyFactory", "mutinySession", "bridge"],
    edges: [["stageFactory", "stageSession"], ["mutinyFactory", "mutinySession"], ["stageFactory", "bridge"], ["mutinyFactory", "bridge"]],
  });
}

function renderElasticsearch() {
  const body = [
    panel(82, 160, 1350, 220, "Client construction"),
    card("app", 125, 225, 270, 94, "User application", ["DSL config", "suspend and Flow calls"], "blue"),
    card("builder", 485, 210, 300, 124, "Client Builder DSL", ["ElasticsearchClientConfig", "sync and async client builders"], "green"),
    card("clients", 875, 210, 310, 124, "ElasticsearchClients", ["clientOf / asyncClientOf", "transport factory"], "amber"),
    card("javaClients", 1190, 225, 210, 94, "ES Java clients", ["async", "blocking"], "pink"),
    panel(82, 455, 1350, 170, "Transport support"),
    card("transport", 355, 525, 320, 62, "Rest5ClientTransport", ["HTTP client 5 / auth / SSL"], "olive"),
    card("mapping", 840, 525, 320, 62, "JSON Mapping", ["JsonpMapper / Jackson fallback"], "blue"),
    panel(82, 700, 1350, 275, "Runtime flows"),
    card("search", 135, 785, 330, 118, "Search Flow", ["open PIT", "page with search_after", "close PIT in finally"], "teal"),
    card("bulk", 590, 785, 330, 118, "Bulk Flow", ["Flow<BulkOperation>", "chunked requests", "item error callback"], "amber"),
    card("cluster", 1075, 780, 300, 126, "Elasticsearch Cluster", ["Search API", "Bulk API", "PIT lifecycle"], "purple"),
    route("app", "builder", "M395 272 L485 272", "blue"),
    route("builder", "clients", "M785 272 L875 272", "green"),
    route("clients", "javaClients", "M1185 272 L1190 272", "amber"),
    route("javaClients", "cluster", "M1295 319 L1295 780", "pink"),
    route("clients", "transport", "M1030 334 L1030 415 L515 415 L515 525", "olive"),
    route("clients", "mapping", "M1090 334 L1090 450 L1000 450 L1000 525", "blue"),
    route("search", "cluster", "M465 870 L520 870 L520 940 L1225 940 L1225 906", "teal", true),
    route("bulk", "cluster", "M920 844 L1075 844", "amber"),
    route("builder", "search", "M635 334 L635 405 L340 405 L340 785", "green", true),
    route("builder", "bulk", "M700 334 L700 430 L810 430 L810 785", "green"),
  ].join("\n");
  write("infra-elasticsearch-diagram-02", base(1500, 1040, "Elasticsearch Module Architecture", "Client builders, transport support, and search/bulk flows are placed so every route lands on a visible card boundary.", body), {
    nodes: ["app", "builder", "clients", "javaClients", "search", "bulk", "cluster", "transport", "mapping"],
    edges: [["app", "builder"], ["builder", "clients"], ["clients", "javaClients"], ["javaClients", "cluster"], ["clients", "transport"], ["clients", "mapping"], ["search", "cluster"], ["bulk", "cluster"], ["builder", "search"], ["builder", "bulk"]],
  });
}

function renderRedissonBatchTransactionFlow() {
  const body = [
    panel(82, 160, 1436, 180, "DSL entrypoints"),
    panel(82, 380, 1436, 220, "Batch: queue commands, execute once"),
    panel(82, 670, 1436, 410, "Transaction: commit or rollback contract"),
    card("syncDsl", 150, 220, 330, 76, "Sync DSL", ["withBatch / withTransaction"], "blue"),
    card("suspendDsl", 610, 220, 360, 76, "Suspend DSL", ["withSuspendedBatch / Transaction"], "purple"),
    card("client", 1110, 220, 320, 76, "RedissonClient", ["creates RBatch or RTransaction"], "teal"),
    card("batchDsl", 135, 460, 320, 86, "withBatch block", ["action receiver: RBatch"], "blue"),
    card("rbatch", 545, 460, 320, 86, "RBatch", ["queues Redis commands", "BatchOptions"], "green"),
    card("execute", 945, 460, 320, 86, "execute", ["execute() or executeAsync().await()"], "teal"),
    card("batchResult", 1320, 460, 170, 86, "BatchResult", ["one RTT", "result list"], "amber"),
    card("txDsl", 135, 790, 320, 86, "withTransaction block", ["action receiver: RTransaction"], "purple"),
    card("rtx", 475, 790, 300, 86, "RTransaction", ["createTransaction", "TransactionOptions"], "green"),
    diamond("throws", 820, 770, 220, 126, "action throws?", ["normal exit vs exception"], "amber"),
    card("commit", 1060, 730, 300, 76, "Commit path", ["commit / commitAsync().await()"], "teal"),
    card("callerOutcome", 1150, 840, 300, 70, "Caller outcome", ["success result", "or original error"], "blue"),
    card("rollback", 1060, 950, 300, 76, "Rollback path", ["rollback then rethrow"], "pink"),
    text(1088, 742, "no", "detail"),
    text(1078, 918, "yes", "detail"),
    route("syncDsl", "client", "M315 296 L315 330 L1270 330 L1270 296", "blue"),
    route("suspendDsl", "client", "M970 258 L1110 258", "purple"),
    route("batchDsl", "rbatch", "M455 503 L545 503", "blue"),
    route("rbatch", "execute", "M865 503 L945 503", "green"),
    route("execute", "batchResult", "M1265 503 L1320 503", "teal"),
    route("txDsl", "rtx", "M455 833 L475 833", "purple"),
    route("rtx", "throws", "M775 833 L820 833", "green"),
    route("throws", "commit", "M1040 805 L1060 768", "teal"),
    route("throws", "rollback", "M1040 861 L1060 988", "pink", true),
    route("commit", "callerOutcome", "M1210 806 L1300 840", "teal"),
    route("rollback", "callerOutcome", "M1210 950 L1300 910", "pink", true),
  ].join("\n");
  const svg = base(1600, 1170, "Batch and Transaction Processing Flow", "Batch reduces Redis round trips by executing queued commands once; transaction wrappers commit on normal exit and rollback before rethrowing on failure.", body).replace("<svg ", '<svg data-layout="decision-flow" ');
  write("infra-redisson-diagram-02", svg, {
    nodes: ["syncDsl", "suspendDsl", "client", "batchDsl", "rbatch", "execute", "batchResult", "txDsl", "rtx", "throws", "commit", "rollback", "callerOutcome"],
    edges: [["syncDsl", "client"], ["suspendDsl", "client"], ["batchDsl", "rbatch"], ["rbatch", "execute"], ["execute", "batchResult"], ["txDsl", "rtx"], ["rtx", "throws"], ["throws", "commit"], ["throws", "rollback"], ["commit", "callerOutcome"], ["rollback", "callerOutcome"]],
  });
}

function renderJsonImplementationSelectionFlow() {
  const body = [
    panel(82, 160, 1356, 170, "Common contract"),
    panel(82, 405, 1356, 430, "Implementation choice"),
    panel(82, 910, 1356, 220, "Shared API surface"),
    card("caller", 150, 225, 300, 76, "Caller code", ["chooses one serializer"], "blue"),
    card("contract", 600, 220, 360, 86, "JsonSerializer", ["common byte and string contract"], "green"),
    card("reified", 1080, 225, 280, 76, "Kotlin reified helpers", ["deserialize<T>() shortcuts"], "purple"),
    diamond("needsJsonb", 315, 535, 230, 130, "Need JSONB?", ["Fastjson2 byte format"], "amber"),
    card("fastjson", 610, 485, 350, 86, "FastjsonSerializer", ["Fastjson2 JSONB bytes", "standard JSON string"], "teal"),
    diamond("jacksonMajor", 600, 660, 240, 130, "Jackson major?", ["module dependency decides"], "purple"),
    card("jackson3", 970, 590, 350, 86, "JacksonSerializer", ["bluetape4k-jackson3", "Jackson 3.x ObjectMapper"], "green"),
    card("jackson2", 970, 725, 350, 86, "JacksonSerializer", ["bluetape4k-jackson2", "Jackson 2.x ObjectMapper"], "blue"),
    card("byteApi", 210, 990, 320, 76, "ByteArray API", ["serialize / deserialize"], "pink"),
    card("stringApi", 600, 990, 320, 76, "JSON String API", ["serializeAsString", "deserializeFromString"], "olive"),
    card("failure", 950, 980, 420, 112, "Failure policy", ["null -> empty/null", "errors throw", "JsonSerializationException"], "amber"),
    text(505, 520, "yes", "detail"),
    text(485, 720, "no", "detail"),
    text(855, 590, "default 3.x", "detail"),
    text(855, 760, "fallback 2.x", "detail"),
    route("caller", "contract", "M450 263 L600 263", "blue"),
    route("contract", "reified", "M960 263 L1080 263", "green"),
    route("contract", "needsJsonb", "M780 306 L780 370 L430 370 L430 535", "amber"),
    route("needsJsonb", "fastjson", "M545 600 L610 528", "teal"),
    route("needsJsonb", "jacksonMajor", "M430 665 L430 725 L600 725", "purple"),
    route("jacksonMajor", "jackson3", "M840 725 L905 725 L905 633 L970 633", "green"),
    route("jacksonMajor", "jackson2", "M840 725 L970 768", "blue"),
  ].join("\n");
  const svg = base(1520, 1210, "JsonSerializer Implementation Selection Flow", "Choose Fastjson2 when JSONB byte payloads are required; otherwise select the Jackson 3 or Jackson 2 module and keep callers on the common JsonSerializer API.", body).replace("<svg ", '<svg data-layout="decision-flow" ');
  write("io-json-diagram-02", svg, {
    nodes: ["caller", "contract", "reified", "needsJsonb", "fastjson", "jacksonMajor", "jackson3", "jackson2", "byteApi", "stringApi", "failure"],
    edges: [["caller", "contract"], ["contract", "reified"], ["contract", "needsJsonb"], ["needsJsonb", "fastjson"], ["needsJsonb", "jacksonMajor"], ["jacksonMajor", "jackson3"], ["jacksonMajor", "jackson2"]],
  });
}

function renderHttpMultiBackendArchitecture() {
  const body = [
    panel(82, 160, 1436, 185, "Caller-facing API"),
    panel(82, 420, 1436, 250, "Backend client stacks"),
    panel(82, 745, 1436, 270, "Runtime boundary"),
    card("caller", 145, 235, 310, 78, "Application code", ["HTTP call intent"], "blue"),
    card("support", 610, 228, 360, 92, "HTTP support facade", ["request/entity/protocol DSL", "timeouts, retry, coroutine hooks"], "green"),
    card("cross", 1095, 235, 340, 78, "Cross-cutting helpers", ["auth, SSL, cache, routing"], "purple"),
    card("hc5", 130, 520, 270, 86, "Apache HC5", ["classic, async, cache", "virtual-thread factories"], "teal"),
    card("okhttp", 470, 520, 290, 86, "OkHttp3", ["interceptors, cache", "coroutine Call adapter"], "amber"),
    card("vertx", 830, 520, 300, 86, "Vert.x WebClient", ["event-loop client", "coroutine bridge"], "pink"),
    card("ktor", 1190, 520, 300, 86, "Ktor / JDK", ["Ktor CIO helpers", "JDK HttpClient support"], "olive"),
    card("wire", 160, 815, 1280, 78, "HTTP wire request / response", ["method, URI, headers, body, status, response bytes"], "blue"),
    card("remote", 610, 930, 380, 66, "Remote service", ["REST endpoint or mock benchmark server"], "green"),
    route("caller", "support", "M455 274 L610 274", "blue"),
    route("support", "cross", "M970 274 L1095 274", "green"),
    route("support", "hc5", "M700 320 L265 520", "teal"),
    route("support", "okhttp", "M760 320 L615 520", "amber"),
    route("support", "vertx", "M820 320 L980 520", "pink"),
    route("support", "ktor", "M880 320 L1340 520", "olive"),
    route("hc5", "wire", "M265 606 L420 815", "teal"),
    route("okhttp", "wire", "M615 606 L615 815", "amber"),
    route("vertx", "wire", "M980 606 L980 815", "pink"),
    route("ktor", "wire", "M1340 606 L1340 815", "olive"),
    route("wire", "remote", "M800 893 L800 930", "blue"),
  ].join("\n");
  write("io-http-diagram-01", base(1600, 1095, "Multi-Backend HTTP Client Architecture", "Common request helpers fan out to HC5, OkHttp3, Vert.x, Ktor, and JDK-backed stacks while every backend converges on the same HTTP wire boundary.", body), {
    nodes: ["caller", "support", "cross", "hc5", "okhttp", "vertx", "ktor", "wire", "remote"],
    edges: [["caller", "support"], ["support", "cross"], ["support", "hc5"], ["support", "okhttp"], ["support", "vertx"], ["support", "ktor"], ["hc5", "wire"], ["okhttp", "wire"], ["vertx", "wire"], ["ktor", "wire"], ["wire", "remote"]],
  });
}

function renderRedisUmbrellaModuleStructure() {
  const body = [
    panel(82, 160, 1336, 170, "Compatibility dependency"),
    panel(82, 405, 1336, 260, "Client module choices"),
    panel(82, 740, 1336, 150, "Reader guidance"),
    card("umbrella", 240, 230, 1020, 74, "bluetape4k-redis umbrella", ["keeps existing full-bundle dependency working"], "blue"),
    card("lettuce", 135, 495, 330, 94, "bluetape4k-lettuce", ["async/coroutine Redis client", "binary and Protobuf codecs"], "green"),
    card("redisson", 585, 495, 330, 94, "bluetape4k-redisson", ["distributed objects", "NearCache and leader election"], "purple"),
    card("springData", 1035, 495, 330, 94, "spring-data-redis", ["RedisTemplate serializers", "ReactiveRedisTemplate context"], "amber"),
    card("guidance", 230, 800, 1040, 66, "Choose by dependency boundary", ["umbrella for compatibility; direct clients for smaller dependencies; Spring Data Redis only for template serialization"], "gray"),
    route("umbrella", "lettuce", "M500 304 L500 542 L465 542", "green"),
    route("umbrella", "redisson", "M750 304 L750 495", "purple"),
  ].join("\n");
  write("infra-redis-diagram-01", base(1500, 980, "Redis Umbrella Module Structure", "The umbrella dependency bundles Lettuce and Redisson for compatibility; direct client dependencies and Spring Data Redis serializers remain separate choices.", body), {
    nodes: ["umbrella", "lettuce", "redisson", "springData", "guidance"],
    edges: [["umbrella", "lettuce"], ["umbrella", "redisson"]],
  });
}

function renderSpringRestClientDsl() {
  const body = [
    panel(82, 160, 1080, 180, "Coroutine-facing API"),
    panel(82, 430, 1080, 170, "HTTP exchange"),
    card("app", 120, 232, 280, 82, "Application code", ["calls suspend helper"], "blue"),
    card("dsl", 485, 218, 330, 112, "RestClient coroutine DSL", ["awaitBody / awaitEntity", "typed request helpers"], "green"),
    card("spring", 900, 232, 220, 82, "Spring RestClient", ["blocking client facade"], "amber"),
    card("request", 205, 485, 300, 88, "HTTP request", ["method, URI, headers, body"], "pink"),
    card("api", 705, 485, 300, 88, "External REST API", ["status, headers, response body"], "teal"),
    route("app", "dsl", "M400 273 L485 273", "blue"),
    route("dsl", "spring", "M815 274 L900 274", "green"),
    route("dsl", "request", "M650 330 L650 384 L355 384 L355 485", "purple"),
    route("request", "api", "M505 529 L705 529", "pink"),
    route("api", "spring", "M855 485 L855 384 L1010 384 L1010 314", "teal"),
  ].join("\n");
  write("spring-boot-core-diagram-03", base(1244, 710, "RestClient Coroutine DSL Flow", "Suspend helpers stay explicit between application code, Spring RestClient, and the actual HTTP exchange.", body), {
    nodes: ["app", "dsl", "spring", "request", "api"],
    edges: [["app", "dsl"], ["dsl", "spring"], ["dsl", "request"], ["request", "api"], ["api", "spring"]],
  });
}

function renderSpringBootCassandraDemo() {
  const body = [
    panel(82, 160, 1196, 160, "Web and service boundary"),
    panel(82, 405, 1196, 170, "Coroutine data access"),
    panel(82, 650, 1196, 110, "Cassandra runtime"),
    card("controller", 130, 210, 290, 84, "REST Controller", ["HTTP request / response"], "blue"),
    card("service", 540, 210, 300, 84, "Coroutine service", ["suspend orchestration"], "green"),
    card("repository", 960, 210, 260, 84, "Cassandra repository", ["query boundary"], "purple"),
    card("dsl", 250, 460, 330, 88, "CqlSession DSL", ["typed query helpers", "row mapping extensions"], "teal"),
    card("session", 765, 460, 330, 88, "CqlSession / AsyncCqlSession", ["driver execution", "prepared statements"], "amber"),
    card("cluster", 505, 685, 350, 68, "Cassandra cluster", ["tables, partitions, consistency"], "pink"),
    route("controller", "service", "M420 252 L540 252", "blue"),
    route("service", "repository", "M840 252 L960 252", "green"),
    route("repository", "dsl", "M1040 294 L415 460", "purple"),
    route("repository", "session", "M1140 294 L930 460", "amber"),
    route("dsl", "cluster", "M415 548 L415 615 L590 615 L590 685", "teal"),
    route("session", "cluster", "M930 548 L930 625 L770 625 L770 685", "amber"),
  ].join("\n");
  write("spring-boot-cassandra-demo-diagram-01", base(1360, 840, "Cassandra Demo Request Flow", "The demo diagram follows request handling into coroutine data access and the Cassandra driver runtime.", body), {
    nodes: ["controller", "service", "repository", "dsl", "session", "cluster"],
    edges: [["controller", "service"], ["service", "repository"], ["repository", "dsl"], ["repository", "session"], ["dsl", "cluster"], ["session", "cluster"]],
  });
}

function renderSpringBootObservabilityDemoArchitecture() {
  const body = [
    panel(82, 160, 1376, 190, "HTTP entry"),
    panel(82, 415, 1376, 200, "Application work"),
    panel(82, 680, 1376, 220, "Observation model"),
    panel(82, 960, 1376, 130, "Export endpoints"),
    card("client", 170, 235, 320, 78, "HTTP client", ["POST /orders/{id}/events", "optional X-Request-Id"], "blue"),
    card("controller", 600, 230, 360, 88, "OrderEventController", ["Spring MVC route", "delegates to service"], "teal"),
    card("health", 1065, 235, 300, 78, "Actuator / health", ["Spring Boot owned endpoint"], "purple"),
    card("service", 600, 490, 360, 92, "OrderEventService", ["creates EventTelemetry", "returns accepted JSON"], "green"),
    card("observeSpring", 180, 760, 340, 92, "observeSpring", ["orders.http.publish", "HTTP/service boundary"], "teal"),
    card("registry", 600, 760, 340, 92, "ObservationRegistry", ["Micrometer handlers", "metrics and optional spans"], "purple"),
    card("eventTelemetry", 1030, 760, 340, 92, "EventTelemetry", ["event.publish", "event.consume"], "green"),
    card("prometheus", 365, 1010, 350, 66, "Actuator Prometheus", ["/actuator/prometheus"], "amber"),
    card("otlp", 825, 1010, 350, 66, "Optional OTLP traces", ["configured by Spring properties"], "purple"),
    route("client", "controller", "M490 274 L600 274", "blue"),
    route("controller", "health", "M960 274 L1065 274", "purple", true),
    route("controller", "service", "M780 318 L780 490", "teal"),
    route("service", "observeSpring", "M695 582 L695 650 L350 650 L350 760", "teal"),
    route("service", "eventTelemetry", "M865 582 L865 650 L1200 650 L1200 760", "green"),
    route("observeSpring", "registry", "M520 806 L600 806", "purple"),
    route("eventTelemetry", "registry", "M1030 806 L940 806", "green"),
    route("registry", "prometheus", "M710 852 L710 925 L540 925 L540 1010", "amber"),
    route("registry", "otlp", "M830 852 L830 930 L1000 930 L1000 1010", "purple", true),
  ].join("\n");
  write("examples-spring-boot-observability-spring-boot-demo-architecture-01", base(1540, 1160, "Spring Boot Observability Demo Architecture", "The demo records HTTP and order-event observations locally, then Spring Boot Actuator exposes Prometheus metrics while OTLP tracing remains optional.", body), {
    nodes: ["client", "controller", "health", "service", "observeSpring", "registry", "eventTelemetry", "prometheus", "otlp"],
    edges: [["client", "controller"], ["controller", "health"], ["controller", "service"], ["service", "observeSpring"], ["service", "eventTelemetry"], ["observeSpring", "registry"], ["eventTelemetry", "registry"], ["registry", "prometheus"], ["registry", "otlp"]],
  });
}

function renderCsvProcessingFlow() {
  const body = [
    panel(82, 160, 1436, 175, "Input and settings"),
    panel(82, 410, 1436, 190, "Synchronous Sequence path"),
    panel(82, 675, 1436, 190, "Coroutine Flow path"),
    panel(82, 940, 1436, 155, "Output"),
    card("source", 180, 235, 370, 74, "File / InputStream / String", ["UTF-8 or caller charset"], "blue"),
    card("settings", 1000, 235, 360, 74, "CsvSettings / TsvSettings", ["delimiter, quote, empty/null policy"], "purple"),
    card("syncReader", 150, 470, 350, 88, "Csv/TsvRecordReader", ["read(...) returns Sequence<Record>"], "green"),
    card("lexer", 615, 465, 350, 98, "CsvLexer / TsvLexer", ["RFC 4180 state machine", "HeaderIndex + ArrayRecord"], "teal"),
    card("sequence", 1080, 470, 340, 88, "Sequence<T>", ["mapper transforms each Record"], "amber"),
    card("suspendReader", 150, 735, 370, 88, "SuspendCsv/TsvRecordReader", ["cold Flow<Record>", "parsed on collect"], "pink"),
    card("flowEngine", 615, 730, 350, 98, "runInterruptible parser", ["uses same lexer semantics", "cancellation-aware collection"], "purple"),
    card("flow", 1080, 735, 340, 88, "Flow<T>", ["collect rows in order"], "green"),
    card("output", 335, 1005, 360, 68, "File / Writer output", ["CSV or TSV rows"], "teal"),
    card("writer", 835, 1005, 430, 68, "RecordWriter / SuspendRecordWriter", ["DelimitedWriter / Okio writer fast path"], "amber"),
    route("source", "syncReader", "M365 309 L365 385 L470 385 L470 470", "blue"),
    route("settings", "lexer", "M1180 309 L1180 360 L795 360 L795 465", "purple"),
    route("syncReader", "lexer", "M500 514 L615 514", "green"),
    route("lexer", "sequence", "M965 514 L1080 514", "teal"),
    route("source", "suspendReader", "M180 272 L100 272 L100 779 L150 779", "pink", true),
    route("suspendReader", "flowEngine", "M520 779 L615 779", "pink"),
    route("flowEngine", "flow", "M965 779 L1080 779", "purple"),
    route("sequence", "writer", "M1420 514 L1450 514 L1450 1020 L1265 1020", "amber"),
    route("flow", "writer", "M1250 823 L1250 1005", "green"),
    route("writer", "output", "M835 1039 L695 1039", "teal"),
  ].join("\n");
  write("io-csv-diagram-02", base(1600, 1180, "CSV/TSV Processing Flow", "The flow separates synchronous Sequence parsing from coroutine Flow parsing while both converge on the same writer/output path.", body), {
    nodes: ["source", "settings", "syncReader", "lexer", "sequence", "suspendReader", "flowEngine", "flow", "writer", "output"],
    edges: [["source", "syncReader"], ["settings", "lexer"], ["syncReader", "lexer"], ["lexer", "sequence"], ["source", "suspendReader"], ["suspendReader", "flowEngine"], ["flowEngine", "flow"], ["sequence", "writer"], ["flow", "writer"], ["writer", "output"]],
  });
}

function renderSpringWebFluxCoroutineFlow() {
  const body = [
    panel(82, 160, 1276, 150, "HTTP entry"),
    panel(82, 385, 1276, 220, "Coroutine controller contract"),
    panel(82, 680, 1276, 180, "Client and test helpers"),
    panel(82, 935, 1276, 120, "Runtime boundary"),
    card("client", 130, 215, 260, 74, "HTTP client", ["browser, service, or test"], "blue"),
    card("webflux", 520, 210, 310, 84, "Spring WebFlux", ["Netty + DispatcherHandler"], "green"),
    card("route", 970, 215, 300, 74, "Controller route", ["suspend fun or Flow<T>"], "purple"),
    card("defaultScope", 140, 455, 300, 96, "Default scope base", ["Dispatchers.Default", "CPU-oriented work"], "teal"),
    card("ioScope", 520, 455, 300, 96, "IO scope base", ["Dispatchers.IO", "blocking bridge work"], "amber"),
    card("vtScope", 900, 455, 320, 96, "VT scope base", ["Dispatchers.VT", "VT-backed blocking bridge"], "pink"),
    card("webClient", 150, 735, 340, 84, "WebClient helpers", ["httpGet/httpPost/httpPut", "Publisher / Flow bodies"], "green"),
    card("webTest", 550, 735, 410, 84, "WebTestClient helpers", ["exchange + status assertion", "Publisher / Flow body"], "blue"),
    card("reactorBridge", 985, 735, 300, 84, "Reactor bridge", ["Mono/Flux coroutine bridge", "await/collect at call site"], "purple"),
    card("service", 510, 970, 320, 66, "App service", ["business coroutine code"], "olive"),
    card("external", 900, 970, 320, 66, "External HTTP / DB", ["called by service or WebClient"], "pink"),
    route("client", "webflux", "M390 252 L520 252", "blue"),
    route("webflux", "route", "M830 252 L970 252", "green"),
    route("route", "defaultScope", "M1040 289 L1040 350 L500 350 L500 503 L440 503", "teal"),
    route("route", "ioScope", "M1120 289 L1120 365 L670 365 L670 455", "amber"),
    route("route", "vtScope", "M1200 289 L1200 420 L1060 420 L1060 455", "pink"),
    route("defaultScope", "service", "M290 551 L290 640 L500 640 L500 910 L585 910 L585 970", "teal"),
    route("ioScope", "service", "M670 551 L670 620 L535 620 L535 930 L635 930 L635 970", "amber"),
    route("vtScope", "external", "M1060 551 L1060 620 L1320 620 L1320 1003 L1220 1003", "pink"),
    route("webClient", "external", "M320 819 L320 885 L940 885 L940 970", "green"),
    route("reactorBridge", "webTest", "M985 777 L960 777", "purple"),
    route("service", "external", "M830 1003 L900 1003", "olive"),
  ].join("\n");
  write("spring-boot-core-diagram-02", base(1440, 1135, "Spring WebFlux + Coroutines Request Model", "The source-backed model separates WebFlux entry, coroutine controller scope choices, helper clients, and runtime exits.", body), {
    nodes: ["client", "webflux", "route", "defaultScope", "ioScope", "vtScope", "webClient", "webTest", "reactorBridge", "service", "external"],
    edges: [["client", "webflux"], ["webflux", "route"], ["route", "defaultScope"], ["route", "ioScope"], ["route", "vtScope"], ["defaultScope", "service"], ["ioScope", "service"], ["vtScope", "external"], ["webClient", "external"], ["reactorBridge", "webTest"], ["service", "external"]],
  });
}

function renderSpringBootRetrofitIntegration() {
  const body = [
    panel(82, 160, 1276, 155, "Spring Boot application-owned configuration"),
    panel(82, 390, 1276, 250, "bluetape4k-retrofit2 assembly"),
    panel(82, 735, 1276, 190, "Runtime HTTP path"),
    card("springApp", 135, 215, 310, 76, "Spring Boot app", ["defines service beans and base URLs"], "blue"),
    card("beanConfig", 565, 210, 330, 86, "Retrofit @Bean config", ["calls retrofitOf / builder DSL", "keeps credentials app-owned"], "green"),
    card("serviceIface", 1015, 215, 260, 76, "API service interface", ["suspend functions / Call<T>"], "purple"),
    card("retrofitOf", 165, 470, 310, 98, "retrofitOf builder", ["baseUrl + converter factory", "default ResultCallAdapterFactory"], "teal"),
    card("callAdapters", 575, 455, 320, 126, "Call adapters", ["Result<T> adapter", "optional RxJava/Reactor adapters", "coroutine service methods"], "amber"),
    card("callFactory", 1000, 455, 300, 126, "HTTP call factories", ["OkHttp default", "Apache HC5 bridge", "Vert.x bridge"], "pink"),
    card("retrofit", 185, 805, 300, 84, "Retrofit proxy", ["creates typed API implementation"], "purple"),
    card("httpClient", 590, 805, 300, 84, "HTTP client", ["request execution and cancellation"], "green"),
    card("remoteApi", 1000, 805, 300, 84, "External REST API", ["JSON response or transport error"], "olive"),
    route("springApp", "beanConfig", "M445 253 L565 253", "blue"),
    route("beanConfig", "serviceIface", "M895 253 L1015 253", "green"),
    route("beanConfig", "retrofitOf", "M730 296 L730 350 L500 350 L500 440 L320 440 L320 470", "teal"),
    route("serviceIface", "retrofit", "M1145 291 L1145 350 L1320 350 L1320 950 L335 950 L335 889", "purple", true),
    route("retrofitOf", "callAdapters", "M475 519 L575 519", "amber"),
    route("callAdapters", "callFactory", "M895 519 L1000 519", "pink"),
    route("retrofitOf", "retrofit", "M320 568 L320 665 L500 665 L500 830 L485 830", "teal"),
    route("callFactory", "httpClient", "M1150 581 L1150 685 L740 685 L740 805", "pink"),
    route("retrofit", "httpClient", "M485 847 L590 847", "purple"),
    route("httpClient", "remoteApi", "M890 847 L1000 847", "green"),
  ].join("\n");
  write("spring-boot-core-diagram-04", base(1440, 1010, "Spring Boot + Retrofit2 Integration Map", "This redraw treats Retrofit2 as an integration boundary: Spring config assembles the module, then Retrofit proxies route through selected HTTP clients.", body), {
    nodes: ["springApp", "beanConfig", "serviceIface", "retrofitOf", "callAdapters", "callFactory", "retrofit", "httpClient", "remoteApi"],
    edges: [["springApp", "beanConfig"], ["beanConfig", "serviceIface"], ["beanConfig", "retrofitOf"], ["serviceIface", "retrofit"], ["retrofitOf", "callAdapters"], ["callAdapters", "callFactory"], ["retrofitOf", "retrofit"], ["callFactory", "httpClient"], ["retrofit", "httpClient"], ["httpClient", "remoteApi"]],
  });
}

function renderSpringCassandraDataAccessLayer() {
  const body = [
    panel(82, 160, 1276, 175, "Application boundary"),
    panel(82, 410, 1276, 230, "Coroutine extension families"),
    panel(82, 745, 1276, 190, "Spring Data Cassandra APIs"),
    panel(82, 1025, 1276, 155, "Driver and cluster"),
    card("repository", 135, 235, 320, 74, "Repository code", ["suspend functions and Flow<T>"], "blue"),
    card("admin", 560, 235, 310, 74, "Admin tasks", ["schema create / truncate"], "purple"),
    card("domain", 985, 235, 290, 74, "Domain mapper", ["Row/Gettable -> model"], "green"),
    card("reactiveOps", 130, 485, 350, 98, "ReactiveCassandraOperations", ["selectAsFlow", "insert/update/delete suspending"], "teal"),
    card("asyncOps", 550, 485, 340, 98, "AsyncCassandraOperations", ["await async query results", "count/exists/delete helpers"], "amber"),
    card("sessionDsl", 970, 485, 320, 98, "CqlSession DSL", ["options builder", "statement and row helpers"], "pink"),
    card("springOps", 135, 820, 330, 82, "Spring Data operations", ["Reactive/Async operations"], "green"),
    card("schema", 550, 820, 320, 82, "Schema utilities", ["SchemaGenerator", "truncate/create support"], "purple"),
    card("session", 970, 815, 330, 92, "ReactiveSession / CqlSession", ["prepare and execute statements"], "blue"),
    card("driver", 345, 1085, 320, 66, "Cassandra Java driver", ["CqlSession and async result sets"], "amber"),
    card("cluster", 805, 1085, 320, 66, "Cassandra cluster", ["keyspace, tables, consistency"], "pink"),
    route("repository", "reactiveOps", "M295 309 L295 380 L505 380 L505 534 L480 534", "teal"),
    route("repository", "asyncOps", "M455 272 L500 272 L500 360 L720 360 L720 485", "amber"),
    route("admin", "schema", "M715 309 L715 335 L930 335 L930 790 L800 790 L800 820", "purple"),
    route("domain", "sessionDsl", "M1130 309 L1130 485", "pink"),
    route("reactiveOps", "springOps", "M305 583 L305 705 L500 705 L500 861 L465 861", "teal"),
    route("asyncOps", "schema", "M720 583 L720 820", "amber", false, { x: 835, y: 695, text: "await bridge", width: 112 }),
    route("sessionDsl", "session", "M1130 583 L1130 815", "pink"),
    route("schema", "session", "M870 861 L970 861", "purple"),
    route("springOps", "driver", "M300 902 L300 985 L505 985 L505 1085", "green"),
    route("session", "driver", "M1135 907 L1135 985 L650 985 L650 1085", "blue"),
    route("driver", "cluster", "M665 1118 L805 1118", "amber"),
  ].join("\n");
  write("spring-boot-cassandra-diagram-02", base(1440, 1280, "Cassandra Data Access Layer", "The module is a layered adapter map, not a serial chain: repository code chooses operation/session helpers that converge on the driver and cluster.", body), {
    nodes: ["repository", "admin", "domain", "reactiveOps", "asyncOps", "sessionDsl", "springOps", "session", "schema", "driver", "cluster"],
    edges: [["repository", "reactiveOps"], ["repository", "asyncOps"], ["admin", "schema"], ["domain", "sessionDsl"], ["reactiveOps", "springOps"], ["asyncOps", "schema"], ["sessionDsl", "session"], ["schema", "session"], ["springOps", "driver"], ["session", "driver"], ["driver", "cluster"]],
  });
}

function renderNettySmartEncodingFlow() {
  const body = [
    panel(82, 160, 1296, 720, "Range-based encoding decisions"),
    card("value", 480, 232, 460, 78, "Integer value", ["caller writes ShortSmart / IntSmart / USmart"], "blue"),
    diamond("signedByte", 505, 352, 410, 120, "Signed short-smart byte range?", ["-64..63"], "teal"),
    card("oneByte", 990, 350, 280, 78, "Write 1 byte", ["value + 64"], "green"),
    diamond("signedShort", 505, 527, 410, 120, "Signed short-smart range?", ["-16384..16383"], "amber"),
    card("twoByte", 990, 547, 280, 78, "Write 2 bytes", ["value + 49152 marker"], "amber"),
    diamond("signedInt", 505, 702, 410, 120, "Int-smart range?", ["-1073741824..1073741823"], "purple"),
    card("fourByte", 990, 722, 280, 78, "Write 4 bytes", ["Int.MIN marker bit"], "purple"),
    card("unsigned", 140, 520, 270, 116, "USmart thresholds", ["0..127 -> 1 byte", "0..32767 -> 2 bytes", "else -> 4 bytes"], "pink"),
    route("value", "signedByte", "M710 310 L710 352", "blue"),
    route("signedByte", "oneByte", "M915 412 L990 412", "green", false, { x: 952, y: 370, text: "yes", width: 52 }),
    route("signedByte", "signedShort", "M710 472 L710 527", "teal", false, { x: 744, y: 500, text: "no", width: 48 }),
    route("signedShort", "twoByte", "M915 587 L990 587", "amber", false, { x: 952, y: 545, text: "yes", width: 52 }),
    route("signedShort", "signedInt", "M710 647 L710 702", "amber", false, { x: 744, y: 675, text: "no", width: 48 }),
    route("signedInt", "fourByte", "M915 762 L990 762", "purple", false, { x: 952, y: 720, text: "yes", width: 52 }),
    route("value", "unsigned", "M480 271 L275 271 L275 520", "pink", true, { x: 370, y: 306, text: "unsigned path", width: 122 }),
  ].join("\n");
  write("io-netty-diagram-02", base(1460, 960, "Smart Encoding Data Flow", "Smart encoding is a range decision tree; the card positions keep yes/no branches short and visible.", body, "decision-flow"), {
    nodes: ["value", "signedByte", "oneByte", "signedShort", "twoByte", "signedInt", "fourByte", "unsigned"],
    edges: [["value", "signedByte"], ["signedByte", "oneByte"], ["signedByte", "signedShort"], ["signedShort", "twoByte"], ["signedShort", "signedInt"], ["signedInt", "fourByte"], ["value", "unsigned"]],
  });
}

function renderNettyByteBufProcessingFlow() {
  const body = [
    panel(82, 160, 1276, 160, "Input"),
    panel(82, 395, 1276, 240, "ByteBuf extension families"),
    panel(82, 710, 1276, 190, "Output"),
    card("source", 135, 215, 290, 74, "ByteBuf input", ["network frame or binary payload"], "blue"),
    card("cursor", 560, 210, 330, 84, "Reader / writer index", ["sequential and indexed access"], "green"),
    card("target", 1010, 215, 270, 74, "ByteBuf output", ["encoded response payload"], "purple"),
    card("endian", 150, 470, 300, 92, "Endian / offset helpers", ["LE, ME, IME variants", "add/subtract transforms"], "teal"),
    card("smart", 535, 470, 300, 92, "Smart / VarInt helpers", ["compact numeric encodings", "range markers"], "amber"),
    card("strings", 920, 470, 310, 92, "String / byte-array helpers", ["null-terminated strings", "reversed byte arrays"], "pink"),
    card("domain", 230, 790, 300, 70, "Domain values", ["numbers, strings, payload blocks"], "olive"),
    card("writeBack", 790, 790, 330, 70, "Write helpers", ["write* mirrors read* families"], "green"),
    route("source", "cursor", "M425 252 L560 252", "blue"),
    route("cursor", "target", "M890 252 L1010 252", "green"),
    route("cursor", "endian", "M650 294 L650 350 L300 350 L300 470", "teal"),
    route("cursor", "smart", "M725 294 L725 470", "amber"),
    route("cursor", "strings", "M800 294 L800 350 L1075 350 L1075 470", "pink"),
    route("endian", "domain", "M300 562 L300 790", "teal"),
    route("smart", "domain", "M685 562 L685 690 L380 690 L380 790", "amber"),
    route("strings", "writeBack", "M1075 562 L1075 690 L955 690 L955 790", "pink"),
    route("domain", "writeBack", "M530 825 L790 825", "olive"),
    route("writeBack", "target", "M1120 825 L1305 825 L1305 252 L1280 252", "green"),
  ].join("\n");
  write("io-netty-diagram-03", base(1440, 985, "ByteBuf Processing Flow", "The diagram now matches the module source: ByteBuf helpers transform binary data through read, decode, encode, and write paths.", body), {
    nodes: ["source", "cursor", "target", "endian", "smart", "strings", "domain", "writeBack"],
    edges: [["source", "cursor"], ["cursor", "target"], ["cursor", "endian"], ["cursor", "smart"], ["cursor", "strings"], ["endian", "domain"], ["smart", "domain"], ["strings", "writeBack"], ["domain", "writeBack"], ["writeBack", "target"]],
  });
}

function renderProtobufConversionFlow() {
  const body = [
    panel(82, 160, 1300, 160, "Domain types"),
    panel(82, 420, 1300, 210, "Protobuf types"),
    panel(82, 720, 1300, 165, "Wire format"),
    card("duration", 130, 215, 250, 72, "java.time.Duration", ["parse / format support"], "blue"),
    card("dateTime", 385, 215, 360, 72, "Date/time types", ["LocalDate / LocalTime / LocalDateTime"], "green"),
    card("money", 750, 215, 250, 72, "JavaMoney", ["currency + units/nanos"], "amber"),
    card("message", 1035, 215, 280, 72, "Message", ["generated protobuf message"], "purple"),
    card("protoDuration", 130, 500, 250, 78, "Proto Duration", ["seconds + nanos"], "blue"),
    card("protoDate", 430, 500, 270, 78, "Proto Date/Time", ["date, time, datetime"], "green"),
    card("protoMoney", 750, 500, 250, 78, "Proto Money", ["currencyCode + units"], "amber"),
    card("any", 1035, 500, 280, 78, "Proto Any", ["pack / unpack typed messages"], "purple"),
    card("bytes", 560, 775, 310, 72, "ByteArray payload", ["toByteArray / parseFrom"], "pink"),
    route("duration", "protoDuration", "M255 287 L255 360 L330 360 L330 500", "blue", false, { x: 420, y: 376, text: "toProto / toJava", width: 130 }),
    route("dateTime", "protoDate", "M565 287 L565 500", "green", false, { x: 646, y: 376, text: "toProto / toJava", width: 130 }),
    route("money", "protoMoney", "M875 287 L875 500", "amber", false, { x: 956, y: 376, text: "toProto / toJava", width: 130 }),
    route("message", "any", "M1175 287 L1175 500", "purple", false, { x: 1238, y: 376, text: "pack", width: 70 }),
    route("any", "bytes", "M1175 578 L1175 675 L715 675 L715 775", "purple", false, { x: 950, y: 650, text: "serialize", width: 100 }),
    route("bytes", "any", "M870 810 L1260 810 L1260 578", "pink", true, { x: 1070, y: 785, text: "parse + unpack", width: 122 }),
  ].join("\n");
  write("io-protobuf-diagram-02", base(1464, 965, "Protobuf Type Conversion Flow", "Every conversion path is explicit: domain types map to protobuf types, and Message values cross the wire through Any and ByteArray.", body), {
    nodes: ["duration", "dateTime", "money", "message", "protoDuration", "protoDate", "protoMoney", "any", "bytes"],
    edges: [["duration", "protoDuration"], ["dateTime", "protoDate"], ["money", "protoMoney"], ["message", "any"], ["any", "bytes"], ["bytes", "any"]],
  });
}

function renderVertxModuleCapabilityArchitecture() {
  const body = [
    panel(82, 160, 1276, 150, "Application boundary"),
    panel(82, 385, 1276, 230, "Module capabilities"),
    panel(82, 690, 1276, 140, "Vert.x runtime"),
    card("app", 135, 215, 330, 74, "Vert.x application", ["routes, verticles, async services"], "blue"),
    card("config", 570, 215, 300, 74, "Application config", ["owns pools, registry, policies"], "purple"),
    card("core", 115, 455, 280, 92, "Core coroutine support", ["dispatcherOf", "await helpers", "Future bridges"], "teal"),
    card("web", 430, 455, 260, 92, "Web helpers", ["route handlers", "request/response helpers"], "green"),
    card("sql", 725, 455, 280, 92, "SQL / MyBatis support", ["SqlClient extensions", "row/result mapping"], "amber"),
    card("resilience", 1040, 455, 260, 92, "Resilience4j support", ["decorate Future suppliers", "circuit/retry policies"], "pink"),
    card("vertx", 170, 735, 290, 62, "Vert.x core", ["event loop + Future"], "blue"),
    card("sqlRuntime", 575, 735, 290, 62, "SqlClient / Pool", ["Postgres, MySQL, JDBC"], "amber"),
    card("resRuntime", 980, 735, 290, 62, "Resilience runtime", ["CircuitBreaker, Retry"], "pink"),
    route("app", "core", "M300 289 L300 455", "teal"),
    route("app", "web", "M465 252 L560 252 L560 455", "green"),
    route("config", "sql", "M720 289 L720 350 L865 350 L865 455", "amber"),
    route("config", "resilience", "M870 252 L1170 252 L1170 455", "pink"),
    route("core", "vertx", "M255 547 L255 735", "teal"),
    route("web", "vertx", "M560 547 L560 645 L315 645 L315 735", "green"),
    route("sql", "sqlRuntime", "M865 547 L865 645 L720 645 L720 735", "amber"),
    route("resilience", "resRuntime", "M1170 547 L1170 735", "pink"),
  ].join("\n");
  write("io-vertx-diagram-01", base(1440, 910, "Vert.x Module Capability Architecture", "The diagram names what the module provides instead of pretending to show vague dependencies.", body), {
    nodes: ["app", "config", "core", "web", "sql", "resilience", "vertx", "sqlRuntime", "resRuntime"],
    edges: [["app", "core"], ["app", "web"], ["config", "sql"], ["config", "resilience"], ["core", "vertx"], ["web", "vertx"], ["sql", "sqlRuntime"], ["resilience", "resRuntime"]],
  });
}

function renderKtorCoreArchitecture() {
  const body = [
    panel(82, 160, 1276, 150, "Application install"),
    panel(82, 385, 1276, 215, "bluetape4k Ktor core"),
    panel(82, 675, 1276, 145, "Ktor runtime"),
    card("app", 130, 215, 300, 74, "Application.module", ["installBluetape4kKtorCore"], "blue"),
    card("config", 560, 215, 300, 74, "Core config", ["JSON, errors, health readiness"], "purple"),
    card("routes", 980, 215, 300, 74, "Application routes", ["use request parameter helpers"], "green"),
    card("json", 135, 455, 280, 86, "Content negotiation", ["Jackson JSON defaults"], "teal"),
    card("errors", 465, 455, 280, 86, "StatusPages", ["standard API error response"], "pink"),
    card("health", 795, 455, 280, 86, "Health routes", ["/health, /ready"], "amber"),
    card("params", 1095, 455, 230, 86, "Request helpers", ["typed path/query"], "green"),
    card("ktor", 250, 720, 310, 62, "Ktor plugin pipeline", ["plugins execute around routes"], "blue"),
    card("response", 850, 720, 310, 62, "HTTP response", ["JSON body or API error"], "purple"),
    route("app", "config", "M430 252 L560 252", "blue"),
    route("config", "routes", "M860 252 L980 252", "purple"),
    route("config", "json", "M640 289 L275 455", "teal"),
    route("config", "errors", "M710 289 L605 455", "pink"),
    route("config", "health", "M785 289 L935 455", "amber"),
    route("routes", "params", "M1130 289 L1130 455", "green"),
    route("json", "ktor", "M275 541 L275 720", "teal"),
    route("errors", "ktor", "M605 541 L605 650 L405 650 L405 720", "pink"),
    route("health", "response", "M935 541 L935 720", "amber"),
    route("params", "response", "M1210 541 L1210 650 L1005 650 L1005 720", "green"),
    route("ktor", "response", "M560 751 L850 751", "blue"),
  ].join("\n");
  write("ktor-core-architecture-01", base(1440, 900, "Ktor Core Layered Architecture", "The core module installs a small baseline: JSON, standard errors, health routes, and request helpers around normal Ktor routing.", body), {
    nodes: ["app", "config", "routes", "json", "errors", "health", "params", "ktor", "response"],
    edges: [["app", "config"], ["config", "routes"], ["config", "json"], ["config", "errors"], ["config", "health"], ["routes", "params"], ["json", "ktor"], ["errors", "ktor"], ["health", "response"], ["params", "response"], ["ktor", "response"]],
  });
}

function renderKtorObservabilityArchitecture() {
  const body = [
    panel(82, 160, 1276, 145, "Application-owned telemetry inputs"),
    panel(82, 375, 1276, 230, "Ktor observability module"),
    panel(82, 680, 1276, 145, "Backends"),
    card("app", 115, 215, 340, 70, "Application config", ["service name, registry, OTel toggle"], "blue"),
    card("registry", 570, 215, 300, 70, "MeterRegistry / tracer", ["owned by application"], "purple"),
    card("installer", 1000, 215, 270, 70, "installObservability", ["applies selected plugins"], "green"),
    card("callId", 90, 445, 330, 88, "CallId", ["correlation id extraction/generation"], "teal"),
    card("logging", 430, 445, 260, 88, "CallLogging + MDC", ["structured request logging"], "amber"),
    card("metrics", 735, 445, 260, 88, "MicrometerMetrics", ["HTTP server metrics"], "pink"),
    card("tracing", 1020, 445, 300, 88, "KtorServerTelemetry", ["optional OpenTelemetry traces"], "purple"),
    card("prom", 265, 720, 300, 66, "Prometheus scrape route", ["/metrics"], "green"),
    card("otel", 875, 720, 300, 66, "OpenTelemetry exporter", ["configured outside module"], "purple"),
    route("app", "installer", "M285 215 L285 190 L1135 190 L1135 215", "blue"),
    route("registry", "metrics", "M720 285 L865 445", "pink"),
    route("installer", "callId", "M1025 285 L255 445", "teal"),
    route("installer", "logging", "M1095 285 L560 445", "amber"),
    route("installer", "metrics", "M1165 285 L865 445", "pink"),
    route("installer", "tracing", "M1235 285 L1170 445", "purple"),
    route("metrics", "prom", "M865 533 L865 630 L415 630 L415 720", "green"),
    route("tracing", "otel", "M1170 533 L1170 635 L1025 635 L1025 720", "purple"),
  ].join("\n");
  write("ktor-observability-component-01", base(1440, 905, "Ktor Observability Components", "Observability stays explicit: application-owned registries feed Ktor plugins and export through Prometheus or OpenTelemetry backends.", body), {
    nodes: ["app", "registry", "installer", "callId", "logging", "metrics", "tracing", "prom", "otel"],
    edges: [["app", "installer"], ["registry", "metrics"], ["installer", "callId"], ["installer", "logging"], ["installer", "metrics"], ["installer", "tracing"], ["metrics", "prom"], ["tracing", "otel"]],
  });
}

function renderKtorTestingSequence() {
  const y0 = 170;
  const body = [
    participant("test", 100, y0, 200, "Test", "blue", 790),
    participant("builder", 330, y0, 280, "ApplicationTestBuilder", "green", 790),
    participant("app", 660, y0, 210, "Ktor app", "purple", 790),
    participant("client", 935, y0, 210, "JSON client", "amber", 790),
    participant("assertions", 1145, y0, 300, "Assertions / MockEngine", "pink", 790),
    seqMessage("m1", "test", "builder", 200, 470, 310, "testApplication", 1, "blue"),
    seqMessage("m2", "builder", "app", 470, 765, 395, "install core module", 2, "green"),
    seqMessage("m3", "builder", "client", 470, 1040, 480, "create JSON client", 3, "amber"),
    seqMessage("m4", "client", "app", 1040, 765, 565, "execute request", 4, "purple"),
    seqMessage("m5", "app", "assertions", 765, 1295, 650, "decode body / assert status", 5, "pink"),
    seqMessage("m6", "assertions", "test", 1295, 200, 735, "return verified response", 6, "green", true),
  ].join("\n");
  write("ktor-testing-sequence-01", base(1500, 880, "Ktor Testing Helper Sequence", "The sequence shows the real test path: install core defaults, create a JSON client, execute, then assert or use MockEngine helpers.", body, "sequence"), {
    nodes: ["test", "builder", "app", "client", "assertions"],
    edges: [["test", "builder"], ["builder", "app"], ["builder", "client"], ["client", "app"], ["app", "assertions"], ["assertions", "test"]],
  });
}

function renderKtorIdGeneratorDemoArchitecture() {
  const body = [
    panel(82, 160, 1336, 190, "HTTP entry"),
    panel(82, 430, 1336, 220, "Request policy"),
    panel(82, 730, 1336, 330, "Generation outcomes"),
    card("client", 165, 235, 320, 78, "HTTP client", ["/ids, /idgen, /generators"], "blue"),
    card("routes", 590, 230, 340, 88, "idGeneratorRoutes", ["single-id and batch endpoints"], "green"),
    card("support", 1035, 235, 300, 78, "Ktor support", ["JSON, StatusPages, telemetry"], "purple"),
    card("validation", 330, 520, 360, 88, "Request validation", ["generator type exists", "batch size is 1..100"], "amber"),
    card("registry", 810, 520, 360, 88, "IdGeneratorRegistry", ["maps request type", "selects generator implementation"], "teal"),
    card("errors", 175, 815, 390, 90, "API errors", ["unknown generator type", "invalid batch size"], "pink"),
    card("generators", 835, 815, 390, 90, "Generator families", ["UUID, ULID, KSUID", "Snowflake and Flake"], "green"),
    card("response", 505, 940, 390, 78, "HTTP response payload", ["generated ids or structured error"], "blue"),
    route("client", "routes", "M485 274 L590 274", "blue"),
    route("routes", "support", "M930 274 L1035 274", "purple", true),
    route("routes", "validation", "M760 318 L760 385 L510 385 L510 520", "amber"),
    route("validation", "registry", "M690 564 L810 564", "teal"),
    route("validation", "errors", "M510 608 L510 710 L370 710 L370 815", "pink", true),
    route("registry", "generators", "M990 608 L990 815", "green"),
    route("errors", "response", "M370 905 L370 925 L610 925 L610 940", "pink", true),
    route("generators", "response", "M1030 905 L1030 925 L790 925 L790 940", "green"),
  ].join("\n");
  write("examples-ktor-idgenerator-ktor-demo-diagram-01", base(1500, 1140, "Ktor IdGenerator Demo Request Flow", "Routes validate input, resolve the requested generator, then return generated IDs or structured API errors; Ktor support is installed beside the request path.", body), {
    nodes: ["client", "routes", "support", "validation", "registry", "generators", "errors", "response"],
    edges: [["client", "routes"], ["routes", "support"], ["routes", "validation"], ["validation", "registry"], ["registry", "generators"], ["validation", "errors"], ["generators", "response"], ["errors", "response"]],
  });
}

function renderKtorObservabilityDemoArchitecture() {
  const body = [
    panel(82, 160, 1276, 145, "Ktor setup"),
    panel(82, 375, 1276, 230, "Demo domain flow"),
    panel(82, 680, 1276, 185, "Telemetry outputs"),
    card("module", 130, 215, 300, 70, "observabilityKtorModule", ["installs core + observability"], "blue"),
    card("routes", 560, 215, 300, 70, "observabilityRoutes", ["order event + metrics routes"], "green"),
    card("registries", 990, 215, 300, 70, "Telemetry registries", ["PrometheusMeterRegistry", "ObservationRegistry / OTel"], "purple"),
    card("orderRoute", 140, 445, 300, 92, "Order event route", ["POST /orders/{id}/events", "path parameter extraction"], "teal"),
    card("service", 550, 445, 320, 92, "OrderEventTelemetryService", ["publish + consume observations"], "amber"),
    card("eventTelemetry", 980, 445, 310, 92, "EventTelemetry", ["event name, outcome, latency tags"], "pink"),
    card("metrics", 250, 755, 300, 66, "Prometheus /metrics", ["scrape application metrics"], "green"),
    card("traces", 880, 755, 300, 66, "OpenTelemetry traces", ["optional OTLP export"], "purple"),
    route("module", "routes", "M430 250 L560 250", "blue"),
    route("routes", "registries", "M860 250 L990 250", "green"),
    route("routes", "orderRoute", "M650 285 L650 350 L360 350 L360 445", "teal"),
    route("orderRoute", "service", "M440 491 L550 491", "amber"),
    route("service", "eventTelemetry", "M870 491 L980 491", "pink"),
    route("routes", "metrics", "M770 285 L770 330 L505 330 L505 720 L400 720 L400 755", "green"),
    route("eventTelemetry", "traces", "M1135 537 L1135 635 L1030 635 L1030 755", "purple"),
    route("registries", "eventTelemetry", "M1140 285 L1140 445", "purple"),
  ].join("\n");
  write("examples-ktor-observability-ktor-demo-architecture-01", base(1440, 960, "Ktor Observability Demo Architecture", "The demo owns telemetry registries, installs Ktor observability, and routes order events into EventTelemetry before exporting metrics or traces.", body), {
    nodes: ["module", "routes", "registries", "orderRoute", "service", "eventTelemetry", "metrics", "traces"],
    edges: [["module", "routes"], ["routes", "registries"], ["routes", "orderRoute"], ["orderRoute", "service"], ["service", "eventTelemetry"], ["routes", "metrics"], ["eventTelemetry", "traces"], ["registries", "eventTelemetry"]],
  });
}

function renderKtorObservabilityDemoSequence() {
  const y0 = 165;
  const body = [
    participant("client", 90, y0, 200, "Client", "blue", 790),
    participant("route", 345, y0, 230, "Ktor route", "green", 790),
    participant("service", 630, y0, 260, "Telemetry service", "amber", 790),
    participant("eventTelemetry", 955, y0, 250, "EventTelemetry", "pink", 790),
    participant("backend", 1260, y0, 210, "Metrics / Traces", "purple", 790),
    seqMessage("kobs1", "client", "route", 190, 460, 305, "POST order event", 1, "blue"),
    seqMessage("kobs2", "route", "service", 460, 760, 390, "extract id + payload", 2, "green"),
    seqMessage("kobs3", "service", "eventTelemetry", 760, 1080, 475, "publish observation", 3, "amber"),
    seqMessage("kobs4", "eventTelemetry", "backend", 1080, 1365, 560, "record metric / span", 4, "purple"),
    seqMessage("kobs5", "service", "route", 760, 460, 645, "domain response", 5, "green", true),
    seqMessage("kobs6", "client", "backend", 190, 1365, 730, "GET /metrics scrape", 6, "pink", true),
  ].join("\n");
  write("examples-ktor-observability-ktor-demo-sequence-01", base(1560, 885, "Ktor Observability Demo Sequence", "The sequence keeps request handling and the metrics scrape path explicit with numbered arrows.", body, "sequence"), {
    nodes: ["client", "route", "service", "eventTelemetry", "backend"],
    edges: [["client", "route"], ["route", "service"], ["service", "eventTelemetry"], ["eventTelemetry", "backend"], ["service", "route"], ["client", "backend"]],
  });
}

function renderHibernateLettuceComponentMap() {
  const body = [
    panel(82, 160, 1276, 150, "Spring Boot auto-configuration"),
    panel(82, 385, 1276, 220, "Hibernate near-cache integration"),
    panel(82, 680, 1276, 150, "Observability surfaces"),
    card("imports", 110, 215, 320, 74, "AutoConfiguration imports", ["three conditional configs"], "blue"),
    card("props", 520, 210, 400, 84, "LettuceNearCacheSpringProperties", ["redisUri, codec, TTL", "local cache and metrics flags"], "purple"),
    card("conditions", 1000, 215, 290, 74, "Conditions", ["classpath + enabled property"], "green"),
    card("hibernateConfig", 95, 455, 370, 96, "Hibernate auto-config", ["registers HibernatePropertiesCustomizer", "maps Spring properties to Hibernate keys"], "teal"),
    card("regionFactory", 520, 455, 380, 96, "LettuceNearCacheRegionFactory", ["Hibernate 2LC RegionFactory", "L1 Caffeine + L2 Redis"], "amber"),
    card("hibernate", 985, 455, 300, 96, "Hibernate SessionFactory", ["2nd-level cache enabled", "region metadata and statistics"], "pink"),
    card("metrics", 235, 720, 300, 70, "Micrometer binder", ["lettuce.nearcache.* gauges"], "green"),
    card("actuator", 870, 720, 300, 70, "Actuator endpoint", ["/actuator/nearcache"], "purple"),
    route("imports", "props", "M430 252 L520 252", "blue"),
    route("props", "conditions", "M920 252 L1000 252", "purple"),
    route("props", "hibernateConfig", "M660 294 L330 455", "teal"),
    route("conditions", "hibernateConfig", "M1140 289 L280 455", "green"),
    route("hibernateConfig", "regionFactory", "M465 503 L520 503", "teal"),
    route("regionFactory", "hibernate", "M900 503 L985 503", "amber"),
    route("regionFactory", "metrics", "M710 551 L710 640 L385 640 L385 720", "green"),
    route("hibernate", "actuator", "M1135 551 L1135 640 L1020 640 L1020 720", "purple"),
  ].join("\n");
  write("spring-boot-hibernate-lettuce-diagram-01", base(1440, 910, "Hibernate Lettuce Near-Cache Components", "The component map shows what Spring Boot registers and what remains Hibernate/cache runtime responsibility.", body), {
    nodes: ["imports", "props", "conditions", "hibernateConfig", "regionFactory", "hibernate", "metrics", "actuator"],
    edges: [["imports", "props"], ["props", "conditions"], ["props", "hibernateConfig"], ["conditions", "hibernateConfig"], ["hibernateConfig", "regionFactory"], ["regionFactory", "hibernate"], ["regionFactory", "metrics"], ["hibernate", "actuator"]],
  });
}

function renderHibernateLettuceActivationFlow() {
  const body = [
    panel(82, 160, 1216, 700, "Auto-configuration activation decisions"),
    card("start", 540, 205, 300, 72, "Application starts", ["Spring Boot reads imports"], "blue"),
    diamond("classpath", 495, 330, 390, 120, "Required classes present?", ["RegionFactory + EntityManagerFactory"], "purple"),
    diamond("enabled", 495, 505, 390, 120, "lettuce-near enabled?", ["matchIfMissing = true"], "green"),
    card("hibernate", 905, 525, 345, 82, "Register customizer", ["sets RegionFactory and 2LC properties"], "teal"),
    diamond("metricsClass", 495, 680, 390, 120, "MeterRegistry bean present?", ["metrics.enabled also true"], "amber"),
    card("metrics", 905, 700, 345, 82, "Register metrics binder", ["active regions and local size gauges"], "amber"),
    card("actuator", 125, 700, 310, 82, "Optional actuator endpoint", ["Endpoint + EMF on classpath"], "pink"),
    route("start", "classpath", "M690 277 L690 330", "blue"),
    route("classpath", "enabled", "M690 450 L690 505", "purple"),
    route("enabled", "hibernate", "M885 565 L905 565", "teal"),
    route("enabled", "metricsClass", "M690 625 L690 680", "green"),
    route("metricsClass", "metrics", "M885 740 L905 740", "amber"),
    route("metricsClass", "actuator", "M495 740 L435 740", "pink"),
    text(725, 478, "yes", "detail"),
    text(910, 546, "yes", "detail"),
    text(910, 721, "yes", "detail"),
    text(450, 704, "endpoint path", "detail"),
  ].join("\n");
  write("spring-boot-hibernate-lettuce-diagram-02", base(1380, 940, "Auto-Configuration Activation Flow", "The flow shows the actual Spring Boot conditions that decide Hibernate cache, metrics, and actuator registration.", body, "decision-flow"), {
    nodes: ["start", "classpath", "enabled", "hibernate", "metricsClass", "metrics", "actuator"],
    edges: [["start", "classpath"], ["classpath", "enabled"], ["enabled", "hibernate"], ["enabled", "metricsClass"], ["metricsClass", "metrics"], ["metricsClass", "actuator"]],
  });
}

function renderHibernateLettuceDemoRuntimeFlow() {
  const body = [
    panel(82, 160, 1276, 150, "REST API"),
    panel(82, 385, 1276, 235, "Hibernate second-level cache"),
    panel(82, 695, 1276, 170, "Storage and management"),
    card("client", 125, 215, 270, 74, "HTTP client", ["/api/products, /api/cache"], "blue"),
    card("productApi", 525, 210, 310, 84, "ProductController", ["CRUD through ProductRepository"], "green"),
    card("cacheApi", 980, 210, 290, 84, "CacheController", ["stats and L1-only eviction"], "purple"),
    card("jpa", 140, 455, 300, 96, "Spring Data JPA", ["ProductRepository", "@Cacheable Product region"], "teal"),
    card("hibernate", 535, 455, 330, 96, "Hibernate SessionFactory", ["2nd-level cache lookup", "RegionFactory service"], "amber"),
    card("nearCache", 970, 455, 310, 96, "Lettuce Near Cache", ["L1 Caffeine", "L2 Redis with RESP3 tracking"], "pink"),
    card("h2", 200, 760, 300, 70, "H2 database", ["source of truth for demo"], "olive"),
    card("redis", 540, 760, 360, 70, "Redis L2", ["shared cached values and invalidation"], "pink"),
    card("stats", 915, 760, 350, 70, "Metrics / actuator stats", ["region size, hits, misses"], "purple"),
    route("client", "productApi", "M395 252 L525 252", "blue"),
    route("client", "cacheApi", "M260 289 L260 340 L1125 340 L1125 294", "purple", true),
    route("productApi", "jpa", "M680 294 L680 350 L290 350 L290 455", "green"),
    route("jpa", "hibernate", "M440 503 L535 503", "teal"),
    route("hibernate", "nearCache", "M865 503 L970 503", "amber"),
    route("hibernate", "h2", "M700 551 L700 640 L350 640 L350 760", "olive", true, { x: 520, y: 617, text: "cache miss", width: 102 }),
    route("nearCache", "redis", "M1085 551 L720 760", "pink"),
    route("cacheApi", "stats", "M1270 252 L1325 252 L1325 690 L1090 690 L1090 760", "purple"),
    route("nearCache", "stats", "M1185 551 L1090 760", "purple"),
  ].join("\n");
  write("spring-boot-hibernate-lettuce-demo-diagram-02", base(1440, 950, "Hibernate Lettuce Demo Runtime Flow", "The demo flow separates Product CRUD, cache-management endpoints, Hibernate 2LC, L1/L2 cache tiers, and backing storage.", body), {
    nodes: ["client", "productApi", "cacheApi", "jpa", "hibernate", "nearCache", "h2", "redis", "stats"],
    edges: [["client", "productApi"], ["client", "cacheApi"], ["productApi", "jpa"], ["jpa", "hibernate"], ["hibernate", "nearCache"], ["hibernate", "h2"], ["nearCache", "redis"], ["cacheApi", "stats"], ["nearCache", "stats"]],
  });
}

function renderMongoClassStructure() {
  const body = [
    panel(82, 160, 1276, 155, "Spring Data receiver"),
    panel(82, 390, 1276, 225, "Coroutine and DSL extension groups"),
    panel(82, 690, 1276, 155, "MongoDB query objects"),
    card("ops", 505, 215, 430, 78, "ReactiveMongoOperations", ["source receiver for coroutine extension functions"], "blue"),
    card("coroutines", 505, 455, 430, 96, "Reactive Mongo operations extensions", ["Flux -> Flow", "Mono -> suspend / nullable suspend"], "teal"),
    card("criteria", 145, 455, 280, 86, "Criteria DSL", ["criteria(), eq/gt/in", "andWith/orWith"], "green"),
    card("query", 980, 455, 280, 86, "Query extensions", ["queryOf, sort, paginate", "limit/skip helpers"], "amber"),
    card("update", 145, 745, 280, 70, "Update DSL", ["setTo/incBy/push/pull"], "pink"),
    card("springTypes", 505, 745, 430, 70, "Spring Data types", ["Criteria, Query, Update"], "purple"),
    card("mongo", 980, 745, 280, 70, "MongoDB reactive driver", ["Flux/Mono command results"], "olive"),
    route("ops", "coroutines", "M720 293 L720 455", "blue"),
    route("criteria", "springTypes", "M425 498 L470 498 L470 690 L640 690 L640 745", "green"),
    route("query", "springTypes", "M1120 541 L1120 635 L865 635 L865 745", "amber"),
    route("update", "springTypes", "M425 780 L505 780", "pink"),
    route("coroutines", "mongo", "M880 551 L880 670 L1120 670 L1120 745", "teal"),
  ].join("\n");
  write("spring-boot-mongodb-diagram-01", base(1440, 925, "MongoDB Coroutine Extension Structure", "ReactiveMongoOperations stays as the receiver; coroutine extensions move below it and query/update DSL groups stay beside the Spring Data types they build.", body), {
    nodes: ["ops", "coroutines", "criteria", "query", "update", "springTypes", "mongo"],
    edges: [["ops", "coroutines"], ["criteria", "springTypes"], ["query", "springTypes"], ["update", "springTypes"], ["coroutines", "mongo"]],
  });
}

function renderMongoCoroutineFlow() {
  const body = [
    panel(82, 160, 1276, 145, "Caller intent"),
    panel(82, 375, 1276, 230, "Extension conversion"),
    panel(82, 680, 1276, 145, "Results"),
    card("caller", 120, 215, 330, 70, "Repository/service code", ["chooses query or entity operation"], "blue"),
    card("query", 570, 215, 300, 70, "Query / Aggregation", ["Spring Data Mongo criteria"], "purple"),
    card("ops", 960, 215, 360, 70, "ReactiveMongoOperations", ["returns Flux<T>, Mono<T>, Mono<Result>"], "green"),
    card("many", 135, 445, 290, 88, "Many-result extensions", ["findAsFlow", "findAllAsFlow", "aggregateAsFlow"], "teal"),
    card("single", 505, 445, 310, 88, "Single-result extensions", ["findOneSuspending", "findByIdOrNullSuspending"], "amber"),
    card("write", 895, 445, 330, 88, "Write extensions", ["insert/save/update/remove", "await UpdateResult/DeleteResult"], "pink"),
    card("flow", 245, 720, 280, 66, "Flow<T>", ["streamed documents"], "teal"),
    card("suspend", 620, 720, 280, 66, "suspend result", ["T, T?, Long, Boolean"], "amber"),
    card("metadata", 995, 720, 280, 66, "Write metadata", ["UpdateResult / DeleteResult"], "pink"),
    route("caller", "query", "M450 250 L570 250", "blue"),
    route("query", "ops", "M870 250 L960 250", "purple"),
    route("ops", "many", "M1065 285 L280 445", "teal"),
    route("ops", "single", "M1140 285 L660 445", "amber"),
    route("ops", "write", "M1215 285 L1060 445", "pink"),
    route("many", "flow", "M280 533 L280 720", "teal"),
    route("single", "suspend", "M660 533 L660 720", "amber"),
    route("write", "metadata", "M1060 533 L1060 720", "pink"),
  ].join("\n");
  write("spring-boot-mongodb-diagram-02", base(1440, 905, "ReactiveMongoOperations Coroutine Flow", "The flow names the actual conversion boundary: Reactive Mongo publishers become Flow, suspend values, or write metadata.", body), {
    nodes: ["caller", "query", "ops", "many", "single", "write", "flow", "suspend", "metadata"],
    edges: [["caller", "query"], ["query", "ops"], ["ops", "many"], ["ops", "single"], ["ops", "write"], ["many", "flow"], ["single", "suspend"], ["write", "metadata"]],
  });
}

function renderMongoDslFlow() {
  const body = [
    panel(82, 160, 1276, 160, "DSL inputs"),
    panel(82, 395, 1276, 220, "Spring Data query objects"),
    panel(82, 690, 1276, 140, "Mongo operation"),
    card("fields", 145, 215, 280, 74, "Field strings", ["\"age\".criteria()", "\"name\" setTo value"], "blue"),
    card("criteriaDsl", 535, 210, 320, 84, "Criteria infix DSL", ["eq, gt, inValues", "andWith / orWith"], "green"),
    card("updateDsl", 975, 210, 280, 84, "Update DSL", ["setTo, incBy, pushValue", "andSet / andInc"], "pink"),
    card("criteria", 170, 455, 290, 84, "Criteria", ["predicate tree"], "green"),
    card("query", 575, 455, 290, 84, "Query", ["queryOf + sort + paginate"], "amber"),
    card("update", 980, 455, 290, 84, "Update", ["Mongo update document"], "pink"),
    card("mongoOps", 535, 735, 370, 62, "ReactiveMongoOperations", ["find/update/remove executes objects"], "purple"),
    route("fields", "criteriaDsl", "M425 252 L535 252", "blue"),
    route("fields", "updateDsl", "M425 289 L975 252", "pink", true),
    route("criteriaDsl", "criteria", "M695 294 L695 355 L315 355 L315 455", "green"),
    route("criteria", "query", "M460 497 L575 497", "amber"),
    route("updateDsl", "update", "M1115 294 L1115 455", "pink"),
    route("query", "mongoOps", "M720 539 L720 735", "amber"),
    route("update", "mongoOps", "M1125 539 L1125 635 L760 635 L760 735", "pink"),
  ].join("\n");
  write("spring-boot-mongodb-diagram-03", base(1440, 910, "Criteria / Query / Update DSL Flow", "The DSL flow shows how field-level helpers become Criteria, Query, and Update objects before execution.", body), {
    nodes: ["fields", "criteriaDsl", "updateDsl", "criteria", "query", "update", "mongoOps"],
    edges: [["fields", "criteriaDsl"], ["fields", "updateDsl"], ["criteriaDsl", "criteria"], ["criteria", "query"], ["updateDsl", "update"], ["query", "mongoOps"], ["update", "mongoOps"]],
  });
}

function renderR2dbcCoroutineFlow() {
  const body = [
    panel(82, 160, 1276, 150, "Caller and receiver"),
    panel(82, 385, 1276, 230, "Coroutine CRUD extension groups"),
    panel(82, 690, 1276, 140, "R2DBC runtime"),
    card("service", 145, 215, 290, 74, "Repository/service code", ["suspend and Flow APIs"], "blue"),
    card("ops", 560, 210, 320, 84, "R2dbcEntityOperations", ["entity template receiver"], "green"),
    card("specs", 1000, 215, 280, 74, "Reactive CRUD specs", ["select/insert/update/delete"], "purple"),
    card("select", 120, 455, 280, 92, "Select extensions", ["selectAllSuspending", "selectOneOrNullSuspending"], "teal"),
    card("write", 435, 455, 280, 92, "Insert / update", ["insertSuspending", "updateSuspending"], "amber"),
    card("delete", 750, 455, 280, 92, "Delete extensions", ["deleteSuspending", "deleteAllSuspending"], "pink"),
    card("count", 1065, 455, 220, 92, "Count / exists", ["Long or Boolean"], "olive"),
    card("databaseClient", 300, 735, 320, 62, "DatabaseClient / driver", ["Publisher results"], "purple"),
    card("flowSuspend", 805, 735, 320, 62, "Flow<T> / suspend values", ["converted with coroutine bridges"], "blue"),
    route("service", "ops", "M435 252 L560 252", "blue"),
    route("ops", "specs", "M880 252 L1000 252", "green"),
    route("ops", "select", "M640 294 L260 455", "teal"),
    route("specs", "write", "M1065 289 L575 455", "amber"),
    route("specs", "delete", "M1140 289 L890 455", "pink"),
    route("specs", "count", "M1215 289 L1175 455", "olive"),
    route("select", "databaseClient", "M260 547 L260 660 L410 660 L410 735", "teal"),
    route("write", "databaseClient", "M575 547 L575 660 L510 660 L510 735", "amber"),
    route("delete", "flowSuspend", "M890 547 L890 735", "pink"),
    route("count", "flowSuspend", "M1175 547 L1175 660 L965 660 L965 735", "olive"),
    route("databaseClient", "flowSuspend", "M620 766 L805 766", "purple"),
  ].join("\n");
  write("spring-boot-r2dbc-diagram-02", base(1440, 910, "R2DBC + Coroutines Data Flow", "Coroutine helpers are grouped by CRUD intent and show where reactive driver publishers become Flow or suspend values.", body), {
    nodes: ["service", "ops", "specs", "select", "write", "delete", "count", "databaseClient", "flowSuspend"],
    edges: [["service", "ops"], ["ops", "specs"], ["ops", "select"], ["specs", "write"], ["specs", "delete"], ["specs", "count"], ["select", "databaseClient"], ["write", "databaseClient"], ["delete", "flowSuspend"], ["count", "flowSuspend"], ["databaseClient", "flowSuspend"]],
  });
}

function renderRedisSerializationFlow() {
  const body = [
    panel(82, 160, 1276, 145, "Template configuration"),
    panel(82, 375, 1276, 230, "Serialization choices"),
    panel(82, 680, 1276, 145, "Redis wire"),
    card("config", 130, 215, 300, 70, "Spring configuration", ["ReactiveRedisTemplate bean"], "blue"),
    card("context", 515, 210, 410, 80, "RedisSerializationContext DSL", ["key, value, hashKey, hashValue serializers"], "green"),
    card("template", 990, 215, 310, 70, "ReactiveRedisTemplate", ["uses context for every command"], "purple"),
    card("binary", 125, 445, 300, 88, "RedisBinarySerializer", ["BinarySerializer -> ByteArray", "JDK/Kryo/Fory variants"], "teal"),
    card("compress", 525, 445, 300, 88, "RedisCompressSerializer", ["ByteArray compression only", "LZ4/Zstd/Snappy/GZip"], "amber"),
    card("factory", 925, 445, 330, 88, "RedisBinarySerializers", ["prebuilt serializer combinations", "LZ4Fory, ZstdFory, LZ4Kryo"], "pink"),
    card("bytes", 315, 720, 300, 66, "Redis bytes", ["keys and values serialized"], "blue"),
    card("redis", 820, 720, 300, 66, "Redis server", ["stores wire byte payloads"], "olive"),
    route("config", "context", "M430 250 L515 250", "blue"),
    route("context", "template", "M925 250 L990 250", "green"),
    route("context", "binary", "M620 290 L275 445", "teal"),
    route("context", "compress", "M720 290 L675 445", "amber"),
    route("context", "factory", "M820 290 L1090 445", "pink"),
    route("binary", "bytes", "M275 533 L385 720", "teal"),
    route("compress", "bytes", "M675 533 L465 720", "amber"),
    route("factory", "bytes", "M1090 533 L545 720", "pink"),
    route("bytes", "redis", "M615 753 L820 753", "blue"),
    route("template", "redis", "M1300 250 L1335 250 L1335 755 L1120 755", "purple"),
  ].join("\n");
  write("spring-boot-redis-diagram-02", base(1440, 905, "ReactiveRedisTemplate Serialization Flow", "The flow shows how a RedisSerializationContext selects binary/compression serializers before bytes are sent to Redis.", body), {
    nodes: ["config", "context", "template", "binary", "compress", "factory", "bytes", "redis"],
    edges: [["config", "context"], ["context", "template"], ["context", "binary"], ["context", "compress"], ["context", "factory"], ["binary", "bytes"], ["compress", "bytes"], ["factory", "bytes"], ["bytes", "redis"], ["template", "redis"]],
  });
}

function renderJunit5ExtensionOverview() {
  const body = [
    panel(82, 160, 1276, 145, "JUnit 5 extension points"),
    panel(82, 375, 1276, 250, "bluetape4k testing capabilities"),
    panel(82, 700, 1276, 145, "Execution and reporting"),
    card("junit", 145, 215, 300, 70, "JUnit Jupiter engine", ["discovers tests and extensions"], "blue"),
    card("context", 570, 215, 300, 70, "ExtensionContext", ["store, parameters, lifecycle"], "purple"),
    card("annotations", 975, 215, 320, 70, "Meta-annotations", ["@StopwatchTest, @TempFolderTest"], "green"),
    card("lifecycle", 115, 445, 280, 96, "Lifecycle extensions", ["Stopwatch", "SystemProperty restore", "TempFolder cleanup"], "teal"),
    card("injection", 420, 445, 330, 96, "Parameter / field injection", ["TempFolder", "FakeValue", "RandomValue"], "amber"),
    card("capture", 775, 445, 280, 96, "Output capture", ["stdout/stderr", "Logback appender"], "pink"),
    card("stress", 1035, 445, 360, 96, "Stress testers", ["threads, coroutines, virtual threads"], "olive"),
    card("listener", 205, 740, 370, 66, "Recording and Mermaid listeners", ["execution events and Gantt report"], "purple"),
    card("assertions", 825, 740, 320, 66, "Test assertions", ["awaitility, cancellation contracts"], "blue"),
    route("junit", "context", "M445 250 L570 250", "blue"),
    route("context", "annotations", "M870 250 L975 250", "purple"),
    route("context", "lifecycle", "M650 285 L255 445", "teal"),
    route("context", "injection", "M720 285 L585 445", "amber"),
    route("context", "capture", "M790 285 L915 445", "pink"),
    route("annotations", "stress", "M1135 285 L1135 445", "olive"),
    route("lifecycle", "listener", "M255 541 L255 740", "purple"),
    route("capture", "listener", "M915 541 L915 650 L390 650 L390 740", "pink"),
    route("stress", "assertions", "M1215 541 L1215 650 L1035 650 L1035 740", "olive"),
    route("injection", "assertions", "M585 541 L585 635 L935 635 L935 740", "amber"),
  ].join("\n");
  write("testing-junit5-diagram-01", base(1440, 925, "JUnit5 Extension Component Overview", "This is a capability map around real JUnit extension points, not a sequential call chain.", body), {
    nodes: ["junit", "context", "annotations", "lifecycle", "injection", "capture", "stress", "listener", "assertions"],
    edges: [["junit", "context"], ["context", "annotations"], ["context", "lifecycle"], ["context", "injection"], ["context", "capture"], ["annotations", "stress"], ["lifecycle", "listener"], ["capture", "listener"], ["stress", "assertions"], ["injection", "assertions"]],
  });
}

function write(name, svg, graph) {
  if (ONLY.size > 0 && !ONLY.has(name)) return;
  const svgPath = join(OUT, `${name}.svg`);
  const pngPath = join(OUT, `${name}.png`);
  const model = sourceModels[name];
  const stampedSvg = model && !/\bdata-layout="sequence"/.test(svg) ? stampSourceModel(svg, model) : svg;
  writeFileSync(svgPath, stampedSvg);
  renderPng(svgPath, pngPath);
}

function stampExisting(name) {
  if (ONLY.size > 0 && !ONLY.has(name)) return;
  const model = sourceModels[name];
  if (!model) throw new Error(`Missing source model for ${name}`);
  const svgPath = join(OUT, `${name}.svg`);
  const pngPath = join(OUT, `${name}.png`);
  const current = readFileSync(svgPath, "utf8");
  const stripped = replaceRepoOnlyFooter(current, model).replace(/\sdata-(?:intent|evidence|source-read)="[^"]*"/g, "");
  const stamped = /\bdata-layout="sequence"/.test(stripped) ? stripped : stampSourceModel(stripped, model);
  writeFileSync(svgPath, stamped);
  renderPng(svgPath, pngPath);
}

function replaceRepoOnlyFooter(svg, model) {
  return svg.replace(
    /(<text\b[^>]*>)(?:bluetape4k-projects\s*[-/]\s*)?github\.com\/bluetape4k\/bluetape4k-projects(?:\s*[-/]\s*bluetape4k-projects)?(<\/text>)/g,
    `$1${esc(sourceFooter(model))}$2`,
  ).replace(
    /(<text\b[^>]*>)bluetape4k-projects\s*-\s*github\.com\/bluetape4k\/bluetape4k-projects(<\/text>)/g,
    `$1${esc(sourceFooter(model))}$2`,
  );
}

function sourceFooter(model) {
  const first = model.evidence.find((item) => /README\.md/i.test(item)) ?? model.evidence[0] ?? "module source";
  const moduleName = first.replace(/\/README\.md.*/i, "").replace(/\/src\/main.*/i, "") || "module";
  return `Source: ${moduleName} README and public source contracts.`;
}

function stampSourceModel(svg, model) {
  const evidence = model.evidence.join("; ");
  const sourceRead = model.evidence.filter((item) => /README|src\/main|src\/test|build\.gradle|settings\.gradle|application\.ya?ml|\.kt/i.test(item)).join("; ");
  const layoutAttr = model.layout && !/\bdata-layout=/.test(svg) ? ` data-layout="${esc(model.layout)}"` : "";
  return svg.replace(
    "<svg ",
    `<svg data-intent="${esc(model.intent)}" data-evidence="${esc(evidence)}" data-source-read="${esc(sourceRead)}"${layoutAttr} `,
  );
}

function normalizeLegacyMonoTextPadding(names) {
  for (const name of names) {
    const svgPath = join(OUT, `${name}.svg`);
    const pngPath = join(OUT, `${name}.png`);
    if (!existsSync(svgPath)) continue;
    let svg = readFileSync(svgPath, "utf8");
    svg = svg.replace(/<g([^>]*)>([\s\S]*?)<\/g>/g, (group, attrs, body) => {
      const rectTag = body.match(/<rect[^>]*class="[^"]*(?:card|classCard|card-shape)[^"]*"[^>]*>/i)?.[0];
      if (!rectTag) return group;
      const x = Number(rectTag.match(/\bx="([-\d.]+)"/)?.[1]);
      if (Number.isNaN(x)) return group;
      const legacyX = x + 18;
      const paddedX = x + 34;
      const normalizedBody = body.replace(
        new RegExp(`(<text[^>]*class="[^"]*mono[^"]*"[^>]*\\bx=")${legacyX}(?=")`, "g"),
        `$1${paddedX}`,
      );
      return `<g${attrs}>${normalizedBody}</g>`;
    });
    writeFileSync(svgPath, svg);
    renderPng(svgPath, pngPath);
  }
}

renderRootOverview();
renderRootModuleStructure();
renderFoundationOverviews();
renderDataApiOverviews();
renderIoAndTestingOverviews();
renderUtilityFlowOverviews();
renderWorkflowDiagrams();
renderLoggingProcessingFlow();
renderJackson2ConversionFlow();
renderVertxFutureProcessingFlow();
renderMicrometerComponentMap();
stampExisting("infra-kafka4-diagram-01");
stampExisting("utils-geo-diagram-01");
stampExisting("utils-science-diagram-01");
stampExisting("infra-micrometer-diagram-02");
renderKafkaStreams();
renderHibernateReactive();
renderElasticsearch();
renderRedissonBatchTransactionFlow();
renderJsonImplementationSelectionFlow();
renderHttpMultiBackendArchitecture();
renderRedisUmbrellaModuleStructure();
renderSpringRestClientDsl();
renderSpringWebFluxCoroutineFlow();
renderSpringBootRetrofitIntegration();
renderSpringCassandraDataAccessLayer();
renderSpringBootCassandraDemo();
renderSpringBootObservabilityDemoArchitecture();
renderHibernateLettuceComponentMap();
renderHibernateLettuceActivationFlow();
renderHibernateLettuceDemoRuntimeFlow();
renderMongoClassStructure();
renderMongoCoroutineFlow();
renderMongoDslFlow();
renderR2dbcCoroutineFlow();
renderRedisSerializationFlow();
renderJunit5ExtensionOverview();
renderCsvProcessingFlow();
renderNettySmartEncodingFlow();
renderNettyByteBufProcessingFlow();
renderProtobufConversionFlow();
renderVertxModuleCapabilityArchitecture();
renderKtorCoreArchitecture();
renderKtorObservabilityArchitecture();
renderKtorTestingSequence();
renderKtorIdGeneratorDemoArchitecture();
renderKtorObservabilityDemoArchitecture();
renderKtorObservabilityDemoSequence();

for (const [file, model] of Object.entries(sourceModels)) {
  if (existsSync(join(OUT, `${file}.svg`))) {
    stampExisting(file);
  }
  console.log(`${file}: ${model.intent} sources=${model.evidence.join(", ")}`);
}

normalizeLegacyMonoTextPadding([
  "data-hibernate-reactive-diagram-01",
  "io-feign-diagram-03",
  "io-http-ko-diagram-02",
  "io-http-ko-diagram-03",
  "io-okio-diagram-04",
  "utils-idgenerators-diagram-04",
  "utils-rule-engine-diagram-04",
]);
