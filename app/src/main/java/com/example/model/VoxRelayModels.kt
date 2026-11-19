package com.example.model

import java.util.UUID

enum class VoxTab(val route: String) {
    DISPATCH("dispatch"),
    DIALER("dialer"),
    STUDIO("studio"),
    LOGS_VOICE("logs_voice")
}

enum class ChatbotRole(
    val displayLabel: String,
    val systemInstruction: String
) {
    CALL_CONCIERGE(
        displayLabel = "Concierge",
        systemInstruction = "You are VoxRelay's Executive Call Concierge. You hold natural multi-turn conversations with the user, understand who they need to call and why, clarify missing details concisely, and craft warm, articulate spoken phone scripts."
    ),
    NEGOTIATOR(
        displayLabel = "Advocate",
        systemInstruction = "You are VoxRelay's Consumer Advocate & Bill Negotiator. You help the user challenge unfair fees, request refunds, and resolve company disputes calmly, firmly, and persuasively over the phone."
    ),
    BOOKING_AGENT(
        displayLabel = "Bookings",
        systemInstruction = "You are VoxRelay's Appointment & Reservation Agent. You efficiently handle restaurant bookings, flight changes, clinic rescheduling, and service appointments with clear dates, party sizes, and backup windows."
    )
}

sealed interface AudioTranscriptionState {
    data object Idle : AudioTranscriptionState
    data class Recording(val elapsedSeconds: Long, val amplitude: Float) : AudioTranscriptionState
    data object Transcribing : AudioTranscriptionState
    data class Ready(val transcript: String) : AudioTranscriptionState
    data class Error(val message: String) : AudioTranscriptionState
}

enum class CallTone(val label: String) {
    POLITE("Polite"),
    PROFESSIONAL("Professional"),
    CONCISE("Concise"),
    FIRM("Firm");

    companion object {
        fun fromRaw(raw: String?): CallTone {
            val normalized = raw?.trim()?.lowercase() ?: return PROFESSIONAL
            return entries.firstOrNull { it.name.lowercase() == normalized } ?: PROFESSIONAL
        }
    }
}

data class CallPlan(
    val id: String = UUID.randomUUID().toString(),
    val companyName: String,
    val phoneNumber: String,
    val category: String,
    val objective: String,
    val tone: CallTone,
    val openingScript: String,
    val talkingPoints: List<String>,
    val anticipatedQuestions: List<String>,
    val needsPhoneNumber: Boolean
)

data class ChatEntry(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val callPlan: CallPlan? = null,
    val isError: Boolean = false
)

enum class SpeakerRole {
    AI_VOICE,
    COMPANY_REP,
    SYSTEM
}

data class TranscriptLine(
    val id: String = UUID.randomUUID().toString(),
    val role: SpeakerRole,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class LiveCallSession(
    val id: String = UUID.randomUUID().toString(),
    val plan: CallPlan,
    val startedAtMs: Long = System.currentTimeMillis(),
    val elapsedSeconds: Long = 0L,
    val stageLabel: String = "Opening",
    val goalProgress: String = "Ready to introduce your request",
    val suggestedFollowUps: List<String> = plan.talkingPoints,
    val transcript: List<TranscriptLine> = emptyList(),
    val dialerLaunched: Boolean = false
)

data class CompletedCallRecord(
    val id: String = UUID.randomUUID().toString(),
    val companyName: String,
    val phoneNumber: String,
    val objective: String,
    val startedAtMs: Long,
    val durationSeconds: Long,
    val summary: String,
    val nextSteps: List<String>,
    val transcript: List<TranscriptLine>
)

data class VoiceOption(
    val id: String,
    val displayName: String,
    val localeTag: String
)

data class VoiceSettings(
    val callerName: String = "",
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val selectedVoiceId: String = "",
    val defaultTone: CallTone = CallTone.PROFESSIONAL,
    val autoSpeakReplies: Boolean = true,
    val autoSpeakerphone: Boolean = true,
    val directCallMode: Boolean = false
)

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data class Busy(val label: String) : AiTaskState
    data class Failed(val message: String) : AiTaskState
}

data class DispatchPromptSample(
    val label: String,
    val companyTag: String,
    val prompt: String
)

val DefaultDispatchSamples = listOf(
    DispatchPromptSample(
        label = "Reschedule flight",
        companyTag = "Airline",
        prompt = "Call Horizon Air at 1-800-555-0192 and ask to move my Friday evening flight to Saturday morning."
    ),
    DispatchPromptSample(
        label = "Book dinner table",
        companyTag = "Restaurant",
        prompt = "Call Osteria Verde at 555-0144 and reserve a table for 4 people this Saturday at 7:30 PM."
    ),
    DispatchPromptSample(
        label = "Question bill charge",
        companyTag = "Billing",
        prompt = "Call Metro Fiber Support at 1-800-555-0178 and ask why my monthly internet bill went up by $25."
    ),
    DispatchPromptSample(
        label = "Move dental visit",
        companyTag = "Clinic",
        prompt = "Call Cedar Dental at 555-0136 and reschedule my Tuesday 2 PM cleaning to Thursday afternoon."
    )
)
