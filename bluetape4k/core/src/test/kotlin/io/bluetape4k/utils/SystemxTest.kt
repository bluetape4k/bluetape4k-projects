package io.bluetape4k.utils

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class SystemxTest {

    companion object: KLogging()

    @Test
    fun `자바 버전 조회`() {
        log.debug { "Java implementation version=${Systemx.javaImplementationVersion}" }
        log.debug { "JavaVersion=${Systemx.javaVersion}" }
        log.debug { "JavaHome=${Systemx.javaHome}" }

        Systemx.javaHome.shouldNotBeEmpty()
    }

    @Test
    fun `시스템 설정 정보`() {
        log.debug { "Line separator = ${Systemx.lineSeparator}" }
        log.debug { "File separator = ${Systemx.fileSeparator}" }
        log.debug { "Path separator = ${Systemx.pathSeparator}" }
        log.debug { "File encoding = ${Systemx.fileEncoding}" }

        log.debug { "Temp Dir = ${Systemx.tempDir}" }
        log.debug { "User Dir = ${Systemx.userDir}" }
    }

    @Test
    fun `시스템 설정 얻기`() {
        Systemx.getProp("line.separator") shouldBeEqualTo Systemx.lineSeparator
        Systemx.getProp("java.io.tmpdir") shouldBeEqualTo Systemx.tempDir
    }

    @Test
    fun `System Env 값 얻기`() {
        val envs = Systemx.getenv()
        log.debug { "PATH=${envs["PATH"]}" }
        envs["PATH"].shouldNotBeEmpty()

        Systemx.getenv("PATH") shouldBeEqualTo envs["PATH"]
    }

    @Test
    fun `대표 시스템 속성 매핑은 일치한다`() {
        Systemx.getProp(Systemx.USER_HOME) shouldBeEqualTo Systemx.userHome
        Systemx.getProp(Systemx.USER_DIR) shouldBeEqualTo Systemx.userDir
        Systemx.getProp(Systemx.JAVA_HOME) shouldBeEqualTo Systemx.javaHome
    }

    @Test
    fun `없는 환경 변수는 null을 반환한다`() {
        Systemx.getenv("BLUETAPE4K_NOT_EXISTS_ENV").shouldBeNull()
    }

    @Test
    fun `Java feature 버전 플래그는 런타임과 일관된다`() {
        val feature = Runtime.version().feature()

        Systemx.isJava17 shouldBeEqualTo (feature == 17)
        Systemx.isJava19 shouldBeEqualTo (feature == 19)
        Systemx.isJava21 shouldBeEqualTo (feature == 21)
        Systemx.isJava22 shouldBeEqualTo (feature == 22)
        Systemx.isJava23 shouldBeEqualTo (feature == 23)
        Systemx.isJava24 shouldBeEqualTo (feature == 24)
        Systemx.isJava25 shouldBeEqualTo (feature == 25)
    }

    @Test
    fun `OS 플래그는 소문자 정규화 판별과 일관된다`() {
        val os = (Systemx.osName.orEmpty()).lowercase()

        Systemx.isWindows shouldBeEqualTo (os.contains("win"))
        Systemx.isMac shouldBeEqualTo (os.contains("mac"))
        Systemx.isSolaris shouldBeEqualTo (os.contains("sunos"))
        Systemx.isUnix shouldBeEqualTo (os.contains("nix") || os.contains("nux") || os.contains("aix"))
    }
}
