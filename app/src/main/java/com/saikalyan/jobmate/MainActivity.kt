package com.saikalyan.jobmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saikalyan.jobmate.ui.navigation.AppNavHost
import com.saikalyan.jobmate.ui.theme.JobMateTheme
import com.saikalyan.jobmate.data.storage.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)
    private var darkTheme by mutableStateOf(false)
    @Inject lateinit var appPreferences: AppPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText = intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        lifecycleScope.launch { appPreferences.darkTheme.collect { darkTheme = it } }
        enableEdgeToEdge()
        setContent {
            JobMateTheme(darkTheme = darkTheme) {
                AppNavHost(sharedText = sharedText,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
    }
}
