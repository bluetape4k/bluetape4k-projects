package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import io.bluetape4k.rule.core.ruleEngine
import io.bluetape4k.rule.support.ActivationRuleGroup
import io.bluetape4k.rule.support.ConditionalRuleGroup
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Groovy 엔진의 다양한 사용 예제입니다.
 *
 * Groovy는 Java 상위호환 동적 타이핑 언어로, 클로저·GString·safe navigation(?.)·
 * 컬렉션 리터럴 등 풍부한 표현력을 제공합니다.
 * **바이트코드로 컴파일**되어 실행되며, 복잡한 룰 로직에 적합합니다.
 *
 * **Groovy 표현식 핵심 규칙:**
 * - Facts의 키가 직접 변수로 바인딩됨 → `facts.get("amount")` 대신 `amount`로 접근
 * - 세미콜론 선택사항, 타입 선언 선택사항
 * - 새 변수 대입 → 자동으로 Facts에 반영됨
 */
class GroovyRuleExampleTest {

    companion object: KLogging()

    // =========================================================================
    // 1. 기본 산술 연산
    // =========================================================================

    @Nested
    inner class `산술 연산` {

        @Test
        fun `정수 비교 - Groovy는 변수명으로 직접 접근`() {
            val condition = GroovyCondition("score >= 60")

            condition.evaluate(Facts.of("score" to 60)).shouldBeTrue()
            condition.evaluate(Facts.of("score" to 59)).shouldBeFalse()
            condition.evaluate(Facts.of("score" to 100)).shouldBeTrue()
        }

        @Test
        fun `실수 계산 - 할인 금액 계산`() {
            val action = GroovyAction("discountAmount = amount * rate / 100.0")

            val facts = Facts.of("amount" to 50000, "rate" to 15)
            action.execute(facts)

            facts.get<Number>("discountAmount")!!.toDouble() shouldBeEqualTo 7500.0
        }

        @Test
        fun `범위 연산자 - Groovy의 in 연산자 사용`() {
            val condition = GroovyCondition("age in 18..65")

            condition.evaluate(Facts.of("age" to 17)).shouldBeFalse()
            condition.evaluate(Facts.of("age" to 18)).shouldBeTrue()
            condition.evaluate(Facts.of("age" to 40)).shouldBeTrue()
            condition.evaluate(Facts.of("age" to 65)).shouldBeTrue()
            condition.evaluate(Facts.of("age" to 66)).shouldBeFalse()
        }

        @Suppress("DANGEROUS_CHARACTERS")
        @Test
        fun `거듭제곱 연산 - Groovy의 ** 연산자`() {
            val action = GroovyAction("result = base ** exponent")

            val facts = Facts.of("base" to 2, "exponent" to 10)
            action.execute(facts)

            facts.get<Number>("result")!!.toLong() shouldBeEqualTo 1024L
        }
    }

    // =========================================================================
    // 2. 문자열 처리
    // =========================================================================

    @Nested
    inner class `문자열 처리` {

        @Test
        fun `GString 보간 - 동적 메시지 생성`() {
            // GroovyAction이 GString → String 자동 변환하므로 .toString() 불필요
            val action = GroovyAction(
                """greeting = "Hello, ${"\${role}"} ${"\${name}"}!" """
            )

            val facts = Facts.of("name" to "Alice", "role" to "Manager")
            action.execute(facts)

            facts.get<String>("greeting") shouldBeEqualTo "Hello, Manager Alice!"
        }

        @Test
        fun `safe navigation - null 안전 접근`() {
            val condition = GroovyCondition("name?.toUpperCase() == 'ALICE'")

            condition.evaluate(Facts.of("name" to "alice")).shouldBeTrue()
            condition.evaluate(Facts.of("name" to "bob")).shouldBeFalse()
        }

        @Test
        fun `Elvis 연산자 - null이면 기본값`() {
            // NullSafeBinding 덕분에 Facts에 없는 키를 참조해도 null 반환
            // → Elvis 연산자가 자연스럽게 동작
            val action = GroovyAction("displayName = name ?: 'Guest'")

            val facts1 = Facts.of("dummy" to 0)
            action.execute(facts1)
            facts1.get<String>("displayName") shouldBeEqualTo "Guest"

            val facts2 = Facts.of("name" to "Alice")
            action.execute(facts2)
            facts2.get<String>("displayName") shouldBeEqualTo "Alice"
        }

        @Test
        fun `정규식 매칭 - 이메일 검증`() {
            val condition = GroovyCondition(
                "email ==~ /^[\\w.+-]+@[\\w-]+\\.[\\w.]+$/"
            )

            condition.evaluate(Facts.of("email" to "alice@company.com")).shouldBeTrue()
            condition.evaluate(Facts.of("email" to "invalid-email")).shouldBeFalse()
        }

        @Test
        fun `문자열 multiply - 반복 문자열 생성`() {
            val action = GroovyAction("stars = '*' * rating")

            val facts = Facts.of("rating" to 5)
            action.execute(facts)

            facts.get<String>("stars") shouldBeEqualTo "*****"
        }
    }

