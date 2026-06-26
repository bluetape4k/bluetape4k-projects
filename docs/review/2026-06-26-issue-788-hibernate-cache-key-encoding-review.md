# Review - Hibernate Cache Key Encoding (2026-06-26)

Issue: #788
Branch: `fix/hibernate-cache-key-encoding`
Module: `:bluetape4k-hibernate-cache-lettuce`

## Scope

- Replaced delimiter-based Hibernate cache key normalization in `LettuceNearCacheStorageAccess`.
- Added collision regression tests through the `DomainDataStorageAccess` public path.
- Updated English and Korean README key-isolation wording.

## 7-Tier Review

| Tier | Result | Evidence |
|---|---:|---|
| Tier 1 - Security | PASS | Natural-id delimiter, array/scalar, and same-`toString()` custom identifier collisions are covered by regression tests. |
| Tier 2 - Architecture | PASS | Region isolation stays in `LettuceNearCache`; storage access only normalizes the Hibernate key before the region prefix is applied. |
| Tier 3 - Performance | PASS | Redis keys are bounded to `hck2:<sha256-base64url>` instead of unbounded entity/key strings. |
| Tier 4 - Correctness | PASS | Canonical digest input includes key kind, entity/role name, tenant id, null markers, primitive/object array boundaries, and Serializable object bytes. |
| Tier 5 - Tests | PASS | Targeted class: 6 passing; full module: 80 passing. |
| Tier 6 - Documentation | PASS | `README.md` and `README.ko.md` now distinguish region prefix isolation from collision-resistant Hibernate key encoding. |
| Tier 7 - Evidence | PASS | `git diff --check` clean; CodeGraph change detection reports no additional affected flows or test gaps. |

## Findings

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## Validation Evidence

- `./gradlew :bluetape4k-hibernate-cache-lettuce:compileTestKotlin --warning-mode all --no-daemon --no-configuration-cache`
  - Result: PASS, `BUILD SUCCESSFUL in 12s`.
- `./gradlew :bluetape4k-hibernate-cache-lettuce:test --tests 'io.bluetape4k.hibernate.cache.lettuce.HibernateAdvancedKeyCacheTest' --no-daemon --no-configuration-cache --rerun-tasks`
  - Result: PASS, 6 passing.
- `./gradlew :bluetape4k-hibernate-cache-lettuce:test --no-daemon --no-configuration-cache --rerun-tasks`
  - Result: PASS, 80 passing.
- `git diff --check`
  - Result: PASS.
- CodeGraph `detect_changes_tool(base=develop)`
  - Result: risk score 0.00, no test gaps.
