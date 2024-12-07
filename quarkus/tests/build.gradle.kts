plugins {
    id(Plugins.quarkus)

    // Quarkus 는 기본적으로 root project 기반이라 다른 모듈에서 inject 할 beans를 검색하지 않는다
    // 다른 모듈에서도 beans 를 검색하고, inject 되도록 하기 위해 jandex plugins 를 사용해서 bean 목록을 제공해야 한다
    // 참고: https://quarkus.io/guides/gradle-tooling#publishing-your-application
    // id(Plugins.jandex) version Plugins.Versions.jandex
    // ==> empty beans.xml 을 사용하는 것으로 변경
    // https://stackoverflow.com/questions/55513502/how-to-create-a-jandex-index-in-quarkus-for-classes-in-a-external-module

    kotlin("plugin.allopen")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")

    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // NOTE: Quarkus 는 꼭 gradle platform 으로 참조해야 제대로 빌드가 된다.
    implementation(enforcedPlatform(Libs.quarkus_bom))
    implementation(platform(Libs.quarkus_universe_bom))
    implementation(platform(Libs.resteasy_bom))

    // Testing
    api(Libs.quarkus_junit5)
    api(Libs.rest_assured_kotlin)
    api(Libs.awaitility_kotlin)

    api(project(":bluetape4k-quarkus-core"))
    api(project(":bluetape4k-mutiny"))
    api(project(":bluetape4k-vertx-core"))
    compileOnly(project(":bluetape4k-testcontainers"))
    compileOnly(project(":bluetape4k-junit5"))

    api(Libs.quarkus_kotlin)
    implementation(Libs.quarkus_arc)


    compileOnly(Libs.quarkus_hibernate_reactive_panache)
    compileOnly(Libs.quarkus_opentelemetry)
    compileOnly(Libs.quarkus_vertx)
    compileOnly(Libs.quarkus_reactive_routes)

    api(Libs.vertx_lang_kotlin)
    api(Libs.vertx_lang_kotlin_coroutines)

    compileOnly(Libs.camel_quarkus_kotlin)

    // Coroutines
    api(project(":bluetape4k-coroutines"))
    api(Libs.kotlinx_coroutines_core)
    api(Libs.kotlinx_coroutines_test)

    // TestResource
    compileOnly(Libs.testcontainers_kafka)
    compileOnly(Libs.testcontainers_mysql)

    // Kafka
    testImplementation(Libs.kafka_clients)

    // Database
    testImplementation(Libs.hikaricp)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.postgresql_driver)

    // Redis
    compileOnly(Libs.quarkus("redis-client"))
    testImplementation(Libs.redisson("redisson-quarkus-30"))
}
