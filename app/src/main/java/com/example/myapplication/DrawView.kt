//package com.example.myapplication
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.util.AttributeSet
//import android.view.MotionEvent
//import android.view.View
//
//class DrawView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
//
//    private val path = android.graphics.Path()
//    private val paint = Paint().apply {
//        color = Color.RED
//        style = Paint.Style.STROKE
//        strokeWidth = 8f
//        isAntiAlias = true
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> path.moveTo(event.x, event.y)
//            MotionEvent.ACTION_MOVE -> path.lineTo(event.x, event.y)
//        }
//        invalidate()
//        return true
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        canvas.drawPath(path, paint)
//    }
//
//    fun clear() {
//        path.reset()
//        invalidate()
//    }
//}
package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val path = Path()
    private val paint = Paint().apply {
        color = Color.RED  // 기본 색상
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> path.moveTo(event.x, event.y)
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(event.x, event.y)
                canvas?.drawPath(path, paint)  // 🎨 즉시 반영
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(event.x, event.y)
                canvas?.drawPath(path, paint)
                path.reset()
            }
        }
        invalidate()
        return true
    }

    /** 📌 색상 변경 */
    fun setDrawingColor(color: Int) {
        paint.color = color
        invalidate()
    }

//    /** 📌 원본 사진을 포함한 최종 Bitmap 반환 */
//    fun getBitmapWithBackground(photoBitmap: Bitmap): Bitmap {
//        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val tempCanvas = Canvas(resultBitmap)
//
//        // 📌 원본 사진을 먼저 그린 후, 그 위에 드로잉을 추가
//        tempCanvas.drawBitmap(photoBitmap, 0f, 0f, null)
//        bitmap?.let { tempCanvas.drawBitmap(it, 0f, 0f, null) }
//        tempCanvas.drawPath(path, paint)
//        return resultBitmap
//    }
    /** 📌 원본 사진을 포함한 최종 Bitmap 반환 (좌표 보정 포함) */
    fun getBitmapWithBackground(photoBitmap: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(photoBitmap.width, photoBitmap.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(resultBitmap)

        // 📌 원본 사진 먼저 그리기
        tempCanvas.drawBitmap(photoBitmap, 0f, 0f, null)

        // 📌 좌표 비율로 보정하여 그리기
        val scaleX = photoBitmap.width.toFloat() / width
        val scaleY = photoBitmap.height.toFloat() / height

        tempCanvas.save()
        tempCanvas.scale(scaleX, scaleY)  // 좌표 비율 조정
        bitmap?.let { tempCanvas.drawBitmap(it, 0f, 0f, null) }
        tempCanvas.drawPath(path, paint)
        tempCanvas.restore()

        return resultBitmap
    }
    /** 📌 그리기 초기화 */
    fun clear() {
        bitmap?.eraseColor(Color.TRANSPARENT)
        path.reset()
        invalidate()
    }
}
