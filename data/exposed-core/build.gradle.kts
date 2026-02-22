configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(Libs.exposed_core)
    compileOnly(Libs.exposed_jdbc)
    compileOnly(Libs.exposed_dao)
    compileOnly(Libs.exposed_crypt)
    compileOnly(Libs.exposed_kotlin_datetime)
    compileOnly(Libs.exposed_java_time)
    compileOnly(Libs.exposed_json)
    compileOnly(Libs.exposed_money)

    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    // exposed-dao 모듈에서 idEquals, idHashCode 등 사용
    testImplementation(project(":bluetape4k-exposed-dao"))

    // Entity ID generators (ColumnExtensions에서 사용)
    api(project(":bluetape4k-idgenerators"))
    api(Libs.java_uuid_generator)

    //
    // Custom Column Types
    //

    // Compress column types
    compileOnly(project(":bluetape4k-io"))

    // Serializer (runtime for tests)
    testRuntimeOnly(Libs.kryo5)
    testRuntimeOnly(Libs.fory_kotlin)  // new Apache Fory

    // Compressors
    testRuntimeOnly(Libs.lz4_java)
    testRuntimeOnly(Libs.snappy_java)
    testRuntimeOnly(Libs.zstd_jni)

    // Encryption column types
    compileOnly(project(":bluetape4k-crypto"))
    testRuntimeOnly(Libs.jasypt)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mariadb)
    testImplementation(Libs.testcontainers_mysql)
    testImplementation(Libs.testcontainers_postgresql)

    // Database Drivers
    compileOnly(Libs.hikaricp)
    testRuntimeOnly(Libs.h2_v2)
    testRuntimeOnly(Libs.mariadb_java_client)
    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.postgresql_driver)
    testRuntimeOnly(Libs.pgjdbc_ng)
}
