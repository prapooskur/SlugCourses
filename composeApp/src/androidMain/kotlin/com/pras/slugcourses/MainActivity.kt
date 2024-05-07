package com.pras.slugcourses

import App
import DriverFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val driverFactory = DriverFactory(applicationContext)
        setContent {
            App(driverFactory)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val driverFactory = DriverFactory(LocalContext.current)
    App(driverFactory)
}