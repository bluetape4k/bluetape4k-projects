dependencies {
    api(project(":bluetape4k-cache-core"))
    api(project(":bluetape4k-cache-local"))
    api(project(":bluetape4k-cache-redisson"))

    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly("javax.cache:cache-api:1.1.1")
    compileOnly(Libs.kotlinx_coroutines_core)
}
