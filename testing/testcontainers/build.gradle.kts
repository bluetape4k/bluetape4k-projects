configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
    all {
        // CVE-2025-12183 (CVSS 8.8) + CVE-2025-66566 (CVSS 8.2):
        // kafka-clients 등이 org.lz4:lz4-java 를 transitively 가져온다.
        // at.yawk.lz4:lz4-java:1.11.0 으로 대체한다.
        exclude(group = "org.lz4", module = "lz4-java")
    }
}

kover {
    currentProject {
        instrumentation {
            // LLM 컨테이너 테스트는 이미지 크기와 사용 빈도 때문에 Nightly 에서도 비활성화되어 있다.
            excludedClasses.add("io.bluetape4k.testcontainers.llm.*")
            // Launcher/Launch 객체는 테스트 인프라 재사용용 singleton holder 이므로 유효 커버리지에서 제외한다.
            excludedClasses.add("**\$Launch")
            excludedClasses.add("**\$Launch\$*")
            excludedClasses.add("**\$Launcher")
            excludedClasses.add("**\$Launcher\$*")
        }
    }
    reports {
        filters {
            excludes {
                classes(
                    "io.bluetape4k.testcontainers.llm.*",
                    "**\$Launch",
                    "**\$Launch\$*",
                    "**\$Launcher",
                    "**\$Launcher\$*",
                )
            }
        }
    }
}

// testcontainers 테스트 실행 전 mock-server Docker 이미지를 자동으로 빌드합니다.
// Jib은 소스 변경이 없으면 up-to-date 체크로 스킵하므로 매번 느리지 않습니다.
tasks.test {
    dependsOn(":bluetape4k-mock-web-server:jibDockerBuild")
    dependsOn(":bluetape4k-mock-webflux-server:jibDockerBuild")
    // Ignite 2.18 thin client reflects on java.nio.Buffer under Java 25.
    jvmArgs(
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
    )
    useJUnitPlatform {
        // K3s requires a privileged Docker runner. Excluded from regular CI by default.
        // Enable with: ./gradlew :bluetape4k-testcontainers:test -PincludeK8s
        if (!project.hasProperty("includeK8s")) {
            excludeTags("k8s")
        }
    }
}

// Separate task for k8s-tagged tests (nightly privileged runner).
tasks.register<Test>("k8sTest") {
    description = "Runs @Tag(\"k8s\") tests — requires a privileged Docker runner."
    group = "verification"
    dependsOn(":bluetape4k-mock-web-server:jibDockerBuild")
    dependsOn(":bluetape4k-mock-webflux-server:jibDockerBuild")
    useJUnitPlatform {
        includeTags("k8s")
    }
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}

