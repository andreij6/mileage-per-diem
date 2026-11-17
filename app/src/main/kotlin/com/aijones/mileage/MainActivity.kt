package com.aijones.mileage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.demo.DemoSeeder
import com.aijones.mileage.feature.tracking.data.TrackingInitializer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.feature.settings.domain.ThemeMode
import com.aijones.mileage.startup.StartupTracker
import com.aijones.mileage.ui.AppUiState
import com.aijones.mileage.ui.AppViewModel
import com.aijones.mileage.ui.MileageAppUi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var demoSeeder: dagger.Lazy<DemoSeeder>
    @Inject lateinit var trackingInitializer: dagger.Lazy<TrackingInitializer>

    private val viewModel: AppViewModel by viewModels()
    private val intents = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as MileageApp
        StartupTracker(analytics).track(this, app.coldStartPending)
        app.coldStartPending = false
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { viewModel.state.value is AppUiState.Loading }
        intents.value = intent
        if (intent.getBooleanExtra("seed_demo", false) && resources.getBoolean(R.bool.allow_demo_seed)) {
            lifecycleScope.launch(Dispatchers.IO) { demoSeeder.get().seedIfEmpty() }
        }
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val ready = state as? AppUiState.Ready ?: return@setContent
            val dark = when (ready.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MpdTheme(darkTheme = dark) {
                MileageAppUi(ready.onboarded, intents, onIntentHandled = { intents.value = null })
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        window.decorView.post { trackingInitializer.get().start() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intents.value = intent
    }
}
