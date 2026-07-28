# DynamoDB Local Testcontainers Fixture

배경: `bluetape4k-leader` issue #367은 private test-only `GenericContainer` 대신 reusable DynamoDB Local
Testcontainers launcher가 필요하다.

결정: AWS emulator package 아래에 `DynamoDbLocalServer`를 `bluetape4k-testcontainers`에 추가한다.
SDK-neutral하게 유지하고 AWS-compatible endpoint와 credential property를 expose하며 singleton reuse를
위해 `Launcher.dynamoDb`를 제공한다.

결과: Downstream leader test는 이 fixture를 포함한 catalog version을 consume한 뒤 private container를
제거할 수 있다.

검증: `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.aws.DynamoDbLocalServerTest'`.

향후 가드: Downstream test base에 DynamoDB Local container를 직접 다시 도입하지 말고
`DynamoDbLocalServer.Launcher.dynamoDb`를 consume한다.
