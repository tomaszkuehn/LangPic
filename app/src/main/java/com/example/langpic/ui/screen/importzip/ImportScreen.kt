package com.example.langpic.ui.screen.importzip

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langpic.data.repository.LessonRepository
import com.example.langpic.service.ZipImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    repository: LessonRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.init(repository)
    }

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
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val extractResult = ZipImporter.import(context, tempFile.absolutePath)
                    tempFile.delete()

                    extractResult.fold(
                        onSuccess = { importResult ->
                            val exists = repository.countByTitle(importResult.title) > 0
                            if (exists) {
                                viewModel.showDuplicateWarning(importResult.title, importResult)
                            } else {
                                repository.insertPack(
                                    title = importResult.title,
                                    language = importResult.language,
                                    extractedPath = importResult.extractedPath,
                                    items = importResult.items,
                                )
                                viewModel.setResult(importResult)
                            }
                        },
                        onFailure = { e ->
                            viewModel.setError(e.message ?: "Import failed")
                        },
                    )
                } catch (e: Exception) {
                    viewModel.setError(e.message ?: "Import failed")
                }
            }
        }
    }

    // Duplicate warning dialog
    if (state.warningTitle != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDuplicate() },
            title = { Text("Already Imported") },
            text = {
                Text("\"${state.warningTitle}\" is already in your library. Import it again?")
            },
            confirmButton = {
                TextButton(onClick = {
                    val pending = viewModel.consumePendingResult()
                    if (pending != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.insertPack(
                                    title = pending.title,
                                    language = pending.language,
                                    extractedPath = pending.extractedPath,
                                    items = pending.items,
                                )
                            }
                            viewModel.setResult(pending)
                        }
                    }
                }) {
                    Text("Import Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDuplicate() }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Pack") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Importing...", fontSize = 18.sp)
                        }
                    }
                }

                state.result != null -> {
                    val result = state.result!!
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(32.dp),
                    )
                    Text(
                        "Imported!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("${result.title}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${result.language} — ${result.itemCount} words", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.clearResult() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Import Another", fontSize = 20.sp)
                    }
                }

                state.error != null -> {
                    Text(
                        "Import Failed",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.error!!, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.clearResult() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Try Again", fontSize = 20.sp)
                    }
                }

                else -> {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        "Select a lesson pack ZIP file to import",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { filePicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text(
                            "Choose ZIP File",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
        }
    }
}
