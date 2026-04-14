# Module bluetape4k-testcontainers

English | [한국어](./README.ko.md)

A server wrapper and utility library for building integration tests quickly on top of Testcontainers `2.0.3`.

## Architecture

### Container Lifecycle

```mermaid
sequenceDiagram
    box rgb(248,187,208) Test Layer
        participant TEST as Test Class
    end
    box rgb(178,223,219) Server Wrappers
        participant SERVER as GenericServer (wrapper)
    end
    box rgb(207,216,220) Infrastructure
        participant TC as Testcontainers
        participant DOCKER as Docker Container
    end
    box rgb(200,230,201) Spring Boot
        participant SPRING as Spring Boot
    end

    TEST->>SERVER: start()
    SERVER->>TC: Request container start
    TC->>DOCKER: Docker image pull & run
    DOCKER-->>TC: Container ready
    TC-->>SERVER: Port mapping info
    SERVER->>SERVER: writeToSystemProperties()<br/>testcontainers.{name}.host/port/url registered
    SERVER-->>TEST: Start complete

    TEST->>SPRING: Begin test execution
    SPRING->>SPRING: Load application-test.yml<br/>${testcontainers.mysql.jdbc-url} resolved
    SPRING-->>TEST: ApplicationContext ready

    TEST->>TEST: Execute test logic

    TEST->>SERVER: stop() (or @AfterAll)
    SERVER->>DOCKER: Container stop & remove
```

### Supported Container Class Diagram

```mermaid
classDiagram
    class GenericServer {
        <<abstract>>
        +useDefaultPort: Boolean
        +start()
        +stop()
        +writeToSystemProperties(name)
        +exposeCustomPorts(vararg ports)
    }
    class PostgreSQLServer {
        +withExtensions(vararg names) PostgreSQLServer
    }
    class PostgisServer {
        +withExtensions(vararg names) PostgisServer
    }
    class PgvectorServer {
        +withExtensions(vararg names) PgvectorServer
    }
    class MySQL8Server {
        +getDataSource() DataSource
    }
    class RedisServer {
        +host: String
        +port: Int
    }
    class KafkaServer {
        +bootstrapServers: String
    }
    class LocalStackServer {
        +endpointOverride: URI
        +getCredentialsProvider() AwsCredentialsProvider
    }

    GenericServer <|-- PostgreSQLServer
    GenericServer <|-- PostgisServer
    GenericServer <|-- PgvectorServer
    GenericServer <|-- MySQL8Server
    GenericServer <|-- RedisServer
    GenericServer <|-- KafkaServer
    GenericServer <|-- LocalStackServer
    PostgreSQLServer <|-- PostgisServer
    PostgreSQLServer <|-- PgvectorServer

    style GenericServer fill:#1976D2,stroke:#1565C0,color:#FFFFFF
    style PostgreSQLServer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style PostgisServer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style PgvectorServer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style MySQL8Server fill:#00897B,stroke:#00695C,color:#FFFFFF
    style RedisServer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style KafkaServer fill:#00897B,stroke:#00695C,color:#FFFFFF
    style LocalStackServer fill:#00897B,stroke:#00695C,color:#FFFFFF
```

### Supported Container Structure

```mermaid
flowchart TD
    GS["GenericServer\n(common base)"]

    subgraph Database
        MY5["MySQL5Server"]
        MY8["MySQL8Server"]
        MA["MariaDBServer"]
        PG["PostgreSQLServer\n(withExtensions support)"]
        PGS["PostgisServer\n(postgis auto-activated)"]
        PGV["PgvectorServer\n(vector auto-activated)"]
        CR["CockroachServer"]
        CH["ClickHouseServer"]
    end

    subgraph Storage
        RD["RedisServer"]
        RDC["RedisClusterServer"]
        MGO["MongoDBServer"]
        CS["CassandraServer"]
        ES["ElasticsearchServer"]
        OS["OpenSearchServer"]
        MN["MinIOServer"]
        IFL["InfluxDBServer"]
    end

    subgraph GraphDB
        NJ["Neo4jServer"]
        MG["MemgraphServer"]
        PA["PostgreSQLAgeServer"]
    end

    subgraph MessageQueue
        KF["KafkaServer"]
        RB["RabbitMQServer"]
        PL["PulsarServer"]
        NT["NatsServer"]
        RP["RedpandaServer"]
    end

    subgraph Infra
        CN["ConsulServer"]
        VT["VaultServer"]
        PR["PrometheusServer"]
        ZK["ZooKeeperServer"]
        TX["ToxiproxyServer"]
        KC["KeycloakServer"]
    end

    subgraph DistributedSQL
        TR["TrinoServer"]
    end

    subgraph HTTPMock
        WM["WireMockServer"]
    end

    subgraph AWS
        LS["LocalStackServer\n(S3, DynamoDB, etc.)"]
    end

    GS --> Database
    GS --> Storage
    GS --> GraphDB
    GS --> MessageQueue
    GS --> Infra
    GS --> DistributedSQL
    GS --> HTTPMock
    GS --> AWS

    classDef baseStyle fill:#1976D2,stroke:#1565C0,color:#FFFFFF,font-weight:bold
    classDef dbStyle fill:#00897B,stroke:#00695C,color:#FFFFFF
    classDef storageStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef graphStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF
    classDef mqStyle fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    classDef infraStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef sqlStyle fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    classDef mockStyle fill:#F57F17,stroke:#E65100,color:#000000
    classDef awsStyle fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF

    class GS baseStyle
    class MY5,MY8,MA,PG,PGS,PGV,CR,CH dbStyle
    class RD,RDC,MGO,CS,ES,OS,MN,IFL storageStyle
    class NJ,MG,PA graphStyle
    class KF,RB,PL,NT,RP mqStyle
    class CN,VT,PR,ZK,TX,KC infraStyle
    class TR sqlStyle
    class WM mockStyle
    class LS awsStyle
```

