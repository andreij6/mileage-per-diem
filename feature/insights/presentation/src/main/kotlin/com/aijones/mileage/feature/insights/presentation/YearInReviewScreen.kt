package com.aijones.mileage.feature.insights.presentation

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.Odometer
import com.aijones.mileage.core.designsystem.component.OdometerColors
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.navigation.YearInReviewRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.cos
import kotlin.math.sin

private const val STORY_MS = 5_000
private val Ink = Color.White

@Composable
fun YearInReviewRoute(
    key: YearInReviewRoute,
    onClose: () -> Unit,
    viewModel: YearInReviewViewModel = hiltViewModel<YearInReviewViewModel, YearInReviewViewModel.Factory> { it.create(key) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autoAdvance = remember(context) { autoAdvanceAllowed(context) }
    val chooserTitle = stringResource(R.string.yir_share_chooser)
    val ready = state as? YearInReviewUiState.Ready
    WhiteStatusBar()
    LaunchedEffect(ready?.shareUri) {
        val uri = ready?.shareUri ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri.toUri())
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri.toUri())
        context.startActivity(Intent.createChooser(send, chooserTitle))
        viewModel.onShareHandled()
    }
    YearInReviewScreen(
        state = state,
        autoAdvance = autoAdvance,
        onStory = viewModel::showStory,
        onClose = onClose,
        onShare = { layer ->
            scope.launch {
                val bitmap = layer.toImageBitmap()
                val png = withContext(Dispatchers.Default) {
                    ByteArrayOutputStream().use { out ->
                        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.toByteArray()
                    }
                }
                viewModel.share(png)
            }
        },
    )
}

private fun autoAdvanceAllowed(context: Context): Boolean {
    val a11y = context.getSystemService(AccessibilityManager::class.java)
    return a11y?.isTouchExplorationEnabled != true && ValueAnimator.areAnimatorsEnabled()
}

@Composable
private fun WhiteStatusBar() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val wasLight = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = wasLight }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun YearInReviewScreen(
    state: YearInReviewUiState,
    autoAdvance: Boolean,
    onStory: (Int) -> Unit,
    onClose: () -> Unit,
    onShare: (GraphicsLayer) -> Unit,
) {
    val c = MpdTheme.colors
    val gradient = remember(c) {
        object : ShaderBrush() {
            override fun createShader(size: Size) = LinearGradientShader(
                from = Offset.Zero,
                to = Offset(size.width * 0.4f, size.height),
                colors = listOf(c.storyTop, c.storyBottom),
            )
        }
    }
    Box(Modifier.fillMaxSize().background(gradient)) {
        val ready = state as? YearInReviewUiState.Ready ?: return@Box
        val current by rememberUpdatedState(ready.current)
        val story by rememberUpdatedState(onStory)
        val pagerState = rememberPagerState(initialPage = ready.current) { STORY_COUNT }
        var pressed by remember { mutableStateOf(false) }
        val progress = remember(ready.current) { Animatable(if (autoAdvance) 0f else 1f) }
        val cardLayer = rememberGraphicsLayer()

        LaunchedEffect(ready.current) {
            if (pagerState.currentPage != ready.current) pagerState.animateScrollToPage(ready.current)
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { story(it) }
        }
        LaunchedEffect(ready.current, pressed, autoAdvance) {
            if (!autoAdvance || pressed) return@LaunchedEffect
            progress.animateTo(1f, tween(((1f - progress.value) * STORY_MS).toInt(), easing = LinearEasing))
            if (current < STORY_COUNT - 1) story(current + 1)
        }

        val nextLabel = stringResource(R.string.yir_next)
        val previousLabel = stringResource(R.string.yir_previous)
        HorizontalPager(
            pagerState,
            Modifier
                .fillMaxSize()
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(nextLabel) { story(current + 1); true },
                        CustomAccessibilityAction(previousLabel) { story(current - 1); true },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = { offset -> story(if (offset.x < size.width / 3f) current - 1 else current + 1) },
                    )
                },
            beyondViewportPageCount = 1,
        ) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 114.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (page) {
                    0 -> MilesStory(ready.stories)
                    1 -> PlacesStory(ready.stories)
                    else -> MoneyStory(ready.stories, ready.sharing, cardLayer, onShare = { onShare(cardLayer) })
                }
            }
        }
        StoryChrome(ready.stories.year, ready.current, { progress.value }, onClose)
    }
}

