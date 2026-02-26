plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-idgenerators"))

    compileOnly("javax.cache:cache-api:1.1.1")
    compileOnly(Libs.cache2k_jcache)
    compileOnly(Libs.caffeine_jcache)
    compileOnly(Libs.ehcache)
    compileOnly(Libs.redisson)
    compileOnly(Libs.hazelcast)
    compileOnly(Libs.ignite_core)
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    testFixturesApi(project(":bluetape4k-junit5"))
    testFixturesImplementation("javax.cache:cache-api:1.1.1")
    testFixturesImplementation(Libs.kotlinx_coroutines_test)
}
