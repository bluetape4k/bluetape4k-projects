package io.bluetape4k.utils

import java.util.*

/**
 * System Property 를 제공해주는 Object
 */
object Systemx {

    const val USER_DIR = "user.dir"
    const val USER_NAME = "user.name"
    const val USER_HOME = "user.home"
    const val JAVA_HOME = "java.home"
    const val TEMP_DIR = "java.io.tmpdir"
    const val OS_NAME = "os.name"
    const val OS_VERSION = "os.version"
    const val JAVA_VERSION = "java.version"
    const val JAVA_CLASS_VERION = "java.class.version"
    const val JAVA_SPECIFICATION_VERSION = "java.specification.version"
    const val JAVA_VENDOR = "java.vendor"
    const val JAVA_CLASSPATH = "java.class.path"
    const val PATH_SEPARATOR = "path.separator"
    const val HTTP_PROXY_HOST = "http.proxyHost"
    const val HTTP_PROXY_PORT = "http.proxyPort"
    const val HTTP_PROXY_USER = "http.proxyUser"
    const val HTTP_PROXY_PASSWORD = "http.proxyPassword"
    const val FILE_ENCODING = "file.encoding"
    const val SUN_BOOT_CLASS_PATH = "sun.boot.class.path"

    /** Runtime package */
    val runtimePackage: Package by lazy { Runtime::class.java.`package` }

    /** System Properties */
    val systemProps: Properties by lazy { System.getProperties() }

    /** CPU Core count */
    val processCount: Int by lazy { Runtime.getRuntime().availableProcessors() }

    /** JVM Compipler 정보 */
    val javaCompiler: String? by lazy { System.getProperty("java.compiler") }

    /** JVM 버전 */
    val javaVersion: String? by lazy {
        System.getProperty(JAVA_SPECIFICATION_VERSION) ?: runtimePackage.specificationVersion
    }

    /** JVM 구현 버전 */
    val javaImplementationVersion: String? by lazy { runtimePackage.implementationVersion }

    /** JVM 벤더 */
    val javaVendor: String? by lazy { runtimePackage.specificationVendor }

    /** JVM 벤더 URL */
    val javaVendorUrl: String? by lazy { System.getProperty("java.vendor.url") }

    /** JVM 구현 벤더  */
    val javaImplementationVendor: String? by lazy { runtimePackage.implementationVendor }

    /** JVM 구현 벤더 URL */
    val javaClassVersion: String? by lazy { System.getProperty(JAVA_CLASS_VERION) }

    /** JVM 라이브러리 경로 */
    val javaLibraryPath: String? by lazy { System.getProperty("java.library.path") }

    /** JVM 런타임 명 */
    val javaRuntimeName: String? by lazy { System.getProperty("java.runtime.name") }

    /** JVM 런타임 버전 */
    val javaRuntimeVersion: String? by lazy { System.getProperty("java.runtime.version") }

    /** JVM 사양 이름 */
    val javaSpecificationName: String? by lazy { System.getProperty("java.specification.name") }

    /** JVM 사양 벤더 명 */
    val javaSpecificationVendor: String? by lazy { System.getProperty("java.specification.vendor") }

    private val JavaFeatureVersion: Int? by lazy {
        val raw = javaVersion ?: return@lazy null
        if (raw.startsWith("1.")) raw.removePrefix("1.").substringBefore('.').toIntOrNull()
        else raw.substringBefore('.').toIntOrNull()
    }

    /** Java 1.6 인가? */
    val isJava6: Boolean by lazy { JavaFeatureVersion == 6 }

    /** Java 1.7 인가? */
    val isJava7: Boolean by lazy { JavaFeatureVersion == 7 }

    /** Java 1.8 인가? */
    val isJava8: Boolean by lazy { JavaFeatureVersion == 8 }

    /** Java 9 인가? */
    val isJava9: Boolean by lazy { JavaFeatureVersion == 9 }

