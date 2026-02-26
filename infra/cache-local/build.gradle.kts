dependencies {
    api(project(":bluetape4k-cache-core"))

    // Local Java Cache providers
    api(Libs.caffeine)
    api(Libs.caffeine_jcache)
    api(Libs.cache2k_core)
    api(Libs.cache2k_jcache)
    api(Libs.ehcache)
    api(Libs.ehcache_clustered)
    api(Libs.ehcache_transactions)

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly("javax.cache:cache-api:1.1.1")
    compileOnly(Libs.kotlinx_coroutines_core)

    testImplementation(testFixtures(project(":bluetape4k-cache-core")))
    testImplementation(project(":bluetape4k-junit5"))
}
