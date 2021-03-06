package com.antonionicolaspina.textimageview

import android.graphics.*

internal data class TextProperties(
    var text: String,
    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG),
    val position: PointF = PointF(),
    var scaleFactor: Float = 1f,
    var rotationDegress: Float = 0f,
    val boundingRect: Rect = Rect(),
    val matrix: Matrix = Matrix()
) {
    private var inverseMatrix = Matrix()

    fun toText(w: Int, h: Int) = Text(
        text,
        paint.color,
        paint.textSize/h,
        paint.typeface,
        PointF(position.x/w, position.y/h),
        scaleFactor,
        rotationDegress
    )

    fun inverseMap(x: Float, y: Float): PointF {
        val floats = listOf(x, y).toFloatArray()
        matrix.invert(inverseMatrix)
        inverseMatrix.mapPoints(floats)

        return PointF(floats[0], floats[1])
    }

    fun inverseMap(r: RectF) {
        matrix.invert(inverseMatrix)
        inverseMatrix.mapRect(r)
    }
}
