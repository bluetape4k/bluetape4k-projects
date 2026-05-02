dependencies {
    api(project(":bluetape4k-logging"))
    api(libs.exposed.core)
    // BigQueryContext 가 Database.connect(), transaction() 을 내부적으로 호출하므로 implementation 필요
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    api(libs.kotlinx.coroutines.core)
    api(libs.google.api.services.bigquery)

    // BigQueryContext.create() 가 H2 sqlGenDb 를 내부 생성하므로 런타임 classpath 에 필요하다.
    implementation(libs.h2.v2)

    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.gcloud)
}
