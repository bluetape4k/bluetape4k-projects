package io.bluetape4k.junit5.system

/**
 * 테스트가 변경한 시스템 속성의 복원 정보를 보관합니다.
 *
 * ## 동작/계약
 * - `propertyNames`에 기록된 키를 기준으로 복원/삭제 대상을 결정합니다.
 * - 복원 값이 있으면 `System.setProperty`, 없으면 `System.clearProperty`를 호출합니다.
 * - 인자로 받은 컬렉션은 내부에서 복사해 보관합니다.
 *
 * ```kotlin
 * val ctx = SystemPropertyRestoreContext.Builder().addPropertyName("k").build()
 * ctx.restore()
 * // System.getProperty("k") == null
 * ```
 */
class SystemPropertyRestoreContext(
    propertyNames: MutableSet<String>,
    restoreProperties: MutableMap<String, String?>,
) {

    private val propertyNames = propertyNames.toHashSet()
    private val restoreProperties = restoreProperties.toMutableMap()

    /**
     * 기록된 시스템 속성을 원복합니다.
     *
     * ## 동작/계약
     * - 복원 값이 존재하면 해당 값으로 되돌립니다.
     * - 복원 값이 없으면 테스트 중 추가된 속성으로 간주해 제거합니다.
     */
    fun restore() {
        propertyNames.forEach { name ->
            if (restoreProperties.containsKey(name)) {
                System.setProperty(name, restoreProperties[name].orEmpty())
            } else {
                System.clearProperty(name)
            }
        }
    }

    /**
     * [SystemPropertyRestoreContext] 생성용 빌더입니다.
     *
     * ## 동작/계약
     * - 키/복원값을 누적한 뒤 [build]로 불변 컨텍스트를 생성합니다.
     * - 같은 키를 여러 번 넣으면 마지막 복원값이 유지됩니다.
     */
    class Builder {
        private val propertyNames = HashSet<String>()
        private val restoreProperties = LinkedHashMap<String, String?>()

        /** 복원 대상 시스템 속성 키를 추가합니다. */
        fun addPropertyName(name: String) = apply {
            propertyNames.add(name)
        }

        /** 특정 시스템 속성의 복원 값을 기록합니다. */
        fun addRestoreProperty(name: String, value: String) = apply {
            restoreProperties[name] = value
        }

        /** 누적한 정보로 [SystemPropertyRestoreContext]를 생성합니다. */
        fun build(): SystemPropertyRestoreContext =
            SystemPropertyRestoreContext(propertyNames, restoreProperties)
    }
}
