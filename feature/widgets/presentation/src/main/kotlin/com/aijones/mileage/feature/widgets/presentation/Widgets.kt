package com.aijones.mileage.feature.widgets.presentation

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.DarkColors
import com.aijones.mileage.core.designsystem.theme.LightColors
import com.aijones.mileage.core.designsystem.theme.MpdColors
import androidx.glance.material3.ColorProviders

private fun MpdColors.scheme() = if (isDark) {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryC, onPrimaryContainer = onPrimaryC,
        surface = surface, onSurface = text, surfaceVariant = surface2, onSurfaceVariant = text2,
        background = bg, onBackground = text, error = danger, onError = onDanger, outline = outline2, outlineVariant = outline,
    )
} else {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primaryC, onPrimaryContainer = onPrimaryC,
        surface = surface, onSurface = text, surfaceVariant = surface2, onSurfaceVariant = text2,
        background = bg, onBackground = text, error = danger, onError = onDanger, outline = outline2, outlineVariant = outline,
    )
}

private val MpdGlanceColors = ColorProviders(light = LightColors.scheme(), dark = DarkColors.scheme())

private val WidgetRadius = 28.dp

abstract class SnapshotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val updater = context.widgetEntryPoint().widgetUpdater()
        val initial = updater.current()
        provideContent {
            val snapshot by updater.snapshots.collectAsState(initial)
            GlanceTheme(colors = MpdGlanceColors) { Content(snapshot) }
        }
    }

    @Composable
    abstract fun Content(snapshot: WidgetSnapshot)
}

class MonthWidget : SnapshotWidget() {
    @Composable
    override fun Content(snapshot: WidgetSnapshot) = MonthWidgetContent(snapshot.month)
}

class PerDiemWidget : SnapshotWidget() {
    @Composable
    override fun Content(snapshot: WidgetSnapshot) = PerDiemWidgetContent(snapshot.perDiem)
}

class RecordingWidget : SnapshotWidget() {
    @Composable
    override fun Content(snapshot: WidgetSnapshot) = RecordingWidgetContent(snapshot.recording)
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

class PerDiemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PerDiemWidget()
}

class RecordingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecordingWidget()
}

class StopTripAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entryPoint = context.widgetEntryPoint()
        entryPoint.analytics().track(AnalyticsEvent.WidgetAction(WidgetNames.RECORDING, "stop"))
        entryPoint.stopTrip()()
    }
}

@Composable
private fun WidgetText(text: String, color: ColorProvider, size: TextUnit, weight: FontWeight = FontWeight.Normal) =
    Text(text, style = TextStyle(color = color, fontSize = size, fontWeight = weight), maxLines = 1)

@Composable
private fun Icon(@DrawableRes icon: Int, tint: ColorProvider, size: Dp) {
    Image(ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size(size), colorFilter = ColorFilter.tint(tint))
}

@Composable
private fun WidgetHeader(@DrawableRes icon: Int, text: String, color: ColorProvider, tint: ColorProvider) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, tint, 16.dp)
        Spacer(GlanceModifier.width(6.dp))
        WidgetText(text, color, 12.sp, FontWeight.Medium)
    }
}

@Composable
internal fun MonthWidgetContent(month: MonthWidgetUi) {
    val context = LocalContext.current
    val c = GlanceTheme.colors
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(WidgetRadius).background(c.surface).padding(18.dp)
            .clickable(actionStartActivity(openAppIntent(context))),
    ) {
        WidgetHeader(MpdIcons.Route, month.monthName, c.onSurfaceVariant, c.primary)
        Spacer(GlanceModifier.height(10.dp))
        WidgetText(month.amount, c.onSurface, 28.sp, FontWeight.Bold)
        Spacer(GlanceModifier.height(10.dp))
        val spoken = context.getString(R.string.widget_odometer_description, "${month.whole.trimStart('0').ifEmpty { "0" }}.${month.tenth}")
        Row(GlanceModifier.semantics { contentDescription = spoken }, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                month.whole.forEach { digit -> Drum(digit, c.surfaceVariant, c.onSurface, GlanceModifier.padding(end = 3.dp)) }
            }
            Box(GlanceModifier.padding(end = 3.dp)) { Box(GlanceModifier.size(3.dp).cornerRadius(2.dp).background(c.onSurface)) { } }
            Drum(month.tenth.first(), c.primary, c.onPrimary, GlanceModifier.padding(end = 3.dp))
            WidgetText(context.getString(R.string.widget_mi), c.onSurface, 8.sp, FontWeight.Medium)
        }
        Spacer(GlanceModifier.defaultWeight())
        StartPill(WidgetNames.MONTH)
    }
}