## Key Features

- Wrappers for database, graph DB, storage, messaging, infrastructure, and distributed SQL services
- HTTP mocking through WireMock
- AWS LocalStack support
- Shared `GenericServer` / `GenericContainer` utilities
- Automatic PostgreSQL extension activation for PostGIS and pgvector
- Declarative activation of extra PostgreSQL extensions through `withExtensions()`
- Optional fixed-port mapping with `useDefaultPort=true`
- Automatic export of connection details as system properties at `start()` time
- Simplified Spring Boot wiring through `${testcontainers...}` placeholders

## System Property Export (`PropertyExportingServer`)

Every server implements
`PropertyExportingServer`, which automatically registers connection details as system properties at `start()` time.

- Property keys use lowercase kebab-case
- Format: `testcontainers.{namespace}.{kebab-case-key}`
- Examples: `testcontainers.postgresql.jdbc-url`, `testcontainers.kafka.bootstrap-servers`

### Exported Keys by Server

| Server              | namespace       | Key properties                                                                      |
|---------------------|-----------------|-------------------------------------------------------------------------------------|
| PostgreSQLServer    | `postgresql`    | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| PostgisServer       | `postgis`       | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| PgvectorServer      | `pgvector`      | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| MySQL8Server        | `mysql`         | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| MariaDBServer       | `mariadb`       | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| CockroachServer     | `cockroach`     | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| ClickHouseServer    | `clickhouse`    | `jdbc-url`, `driver-class-name`, `username`, `password`, `database-name`            |
| TrinoServer         | `trino`         | `jdbc-url`, `username`                                                              |
| RedisServer         | `redis`         | `host`, `port`, `url`                                                               |
| MongoDBServer       | `mongo`         | `host`, `port`, `url`                                                               |
| ElasticsearchServer | `elasticsearch` | `host`, `port`, `url`                                                               |
| KafkaServer         | `kafka`         | `host`, `port`, `url`, `bootstrap-servers`, `bound-port-numbers`                    |
| RedpandaServer      | `redpanda`      | `host`, `port`, `url`, `admin-port`, `schema-registry-port`, `rest-proxy-port`      |
| NatsServer          | `nats`          | `host`, `port`, `url`, `cluster-port`, `monitor-port`                               |
| PulsarServer        | `pulsar`        | `host`, `port`, `url`, `broker-url`, `broker-port`, `broker-http-port`              |
| RabbitMQServer      | `rabbitmq`      | `host`, `port`, `url`, `amqp-url`, `amqp-port`, `amqps-port`, `management-url`      |
| LocalStackServer    | `localstack`    | `host`, `port`, `url`                                                               |
| PrometheusServer    | `prometheus`    | `host`, `port`, `url`, `server-port`, `pushgateway-port`, `graphite-exporter-port`  |
| ConsulServer        | `consul`        | `host`, `port`, `url`, `dns-port`, `http-port`, `rpc-port`                          |
| JaegerServer        | `jaeger`        | `host`, `port`, `url`, `frontend-port`, `zipkin-port`, `config-port`, `thrift-port` |

## Usage Examples

### Database

```kotlin
val mysql = MySQL8Server(useDefaultPort = true).apply { start() }
val ds = mysql.getDataSource()
```

### PostgreSQL Extensions

```kotlin
// PostGIS — auto-activates postgis extension
val server = PostgisServer.Launcher.postgis

// pgvector — auto-activates vector extension
val server = PgvectorServer.Launcher.pgvector

// Extra extensions via withExtensions()
PostgisServer()
    .withExtensions("postgis_topology")
    .apply { start() }

PostgreSQLServer()
    .withExtensions("uuid-ossp", "hstore", "pg_trgm")
    .apply { start() }

// Singleton with extensions
val server = PostgreSQLServer.Launcher.withExtensions("uuid-ossp", "hstore")
```

### Graph DB

```kotlin
// Neo4j
val neo4j = Neo4jServer.Launcher.neo4j
val driver = GraphDatabase.driver(neo4j.boltUrl, AuthTokens.basic(neo4j.username, neo4j.password))

// Memgraph
val memgraph = MemgraphServer.Launcher.memgraph
val driver = GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none())

// PostgreSQL with Apache AGE
val age = PostgreSQLAgeServer.Launcher.postgresqlAge
val conn = DriverManager.getConnection(age.jdbcUrl, age.username, age.password)
```

