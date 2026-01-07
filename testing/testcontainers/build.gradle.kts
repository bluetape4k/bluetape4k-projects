configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(Libs.netty_all)

    api(Libs.testcontainers)
    api(Libs.testcontainers_junit_jupiter)

    api(Libs.awaitility_kotlin)

    // Apple Silicon에서 testcontainers 를 사용하기 위해 참조해야 합니다.
    api(Libs.jna)
    api(Libs.jna_platform)

    compileOnly(Libs.hikaricp)

    // MySQL
    compileOnly(Libs.testcontainers_mysql)
    testRuntimeOnly(Libs.mysql_connector_j)

    // MariaDB
    compileOnly(Libs.testcontainers_mariadb)
    testRuntimeOnly(Libs.mariadb_java_client)

    // Postgres
    compileOnly(Libs.testcontainers_postgresql)
    testRuntimeOnly(Libs.postgresql_driver)

    // CockroachDB
    compileOnly(Libs.testcontainers_cockroachdb)

    // R2DBC
    compileOnly(Libs.testcontainers_r2dbc)
    compileOnly(Libs.springBootStarter("data-r2dbc"))
    compileOnly(Libs.r2dbc_mysql)

    // Redis
    compileOnly(Libs.redisson)
    compileOnly(Libs.lettuce_core)

//    compileOnly(Libs.kryo)
//    compileOnly(Libs.fory_kotlin)  // new Apache Fory
//    compileOnly(Libs.fury_kotlin)  // old Apache Fury

//    compileOnly(Libs.commons_compress)
//    compileOnly(Libs.snappy_java)
//    compileOnly(Libs.lz4_java)

    // Hazelcast
    compileOnly(Libs.hazelcast)

    // MongoDB
    compileOnly(Libs.testcontainers_mongodb)
    compileOnly(Libs.mongodb_driver_kotlin_sync)

    // Cassandra
    compileOnly(Libs.testcontainers_cassandra)
    compileOnly(Libs.cassandra_java_driver_core)
    compileOnly(Libs.cassandra_java_driver_query_builder)

    // ElasticSearch
    compileOnly(Libs.testcontainers_elasticsearch)
    compileOnly(Libs.elasticsearch_rest_client)
    compileOnly(Libs.springData("elasticsearch"))

    // Opensearch
    compileOnly(Libs.testcontainers_opensearch)

    // Kafka
    compileOnly(Libs.testcontainers_kafka)
    compileOnly(Libs.kafka_clients)
    compileOnly(Libs.spring_kafka)

    // Pulsar
    compileOnly(Libs.testcontainers_pulsar)
    compileOnly(Libs.pulsar_client)

    // Redpanda
    compileOnly(Libs.testcontainers_redpanda)

    // NATS
    compileOnly(Libs.jnats)

    // RabbitMQ
    compileOnly(Libs.testcontainers_rabbitmq)
    testImplementation(Libs.amqp_client)

    // Zipkin
    testImplementation(Libs.zipkin_brave)

    // HashiCorp Vault
    compileOnly(Libs.testcontainers_vault)
    compileOnly(Libs.vault_java_driver)

    // OkHttp
    testImplementation(Libs.okhttp3)

    // LocalStack for AWS
    compileOnly(Libs.testcontainers_localstack)

    // Amazon SDK V2
    compileOnly(Libs.aws2_auth)
    testImplementation(Libs.aws2_aws_core)
    testImplementation(Libs.aws2_sdk_core)
    testImplementation(Libs.aws2_apache_client)
    testImplementation(Libs.aws2_cloudwatch)
    testImplementation(Libs.aws2_cloudwatchevents)
    testImplementation(Libs.aws2_cloudwatchlogs)
    testImplementation(Libs.aws2_dynamodb_enhanced)
    testImplementation(Libs.aws2_kms)
    testImplementation(Libs.aws2_s3)
    testImplementation(Libs.aws2_ses)
    testImplementation(Libs.aws2_sns)
    testImplementation(Libs.aws2_sqs)
    testImplementation(Libs.aws2_sts)

    // https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration-crt.html
    // https://mvnrepository.com/artifact/software.amazon.awssdk.crt/aws-crt
    testImplementation(Libs.aws2_aws_crt)

    testImplementation(Libs.metrics_jmx)

    // Minio
    compileOnly(Libs.testcontainers_minio)
    compileOnly(Libs.minio)

    // Immudb
    testRuntimeOnly(Libs.immudb4j)

    // Curator framework for ZooKeeper
    compileOnly(Libs.curator_framework)

    // Ollama
    compileOnly(Libs.testcontainers_ollama)

    testImplementation(Libs.rest_assured)
    testImplementation(Libs.rest_assured_kotlin)

    // Nginx
    compileOnly(Libs.testcontainers_nginx)

    // Wiremock
    compileOnly(Libs.wiremock)

    // ClickHouse
    compileOnly(Libs.testcontainers_clickhouse)
    testRuntimeOnly(Libs.clickhouse_jdbc)
    testRuntimeOnly(Libs.httpclient5)

    // Weaviate
    compileOnly(Libs.testcontainers_weaviate)
    testRuntimeOnly(Libs.weaviate_client)

    // ChromaDB
    compileOnly(Libs.testcontainers_chromadb)

    // TiDB
    compileOnly(Libs.testcontainers_tidb)

    testImplementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    testImplementation(Libs.netty_all)
}
