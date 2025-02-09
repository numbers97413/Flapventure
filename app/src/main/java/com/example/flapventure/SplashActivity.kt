package com.example.flapventure

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the splash screen layout (assume you have activity_splash.xml in res/layout)
        setContentView(R.layout.activity_splash)

        // Initialize MediaPlayer to play the splash.wav sound from the raw folder.
        mediaPlayer = MediaPlayer.create(this, R.raw.splash)
        mediaPlayer.start()

        // Delay for 3 seconds then launch MainActivity.
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer to free up resources.
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}
