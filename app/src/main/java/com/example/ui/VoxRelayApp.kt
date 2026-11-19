package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.AudioTranscriptionState
import com.example.model.CallPlan
import com.example.model.VoxTab
import com.example.ui.screens.CallScreen
import com.example.ui.screens.ChatDispatchScreen
import com.example.ui.screens.HistoryAndVoiceScreen
import com.example.ui.screens.LiveCallStudioScreen
import com.example.viewmodel.ListeningTarget
import com.example.viewmodel.VoxRelayViewModel

private data class NavDestination(
    val tab: VoxTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val AppDestinations = listOf(
    NavDestination(
        tab = VoxTab.DISPATCH,
        labelRes = R.string.nav_chat,
        selectedIcon = Icons.Rounded.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_dispatch"
    ),
    NavDestination(
        tab = VoxTab.DIALER,
        labelRes = R.string.nav_dialer,
        selectedIcon = Icons.Rounded.Dialpad,
        unselectedIcon = Icons.Outlined.Dialpad,
        testTag = "nav_dialer"
    ),
    NavDestination(
        tab = VoxTab.STUDIO,
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Rounded.PhoneInTalk,
        unselectedIcon = Icons.Outlined.PhoneInTalk,
        testTag = "nav_studio"
    ),
    NavDestination(
        tab = VoxTab.LOGS_VOICE,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_logs_voice"
    )
)

@Composable
fun VoxRelayApp(
    viewModel: VoxRelayViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingRecordTarget by remember { mutableStateOf<ListeningTarget?>(null) }
    var pendingCallPlan by remember { mutableStateOf<CallPlan?>(null) }

    val micDeniedMsg = stringResource(R.string.permission_mic_denied)
    val callFallbackMsg = stringResource(R.string.permission_call_fallback)

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingRecordTarget
        pendingRecordTarget = null
        if (granted && target != null) {
            viewModel.startAudioRecording(target)
        } else if (!granted) {
            viewModel.postNotice(micDeniedMsg)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val plan = pendingCallPlan
        pendingCallPlan = null
        if (!granted) {
            viewModel.postNotice(callFallbackMsg)
        }
        if (plan != null) {
            viewModel.launchCallAndSpeak(plan = plan, activityContext = context)
        }
    }

    fun requestOrToggleAudioTranscription(target: ListeningTarget) {
        if (uiState.transcriptionState is AudioTranscriptionState.Recording) {
            viewModel.stopRecordingAndTranscribe()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.toggleAudioRecordingForTranscription(target)
        } else {
            pendingRecordTarget = target
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun startCallFlow(plan: CallPlan) {
        if (uiState.voiceSettings.directCallMode) {
            val hasCallPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasCallPerm) {
                pendingCallPlan = plan
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                return
            }
        }
        viewModel.launchCallAndSpeak(plan = plan, activityContext = context)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    BackHandler(enabled = uiState.selectedTab != VoxTab.DISPATCH) {
        viewModel.selectTab(VoxTab.DISPATCH)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppDestinations.forEach { dest ->
                            val selected = uiState.selectedTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    DestinationIcon(
                                        dest = dest,
                                        selected = selected,
                                        hasActiveCall = uiState.activeCall != null
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppDestinations.forEach { dest ->
                            val selected = uiState.selectedTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    DestinationIcon(
                                        dest = dest,
                                        selected = selected,
                                        hasActiveCall = uiState.activeCall != null
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)

                    when (uiState.selectedTab) {
                        VoxTab.DISPATCH -> ChatDispatchScreen(
                            uiState = uiState,
                            onSelectRole = viewModel::selectChatbotRole,
                            onDraftChange = viewModel::updateChatDraft,
                            onSendMessage = { viewModel.sendChatMessage() },
                            onQuickPromptClick = { sample ->
                                viewModel.updateChatDraft(sample)
                                viewModel.sendChatMessage(sample)
                            },
                            onMicRecordToggle = { requestOrToggleAudioTranscription(ListeningTarget.CHAT_COMPOSER) },
                            onCancelRecording = viewModel::cancelAudioRecording,
                            onClearChat = viewModel::clearChatSession,
                            onUpdatePhoneNumber = viewModel::updatePlanPhoneNumber,
                            onUpdateOpeningScript = viewModel::updatePlanOpeningScript,
                            onPreviewVoice = viewModel::previewOrStopVoice,
                            onOpenInDialer = viewModel::openPlanInDialer,
                            onCallAndSpeak = ::startCallFlow,
                            onOpenActiveStudio = { viewModel.selectTab(VoxTab.STUDIO) },
                            modifier = contentModifier
                        )

                        VoxTab.DIALER -> CallScreen(
                            initialPhoneNumber = uiState.dialerPhoneDraft,
                            initialCompanyName = uiState.dialerCompanyDraft,
                            onPhoneNumberChanged = viewModel::updateDialerPhoneDraft,
                            onLaunchVoiceStudio = { company, phone, script ->
                                viewModel.startQuickManualCall(company, phone, script, context)
                            },
                            onStatusMessage = viewModel::postNotice,
                            modifier = contentModifier
                        )

                        VoxTab.STUDIO -> LiveCallStudioScreen(
                            uiState = uiState,
                            onDialCompany = { viewModel.dialActiveCompany(context) },
                            onToggleSpeakerphone = viewModel::toggleSpeakerphone,
                            onSpeakLine = viewModel::speakLiveLine,
                            onStopSpeaking = viewModel::stopSpeaking,
                            onListenToRepToggle = { requestOrToggleAudioTranscription(ListeningTarget.LIVE_REP) },
                            onSubmitRepText = viewModel::submitRepresentativeStatement,
                            onEndCall = viewModel::endActiveCallAndSummarize,
                            onStartManualCall = { company, phone, script ->
                                viewModel.startQuickManualCall(company, phone, script, context)
                            },
                            modifier = contentModifier
                        )

                        VoxTab.LOGS_VOICE -> HistoryAndVoiceScreen(
                            uiState = uiState,
                            onCallerNameChange = viewModel::updateCallerName,
                            onSpeechRateChange = viewModel::updateSpeechRate,
                            onSpeechPitchChange = viewModel::updateSpeechPitch,
                            onSelectVoice = viewModel::updateSelectedVoice,
                            onSelectDefaultTone = viewModel::updateDefaultTone,
                            onToggleAutoSpeak = viewModel::updateAutoSpeakReplies,
                            onToggleAutoSpeakerphone = viewModel::updateAutoSpeakerphone,
                            onToggleDirectCallMode = { enabled ->
                                viewModel.updateDirectCallMode(enabled)
                                if (enabled) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CALL_PHONE
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                    }
                                }
                            },
                            onTestVoice = viewModel::previewOrStopVoice,
                            onRedialRecord = { record ->
                                viewModel.startQuickManualCall(
                                    companyName = record.companyName,
                                    phoneNumber = record.phoneNumber,
                                    script = "Hello, I am following up regarding ${record.objective}",
                                    activityContext = context
                                )
                            },
                            onReuseInChat = viewModel::reuseCallInChat,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    dest: NavDestination,
    selected: Boolean,
    hasActiveCall: Boolean
) {
    val icon = if (selected) dest.selectedIcon else dest.unselectedIcon
    if (dest.tab == VoxTab.STUDIO && hasActiveCall) {
        BadgedBox(
            badge = {
                Badge(containerColor = MaterialTheme.colorScheme.secondary)
            }
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(dest.labelRes))
        }
    } else {
        Icon(imageVector = icon, contentDescription = stringResource(dest.labelRes))
    }
}
