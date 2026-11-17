package com.aijones.mileage.feature.onboarding.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PreciseLocationOffViewModel @Inject constructor(analytics: Analytics) : ViewModel() {
    init {
        analytics.track(AnalyticsEvent.ScreenView("precise_location_off"))
    }
}

@Composable
fun PreciseLocationOffRoute(onLogByHand: () -> Unit, @Suppress("UNUSED_PARAMETER") viewModel: PreciseLocationOffViewModel = hiltViewModel()) {
    val context = LocalContext.current
    PreciseLocationOffScreen(
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        onLogByHand = onLogByHand,
    )
}

@Composable
fun PreciseLocationOffScreen(onOpenSettings: () -> Unit, onLogByHand: () -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    OnboardingPage(modifier) {
        Spacer(Modifier.weight(1f))
        IconTile(MpdIcons.MapPin, c.accentC, c.accent, size = 88.dp, iconSize = 40.dp, radius = 44.dp)
        Text(stringResource(R.string.precise_title), Modifier.semantics { heading() }, style = t.headlineM, color = c.text)
        Text(stringResource(R.string.precise_body), style = t.bodyL, color = c.text2)
        MpdCard(spacing = 10.dp) {
            listOf(R.string.precise_step_1, R.string.precise_step_2, R.string.precise_step_3).forEachIndexed { i, step ->
                Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(c.primaryC), contentAlignment = Alignment.Center) {
                        Text((i + 1).toString(), style = t.label.copy(fontWeight = FontWeight.Bold), color = c.onPrimaryC)
                    }
                    Text(stringResource(step), style = t.bodyM, color = c.text)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        MpdButton(stringResource(R.string.precise_open_settings), onOpenSettings, Modifier.fillMaxWidth(), icon = MpdIcons.ExternalLink)
        MpdButton(stringResource(R.string.precise_log_by_hand), onLogByHand, Modifier.fillMaxWidth(), kind = ButtonKind.Text, height = 48.dp)
    }
}

@Preview(name = "Precise off light", widthDp = 412, heightDp = 879)
@Composable
private fun PreciseOffLightPreview() = PreviewSurface(false) { PreciseLocationOffScreen({}, {}) }

@Preview(name = "Precise off dark", widthDp = 412, heightDp = 879)
@Composable
private fun PreciseOffDarkPreview() = PreviewSurface(true) { PreciseLocationOffScreen({}, {}) }
