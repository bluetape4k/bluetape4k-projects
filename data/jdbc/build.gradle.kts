plugins {
    kotlin("plugin.spring")
}

// NOTE: compileOnly 나 runtimeOnly로 지정된 Dependency를 testImplementation 으로도 지정하도록 합니다.
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))

    compileOnly(Libs.hikaricp)
    compileOnly(Libs.tomcat_jdbc)

    compileOnly(Libs.agroal_pool)
    compileOnly(Libs.agroal_spring_boot_starter)

    compileOnly(Libs.springBootStarter("jdbc"))
    testImplementation(Libs.springBootStarter("test")) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testRuntimeOnly(Libs.h2_v2)
    testImplementation(Libs.testcontainers_mysql)
    testRuntimeOnly(Libs.mysql_connector_j)
    // testRuntimeOnly(Libs.mariadb_java_client)
}
