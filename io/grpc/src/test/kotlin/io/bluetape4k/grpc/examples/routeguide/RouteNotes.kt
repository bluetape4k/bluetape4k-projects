package io.bluetape4k.grpc.examples.routeguide

fun routeNoteOf(message: String, location: Point): RouteNote =
    RouteNote.newBuilder()
        .apply {
            this.message = message
            this.location = location
        }
        .build()

fun routeNoteOf(message: String, latitude: Int, longitude: Int): RouteNote =
    RouteNote.newBuilder()
        .apply {
            this.message = message
            this.location = pointOf(latitude, longitude)

        }
        .build()
