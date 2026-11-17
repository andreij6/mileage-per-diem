package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.Grabber
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterruptedTripSheet(state: InterruptedTripUiState, onResume: () -> Unit, onFinish: () -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    val trip = state.trip ?: return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MpdTheme.colors.surface,
        dragHandle = { Grabber(Modifier.padding(top = 12.dp)) },
    ) {
        InterruptedTripContent(trip, state.busy, onResume, onFinish, onDiscard)
    }
}

@Composable
fun InterruptedTripContent(trip: InterruptedTripUi, busy: Boolean, onResume: () -> Unit, onFinish: () -> Unit, onDiscard: () -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconTile(MpdIcons.TriangleAlert, c.accentC, c.accent, size = 48.dp, iconSize = 24.dp, radius = 14.dp)
        Text(stringResource(R.string.interrupted_title), Modifier.semantics { heading() }, style = MpdTheme.type.headlineS, color = c.text)
        Text(stringResource(R.string.interrupted_body, trip.stoppedAt, trip.distance), style = MpdTheme.type.bodyL, color = c.text2)
        MpdButton(stringResource(R.string.interrupted_resume), onResume, Modifier.fillMaxWidth(), icon = MpdIcons.Play, enabled = !busy)
        MpdButton(stringResource(R.string.interrupted_finish, trip.distance), onFinish, Modifier.fillMaxWidth(), kind = ButtonKind.Outlined, enabled = !busy)
        MpdButton(
            stringResource(R.string.interrupted_discard),
            onDiscard,
            Modifier.fillMaxWidth(),
            kind = ButtonKind.Text,
            height = 48.dp,
            enabled = !busy,
            content = c.danger,
        )
    }
}

private val previewTrip = InterruptedTripUi(tripId = 185, stoppedAt = "9:27 AM", distance = "8.2 mi")

@Preview(widthDp = 412)
@Composable
private fun InterruptedLightPreview() = MpdTheme(darkTheme = false) {
    Column(Modifier.background(MpdTheme.colors.surface)) {
        Grabber(Modifier.padding(top = 12.dp))
        InterruptedTripContent(previewTrip, false, {}, {}, {})
    }
}

@Preview(widthDp = 412)
@Composable
private fun InterruptedDarkPreview() = MpdTheme(darkTheme = true) {
    Column(Modifier.background(MpdTheme.colors.surface)) {
        Grabber(Modifier.padding(top = 12.dp))
        InterruptedTripContent(previewTrip, false, {}, {}, {})
    }
}
