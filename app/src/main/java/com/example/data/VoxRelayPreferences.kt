package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CallTone
import com.example.model.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.voxRelayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "voxrelay_voice_prefs"
)

class VoxRelayPreferences(private val context: Context) {

    private object Keys {
        val CALLER_NAME = stringPreferencesKey("caller_name")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val VOICE_ID = stringPreferencesKey("selected_voice_id")
        val DEFAULT_TONE = stringPreferencesKey("default_tone")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_replies")
        val AUTO_SPEAKERPHONE = booleanPreferencesKey("auto_speakerphone")
        val DIRECT_CALL_MODE = booleanPreferencesKey("direct_call_mode")
    }

    val voiceSettingsFlow: Flow<VoiceSettings> = context.voxRelayDataStore.data.map { prefs ->
        VoiceSettings(
            callerName = prefs[Keys.CALLER_NAME] ?: "",
            speechRate = (prefs[Keys.SPEECH_RATE] ?: 1.0f).coerceIn(0.65f, 1.5f),
            speechPitch = (prefs[Keys.SPEECH_PITCH] ?: 1.0f).coerceIn(0.7f, 1.4f),
            selectedVoiceId = prefs[Keys.VOICE_ID] ?: "",
            defaultTone = CallTone.fromRaw(prefs[Keys.DEFAULT_TONE]),
            autoSpeakReplies = prefs[Keys.AUTO_SPEAK] ?: true,
            autoSpeakerphone = prefs[Keys.AUTO_SPEAKERPHONE] ?: true,
            directCallMode = prefs[Keys.DIRECT_CALL_MODE] ?: false
        )
    }

    suspend fun updateCallerName(name: String) {
        context.voxRelayDataStore.edit { it[Keys.CALLER_NAME] = name.trim() }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.voxRelayDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.65f, 1.5f) }
    }

    suspend fun updateSpeechPitch(pitch: Float) {
        context.voxRelayDataStore.edit { it[Keys.SPEECH_PITCH] = pitch.coerceIn(0.7f, 1.4f) }
    }

    suspend fun updateSelectedVoice(voiceId: String) {
        context.voxRelayDataStore.edit { it[Keys.VOICE_ID] = voiceId }
    }

    suspend fun updateDefaultTone(tone: CallTone) {
        context.voxRelayDataStore.edit { it[Keys.DEFAULT_TONE] = tone.name }
    }

    suspend fun updateAutoSpeakReplies(enabled: Boolean) {
        context.voxRelayDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun updateAutoSpeakerphone(enabled: Boolean) {
        context.voxRelayDataStore.edit { it[Keys.AUTO_SPEAKERPHONE] = enabled }
    }

    suspend fun updateDirectCallMode(enabled: Boolean) {
        context.voxRelayDataStore.edit { it[Keys.DIRECT_CALL_MODE] = enabled }
    }
}