### HTTP Mock Server

```kotlin
val wireMock = WireMockServer.Launcher.wireMock

wireMock.stubFor(
    get("/hello")
        .willReturn(ok("Hello!"))
)

verify(getRequestedFor(urlEqualTo("/hello")))
```

### Keycloak (Auth Server)

```kotlin
val keycloak = KeycloakServer.Launcher.keycloak
println("Auth Server URL: ${keycloak.getAuthServerUrl()}")
println("Admin Username: ${keycloak.getAdminUsername()}")
println("Admin Password: ${keycloak.getAdminPassword()}")
```

### InfluxDB (Time-series)

```kotlin
val influxDB = InfluxDBServer.Launcher.influxDB
println("URL: ${influxDB.url}")
println("Admin Token: ${influxDB.adminToken}")
println("Bucket: ${influxDB.bucket}")
println("Organization: ${influxDB.organization}")
```

### Toxiproxy (Chaos Testing)

```mermaid
sequenceDiagram
    autonumber
    box rgb(248,187,208) Test Layer
        participant TEST as Test Code
    end
    box rgb(207,216,220) Infrastructure
        participant REDIS as RedisServer
        participant TOXI as ToxiproxyServer
    end
    box rgb(178,223,219) Proxy Control
        participant API as ToxiproxyClient
    end
    box rgb(187,222,251) Application
        participant LETTUCE as Lettuce Client
    end

    TEST->>REDIS: start() withNetwork(network)
    TEST->>TOXI: start() withNetwork(network)
    TEST->>API: createProxy("redis-primary", "0.0.0.0:8666", "redis:6379")
    API-->>TOXI: Proxy created with listen/upstream config

    TEST->>LETTUCE: connect(toxiproxy.host, toxiproxy.getMappedPort(8666))
    LETTUCE->>TOXI: PING / SET / GET requests
    TOXI->>REDIS: Forward to redis:6379
    REDIS-->>TOXI: Return response
    TOXI-->>LETTUCE: Return proxy response

    TEST->>API: proxy.toxics().latency(..., DOWNSTREAM, 250)
    API-->>TOXI: Add downstream latency toxic
    LETTUCE->>TOXI: GET request
    TOXI-->>LETTUCE: Delayed response

    TEST->>API: latency.remove()
    API-->>TOXI: Remove toxic
    LETTUCE->>TOXI: GET request
    TOXI-->>LETTUCE: Normal speed response
```

### Distributed SQL

```kotlin
val trino = TrinoServer.Launcher.trino
val conn = DriverManager.getConnection(
    "jdbc:trino://${trino.host}:${trino.port}/memory",
    "test",
    null
)
val stmt = conn.createStatement()
val rs = stmt.executeQuery("SELECT 1 as num")
```

### System Property Access

```kotlin
// After start() — read system properties directly
val postgresUrl = System.getProperty("testcontainers.postgresql.jdbc-url")
val kafkaServers = System.getProperty("testcontainers.kafka.bootstrap-servers")

// Register with auto-restore after test
@BeforeEach
fun setup() {
    registration = PostgreSQLServer.Launcher.postgres.registerSystemProperties()
}

@AfterEach
fun cleanup() {
    registration.close()
}
```

## Spring Boot Configuration

Start containers in `@BeforeAll`, then reference properties in `application-test.yml`:

```kotlin
class MyRepositoryTest {
    companion object {
        private val mysql = MySQL8Server(useDefaultPort = true)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mysql.start()  // registers testcontainers.mysql.* system properties
        }
    }
}
```

```yaml
spring:
  datasource:
    driver-class-name: ${testcontainers.mysql.driver-class-name}
    url: ${testcontainers.mysql.jdbc-url}
    username: ${testcontainers.mysql.username}
    password: ${testcontainers.mysql.password}

  data:
    redis:
      host: ${testcontainers.redis.host}
      port: ${testcontainers.redis.port}

  kafka:
    bootstrap-servers: ${testcontainers.kafka.bootstrap-servers}
```

## Recent Stability Improvements

- `GenericContainer.exposeCustomPorts(...)` now creates port bindings even when `hostConfig` starts empty.
-

`GenericServer.writeToSystemProperties(...)` registers default and additional properties in a stable, consistent order.
- `KafkaServer.Launcher` creates fresh serializer/deserializer instances per use to avoid reuse after `close()`.
- `TiDBServer` is deprecated because Testcontainers 2.x does not support it reliably. Use `MySQL8Server` instead.

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers:${version}")
}
```

## References

- [Testcontainers](https://www.testcontainers.org/)
- [LocalStack](https://www.localstack.cloud/)

## Colima + LocalStack Troubleshooting

When running under Colima, set:

```bash
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
```

If issues persist, restart Colima:

```bash
brew services stop colima
colima stop
rm -f ~/.colima/docker.sock
brew services start colima
```

If Ryuk causes problems (temporary workaround only):

```bash
export TESTCONTAINERS_RYUK_DISABLED=true
```

> **Note**:
`TESTCONTAINERS_RYUK_DISABLED=true` affects automatic resource cleanup. Use with caution in CI/shared environments.
