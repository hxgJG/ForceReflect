package com.example.forcereflect

import android.app.ActivityThread
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val thread = ActivityThread.currentActivityThread()

        Log.i("reflect", "thread = $thread")

        findViewById<TextView>(R.id.btn_test).setOnClickListener {
            try {
                TextToSpeech::class.java.getDeclaredField("mServiceConnection").apply {
                    isAccessible = true
                }
                it.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                (it as? TextView)?.setTextColor(resources.getColor(android.R.color.white))
            } catch (e: Throwable) {
                it.setBackgroundColor(resources.getColor(R.color.colorAccent))
                e.printStackTrace()
            }
        }
    }
}
