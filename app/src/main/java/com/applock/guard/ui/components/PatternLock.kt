package com.applock.guard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.applock.guard.ui.theme.*
import kotlin.math.sqrt

@Composable
fun PatternLock(
    onPatternComplete: (List<Int>) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentDragPos by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val dotColor = if (isError) ErrorRed else PatternDotDefault
    val selectedColor = if (isError) ErrorRed else PatternDotSelected
    val lineColor = if (isError) ErrorRed.copy(alpha = 0.7f) else PatternLineColor.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedDots.clear()
                            isDragging = true
                            currentDragPos = offset

                            val dotIndex = findNearestDot(offset, size.width.toFloat(), size.height.toFloat())
                            if (dotIndex != null && dotIndex !in selectedDots) {
                                selectedDots.add(dotIndex)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = { change, _ ->
                            currentDragPos = change.position

                            val dotIndex = findNearestDot(
                                change.position,
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                            if (dotIndex != null && dotIndex !in selectedDots) {
                                selectedDots.add(dotIndex)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            currentDragPos = null
                            if (selectedDots.size >= 3) {
                                onPatternComplete(selectedDots.toList())
                            }
                            selectedDots.clear()
                        },
                        onDragCancel = {
                            isDragging = false
                            currentDragPos = null
                            selectedDots.clear()
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val spacing = w / 4f
            val dotRadius = 14f
            val selectedDotRadius = 20f
            val outerRingRadius = 32f

            // Calculate dot positions
            val dotPositions = mutableListOf<Offset>()
            for (row in 0..2) {
                for (col in 0..2) {
                    val x = spacing + col * spacing
                    val y = spacing + row * spacing
                    dotPositions.add(Offset(x, y))
                }
            }

            // Draw lines between selected dots
            for (i in 0 until selectedDots.size - 1) {
                val from = dotPositions[selectedDots[i]]
                val to = dotPositions[selectedDots[i + 1]]
                drawLine(
                    color = lineColor,
                    start = from,
                    end = to,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // Draw line from last selected dot to current drag position
            if (isDragging && selectedDots.isNotEmpty() && currentDragPos != null) {
                val lastDot = dotPositions[selectedDots.last()]
                drawLine(
                    color = lineColor.copy(alpha = 0.4f),
                    start = lastDot,
                    end = currentDragPos!!,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // Draw dots
            dotPositions.forEachIndexed { index, position ->
                val isSelected = index in selectedDots

                if (isSelected) {
                    // Outer ring
                    drawCircle(
                        color = selectedColor.copy(alpha = 0.2f),
                        radius = outerRingRadius,
                        center = position
                    )
                    // Inner dot
                    drawCircle(
                        color = selectedColor,
                        radius = selectedDotRadius,
                        center = position
                    )
                } else {
                    // Default dot
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = position
                    )
                }
            }
        }
    }
}

private fun findNearestDot(
    position: Offset,
    width: Float,
    height: Float,
    threshold: Float = 60f
): Int? {
    val spacing = width / 4f
    val dotPositions = mutableListOf<Offset>()

    for (row in 0..2) {
        for (col in 0..2) {
            val x = spacing + col * spacing
            val y = spacing + row * spacing
            dotPositions.add(Offset(x, y))
        }
    }

    var nearestIndex: Int? = null
    var nearestDistance = Float.MAX_VALUE

    dotPositions.forEachIndexed { index, dotPos ->
        val dx = position.x - dotPos.x
        val dy = position.y - dotPos.y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < threshold && distance < nearestDistance) {
            nearestDistance = distance
            nearestIndex = index
        }
    }

    return nearestIndex
}
