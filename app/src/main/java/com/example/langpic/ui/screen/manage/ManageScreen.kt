package com.example.langpic.ui.screen.manage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langpic.data.repository.LessonRepository
import com.example.langpic.domain.model.LessonPack
import com.example.langpic.ui.theme.Blue400
import com.example.langpic.ui.theme.CreamBg
import com.example.langpic.ui.theme.DarkText
import com.example.langpic.ui.theme.ErrorRed
import com.example.langpic.ui.theme.GradientPlayful
import com.example.langpic.ui.theme.MediumText
import com.example.langpic.ui.theme.Orange100
import com.example.langpic.ui.theme.Orange500
import com.example.langpic.ui.theme.Pink400
import com.example.langpic.ui.theme.Purple400
import com.example.langpic.ui.theme.SuccessGreen
import com.example.langpic.ui.theme.SurfaceWhite
import com.example.langpic.ui.theme.Teal400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    viewModel: ManageViewModel,
    repository: LessonRepository,
    onBack: () -> Unit,
) {
    val packs by viewModel.packs.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadPacks(repository) }

    Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = { Text("Manage Packs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
            )
        },
    ) { padding ->
        if (packs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No lesson packs yet.", fontSize = 18.sp, color = MediumText, fontWeight = FontWeight.Medium)
                    Text("Import one to get started!", fontSize = 15.sp, color = MediumText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(items = packs, key = { it.id }) { pack ->
                    PackCard(
                        pack = pack,
                        onToggle = { enabled -> viewModel.togglePack(pack.id, enabled) },
                        onDelete = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    File(pack.extractedPath).deleteRecursively()
                                }
                                viewModel.deletePack(pack.id)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun PackCard(
    pack: LessonPack,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    // Pick a gradient based on language for visual variety
    val accentColors = when {
        pack.language.contains("ja") -> listOf(ErrorRed, Pink400)
        pack.language.contains("pl") -> listOf(Purple400, Blue400)
        else -> listOf(Orange500, Color(0xFFFF7043))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colorful accent bar on the left
            Box(
                modifier = Modifier
                    .width(5.dp).height(56.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(accentColors)),
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pack.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
                    if (!pack.enabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MediumText.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) { Text("off", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MediumText) }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape).background(Orange500)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${pack.language.ifEmpty { "—" }}  ·  ${pack.itemCount} word${if (pack.itemCount > 1) "s" else ""}",
                        fontSize = 13.sp, color = MediumText,
                    )
                }
                if (pack.highScore > 0f) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("🏆 Best: ${"%.1f".format(pack.highScore)}", fontSize = 12.sp, color = Orange500, fontWeight = FontWeight.SemiBold)
                }
            }

            // Enabled toggle
            Switch(
                checked = pack.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SuccessGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MediumText.copy(alpha = 0.3f),
                ),
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Delete button
            IconButton(onClick = onDelete) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(ErrorRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) { Text("🗑️", fontSize = 16.sp) }
            }
        }
    }
}
