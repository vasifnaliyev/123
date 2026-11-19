package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.VoxRelayAiRepository
import com.example.data.VoxRelayPreferences
import com.example.model.AiTaskState
import com.example.model.AudioTranscriptionState
import com.example.model.CallPlan
import com.example.model.CallTone
import com.example.model.ChatEntry
import com.example.model.ChatbotRole
import com.example.model.CompletedCallRecord
import com.example.model.LiveCallSession
import com.example.model.SpeakerRole
import com.example.model.TranscriptLine
import com.example.model.VoiceOption
import com.example.model.VoiceSettings
import com.example.model.VoxTab
import com.example.voice.AudioRecorderHelper
import com.example.voice.DialLaunchOutcome
import com.example.voice.PhoneCallLauncher
import com.example.voice.VoiceSpeechManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VM_TAG = "VoxRelayViewModel"

enum class ListeningTarget {
    CHAT_COMPOSER,
    LIVE_REP
}

data class VoxRelayUiState(
    val selectedTab: VoxTab = VoxTab.DISPATCH,
    val chatbotRole: ChatbotRole = ChatbotRole.CALL_CONCIERGE,
    val chatMessages: List<ChatEntry> = emptyList(),
    val chatDraft: String = "",
    val dialerPhoneDraft: String = "",
    val dialerCompanyDraft: String = "",
    val aiState: AiTaskState = AiTaskState.Idle,
    val transcriptionState: AudioTranscriptionState = AudioTranscriptionState.Idle,
    val transcriptionTarget: ListeningTarget? = null,
    val activeCall: LiveCallSession? = null,
    val callHistory: List<CompletedCallRecord> = emptyList(),
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val isTtsReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val currentlySpokenText: String = "",
    val isListening: Boolean = false,
    val listeningTarget: ListeningTarget? = null,
    val partialTranscript: String = "",
    val audioLevel: Float = 0f,
    val isSpeakerphoneOn: Boolean = false,
    val availableVoices: List<VoiceOption> = emptyList(),
    val snackbarMessage: String? = null
)

private data class CoreSessionState(
    val selectedTab: VoxTab = VoxTab.DISPATCH,
    val chatbotRole: ChatbotRole = ChatbotRole.CALL_CONCIERGE,
    val chatMessages: List<ChatEntry> = emptyList(),
    val chatDraft: String = "",
    val dialerPhoneDraft: String = "",
    val dialerCompanyDraft: String = "",
    val aiState: AiTaskState = AiTaskState.Idle,
    val transcriptionState: AudioTranscriptionState = AudioTranscriptionState.Idle,
    val transcriptionTarget: ListeningTarget? = null,
    val activeCall: LiveCallSession? = null,
    val callHistory: List<CompletedCallRecord> = emptyList(),
    val listeningTarget: ListeningTarget? = null,
    val snackbarMessage: String? = null
)

private data class VoiceTelemetry(
    val isTtsReady: Boolean,
    val isSpeaking: Boolean,
    val currentlySpokenText: String,
    val isListening: Boolean,
    val partialTranscript: String,
    val audioLevel: Float,
    val isSpeakerphoneOn: Boolean,
    val availableVoices: List<VoiceOption>
)

class VoxRelayViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository = VoxRelayAiRepository()
    private val preferences = VoxRelayPreferences(application.applicationContext)
    private val speechManager = VoiceSpeechManager(application.applicationContext)
    private val audioRecorder = AudioRecorderHelper(application.applicationContext)

    private val coreState = MutableStateFlow(CoreSessionState())
    private var latestSettings = VoiceSettings()
    private var callTimerJob: Job? = null
    private var recordingMeterJob: Job? = null

    private val voiceTelemetryFlow = combine(
        combine(
            speechManager.isTtsReady,
            speechManager.isSpeaking,
            speechManager.currentlySpokenText,
            speechManager.isListening
        ) { ready, speaking, spokenText, listening ->
            listOf(ready, speaking, spokenText, listening)
        },
        combine(
            speechManager.partialTranscript,
            speechManager.audioLevel,
            speechManager.isSpeakerphoneOn,
            speechManager.availableVoices
        ) { partial, level, speaker, voices ->
            listOf(partial, level, speaker, voices)
        }
    ) { first, second ->
        @Suppress("UNCHECKED_CAST")
        VoiceTelemetry(
            isTtsReady = first[0] as Boolean,
            isSpeaking = first[1] as Boolean,
            currentlySpokenText = first[2] as String,
            isListening = first[3] as Boolean,
            partialTranscript = second[0] as String,
            audioLevel = second[1] as Float,
            isSpeakerphoneOn = second[2] as Boolean,
            availableVoices = second[3] as List<VoiceOption>
        )
    }

    val uiState: StateFlow<VoxRelayUiState> = combine(
        coreState,
        preferences.voiceSettingsFlow,
        voiceTelemetryFlow
    ) { core, settings, voice ->
        val recordingAmp = (core.transcriptionState as? AudioTranscriptionState.Recording)?.amplitude ?: 0f
        VoxRelayUiState(
            selectedTab = core.selectedTab,
            chatbotRole = core.chatbotRole,
            chatMessages = core.chatMessages,
            chatDraft = core.chatDraft,
            dialerPhoneDraft = core.dialerPhoneDraft,
            dialerCompanyDraft = core.dialerCompanyDraft,
            aiState = core.aiState,
            transcriptionState = core.transcriptionState,
            transcriptionTarget = core.transcriptionTarget,
            activeCall = core.activeCall,
            callHistory = core.callHistory,
            voiceSettings = settings,
            isTtsReady = voice.isTtsReady,
            isSpeaking = voice.isSpeaking,
            currentlySpokenText = voice.currentlySpokenText,
            isListening = voice.isListening || core.transcriptionState is AudioTranscriptionState.Recording,
            listeningTarget = if (core.transcriptionState is AudioTranscriptionState.Recording) {
                core.transcriptionTarget
            } else if (voice.isListening) {
                core.listeningTarget
            } else {
                null
            },
            partialTranscript = voice.partialTranscript,
            audioLevel = maxOf(voice.audioLevel, recordingAmp),
            isSpeakerphoneOn = voice.isSpeakerphoneOn,
            availableVoices = voice.availableVoices,
            snackbarMessage = core.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VoxRelayUiState()
    )

    init {
        viewModelScope.launch {
            preferences.voiceSettingsFlow.collect { settings ->
                latestSettings = settings
                speechManager.applyVoiceSettings(settings)
            }
        }
        viewModelScope.launch {
            speechManager.ttsError.collect { errorMsg ->
                if (!errorMsg.isNullOrBlank()) {
                    postNotice(errorMsg)
                }
            }
        }
    }

    fun selectTab(tab: VoxTab) {
        coreState.update { it.copy(selectedTab = tab) }
    }

    fun selectChatbotRole(role: ChatbotRole) {
        if (coreState.value.chatbotRole == role) return
        aiRepository.resetChatSession()
        coreState.update { it.copy(chatbotRole = role) }
        postNotice("Switched Gemini assistant role to ${role.displayLabel}.")
    }

    fun updateChatDraft(draft: String) {
        coreState.update { it.copy(chatDraft = draft) }
    }

    fun updateDialerPhoneDraft(phone: String) {
        coreState.update { it.copy(dialerPhoneDraft = phone) }
    }

    fun openPlanInDialer(plan: CallPlan) {
        coreState.update {
            it.copy(
                dialerPhoneDraft = plan.phoneNumber,
                dialerCompanyDraft = plan.companyName,
                selectedTab = VoxTab.DIALER
            )
        }
    }

    fun consumeSnackbar() {
        coreState.update { it.copy(snackbarMessage = null) }
    }

    fun postNotice(message: String) {
        coreState.update { it.copy(snackbarMessage = message) }
    }

    fun clearChatSession() {
        speechManager.stopSpeaking()
        speechManager.stopListening()
        cancelAudioRecording()
        aiRepository.resetChatSession()
        coreState.update {
            it.copy(
                chatMessages = emptyList(),
                chatDraft = "",
                aiState = AiTaskState.Idle,
                transcriptionState = AudioTranscriptionState.Idle,
                transcriptionTarget = null
            )
        }
    }

    fun sendChatMessage(rawText: String = coreState.value.chatDraft) {
        val message = rawText.trim()
        if (message.isEmpty() || coreState.value.aiState is AiTaskState.Busy) return

        val userEntry = ChatEntry(isUser = true, text = message)
        val role = coreState.value.chatbotRole

        coreState.update {
            it.copy(
                chatMessages = it.chatMessages + userEntry,
                chatDraft = "",
                aiState = AiTaskState.Busy("Gemini is thinking…")
            )
        }

        viewModelScope.launch {
            try {
                val result = aiRepository.sendMultiTurnChatMessage(
                    userMessage = message,
                    role = role,
                    callerName = latestSettings.callerName,
                    preferredTone = latestSettings.defaultTone
                )

                val aiEntry = ChatEntry(
                    isUser = false,
                    text = result.assistantReply,
                    callPlan = result.callPlan
                )

                coreState.update {
                    it.copy(
                        chatMessages = it.chatMessages + aiEntry,
                        aiState = AiTaskState.Idle
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "Dispatch chat timed out", e)
                appendChatError("Request timed out after 60 seconds. Please check your connection and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Failed to send Gemini chat message", e)
                val readable = e.localizedMessage ?: "Unable to reach Gemini right now."
                appendChatError(readable)
            }
        }
    }

    private fun appendChatError(message: String) {
        val errEntry = ChatEntry(
            isUser = false,
            text = message,
            isError = true
        )
        coreState.update {
            it.copy(
                chatMessages = it.chatMessages + errEntry,
                aiState = AiTaskState.Failed(message)
            )
        }
    }

    fun toggleAudioRecordingForTranscription(target: ListeningTarget) {
        val currentState = coreState.value.transcriptionState
        if (currentState is AudioTranscriptionState.Recording) {
            stopRecordingAndTranscribe()
            return
        }
        if (currentState is AudioTranscriptionState.Transcribing) {
            return
        }
        startAudioRecording(target)
    }

    fun startAudioRecording(target: ListeningTarget) {
        speechManager.stopSpeaking()
        speechManager.stopListening()
        val result = audioRecorder.startRecording()
        if (result.isFailure) {
            val msg = result.exceptionOrNull()?.localizedMessage
                ?: "Could not access the microphone for audio recording."
            coreState.update {
                it.copy(
                    transcriptionState = AudioTranscriptionState.Error(msg),
                    transcriptionTarget = null
                )
            }
            postNotice(msg)
            return
        }

        coreState.update {
            it.copy(
                transcriptionState = AudioTranscriptionState.Recording(elapsedSeconds = 0L, amplitude = 0f),
                transcriptionTarget = target
            )
        }

        recordingMeterJob?.cancel()
        recordingMeterJob = viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            while (isActive && audioRecorder.isRecording.value) {
                val amp = audioRecorder.pollAmplitude()
                val secs = (System.currentTimeMillis() - startMs) / 1_000L
                coreState.update { state ->
                    if (state.transcriptionState is AudioTranscriptionState.Recording) {
                        state.copy(
                            transcriptionState = AudioTranscriptionState.Recording(
                                elapsedSeconds = secs,
                                amplitude = amp
                            )
                        )
                    } else {
                        state
                    }
                }
                delay(150L)
            }
        }
    }

    fun stopRecordingAndTranscribe() {
        recordingMeterJob?.cancel()
        recordingMeterJob = null
        val target = coreState.value.transcriptionTarget ?: ListeningTarget.CHAT_COMPOSER

        val clipResult = audioRecorder.stopAndReadClip()
        if (clipResult.isFailure) {
            val msg = clipResult.exceptionOrNull()?.localizedMessage ?: "Recording failed."
            coreState.update {
                it.copy(
                    transcriptionState = AudioTranscriptionState.Error(msg),
                    transcriptionTarget = null
                )
            }
            postNotice(msg)
            return
        }

        val clip = clipResult.getOrThrow()
        coreState.update {
            it.copy(transcriptionState = AudioTranscriptionState.Transcribing)
        }

        viewModelScope.launch {
            try {
                val transcript = withContext(Dispatchers.IO) {
                    aiRepository.transcribeAudio(
                        audioBytes = clip.bytes,
                        mimeType = clip.mimeType
                    )
                }

                coreState.update {
                    it.copy(
                        transcriptionState = AudioTranscriptionState.Ready(transcript),
                        transcriptionTarget = null
                    )
                }

                when (target) {
                    ListeningTarget.CHAT_COMPOSER -> {
                        val combined = listOf(coreState.value.chatDraft.trim(), transcript)
                            .filter { it.isNotEmpty() }
                            .joinToString(" ")
                        coreState.update { it.copy(chatDraft = combined) }
                        postNotice("Audio transcribed — review or tap Send.")
                    }

                    ListeningTarget.LIVE_REP -> {
                        submitRepresentativeStatement(transcript)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "Audio transcription timed out", e)
                val msg = "Audio transcription timed out after 60 seconds. Please try again."
                coreState.update {
                    it.copy(
                        transcriptionState = AudioTranscriptionState.Error(msg),
                        transcriptionTarget = null
                    )
                }
                postNotice(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Gemini audio transcription failed", e)
                val msg = e.localizedMessage ?: "Audio transcription failed."
                coreState.update {
                    it.copy(
                        transcriptionState = AudioTranscriptionState.Error(msg),
                        transcriptionTarget = null
                    )
                }
                postNotice(msg)
            }
        }
    }

    fun cancelAudioRecording() {
        recordingMeterJob?.cancel()
        recordingMeterJob = null
        audioRecorder.stopAndDiscard()
        coreState.update {
            it.copy(
                transcriptionState = AudioTranscriptionState.Idle,
                transcriptionTarget = null
            )
        }
    }

    fun updatePlanPhoneNumber(entryId: String, newPhone: String) {
        coreState.update { state ->
            val updated = state.chatMessages.map { entry ->
                if (entry.id == entryId && entry.callPlan != null) {
                    val cleaned = newPhone.trim()
                    entry.copy(
                        callPlan = entry.callPlan.copy(
                            phoneNumber = cleaned,
                            needsPhoneNumber = cleaned.isBlank()
                        )
                    )
                } else {
                    entry
                }
            }
            state.copy(chatMessages = updated)
        }
    }

    fun updatePlanOpeningScript(entryId: String, newScript: String) {
        coreState.update { state ->
            val updated = state.chatMessages.map { entry ->
                if (entry.id == entryId && entry.callPlan != null) {
                    entry.copy(
                        callPlan = entry.callPlan.copy(openingScript = newScript)
                    )
                } else {
                    entry
                }
            }
            state.copy(chatMessages = updated)
        }
    }

    fun previewOrStopVoice(text: String) {
        if (speechManager.isSpeaking.value) {
            speechManager.stopSpeaking()
        } else {
            val started = speechManager.speak(text)
            if (!started && !speechManager.isTtsReady.value) {
                postNotice("Initializing voice engine — speaking shortly…")
            }
        }
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    fun toggleSpeakerphone() {
        val next = !speechManager.isSpeakerphoneOn.value
        speechManager.setSpeakerphone(next)
    }

    fun launchCallAndSpeak(
        plan: CallPlan,
        activityContext: Context,
        launchPhoneDialer: Boolean = true,
        speakOpeningImmediately: Boolean = true
    ) {
        val sanitizedNumber = PhoneCallLauncher.sanitizePhoneNumber(plan.phoneNumber)
        if (launchPhoneDialer && sanitizedNumber.count { it.isDigit() } < 3) {
            postNotice("Please enter a valid phone number for ${plan.companyName} first.")
            return
        }

        if (latestSettings.autoSpeakerphone) {
            speechManager.setSpeakerphone(true)
        }

        val initialTranscript = mutableListOf<TranscriptLine>()
        var dialerLaunched = false

        if (launchPhoneDialer) {
            when (
                PhoneCallLauncher.placeOrDialCall(
                    context = activityContext,
                    rawPhoneNumber = sanitizedNumber,
                    preferDirectCall = latestSettings.directCallMode
                )
            ) {
                DialLaunchOutcome.DirectCallStarted -> {
                    dialerLaunched = true
                    initialTranscript.add(
                        TranscriptLine(
                            role = SpeakerRole.SYSTEM,
                            text = "Direct call initiated to ${plan.companyName} ($sanitizedNumber)."
                        )
                    )
                    postNotice("Calling ${plan.companyName}… Put call on speakerphone to relay voice.")
                }

                DialLaunchOutcome.DialerOpened -> {
                    dialerLaunched = true
                    initialTranscript.add(
                        TranscriptLine(
                            role = SpeakerRole.SYSTEM,
                            text = "Opened phone dialer for ${plan.companyName} ($sanitizedNumber)."
                        )
                    )
                    postNotice("Dialer ready for ${plan.companyName}. Tap Call, enable speaker, and return to speak.")
                }

                DialLaunchOutcome.NoDialerAvailable -> {
                    initialTranscript.add(
                        TranscriptLine(
                            role = SpeakerRole.SYSTEM,
                            text = "No phone dialer found on this device — running in live voice console."
                        )
                    )
                    postNotice("No dialer app found on this device — continuing in Call Studio.")
                }

                DialLaunchOutcome.InvalidNumber -> {
                    postNotice("Please enter a valid company phone number.")
                    return
                }
            }
        } else {
            initialTranscript.add(
                TranscriptLine(
                    role = SpeakerRole.SYSTEM,
                    text = "Live voice studio started for ${plan.companyName}."
                )
            )
        }

        val normalizedPlan = plan.copy(
            phoneNumber = sanitizedNumber.ifBlank { plan.phoneNumber },
            needsPhoneNumber = sanitizedNumber.isBlank()
        )

        val session = LiveCallSession(
            plan = normalizedPlan,
            transcript = initialTranscript,
            dialerLaunched = dialerLaunched
        )

        coreState.update {
            it.copy(
                activeCall = session,
                selectedTab = VoxTab.STUDIO
            )
        }
        startCallTimer()

        if (speakOpeningImmediately && normalizedPlan.openingScript.isNotBlank()) {
            speakLiveLine(normalizedPlan.openingScript)
        }
    }

    fun startQuickManualCall(
        companyName: String,
        phoneNumber: String,
        script: String,
        activityContext: Context
    ) {
        val cleanCompany = companyName.trim().ifBlank { "Company Support" }
        val cleanScript = script.trim().ifBlank {
            "Hello, I am calling $cleanCompany for assistance. Could you please help me today?"
        }
        val manualPlan = CallPlan(
            companyName = cleanCompany,
            phoneNumber = phoneNumber.trim(),
            category = "Direct Call",
            objective = cleanScript,
            tone = latestSettings.defaultTone,
            openingScript = cleanScript,
            talkingPoints = listOf(
                "Could you repeat that confirmation number slowly?",
                "Yes, please proceed with that option.",
                "Thank you so much for your help today."
            ),
            anticipatedQuestions = emptyList(),
            needsPhoneNumber = phoneNumber.isBlank()
        )
        launchCallAndSpeak(
            plan = manualPlan,
            activityContext = activityContext,
            launchPhoneDialer = phoneNumber.isNotBlank(),
            speakOpeningImmediately = true
        )
    }

    fun dialActiveCompany(activityContext: Context) {
        val session = coreState.value.activeCall ?: return
        val outcome = PhoneCallLauncher.placeOrDialCall(
            context = activityContext,
            rawPhoneNumber = session.plan.phoneNumber,
            preferDirectCall = latestSettings.directCallMode
        )
        when (outcome) {
            DialLaunchOutcome.DirectCallStarted -> postNotice("Calling ${session.plan.companyName}…")
            DialLaunchOutcome.DialerOpened -> postNotice("Opened dialer for ${session.plan.phoneNumber}")
            DialLaunchOutcome.InvalidNumber -> postNotice("No valid phone number set for this call.")
            DialLaunchOutcome.NoDialerAvailable -> postNotice("No phone dialer app available on this device.")
        }
    }

    fun speakLiveLine(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        speechManager.speak(cleaned)
        coreState.update { state ->
            val current = state.activeCall ?: return@update state
            val alreadyLatest = current.transcript.lastOrNull()?.let {
                it.role == SpeakerRole.AI_VOICE && it.text == cleaned
            } == true
            val updatedTranscript = if (alreadyLatest) {
                current.transcript
            } else {
                current.transcript + TranscriptLine(role = SpeakerRole.AI_VOICE, text = cleaned)
            }
            state.copy(activeCall = current.copy(transcript = updatedTranscript))
        }
    }

    fun submitRepresentativeStatement(rawStatement: String) {
        val statement = rawStatement.trim()
        val active = coreState.value.activeCall ?: return
        if (statement.isEmpty() || coreState.value.aiState is AiTaskState.Busy) return

        val updatedSession = active.copy(
            transcript = active.transcript + TranscriptLine(
                role = SpeakerRole.COMPANY_REP,
                text = statement
            )
        )

        coreState.update {
            it.copy(
                activeCall = updatedSession,
                aiState = AiTaskState.Busy("Drafting spoken reply…")
            )
        }

        viewModelScope.launch {
            try {
                val reply = aiRepository.draftLiveSpokenReply(
                    session = updatedSession,
                    representativeSaid = statement,
                    callerName = latestSettings.callerName
                )

                val aiVoiceLine = TranscriptLine(
                    role = SpeakerRole.AI_VOICE,
                    text = reply.spokenReply
                )

                coreState.update { state ->
                    val current = state.activeCall ?: return@update state.copy(aiState = AiTaskState.Idle)
                    state.copy(
                        activeCall = current.copy(
                            stageLabel = reply.stageLabel,
                            goalProgress = reply.goalProgress,
                            suggestedFollowUps = reply.suggestedFollowUps,
                            transcript = current.transcript + aiVoiceLine
                        ),
                        aiState = AiTaskState.Idle
                    )
                }

                if (latestSettings.autoSpeakReplies) {
                    speechManager.speak(reply.spokenReply)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "Live reply timed out", e)
                coreState.update { it.copy(aiState = AiTaskState.Failed("Reply timed out.")) }
                postNotice("Live reply timed out. Tap any quick line or try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Live reply generation failed", e)
                val msg = e.localizedMessage ?: "Unable to draft reply."
                coreState.update { it.copy(aiState = AiTaskState.Failed(msg)) }
                postNotice(msg)
            }
        }
    }

    fun endActiveCallAndSummarize() {
        val endingSession = coreState.value.activeCall ?: return
        stopCallTimer()
        cancelAudioRecording()
        speechManager.stopSpeaking()
        speechManager.stopListening()
        speechManager.setSpeakerphone(false)

        coreState.update {
            it.copy(
                activeCall = null,
                aiState = AiTaskState.Busy("Summarizing your call…"),
                selectedTab = VoxTab.LOGS_VOICE
            )
        }

        viewModelScope.launch {
            try {
                val summaryResult = aiRepository.summarizeCompletedCall(endingSession)
                val record = CompletedCallRecord(
                    companyName = endingSession.plan.companyName,
                    phoneNumber = endingSession.plan.phoneNumber,
                    objective = endingSession.plan.objective,
                    startedAtMs = endingSession.startedAtMs,
                    durationSeconds = endingSession.elapsedSeconds,
                    summary = summaryResult.summary,
                    nextSteps = summaryResult.nextSteps,
                    transcript = endingSession.transcript
                )
                coreState.update {
                    it.copy(
                        callHistory = listOf(record) + it.callHistory,
                        aiState = AiTaskState.Idle
                    )
                }
                postNotice("Call ended and summarized.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Call summary failed", e)
                val fallbackRecord = CompletedCallRecord(
                    companyName = endingSession.plan.companyName,
                    phoneNumber = endingSession.plan.phoneNumber,
                    objective = endingSession.plan.objective,
                    startedAtMs = endingSession.startedAtMs,
                    durationSeconds = endingSession.elapsedSeconds,
                    summary = "Call logged (${endingSession.transcript.size} transcript entries). Summary unavailable: ${e.localizedMessage ?: "network error"}.",
                    nextSteps = emptyList(),
                    transcript = endingSession.transcript
                )
                coreState.update {
                    it.copy(
                        callHistory = listOf(fallbackRecord) + it.callHistory,
                        aiState = AiTaskState.Idle
                    )
                }
            }
        }
    }

    fun reuseCallInChat(record: CompletedCallRecord) {
        val numberSegment = if (record.phoneNumber.isNotBlank()) " at ${record.phoneNumber}" else ""
        val followUpDraft = "Follow up with ${record.companyName}$numberSegment regarding: ${record.objective}"
        coreState.update {
            it.copy(
                chatDraft = followUpDraft,
                selectedTab = VoxTab.DISPATCH
            )
        }
    }

    fun updateCallerName(name: String) {
        viewModelScope.launch { preferences.updateCallerName(name) }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch { preferences.updateSpeechRate(rate) }
    }

    fun updateSpeechPitch(pitch: Float) {
        viewModelScope.launch { preferences.updateSpeechPitch(pitch) }
    }

    fun updateSelectedVoice(voiceId: String) {
        viewModelScope.launch { preferences.updateSelectedVoice(voiceId) }
    }

    fun updateDefaultTone(tone: CallTone) {
        viewModelScope.launch { preferences.updateDefaultTone(tone) }
    }

    fun updateAutoSpeakReplies(enabled: Boolean) {
        viewModelScope.launch { preferences.updateAutoSpeakReplies(enabled) }
    }

    fun updateAutoSpeakerphone(enabled: Boolean) {
        viewModelScope.launch { preferences.updateAutoSpeakerphone(enabled) }
    }

    fun updateDirectCallMode(enabled: Boolean) {
        viewModelScope.launch { preferences.updateDirectCallMode(enabled) }
    }

    private fun startCallTimer() {
        stopCallTimer()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                coreState.update { state ->
                    val current = state.activeCall ?: return@update state
                    state.copy(activeCall = current.copy(elapsedSeconds = current.elapsedSeconds + 1L))
                }
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    override fun onCleared() {
        stopCallTimer()
        recordingMeterJob?.cancel()
        audioRecorder.stopAndDiscard()
        speechManager.release()
        super.onCleared()
    }
}
