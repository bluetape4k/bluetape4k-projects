# Module Examples - MapStruct

English | [한국어](./README.ko.md)

> **⚠️ Obsolete**: This module is no longer in use and has been fully excluded from the build.

Examples of using [MapStruct](http://mapstruct.org/) with Kotlin.

## Examples

### Basic Mapping (PersonConverterTest.kt)

Demonstrates the basic pattern for mapping between entities and DTOs.

```kotlin
@Mapper
interface PersonMapper {
    fun toDto(entity: Person): PersonDto
    fun toEntity(dto: PersonDto): Person
}
```

### Field Mapping (FieldMappingExample.kt)

Demonstrates how to map objects with different field names.

```kotlin
@Mapper
interface FieldMapper {
    @Mapping(source = "firstName", target = "givenName")
    @Mapping(source = "lastName", target = "familyName")
    fun toDto(entity: NameEntity): NameDto
}
```

## Key Learning Points

1. **@Mapper interface**: MapStruct automatically generates the implementation
2. **@Mapping**: Defines mapping rules when field names differ
3. **Custom mapping**: Implementing complex transformation logic
4. **Collection mapping**: Converting `List`, `Set`, and other collection types

## Running the Examples

```bash
./gradlew :examples:mapstruct:test
```

## References

- [MapStruct Official Documentation](http://mapstruct.org/)
- [MapStruct Kotlin Support](https://mapstruct.org/faq/#can-i-use-mapstruct-together-with-kotlin)
