package com.andreibelous.arwar.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import com.andreibelous.arwar.R
import com.andreibelous.arwar.cast
import com.andreibelous.arwar.dp
import com.andreibelous.arwar.toBitmap

class MiniMapView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = context.dp(12f)
                outline.setRoundRect(0, 0, view.height, view.width, radius)
            }
        }
        setBackgroundColor(Color.WHITE)
    }

    private val headingPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            color = Color.parseColor("#237BFF")
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }

    private val netPaint =
        Paint().apply {
            color = ColorUtils.setAlphaComponent(Color.BLACK, (255 * 0.3f).toInt())
        }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val headingIcon: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.map_location_default_view_angle)
    private val navigationIcon =
        AppCompatResources
            .getDrawable(context, R.drawable.map_location_default)!!
            .cast<Drawable>()
            .toBitmap()!!

    var headingProvider: (() -> Float?)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val dx = (width / LINE_COUNT).toFloat()
        val dy = (height / LINE_COUNT).toFloat()

        for (i in 1 until LINE_COUNT) {
            canvas.drawLine(0f, dy * i, width.toFloat(), dy * i, netPaint)
            canvas.drawLine(dx * i, 0f, dx * i, height.toFloat(), netPaint)
        }

        val heading = headingProvider?.invoke()

        if (heading != null) {
            val x = width / 2f
            val y = height / 2f
            canvas.save()
            canvas.rotate(heading - 180, x, y)
            canvas.drawBitmap(
                headingIcon,
                (x - headingIcon.width / 2),
                (y - headingIcon.height / 2),
                headingPaint
            )
            canvas.restore()

            canvas.drawBitmap(
                navigationIcon,
                (x - navigationIcon.width / 2),
                (y - navigationIcon.height / 2),
                bitmapPaint
            )
        }

        invalidate()
    }

    private companion object {

        private const val LINE_COUNT = 4
    }
}