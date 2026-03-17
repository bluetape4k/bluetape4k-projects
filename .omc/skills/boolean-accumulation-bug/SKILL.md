---
name: boolean-accumulation-bug
description: forEach 루프에서 boolean 결과 누적 시 단락 평가(short-circuit) 또는 직접 대입으로 발생하는 버그 패턴
source: conversation
triggers:
  - "modified = add"
  - "removed = removed ||"
  - "forEach boolean"
  - "addAll bug"
  - "removeAll bug"
  - "CharArraySet"
  - "boolean accumulation"
quality: high
---

# Boolean 누적 플래그 단락 평가 버그

## The Insight

`forEach` 루프에서 boolean 플래그를 누적할 때 두 가지 미묘한 버그가 발생한다:

1. **직접 대입 버그**: `modified = operation()` → 마지막 원소의 결과만 남음
2. **단락 평가(short-circuit) 버그**: `flag = flag || operation()` → `flag`가 이미 `true`이면 이후 `operation()`이 **호출되지 않음**

두 번째 버그는 특히 위험한데, side effect(실제 삽입/삭제)가 발생해야 하는 연산이 건너뛰어진다.

## Why This Matters

`CharArraySet.removeAll()`에서 실제로 발생한 버그:

```kotlin
// 버그: 첫 번째 remove()가 true를 반환하면, 나머지 원소들의 remove()가 호출되지 않음!
var removed = false
elements.forEach {
    removed = removed || remove(it)  // ❌ 단락 평가로 인해 이후 원소 미처리
}
```

```kotlin
// 버그: 마지막 add() 결과만 modified에 저장됨
var modified = false
elements.forEach {
    modified = add(it)  // ❌ 이전 원소의 추가 성공 여부가 덮어쓰여짐
}
```

`addAll()`은 단 하나의 원소라도 추가되면 `true`를 반환해야 하지만,
마지막 원소가 이미 존재하면 `false`가 반환되어 실제로는 추가가 됐음에도 `false` 리턴.

## Recognition Pattern

다음 패턴이 보이면 이 버그를 의심할 것:
- `forEach` 또는 `for` 루프 안에서 boolean 플래그를 `||` 또는 직접 대입으로 갱신
- `addAll()`, `removeAll()`, `retainAll()` 같은 컬렉션 변형 메서드 구현
- 결과 추적이 필요한 side effect 연산

## The Approach

**올바른 패턴**: 조건부 대입을 사용하여 플래그가 한 번 `true`가 되면 유지되도록 한다.

```kotlin
// ✓ 올바른 addAll 패턴: 하나라도 추가되면 true
var modified = false
elements.forEach {
    if (add(it)) modified = true
}
return modified
```

```kotlin
// ✓ 올바른 removeAll 패턴: 모든 원소를 처리하고 하나라도 제거되면 true
var removed = false
elements.forEach {
    if (remove(it)) removed = true
}
return removed
```

`all { }` 사용 시 주의: `all`도 단락 평가를 한다. 즉, `false`를 반환하는 원소를 만나면 이후 원소를 처리하지 않는다.

```kotlin
// ⚠️ 주의: all도 단락 평가 → 첫 remove()가 false면 나머지 미처리
return elements.all { remove(it) }
```

## Example

```kotlin
// 파일: tokenizer/core/src/main/kotlin/io/bluetape4k/tokenizer/utils/CharArraySet.kt

// ❌ 수정 전 (버그 있음)
override fun addAll(elements: Collection<Any>): Boolean {
    var modified = false
    elements.forEach {
        modified = add(it)  // 마지막 결과만 남음
    }
    return modified
}

override fun removeAll(elements: Collection<Any>): Boolean {
    var removed = false
    elements.forEach {
        removed = removed || remove(it)  // 단락 평가로 이후 원소 미처리
    }
    return removed
}

// ✓ 수정 후 (올바름)
override fun addAll(elements: Collection<Any>): Boolean {
    var modified = false
    elements.forEach {
        if (add(it)) modified = true
    }
    return modified
}
```

## Notes

- Java의 `AbstractCollection.addAll/removeAll` 기본 구현도 이 패턴을 올바르게 구현하고 있으므로 참고할 것
- Kotlin `MutableCollection`을 상속할 때 이 메서드들을 직접 구현하는 경우 특히 주의
- 이 버그는 단위 테스트에서도 놓치기 쉬운데, 단일 원소 테스트는 이 버그를 드러내지 못함
