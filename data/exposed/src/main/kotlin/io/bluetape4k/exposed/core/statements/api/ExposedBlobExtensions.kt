package io.bluetape4k.exposed.core.statements.api

import io.bluetape4k.support.toUtf8String
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.io.InputStream

fun String.toExposedBlob(): ExposedBlob = ExposedBlob(toByteArray())
fun ByteArray.toExposedBlob(): ExposedBlob = ExposedBlob(this)
fun InputStream.toExposedBlob(): ExposedBlob = ExposedBlob(this)

fun ExposedBlob.toUtf8String(): String = bytes.toUtf8String()
fun ExposedBlob.toInputStream(): InputStream = bytes.inputStream()
fun ExposedBlob.toByteArray(): ByteArray = bytes
