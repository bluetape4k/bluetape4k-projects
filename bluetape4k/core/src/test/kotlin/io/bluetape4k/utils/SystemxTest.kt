package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class SystemxTest {

    companion object: KLogging()

    @Test
    fun `자바 버전 조회`() {
        log.debug { "Java implementation version=${Systemx.javaImplementationVersion}" }
        log.debug { "JavaVersion=${Systemx.javaVersion}" }
        log.debug { "JavaHome=${Systemx.javaHome}" }
        Systemx.javaHome.shouldNotBeNull().shouldNotBeEmpty()
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
        envs["PATH"].shouldNotBeNull().shouldNotBeEmpty()

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

        (Systemx.isJava6 == (feature == 6)).shouldBeTrue()
        (Systemx.isJava7 == (feature == 7)).shouldBeTrue()
        (Systemx.isJava8 == (feature == 8)).shouldBeTrue()
        (Systemx.isJava9 == (feature == 9)).shouldBeTrue()
        (Systemx.isJava10 == (feature == 10)).shouldBeTrue()
        (Systemx.isJava11 == (feature == 11)).shouldBeTrue()
        (Systemx.isJava17 == (feature == 17)).shouldBeTrue()
        (Systemx.isJava19 == (feature == 19)).shouldBeTrue()
        (Systemx.isJava21 == (feature == 21)).shouldBeTrue()
        (Systemx.isJava22 == (feature == 22)).shouldBeTrue()
        (Systemx.isJava23 == (feature == 23)).shouldBeTrue()
    }

    @Test
    fun `OS 플래그는 소문자 정규화 판별과 일관된다`() {
        val os = (Systemx.osName.orEmpty()).lowercase()

        (Systemx.isWindows == os.contains("win")).shouldBeTrue()
        (Systemx.isMac == os.contains("mac")).shouldBeTrue()
        (Systemx.isSolaris == os.contains("sunos")).shouldBeTrue()
        (Systemx.isUnix == (os.contains("nix") || os.contains("nux") || os.contains("aix"))).shouldBeTrue()
    }
}
