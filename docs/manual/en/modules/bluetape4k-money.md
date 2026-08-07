---
manualId: bluetape4k-money
title: "Monetary Calculations"
description: "A library built on the Java standard Money API (JSR-354) to simplify financial and currency operations. It uses the JavaMoney Moneta implementation for currency units, money calculations, and exchange-rate conversion."
kind: library
group: utilities
learningOrder: 1250
---

# Monetary Calculations

## Problem {#problem}

A library built on the Java standard Money API (JSR-354) to simplify financial and currency operations. It uses the JavaMoney Moneta implementation for currency units, money calculations, and exchange-rate conversion. This manual connects that purpose to the current build, source entry points, tests, configuration resources, and lifecycle evidence instead of duplicating the README feature list.

## When to use {#when-to-use}

Use `bluetape4k-money` when the application needs input contracts, value semantics, algorithmic cost, and deterministic output. Start with the source entry points below and confirm that their ownership and failure contracts match the calling component. Prefer a smaller standard-library or already-adopted module when it satisfies the same contract without another runtime boundary.

## Coordinates {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-money")
}
```

Gradle project path: `:bluetape4k-money`. Source directory: `utils/money`.

## Concepts {#concepts}

The first source-level concepts to inspect are `CurrencyConversionSupport`, `CurrencyConverter`, `CurrencySupport`, `FastMoneySupport`, `MoneyAmountSupport`, and `MoneySupport`. File names are navigation anchors; read each declaration and its tests before treating it as a public contract.

## Quick start {#quick-start}

Add the coordinate above, refresh Gradle, and start from the smallest entry point that owns the required task. Open [`CurrencyConversionSupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConversionSupport.kt) first; it is a concrete source entry point for the module.

## API by task {#api-by-task}

| Entry point | What to verify |
| --- | --- |
| [`CurrencyConversionSupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConversionSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CurrencyConverter`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConverter.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`CurrencySupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`FastMoneySupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/FastMoneySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MoneyAmountSupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/MoneyAmountSupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |
| [`MoneySupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/MoneySupport.kt) | Inspect this declaration's constructors, functions, and ownership contract. |

## Patterns {#patterns}

The README evidence is organized around **Overview**, **Adding the Dependency**, **Key Features**, **Class Diagram**, **Usage Examples**, **Create Currency Units**, **Create Money (Money)**, **High-Performance Money (FastMoney)**, **Create MonetaryAmount**, and **Money Arithmetic**. Use those topics as a navigation map, then confirm behavior in source and tests. Keep adoption narrow and connect owned resources to the caller lifecycle.

## Integrations {#integrations}

The current build declares these integration edges:

```kotlin
api(project(":bluetape4k-core"))
api(libs.javax.money.api)
api(libs.javamoney.moneta)
```

Treat `compileOnly` edges as caller-provided capabilities and verify runtime availability before using their APIs.

## Configuration {#configuration}

No module-level configuration resource was found under `src/main/resources`. Configuration is supplied through constructors, builders, function arguments, or the integrating framework; confirm defaults in source.

## Failures {#failures}

Failure semantics are defined by the linked entry points and tests, not inferred from the artifact name. Keep cancellation and timeout signals intact, close owned resources, and translate backend exceptions only at a boundary that can add a stable domain contract. Use the test anchors below to verify the exact behavior before adding retries or fallbacks.

## Operations {#operations}

Measure hot paths, bound input sizes, and monitor failures at the application boundary that calls the utility. Keep capacity, timeout, retry, and shutdown settings next to the component that owns the resource; avoid process-wide defaults that hide which caller accepted the trade-off.

## Testing {#testing}

Run the module test task:

```bash
./gradlew :bluetape4k-money:test --no-configuration-cache
```

Representative test anchors:

- [`CurrencyConversionSupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/CurrencyConversionSupportTest.kt)
- [`CurrencyConverterTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/CurrencyConverterTest.kt)
- [`CurrencyUnitSupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/CurrencyUnitSupportTest.kt)
- [`FastMoneySupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/FastMoneySupportTest.kt)
- [`MoneyAmountSupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/MoneyAmountSupportTest.kt)
- [`MoneySupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/MoneySupportTest.kt)

## Workshops {#workshops}

No dedicated workshop path is registered in the manual manifest. Use the module README and the representative tests above as runnable evidence.

## Limitations {#limitations}

This page documents the repository state represented by the linked source and tests. It does not turn optional backends into application defaults or claim performance without a benchmark artifact. Re-check compatibility and lifecycle notes when the module version changes.

<!-- release-readme-diagrams:start -->
## Release diagrams {#release-diagrams}

These diagrams are loaded directly from README assets published with the `1.12.1` release and pinned to its immutable commit. They describe this manual's released structure and runtime flows, not later Snapshot changes. Select a preview to open the SVG at the same release commit.

### money Class Structure diagram

[![money Class Structure diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-money-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-money-diagram-01.svg)

_Release README: [`utils/money/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/money/README.md)_

### Currency Operation Flow diagram

[![Currency Operation Flow diagram](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-money-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/utils-money-diagram-02.svg)

_Release README: [`utils/money/README.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/utils/money/README.md)_

<!-- release-readme-diagrams:end -->

## Sources {#sources}

- [Module README](../../../../utils/money/README.md)
- [Module build](../../../../utils/money/build.gradle.kts)
- [`CurrencyConversionSupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConversionSupport.kt)
- [`CurrencyConverter`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConverter.kt)
- [`CurrencySupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/CurrencySupport.kt)
- [`FastMoneySupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/FastMoneySupport.kt)
- [`MoneyAmountSupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/MoneyAmountSupport.kt)
- [`MoneySupport`](../../../../utils/money/src/main/kotlin/io/bluetape4k/money/MoneySupport.kt)
- [`CurrencyConversionSupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/CurrencyConversionSupportTest.kt)
- [`CurrencyConverterTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/CurrencyConverterTest.kt)
- [`CurrencyUnitSupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/CurrencyUnitSupportTest.kt)
- [`FastMoneySupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/FastMoneySupportTest.kt)
- [`MoneyAmountSupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/MoneyAmountSupportTest.kt)
- [`MoneySupportTest`](../../../../utils/money/src/test/kotlin/io/bluetape4k/money/MoneySupportTest.kt)
