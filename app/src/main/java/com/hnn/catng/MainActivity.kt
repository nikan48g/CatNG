package com.hnn.catng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.hnn.catng.ui.main.MainScreen
import com.hnn.catng.ui.theme.CatNGTheme
import com.hnn.catng.ui.viewmodel.MainViewModel
import com.hnn.catng.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CatNGTheme {
                val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

                if (isFirstLaunch) {
                    WelcomeScreen(
                        onGetStarted = {
                            viewModel.completeFirstLaunch()
                        }
                    )
                } else {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
