package com.aijones.mileage.feature.trips.presentation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.component.SelectChip
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.ui.RouteCanvas
import com.aijones.mileage.feature.trips.domain.TripCategory
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.launch

@Composable
internal fun ReviewRoute(navigator: Navigator, viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewScreen(
        state = state,
        onBack = navigator::back,
        onClassify = viewModel::classify,
        onUndo = viewModel::undo,
        onSelectPurpose = viewModel::selectPurpose,
    )
}

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onBack: () -> Unit,
    onClassify: (TripCategory, String) -> Unit,
    onUndo: () -> Unit,
    onSelectPurpose: (String) -> Unit,
) {
    val c = MpdTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg).navigationBarsPadding()) {
        MpdTopBar(stringResource(R.string.trips_review_title), onBack = onBack)
        if (state.loading) return@Column
        ProgressSegments(state)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = state.top,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f)) togetherWith fadeOut(tween(180))
                },
                contentKey = { it == null },
                label = "reviewDeck",
            ) { top ->
                if (top == null) DoneState(onBack) else Deck(state, top, onClassify, onSelectPurpose)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top,
        ) {
            val enabled = state.top != null
            DeckButton(MpdIcons.X, stringResource(R.string.trips_personal), 64, c.surface, c.danger, true, enabled) {
                onClassify(TripCategory.PERSONAL, "button")
            }
            DeckButton(MpdIcons.RotateCcw, stringResource(R.string.trips_undo), 48, c.surface, c.text2, true, state.canUndo, onUndo)
            DeckButton(MpdIcons.Check, stringResource(R.string.trips_business), 64, c.primary, c.onPrimary, false, enabled) {
                onClassify(TripCategory.BUSINESS, "button")
            }
        }
        Text(
            stringResource(R.string.trips_review_hint),
            Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 28.dp, start = 16.dp, end = 16.dp),
            style = MpdTheme.type.caption,
            color = c.text2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProgressSegments(state: ReviewUiState) {
    val c = MpdTheme.colors
    val description = stringResource(R.string.trips_review_progress, state.reviewed, state.total)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp)
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(state.total) { i ->
            val segColor by animateColorAsState(if (i <= state.reviewed) c.primary else c.outline, tween(240), label = "seg$i")
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(segColor))
        }
    }
}

@Composable
private fun Deck(state: ReviewUiState, top: ReviewCardUi, onClassify: (TripCategory, String) -> Unit, onSelectPurpose: (String) -> Unit) {
    val c = MpdTheme.colors
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember(top.id) { Animatable(0f) }
    val markBusiness = stringResource(R.string.trips_mark_business)
    val markPersonal = stringResource(R.string.trips_mark_personal)
    BoxWithConstraints(Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
        val widthPx = with(density) { maxWidth.toPx() }
        val progress = (abs(offsetX.value) / (widthPx * SWIPE_THRESHOLD)).coerceIn(0f, 1f)
        val behind = (state.cards.size - 1).coerceAtMost(2)
        for (i in behind downTo 1) {
            val card = state.cards[i]
            val base = if (i == 2) 0.88f else 0.94f
            val scale = base + (0.06f * progress)
            val dy = if (i == 2) 34.dp else 18.dp
            Column(
                Modifier
                    .offset(y = dy + 220.dp * (1 - scale))
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clearAndSetSemantics {}
                    .width(CARD_WIDTH)
                    .heightIn(min = minOf(CARD_HEIGHT, maxHeight), max = maxHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.surface)
                    .border(1.dp, c.outline, RoundedCornerShape(28.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CardContent(card, card.purposes.firstOrNull(), offset = 0f, progress = 0f, onSelectPurpose = {})
            }
        }
        Column(
            Modifier
                .pointerInput(top.id) {
                    val tracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x
                            val value = offsetX.value
                            val direction = when {
                                abs(value) > widthPx * SWIPE_THRESHOLD -> sign(value)
                                abs(velocity) > FLING_VELOCITY && sign(velocity) == sign(value) -> sign(value)
                                else -> 0f
                            }
                            scope.launch {
                                if (direction == 0f) {
                                    offsetX.animateTo(0f, spring())
                                } else {
                                    offsetX.animateTo(direction * widthPx * 1.4f)
                                    onClassify(if (direction > 0) TripCategory.BUSINESS else TripCategory.PERSONAL, "swipe")
                                }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + amount) }
                        },
                    )
                }
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = offsetX.value / density.density / 18f
                }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(markBusiness) { onClassify(TripCategory.BUSINESS, "button"); true },
                        CustomAccessibilityAction(markPersonal) { onClassify(TripCategory.PERSONAL, "button"); true },
                    )
                }
                .width(CARD_WIDTH)
                .heightIn(min = minOf(CARD_HEIGHT, maxHeight), max = maxHeight)
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .border(1.dp, c.outline, RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardContent(top, state.selectedPurpose, offsetX.value, progress, onSelectPurpose)
        }
    }
}

