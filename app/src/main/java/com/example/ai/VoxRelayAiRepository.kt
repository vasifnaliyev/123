package com.example.ai

import com.example.model.CallPlan
import com.example.model.CallTone
import com.example.model.ChatbotRole
import com.example.model.LiveCallSession
import com.example.model.SpeakerRole
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class DispatchUnderstandingResult(
    val assistantReply: String,
    val callPlan: CallPlan?
)

data class LiveReplyResult(
    val spokenReply: String,
    val stageLabel: String,
    val goalProgress: String,
    val suggestedFollowUps: List<String>
)

data class CallSummaryResult(
    val summary: String,
    val nextSteps: List<String>
)

class VoxRelayAiRepository {

    private var activeChatRole: ChatbotRole? = null
    private var activeChatSession: Chat? = null

    private val dispatchSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "assistantReply" to Schema.string(
                    description = "Friendly multi-turn conversational reply acknowledging the user's message, answering questions, or confirming the prepared call plan."
                ),
                "hasCallIntent" to Schema.boolean(
                    description = "True when the user wants to place or prepare a phone call to a company or business."
                ),
                "companyName" to Schema.string(
                    description = "Name of the company, business, or service to call."
                ),
                "phoneNumber" to Schema.string(
                    description = "Extracted phone number if provided by the user anywhere in the conversation, or empty string if missing."
                ),
                "category" to Schema.string(
                    description = "Short category label such as Airline, Restaurant, Billing, Clinic, Delivery, Hotel, or Support."
                ),
                "objective" to Schema.string(
                    description = "Clear one-sentence summary of what the call should accomplish."
                ),
                "tone" to Schema.enumeration(
                    values = listOf("POLITE", "PROFESSIONAL", "CONCISE", "FIRM"),
                    description = "Recommended vocal tone for speaking with the representative."
                ),
                "openingScript" to Schema.string(
                    description = "Natural, spoken-ready opening statement (2-3 crisp sentences) to say aloud as soon as the representative answers."
                ),
                "talkingPoints" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 4 short, ready-to-speak follow-up lines the user can tap during the call."
                ),
                "anticipatedQuestions" to Schema.array(
                    items = Schema.string(),
                    description = "2 to 3 likely questions the company rep might ask."
                )
            )
        )
    }

    private val liveReplySchema by lazy {
        Schema.obj(
            properties = mapOf(
                "spokenReply" to Schema.string(
                    description = "Natural, human-sounding spoken response (1-2 concise sentences) ready to be spoken aloud over the phone right now."
                ),
                "stageLabel" to Schema.string(
                    description = "Short 1-2 word stage label such as Greeting, Verification, Negotiating, Confirming, or Wrap-Up."
                ),
                "goalProgress" to Schema.string(
                    description = "Brief status note describing progress toward the call objective."
                ),
                "suggestedFollowUps" to Schema.array(
                    items = Schema.string(),
                    description = "3 short quick-tap lines to speak next depending on how the rep responds."
                )
            )
        )
    }

    private val summarySchema by lazy {
        Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(
                    description = "Concise 2-sentence outcome summary of the phone call."
                ),
                "nextSteps" to Schema.array(
                    items = Schema.string(),
                    description = "1 to 3 concrete follow-up action items for the user."
                )
            )
        )
    }

    private fun buildDispatchChat(role: ChatbotRole): Chat {
        val systemText = buildString {
            appendLine(role.systemInstruction)
            appendLine("Always respond strictly using the configured JSON schema.")
            appendLine("Rules:")
            appendLine("- Maintain full context across all previous turns in this multi-turn chat.")
            appendLine("- If the user is asking a general question or brainstorming before calling, answer helpfully in assistantReply with hasCallIntent = false.")
            appendLine("- If the user wants to call a company (or updates details like phone number, time, or script for an ongoing call plan), set hasCallIntent = true and provide the complete updated call plan.")
            appendLine("- Never invent a fake phone number. Only copy a phone number explicitly mentioned by the user; otherwise return an empty string for phoneNumber.")
            appendLine("- Keep openingScript concise (under 45 words) in natural spoken English without markdown or brackets.")
        }

        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dispatchSchema
                temperature = 0.4f
            },
            systemInstruction = content {
                text(systemText)
            }
        )
        return model.startChat()
    }

    private val liveReplyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = liveReplySchema
                temperature = 0.35f
            },
            systemInstruction = content {
                text("You are VoxRelay speaking live on a phone call on behalf of the user. Produce natural, crisp spoken replies under 2 sentences with zero markdown.")
            }
        )
    }

    private val summaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = summarySchema
                temperature = 0.3f
            }
        )
    }

    // Dedicated audio transcription model per user feature specification
    private val transcriptionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe"
        )
    }

    fun resetChatSession() {
        activeChatSession = null
        activeChatRole = null
    }

    suspend fun sendMultiTurnChatMessage(
        userMessage: String,
        role: ChatbotRole,
        callerName: String,
        preferredTone: CallTone
    ): DispatchUnderstandingResult {
        val chat = if (activeChatSession == null || activeChatRole != role) {
            buildDispatchChat(role).also {
                activeChatSession = it
                activeChatRole = role
            }
        } else {
            activeChatSession!!
        }

        val callerContext = if (callerName.isNotBlank()) {
            "Caller name: $callerName. Preferred tone: ${preferredTone.name}."
        } else {
            "Preferred tone: ${preferredTone.name}."
        }

        val enrichedTurn = "$callerContext\nUser message: $userMessage"

        val response = withTimeout(60_000L) {
            chat.sendMessage(enrichedTurn)
        }
        val rawJson = response.text ?: throw IllegalStateException("Empty response from Gemini.")
        val json = JSONObject(rawJson)

        val assistantReply = json.optString("assistantReply").ifBlank {
            "I understood your message and prepared the call details."
        }
        val hasCallIntent = json.optBoolean("hasCallIntent", false)
        val companyName = json.optString("companyName").trim()

        if (!hasCallIntent || companyName.isBlank()) {
            return DispatchUnderstandingResult(
                assistantReply = assistantReply,
                callPlan = null
            )
        }

        val extractedPhone = json.optString("phoneNumber").trim()
        val talkingPoints = json.optJSONArray("talkingPoints").toStringList()
        val anticipated = json.optJSONArray("anticipatedQuestions").toStringList()

        val plan = CallPlan(
            companyName = companyName,
            phoneNumber = extractedPhone,
            category = json.optString("category", "Support").ifBlank { "Support" },
            objective = json.optString("objective", userMessage).ifBlank { userMessage },
            tone = CallTone.fromRaw(json.optString("tone", preferredTone.name)),
            openingScript = json.optString("openingScript").ifBlank {
                "Hello, I am calling regarding $companyName. $userMessage"
            },
            talkingPoints = talkingPoints.ifEmpty {
                listOf(
                    "Could you confirm the earliest available option?",
                    "Yes, please go ahead and update that for me.",
                    "Could you share a confirmation number before we finish?"
                )
            },
            anticipatedQuestions = anticipated,
            needsPhoneNumber = extractedPhone.isBlank()
        )

        return DispatchUnderstandingResult(
            assistantReply = assistantReply,
            callPlan = plan
        )
    }

    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4"
    ): String {
        val response = withTimeout(60_000L) {
            transcriptionModel.generateContent(
                content {
                    inlineData(bytes = audioBytes, mimeType = mimeType)
                    text("Transcribe this spoken audio accurately. Return only the clean transcript text without extra commentary.")
                }
            )
        }
        val transcript = response.text?.trim().orEmpty()
        if (transcript.isBlank()) {
            throw IllegalStateException("Gemini returned an empty audio transcript.")
        }
        return transcript
    }

    suspend fun draftLiveSpokenReply(
        session: LiveCallSession,
        representativeSaid: String,
        callerName: String
    ): LiveReplyResult {
        val transcriptText = session.transcript
            .takeLast(12)
            .joinToString("\n") { line ->
                val who = when (line.role) {
                    SpeakerRole.AI_VOICE -> "VoxRelay"
                    SpeakerRole.COMPANY_REP -> "Representative"
                    SpeakerRole.SYSTEM -> "System"
                }
                "$who: ${line.text}"
            }

        val prompt = buildString {
            appendLine("Company on the line: ${session.plan.companyName}")
            appendLine("Call objective: ${session.plan.objective}")
            appendLine("Desired vocal tone: ${session.plan.tone.name}")
            if (callerName.isNotBlank()) {
                appendLine("Caller name: $callerName")
            }
            appendLine("If the rep asks for private info you don't know (card number, confirmation code, birth date), politely ask them to hold one second while the caller provides it.")
            appendLine()
            appendLine("Transcript so far:")
            appendLine(transcriptText.ifBlank { "(Call just started)" })
            appendLine()
            appendLine("Company representative just said:")
            appendLine(representativeSaid)
        }

        val response = withTimeout(60_000L) {
            liveReplyModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("Empty live reply from Gemini.")
        val json = JSONObject(rawJson)

        val spokenReply = json.optString("spokenReply").ifBlank {
            "Thank you for checking on that. Could you let me know what options are available?"
        }
        val stageLabel = json.optString("stageLabel", "In Call").ifBlank { "In Call" }
        val goalProgress = json.optString("goalProgress", "Responding to representative").ifBlank {
            "Responding to representative"
        }
        val followUps = json.optJSONArray("suggestedFollowUps").toStringList()

        return LiveReplyResult(
            spokenReply = spokenReply,
            stageLabel = stageLabel,
            goalProgress = goalProgress,
            suggestedFollowUps = followUps.ifEmpty { session.suggestedFollowUps }
        )
    }

    suspend fun summarizeCompletedCall(session: LiveCallSession): CallSummaryResult {
        val transcriptText = session.transcript.joinToString("\n") { line ->
            val who = when (line.role) {
                SpeakerRole.AI_VOICE -> "VoxRelay"
                SpeakerRole.COMPANY_REP -> "Representative"
                SpeakerRole.SYSTEM -> "System"
            }
            "$who: ${line.text}"
        }

        val prompt = buildString {
            appendLine("Summarize this completed phone call with ${session.plan.companyName}.")
            appendLine("Original objective: ${session.plan.objective}")
            appendLine("Transcript:")
            appendLine(transcriptText.ifBlank { "Opening script prepared: ${session.plan.openingScript}" })
        }

        val response = withTimeout(60_000L) {
            summaryModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("Empty call summary from Gemini.")
        val json = JSONObject(rawJson)

        val summary = json.optString("summary").ifBlank {
            "Completed call session with ${session.plan.companyName} regarding: ${session.plan.objective}."
        }
        val nextSteps = json.optJSONArray("nextSteps").toStringList()

        return CallSummaryResult(
            summary = summary,
            nextSteps = nextSteps.ifEmpty { listOf("Check your email or SMS for confirmation from ${session.plan.companyName}.") }
        )
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optString(index).trim()
                if (item.isNotEmpty()) add(item)
            }
        }
    }
}
