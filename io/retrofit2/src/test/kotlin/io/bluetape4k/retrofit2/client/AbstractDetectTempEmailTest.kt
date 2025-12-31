package io.bluetape4k.retrofit2.client

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.DetectTempEmailService
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

abstract class AbstractDetectTempEmailTest {

    companion object: KLogging() {
        protected val faker = Fakers.faker
        private const val REPEAT_SIZE = 5
    }

    protected abstract val callFactory: okhttp3.Call.Factory

    private val detectTempEmailApi: DetectTempEmailService.DetectTempEmailApi by lazy {
        retrofitOf(DetectTempEmailService.BASE_URL, callFactory, defaultJsonConverterFactory).service()
    }

    @Test
    fun `DetectTempEmailApi 인스턴스 생성`() {
        detectTempEmailApi.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `정식 이메일에 대한 임시 여부 검출하기`() = runTest {
        val email = faker.internet().emailAddress()
        val result = detectTempEmailApi.detect(email)
        log.debug { "Email=$email, disposable=${result.disposable}" }
        result.disposable.shouldBeFalse()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "mailnator.com",
            "mailinator.com",
            "guerrillamail.com",
            "sharklasers.com",
            "temp-mail.org",
            "tempail.com",
            "tempemail.net",
            "tempinbox.com",
            "temp-mail.org",
            "tempomail.fr",
            "temporarily.de",
            "temporarioemail.com.br",
            "temporaryemail.net",
            "tempthe.net",
            "throwawayemailaddress.com",
            "trashmail.com",
            "yopmail.com"
        ]
    )
    fun `임시 이메일에 대한 임시 여부 검출하기`(emailDomain: String) = runTest {
        val email = faker.credentials().username() + "@" + emailDomain
        val result = detectTempEmailApi.detect(email)
        log.debug { "Email=$email, disposable=${result.disposable}" }
        result.disposable.shouldBeTrue()
    }

}
