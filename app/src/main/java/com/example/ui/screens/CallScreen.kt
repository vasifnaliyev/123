package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.MonoFontFamily
import com.example.voice.PhoneCallLauncher

private val DialPadRows = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@Composable
fun CallScreen(
    initialPhoneNumber: String = "",
    initialCompanyName: String = "",
    onPhoneNumberChanged: (String) -> Unit = {},
    onLaunchVoiceStudio: ((companyName: String, phoneNumber: String, script: String) -> Unit)? = null,
    onStatusMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var phoneNumber by rememberSaveable(initialPhoneNumber) { mutableStateOf(initialPhoneNumber) }
    var companyLabel by rememberSaveable(initialCompanyName) { mutableStateOf(initialCompanyName) }
    var spokenMessage by rememberSaveable { mutableStateOf("") }

    val invalidNumberMsg = stringResource(R.string.error_missing_phone)
    val dialerUnavailableMsg = stringResource(R.string.dialer_unavailable)

    fun initiateDialerIntent() {
        val cleaned = PhoneCallLauncher.sanitizePhoneNumber(phoneNumber)
        if (cleaned.count { it.isDigit() } < 3) {
            onStatusMessage(invalidNumberMsg)
            return
        }
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.fromParts("tel", cleaned, null)
        }
        try {
            context.startActivity(dialIntent)
            onStatusMessage("Opened dialer for $cleaned")
        } catch (_: ActivityNotFoundException) {
            onStatusMessage(dialerUnavailableMsg)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Dialpad,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.call_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.call_screen_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Phone Number Card + Dialer Button
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { updated ->
                        phoneNumber = updated
                        onPhoneNumberChanged(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("call_screen_phone_input"),
                    label = { Text(stringResource(R.string.phone_number_label)) },
                    placeholder = { Text(stringResource(R.string.phone_number_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (phoneNumber.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    phoneNumber = ""
                                    onPhoneNumberChanged("")
                                },
                                modifier = Modifier.testTag("call_screen_clear_phone")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.cd_clear_phone_number)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { initiateDialerIntent() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Tactile Dial Pad Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialPadRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (digit, letters) ->
                                Surface(
                                    onClick = {
                                        val next = phoneNumber + digit
                                        phoneNumber = next
                                        onPhoneNumberChanged(next)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("dial_key_$digit")
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = digit,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontFamily = MonoFontFamily,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (letters.isNotEmpty()) {
                                            Text(
                                                text = letters,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Primary Dial Intent Button + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { initiateDialerIntent() },
                        enabled = phoneNumber.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("call_screen_dial_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_open_dialer),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (phoneNumber.isNotEmpty()) {
                                val next = phoneNumber.dropLast(1)
                                phoneNumber = next
                                onPhoneNumberChanged(next)
                            }
                        },
                        enabled = phoneNumber.isNotEmpty(),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("call_screen_backspace"),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = stringResource(R.string.cd_backspace_digit),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Optional Bridge to AI Voice Studio
        if (onLaunchVoiceStudio != null) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.call_screen_studio_bridge_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = companyLabel,
                        onValueChange = { companyLabel = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_screen_company_input"),
                        label = { Text(stringResource(R.string.studio_quick_company_label)) },
                        placeholder = { Text("e.g. Horizon Air Support") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = spokenMessage,
                        onValueChange = { spokenMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_screen_script_input"),
                        label = { Text(stringResource(R.string.studio_quick_script_label)) },
                        placeholder = { Text("Optional line for VoxRelay to speak aloud…") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            onLaunchVoiceStudio(
                                companyLabel.ifBlank { "Company Call" },
                                phoneNumber,
                                spokenMessage
                            )
                        },
                        enabled = phoneNumber.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("call_screen_launch_studio_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_dial_and_speak_studio),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
