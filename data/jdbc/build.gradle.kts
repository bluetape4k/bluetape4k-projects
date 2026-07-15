plugins {
    kotlin("plugin.spring")
}

// NOTE: compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    compileOnly(bt4k.hikaricp)
    compileOnly(bt4k.tomcat.jdbc)

    // compileOnly(bt4k.agroal.pool)
    compileOnly(libs.agroal.spring.boot.starter)

    compileOnly("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testRuntimeOnly(libs.h2.v2)
    testImplementation(libs.testcontainers.mysql)
    testRuntimeOnly(bt4k.mysql.connector.j)
    // testRuntimeOnly(libs.mariadb.java.client)
}
