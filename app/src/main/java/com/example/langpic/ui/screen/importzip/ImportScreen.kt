package com.example.langpic.ui.screen.importzip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langpic.data.repository.LessonRepository
import com.example.langpic.service.TtsHelper
import com.example.langpic.service.ZipImporter
import com.example.langpic.ui.theme.Blue400
import com.example.langpic.ui.theme.CreamBg
import com.example.langpic.ui.theme.DarkText
import com.example.langpic.ui.theme.ErrorRed
import com.example.langpic.ui.theme.ErrorRedLight
import com.example.langpic.ui.theme.GradientCool
import com.example.langpic.ui.theme.MediumText
import com.example.langpic.ui.theme.Orange100
import com.example.langpic.ui.theme.Orange500
import com.example.langpic.ui.theme.PopIn
import com.example.langpic.ui.theme.SuccessGreen
import com.example.langpic.ui.theme.SuccessGreenLight
import com.example.langpic.ui.theme.SurfaceWhite
import com.example.langpic.ui.theme.Teal400
import com.example.langpic.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    repository: LessonRepository,
    ttsHelper: TtsHelper,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.init(repository) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.setLoading()
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val tempFile = File(context.cacheDir, "import_temp.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val extractResult = ZipImporter.import(context, tempFile.absolutePath)
                    tempFile.delete()
                    extractResult.fold(
                        onSuccess = { importResult ->
                            val allLangs = importResult.items.flatMap {
                                listOf(it.language1, it.language2)
                            }.filter { it.isNotEmpty() }.distinct()
                            val missing = allLangs.filter { !ttsHelper.isLanguageAvailable(it) }
                            val exists = repository.countByTitle(importResult.title) > 0
                            if (exists) {
                                viewModel.showDuplicateWarning(importResult.title, importResult)
                            } else {
                                repository.insertPack(
                                    title = importResult.title, language = importResult.language,
                                    extractedPath = importResult.extractedPath, items = importResult.items,
                                )
                                viewModel.setResult(importResult, missing)
                            }
                        },
                        onFailure = { e -> viewModel.setError(e.message ?: "Import failed") },
                    )
                } catch (e: Exception) {
                    viewModel.setError(e.message ?: "Import failed")
                }
            }
        }
    }

    // Duplicate dialog
    if (state.warningTitle != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.cancelDuplicate() },
            title = { Text("Already Imported", fontWeight = FontWeight.Bold) },
            text = { Text("\"${state.warningTitle}\" is already in your library. Import it again?") },
            confirmButton = {
                Button(onClick = {
                    val pending = viewModel.consumePendingResult()
                    if (pending != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.insertPack(
                                    title = pending.title, language = pending.language,
                                    extractedPath = pending.extractedPath, items = pending.items,
                                )
                            }
                            viewModel.setResult(pending)
                        }
                    }
                }) { Text("Import Anyway") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.cancelDuplicate() }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = { Text("Import Pack", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Orange500)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Importing your pack...", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = DarkText)
                            Text("This will only take a moment", fontSize = 14.sp, color = MediumText)
                        }
                    }
                }

                state.result != null -> {
                    val result = state.result!!
                    PopIn {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier.size(80.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(GradientCool)),
                                contentAlignment = Alignment.Center,
                            ) { Text("✅", fontSize = 38.sp) }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Successfully Imported!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Teal400)
                            Spacer(modifier = Modifier.height(20.dp))

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(result.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(10.dp).clip(CircleShape).background(Orange500)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${result.language.ifEmpty { "—" }}  ·  ${result.itemCount} word${if (result.itemCount > 1) "s" else ""}", fontSize = 16.sp, color = MediumText)
                                    }
                                }
                            }

                            // Missing languages warning
                            if (state.missingLanguages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Orange100.copy(alpha = 0.6f)),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Text("⚠️", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Missing TTS languages", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                                            Text(state.missingLanguages.joinToString(", "), fontSize = 13.sp, color = MediumText)
                                            Text("These words won't be spoken.", fontSize = 12.sp, color = MediumText)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))
                            Button(
                                onClick = { viewModel.clearResult() },
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal400),
                            ) { Text("📦  Import Another Pack", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                state.error != null -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(ErrorRedLight),
                        contentAlignment = Alignment.Center,
                    ) { Text("😞", fontSize = 34.sp) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Import Failed", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ErrorRed)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(state.error!!, fontSize = 15.sp, color = MediumText, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.clearResult() },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange500),
                    ) { Text("Try Again", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }

                else -> {
                    Spacer(modifier = Modifier.height(56.dp))
                    Text("📂", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Add New Words", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = DarkText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select a lesson pack ZIP file", fontSize = 16.sp, color = MediumText)
                    Spacer(modifier = Modifier.height(36.dp))

                    // Drop-zone style picker button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(3.dp, Orange500.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                            .background(Orange100.copy(alpha = 0.25f))
                            .clickable { filePicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Orange500, Color(0xFFFF7043)))),
                                contentAlignment = Alignment.Center,
                            ) { Text("📁", fontSize = 28.sp) }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Tap to choose a ZIP file", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = DarkText)
                            Text("or drop it here", fontSize = 13.sp, color = MediumText)
                        }
                    }
                }
            }
        }
    }
}
