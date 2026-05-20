package io.bluetape4k.annotations

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.SubclassOptInRequired

@BluetapeExperimentalApi
private class ExperimentalDeclaration

@BluetapeBetaApi
private class BetaDeclaration

@BluetapeInternalApi
private class InternalDeclaration

@BluetapeDelicateApi
private class DelicateDeclaration

@BluetapeObsoleteApi
private class ObsoleteDeclaration

@SubclassOptInRequired(BluetapeImplementationApi::class)
private interface ImplementationSensitiveSpi

@OptIn(BluetapeImplementationApi::class)
private class LocalImplementation : ImplementationSensitiveSpi

class BluetapeApiMarkersTest {

    @Test
    @OptIn(
        BluetapeExperimentalApi::class,
        BluetapeBetaApi::class,
        BluetapeInternalApi::class,
        BluetapeDelicateApi::class,
        BluetapeObsoleteApi::class,
    )
    fun `normal markers compile with explicit opt-in`() {
        ExperimentalDeclaration::class.simpleName shouldBeEqualTo "ExperimentalDeclaration"
        BetaDeclaration::class.simpleName shouldBeEqualTo "BetaDeclaration"
        InternalDeclaration::class.simpleName shouldBeEqualTo "InternalDeclaration"
        DelicateDeclaration::class.simpleName shouldBeEqualTo "DelicateDeclaration"
        ObsoleteDeclaration::class.simpleName shouldBeEqualTo "ObsoleteDeclaration"
    }

    @Test
    fun `subclass opt-in marker compiles for local implementation`() {
        LocalImplementation::class.simpleName shouldBeEqualTo "LocalImplementation"
    }

    @Test
    fun `marker annotation type names are stable`() {
        BluetapeExperimentalApi::class.simpleName shouldBeEqualTo "BluetapeExperimentalApi"
        BluetapeBetaApi::class.simpleName shouldBeEqualTo "BluetapeBetaApi"
        BluetapeInternalApi::class.simpleName shouldBeEqualTo "BluetapeInternalApi"
        BluetapeDelicateApi::class.simpleName shouldBeEqualTo "BluetapeDelicateApi"
        BluetapeObsoleteApi::class.simpleName shouldBeEqualTo "BluetapeObsoleteApi"
        BluetapeImplementationApi::class.simpleName shouldBeEqualTo "BluetapeImplementationApi"
    }
}
