package com.touchpc.remotecontrol.gesture

import android.view.MotionEvent

data class PointerData(
    val id: Int,
    val startX: Float,
    val startY: Float,
    var currentX: Float,
    var currentY: Float,
    val startTime: Long
) {
    val deltaX: Float get() = currentX - startX
    val deltaY: Float get() = currentY - startY
    val elapsedTime: Long get() = System.currentTimeMillis() - startTime
}

class MultiTouchTracker {

    private val activePointers = mutableMapOf<Int, PointerData>()

    val pointerCount: Int get() = activePointers.size

    fun getPointer(index: Int): PointerData? {
        return activePointers.values.toList().getOrNull(index)
    }

    fun getPointerById(id: Int): PointerData? {
        return activePointers[id]
    }

    fun getAllPointers(): List<PointerData> {
        return activePointers.values.toList()
    }

    fun processEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointers.clear()
                val pointerId = event.getPointerId(0)
                activePointers[pointerId] = PointerData(
                    id = pointerId,
                    startX = event.x,
                    startY = event.y,
                    currentX = event.x,
                    currentY = event.y,
                    startTime = System.currentTimeMillis()
                )
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                activePointers[pointerId] = PointerData(
                    id = pointerId,
                    startX = event.getX(pointerIndex),
                    startY = event.getY(pointerIndex),
                    currentX = event.getX(pointerIndex),
                    currentY = event.getY(pointerIndex),
                    startTime = System.currentTimeMillis()
                )
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    activePointers[pointerId]?.let { pointer ->
                        pointer.currentX = event.getX(i)
                        pointer.currentY = event.getY(i)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                activePointers.clear()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                activePointers.remove(pointerId)
            }

            MotionEvent.ACTION_CANCEL -> {
                activePointers.clear()
            }
        }
    }

    fun getDeltaX(pointerIndex: Int = 0): Float {
        return getPointer(pointerIndex)?.deltaX ?: 0f
    }

    fun getDeltaY(pointerIndex: Int = 0): Float {
        return getPointer(pointerIndex)?.deltaY ?: 0f
    }

    fun getElapsedTime(pointerIndex: Int = 0): Long {
        return getPointer(pointerIndex)?.elapsedTime ?: 0L
    }

    fun getAverageDeltaX(): Float {
        if (activePointers.isEmpty()) return 0f
        return activePointers.values.map { it.deltaX }.average().toFloat()
    }

    fun getAverageDeltaY(): Float {
        if (activePointers.isEmpty()) return 0f
        return activePointers.values.map { it.deltaY }.average().toFloat()
    }

    fun getDistanceBetweenPointers(): Float {
        val pointers = activePointers.values.toList()
        if (pointers.size < 2) return 0f
        val dx = pointers[0].currentX - pointers[1].currentX
        val dy = pointers[0].currentY - pointers[1].currentY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun reset() {
        activePointers.clear()
    }
}
