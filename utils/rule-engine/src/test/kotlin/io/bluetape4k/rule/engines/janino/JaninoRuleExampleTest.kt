package io.bluetape4k.rule.engines.janino

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.core.DefaultRuleEngine
import io.bluetape4k.rule.core.ruleEngine
import io.bluetape4k.rule.support.ActivationRuleGroup
import io.bluetape4k.rule.support.UnitRuleGroup
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Janino 엔진의 다양한 사용 예제입니다.
 *
 * Janino는 Java 표현식을 런타임에 **바이트코드로 컴파일**하여 실행합니다.
 * 인터프리터 방식 대비 네이티브에 가까운 속도를 제공하므로,
 * 대량 룰 반복 평가(가격 계산, 유효성 검증, 할인 정책 등)에 적합합니다.
 *
 * **Janino 표현식 핵심 규칙:**
 * - 파라미터 `facts`는 `Map<String, Object>` 타입
 * - `facts.get("key")` → `Object` 반환, 사용 전 명시적 캐스팅 필수
 * - Condition: boolean을 반환하는 Java 표현식
 * - Action: Java 문장(statement) 블록, 세미콜론(;) 필수
 */
class JaninoRuleExampleTest {

    companion object: KLogging()

    // =========================================================================
    // 1. 기본 산술 연산
    // =========================================================================

    @Nested
    inner class `산술 연산` {

        @Test
        fun `정수 비교 - 값이 임계값 초과인지 검사`() {
            val condition = JaninoCondition(
                "((Integer)facts.get(\"score\")).intValue() >= 60"
            )

            condition.evaluate(Facts.of("score" to 60)).shouldBeTrue()
            condition.evaluate(Facts.of("score" to 59)).shouldBeFalse()
            condition.evaluate(Facts.of("score" to 100)).shouldBeTrue()
        }

        @Test
        fun `실수 계산 - 할인 금액 계산 후 facts에 저장`() {
            val action = JaninoAction(
                """
                double amount = ((Number)facts.get("amount")).doubleValue();
                double rate = ((Number)facts.get("rate")).doubleValue();
                facts.put("discountAmount", Double.valueOf(amount * rate / 100.0));
                """.trimIndent()
            )

            val facts = Facts.of("amount" to 50000, "rate" to 15)
            action.execute(facts)

            val discount = facts.get<Double>("discountAmount")
            discount.shouldNotBeNull()
            discount shouldBeEqualTo 7500.0
        }

        @Test
        fun `범위 검사 - 값이 특정 범위 내인지`() {
            // ExpressionEvaluator는 변수 선언(statement)을 지원하지 않으므로
            // 인라인 표현식으로 작성해야 함
            val condition = JaninoCondition(
                "((Integer)facts.get(\"age\")).intValue() >= 18 && ((Integer)facts.get(\"age\")).intValue() <= 65"
            )

            condition.evaluate(Facts.of("age" to 17)).shouldBeFalse()
            condition.evaluate(Facts.of("age" to 18)).shouldBeTrue()
            condition.evaluate(Facts.of("age" to 40)).shouldBeTrue()
            condition.evaluate(Facts.of("age" to 65)).shouldBeTrue()
            condition.evaluate(Facts.of("age" to 66)).shouldBeFalse()
        }
    }

    // =========================================================================
    // 2. 문자열 처리
    // =========================================================================

