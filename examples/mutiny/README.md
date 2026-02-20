# Examples - Mutiny

[Mutiny](https://smallrye.io/smallrye-mutiny/)는 Reactive Programming을 위한 SmallRye 라이브러리입니다. 이 프로젝트는 Mutiny를 사용하는 다양한 예제를 제공합니다.

## 예제 목록

### 기초 예제

| 예제 파일                              | 설명                  |
|------------------------------------|---------------------|
| `01_Basic_Uni.kt`                  | Uni 기본: 단일 값 비동기 처리 |
| `02_Basic_Multi.kt`                | Multi 기본: 스트림 처리    |
| `03_Groups.kt`                     | 그룹화 및 배치 처리         |
| `04_Composition_Transformation.kt` | 조합 및 변환 연산자         |
| `05_Failures.kt`                   | 실패 처리 및 복구          |
| `06_Backpressure.kt`               | 배압(Backpressure) 관리 |
| `07_Threading.kt`                  | 스레딩 모델과 스케줄러        |
| `08_Multi_CustomOperator.kt`       | 커스텀 연산자 구현          |

### Backpressure 예제 (backpressure/)

| 예제 파일                 | 설명          |
|-----------------------|-------------|
| `01_Drop.kt`          | 초과 항목 드롭 전략 |
| `02_Buffer.kt`        | 버퍼링 전략      |
| `03_Visual_Drop.kt`   | 드롭 전략 시각화   |
| `04_Visual_Buffer.kt` | 버퍼 전략 시각화   |

## 주요 학습 포인트

### Uni (단일 비동기 값)

```kotlin
// Uni 생성
val uni = Uni.createFrom().item("Hello")

// 변환
uni.map { it.uppercase() }
    .flatMap { processAsync(it) }

// 구독
uni.subscribe().with(
    { item -> println(item) },
    { failure -> println(failure.message) }
)
```

### Multi (스트림)

```kotlin
// Multi 생성
val multi = Multi.createFrom().items(1, 2, 3, 4, 5)

// 변환
multi.filter { it % 2 == 0 }
     .map { it * 2 }

// 구독
multi.subscribe().with { item -> println(item) }
```

### Backpressure

```kotlin
// Drop 전략
multi.onOverflow().drop()

// Buffer 전략
multi.onOverflow().buffer(100)
```

## 실행 방법

```bash
# 모든 예제 실행
./gradlew :examples:mutiny:test

# 특정 예제만 실행
./gradlew :examples:mutiny:test --tests "*01_Basic*"
```

## 참고

- [Mutiny 공식 문서](https://smallrye.io/smallrye-mutiny/)
- [Mutiny Getting Started](https://smallrye.io/smallrye-mutiny/getting-started/)
