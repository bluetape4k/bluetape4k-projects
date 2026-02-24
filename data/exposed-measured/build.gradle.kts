configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_dao)

    api(project(":bluetape4k-measured"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    // Database Drivers for exposed-jdbc-tests dialect matrix
    testRuntimeOnly(Libs.hikaricp)
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mariadb_java_client)
    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.pgjdbc_ng)
}