    @Nested
    inner class `문자열 처리` {

        @Test
        fun `문자열 equals 비교`() {
            val condition = JaninoCondition(
                "\"VIP\".equals(facts.get(\"memberType\"))"
            )

            condition.evaluate(Facts.of("memberType" to "VIP")).shouldBeTrue()
            condition.evaluate(Facts.of("memberType" to "NORMAL")).shouldBeFalse()
        }

        @Test
        fun `문자열 contains 검사`() {
            val condition = JaninoCondition(
                "((String)facts.get(\"email\")).contains(\"@company.com\")"
            )

            condition.evaluate(Facts.of("email" to "alice@company.com")).shouldBeTrue()
            condition.evaluate(Facts.of("email" to "bob@gmail.com")).shouldBeFalse()
        }

        @Test
        fun `문자열 결합 - 인사 메시지 생성`() {
            val action = JaninoAction(
                """
                String name = (String)facts.get("name");
                String role = (String)facts.get("role");
                facts.put("greeting", "Hello, " + role + " " + name + "!");
                """.trimIndent()
            )

            val facts = Facts.of("name" to "Alice", "role" to "Manager")
            action.execute(facts)

            facts.get<String>("greeting") shouldBeEqualTo "Hello, Manager Alice!"
        }

        @Test
        fun `null 안전 검사 - null이면 기본값 사용`() {
            val action = JaninoAction(
                """
                Object nameObj = facts.get("name");
                String name = nameObj != null ? (String)nameObj : "Guest";
                facts.put("displayName", name);
                """.trimIndent()
            )

            // null인 경우
            val facts1 = Facts.of("dummy" to 0)
            action.execute(facts1)
            facts1.get<String>("displayName") shouldBeEqualTo "Guest"

            // 값이 있는 경우
            val facts2 = Facts.of("name" to "Alice")
            action.execute(facts2)
            facts2.get<String>("displayName") shouldBeEqualTo "Alice"
        }
    }

    // =========================================================================
    // 3. 조건 분기 로직
    // =========================================================================

    @Nested
    inner class `조건 분기` {

        @Test
        fun `삼항 연산자 - 등급 결정`() {
            val action = JaninoAction(
                """
                int score = ((Integer)facts.get("score")).intValue();
                String grade = score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : "F";
                facts.put("grade", grade);
                """.trimIndent()
            )

            val facts1 = Facts.of("score" to 95)
            action.execute(facts1)
            facts1.get<String>("grade") shouldBeEqualTo "A"

            val facts2 = Facts.of("score" to 85)
            action.execute(facts2)
            facts2.get<String>("grade") shouldBeEqualTo "B"

            val facts3 = Facts.of("score" to 50)
            action.execute(facts3)
            facts3.get<String>("grade") shouldBeEqualTo "F"
        }

        @Test
        fun `if-else 블록 - 세금 계산`() {
            val action = JaninoAction(
                """
                double income = ((Number)facts.get("income")).doubleValue();
                double tax;
                if (income > 100000) {
                    tax = income * 0.35;
                } else if (income > 50000) {
                    tax = income * 0.25;
                } else if (income > 20000) {
                    tax = income * 0.15;
                } else {
                    tax = 0.0;
                }
                facts.put("tax", Double.valueOf(tax));
                """.trimIndent()
            )

            val lowIncome = Facts.of("income" to 15000)
            action.execute(lowIncome)
            lowIncome.get<Double>("tax") shouldBeEqualTo 0.0

            val midIncome = Facts.of("income" to 60000)
            action.execute(midIncome)
            midIncome.get<Double>("tax") shouldBeEqualTo 15000.0

            val highIncome = Facts.of("income" to 200000)
            action.execute(highIncome)
            highIncome.get<Double>("tax") shouldBeEqualTo 70000.0
        }
    }

    // =========================================================================
    // 4. 복합 룰 시나리오 (실무 예제)
    // =========================================================================

