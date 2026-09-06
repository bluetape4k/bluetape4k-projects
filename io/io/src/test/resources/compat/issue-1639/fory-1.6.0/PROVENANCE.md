# Fory 1.6.0 fixture provenance

이 fixture는 Projects의 `f07eb62b27d670c2383933a927066fc92c8e14f8`
(tree `3acf71b14db31d595bb8fc935891ca77e9c14577`)에 있는 payload와 generator를
중앙 catalog `e4d0748f6204083e4f4080eab8719e9155cd527b`로 해석해 생성했다. 이 catalog는
`fory-core`와 `fory-kotlin`을 모두 `1.6.0`으로 선택한다. 비교 대상 upgrade catalog는
`9698c9d66bea6fcba373143ee8fa5bfbd9812d4b`이다.

## 생성 환경

- Java: Oracle GraalVM `25.0.4.1+1.1`
- Gradle Wrapper: `9.7.0`
- Kotlin: `2.4.10`
- Fory core/Kotlin: `1.6.0`

## dependency read-back

```bash
BLUETAPE4K_DEPENDENCIES_CATALOG_REF=e4d0748f6204083e4f4080eab8719e9155cd527b \
  ./gradlew :bluetape4k-io:dependencyInsight \
  --dependency org.apache.fory --configuration testRuntimeClasspath --no-daemon
```

위 명령에서 `fory-core:1.6.0`과 `fory-kotlin:1.6.0`을 확인한 뒤 생성한다.

## 생성 명령

```bash
BLUETAPE4K_DEPENDENCIES_CATALOG_REF=e4d0748f6204083e4f4080eab8719e9155cd527b \
  ./gradlew :bluetape4k-io:generateIssue1639ForyFixture \
  --init-script \
  io/io/src/test/resources/compat/issue-1639/fory-1.6.0/generate/generate-fixture.init.gradle \
  --no-daemon --no-configuration-cache
```

init script는 동일 catalog로 `testClasses`를 먼저 컴파일하고 그
`testRuntimeClasspath`에서 source-file mode로 `GenerateForyWireFixture.java`를 실행한다.
payload의 fully-qualified class name과 필드 목록은 `manifest.json`에 고정했다.

검증 값:

- fixture size: `241` bytes
- fixture SHA-256: `4e672fb8d1b5cad5cd66537461ce714f4027b794a8be32e54d8337f6cddac819`
- generator SHA-256: `722d3ef0654b39babbf2d505022385c9ab1c006ea12487b957ad5f9afae4d84c`
- init script SHA-256: `7aa711df47acba41b03188a7cb66a307d6582c04ef7625bc7c4154ea72e90b27`