@Composable
private fun Drum(digit: Char, bg: ColorProvider, ink: ColorProvider, outer: GlanceModifier = GlanceModifier) {
    Box(outer) {
        Box(GlanceModifier.size(12.dp, 20.dp).cornerRadius(3.dp).background(bg), contentAlignment = Alignment.Center) {
            WidgetText(digit.toString(), ink, 12.sp, FontWeight.Bold)
        }
    }
}

@Composable
private fun StartPill(widget: String, modifier: GlanceModifier = GlanceModifier.fillMaxWidth()) {
    val context = LocalContext.current
    val c = GlanceTheme.colors
    Row(
        modifier.height(40.dp).cornerRadius(20.dp).background(c.primary).padding(horizontal = 16.dp)
            .clickable(actionStartActivity(startTripIntent(context, widget))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(MpdIcons.Play, c.onPrimary, 16.dp)
        Spacer(GlanceModifier.width(6.dp))
        WidgetText(context.getString(R.string.widget_start_trip), c.onPrimary, 14.sp, FontWeight.Medium)
    }
}

@Composable
internal fun PerDiemWidgetContent(perDiem: PerDiemWidgetUi?) {
    val context = LocalContext.current
    val c = GlanceTheme.colors
    val ink = c.onPrimaryContainer
    Column(
        GlanceModifier.fillMaxSize().cornerRadius(WidgetRadius).background(c.primaryContainer).padding(18.dp)
            .clickable(actionStartActivity(openAppIntent(context))),
    ) {
        WidgetHeader(MpdIcons.Utensils, context.getString(R.string.widget_per_diem_title), ink, ink)
        Spacer(GlanceModifier.height(8.dp))
        if (perDiem == null) {
            Text(context.getString(R.string.widget_per_diem_empty), style = TextStyle(color = ink, fontSize = 13.sp))
            return@Column
        }
        WidgetText(perDiem.mie, ink, 34.sp, FontWeight.Bold)
        Spacer(GlanceModifier.height(8.dp))
        Text(context.getString(R.string.widget_per_diem_place, perDiem.city), style = TextStyle(color = ink, fontSize = 13.sp), maxLines = 2)
        Spacer(GlanceModifier.defaultWeight())
        WidgetText(context.getString(R.string.widget_lodging, perDiem.lodging), ink, 13.sp, FontWeight.Medium)
    }
}

@Composable
internal fun RecordingWidgetContent(recording: RecordingWidgetUi) {
    val context = LocalContext.current
    val c = GlanceTheme.colors
    val active = recording as? RecordingWidgetUi.Active
    Row(
        GlanceModifier.fillMaxSize().cornerRadius(WidgetRadius).background(c.surface).padding(start = 18.dp, end = 12.dp)
            .clickable(actionStartActivity(if (active != null) openLiveTripIntent(context) else openAppIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(12.dp).cornerRadius(6.dp).background(if (active?.paused == false) c.error else c.onSurfaceVariant)) { }
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight()) {
            if (active == null) {
                WidgetText(context.getString(R.string.widget_not_recording), c.onSurface, 16.sp, FontWeight.Medium)
            } else {
                val title = if (active.paused) R.string.widget_paused else R.string.widget_recording
                WidgetText(context.getString(title, active.distance), c.onSurface, 16.sp, FontWeight.Medium)
                WidgetText(context.getString(R.string.widget_recording_detail, active.elapsed, active.soFar), c.onSurfaceVariant, 13.sp)
            }
        }
        Spacer(GlanceModifier.width(12.dp))
        if (active == null) {
            StartPill(WidgetNames.RECORDING, GlanceModifier)
        } else {
            val stop = context.getString(R.string.widget_stop_trip)
            Box(
                GlanceModifier.size(56.dp).cornerRadius(28.dp).background(c.error).clickable(actionRunCallback<StopTripAction>())
                    .semantics { contentDescription = stop },
                contentAlignment = Alignment.Center,
            ) {
                Icon(MpdIcons.Square, c.onError, 20.dp)
            }
        }
    }
}