    @Nested
    inner class `실무 시나리오` {

        @Test
        fun `주문 할인 정책 - 여러 룰 순차 적용`() {
            // 룰 1: 기본 할인 (10000원 이상 → 5%)
            val basicDiscount = JaninoRule(name = "basicDiscount", priority = 1)
                .whenever("((Integer)facts.get(\"amount\")).intValue() >= 10000")
                .then(
                    """
                    double amount = ((Integer)facts.get("amount")).doubleValue();
                    facts.put("discountRate", Double.valueOf(5.0));
                    facts.put("discountAmount", Double.valueOf(amount * 0.05));
                    """.trimIndent()
                )

            // 룰 2: VIP 추가 할인 (VIP 회원이면 +5%)
            val vipDiscount = JaninoRule(name = "vipDiscount", priority = 2)
                .whenever("\"VIP\".equals(facts.get(\"memberType\"))")
                .then(
                    """
                    double currentRate = facts.get("discountRate") != null
                        ? ((Number)facts.get("discountRate")).doubleValue() : 0.0;
                    double amount = ((Integer)facts.get("amount")).doubleValue();
                    double newRate = currentRate + 5.0;
                    facts.put("discountRate", Double.valueOf(newRate));
                    facts.put("discountAmount", Double.valueOf(amount * newRate / 100.0));
                    """.trimIndent()
                )

            // 룰 3: 무료 배송 (50000원 이상)
            val freeShipping = JaninoRule(name = "freeShipping", priority = 3)
                .whenever("((Integer)facts.get(\"amount\")).intValue() >= 50000")
                .then("facts.put(\"freeShipping\", Boolean.TRUE);")

            val engine = DefaultRuleEngine()
            val facts = Facts.of("amount" to 60000, "memberType" to "VIP")
            engine.fire(ruleSetOf(basicDiscount, vipDiscount, freeShipping), facts)

            facts.get<Number>("discountRate")!!.toDouble() shouldBeEqualTo 10.0
            facts.get<Number>("discountAmount")!!.toDouble() shouldBeEqualTo 6000.0
            facts.get<Boolean>("freeShipping")!!.shouldBeTrue()
        }

        @Test
        fun `회원 등급 분류 - ActivationRuleGroup으로 첫 매칭만 실행`() {
            val platinum = JaninoRule(name = "platinum", priority = 1)
                .whenever("((Integer)facts.get(\"totalPurchase\")).intValue() >= 1000000")
                .then("facts.put(\"tier\", \"PLATINUM\");")

            val gold = JaninoRule(name = "gold", priority = 2)
                .whenever("((Integer)facts.get(\"totalPurchase\")).intValue() >= 500000")
                .then("facts.put(\"tier\", \"GOLD\");")

            val silver = JaninoRule(name = "silver", priority = 3)
                .whenever("((Integer)facts.get(\"totalPurchase\")).intValue() >= 100000")
                .then("facts.put(\"tier\", \"SILVER\");")

            val bronze = JaninoRule(name = "bronze", priority = 4)
                .whenever("((Integer)facts.get(\"totalPurchase\")).intValue() >= 0")
                .then("facts.put(\"tier\", \"BRONZE\");")

            val group = ActivationRuleGroup("tierClassification")
            group.addRule(platinum)
            group.addRule(gold)
            group.addRule(silver)
            group.addRule(bronze)

            val engine = DefaultRuleEngine()

            // 750,000원 → GOLD
            val facts1 = Facts.of("totalPurchase" to 750000)
            engine.fire(ruleSetOf(group), facts1)
            facts1.get<String>("tier") shouldBeEqualTo "GOLD"

            // 50,000원 → BRONZE
            val facts2 = Facts.of("totalPurchase" to 50000)
            engine.fire(ruleSetOf(group), facts2)
            facts2.get<String>("tier") shouldBeEqualTo "BRONZE"
        }

        @Test
        fun `나이 및 지역 확인 - UnitRuleGroup으로 모든 조건 충족 시 실행`() {
            val ageCheck = JaninoRule(name = "ageCheck", priority = 1)
                .whenever("((Integer)facts.get(\"age\")).intValue() >= 18")
                .then("facts.put(\"ageVerified\", Boolean.TRUE);")

            val regionCheck = JaninoRule(name = "regionCheck", priority = 2)
                .whenever("\"KR\".equals(facts.get(\"region\"))")
                .then("facts.put(\"regionVerified\", Boolean.TRUE);")

            val group = UnitRuleGroup("verification")
            group.addRule(ageCheck)
            group.addRule(regionCheck)

            val engine = DefaultRuleEngine()

            // 모든 조건 충족
            val facts1 = Facts.of("age" to 25, "region" to "KR")
            engine.fire(ruleSetOf(group), facts1)
            facts1.get<Boolean>("ageVerified")!!.shouldBeTrue()
            facts1.get<Boolean>("regionVerified")!!.shouldBeTrue()

            // 나이 미충족 → 아무것도 실행 안 됨
            val facts2 = Facts.of("age" to 15, "region" to "KR")
            engine.fire(ruleSetOf(group), facts2)
            facts2.containsKey("ageVerified").shouldBeFalse()
        }

        @Test
        fun `배송비 계산 - 무게와 거리 기반 복합 연산`() {
            val shippingRule = JaninoRule(name = "shipping", priority = 1)
                .whenever("((Number)facts.get(\"weight\")).doubleValue() > 0")
                .then(
                    """
                    double weight = ((Number)facts.get("weight")).doubleValue();
                    int distance = ((Integer)facts.get("distance")).intValue();
                    boolean isExpress = facts.get("express") != null
                        && ((Boolean)facts.get("express")).booleanValue();

                    double baseCost = weight * 100;
                    double distanceCost = distance * 50;
                    double expressFee = isExpress ? 3000.0 : 0.0;
                    double total = baseCost + distanceCost + expressFee;

                    facts.put("shippingCost", Double.valueOf(total));
                    """.trimIndent()
                )

            val engine = DefaultRuleEngine()

            // 일반 배송: weight=2.5kg, distance=200km
            val facts1 = Facts.of("weight" to 2.5, "distance" to 200, "express" to false)
            engine.fire(ruleSetOf(shippingRule), facts1)
            facts1.get<Double>("shippingCost") shouldBeEqualTo 10250.0  // 250 + 10000 + 0

            // 특급 배송
            val facts2 = Facts.of("weight" to 1.0, "distance" to 100, "express" to true)
            engine.fire(ruleSetOf(shippingRule), facts2)
            facts2.get<Double>("shippingCost") shouldBeEqualTo 8100.0  // 100 + 5000 + 3000
        }

        @Test
        fun `check로 룰 평가만 수행 - 실행 없이 조건 확인`() {
            val adultRule = JaninoRule(name = "adult", priority = 1)
                .whenever("((Integer)facts.get(\"age\")).intValue() >= 18")

            val premiumRule = JaninoRule(name = "premium", priority = 2)
                .whenever("((Integer)facts.get(\"purchaseCount\")).intValue() >= 10")

            val engine = DefaultRuleEngine()
            val facts = Facts.of("age" to 25, "purchaseCount" to 5)

            val result = engine.check(ruleSetOf(adultRule, premiumRule), facts)

            result[adultRule]!!.shouldBeTrue()
            result[premiumRule]!!.shouldBeFalse()
        }
    }

