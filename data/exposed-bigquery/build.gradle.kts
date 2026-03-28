dependencies {
    api(project(":bluetape4k-logging"))
    api(Libs.exposed_core)
    // BigQueryContext 가 Database.connect(), transaction() 을 내부적으로 호출하므로 implementation 필요
    implementation(Libs.exposed_jdbc)
    implementation(Libs.exposed_java_time)
    api(Libs.kotlinx_coroutines_core)
    api(Libs.google_api_services_bigquery)

    // BigQueryContext.create() 가 H2 sqlGenDb 를 내부 생성하므로 런타임 classpath 에 필요하다.
    implementation(Libs.h2_v2)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_gcloud)
}
