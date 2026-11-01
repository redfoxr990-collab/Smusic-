package com.smusic.app.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smusic.app.PlayerViewModel
import com.smusic.app.Song
import kotlin.math.min

object N {
    val BgTop = Color(0xFF303030)
    val BgBottom = Color(0xFF1F1F1F)
    val GradA = Color(0xFF333333)
    val GradB = Color(0xFF252525)
    val Dark = Color(0xFF0F0F0F)
    val Light = Color(0xFF3E3E3E)
    val Text = Color(0xFFCDC2AD)
    val Muted = Color(0xFF857F73)
    val Accent = Color(0xFFE6D9BF)
}

/** Raised (embossed) surface: dark shadow bottom-right, light shadow top-left. */
fun Modifier.neuRaised(radius: Dp, depth: Dp = 8.dp): Modifier = drawBehind {
    val r = min(radius.toPx(), min(size.width, size.height) / 2f)
    val d = depth.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = N.GradB.toArgb() }
        val rect = RectF(0f, 0f, size.width, size.height)
        paint.setShadowLayer(d * 1.6f, d, d, N.Dark.toArgb())
        canvas.nativeCanvas.drawRoundRect(rect, r, r, paint)
        paint.setShadowLayer(d * 1.6f, -d, -d, N.Light.toArgb())
        canvas.nativeCanvas.drawRoundRect(rect, r, r, paint)
    }
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(N.GradA, N.GradB),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(r, r)
    )
}

/** Pressed-in / engraved surface. */
fun Modifier.neuInset(radius: Dp): Modifier = drawBehind {
    val r = min(radius.toPx(), min(size.width, size.height) / 2f)
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFF1B1B1B), Color(0xFF2B2B2B)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(r, r)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color(0x00FFFFFF), Color(0x1AFFFFFF)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(1.dp.toPx())
    )
}

@Composable
fun NeuButton(
    size: Dp,
    radius: Dp = size / 2,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        Modifier
            .size(size)
            .then(if (pressed) Modifier.neuInset(radius) else Modifier.neuRaised(radius, 5.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun AlbumArt(vm: PlayerViewModel, song: Song?, modifier: Modifier, radius: Dp) {
    val bmp by produceState<ImageBitmap?>(null, song?.id) {
        value = song?.let { vm.loadArt(it) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .neuInset(radius),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Filled.MusicNote, null, tint = N.Muted, modifier = Modifier.size(40.dp))
        }
    }
}

private fun PointerInputScope.fraction(x: Float): Float {
    val pad = 8.dp.toPx()
    return ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
}

@Composable
fun SeekBar(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val shown = drag ?: progress
    val seek by rememberUpdatedState(onSeek)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) { detectTapGestures { seek(fraction(it.x)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = fraction(it.x) },
                    onDragEnd = { drag?.let { f -> seek(f) }; drag = null },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { change, _ -> drag = fraction(change.position.x) }
                )
            }
    ) {
        val cy = size.height / 2f
        val pad = 8.dp.toPx()
        val w = size.width - 2 * pad
        val stroke = 3.dp.toPx()
        drawLine(Color(0xFF4A4A4A), Offset(pad, cy), Offset(pad + w, cy), stroke, StrokeCap.Round)
        drawLine(Color(0xFFB9AF9C), Offset(pad, cy), Offset(pad + w * shown, cy), stroke, StrokeCap.Round)
        val c = Offset(pad + w * shown, cy)
        drawCircle(Color(0xFF2B2B2B), 8.dp.toPx(), c)
        drawCircle(Color(0xFFB9AF9C), 8.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
    }
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
