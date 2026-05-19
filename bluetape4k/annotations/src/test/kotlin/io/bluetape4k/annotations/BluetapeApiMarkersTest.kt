package io.bluetape4k.annotations

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.SubclassOptInRequired

@ExperimentalBluetapeApi
private class ExperimentalDeclaration

@BetaBluetapeApi
private class BetaDeclaration

@InternalBluetapeApi
private class InternalDeclaration

@DelicateBluetapeApi
private class DelicateDeclaration

@SubclassOptInRequired(BluetapeImplementationApi::class)
private interface ImplementationSensitiveSpi

@OptIn(BluetapeImplementationApi::class)
private class LocalImplementation : ImplementationSensitiveSpi

class BluetapeApiMarkersTest {

    @Test
    @OptIn(
        ExperimentalBluetapeApi::class,
        BetaBluetapeApi::class,
        InternalBluetapeApi::class,
        DelicateBluetapeApi::class,
    )
    fun `normal markers compile with explicit opt-in`() {
        ExperimentalDeclaration::class.simpleName shouldBeEqualTo "ExperimentalDeclaration"
        BetaDeclaration::class.simpleName shouldBeEqualTo "BetaDeclaration"
        InternalDeclaration::class.simpleName shouldBeEqualTo "InternalDeclaration"
        DelicateDeclaration::class.simpleName shouldBeEqualTo "DelicateDeclaration"
    }

    @Test
    fun `subclass opt-in marker compiles for local implementation`() {
        LocalImplementation::class.simpleName shouldBeEqualTo "LocalImplementation"
    }

    @Test
    fun `marker annotation type names are stable`() {
        ExperimentalBluetapeApi::class.simpleName shouldBeEqualTo "ExperimentalBluetapeApi"
        BetaBluetapeApi::class.simpleName shouldBeEqualTo "BetaBluetapeApi"
        InternalBluetapeApi::class.simpleName shouldBeEqualTo "InternalBluetapeApi"
        DelicateBluetapeApi::class.simpleName shouldBeEqualTo "DelicateBluetapeApi"
        BluetapeImplementationApi::class.simpleName shouldBeEqualTo "BluetapeImplementationApi"
    }
}
