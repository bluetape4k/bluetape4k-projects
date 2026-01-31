package io.bluetape4k.naivebayes

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import java.time.LocalDate

class NaiveBayesClassifierKoreanTest: AbstractNaiveBayesClassifierTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `메일 제목으로 spam 분류하기`() {

        val emails = fastListOf(
            Email("로켓펀치 누적 프로필 20만 돌파 기념 인포그래픽 공개! ", isSpam = false),
            Email("(광고)마이크로소프트 IoT와 함께 하는 에너지 절약 기술의 미래 웨비나에 지금 등록하세요", isSpam = false),
            Email("[쿠팡] 배*혁님 주문하신 내역을 확인해주세요.", isSpam = false),
            Email("G마켓 이용약관 개정 안내", isSpam = false),

            Email("스스로를 지키세요. SWAT 조끼, 쇳조각, 미니 테이저총, 전투 장비를 월간 재고 정리 할인으로 저렴하게 구매하세요...", isSpam = true),
            Email("걸음마 아기, 신생아 및 어린이용: 스웨터, 장난감, 신발, 놀이 텐트 등 오늘 50-90% 할인!", isSpam = true),
            Email("오늘만, 어디에나 어울리는 캐주얼 바지를 90% 할인가에 쇼핑하세요.", isSpam = true),
            Email("[해피포인트] 회원 이용약관 개정 안내 (2019년 10월 1일부)", isSpam = true)
        )

        val nbc = naiveBayesClassifierOf(
            emails,
            featuresSelector = { it.message.tokenize().toUnifiedSet() },
            categorySelector = { it.isSpam }
        )

        // TEST 1 (스팸)
        val input = "전투 장비를 창고 대방출 90% 할인가에 쇼핑하세요".tokenize().toUnifiedSet()
        val isSpam = nbc.predict(input)!!
        isSpam.shouldBeTrue()

        // TEST 2 (스팸 아님)
        val input2 = "로켓펀치 이용약관 개정".tokenize().toUnifiedSet()
        val isSpam2 = nbc.predict(input2)!!
        isSpam2.shouldBeFalse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `한글 명세서로 분류하기`() {

        val bankTransactions = fastListOf(
            BankTransaction(
                date = LocalDate.of(2018, 3, 13),
                amount = 12.69,
                memo = "새벽배송 신선 양배추",
                category = "GROCERY"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 13),
                amount = 4.64,
                memo = "네슬레 커피믹스 커피 330개",
                category = "COFFEE"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 13),
                amount = 14.23,
                memo = "아마존 세일 전자기기",
                category = "ELECTRONICS"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 13),
                amount = 7.99,
                memo = "카프카 스트림 시작하기",
                category = "BOOK"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 10),
                amount = 5.40,
                memo = "넷플릭스 비디오 #203",
                category = "ENTERTAINMENT"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 10),
                amount = 61.27,
                memo = "새벽배송 신선식품 샐러드",
                category = "GROCERY"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 12),
                amount = 61.27,
                memo = "스타벅스 아이스아메리카노 커피믹스",
                category = "COFFEE"
            ),
            BankTransaction(
                date = LocalDate.of(2018, 3, 7),
                amount = 2.29,
                memo = "비디오 온 디맨드 가입신청",
                category = "ENTERTAINMENT"
            )
        )

        val nbc = naiveBayesClassifierOf(
            bankTransactions,
            featuresSelector = { it.memo.tokenize().toUnifiedSet() },
            categorySelector = { it.category ?: "undefined" }
        )

        // TEST 1
        val input1 = BankTransaction(
            date = LocalDate.of(2018, 3, 31),
            amount = 13.99,
            memo = "넷플릭스 비디오 #123"
        )
        val result1 = nbc.predictWithProbability(input1.memo.tokenize().toUnifiedSet()).shouldNotBeNull()
        log.debug { "result1=$result1" }
        result1.category shouldBeEqualTo "ENTERTAINMENT"

        // TEST 2
        val input2 = BankTransaction(
            date = LocalDate.of(2018, 3, 6),
            amount = 17.21,
            memo = "커피 까페모카 2잔"
        )
        val result2 = nbc.predictWithProbability(input2.memo.tokenize().toUnifiedSet()).shouldNotBeNull()
        log.debug { "result2=$result2" }
        result2.category shouldBeEqualTo "COFFEE"
    }
}
