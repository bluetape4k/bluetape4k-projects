# DynamoDB Local Testcontainers Fixture

Context: `bluetape4k-leader` issue #367 needs a reusable DynamoDB Local
Testcontainers launcher instead of a private test-only `GenericContainer`.

Decision: Add `DynamoDbLocalServer` to `bluetape4k-testcontainers` under the
AWS emulator package. Keep it SDK-neutral, expose AWS-compatible endpoint and
credential properties, and provide `Launcher.dynamoDb` for singleton reuse.

Outcome: Downstream leader tests can remove their private container once they
consume a catalog version containing this fixture.

Verification: `./gradlew :bluetape4k-testcontainers:test --tests
'io.bluetape4k.testcontainers.aws.DynamoDbLocalServerTest'`.

Future guard: Do not reintroduce DynamoDB Local containers directly in
downstream test bases; consume `DynamoDbLocalServer.Launcher.dynamoDb` instead.
