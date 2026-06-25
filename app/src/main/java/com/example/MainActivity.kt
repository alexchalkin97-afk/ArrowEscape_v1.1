package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameViewModel
import com.example.ui.GameScreen
import com.example.ui.theme.MyApplicationTheme
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.common.InitializationListener
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Yandex AppMetrika SDK
    val appMetricaConfig = AppMetricaConfig.newConfigBuilder("95084953-8df0-475f-bd14-18bca5aabab1").build()
    AppMetrica.activate(applicationContext, appMetricaConfig)
    
    // Initialize Yandex Mobile Ads SDK
    MobileAds.initialize(this, object : InitializationListener {
      override fun onInitializationCompleted() {
        android.util.Log.d("YandexAds", "SDK initialized successfully")
      }
    })

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: GameViewModel = viewModel()
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          GameScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}
