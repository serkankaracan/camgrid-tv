package io.github.serkankaracan.camgridtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import io.github.serkankaracan.camgridtv.ui.CamGridApp
import io.github.serkankaracan.camgridtv.ui.theme.CamGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CamGridTheme { CamGridApp() } }
    }
}
