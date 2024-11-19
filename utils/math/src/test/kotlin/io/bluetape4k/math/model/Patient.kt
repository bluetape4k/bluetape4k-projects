package io.bluetape4k.math.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Patient(
    val firstName: String,
    val lastName: String,
    val gender: Gender,
    val birthday: LocalDate,
    val whiteBloodCellCount: Int,
) {
    val age: Long get() = ChronoUnit.YEARS.between(birthday, LocalDate.now())
}
