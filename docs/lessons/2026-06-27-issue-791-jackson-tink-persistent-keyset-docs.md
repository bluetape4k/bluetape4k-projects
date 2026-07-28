# 이슈 791: Jackson Tink durable-search 지침

## 배경

Jackson2와 Jackson3 field-encryption 문서는 `DETERMINISTIC_AES256_SIV`가 DB 검색에
적합하다고 설명했지만, `@JsonTinkEncrypt`는 `TinkEncryptors` singleton instance를
통해 algorithm을 해석한다. 이 singleton keyset은 현재 JVM process의 memory에서
생성되므로 restart, rollout, multi-instance access 이후 persisted ciphertext가
읽히지 않거나 검색 호환성을 잃을 수 있다.

## 결정

이 milestone 이슈에서는 documentation-safe fix path를 사용한다. Jackson field
encryption README/KDoc에서 durable DB-search claim을 제거하고 API 동작은 그대로
유지하며, durable searchable storage 지침은 `bluetape4k-tink`의 versioned keyset
API로 안내한다.

## 결과

- Jackson2와 Jackson3는 더 이상 durable DB search 용도로 `@JsonTinkEncrypt(DETERMINISTIC_AES256_SIV)`를 홍보하지 않는다.
- `JsonTinkEncrypt`, `TinkEncryptAlgorithm`, `TinkEncryptors` KDoc은 process-local singleton-keyset 경계를 설명한다.
- Tink README 지침은 durable searchable DB column에 persisted AES-SIV keyset과 `TinkDaeads.versioned(store)`를 권장한다.

## 검증

- Red evidence: 변경 전 `rg`에서 Jackson README/KDoc의 DB-search claim이 확인되었다.
- 수정 뒤 `rg -n "DB search|DB 검색|searchable in DB|DB 검색 가능" io/jackson2 io/jackson3 -g '*.md' -g '*.kt'`는 match를 내지 않았다.
- `./gradlew :bluetape4k-jackson2:compileTestKotlin :bluetape4k-jackson3:compileTestKotlin :bluetape4k-tink:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.crypto.JsonTinkEncryptTest" :bluetape4k-jackson3:test --tests "io.bluetape4k.jackson3.crypto.JsonTinkEncryptTest" :bluetape4k-tink:test --tests "io.bluetape4k.tink.encrypt.TinkEncryptorTest" --no-daemon --no-configuration-cache`
- `git diff --check`

## 향후 지침

Process-local singleton encryptor를 durable database storage helper로 문서화하지
않는다. 나중에 Jackson field encryption에 durable searchable storage가 필요하면
명시적인 keyset/provider 기반 API를 추가하고, ciphertext가 keyset reload 후에도
생존하며 deterministic field 검색 호환성을 유지한다는 테스트를 만든다.

## 동시성 helper gate

이 변경은 documentation/KDoc 전용이므로 새 concurrency helper가 필요하지 않았다.
기존 Jackson field-encryption test는 이미 multithreading, coroutine suspend job,
virtual-thread 실행을 검증했고 이 작업 중에도 통과했다.
