package com.touchpc.remotecontrol.ui.touchpad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.touchpc.remotecontrol.R
import com.touchpc.remotecontrol.gesture.GestureInterpreter
import com.touchpc.remotecontrol.protocol.Command

class TouchpadView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gestureInterpreter = GestureInterpreter(context)
    private var onCommandsListener: ((List<Command>) -> Unit)? = null
    private val borderPaint = Paint().apply {
        color = context.getColor(R.color.touchpad_border)
        style = Paint.Style.STROKE
        strokeWidth = 2f * context.resources.displayMetrics.density
    }

    fun setOnCommandsListener(listener: (List<Command>) -> Unit) { onCommandsListener = listener }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val commands = gestureInterpreter.onTouchEvent(event)
        if (commands.isNotEmpty()) { onCommandsListener?.invoke(commands) }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = borderPaint.strokeWidth / 2f
        canvas.drawRoundRect(inset, inset, width.toFloat() - inset, height.toFloat() - inset,
            resources.getDimension(R.dimen.touchpad_corner_radius),
            resources.getDimension(R.dimen.touchpad_corner_radius), borderPaint)
    }

    fun reset() { gestureInterpreter.reset() }
}
