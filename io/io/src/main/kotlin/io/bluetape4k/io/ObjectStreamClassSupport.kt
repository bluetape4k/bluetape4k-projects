package io.bluetape4k.io

import java.io.ObjectStreamClass
import kotlin.reflect.KClass


fun <T: Any> KClass<T>.lookup(): ObjectStreamClass =
    ObjectStreamClass.lookup(this.java)