    // =========================================================================
    // 3. 컬렉션 처리 (Groovy의 강점)
    // =========================================================================

    @Nested
    inner class `컬렉션 처리` {

        @Test
        fun `리스트 리터럴과 contains`() {
            val condition = GroovyCondition(
                "category in ['ELECTRONICS', 'BOOKS', 'CLOTHING']"
            )

            condition.evaluate(Facts.of("category" to "BOOKS")).shouldBeTrue()
            condition.evaluate(Facts.of("category" to "FOOD")).shouldBeFalse()
        }

        @Test
        fun `collect - 리스트 변환 (map 연산)`() {
            val action = GroovyAction(
                """
                def prices = [100, 200, 300, 400, 500]
                discounted = prices.collect { it * (1 - discountRate / 100.0) }
                totalAfterDiscount = discounted.sum()
                """.trimIndent()
            )

            val facts = Facts.of("discountRate" to 10)
            action.execute(facts)

            facts.get<Number>("totalAfterDiscount")!!.toDouble() shouldBeEqualTo 1350.0
        }

        @Test
        fun `findAll - 조건 필터링`() {
            val action = GroovyAction(
                """
                def scores = [85, 92, 67, 78, 95, 43, 88]
                passingScores = scores.findAll { it >= 70 }
                passCount = passingScores.size()
                average = passingScores.sum() / passCount
                """.trimIndent()
            )

            val facts = Facts.empty()
            action.execute(facts)

            facts.get<Number>("passCount")!!.toInt() shouldBeEqualTo 5
        }

        @Test
        fun `groupBy - 그룹핑`() {
            val action = GroovyAction(
                """
                def items = [
                    [name: 'Apple', category: 'FRUIT'],
                    [name: 'Carrot', category: 'VEGETABLE'],
                    [name: 'Banana', category: 'FRUIT'],
                    [name: 'Broccoli', category: 'VEGETABLE'],
                    [name: 'Cherry', category: 'FRUIT']
                ]
                grouped = items.groupBy { it.category }
                fruitCount = grouped['FRUIT'].size()
                vegCount = grouped['VEGETABLE'].size()
                """.trimIndent()
            )

            val facts = Facts.empty()
            action.execute(facts)

            facts.get<Number>("fruitCount")!!.toInt() shouldBeEqualTo 3
            facts.get<Number>("vegCount")!!.toInt() shouldBeEqualTo 2
        }

        @Test
        fun `spread 연산자 - 중첩 프로퍼티 추출`() {
            val action = GroovyAction(
                """
                def users = [
                    [name: 'Alice', age: 30],
                    [name: 'Bob', age: 25],
                    [name: 'Charlie', age: 35]
                ]
                names = users*.name
                avgAge = users*.age.sum() / users.size()
                """.trimIndent()
            )

            val facts = Facts.empty()
            action.execute(facts)

            @Suppress("UNCHECKED_CAST")
            val names = facts.get<List<String>>("names")!!
            names shouldContain "Alice"
            names shouldContain "Bob"
            names shouldContain "Charlie"
            facts.get<Number>("avgAge")!!.toInt() shouldBeEqualTo 30
        }
    }

    // =========================================================================
    // 4. 조건 분기 로직
    // =========================================================================

