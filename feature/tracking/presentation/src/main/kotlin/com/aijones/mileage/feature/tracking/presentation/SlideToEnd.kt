package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

enum class SlideValue { Start, End }

@Composable
fun SlideToEnd(onEnd: () -> Unit, modifier: Modifier = Modifier, initial: SlideValue = SlideValue.Start) {
    val c = MpdTheme.colors
    val haptics = LocalHapticFeedback.current
    val end by rememberUpdatedState(onEnd)
    val label = stringResource(R.string.live_slide_to_end)
    val action = stringResource(R.string.live_end_trip)
    val state = remember { AnchoredDraggableState(initial) }
    BoxWithConstraints(
        modifier
            .height(64.dp)
            .clip(CircleShape)
            .background(c.dangerC)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                customActions = listOf(CustomAccessibilityAction(action) { end(); true })
            },
    ) {
        val density = LocalDensity.current
        val travel = with(density) { (maxWidth - THUMB - PAD * 2).toPx() }.coerceAtLeast(0f)
        LaunchedEffect(travel) {
            state.updateAnchors(DraggableAnchors { SlideValue.Start at 0f; SlideValue.End at travel }, state.targetValue)
        }
        val progress by remember { derivedStateOf { if (travel > 0f && !state.offset.isNaN()) (state.offset / travel).coerceIn(0f, 1f) else 0f } }
        val ended = state.settledValue == SlideValue.End
        LaunchedEffect(state) {
            snapshotFlow { progress >= 0.5f }.distinctUntilChanged().drop(1).filter { it }
                .collect { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
        }
        LaunchedEffect(state) {
            snapshotFlow { state.settledValue }.drop(1).filter { it == SlideValue.End }.collect {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                end()
            }
        }
        val fill = with(density) { (THUMB + PAD * 2).toPx() + progress * travel }
        if (progress > 0f || ended) Box(
            Modifier
                .fillMaxHeight()
                .width(with(density) { fill.toDp() })
                .clip(CircleShape)
                .background(if (ended) c.danger else c.danger.copy(alpha = 0.35f)),
        )
        if (ended) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                MpdIcon(MpdIcons.Check, c.onDanger, size = 22.dp)
                Text(stringResource(R.string.live_trip_ended), Modifier.offset(x = 6.dp), style = MpdTheme.type.labelL, color = c.onDanger)
            }
            return@BoxWithConstraints
        }
        Text(
            label,
            Modifier.align(Alignment.Center).graphicsLayer { alpha = (1f - progress * 1.4f).coerceAtLeast(0.25f) },
            style = MpdTheme.type.labelL,
            color = if (c.isDark) c.text else c.danger,
        )
        Box(
            Modifier
                .offset { IntOffset((PAD.toPx() + (state.offset.takeUnless { it.isNaN() } ?: 0f)).roundToInt(), PAD.roundToPx()) }
                .size(THUMB)
                .clip(CircleShape)
                .background(c.danger)
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state, positionalThreshold = { it * CONFIRM_FRACTION }),
                )
                .pointerInput(Unit) { detectTapGestures(onLongPress = { end() }) },
            contentAlignment = Alignment.Center,
        ) { MpdIcon(MpdIcons.Square, c.onDanger, size = 20.dp) }
    }
}

private val THUMB = 56.dp
private val PAD = 4.dp
private const val CONFIRM_FRACTION = 0.85f
