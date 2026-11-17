package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.SpaceGrotesk
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.navigation.EditTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.core.navigation.TripsRoute
import com.aijones.mileage.core.ui.RouteCanvas
import java.util.Random

@Composable
fun TripReceiptRoute(key: TripReceiptRoute, navigator: Navigator) {
    val vm = hiltViewModel<TripReceiptViewModel, TripReceiptViewModel.Factory>(creationCallback = { it.create(key) })
    val state by vm.state.collectAsStateWithLifecycle()
    TripReceiptScreen(
        state,
        onEdit = { navigator.navigate(EditTripRoute(key.tripId)) },
        onDone = { navigator.switchTab(TripsRoute) },
    )
}

@Composable
fun TripReceiptScreen(state: TripReceiptUiState, onEdit: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier, animate: Boolean = true) {
    val c = MpdTheme.colors
    Column(modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(c.primaryC), contentAlignment = Alignment.Center) {
                MpdIcon(MpdIcons.Check, c.onPrimaryC, size = 24.dp)
            }
            Text(
                stringResource(R.string.receipt_saved),
                Modifier.padding(top = 6.dp).semantics { heading() },
                style = MpdTheme.type.headlineS,
                color = c.text,
            )
            state.receipt?.let { PrintedReceipt(it, animate, Modifier.padding(top = 34.dp, start = 36.dp, end = 36.dp)) }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MpdButton(stringResource(R.string.receipt_edit), onEdit, Modifier.weight(1f), kind = ButtonKind.Outlined, icon = MpdIcons.Pencil, enabled = state.receipt != null)
            MpdButton(stringResource(R.string.receipt_done), onDone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PrintedReceipt(receipt: ReceiptUi, animate: Boolean, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    var printed by rememberSaveable(receipt.tripId) { mutableStateOf(!animate || reduced) }
    val reveal = remember { Animatable(if (printed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!printed) {
            reveal.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = PRINT_STIFFNESS))
            printed = true
        }
    }
    Box(
        modifier.drawWithContent {
            clipRect(left = -size.width, top = -PRINT_SHADOW_PX, right = size.width * 2, bottom = size.height * reveal.value.coerceAtMost(1f) + PRINT_SHADOW_PX) {
                this@drawWithContent.drawContent()
            }
        },
    ) { Receipt(receipt) }
}

private val ReceiptPaper = GenericShape { size, _ ->
    val tooth = 12f * (size.width / 340f).coerceAtLeast(0.1f)
    val depth = tooth * 7f / 12f
    val n = (size.width / tooth).toInt().coerceAtLeast(1)
    val w = size.width
    val h = size.height
    moveTo(0f, depth)
    for (i in 0 until n) {
        lineTo((i + 0.5f) * w / n, if (i % 2 == 0) 0f else depth)
        lineTo((i + 1f) * w / n, depth)
    }
    lineTo(w, h - depth)
    for (i in 0 until n) {
        lineTo(w - (i + 0.5f) * w / n, if (i % 2 == 0) h else h - depth)
        lineTo(w - (i + 1f) * w / n, h - depth)
    }
    close()
}

@Composable
private fun Receipt(r: ReceiptUi) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 600.dp)
            .shadow(16.dp, ReceiptPaper, clip = false)
            .background(c.surface, ReceiptPaper)
            .padding(horizontal = 24.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.receipt_title),
                Modifier.semantics { heading() },
                style = t.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.12.em),
                color = c.text2,
            )
            Text(r.number, style = t.caption.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium), color = c.text2)
        }
        Text(r.dateLine, style = t.bodyS.copy(fontWeight = FontWeight.Medium), color = c.text)
        RouteCanvas(
            points = r.route,
            description = stringResource(R.string.receipt_route_description, r.distance),
            modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)),
            showScale = false,
            padding = 18f,
        )
        r.places?.let { Text(it, Modifier.fillMaxWidth(), style = t.bodyS.copy(fontWeight = FontWeight.SemiBold), color = c.text) }
        ReceiptRow(stringResource(R.string.receipt_distance), r.distance)
        ReceiptRow(stringResource(R.string.receipt_duration), durationText(r.minutes))
        ReceiptRow(
            r.rateFrom?.let { stringResource(R.string.receipt_rate_irs, it) } ?: stringResource(R.string.receipt_rate_custom),
            stringResource(R.string.receipt_rate_value, r.rate),
        )
        ReceiptRow(stringResource(R.string.receipt_purpose), r.purpose ?: stringResource(R.string.receipt_purpose_none))
        Perforation()
        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Bottom) {
            Text(
                stringResource(R.string.receipt_total),
                Modifier.weight(1f).padding(bottom = 6.dp),
                style = t.bodyS.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.1.em),
                color = c.text,
            )
            Text(r.total, style = t.displayM.tnum(), color = c.text)
        }
        Perforation()
        Barcode(r.tripId)
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    val c = MpdTheme.colors
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MpdTheme.type.bodyS, color = c.text2)
        Text(value, style = MpdTheme.type.bodyS.copy(fontWeight = FontWeight.Medium).tnum(), color = c.text)
    }
}

@Composable
private fun Perforation() {
    val color = MpdTheme.colors.outline2
    Canvas(Modifier.fillMaxWidth().height(2.dp).clearAndSetSemantics {}) {
        val dash = 6.dp.toPx()
        val step = 11.dp.toPx()
        val y = size.height / 2
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, y), Offset((x + dash).coerceAtMost(size.width), y), 1.5.dp.toPx())
            x += step
        }
    }
}

@Composable
private fun Barcode(seed: Long) {
    val color = MpdTheme.colors.text
    val widths = remember(seed) { Random(seed).let { rnd -> IntArray(BARS) { 1 + rnd.nextInt(4) } } }
    Canvas(Modifier.fillMaxWidth().height(36.dp).clipToBounds().clearAndSetSemantics {}) {
        val unit = 1.dp.toPx()
        var x = 0f
        var i = 0
        while (x < size.width) {
            val w = widths[i % widths.size] * unit
            if (i % 2 == 0) drawRect(color, Offset(x, 0f), Size(w, size.height))
            x += w + 1.5f * unit
            i++
        }
    }
}

private const val PRINT_STIFFNESS = 120f
private const val PRINT_SHADOW_PX = 80f
private const val BARS = 64

private val previewReceipt = ReceiptUi(
    tripId = 186,
    number = "#0186",
    dateLine = "Sun, Nov 16 · 9:12 – 9:41 AM",
    route = previewRoute,
    places = "Brooklyn office → Acme HQ",
    distance = "18.4 mi",
    minutes = 29,
    rateFrom = "Jul 1",
    rate = "76¢",
    purpose = "Client meeting",
    total = "$13.98",
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TripReceiptLightPreview() = MpdTheme(darkTheme = false) { TripReceiptScreen(TripReceiptUiState(previewReceipt), {}, {}, animate = false) }

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TripReceiptDarkPreview() = MpdTheme(darkTheme = true) { TripReceiptScreen(TripReceiptUiState(previewReceipt), {}, {}, animate = false) }