@Composable
private fun StoryChrome(year: Int, current: Int, progress: () -> Float, onClose: () -> Unit) {
    val stateText = stringResource(R.string.yir_progress, current + 1, STORY_COUNT)
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clearAndSetSemantics { stateDescription = stateText },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(STORY_COUNT) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .drawBehind {
                            val r = CornerRadius(2.dp.toPx())
                            drawRoundRect(Ink.copy(alpha = 0.25f), cornerRadius = r)
                            val fill = when {
                                i < current -> 1f
                                i == current -> progress()
                                else -> 0f
                            }
                            if (fill > 0f) drawRoundRect(Ink, size = Size(size.width * fill, size.height), cornerRadius = r)
                        },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.yir_header, year),
                Modifier.weight(1f).semantics { heading() },
                style = MpdTheme.type.labelM,
                color = Ink.copy(alpha = 0.8f),
            )
            IconButton(onClick = onClose) { MpdIcon(MpdIcons.X, Ink, contentDescription = stringResource(R.string.yir_close)) }
        }
    }
}

@Composable
private fun ColumnScope.StoryLead(text: String) {
    Text(text, Modifier.padding(horizontal = 28.dp), style = MpdTheme.type.valueM.copy(fontFamily = MpdTheme.type.bodyL.fontFamily, fontWeight = FontWeight.Medium), color = Ink.copy(alpha = 0.8f))
}

@Composable
private fun ColumnScope.MilesStory(stories: StoriesUi) {
    val c = MpdTheme.colors
    StoryLead(stringResource(R.string.yir_you_drove))
    Odometer(
        tenths = stories.tenths,
        colors = OdometerColors(digit = Ink, drum = Ink.copy(alpha = 0.12f), tenthDrum = c.drumTenth, tenthDigit = c.onDrumTenth),
        modifier = Modifier.padding(horizontal = 28.dp),
        wholeDigits = maxOf(4, (stories.tenths / 10).toString().length),
        size = 50,
        unit = null,
    )
    Text(stringResource(R.string.yir_business_miles, stories.year), Modifier.padding(horizontal = 28.dp), style = MpdTheme.type.valueM, color = Ink)
    EarthRing(stories.earthPercent)
    Text(
        stringResource(R.string.yir_earth, stories.earthPercent),
        Modifier.padding(horizontal = 28.dp),
        style = MpdTheme.type.bodyL.copy(fontSize = 18.sp, lineHeight = 1.4.em),
        color = Ink.copy(alpha = 0.8f),
    )
}

@Composable
private fun EarthRing(percent: Int) {
    val amber = MpdTheme.colors.drumTenth
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clearAndSetSemantics { }
            .drawBehind {
                val stroke = 14.dp.toPx()
                val radius = 120.dp.toPx()
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                drawCircle(Ink.copy(alpha = 0.15f), radius, style = Stroke(stroke))
                drawArc(amber, -90f, 360f * percent.coerceIn(0, 100) / 100f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(Ink.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            MpdIcon(MpdIcons.Globe, Ink, size = 72.dp)
        }
    }
}

@Composable
private fun ColumnScope.PlacesStory(stories: StoriesUi) {
    val c = MpdTheme.colors
    Text(
        stringResource(R.string.yir_where_most),
        Modifier.padding(horizontal = 28.dp).semantics { heading() },
        style = MpdTheme.type.displayM.copy(fontSize = 30.sp, lineHeight = 1.15.em),
        color = Ink,
    )
    if (stories.places.isEmpty()) {
        Text(stringResource(R.string.yir_no_places), Modifier.padding(horizontal = 28.dp), style = MpdTheme.type.bodyL, color = Ink.copy(alpha = 0.8f))
        return
    }
    stories.places.forEach { place ->
        val first = place.rank == 1
        Column(Modifier.padding(horizontal = 28.dp).semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(if (first) Ink else Ink.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(place.rank.toString(), style = MpdTheme.type.labelL.copy(fontWeight = FontWeight.Bold), color = if (first) c.storyBottom else Ink)
                }
                Text(place.label, Modifier.weight(1f), style = MpdTheme.type.titleM, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.yir_trips, place.trips, place.trips), style = MpdTheme.type.bodyS.tnum(), color = Ink.copy(alpha = 0.7f))
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Ink.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(place.fraction).height(8.dp).clip(CircleShape).background(if (first) c.drumTenth else Ink.copy(alpha = 0.6f)))
            }
        }
    }
    RouteSpokes(stories.places.first().label)
}

private val SPOKES = listOf(-20 to 140, 35 to 110, 80 to 95, 130 to 120, 170 to 80, 215 to 130, 260 to 90, 310 to 105)

