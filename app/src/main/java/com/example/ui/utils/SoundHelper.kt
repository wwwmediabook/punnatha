package com.example.ui.utils

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.sin

object SoundHelper {
    fun playPageFlipSound() {
        thread {
            try {
                val sampleRate = 16000
                val durationMs = 200
                val numSamples = durationMs * sampleRate / 1000
                val generatedSnd = ByteArray(2 * numSamples)
                
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    // Create a soft sweeping low-pass noise frequency that mimics paper rustling
                    // Combined sweep frequency from 450Hz down to 150Hz
                    val frequency = 450.0 - (t * 300.0) 
                    val angle = 2.0 * Math.PI * frequency * t
                    val tone = sin(angle)
                    
                    // Add some noise to simulate the paper friction
                    val noise = (Math.random() * 2.0 - 1.0) * 0.12
                    
                    // Smooth envelope: fade-in fast, fade-out smoothly
                    val envelope = if (i < numSamples * 0.15) {
                        i / (numSamples * 0.15)
                    } else {
                        (numSamples - i) / (numSamples * 0.85)
                    }
                    
                    val sample = ((tone * 0.08 + noise) * envelope * 32767).toInt().coerceIn(-32768, 32767)
                    
                    // 16-bit PCM Mono
                    generatedSnd[2 * i] = (sample and 0x00FF).toByte()
                    generatedSnd[2 * i + 1] = ((sample and 0xFF00) ushr 8).toByte()
                }
                
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                
                // Let it play, then release
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