    @Nested
    inner class `조건 분기` {

        @Test
        fun `switch-case 등급 분류`() {
            val action = GroovyAction(
                """
                switch (score) {
                    case 90..100: grade = 'A'; break
                    case 80..89:  grade = 'B'; break
                    case 70..79:  grade = 'C'; break
                    case 60..69:  grade = 'D'; break
                    default:      grade = 'F'
                }
                """.trimIndent()
            )

            val facts1 = Facts.of("score" to 95)
            action.execute(facts1)
            facts1.get<String>("grade") shouldBeEqualTo "A"

            val facts2 = Facts.of("score" to 73)
            action.execute(facts2)
            facts2.get<String>("grade") shouldBeEqualTo "C"

            val facts3 = Facts.of("score" to 45)
            action.execute(facts3)
            facts3.get<String>("grade") shouldBeEqualTo "F"
        }

        @Test
        fun `세금 계산 - 누진세 구간`() {
            val action = GroovyAction(
                """
                def tax
                switch (income) {
                    case { it > 100000 }: tax = income * 0.35; break
                    case { it > 50000 }:  tax = income * 0.25; break
                    case { it > 20000 }:  tax = income * 0.15; break
                    default:              tax = 0.0
                }
                taxAmount = tax
                """.trimIndent()
            )

            val low = Facts.of("income" to 15000)
            action.execute(low)
            low.get<Number>("taxAmount")!!.toDouble() shouldBeEqualTo 0.0

            val mid = Facts.of("income" to 60000)
            action.execute(mid)
            mid.get<Number>("taxAmount")!!.toDouble() shouldBeEqualTo 15000.0

            val high = Facts.of("income" to 200000)
            action.execute(high)
            high.get<Number>("taxAmount")!!.toDouble() shouldBeEqualTo 70000.0
        }
    }

    // =========================================================================
    // 5. 복합 룰 시나리오 (실무 예제)
    // =========================================================================

