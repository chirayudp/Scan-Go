package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Utility for providing audio feedback during retail operations.
 * Generates a clean, subtle retail POS supermarket scanner 'beep' when a barcode is identified.
 */
object SoundFeedback {
    private const val TAG = "SoundFeedback"
    
    @Volatile
    var isAudioEnabled: Boolean = true
    
    private var toneGenerator: ToneGenerator? = null

    init {
        initToneGenerator()
    }

    private fun initToneGenerator() {
        try {
            // STREAM_MUSIC at 70% volume gives a crisp, subtle retail supermarket scanner beep
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Throwable) {
            try {
                // Secondary attempt using STREAM_NOTIFICATION or STREAM_SYSTEM
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
            } catch (ex: Throwable) {
                Log.d(TAG, "ToneGenerator deferred or unavailable: ${ex.message}")
            }
        }
    }

    /**
     * Emits a subtle, crisp 'beep' audio feedback (120ms standard retail scanner beep)
     * when a barcode is successfully identified and scanned by CameraX.
     */
    fun playBarcodeScanBeep() {
        if (!isAudioEnabled) return
        try {
            if (toneGenerator == null) {
                initToneGenerator()
            }
            // TONE_PROP_BEEP is the standard proprietary supermarket scanner frequency (~1500Hz, 120ms)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Throwable) {
            try {
                val fallbackGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                fallbackGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (ex: Throwable) {
                Log.d(TAG, "Audio beep suppressed in headless or restricted environment: ${ex.message}")
            }
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Throwable) {
            // Ignore
        }
    }
}