dependencies {
    implementation(platform(bt4k.spring.boot4.dependencies))
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.netty.all)

    api(libs.testcontainers)
    api(libs.testcontainers.junit.jupiter)

    // Ignite 2 thin-client workload is test-only; it must not enter the published POM.
    testImplementation(bt4k.ignite.core)

    api(libs.awaitility.kotlin)

    // Apple Silicon에서 testcontainers 를 사용하기 위해 참조해야 합니다.
    api(bt4k.jna)
    api(bt4k.jna.platform)

    compileOnly(bt4k.hikaricp)

    // MySQL
    compileOnly(libs.testcontainers.mysql)
    testRuntimeOnly(bt4k.mysql.connector.j)

    // MariaDB
    compileOnly(libs.testcontainers.mariadb)
    testRuntimeOnly(bt4k.mariadb.java.client)

    // Postgres
    compileOnly(libs.testcontainers.postgresql)
    testRuntimeOnly(bt4k.postgresql)

    // CockroachDB
    compileOnly(libs.testcontainers.cockroachdb)

    // R2DBC
    compileOnly(libs.testcontainers.r2dbc)
    compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testRuntimeOnly(bt4k.r2dbc.mariadb)
    testRuntimeOnly(bt4k.r2dbc.mysql)
    testRuntimeOnly(bt4k.r2dbc.postgresql)

    // Redis
    compileOnly(bt4k.redisson)
    compileOnly(libs.lettuce.core)

    compileOnly(bt4k.fory.kotlin)  // new Apache Fory
    compileOnly(bt4k.kryo)

    compileOnly(bt4k.commons.compress)
    compileOnly(bt4k.at.yawk.lz4.java)
    compileOnly(bt4k.snappy.java)
    compileOnly(bt4k.zstd.jni)

    // Hazelcast
    compileOnly(bt4k.hazelcast)

    // MongoDB
    compileOnly(libs.testcontainers.mongodb)
    compileOnly(bt4k.mongodb.driver.kotlin.sync)
    compileOnly(bt4k.mongodb.driver.kotlin.coroutine)
    compileOnly(bt4k.mongodb.driver.kotlin.extensions)

    // Cassandra
    api(libs.testcontainers.cassandra)
    compileOnly(bt4k.cassandra.java.driver.core)
    compileOnly(bt4k.cassandra.java.driver.query.builder)

    // Kubernetes (K3s)
    api(libs.testcontainers.k3s)
    compileOnly(bt4k.fabric8.kubernetes.client)
    testImplementation(bt4k.fabric8.kubernetes.client)

    // Graph DB (Neo4j)
    compileOnly(libs.testcontainers.neo4j)
    compileOnly(bt4k.neo4j.driver6)
    testRuntimeOnly(bt4k.neo4j.driver6)
    testRuntimeOnly(bt4k.neo4j.bolt.connection.netty)

    // Graph DB (FalkorDB)
    compileOnly(bt4k.jfalkordb)

    // ElasticSearch
    compileOnly(libs.testcontainers.elasticsearch)
    compileOnly(bt4k.elasticsearch.rest.client)
    compileOnly(bt4k.elasticsearch.rest.client.sniffer)
    compileOnly("org.springframework.data:spring-data-elasticsearch")

    // Opensearch
    compileOnly(bt4k.testcontainers.opensearch)

    // Kafka
    compileOnly(libs.testcontainers.kafka)
    compileOnly(bt4k.kafka.clients)
    compileOnly(bt4k.spring.kafka)

    // Pulsar
    compileOnly(libs.testcontainers.pulsar)
    compileOnly(bt4k.pulsar4.client.api)
    compileOnly(bt4k.pulsar4.client)

    // Redpanda
    compileOnly(libs.testcontainers.redpanda)

    // Chaos Testing (Toxiproxy)
    compileOnly(libs.testcontainers.toxiproxy)

    // Distributed SQL (Trino)
    compileOnly(libs.testcontainers.trino)
    testRuntimeOnly(bt4k.trino.jdbc)

    // NATS
    compileOnly(bt4k.jnats)

    // RabbitMQ
    compileOnly(libs.testcontainers.rabbitmq)
    testImplementation(bt4k.amqp.client)

    // Zipkin
    testImplementation(bt4k.zipkin.brave)

    // HashiCorp Vault
    compileOnly(libs.testcontainers.vault)
    compileOnly(bt4k.vault.java.driver)

    // Apache HttpComponents 5 — used by GrafanaServer for Grafana HTTP API provisioning
    implementation(libs.httpclient5.fluent)

    // OkHttp
    testImplementation(bt4k.okhttp3)

    // LocalStack for AWS
    compileOnly(libs.testcontainers.localstack)

    // MiniStack for AWS emulation
    compileOnly(bt4k.testcontainers.ministack)

    // ElasticMQ - embedded SQS emulator (no Docker)
    compileOnly(bt4k.elasticmq.rest.sqs)

    // Amazon SDK V2
    compileOnly(libs.aws2.auth)
    testImplementation(libs.aws2.aws.core)
    testImplementation(libs.aws2.sdk.core)
    testImplementation(libs.aws2.apache.client)
    testImplementation(libs.aws2.cloudwatch)
    testImplementation(libs.aws2.cloudwatchevents)
    testImplementation(libs.aws2.cloudwatchlogs)
    testImplementation(libs.aws2.dynamodb.enhanced)
    testImplementation(libs.aws2.kafka)
    testImplementation(libs.aws2.kinesis)
    testImplementation(libs.aws2.kms)
    testImplementation(libs.aws2.s3)
    testImplementation(libs.aws2.ses)
    testImplementation(libs.aws2.sns)
    testImplementation(libs.aws2.sqs)
    testImplementation(libs.aws2.sts)

    // https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration-crt.html
    // https://mvnrepository.com/artifact/software.amazon.awssdk.crt/aws-crt
    testImplementation(bt4k.aws2.aws.crt)

    // Minio
    compileOnly(libs.testcontainers.minio)
    compileOnly(bt4k.minio.v9)

    // Immudb
    testRuntimeOnly(bt4k.immudb4j)

    // Curator framework for ZooKeeper
    compileOnly(bt4k.curator.framework)

    // Ollama
    compileOnly(libs.testcontainers.ollama)

    testImplementation(bt4k.rest.assured)
    testImplementation(bt4k.rest.assured.kotlin)

    // Nginx
    compileOnly(libs.testcontainers.nginx)

    // Wiremock
    compileOnly(bt4k.wiremock) {
        // WireMock container client만 노출하며 embedded server와 template engine은 사용하지 않는다.
        exclude(group = "com.github.jknack")
        exclude(group = "org.eclipse.jetty")
        exclude(group = "org.eclipse.jetty.http2")
    }

    // Keycloak
    compileOnly(bt4k.keycloak.testcontainers)

    // ClickHouse
    compileOnly(libs.testcontainers.clickhouse)
    testRuntimeOnly(bt4k.clickhouse.jdbc)
    // testRuntimeOnly(bt4k.httpclient5)

    // Weaviate
    compileOnly(libs.testcontainers.weaviate)
    testRuntimeOnly(bt4k.weaviate.client)

    // ChromaDB
    compileOnly(libs.testcontainers.chromadb)

    // InfluxDB
    compileOnly(libs.testcontainers.influxdb)

}
