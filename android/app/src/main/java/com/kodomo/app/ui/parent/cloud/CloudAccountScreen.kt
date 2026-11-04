package com.kodomo.app.ui.parent.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.BuildConfig
import com.kodomo.app.core.util.PersianDigits
import com.kodomo.app.core.util.fa
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.data.prefs.SessionState
import com.kodomo.app.data.remote.LiveUsageSocket
import com.kodomo.app.data.repository.CloudSyncRepository
import com.kodomo.app.domain.repository.ForumRepository
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CloudAccountViewModel @Inject constructor(
    private val cloud: CloudSyncRepository,
    private val session: SessionPreferences,
    private val forumRepository: ForumRepository,
    private val liveUsageSocket: LiveUsageSocket,
) : ViewModel() {
    val state: StateFlow<SessionState?> = session.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val requestId = MutableStateFlow<String?>(null)

    private fun launchBusy(block: suspend () -> Unit) = viewModelScope.launch {
        busy.value = true
        try {
            block()
        } finally {
            busy.value = false
        }
    }

    fun saveServer(url: String) = viewModelScope.launch {
        session.setApiBaseUrl(url)
        message.value = "آدرس سرور ذخیره شد."
    }

    fun requestOtp(phone: String) = launchBusy {
        when (val r = cloud.requestOtp(phone)) {
            is CloudSyncRepository.Result.Success -> {
                requestId.value = r.value
                message.value = "کد تأیید پیامک شد."
            }
            is CloudSyncRepository.Result.Failure -> message.value = r.message
        }
    }

    fun verify(phone: String, code: String) = launchBusy {
        val id = requestId.value ?: return@launchBusy
        when (val r = cloud.verifyOtp(id, phone, code)) {
            is CloudSyncRepository.Result.Success -> {
                requestId.value = null
                message.value = "ورود موفق بود. حالا می‌توانید همگام‌سازی کنید."
                liveUsageSocket.ensureConnected()
            }
            is CloudSyncRepository.Result.Failure -> message.value = r.message
        }
    }

    fun syncAll() = launchBusy {
        val profiles = cloud.syncFamily()
        val catalog = cloud.pullCatalog()
        forumRepository.refreshFromServer()
        message.value = buildString {
            append(if (profiles is CloudSyncRepository.Result.Failure) "پروفایل‌ها: ${profiles.message}\n" else "پروفایل‌ها پشتیبان‌گیری شد.\n")
            append(
                when (catalog) {
                    is CloudSyncRepository.Result.Success -> "${catalog.value.fa()} محتوای جدید یا به‌روز دریافت شد."
                    is CloudSyncRepository.Result.Failure -> "کاتالوگ: ${catalog.message}"
                },
            )
        }
    }

    fun logout() = viewModelScope.launch {
        liveUsageSocket.close()
        cloud.logout()
        message.value = "از حساب ابری خارج شدید."
    }
}

@Composable
fun CloudAccountScreen(onBack: () -> Unit, viewModel: CloudAccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val requestId by viewModel.requestId.collectAsStateWithLifecycle()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var server by remember(state?.apiBaseUrl) { mutableStateOf(state?.apiBaseUrl ?: BuildConfig.API_BASE_URL) }
    val loggedIn = state?.authToken != null

    KidScreenScaffold(title = "حساب ابری کدومو", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KodomoCard(Modifier.fillMaxWidth()) {
                SectionTitle("سرور")
                OutlinedTextField(server, { server = it }, label = { Text("آدرس API") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { viewModel.saveServer(server) }) { Text("ذخیره‌ی آدرس") }
            }
            if (!loggedIn) {
                KodomoCard(Modifier.fillMaxWidth()) {
                    SectionTitle("ورود با رمز یکبار مصرف")
                    OutlinedTextField(
                        phone, { phone = PersianDigits.digitsOnly(it).take(12) }, label = { Text("شماره موبایل والد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    if (requestId == null) {
                        KodomoPrimaryButton("دریافت کد", onClick = { viewModel.requestOtp(phone) }, enabled = phone.length >= 10 && !busy)
                    } else {
                        OutlinedTextField(
                            code, { code = PersianDigits.digitsOnly(it).take(6) }, label = { Text("کد تأیید") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                        KodomoPrimaryButton("تأیید و ورود", onClick = { viewModel.verify(phone, code) }, enabled = code.length >= 4 && !busy)
                    }
                }
            } else {
                KodomoCard(Modifier.fillMaxWidth()) {
                    SectionTitle("همگام‌سازی")
                    val last = state?.lastSyncMillis ?: 0L
                    Text(
                        if (last > 0) "آخرین همگام‌سازی: ${DateFormat.getDateTimeInstance().format(Date(last)).fa()}" else "هنوز همگام‌سازی نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    KodomoPrimaryButton("همگام‌سازی پروفایل‌ها، کاتالوگ و تالار", onClick = viewModel::syncAll, enabled = !busy)
                    OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) { Text("خروج از حساب ابری") }
                }
            }
            if (busy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator() }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
