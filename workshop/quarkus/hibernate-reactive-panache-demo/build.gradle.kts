plugins {
    id(Plugins.quarkus)
    kotlin("plugin.allopen")

    // NOTE: Quarkus 에서는 JPA 용 Entity를 open 으로 변경하는 것이 작동하지 않는다.
    // NOTE: 아럐 annotation("javax.persistence.Entity") 를 추가해주어야 한다
    kotlin("plugin.jpa")
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

    api(project(":bluetape4k-quarkus-core"))
    testImplementation(project(":bluetape4k-quarkus-tests"))
    testImplementation(project(":bluetape4k-junit5"))


    // Quarkus 라이브러리 (https://quarkus.io/extensions/)
    implementation(Libs.quarkus("hibernate-reactive-panache"))
    implementation(Libs.quarkus("hibernate-validator"))
    implementation(Libs.quarkus("kotlin"))
    implementation(Libs.quarkus("smallrye-openapi"))

    // 참고: https://quarkus.io/guides/hibernate-reactive-panache#testing
    implementation(Libs.quarkus("vertx"))
    testImplementation(Libs.quarkus("test-vertx"))
    testImplementation(Libs.quarkus("test-hibernate-reactive-panache"))

    // rest
    implementation(Libs.quarkus("rest"))
    implementation(Libs.quarkus("rest-kotlin"))
    implementation(Libs.quarkus("rest-jackson"))

    // see: https://quarkus.io/guides/datasource
    // rective datasource 는 mysql, postres 밖에 없다
    // implementation(Libs.quarkus("reactive-mysql-client"))
    implementation(Libs.quarkus("reactive-pg-client"))

    testImplementation(Libs.quarkus_junit5)
    testImplementation(Libs.rest_assured_kotlin)

    // coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    implementation(Libs.kotlinx_coroutines_reactive)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Bluetape4k
    implementation(project(":bluetape4k-jackson"))
    testImplementation(project(":bluetape4k-idgenerators"))
}
