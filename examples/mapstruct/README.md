# Examples - MapStruct

[MapStruct](http://mapstruct.org/)를 Kotlin에서 사용하는 매핑 예제입니다.

## 예제 목록

### 기본 매핑 (PersonConverterTest.kt)

Entity와 DTO 간의 기본적인 매핑 패턴을 학습합니다.

```kotlin
@Mapper
interface PersonMapper {
    fun toDto(entity: Person): PersonDto
    fun toEntity(dto: PersonDto): Person
}
```

### 필드 매핑 (FieldMappingExample.kt)

서로 다른 필드명을 가진 객체 간의 매핑 방법을 학습합니다.

```kotlin
@Mapper
interface FieldMapper {
    @Mapping(source = "firstName", target = "givenName")
    @Mapping(source = "lastName", target = "familyName")
    fun toDto(entity: NameEntity): NameDto
}
```

## 주요 학습 포인트

1. **@Mapper 인터페이스**: MapStruct가 자동으로 구현체 생성
2. **@Mapping**: 필드명이 다를 때 매핑 규칙 정의
3. **커스텀 매핑**: 복잡한 변환 로직 구현
4. **컬렉션 매핑**: List, Set 등 컬렉션 변환

## 실행 방법

```bash
./gradlew :examples:mapstruct:test
```

## 참고

- [MapStruct 공식 문서](http://mapstruct.org/)
- [MapStruct Kotlin 지원](https://mapstruct.org/faq/#can-i-use-mapstruct-together-with-kotlin)
