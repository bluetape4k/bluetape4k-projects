package io.bluetape4k.junit5.system

/**
 * 테스트 시에 재정의된 시스템 속성을 테스트 후에는 원래 속성 값으로 복원하고, 추가된 신규 속성은 제거합니다.
 */
class SystemPropertyRestoreContext(
    propertyNames: MutableSet<String>,
    restoreProperties: MutableMap<String, String?>,
) {

    private val propertyNames = propertyNames.toHashSet()
    private val restoreProperties = restoreProperties.toMutableMap()

    /**
     * 재정의된 시스템 속성을 원복합니다. 새롭게 추가된 속성은 제거합니다.
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

    class Builder {
        private val propertyNames = HashSet<String>()
        private val restoreProperties = LinkedHashMap<String, String?>()

        fun addPropertyName(name: String) = apply {
            propertyNames.add(name)
        }

        fun addRestoreProperty(name: String, value: String) = apply {
            restoreProperties[name] = value
        }

        fun build(): SystemPropertyRestoreContext =
            SystemPropertyRestoreContext(propertyNames, restoreProperties)
    }
}
