package com.zhan9san.hexchain.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.zhan9san.hexchain.core.Cell
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLUMNS = 12

/**
 * Hexagon geometry for a chart [width] pixels wide (core section 3): pointy-top
 * hexagons, long rows of 12 cells and short rows shifted right by half a cell.
 */
private class Geometry(width: Float) {
    val hexWidth = width / COLUMNS
    val radius = hexWidth / sqrt(3f)

    fun centre(cell: Cell): Offset {
        val x = hexWidth / 2 + cell.col * hexWidth + if (cell.row % 2 == 1) hexWidth / 2 else 0f
        return Offset(x, radius + cell.row * 1.5f * radius)
    }

    fun hexagon(centre: Offset, r: Float) = Path().apply {
        for (i in 0 until 6) {
            val angle = Math.toRadians(-90.0 + 60 * i)
            val p = Offset(centre.x + r * cos(angle).toFloat(), centre.y + r * sin(angle).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

    companion object {
        /** Width / height of a 16-row chart: 12 hexagons wide, 24.5 radii high. */
        val aspectRatio = COLUMNS * sqrt(3f) / 24.5f
    }
}

/**
 * The honeycomb chart (A-9 to A-12). [highlight] maps a cell to the colour of the
 * latest round that highlights it. Cells are hidden from screen readers (A-24).
 */
@Composable
fun HoneycombChart(grid: List<List<Int>>, highlight: Map<Cell, Color>, modifier: Modifier = Modifier) {
    val colours = LocalChartColours.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 16)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .clipToBounds()
            .testTag("chart")
            .clearAndSetSemantics {}
            // A-11: pinch-to-zoom and pan; double-tap resets.
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    val maxX = (scale - 1) * size.width / 2
                    val maxY = (scale - 1) * size.height / 2
                    offset = Offset(
                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                        (offset.y + pan.y).coerceIn(-maxY, maxY),
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offset = Offset.Zero
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .aspectRatio(Geometry.aspectRatio)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            val g = Geometry(size.width)
            val style = TextStyle(fontSize = (g.radius * 0.85f).toSp(), fontWeight = FontWeight.Medium)
            val outline = Stroke(width = 2.5.dp.toPx())
            val border = Stroke(width = 1.dp.toPx())
            grid.forEachIndexed { r, row ->
                row.forEachIndexed { c, digit ->
                    val cell = Cell(r, c)
                    val centre = g.centre(cell)
                    val hex = g.hexagon(centre, g.radius)
                    drawPath(hex, colours.tones[(r / 2 + c + (r % 2) * 2) % colours.tones.size])
                    drawPath(hex, colours.border, style = border)
                    highlight[cell]?.let { colour ->
                        drawPath(hex, colour.copy(alpha = 0.38f))
                        drawPath(g.hexagon(centre, g.radius * 0.88f), colour, style = outline)
                    }
                    val text = textMeasurer.measure(digit.toString(), style)
                    drawText(
                        text,
                        color = colours.digit,
                        topLeft = centre - Offset(text.size.width / 2f, text.size.height / 2f),
                    )
                }
            }
        }
    }
}
