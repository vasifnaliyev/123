package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiTaskState
import com.example.model.LiveCallSession
import com.example.model.SpeakerRole
import com.example.model.TranscriptLine
import com.example.ui.components.AcousticOrb
import com.example.ui.components.AcousticOrbMode
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.ListeningTarget
import com.example.viewmodel.VoxRelayUiState

@Composable
fun LiveCallStudioScreen(
    uiState: VoxRelayUiState,
    onDialCompany: () -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onSpeakLine: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onListenToRepToggle: () -> Unit,
    onSubmitRepText: (String) -> Unit,
    onEndCall: () -> Unit,
    onStartManualCall: (company: String, phone: String, script: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCall = uiState.activeCall
    if (activeCall == null) {
        EmptyCallStudioContent(
            onStartManualCall = onStartManualCall,
            modifier = modifier
        )
    } else {
        ActiveCallConsoleContent(
            session = activeCall,
            uiState = uiState,
            onDialCompany = onDialCompany,
            onToggleSpeakerphone = onToggleSpeakerphone,
            onSpeakLine = onSpeakLine,
            onStopSpeaking = onStopSpeaking,
            onListenToRepToggle = onListenToRepToggle,
            onSubmitRepText = onSubmitRepText,
            onEndCall = onEndCall,
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyCallStudioContent(
    onStartManualCall: (company: String, phone: String, script: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var company by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var script by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        AcousticOrb(
            mode = AcousticOrbMode.IDLE,
            audioLevel = 0f,
            orbSize = 148.dp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.studio_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.studio_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Manual Call & Speak",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_company_input"),
                    label = { Text(stringResource(R.string.studio_quick_company_label)) },
                    placeholder = { Text("e.g. City Electric Support") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_phone_input"),
                    label = { Text(stringResource(R.string.phone_number_label)) },
                    placeholder = { Text(stringResource(R.string.phone_number_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFontFamily),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = script,
                    onValueChange = { script = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_script_input"),
                    label = { Text(stringResource(R.string.studio_quick_script_label)) },
                    placeholder = { Text("Hello, I'm calling to check my service appointment time…") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = { onStartManualCall(company, phone, script) },
                    enabled = company.isNotBlank() || script.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("manual_start_call_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.studio_start_manual_call),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveCallConsoleContent(
    session: LiveCallSession,
    uiState: VoxRelayUiState,
    onDialCompany: () -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onSpeakLine: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onListenToRepToggle: () -> Unit,
    onSubmitRepText: (String) -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var repDraft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isListeningRep = uiState.transcriptionState is com.example.model.AudioTranscriptionState.Recording &&
        uiState.transcriptionTarget == ListeningTarget.LIVE_REP
    val isTranscribingRep = uiState.transcriptionState is com.example.model.AudioTranscriptionState.Transcribing &&
        uiState.transcriptionTarget == ListeningTarget.LIVE_REP
    val isThinking = uiState.aiState is AiTaskState.Busy || isTranscribingRep

    val orbMode = when {
        uiState.isSpeaking -> AcousticOrbMode.SPEAKING
        isListeningRep -> AcousticOrbMode.LISTENING
        isThinking -> AcousticOrbMode.THINKING
        else -> AcousticOrbMode.IDLE
    }

    val statusLabel = when {
        isTranscribingRep -> stringResource(R.string.status_transcribing_audio)
        isListeningRep -> {
            val secs = (uiState.transcriptionState as? com.example.model.AudioTranscriptionState.Recording)?.elapsedSeconds ?: 0L
            stringResource(R.string.status_recording_audio, secs)
        }
        orbMode == AcousticOrbMode.SPEAKING -> stringResource(R.string.status_speaking)
        orbMode == AcousticOrbMode.THINKING -> stringResource(R.string.status_thinking)
        else -> stringResource(R.string.status_ready)
    }

    LaunchedEffect(session.transcript.size) {
        if (session.transcript.isNotEmpty()) {
            listState.animateScrollToItem(session.transcript.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Active Call Telemetry Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = session.stageLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = formatCallDuration(session.elapsedSeconds),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = MonoFontFamily,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = session.plan.companyName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (session.plan.phoneNumber.isNotBlank()) {
                            Text(
                                text = session.plan.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = MonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onEndCall,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("end_call_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "End",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Utility Controls Row: Dial Phone + Speakerphone Toggle + Speak Opening
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (session.plan.phoneNumber.isNotBlank()) {
                        OutlinedButton(
                            onClick = onDialCompany,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("studio_dial_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneInTalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_dial_phone),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onToggleSpeakerphone,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("studio_speaker_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isSpeakerphoneOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isSpeakerphoneOn) {
                                stringResource(R.string.action_speaker_on)
                            } else {
                                stringResource(R.string.action_speaker_off)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Scrollable Acoustic Centerpiece + Quick Speak + Live Transcript
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Acoustic Orb + Primary Voice Controls
            item(key = "acoustic_stage") {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AcousticOrb(
                            mode = orbMode,
                            audioLevel = uiState.audioLevel,
                            orbSize = 152.dp
                        )

                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isListeningRep && uiState.partialTranscript.isNotBlank()) {
                                "\"${uiState.partialTranscript}\""
                            } else if (uiState.isSpeaking && uiState.currentlySpokenText.isNotBlank()) {
                                "\"${uiState.currentlySpokenText}\""
                            } else {
                                session.goalProgress
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (uiState.isSpeaking) {
                                        onStopSpeaking()
                                    } else {
                                        onSpeakLine(session.plan.openingScript)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("speak_opening_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = if (uiState.isSpeaking) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isSpeaking) {
                                        stringResource(R.string.action_stop_voice)
                                    } else {
                                        stringResource(R.string.action_speak_opening)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = onListenToRepToggle,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("listen_rep_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListeningRep) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    contentColor = if (isListeningRep) {
                                        MaterialTheme.colorScheme.onError
                                    } else {
                                        MaterialTheme.colorScheme.onSecondary
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = if (isListeningRep) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isListeningRep) {
                                        stringResource(R.string.action_stop_listen)
                                    } else {
                                        stringResource(R.string.action_listen_rep)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.speakerphone_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Quick-Tap Spoken Replies
            if (session.suggestedFollowUps.isNotEmpty()) {
                item(key = "quick_speak_chips") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.talking_points_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(session.suggestedFollowUps) { line ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .width(236.dp)
                                        .clickable { onSpeakLine(line) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VolumeUp,
                                            contentDescription = "Speak reply",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "transcript_header") {
                Text(
                    text = stringResource(R.string.live_transcript_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            items(session.transcript, key = { it.id }) { line ->
                TranscriptBubble(
                    line = line,
                    onReplaySpeak = { onSpeakLine(line.text) }
                )
            }
        }

        // Bottom Rep Statement Relay Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = repDraft,
                    onValueChange = { repDraft = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rep_statement_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.rep_input_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (repDraft.isNotBlank()) {
                                onSubmitRepText(repDraft)
                                repDraft = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                IconButton(
                    onClick = {
                        if (repDraft.isNotBlank()) {
                            onSubmitRepText(repDraft)
                            repDraft = ""
                        }
                    },
                    enabled = repDraft.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("rep_statement_send"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cd_send_rep_line)
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptBubble(
    line: TranscriptLine,
    onReplaySpeak: () -> Unit
) {
    val roleLabel = when (line.role) {
        SpeakerRole.AI_VOICE -> stringResource(R.string.role_ai_voice)
        SpeakerRole.COMPANY_REP -> stringResource(R.string.role_company_rep)
        SpeakerRole.SYSTEM -> stringResource(R.string.role_system)
    }

    val containerColor = when (line.role) {
        SpeakerRole.AI_VOICE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        SpeakerRole.COMPANY_REP -> MaterialTheme.colorScheme.surfaceContainerHigh
        SpeakerRole.SYSTEM -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roleLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (line.role) {
                        SpeakerRole.AI_VOICE -> MaterialTheme.colorScheme.primary
                        SpeakerRole.COMPANY_REP -> MaterialTheme.colorScheme.secondary
                        SpeakerRole.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (line.role == SpeakerRole.AI_VOICE) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onReplaySpeak() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Speak this line again",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatCallDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
