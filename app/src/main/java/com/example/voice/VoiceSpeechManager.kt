package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.VoiceOption
import com.example.model.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val VOICE_TAG = "VoxSpeechManager"

class VoiceSpeechManager(private val appContext: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentSettings: VoiceSettings = VoiceSettings()
    private var pendingUtterance: String? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpokenText = MutableStateFlow("")
    val currentlySpokenText: StateFlow<String> = _currentlySpokenText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _isSpeakerphoneOn = MutableStateFlow(false)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private val _ttsError = MutableStateFlow<String?>(null)
    val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (error: Exception) {
            Log.e(VOICE_TAG, "Failed to instantiate TextToSpeech", error)
            _ttsError.value = "Text-to-speech is unavailable on this device."
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val defaultLocale = Locale.getDefault()
            val langStatus = engine.setLanguage(defaultLocale)
            if (langStatus == TextToSpeech.LANG_MISSING_DATA || langStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentlySpokenText.value = ""
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentlySpokenText.value = ""
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _currentlySpokenText.value = ""
                    Log.e(VOICE_TAG, "TTS utterance error code=$errorCode")
                }
            })

            loadAvailableVoices(engine)
            applyVoiceSettings(currentSettings)
            _isTtsReady.value = true
            _ttsError.value = null

            pendingUtterance?.let { queued ->
                pendingUtterance = null
                speak(queued)
            }
        } else {
            Log.e(VOICE_TAG, "TextToSpeech init failed with status=$status")
            _isTtsReady.value = false
            _ttsError.value = "Unable to initialize voice engine."
        }
    }

    private fun loadAvailableVoices(engine: TextToSpeech) {
        try {
            val deviceLang = Locale.getDefault().language
            val voices = engine.voices
                ?.filter { voice ->
                    !voice.isNetworkConnectionRequired &&
                        (voice.locale.language == deviceLang || voice.locale.language == Locale.ENGLISH.language)
                }
                ?.sortedWith(compareBy<android.speech.tts.Voice> { it.locale.displayName }.thenBy { it.name })
                ?.take(12)
                ?.mapIndexed { index, voice ->
                    val region = voice.locale.displayCountry.ifBlank { voice.locale.displayLanguage }
                    VoiceOption(
                        id = voice.name,
                        displayName = "Voice ${index + 1} ($region)",
                        localeTag = voice.locale.toLanguageTag()
                    )
                }
                .orEmpty()
            _availableVoices.value = voices
        } catch (error: Exception) {
            Log.w(VOICE_TAG, "Could not enumerate TTS voices", error)
        }
    }

    fun applyVoiceSettings(settings: VoiceSettings) {
        currentSettings = settings
        val engine = tts ?: return
        engine.setSpeechRate(settings.speechRate.coerceIn(0.65f, 1.5f))
        engine.setPitch(settings.speechPitch.coerceIn(0.7f, 1.4f))
        if (settings.selectedVoiceId.isNotBlank()) {
            val matched = engine.voices?.firstOrNull { it.name == settings.selectedVoiceId }
            if (matched != null) {
                engine.voice = matched
            }
        }
    }

    fun speak(text: String, flushQueue: Boolean = true): Boolean {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return false
        val engine = tts
        if (engine == null || !_isTtsReady.value) {
            pendingUtterance = cleaned
            return false
        }
        stopListening()
        _currentlySpokenText.value = cleaned
        val mode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()
        val result = engine.speak(cleaned, mode, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _isSpeaking.value = false
            _currentlySpokenText.value = ""
            return false
        }
        return true
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        _currentlySpokenText.value = ""
    }

    fun setSpeakerphone(enabled: Boolean) {
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enabled) {
                    val speaker = manager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        manager.setCommunicationDevice(speaker)
                    }
                } else {
                    manager.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                manager.isSpeakerphoneOn = enabled
            }
            _isSpeakerphoneOn.value = enabled
        } catch (error: Exception) {
            Log.w(VOICE_TAG, "Speakerphone toggle failed", error)
        }
    }

    fun startListening(
        onFinalResult: (String) -> Unit,
        onErrorMessage: (String) -> Unit
    ) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                onErrorMessage("Speech recognition is not installed on this device. You can type directly instead.")
                return@post
            }

            stopSpeaking()
            stopListeningInternal()

            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _audioLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    _partialTranscript.value = ""
                    val readable = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that clearly. Tap mic to try again or type."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try speaking closer to the mic."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required to listen."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network error. You can type instead."
                        else -> "Microphone listener stopped. You can retry or type directly."
                    }
                    onErrorMessage(readable)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    if (best.isNotEmpty()) {
                        onFinalResult(best)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                recognizer.startListening(intent)
                _isListening.value = true
            } catch (error: Exception) {
                Log.e(VOICE_TAG, "Failed to start SpeechRecognizer", error)
                _isListening.value = false
                onErrorMessage("Could not start voice listener on this device.")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _audioLevel.value = 0f
            _partialTranscript.value = ""
        }
    }

    fun release() {
        stopListening()
        stopSpeaking()
        if (_isSpeakerphoneOn.value) {
            setSpeakerphone(false)
        }
        tts?.shutdown()
        tts = null
    }
}
