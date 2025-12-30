package com.saip.lumio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SettingsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    // Manual Trigger State
    private var tapCount = 0
    private var lastTapTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tts = TextToSpeech(this, this)
        val prefs = getSharedPreferences("LumioPrefs", Context.MODE_PRIVATE)

        // --- BIND VIEWS ---
        val tvSpeedLabel = findViewById<TextView>(R.id.tvSpeedLabel)
        val seekSpeed = findViewById<SeekBar>(R.id.seekSpeed)

        val swText = findViewById<Switch>(R.id.swText)
        val swScene = findViewById<Switch>(R.id.swScene)
        val swObject = findViewById<Switch>(R.id.swObject)
        val swPerson = findViewById<Switch>(R.id.swPerson)
        val swTouchRead = findViewById<Switch>(R.id.swTouchRead)

        val btnClearFaces = findViewById<Button>(R.id.btnClearFaces)
        val viewManualTrigger = findViewById<View>(R.id.viewManualTrigger)

        // --- 1. SLIDER LOGIC ---
        // Range: 0.5x to 3.0x mapped to 0-100 progress
        val currentRate = prefs.getFloat("speech_rate", 1.0f)
        val progress = ((currentRate - 0.5f) / 2.5f * 100).toInt()
        seekSpeed.progress = progress
        tvSpeedLabel.text = String.format("Voice Speed: %.1fx", currentRate)

        seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newRate = 0.5f + (progress / 100f) * 2.5f
                tvSpeedLabel.text = String.format("Voice Speed: %.1fx", newRate)
                prefs.edit().putFloat("speech_rate", newRate).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- 2. SWITCHES LOGIC ---
        // Load saved state
        swText.isChecked = prefs.getBoolean("feat_text", true)
        swScene.isChecked = prefs.getBoolean("feat_scene", true)
        swObject.isChecked = prefs.getBoolean("feat_object", true)
        swPerson.isChecked = prefs.getBoolean("feat_person", true)
        swTouchRead.isChecked = prefs.getBoolean("feat_touch_read", true)

        // Save on change
        swText.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("feat_text", v).apply() }
        swScene.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("feat_scene", v).apply() }
        swObject.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("feat_object", v).apply() }
        swPerson.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("feat_person", v).apply() }
        swTouchRead.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("feat_touch_read", v).apply() }

        // --- 3. CLEAR DATABASE ---
        btnClearFaces.setOnClickListener {
            getSharedPreferences("LumioFaces", Context.MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "Faces Cleared", Toast.LENGTH_SHORT).show()
        }

        // --- 4. MANUAL TRIGGER (Triple Tap) ---
        viewManualTrigger.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < 400) {
                tapCount++
                if (tapCount == 3) {
                    readAppManual()
                    tapCount = 0
                }
            } else {
                tapCount = 1
            }
            lastTapTime = now
        }
    }

    private fun readAppManual() {
        val manual = "Lumio User Guide. " +
                "One Finger Double Tap reads text. " +
                "One Finger Triple Tap describes the scene. " +
                "Two Finger Swipe identifies objects. " +
                "Two Finger Tap identifies people. " +
                "Long Press enables Touch Reader mode. " +
                "Use the slider at the top to change voice speed."
        tts?.speak(manual, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}