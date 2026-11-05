package com.kodomo.app.ui.parent.family

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.ChildDraft
import com.kodomo.app.domain.model.MemoryPhoto
import com.kodomo.app.domain.model.MemorySource
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.MemoryRepository
import com.kodomo.app.ui.components.ChildProfileForm
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoAvatar
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.parent.ParentHubViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import javax.inject.Inject

@Composable
fun FamilyTab(
    viewModel: ParentHubViewModel,
    onEditChild: (Long) -> Unit,
    onAddChild: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCloud: () -> Unit,
    onOpenAllowedApps: () -> Unit,
    onOpenSites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val family by viewModel.family.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    var editFamily by remember { mutableStateOf(false) }
    var changePin by remember { mutableStateOf(false) }

    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            KodomoCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KodomoAvatar(AvatarKind.FATHER, size = 48.dp)
                    KodomoAvatar(AvatarKind.MOTHER, size = 48.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(family?.familyName.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text("${(children.size + listOfNotNull(family?.fatherName?.takeIf { it.isNotBlank() }, family?.motherName?.takeIf { it.isNotBlank() }).size).fa()} عضو", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = { editFamily = true }) { Text("ویرایش") }
                }
            }
        }
        item { SectionTitle("اعضای خانواده") }
        family?.let { f ->
            if (f.fatherName.isNotBlank()) item { MemberRow(AvatarKind.FATHER, f.fatherName, "والد یا سرپرست اول", null) }
            if (f.motherName.isNotBlank()) item { MemberRow(AvatarKind.MOTHER, f.motherName, "والد یا سرپرست دوم", null) }
        }
        items(children, key = { it.id }) { child ->
            MemberRow(child.avatar, child.name, "${child.ageYears().fa()} ساله • ${child.ageGroup().label} • ⭐ ${child.stars.fa()}") { onEditChild(child.id) }
        }
        item { OutlinedButton(onClick = onAddChild, modifier = Modifier.fillMaxWidth()) { Text("➕ افزودن فرزند") } }
        item { SectionTitle("امکانات") }
        item { MemberRow(AvatarKind.ROBOT, "برنامه‌های مجاز", "بازی‌ها و برنامه‌هایی که کودک اجازه دارد از داخل کدومو باز کند", onOpenAllowedApps) }
        item { MemberRow(AvatarKind.ROBOT, "سایت‌های مجاز مرورگر", "سایت‌هایی که «مرورگر امن من» اجازه دارد باز کند و درخواست‌های فرزند", onOpenSites) }
        item { MemberRow(AvatarKind.ROBOT, "گالری محرمانه‌ی خاطرات", "عکس‌های رمزنگاری‌شده‌ی پروژه‌ها و لحظه‌های شاد", onOpenGallery) }
        item { MemberRow(AvatarKind.ROBOT, "حساب ابری و همگام‌سازی", "ورود با کد یکبارمصرف، پشتیبان پروفایل‌ها و پایش زنده", onOpenCloud) }
        item { OutlinedButton(onClick = { changePin = true }, modifier = Modifier.fillMaxWidth()) { Text("🔐 تغییر رمز والدین") } }
        item { CrashReportCard() }
        item { ExitAppCard() }
    }

    if (editFamily) {
        var name by remember { mutableStateOf(family?.familyName.orEmpty()) }
        var father by remember { mutableStateOf(family?.fatherName.orEmpty()) }
        var mother by remember { mutableStateOf(family?.motherName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editFamily = false },
            title = { Text("ویرایش حساب خانواده") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("نام خانواده") }, singleLine = true)
                    OutlinedTextField(father, { father = it }, label = { Text("پدر") }, singleLine = true)
                    OutlinedTextField(mother, { mother = it }, label = { Text("مادر") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.updateFamily(name, father, mother); editFamily = false }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { editFamily = false }) { Text("انصراف") } },
        )
    }
    if (changePin) {
        var old by remember { mutableStateOf("") }
        var new by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { changePin = false },
            title = { Text("تغییر رمز والدین") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(old, { old = com.kodomo.app.core.util.PersianDigits.digitsOnly(it).take(4) }, label = { Text("رمز فعلی") }, singleLine = true)
                    OutlinedTextField(new, { new = com.kodomo.app.core.util.PersianDigits.digitsOnly(it).take(4) }, label = { Text("رمز جدید ۴ رقمی") }, singleLine = true)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = old.length == 4 && new.length == 4, onClick = {
                    viewModel.changePin(old, new) { ok -> if (ok) changePin = false else error = "رمز فعلی درست نیست." }
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { changePin = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun MemberRow(avatar: AvatarKind, title: String, subtitle: String, onClick: (() -> Unit)?) {
    KodomoCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KodomoAvatar(avatar, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null)
        }
    }
}

@HiltViewModel
class ChildEditorViewModel @Inject constructor(private val familyRepository: FamilyRepository) : ViewModel() {
    var original: Child? = null
        private set

    suspend fun load(id: Long): ChildDraft? {
        val child = familyRepository.getChild(id) ?: return null
        original = child
        return ChildDraft(child.name, child.birthDate, child.gender, child.avatar, child.interests, child.talents, child.abilities)
    }

    fun save(draft: ChildDraft, onDone: () -> Unit) = viewModelScope.launch {
        val existing = original
        if (existing == null) {
            familyRepository.addChild(draft, ParentalSettings.defaults(0).dailyLimitMinutes)
        } else {
            familyRepository.updateChild(
                existing.copy(
                    name = draft.name, birthDate = draft.birthDate ?: existing.birthDate, gender = draft.gender, avatar = draft.avatar,
                    interests = draft.interests, talents = draft.talents, abilities = draft.abilities,
                ),
            )
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        original?.let { familyRepository.deleteChild(it.id) }
        onDone()
    }
}

@Composable
fun ChildEditorScreen(childId: Long?, onBack: () -> Unit, viewModel: ChildEditorViewModel = hiltViewModel()) {
    var draft by remember { mutableStateOf(ChildDraft()) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(childId) { if (childId != null) viewModel.load(childId)?.let { draft = it } }

    KidScreenScaffold(
        title = if (childId == null) "افزودن فرزند" else "پروفایل ${draft.name}",
        onBack = onBack,
        actions = {
            if (childId != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, contentDescription = "حذف") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            KodomoAvatar(draft.avatar, size = 96.dp)
            ChildProfileForm(draft = draft, onChange = { draft = it }, modifier = Modifier.padding(vertical = 12.dp))
            KodomoPrimaryButton("ذخیره", onClick = { viewModel.save(draft, onBack) }, enabled = draft.isValid)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف پروفایل") },
            text = { Text("تمام سوابق، تنظیمات و خاطرات این فرزند حذف می‌شود. مطمئن هستید؟") },
            confirmButton = { TextButton(onClick = { viewModel.delete(onBack) }) { Text("حذف", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } },
        )
    }
}

@HiltViewModel
class MemoriesViewModel @Inject constructor(private val memoryRepository: MemoryRepository, familyRepository: FamilyRepository) : ViewModel() {
    val memories: StateFlow<List<MemoryPhoto>> = memoryRepository.observeMemories(null).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val children: StateFlow<List<Child>> = familyRepository.observeChildren().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun bitmap(memory: MemoryPhoto): ImageBitmap? = withContext(Dispatchers.Default) {
        runCatching {
            val bytes = memoryRepository.decrypt(memory)
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
        }.getOrNull()
    }

    fun delete(memory: MemoryPhoto) = viewModelScope.launch { memoryRepository.delete(memory) }

    fun import(jpeg: ByteArray, childId: Long, caption: String) = viewModelScope.launch {
        memoryRepository.saveJpeg(childId, jpeg, caption.ifBlank { "خاطره" }, MemorySource.MANUAL)
    }
}

/** Parents-only, encrypted memories gallery. Images are decrypted in memory only while displayed. */
@Composable
fun MemoriesGalleryScreen(onBack: () -> Unit, viewModel: MemoriesViewModel = hiltViewModel()) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<MemoryPhoto?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var picked by remember { mutableStateOf<ByteArray?>(null) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { picked = com.kodomo.app.core.util.importImageAsJpeg(context, uri) }
    }
    KidScreenScaffold(title = "گالری محرمانه‌ی خاطرات", onBack = onBack, actions = {
        TextButton(onClick = {
            picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text("➕ از گالری") }
    }) { padding ->
        if (memories.isEmpty()) {
            EmptyState("هنوز خاطره‌ای ثبت نشده. عکس‌های پروژه‌ها اینجا ذخیره می‌شوند؛ با «➕ از گالری» هم می‌توانید عکس اضافه کنید.", Modifier.padding(padding))
            return@KidScreenScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(memories, key = { it.id }) { memory ->
                val image by produceState<ImageBitmap?>(null, memory.id) { value = viewModel.bitmap(memory) }
                Column(Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        image?.let {
                            Image(it, contentDescription = memory.caption, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().let { m -> m })
                        }
                    }
                    Column(Modifier.padding(8.dp)) {
                        Text(memory.caption, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        Text(
                            "${children.firstOrNull { it.id == memory.childId }?.name.orEmpty()} • ${PersianCalendar.format(memory.createdAt.atZone(ZoneId.systemDefault()).toLocalDate())}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        TextButton(onClick = { open = memory }) { Text("مشاهده") }
                    }
                }
            }
        }
    }
    picked?.let { jpeg ->
        var caption by remember { mutableStateOf("") }
        var childId by remember { mutableStateOf(children.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { picked = null },
            title = { Text("افزودن به خاطرات") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = caption, onValueChange = { caption = it.take(80) }, label = { Text("توضیح") })
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(children.size) { i ->
                            val c = children[i]
                            androidx.compose.material3.FilterChip(selected = childId == c.id, onClick = { childId = c.id }, label = { Text(c.name) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = childId != null, onClick = {
                    childId?.let { viewModel.import(jpeg, it, caption) }
                    picked = null
                }) { Text("ذخیره (رمزنگاری‌شده)") }
            },
            dismissButton = { TextButton(onClick = { picked = null }) { Text("انصراف") } },
        )
    }
    open?.let { memory ->
        val image by produceState<ImageBitmap?>(null, memory.id) { value = viewModel.bitmap(memory) }
        AlertDialog(
            onDismissRequest = { open = null },
            text = { image?.let { Image(it, contentDescription = memory.caption, modifier = Modifier.fillMaxWidth()) } },
            confirmButton = { TextButton(onClick = { open = null }) { Text("بستن") } },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.delete(memory)
                    open = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

/** Shows the last crash (if any) so the parent can copy it and send it to the developers. */
@Composable
private fun CrashReportCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var report by remember { mutableStateOf(com.kodomo.app.core.CrashReporter.lastCrash(context)) }
    val text = report ?: return
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    KodomoCard(Modifier.fillMaxWidth()) {
        SectionTitle("گزارش آخرین خطای برنامه")
        Text("کدومو قبلاً با خطا بسته شده است. متن زیر را کپی کنید و برای پشتیبانی بفرستید.", style = MaterialTheme.typography.bodySmall)
        Text(text.take(1_500), style = MaterialTheme.typography.labelSmall, maxLines = 14)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(text)) }) { Text("📋 کپی گزارش") }
            TextButton(onClick = {
                com.kodomo.app.core.CrashReporter.clear(context)
                report = null
            }) { Text("حذف") }
        }
    }
}

/** The parent's way out of Kodomo: back to the phone's own launcher, optionally changing the default Home app. */
@Composable
private fun ExitAppCard() {
    val kiosk = com.kodomo.app.kiosk.LocalKioskController.current
    var confirm by remember { mutableStateOf(false) }
    KodomoCard(Modifier.fillMaxWidth()) {
        Text("🚪 خروج از کدومو", style = MaterialTheme.typography.titleMedium)
        Text(
            "برنامه بسته می‌شود و صفحه‌ی اصلی گوشی باز می‌شود. اگر کدومو لانچر پیش‌فرض است و می‌خواهید بعد از خاموش و روشن شدن هم گوشی عادی بالا بیاید، لانچر پیش‌فرض را عوض کنید.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(onClick = { confirm = true }, modifier = Modifier.weight(1f)) { Text("خروج از برنامه") }
            OutlinedButton(onClick = { kiosk.openDefaultHomeSettings() }, modifier = Modifier.weight(1f)) { Text("تغییر لانچر پیش‌فرض") }
        }
    }
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("خروج از کدومو؟") },
            text = { Text("محیط امن کودک بسته می‌شود. برای برگشت، آیکون کدومو را بزنید.") },
            confirmButton = { TextButton(onClick = { confirm = false; kiosk.exitToPhone() }) { Text("خروج") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("انصراف") } },
        )
    }
}
