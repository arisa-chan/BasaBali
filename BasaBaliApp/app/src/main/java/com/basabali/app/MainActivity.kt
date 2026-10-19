package com.basabali.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.basabali.app.ui.AppRoot
import com.basabali.app.ui.theme.BasaBaliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BasaBaliTheme {
                AppRoot()
            }
        }
    }
}
