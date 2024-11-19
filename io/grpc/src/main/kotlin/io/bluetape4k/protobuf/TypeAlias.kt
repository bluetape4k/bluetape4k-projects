package io.bluetape4k.protobuf

typealias ProtoMessage = com.google.protobuf.Message

typealias ProtoAny = com.google.protobuf.Any
typealias ProtoEmpty = com.google.protobuf.Empty

typealias ProtoMoney = com.google.type.Money

typealias ProtoDate = com.google.type.Date
typealias ProtoTime = com.google.type.TimeOfDay
typealias ProtoDateTime = com.google.type.DateTime

typealias ProtoDuration = com.google.protobuf.Duration
typealias ProtoTimestamp = com.google.protobuf.Timestamp


@JvmField
val PROTO_EMPTY: ProtoEmpty = ProtoEmpty.getDefaultInstance()

@JvmField
val PROTO_ANY: ProtoAny = ProtoAny.getDefaultInstance()
