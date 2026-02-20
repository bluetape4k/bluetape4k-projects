# Benchmark 결과

## Binary Serializers

직렬화 할 대상은 다음과 같은 단순한 data class 에 랜덤한 값을 주입하고, 20개의 컬렉션을 직렬화/역직렬화 하는 작업에 대한 성능테스트입니다.

```kotlin
data class SimpleData(
    val id: Long,
    val name: String,
    val age: Int,
    val birth: LocalDate,
    val biography: String,
    val zip: String,
    val address: String,
    val price: BigDecimal? = null,
    val uuid: UUID? = null,         // NOTE: Kryo 는 JDK에 따라 UUID 를 지원하지 않을 수 있음 
    val bytes: ByteArray? = null,
): Serializable
```

### Byte Array 가 없는 경우

1. Byte Array 속성에 값이 없는 경우

| Library | ops/s   |
|---------|---------|
| fury    | 305,821 |
| kryo    | 81,823  |
| jdk     | 22,249  |
| jackson | 39,510  |

> Fury 가 홍보하는 정도까지는 아니지만, 기존에 가장 많이 사용하는 Kryo 보다 3배 이상 빠르다.
> 단순 수형에 대한 처리는 의외로 JDK 가 가장 느리다.
>

2. Byte Array 에 랜덤한 4096 바이트를 넣은 경우

| Library | ops/s  |
|---------|--------|
| fury    | 59,192 |
| kryo    | 29,329 |
| jdk     | 8,431  |
| jackson | 4,323  |

> Fury 가 홍보하는 정도까지는 아니지만, 기존에 가장 많이 사용하는 Kryo 보다 3배 이상 빠르다.
> ByteArray 직렬화 하는 경우, Jackson 이 가장 느리다.

## Compressor

resources/files/Utf8Samples.txt (40kb) 파일을 압축/복원 성능 테스트

| Compressor | ops/s |
|------------|-------|
| gzip       | 1,195 |
| deflate    | 1,084 |
| lz4        | 6,769 |
| snappy     | 8,073 |
| zstd       | 5,103 |
