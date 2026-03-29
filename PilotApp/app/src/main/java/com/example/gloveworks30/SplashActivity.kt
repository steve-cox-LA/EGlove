package com.example.gloveworks30

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import kotlinx.coroutines.*

class SplashActivity : ComponentActivity() {
    private val splashScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.black)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashScope.launch {
            delay(2000)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        splashScope.cancel()
        super.onDestroy()
    }
}