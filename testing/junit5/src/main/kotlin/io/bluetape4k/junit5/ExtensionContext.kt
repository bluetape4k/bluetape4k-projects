package io.bluetape4k.junit5

import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KClass

internal fun ExtensionContext.namespace(clazz: Class<*>): ExtensionContext.Namespace =
    ExtensionContext.Namespace.create(clazz, this)

internal fun ExtensionContext.namespace(kclazz: KClass<*>): ExtensionContext.Namespace =
    ExtensionContext.Namespace.create(kclazz.java, this)

internal fun ExtensionContext.store(clazz: Class<*>): ExtensionContext.Store =
    getStore(namespace(clazz))

internal fun ExtensionContext.store(kclazz: KClass<*>): ExtensionContext.Store =
    getStore(namespace(kclazz))
