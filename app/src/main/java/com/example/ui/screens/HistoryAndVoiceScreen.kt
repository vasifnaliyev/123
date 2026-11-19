package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CallTone
import com.example.model.CompletedCallRecord
import com.example.model.SpeakerRole
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.VoxRelayUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryAndVoiceScreen(
    uiState: VoxRelayUiState,
    onCallerNameChange: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onSelectVoice: (String) -> Unit,
    onSelectDefaultTone: (CallTone) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onToggleAutoSpeakerphone: (Boolean) -> Unit,
    onToggleDirectCallMode: (Boolean) -> Unit,
    onTestVoice: (String) -> Unit,
    onRedialRecord: (CompletedCallRecord) -> Unit,
    onReuseInChat: (CompletedCallRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.voiceSettings
    val sampleTestPhrase = if (settings.callerName.isNotBlank()) {
        stringResource(R.string.test_voice_sample, settings.callerName)
    } else {
        stringResource(R.string.test_voice_sample_anonymous)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Voice & Caller Studio
        item(key = "voice_studio_header") {
            SectionTitleRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.section_voice_studio)
            )
        }

        item(key = "voice_settings_card") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = settings.callerName,
                        onValueChange = onCallerNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_name_input"),
                        label = { Text(stringResource(R.string.caller_name_label)) },
                        placeholder = { Text(stringResource(R.string.caller_name_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Tone Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.default_tone_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CallTone.entries) { tone ->
                                FilterChip(
                                    selected = settings.defaultTone == tone,
                                    onClick = { onSelectDefaultTone(tone) },
                                    modifier = Modifier.testTag("tone_chip_${tone.name.lowercase()}"),
                                    label = { Text(tone.label) }
                                )
                            }
                        }
                    }

                    // Device TTS Voice Picker
                    if (uiState.availableVoices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.voice_persona_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item(key = "default_voice") {
                                    FilterChip(
                                        selected = settings.selectedVoiceId.isBlank(),
                                        onClick = { onSelectVoice("") },
                                        label = { Text(stringResource(R.string.voice_default_name)) }
                                    )
                                }
                                items(uiState.availableVoices, key = { it.id }) { voice ->
                                    FilterChip(
                                        selected = settings.selectedVoiceId == voice.id,
                                        onClick = { onSelectVoice(voice.id) },
                                        label = { Text(voice.displayName) }
                                    )
                                }
                            }
                        }
                    }

                    // Speech Rate Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.speech_rate_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.2fx".format(settings.speechRate),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = MonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.speechRate,
                            onValueChange = onSpeechRateChange,
                            valueRange = 0.70f..1.45f,
                            modifier = Modifier.testTag("speech_rate_slider")
                        )
                    }

                    // Speech Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.speech_pitch_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.2fx".format(settings.speechPitch),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = MonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.speechPitch,
                            onValueChange = onSpeechPitchChange,
                            valueRange = 0.75f..1.35f,
                            modifier = Modifier.testTag("speech_pitch_slider")
                        )
                    }

                    Button(
                        onClick = { onTestVoice(sampleTestPhrase) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("test_voice_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isSpeaking) {
                                stringResource(R.string.action_stop_voice)
                            } else {
                                stringResource(R.string.action_test_voice)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SettingToggleRow(
                        title = stringResource(R.string.toggle_auto_speak),
                        subtitle = stringResource(R.string.toggle_auto_speak_desc),
                        checked = settings.autoSpeakReplies,
                        onCheckedChange = onToggleAutoSpeak,
                        testTag = "toggle_auto_speak"
                    )

                    SettingToggleRow(
                        title = stringResource(R.string.toggle_auto_speaker),
                        subtitle = stringResource(R.string.toggle_auto_speaker_desc),
                        checked = settings.autoSpeakerphone,
                        onCheckedChange = onToggleAutoSpeakerphone,
                        testTag = "toggle_auto_speaker"
                    )

                    SettingToggleRow(
                        title = stringResource(R.string.toggle_direct_call),
                        subtitle = stringResource(R.string.toggle_direct_call_desc),
                        checked = settings.directCallMode,
                        onCheckedChange = onToggleDirectCallMode,
                        testTag = "toggle_direct_call"
                    )
                }
            }
        }

        // Section 2: Session Call Logs
        item(key = "call_logs_header") {
            SectionTitleRow(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.section_call_logs)
            )
        }

        if (uiState.callHistory.isEmpty()) {
            item(key = "empty_logs_card") {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_logs_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.empty_logs_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.callHistory, key = { it.id }) { record ->
                CallHistoryCard(
                    record = record,
                    onRedial = { onRedialRecord(record) },
                    onReuse = { onReuseInChat(record) }
                )
            }
        }

        item(key = "bottom_pad") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun CallHistoryCard(
    record: CompletedCallRecord,
    onRedial: () -> Unit,
    onReuse: () -> Unit
) {
    var expanded by remember(record.id) { mutableStateOf(false) }
    val formattedTime = remember(record.startedAtMs) {
        SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date(record.startedAtMs))
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = listOfNotNull(
                            record.phoneNumber.takeIf { it.isNotBlank() },
                            formattedTime,
                            "${record.durationSeconds}s"
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse transcript" else "Expand transcript",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = record.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (record.nextSteps.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NEXT STEPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        record.nextSteps.forEach { step ->
                            Text(
                                text = "• $step",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    record.transcript.forEach { line ->
                        val prefix = when (line.role) {
                            SpeakerRole.AI_VOICE -> "VoxRelay"
                            SpeakerRole.COMPANY_REP -> "Rep"
                            SpeakerRole.SYSTEM -> "System"
                        }
                        Text(
                            text = "$prefix: ${line.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (record.phoneNumber.isNotBlank()) {
                    OutlinedButton(
                        onClick = onRedial,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_redial))
                    }
                }

                OutlinedButton(
                    onClick = onReuse,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_reuse_chat))
                }
            }
        }
    }
}
