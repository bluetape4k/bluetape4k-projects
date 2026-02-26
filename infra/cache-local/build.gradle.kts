dependencies {
    api(project(":bluetape4k-cache"))

    // Local Java Cache providers
    api(Libs.caffeine)
    api(Libs.caffeine_jcache)
    api(Libs.cache2k_core)
    api(Libs.cache2k_jcache)
    api(Libs.ehcache)
    api(Libs.ehcache_clustered)
    api(Libs.ehcache_transactions)
}
