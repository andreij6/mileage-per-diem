package com.aijones.mileage.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.GeoPoint
import kotlin.math.roundToInt

@Composable
fun RouteCanvas(
    points: List<GeoPoint>,
    description: String,
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    replayAt: Float? = null,
    live: Boolean = false,
    startLabel: String? = null,
    endLabel: String? = null,
    showScale: Boolean = true,
    drawOn: Boolean = false,
    padding: Float = 28f,
) {
    val c = MpdTheme.colors
    val reveal = remember(points.size) { Animatable(if (drawOn) 0f else 1f) }
    if (drawOn) LaunchedEffect(points.size) { reveal.animateTo(1f, tween(700)) }
    BoxWithConstraints(modifier.background(c.mapBg).semantics { contentDescription = description }) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { padding.dp.toPx() }
        val proj = remember(points, wPx, hPx) { RouteProjection(points, wPx, hPx, padPx) }
        val xy = remember(proj) { points.map { proj.x(it) to proj.y(it) } }
        Canvas(Modifier.matchParentSize()) {
            val step = 24.dp.toPx()
            val w = size.width
            val h = size.height
            val dots = buildList {
                var x = step / 2
                while (x < w) {
                    var y = step / 2
                    while (y < h) { add(Offset(x, y)); y += step }
                    x += step
                }
            }
            drawPoints(dots, PointMode.Points, c.mapDot, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            if (xy.size < 2) return@Canvas
            fun path(pts: List<Pair<Float, Float>>) = Path().apply {
                moveTo(pts[0].first, pts[0].second)
                for (i in 1 until pts.size) lineTo(pts[i].first, pts[i].second)
            }
            val shown = prefixByLength(xy, (replayAt ?: progress) * reveal.value)
            if (replayAt != null) {
                drawPath(path(xy), c.outline2, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val p = path(shown)
            drawPath(p, c.primaryC, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(p, c.primary, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            fun pin(at: Pair<Float, Float>, ring: Color) {
                drawCircle(c.surface, 9.dp.toPx(), Offset(at.first, at.second))
                drawCircle(ring, 6.5.dp.toPx(), Offset(at.first, at.second), style = Stroke(5.dp.toPx()))
            }
            pin(xy.first(), c.text2)
            val head = shown.last()
            if (live || replayAt != null) {
                drawCircle(c.primary.copy(alpha = 0.2f), 22.dp.toPx(), Offset(head.first, head.second))
                drawCircle(Color.White, 10.5.dp.toPx(), Offset(head.first, head.second))
                drawCircle(c.primary, 7.5.dp.toPx(), Offset(head.first, head.second))
            }
            if (replayAt != null || (!live && reveal.value >= 1f && progress >= 1f)) pin(xy.last(), c.primary)
        }
        if (xy.size >= 2) {
            startLabel?.let { Label(it, xy.first().first + with(density) { 16.dp.toPx() }, xy.first().second - with(density) { 12.dp.toPx() }) }
            endLabel?.let { Label(it, xy.last().first - with(density) { 120.dp.toPx() }, xy.last().second + with(density) { 16.dp.toPx() }) }
        }
        if (showScale && points.size >= 2) {
            val (barPx, label) = proj.niceScale(with(density) { 48.dp.toPx() })
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(with(density) { barPx.toDp() }).height(3.dp).clip(CircleShape).background(c.text2))
                Text(label, Modifier.padding(start = 6.dp), style = MpdTheme.type.micro, color = c.text2)
            }
        }
    }
}

@Composable
private fun Label(text: String, x: Float, y: Float) {
    Text(
        text,
        Modifier
            .offset { IntOffset(x.roundToInt().coerceAtLeast(4), y.roundToInt().coerceAtLeast(4)) }
            .clip(CircleShape)
            .background(MpdTheme.colors.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MpdTheme.type.label,
        color = MpdTheme.colors.text,
    )
}
