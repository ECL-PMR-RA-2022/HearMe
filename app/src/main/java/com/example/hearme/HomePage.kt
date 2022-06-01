package com.example.hearme

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val hearButton : Button = findViewById(R.id.hearButton)
        val viewButton : Button = findViewById(R.id.viewButton)

        hearButton.setOnClickListener {
            Toast.makeText(this, "Start to hear!", Toast.LENGTH_SHORT).show()
            // TODO:

        }

        viewButton.setOnClickListener {
            Toast.makeText(this, "Start to see!", Toast.LENGTH_SHORT).show()
            // TODO:
            val versView = Intent(this, SignLanguageRecognitionActivity::class.java)
            startActivity(versView)
        }
    }


}