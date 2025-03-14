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
import java.util.HashMap

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
    
    // 아이콘 관련 변수
    private var isIconMode = false
    private var isEraserMode = false
    private var selectedIconNumber = 1
    private val iconPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val iconBackgroundPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // 아이콘 위치 저장
    private val icons = HashMap<Int, PointF>()
    
    // 아이콘별 메모 저장
    private val iconMemos = HashMap<Int, String>()
    
    // 아이콘별 색상 저장
    private val iconColors = HashMap<Int, Int>().apply {
        // 기본 색상 설정
        put(1, Color.RED)
        put(2, Color.BLUE)
        put(3, Color.GREEN)
        put(4, Color.MAGENTA)
        put(5, Color.BLACK)
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
        
        // 아이콘 그리기
        for ((number, position) in icons) {
            // 해당 아이콘의 색상 가져오기
            val iconColor = iconColors[number] ?: Color.RED
            iconBackgroundPaint.color = iconColor
            
            // 원 배경 그리기
            canvas.drawCircle(position.x, position.y, 30f, iconBackgroundPaint)
            // 숫자 그리기
            canvas.drawText(number.toString(), position.x, position.y + 15f, iconPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isEraserMode) {
            // 지우기 모드일 때
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    eraseAtPosition(event.x, event.y)
                }
            }
        } else if (isIconMode) {
            // 아이콘 모드일 때
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    // 화면에 아이콘 추가
                    addIcon(selectedIconNumber, event.x, event.y)
                }
            }
        } else {
            // 그리기 모드일 때
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
        }
        invalidate()
        return true
    }
    
    /**
     * 지우기 기능 - 터치 위치에서 아이콘과 그림 지우기
     */
    private fun eraseAtPosition(x: Float, y: Float) {
        // 1. 아이콘 지우기 - 터치 위치에 아이콘이 있는지 확인
        val iconsToRemove = mutableListOf<Int>()
        
        for ((number, position) in icons) {
            // 아이콘과 터치 위치 사이의 거리 계산
            val distance = Math.sqrt(
                Math.pow((position.x - x).toDouble(), 2.0) + 
                Math.pow((position.y - y).toDouble(), 2.0)
            )
            
            // 아이콘 반경(30f) 내에 터치하면 아이콘 제거
            if (distance <= 30.0) {
                iconsToRemove.add(number)
            }
        }
        
        // 아이콘 제거
        for (number in iconsToRemove) {
            icons.remove(number)
            iconMemos.remove(number)
            iconColors.remove(number)
        }
        
        // 2. 비트맵에서 그림 지우기 - 터치 위치 주변에 투명한 원 그리기
        if (bitmap != null) {
            val canvas = Canvas(bitmap!!)
            val eraserPaint = Paint().apply {
                color = Color.TRANSPARENT
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            
            // 지우기 반경
            val eraserRadius = 20f
            canvas.drawCircle(x, y, eraserRadius, eraserPaint)
        }
    }

    /** 📌 그리기 색상 변경 */
    fun setDrawingColor(color: Int) {
        paint.color = color
        invalidate()
    }
    
    /** 📌 아이콘 색상 변경 */
    fun setIconColor(number: Int, color: Int) {
        iconColors[number] = color
        invalidate()
    }
    
    /** 📌 아이콘 색상 가져오기 */
    fun getIconColor(number: Int): Int {
        return iconColors[number] ?: Color.RED
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
        
        // 아이콘 그리기 추가
        for ((number, position) in icons) {
            // 해당 아이콘의 색상 가져오기
            val iconColor = iconColors[number] ?: Color.RED
            iconBackgroundPaint.color = iconColor
            
            // 원 배경 그리기
            tempCanvas.drawCircle(position.x, position.y, 30f, iconBackgroundPaint)
            // 숫자 그리기
            tempCanvas.drawText(number.toString(), position.x, position.y + 15f, iconPaint)
        }
        
        tempCanvas.restore()

        return resultBitmap
    }
    /** 📌 그리기 초기화 */
    fun clear() {
        bitmap?.eraseColor(Color.TRANSPARENT)
        path.reset()
        icons.clear()
        iconMemos.clear()
        invalidate()
    }
    
    /** 아이콘 모드 설정 */
    fun setIconMode(enabled: Boolean) {
        isIconMode = enabled
        if (enabled) {
            isEraserMode = false
        }
    }
    
    /** 지우기 모드 설정 */
    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
        if (enabled) {
            isIconMode = false
        }
    }
    
    /** 지우기 모드 상태 가져오기 */
    fun isEraserMode(): Boolean {
        return isEraserMode
    }
    
    /** 선택한 아이콘 번호 설정 */
    fun setSelectedIcon(number: Int) {
        selectedIconNumber = number
    }
    
    /** 아이콘 추가 */
    private fun addIcon(number: Int, x: Float, y: Float) {
        icons[number] = PointF(x, y)
        invalidate()
    }
    
    /** 아이콘에 메모 연결 */
    fun setIconMemo(number: Int, memo: String) {
        iconMemos[number] = memo
    }
    
    /** 아이콘의 메모 가져오기 */
    fun getIconMemo(number: Int): String {
        return iconMemos[number] ?: ""
    }
    
    /** 아이콘이 존재하는지 확인 */
    fun hasIcon(number: Int): Boolean {
        return icons.containsKey(number)
    }
}
