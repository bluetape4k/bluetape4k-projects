configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.exposed.bom))
    api(libs.exposed.core)
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.exposed.dao)

    api(project(":bluetape4k-measured"))

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))

    // Database Drivers for exposed-jdbc-tests dialect matrix
    testRuntimeOnly(libs.hikaricp)
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)
}
