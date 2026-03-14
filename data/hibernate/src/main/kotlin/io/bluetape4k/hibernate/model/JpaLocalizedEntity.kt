package io.bluetape4k.hibernate.model

import io.bluetape4k.ValueObject
import java.util.*

/**
 * 지역화 정보를 포함하는 JPA 엔티티를 표현하는 인터페이스입니다.
 */
interface JpaLocalizedEntity<LV : JpaLocalizedEntity.LocalizedValue> : PersistenceObject {
    /**
     * 지역화된 값을 나타내는 인터페이스입니다.
     */
    interface LocalizedValue : ValueObject

    /**
     * [Locale]을 키로 하는 지역화 값 맵입니다.
     */
    val localeMap: MutableMap<Locale, LV>

    /**
     * 기본 지역화 값을 생성합니다. 해당 지역의 정보가 없을 때 대체값으로 사용됩니다.
     *
     * @return 기본 지역화 값
     */
    fun createDefaultLocalizedValue(): LV

    /**
     * 특정 지역에 해당하는 정보
     *
     * @param locale Locale 정보
     * @return 특정 지역에 해당하는 정보
     */
    fun getLocalizedValue(locale: Locale): LV = getLocalizedValueOrDefault(locale)

    /**
     * 특정 지역의 정보를 가져옵니다. 만약 해당 지역의 정보가 없다면 엔티티의 정보를 이용한 정보를 제공합니다.
     * @param locale 지역 정보
     * @return 지역화 정보
     */
    fun getLocalizedValueOrDefault(locale: Locale = Locale.getDefault()): LV =
        localeMap[locale] ?: createDefaultLocalizedValue()

    /**
     * 현 Thread Context 에 해당하는 지역의 정보를 제공합니다.
     * @return 지역화 정보
     */
    fun getCurrentLocalizedValue(): LV = getLocalizedValueOrDefault(Locale.getDefault())
}
