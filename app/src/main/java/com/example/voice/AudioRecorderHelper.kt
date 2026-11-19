package com.example.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val RECORDER_TAG = "VoxAudioRecorder"
private const val MAX_INLINE_AUDIO_BYTES = 18 * 1024 * 1024 // Stay safely below 20MB inline limit

data class RecordedAudioClip(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMs: Long
)

class AudioRecorderHelper(private val appContext: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitudeLevel = MutableStateFlow(0f)
    val amplitudeLevel: StateFlow<Float> = _amplitudeLevel.asStateFlow()

    fun startRecording(): Result<Unit> {
        stopAndDiscard()
        return try {
            val file = File(appContext.cacheDir, "vox_recording_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(16_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            outputFile = file
            startedAtMs = System.currentTimeMillis()
            _isRecording.value = true
            _amplitudeLevel.value = 0f
            Result.success(Unit)
        } catch (error: Exception) {
            Log.e(RECORDER_TAG, "Failed to start microphone recording", error)
            stopAndDiscard()
            Result.failure(error)
        }
    }

    fun pollAmplitude(): Float {
        val current = recorder ?: return 0f
        if (!_isRecording.value) return 0f
        return try {
            val maxAmp = current.maxAmplitude
            val normalized = (maxAmp / 18_000f).coerceIn(0f, 1f)
            _amplitudeLevel.value = normalized
            normalized
        } catch (_: Exception) {
            0f
        }
    }

    fun stopAndReadClip(): Result<RecordedAudioClip> {
        val activeRecorder = recorder
        val file = outputFile
        val durationMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)

        recorder = null
        outputFile = null
        _isRecording.value = false
        _amplitudeLevel.value = 0f

        if (activeRecorder == null || file == null) {
            return Result.failure(IllegalStateException("No active audio recording."))
        }

        return try {
            try {
                activeRecorder.stop()
            } catch (stopError: RuntimeException) {
                Log.w(RECORDER_TAG, "Recording stopped too early or had no audio data", stopError)
                file.delete()
                return Result.failure(IllegalStateException("Recording was too short. Hold and speak for at least a second."))
            } finally {
                activeRecorder.release()
            }

            if (!file.exists() || file.length() == 0L) {
                file.delete()
                return Result.failure(IllegalStateException("Captured audio clip was empty."))
            }

            if (file.length() > MAX_INLINE_AUDIO_BYTES) {
                file.delete()
                return Result.failure(IllegalStateException("Audio clip is too large (over 18MB). Please record a shorter clip."))
            }

            val bytes = file.readBytes()
            file.delete()
            Result.success(
                RecordedAudioClip(
                    bytes = bytes,
                    mimeType = "audio/mp4",
                    durationMs = durationMs
                )
            )
        } catch (error: Exception) {
            Log.e(RECORDER_TAG, "Failed to finalize audio clip", error)
            file.delete()
            Result.failure(error)
        }
    }

    fun stopAndDiscard() {
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                }
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            outputFile?.delete()
            outputFile = null
            _isRecording.value = false
            _amplitudeLevel.value = 0f
        }
    }
}
