---
name: eclipse-collections-migration
description: Eclipse Collections(UnifiedMap, AbstractSet 등)을 Kotlin stdlib(AbstractMutableMap, AbstractMutableSet)으로 마이그레이션하는 패턴
source: conversation
triggers:
  - "UnifiedMap"
  - "eclipse-collections"
  - "AbstractSet java.util"
  - "fastListOf"
  - "eclipse collection migration"
  - "org.eclipse.collections"
  - "UnifiedMap to AbstractMutableMap"
quality: high
---

# Eclipse Collections → Kotlin Stdlib 마이그레이션

## The Insight

bluetape4k 프로젝트는 Eclipse Collections 의존성을 제거하고 Kotlin 표준 라이브러리로 전환하는 대규모 리팩토링을 진행했다. 단순한 import 변경이 아니라, 상속 구조와 API 호환성 차이로 인해 몇 가지 비자명한 변환이 필요하다.

## Why This Matters

Eclipse Collections의 `UnifiedMap`, `AbstractSet` 등을 그냥 Kotlin stdlib으로 바꾸면 컴파일 에러 또는 런타임 오류가 발생한다. 특히:
- `AbstractMutableSet`은 `add()` 추상 메서드 구현이 필수
- `.isEmpty` 프로퍼티가 `.isEmpty()` 함수로 바뀜
- `entries` 반환 타입이 더 엄격해짐

## Recognition Pattern

다음 코드가 보이면 이 스킬을 적용:
- `import org.eclipse.collections.impl.map.mutable.UnifiedMap`
- `import org.eclipse.collections.impl.list.mutable.FastList`
- `class Foo: UnifiedMap<K, V>()`
- `class Bar: java.util.AbstractSet<T>()`
- `fastListOf(...)`, `mutableListOf(...)` → 이미 Kotlin stdlib

## The Approach

### 1. 클래스 상속 변환

```kotlin
// Before
import org.eclipse.collections.impl.map.mutable.UnifiedMap
class CharArrayMap<V>(startSize: Int): UnifiedMap<Any, V>()

// After
class CharArrayMap<V>(startSize: Int): AbstractMutableMap<Any, V>()
```

```kotlin
// Before
import java.util.AbstractSet
class OriginalKeySet: AbstractSet<Any>()

// After - AbstractMutableSet은 add()가 abstract이므로 반드시 구현 필요!
class OriginalKeySet: AbstractMutableSet<Any>() {
    override fun add(element: Any): Boolean = throw UnsupportedOperationException()
    // ...
}
```

### 2. API 차이 처리

```kotlin
// isEmpty: 프로퍼티 → 함수
map.isEmpty        // Before (Eclipse)
map.isEmpty()      // After (Kotlin stdlib)
```

```kotlin
// entries 반환 타입 명시
// Before (Eclipse - 암묵적 타입)
override val entries: EntrySet get() = _entrySet

// After (Kotlin stdlib - 명시적 타입 필요)
override val entries: MutableSet<MutableMap.MutableEntry<Any, V>> get() = _entrySet
```

### 3. keys 반환 타입 변환

```kotlin
// Before
override val keys: CharArraySet get() = _keySet

// After (Kotlin stdlib은 더 엄격한 타입 요구)
override val keys: MutableSet<Any> get() = _keySet
```

### 4. 리스트 변환

```kotlin
// Before (Eclipse Collections)
import org.eclipse.collections.impl.list.mutable.FastList
val list = FastList.newList<String>()
fastListOf("a", "b", "c")

// After (Kotlin stdlib)
val list = mutableListOf<String>()
mutableListOf("a", "b", "c")
```

### 5. AbstractSet inner class

```kotlin
// Before (java.util.AbstractSet 상속)
object: AbstractSet<Any>() {
    override fun iterator(): Iterator<Any> = ...
    override val size: Int get() = ...
    override fun isEmpty(): Boolean = ...
    override fun clear() = ...
    override fun contains(element: Any?): Boolean = ...
}

// After (AbstractMutableSet 상속 - add() 필수, 일부 메서드 시그니처 변경)
object: AbstractMutableSet<Any>() {
    override fun add(element: Any): Boolean = throw UnsupportedOperationException()
    override fun iterator(): MutableIterator<Any> = ...
    override val size: Int get() = ...
    // contains 시그니처: Any? → Any (non-null)
    override fun contains(element: Any): Boolean = ...
}
```

## Example

실제 변환 예시 (`CharArrayMap.kt`):

```kotlin
// 파일: tokenizer/core/src/main/kotlin/io/bluetape4k/tokenizer/utils/CharArrayMap.kt

// ❌ Before
import org.eclipse.collections.impl.map.mutable.UnifiedMap
open class CharArrayMap<V>(startSize: Int): UnifiedMap<Any, V>(), Serializable {
    fun unmodifiableMap(map: CharArrayMap<V>): CharArrayMap<V> {
        return when {
            map.isEmpty -> emptyMap()  // 프로퍼티
            ...
        }
    }
}

// ✓ After
open class CharArrayMap<V>(startSize: Int): AbstractMutableMap<Any, V>(), Serializable {
    fun unmodifiableMap(map: CharArrayMap<V>): CharArrayMap<V> {
        return when {
            map.isEmpty() -> emptyMap()  // 함수 호출
            ...
        }
    }
}
```

## Notes

- **build.gradle.kts에서 의존성 제거**: `implementation("org.eclipse.collections:eclipse-collections:...")` 삭제
- 마이그레이션 후 반드시 테스트 실행: `./gradlew :<module>:test`
- Eclipse Collections의 `UnifiedMap`은 내부적으로 open addressing hash map이라 성능 특성이 다를 수 있으나, 대부분의 use case에서 차이 없음
- 이 마이그레이션은 bluetape4k 전체 프로젝트에 걸쳐 10개 이상의 모듈에서 적용됨 (aws, cache, csv, coroutines, data, infra 등)
- `fastListOf` 사용처 검색: `grep -r "fastListOf\|UnifiedMap\|eclipse.collections" --include="*.kt" .`