    // =========================================================================
    // 5. 루프 및 반복 계산
    // =========================================================================

    @Nested
    inner class `반복 계산` {

        @Test
        fun `for 루프 - 팩토리얼 계산`() {
            val action = JaninoAction(
                """
                int n = ((Integer)facts.get("n")).intValue();
                long result = 1;
                for (int i = 2; i <= n; i++) {
                    result *= i;
                }
                facts.put("factorial", Long.valueOf(result));
                """.trimIndent()
            )

            val facts = Facts.of("n" to 10)
            action.execute(facts)
            facts.get<Long>("factorial") shouldBeEqualTo 3628800L
        }

        @Test
        fun `while 루프 - 복리 이자 계산`() {
            val action = JaninoAction(
                """
                double principal = ((Number)facts.get("principal")).doubleValue();
                double rate = ((Number)facts.get("annualRate")).doubleValue() / 100.0;
                int years = ((Integer)facts.get("years")).intValue();

                double amount = principal;
                int year = 0;
                while (year < years) {
                    amount = amount * (1.0 + rate);
                    year++;
                }
                facts.put("finalAmount", Double.valueOf(Math.round(amount * 100.0) / 100.0));
                """.trimIndent()
            )

            // 1000만원, 연 5%, 3년 복리
            val facts = Facts.of("principal" to 10000000, "annualRate" to 5, "years" to 3)
            action.execute(facts)

            facts.get<Double>("finalAmount") shouldBeEqualTo 11576250.0
        }
    }
}
