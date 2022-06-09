package com.example.hearme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log


class SpeechToTextActivity : AppCompatActivity() {

    var CAT: String = "SPEECH_ACTIVITY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_to_text)

        supportActionBar?.title = "Speech Activity"
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.i(CAT, "going back")
        finish()
        return super.onSupportNavigateUp()
    }
}