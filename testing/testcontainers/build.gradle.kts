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
    implementation(platform(libs.spring.boot.dependencies))
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.netty.all)

    api(libs.testcontainers)
    api(libs.testcontainers.junit.jupiter)

    api(libs.awaitility.kotlin)

    // Apple Silicon에서 testcontainers 를 사용하기 위해 참조해야 합니다.
    api(libs.jna)
    api(libs.jna.platform)

    compileOnly(libs.hikaricp)

    // MySQL
    compileOnly(libs.testcontainers.mysql)
    testRuntimeOnly(libs.mysql.connector.j)

    // MariaDB
    compileOnly(libs.testcontainers.mariadb)
    testRuntimeOnly(libs.mariadb.java.client)

    // Postgres
    compileOnly(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.postgresql.driver)

    // CockroachDB
    compileOnly(libs.testcontainers.cockroachdb)

    // R2DBC
    compileOnly(libs.testcontainers.r2dbc)
    compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testRuntimeOnly(libs.r2dbc.mariadb)
    testRuntimeOnly(libs.r2dbc.mysql)
    testRuntimeOnly(libs.r2dbc.postgresql)

    // Redis
    compileOnly(libs.redisson)
    compileOnly(libs.lettuce.core)

    compileOnly(libs.fory.kotlin)  // new Apache Fory
    compileOnly(libs.kryo)

    compileOnly(libs.commons.compress)
    compileOnly(libs.lz4.java)
    compileOnly(libs.snappy.java)
    compileOnly(libs.zstd.jni)

    // Hazelcast
    compileOnly(libs.hazelcast)

    // MongoDB
    compileOnly(libs.testcontainers.mongodb)
    compileOnly(libs.mongodb.driver.kotlin.sync)
    compileOnly(libs.mongodb.driver.kotlin.coroutine)
    compileOnly(libs.mongodb.driver.kotlin.extensions)

    // Cassandra
    api(libs.testcontainers.cassandra)
    compileOnly(libs.cassandra.java.driver.core)
    compileOnly(libs.cassandra.java.driver.query.builder)

    // Kubernetes (K3s)
    api(libs.testcontainers.k3s)
    compileOnly(libs.fabric8.kubernetes.client)
    testImplementation(libs.fabric8.kubernetes.client)

    // Graph DB (Neo4j)
    compileOnly(libs.testcontainers.neo4j)
    compileOnly(libs.neo4j.java.driver)
    testRuntimeOnly(libs.neo4j.java.driver)
    testRuntimeOnly(libs.neo4j.bolt.connection.netty)

    // Graph DB (FalkorDB)
    compileOnly(libs.jfalkordb)

    // ElasticSearch
    compileOnly(libs.testcontainers.elasticsearch)
    compileOnly(libs.elasticsearch.rest.client)
    compileOnly(libs.elasticsearch.rest.client.sniffer)
    compileOnly("org.springframework.data:spring-data-elasticsearch")

    // Opensearch
    compileOnly(libs.testcontainers.opensearch)

    // Kafka
    compileOnly(libs.testcontainers.kafka)
    compileOnly(libs.kafka.clients)
    compileOnly(libs.spring.kafka)

    // Pulsar
    compileOnly(libs.testcontainers.pulsar)
    compileOnly(libs.pulsar.client.api)
    compileOnly(libs.pulsar.client)

    // Redpanda
    compileOnly(libs.testcontainers.redpanda)

    // Chaos Testing (Toxiproxy)
    compileOnly(libs.testcontainers.toxiproxy)

    // Distributed SQL (Trino)
    compileOnly(libs.testcontainers.trino)
    testRuntimeOnly(libs.trino.jdbc)

    // NATS
    compileOnly(libs.jnats)

    // RabbitMQ
    compileOnly(libs.testcontainers.rabbitmq)
    testImplementation(libs.amqp.client)

    // Zipkin
    testImplementation(libs.zipkin.brave)

    // HashiCorp Vault
    compileOnly(libs.testcontainers.vault)
    compileOnly(libs.vault.java.driver)

    // OkHttp
    testImplementation(libs.okhttp3)

    // LocalStack for AWS
    compileOnly(libs.testcontainers.localstack)

    // MiniStack for AWS emulation
    compileOnly(libs.testcontainers.ministack)

    // ElasticMQ - embedded SQS emulator (no Docker)
    compileOnly(libs.elasticmq.rest.sqs)

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
    testImplementation(libs.aws2.aws.crt)

    // Minio
    compileOnly(libs.testcontainers.minio)
    compileOnly(libs.minio)

    // Immudb
    testRuntimeOnly(libs.immudb4j)

    // Curator framework for ZooKeeper
    compileOnly(libs.curator.framework)

    // Ollama
    compileOnly(libs.testcontainers.ollama)

    testImplementation(libs.rest.assured)
    testImplementation(libs.rest.assured.kotlin)

    // Nginx
    compileOnly(libs.testcontainers.nginx)

    // Wiremock
    compileOnly(libs.wiremock)

    // Keycloak
    compileOnly(libs.keycloak.testcontainers)

    // ClickHouse
    compileOnly(libs.testcontainers.clickhouse)
    testRuntimeOnly(libs.clickhouse.jdbc)
    // testRuntimeOnly(libs.httpclient5)

    // Weaviate
    compileOnly(libs.testcontainers.weaviate)
    testRuntimeOnly(libs.weaviate.client)

    // ChromaDB
    compileOnly(libs.testcontainers.chromadb)

    // InfluxDB
    compileOnly(libs.testcontainers.influxdb)

}