    @Nested
    inner class `실무 시나리오` {

        @Test
        fun `주문 할인 정책 - 여러 룰 순차 적용`() {
            val basicDiscount = GroovyRule(name = "basicDiscount", priority = 1)
                .whenever("amount >= 10000")
                .then(
                    """
                    discountRate = 5.0
                    discountAmount = amount * 0.05
                    """.trimIndent()
                )

            val vipDiscount = GroovyRule(name = "vipDiscount", priority = 2)
                .whenever("memberType == 'VIP'")
                .then(
                    """
                    def currentRate = discountRate ?: 0.0
                    discountRate = currentRate + 5.0
                    discountAmount = amount * discountRate / 100.0
                    """.trimIndent()
                )

            val freeShipping = GroovyRule(name = "freeShipping", priority = 3)
                .whenever("amount >= 50000")
                .then("freeShipping = true")

            val engine = DefaultRuleEngine()
            val facts = Facts.of("amount" to 60000, "memberType" to "VIP")
            engine.fire(ruleSetOf(basicDiscount, vipDiscount, freeShipping), facts)

            facts.get<Number>("discountRate")!!.toDouble() shouldBeEqualTo 10.0
            facts.get<Number>("discountAmount")!!.toDouble() shouldBeEqualTo 6000.0
            facts.get<Boolean>("freeShipping")!!.shouldBeTrue()
        }

        @Test
        fun `회원 등급 분류 - ActivationRuleGroup`() {
            val platinum = GroovyRule(name = "platinum", priority = 1)
                .whenever("totalPurchase >= 1000000")
                .then("tier = 'PLATINUM'")

            val gold = GroovyRule(name = "gold", priority = 2)
                .whenever("totalPurchase >= 500000")
                .then("tier = 'GOLD'")

            val silver = GroovyRule(name = "silver", priority = 3)
                .whenever("totalPurchase >= 100000")
                .then("tier = 'SILVER'")

            val bronze = GroovyRule(name = "bronze", priority = 4)
                .whenever("totalPurchase >= 0")
                .then("tier = 'BRONZE'")

            val group = ActivationRuleGroup("tierClassification")
            group.addRule(platinum)
            group.addRule(gold)
            group.addRule(silver)
            group.addRule(bronze)

            val engine = DefaultRuleEngine()

            val facts1 = Facts.of("totalPurchase" to 750000)
            engine.fire(ruleSetOf(group), facts1)
            facts1.get<String>("tier") shouldBeEqualTo "GOLD"

            val facts2 = Facts.of("totalPurchase" to 50000)
            engine.fire(ruleSetOf(group), facts2)
            facts2.get<String>("tier") shouldBeEqualTo "BRONZE"
        }

        @Test
        fun `배송비 계산 - 무게와 거리 기반`() {
            val shippingRule = GroovyRule(name = "shipping", priority = 1)
                .whenever("weight > 0")
                .then(
                    """
                    def baseCost = weight * 100
                    def distanceCost = distance * 50
                    def expressFee = (express ?: false) ? 3000.0 : 0.0
                    shippingCost = baseCost + distanceCost + expressFee
                    """.trimIndent()
                )

            val engine = DefaultRuleEngine()

            val facts1 = Facts.of("weight" to 2.5, "distance" to 200, "express" to false)
            engine.fire(ruleSetOf(shippingRule), facts1)
            facts1.get<Number>("shippingCost")!!.toDouble() shouldBeEqualTo 10250.0

            val facts2 = Facts.of("weight" to 1.0, "distance" to 100, "express" to true)
            engine.fire(ruleSetOf(shippingRule), facts2)
            facts2.get<Number>("shippingCost")!!.toDouble() shouldBeEqualTo 8100.0
        }

        @Test
        fun `ConditionalRuleGroup - 리더 룰이 통과하면 나머지 실행`() {
            // 리더: 주문이 유효한가?
            val validOrder = GroovyRule(name = "validOrder", priority = 1)
                .whenever("amount > 0 && items > 0")
                .then("orderValid = true")

            // 나머지: 유효할 때만 실행됨
            val applyTax = GroovyRule(name = "applyTax", priority = 2)
                .whenever("amount > 0")
                .then("taxAmount = amount * 0.1")

            val applyPoints = GroovyRule(name = "applyPoints", priority = 3)
                .whenever("amount > 0")
                .then("points = (int)(amount / 1000)")

            val group = ConditionalRuleGroup("orderProcessing")
            group.addRule(validOrder)
            group.addRule(applyTax)
            group.addRule(applyPoints)

            val engine = DefaultRuleEngine()

            // 유효한 주문
            val facts1 = Facts.of("amount" to 50000, "items" to 3)
            engine.fire(ruleSetOf(group), facts1)
            facts1.get<Boolean>("orderValid")!!.shouldBeTrue()
            facts1.get<Number>("taxAmount")!!.toDouble() shouldBeEqualTo 5000.0
            facts1.get<Number>("points")!!.toInt() shouldBeEqualTo 50

            // 유효하지 않은 주문 (items=0) → 아무것도 실행 안 됨
            val facts2 = Facts.of("amount" to 50000, "items" to 0)
            engine.fire(ruleSetOf(group), facts2)
            facts2.containsKey("orderValid").shouldBeFalse()
            facts2.containsKey("taxAmount").shouldBeFalse()
        }

        @Test
        fun `skipOnFirstAppliedRule - 첫 매칭 후 중단`() {
            val highPriority = GroovyRule(name = "highDiscount", priority = 1)
                .whenever("amount >= 100000")
                .then("discount = 30")

            val midPriority = GroovyRule(name = "midDiscount", priority = 2)
                .whenever("amount >= 50000")
                .then("discount = 15")

            val lowPriority = GroovyRule(name = "lowDiscount", priority = 3)
                .whenever("amount >= 10000")
                .then("discount = 5")

            val engine = ruleEngine { skipOnFirstAppliedRule = true }

            // 75000원 → midDiscount만 적용되고 lowDiscount는 스킵
            val facts = Facts.of("amount" to 75000)
            engine.fire(ruleSetOf(highPriority, midPriority, lowPriority), facts)

            facts.get<Number>("discount")!!.toInt() shouldBeEqualTo 15
        }
    }

    // =========================================================================
    // 6. Groovy 고급 기능
    // =========================================================================