    /** Java 10 인가? */
    val isJava10: Boolean by lazy { JavaFeatureVersion == 10 }

    /** Java 11 인가? */
    val isJava11: Boolean by lazy { JavaFeatureVersion == 11 }

    /** Java 17 인가? */
    val isJava17: Boolean by lazy { JavaFeatureVersion == 17 }

    /** Java 19 인가? */
    val isJava19: Boolean by lazy { JavaFeatureVersion == 19 }

    /** Java 21 인가? */
    val isJava21: Boolean by lazy { JavaFeatureVersion == 21 }

    /** Java 22 인가? */
    val isJava22: Boolean by lazy { JavaFeatureVersion == 22 }

    /** Java 23 인가? */
    val isJava23: Boolean by lazy { JavaFeatureVersion == 23 }

    /** Java 24 인가? */
    val isJava24: Boolean by lazy { JavaFeatureVersion == 24 }

    /** Java 25 인가? */
    val isJava25: Boolean by lazy { JavaFeatureVersion == 25 }

    /** JVM home directory */
    val javaHome: String? by lazy { System.getProperty("java.home") }

    val lineSeparator: String by lazy { System.lineSeparator() }

    val fileSeparator: String by lazy { java.nio.file.FileSystems.getDefault().separator }

    val pathSeparator: String by lazy { java.io.File.pathSeparator }

    val fileEncoding: String by lazy {
        java.nio.charset.Charset.defaultCharset().displayName() ?: Charsets.UTF_8.name()
    }

    /** 사용자 정보 */
    val userName: String? by lazy { System.getProperty(USER_NAME) }

    /** 사용자 홈 디렉토리 */
    val userHome: String? by lazy { System.getProperty(USER_HOME) }

    /** 사용자 현재 디렉토리 */
    val userDir: String? by lazy { System.getProperty(USER_DIR) }

    /** 사용자 언어 */
    val userCountry: String? by lazy {
        System.getProperty("user.country") ?: System.getProperty("user.region")
    }

    /** 임시 디렉토리 */
    val tempDir: String? by lazy { System.getProperty(TEMP_DIR) }

    /** 임시 디렉토리 */
    val javaIoTmpDir: String? by lazy { System.getProperty(TEMP_DIR) }

    /** OS 이름 */
    val osName: String? by lazy { System.getProperty(OS_NAME) }

    /** OS 버전 */
    val osVersion: String? by lazy { System.getProperty(OS_VERSION) }

    private val normalizedOsName: String by lazy { osName?.lowercase(Locale.ROOT).orEmpty() }

    /** Windows 운영 체제인가? */
    val isWindows: Boolean by lazy { normalizedOsName.contains("win") }

    /** MAC OSX 운영 체제인가? */
    val isMac: Boolean by lazy { normalizedOsName.contains("mac") }

    /** Solaris 운영 체제인가? */
    val isSolaris: Boolean by lazy { normalizedOsName.contains("sunos") }

    /** UNIX 운영 체제인가? */
    val isUnix: Boolean by lazy {
        normalizedOsName.contains("nix") ||
                normalizedOsName.contains("nux") ||
                normalizedOsName.contains("aix")
    }


    /**
     * 지정한 키[name]에 해당하는 시스템 속성 정보를 가져옵니다. 없다면 null 을 반환합니다.
     *
     * @param name 시스템 속성 key
     * @return 시스템 속성 값
     */
    fun getProp(name: String): String? = System.getProperty(name)

    /**
     * 환경설정 키[name]에 해당하는 값을 가져옵니다. 없다면 null 을 반환합니다.
     *
     * @param name 환경설정 Key
     * @return 환경설정 값
     */
    fun getenv(name: String): String? = System.getenv(name)

    /**
     * 모든 환경설정 정보를 가져옵니다.
     *
     * @return 환경설정 정보
     */
    fun getenv(): Map<String, String> = System.getenv()
}