@Composable
private fun CardContent(card: ReviewCardUi, selected: String?, offset: Float, progress: Float, onSelectPurpose: (String) -> Unit) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val title = routeTitle(card.from, card.to)
    val unknown = stringResource(R.string.trips_unknown_place)
    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp))) {
        RouteCanvas(
            points = card.points,
            description = stringResource(R.string.trips_route_description, card.distance, card.from ?: unknown, card.to ?: unknown),
            modifier = Modifier.fillMaxSize(),
            showScale = false,
        )
        if (offset != 0f) {
            val business = offset > 0
            val ink = if (business) c.primary else c.danger
            Text(
                stringResource(if (business) R.string.trips_stamp_business else R.string.trips_stamp_personal),
                Modifier
                    .padding(16.dp)
                    .graphicsLayer { rotationZ = if (business) 12f else -12f; alpha = progress }
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surface.copy(alpha = 0.9f))
                    .border(3.dp, ink, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = t.valueS.copy(letterSpacing = t.overline.letterSpacing),
                color = ink,
            )
        }
    }
    Text(title, style = t.valueM, color = c.text)
    Text(card.subtitle, style = t.bodyS.tnum(), color = c.text2)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(card.worth, style = t.valueL.tnum(), color = c.primary)
        Text(stringResource(R.string.trips_if_business), style = t.caption, color = c.text2)
    }
    SectionLabel(stringResource(R.string.trips_purpose))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        card.purposes.forEach { p -> SelectChip(p, p == selected, onClick = { onSelectPurpose(p) }) }
    }
}

@Composable
private fun DeckButton(
    @DrawableRes icon: Int,
    label: String,
    size: Int,
    bg: Color,
    fg: Color,
    outlined: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = MpdTheme.colors
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(bg)
                .then(if (outlined) Modifier.border(1.dp, c.outline, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { MpdIcon(icon, fg, size = if (size == 64) 28.dp else 20.dp) }
        Text(label, style = MpdTheme.type.caption.copy(fontWeight = MpdTheme.type.label.fontWeight), color = c.text2)
    }
}

@Composable
private fun DoneState(onBack: () -> Unit) {
    val c = MpdTheme.colors
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        IconTile(MpdIcons.CircleCheck, c.primaryC, c.onPrimaryC, size = 64.dp, iconSize = 30.dp, radius = 32.dp)
        Text(stringResource(R.string.trips_review_done_title), style = MpdTheme.type.titleM, color = c.text)
        Text(stringResource(R.string.trips_review_done_body), style = MpdTheme.type.bodyM, color = c.text2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        MpdButton(stringResource(R.string.trips_review_done_action), onBack, height = 48.dp)
    }
}

private val CARD_WIDTH = 340.dp
private val CARD_HEIGHT = 440.dp
private const val SWIPE_THRESHOLD = 0.4f
private const val FLING_VELOCITY = 1_500f

private val sampleRoute = listOf(
    GeoPoint(40.6782, -73.9442), GeoPoint(40.6950, -73.9900), GeoPoint(40.6975, -74.0400), GeoPoint(40.7050, -74.0650),
    GeoPoint(40.7200, -74.0800), GeoPoint(40.7300, -74.1100), GeoPoint(40.7357, -74.1724), GeoPoint(40.7310, -74.2100),
)

internal val sampleReview = ReviewUiState(
    loading = false,
    cards = listOf(
        ReviewCardUi(1, "Home", "Newark Supply", "Thu, Nov 13 · 11:05 – 11:51 · 24.8 mi", "24.8 mi", "$18.85", listOf("Site visit", "Supplies", "Client"), sampleRoute),
        ReviewCardUi(2, "Office", "Acme HQ", "Wed, Nov 11 · 17:30 – 18:01 · 17.9 mi", "17.9 mi", "$13.60", listOf("Site visit", "Supplies", "Client")),
        ReviewCardUi(3, "Home", "JFK Airport", "Sun, Nov 8 · 7:15 – 7:53 · 15.6 mi", "15.6 mi", "$11.86", listOf("Site visit", "Supplies", "Client")),
    ),
    selectedPurpose = "Site visit",
)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun ReviewLightPreview() = MpdTheme(darkTheme = false) { ReviewScreen(sampleReview, {}, { _, _ -> }, {}, {}) }

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun ReviewDarkPreview() = MpdTheme(darkTheme = true) { ReviewScreen(sampleReview, {}, { _, _ -> }, {}, {}) }

@Preview(widthDp = 412, heightDp = 879, fontScale = 2f)
@Composable
private fun ReviewLargeFontPreview() = MpdTheme(darkTheme = false) { ReviewScreen(sampleReview, {}, { _, _ -> }, {}, {}) }
