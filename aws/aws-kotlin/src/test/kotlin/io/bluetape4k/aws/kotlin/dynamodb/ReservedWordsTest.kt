package io.bluetape4k.aws.kotlin.dynamodb

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class ReservedWordsTest {

    @Test
    fun `예약어 enum에 주요 DynamoDB 예약어가 포함되어 있다`() {
        val names = ReservedWords.entries.map { it.name }

        names shouldContain "SELECT"
        names shouldContain "TABLE"
        names shouldContain "INDEX"
        names shouldContain "KEY"
        names shouldContain "NAME"
        names shouldContain "STATUS"
        names shouldContain "VALUE"
    }

    @Test
    fun `예약어 enum의 name()으로 문자열 비교 가능하다`() {
        ReservedWords.SELECT.name shouldBeEqualTo "SELECT"
        ReservedWords.TABLE.name shouldBeEqualTo "TABLE"
    }

    @Test
    fun `예약어 목록에 중복이 없다`() {
        val entries = ReservedWords.entries
        val uniqueNames = entries.map { it.name }.toSet()

        (entries.size == uniqueNames.size).shouldBeTrue()
    }

    @Test
    fun `예약어 수가 AWS 문서 기준 573개 이상이다`() {
        // AWS DynamoDB 예약어 공식 목록은 573개
        (ReservedWords.entries.size >= 573).shouldBeTrue()
    }
}