@Composable
private fun RouteSpokes(hub: String) {
    val amber = MpdTheme.colors.drumTenth
    val description = stringResource(R.string.yir_routes_description, hub)
    Box(
        Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Ink.copy(alpha = 0.06f))
            .clearAndSetSemantics { contentDescription = description }
            .drawWithCache {
                val unit = size.height / 240f
                val hubAt = Offset(size.width / 2, size.height / 2)
                val routes = SPOKES.map { (deg, length) ->
                    val a = Math.toRadians(deg.toDouble())
                    val end = Offset(hubAt.x + (length * cos(a)).toFloat() * unit, hubAt.y + (length * 0.8 * sin(a)).toFloat() * unit)
                    val mid = Offset((hubAt.x + end.x) / 2 + (18 * sin(a)).toFloat() * unit, (hubAt.y + end.y) / 2 - (18 * cos(a)).toFloat() * unit)
                    end to Path().apply { moveTo(hubAt.x, hubAt.y); lineTo(mid.x, mid.y); lineTo(end.x, end.y) }
                }
                val strong = Stroke(3 * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val faint = Stroke(2 * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    routes.forEachIndexed { i, (end, path) ->
                        val top = i < 3
                        drawPath(path, Ink.copy(alpha = if (top) 0.7f else 0.3f), style = if (top) strong else faint)
                        drawCircle(Ink.copy(alpha = if (top) 1f else 0.5f), 4 * unit, end)
                    }
                    drawCircle(Ink, 10 * unit, hubAt)
                    drawCircle(amber, 7 * unit, hubAt)
                }
            },
    )
}

@Composable
private fun ColumnScope.MoneyStory(stories: StoriesUi, sharing: Boolean, layer: GraphicsLayer, onShare: () -> Unit) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val card = remember(c) { Brush.verticalGradient(listOf(c.storyTop, c.storyBottom)) }
    Column(
        Modifier
            .fillMaxWidth()
            .drawWithContent {
                layer.record {
                    drawRect(card)
                    this@drawWithContent.drawContent()
                }
                drawContent()
            }
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.yir_back_in_pocket), style = t.valueM.copy(fontFamily = t.bodyL.fontFamily, fontWeight = FontWeight.Medium), color = Ink.copy(alpha = 0.8f))
        BasicText(
            stories.total,
            style = t.displayL.copy(fontSize = 64.sp, lineHeight = 1.15.em, color = Ink),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = 64.sp),
        )
        if (stories.periods.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ink.copy(alpha = 0.08f)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                stories.periods.forEach { p ->
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.yir_period, p.label, p.miles, p.rate), Modifier.weight(1f), style = t.bodyS.tnum(), color = Ink.copy(alpha = 0.8f))
                        Text(p.amount, style = t.labelM.tnum(), color = Ink)
                    }
                }
            }
        }
        if (stories.perDiem != null) {
            Text(
                pluralStringResource(R.plurals.yir_per_diem, stories.cities, stories.perDiem, stories.cities),
                style = t.titleM.copy(lineHeight = 1.4.em),
                color = Ink,
            )
        }
    }
    Spacer(Modifier.height(132.dp))
    Box(
        Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(Ink.copy(alpha = if (sharing) 0.6f else 1f))
            .clickable(enabled = !sharing, role = Role.Button, onClick = onShare),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MpdIcon(MpdIcons.Share2, c.storyBottom, size = 20.dp)
            Text(stringResource(R.string.yir_share), style = t.labelL, color = c.storyBottom)
        }
    }
    Text(
        stringResource(R.string.yir_share_note),
        Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        style = t.caption,
        color = Ink.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
    )
}

internal val sampleStories = StoriesUi(
    year = 2026,
    tenths = 94_120,
    earthPercent = 38,
    places = listOf(PlaceUi(1, "Acme HQ, Midtown", 64, 1f), PlaceUi(2, "JFK Airport", 21, 0.33f), PlaceUi(3, "Newark Supply", 17, 0.27f)),
    total = "$6,978.82",
    periods = listOf(PeriodUi("Jan – Jun", "4,980", "72.5¢", "$3,610.50"), PeriodUi("Jul – Nov", "4,432", "76¢", "$3,368.32")),
    perDiem = "$4,212",
    cities = 9,
)

@Composable
private fun StoryPreview(dark: Boolean, story: Int) {
    MpdTheme(darkTheme = dark) {
        YearInReviewScreen(YearInReviewUiState.Ready(sampleStories, story), autoAdvance = false, onStory = {}, onClose = {}, onShare = {})
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MilesLightPreview() = StoryPreview(dark = false, story = 0)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MilesDarkPreview() = StoryPreview(dark = true, story = 0)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun PlacesLightPreview() = StoryPreview(dark = false, story = 1)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun PlacesDarkPreview() = StoryPreview(dark = true, story = 1)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MoneyLightPreview() = StoryPreview(dark = false, story = 2)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MoneyDarkPreview() = StoryPreview(dark = true, story = 2)