    @Nested
    inner class `고급 기능` {

        @Test
        fun `클로저 - 복잡한 데이터 변환`() {
            val action = GroovyAction(
                """
                def orders = [
                    [product: 'Laptop', price: 1500000, qty: 1],
                    [product: 'Mouse',  price: 35000,   qty: 3],
                    [product: 'Monitor', price: 500000, qty: 2]
                ]
                totalAmount = orders.collect { it.price * it.qty }.sum()
                itemCount = orders.collect { it.qty }.sum()
                avgPrice = totalAmount / itemCount
                """.trimIndent()
            )

            val facts = Facts.empty()
            action.execute(facts)

            facts.get<Number>("totalAmount")!!.toLong() shouldBeEqualTo 2605000L
            facts.get<Number>("itemCount")!!.toInt() shouldBeEqualTo 6
        }

        @Test
        fun `맵 리터럴 - 복합 결과 생성`() {
            val action = GroovyAction(
                """
                def result = [:]
                result.status = amount > 0 ? 'APPROVED' : 'REJECTED'
                result.tier = amount >= 100000 ? 'PREMIUM' : 'STANDARD'
                result.freeShipping = amount >= 50000
                orderResult = result
                """.trimIndent()
            )

            val facts = Facts.of("amount" to 80000)
            action.execute(facts)

            @Suppress("UNCHECKED_CAST")
            val result = facts.get<Map<String, Any>>("orderResult")!!
            result["status"] shouldBeEqualTo "APPROVED"
            result["tier"] shouldBeEqualTo "STANDARD"
            result["freeShipping"] shouldBe true
        }

        @Test
        fun `정규식과 문자열 처리 - 데이터 정제`() {
            val action = GroovyAction(
                """
                def raw = rawPhone.replaceAll(/[^0-9]/, '')
                if (raw.length() == 11 && raw.startsWith('010')) {
                    phoneValid = true
                    formattedPhone = raw[0..2] + '-' + raw[3..6] + '-' + raw[7..10]
                } else {
                    phoneValid = false
                    formattedPhone = ''
                }
                """.trimIndent()
            )

            val facts1 = Facts.of("rawPhone" to "010-1234-5678")
            action.execute(facts1)
            facts1.get<Boolean>("phoneValid")!!.shouldBeTrue()
            facts1.get<String>("formattedPhone") shouldBeEqualTo "010-1234-5678"

            val facts2 = Facts.of("rawPhone" to "02-555-1234")
            action.execute(facts2)
            facts2.get<Boolean>("phoneValid")!!.shouldBeFalse()
        }

        @Test
        fun `날짜 처리 - 만료 여부 확인`() {
            val condition = GroovyCondition(
                """
                import java.time.LocalDate
                def expiry = LocalDate.parse(expiryDate)
                expiry.isAfter(LocalDate.now())
                """.trimIndent()
            )

            condition.evaluate(Facts.of("expiryDate" to "2099-12-31")).shouldBeTrue()
            condition.evaluate(Facts.of("expiryDate" to "2020-01-01")).shouldBeFalse()
        }

        @Test
        fun `복리 이자 계산 - times 반복`() {
            val action = GroovyAction(
                """
                def amount = (double) principal
                years.times { amount *= (1 + annualRate / 100.0) }
                finalAmount = Math.round(amount * 100.0) / 100.0
                """.trimIndent()
            )

            val facts = Facts.of("principal" to 10000000, "annualRate" to 5, "years" to 3)
            action.execute(facts)

            facts.get<Number>("finalAmount")!!.toDouble() shouldBeEqualTo 11576250.0
        }

        @Test
        fun `팩토리얼 계산 - inject 활용`() {
            val action = GroovyAction(
                """
                factorial = (2..n).inject(1L) { acc, i -> acc * i }
                """.trimIndent()
            )

            val facts = Facts.of("n" to 10)
            action.execute(facts)

            facts.get<Number>("factorial")!!.toLong() shouldBeEqualTo 3628800L
        }
    }

    // =========================================================================
    // 7. Janino vs Groovy 동일 로직 비교
    // =========================================================================

    @Nested
    inner class `Janino와 비교` {

        @Test
        fun `동일한 할인 룰 - Groovy가 훨씬 간결`() {
            // Janino: 명시적 타입 캐스팅 필요
            // JaninoCondition("((Integer)facts.get(\"amount\")).intValue() > 1000")

            // Groovy: 변수명으로 직접 접근
            val condition = GroovyCondition("amount > 1000")
            val action = GroovyAction("discount = true")

            val facts = Facts.of("amount" to 1500)
            condition.evaluate(facts).shouldBeTrue()
            action.execute(facts)
            facts.get<Boolean>("discount")!!.shouldBeTrue()
        }

        @Test
        fun `동일한 등급 분류 로직 비교`() {
            // Groovy는 switch + range로 매우 간결
            val action = GroovyAction(
                """
                switch (score) {
                    case 90..100: grade = 'A'; break
                    case 80..89:  grade = 'B'; break
                    case 70..79:  grade = 'C'; break
                    default:      grade = 'F'
                }
                """.trimIndent()
            )

            val facts = Facts.of("score" to 85)
            action.execute(facts)
            facts.get<String>("grade") shouldBeEqualTo "B"
        }
    }
}
