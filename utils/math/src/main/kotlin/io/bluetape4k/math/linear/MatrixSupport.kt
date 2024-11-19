package io.bluetape4k.math.linear

import org.apache.commons.math3.Field
import org.apache.commons.math3.FieldElement
import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.fraction.Fraction
import org.apache.commons.math3.linear.AnyMatrix
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.DiagonalMatrix
import org.apache.commons.math3.linear.FieldMatrix
import org.apache.commons.math3.linear.FieldVector
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun diagonalMatrixOf(dimension: Int): DiagonalMatrix = DiagonalMatrix(dimension)

fun diagonalMatrixOf(elements: DoubleArray, copyArray: Boolean = true): DiagonalMatrix =
    DiagonalMatrix(elements, copyArray)

fun DiagonalMatrix.toRealMatrix(rows: Int, columns: Int): RealMatrix =
    createMatrix(rows, columns)

fun Array<DoubleArray>.createRealMatrix(): RealMatrix =
    MatrixUtils.createRealMatrix(this)

fun realMatrixOf(rows: Int, columns: Int): RealMatrix =
    MatrixUtils.createRealMatrix(rows, columns)

fun <T: FieldElement<T>> Field<T>.createFieldMatrix(rows: Int, columns: Int): FieldMatrix<T> =
    MatrixUtils.createFieldMatrix(this, rows, columns)

fun <T: FieldElement<T>> Array<Array<T>>.createFieldMatrix(): FieldMatrix<T> =
    MatrixUtils.createFieldMatrix(this)

fun realIdentityMatrixOf(dimension: Int): RealMatrix =
    MatrixUtils.createRealIdentityMatrix(dimension)

fun <T: FieldElement<T>> Field<T>.createFieldIdentityMatrix(dimension: Int): FieldMatrix<T> =
    MatrixUtils.createFieldIdentityMatrix(this, dimension)

fun realDiagonalMatrixOf(diagonal: DoubleArray): RealMatrix =
    MatrixUtils.createRealDiagonalMatrix(diagonal)

fun <T: FieldElement<T>> fieldIdentityMatrixOf(diagonal: Array<T>): FieldMatrix<T> =
    MatrixUtils.createFieldDiagonalMatrix<T>(diagonal)


fun realVectorOf(data: DoubleArray): RealVector =
    MatrixUtils.createRealVector(data)

fun <T: FieldElement<T>> fieldVectorOf(data: Array<T>): FieldVector<T> =
    MatrixUtils.createFieldVector(data)

fun rowRealMatrixOf(rowData: DoubleArray): RealMatrix =
    MatrixUtils.createRowRealMatrix(rowData)

fun <T: FieldElement<T>> rowFieldMatrixOf(rowData: Array<T>): FieldMatrix<T> =
    MatrixUtils.createRowFieldMatrix(rowData)

fun columnRealMatrixOf(columnData: DoubleArray): RealMatrix =
    MatrixUtils.createColumnRealMatrix(columnData)

fun <T: FieldElement<T>> columnFieldMatrixOf(columnData: Array<T>): FieldMatrix<T> =
    MatrixUtils.createColumnFieldMatrix(columnData)


fun RealMatrix.checkSymmetric(epsilon: Double = 1.0e-8) {
    MatrixUtils.checkSymmetric(this, epsilon)
}

fun RealMatrix.isSymmetric(epsilon: Double = 1.0e-8): Boolean =
    MatrixUtils.isSymmetric(this, epsilon)

fun AnyMatrix.checkMatrixIndex(row: Int, column: Int) {
    MatrixUtils.checkMatrixIndex(this, row, column)
}

fun AnyMatrix.checkRowIndex(row: Int) {
    MatrixUtils.checkRowIndex(this, row)
}

fun AnyMatrix.checkColumnIndex(column: Int) {
    MatrixUtils.checkColumnIndex(this, column)
}

fun AnyMatrix.checkSubMatrixIndex(startRow: Int, endRow: Int, startColumn: Int, endColumn: Int) {
    MatrixUtils.checkSubMatrixIndex(this, startRow, endRow, startColumn, endColumn)
}

fun AnyMatrix.checkSubMatrixIndex(selectedRows: IntArray, selectedColumns: IntArray) {
    MatrixUtils.checkSubMatrixIndex(this, selectedRows, selectedColumns)
}


fun AnyMatrix.checkAdditionCompatible(right: AnyMatrix) {
    MatrixUtils.checkAdditionCompatible(this, right)
}

fun AnyMatrix.checkSubtractionCompatible(right: AnyMatrix) {
    MatrixUtils.checkSubtractionCompatible(this, right)
}

fun AnyMatrix.checkMultiplicationCompatible(right: AnyMatrix) {
    MatrixUtils.checkMultiplicationCompatible(this, right)
}


@JvmName("fractionToRealMatrix")
fun FieldMatrix<Fraction>.toRealMatrix(): Array2DRowRealMatrix =
    MatrixUtils.fractionMatrixToRealMatrix(this)

@JvmName("bigFractionToRealMatrix")
fun FieldMatrix<BigFraction>.toRealMatrix(): Array2DRowRealMatrix =
    MatrixUtils.bigFractionMatrixToRealMatrix(this)


fun RealVector.toByteArray(): ByteArray {
    return ByteArrayOutputStream().use { bos ->
        ObjectOutputStream(bos).use { oos ->
            oos.defaultWriteObject()
            MatrixUtils.serializeRealVector(this, oos)
            oos.flush()
            bos.toByteArray()
        }
    }
}

fun ByteArray.toRealVector(fieldName: String): RealVector {
    return ByteArrayInputStream(this).use { bis ->
        ObjectInputStream(bis).use { ois ->
            ois.defaultReadObject()
            MatrixUtils.deserializeRealVector(this, fieldName, ois)
            ois.readObject() as RealVector
        }
    }
}

fun RealMatrix.toByteArray(): ByteArray {
    return ByteArrayOutputStream().use { bos ->
        ObjectOutputStream(bos).use { oos ->
            oos.defaultWriteObject()
            MatrixUtils.serializeRealMatrix(this, oos)
            oos.flush()
            bos.toByteArray()
        }
    }
}

fun ByteArray.toRealMatrix(fieldName: String): RealMatrix {
    return ByteArrayInputStream(this).use { bis ->
        ObjectInputStream(bis).use { ois ->
            ois.defaultReadObject()
            MatrixUtils.deserializeRealMatrix(this, fieldName, ois)
            ois.readObject() as RealMatrix
        }
    }
}

fun RealMatrix.solveLowerTriangularSystem(v: RealVector) {
    MatrixUtils.solveLowerTriangularSystem(this, v)
}

fun RealMatrix.solveUpperTriangularSystem(v: RealVector) {
    MatrixUtils.solveUpperTriangularSystem(this, v)
}

fun RealMatrix.blockInverse(splitIndex: Int): RealMatrix =
    MatrixUtils.blockInverse(this, splitIndex)

fun RealMatrix.inverse(threshold: Double = 0.0): RealMatrix =
    MatrixUtils.inverse(this, threshold)
