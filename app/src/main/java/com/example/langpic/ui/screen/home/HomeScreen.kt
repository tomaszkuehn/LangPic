package com.example.langpic.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onPlay: () -> Unit,
    onImport: () -> Unit,
    onManage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "LangPic",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(64.dp))

        HomeButton(
            text = "Learn",
            icon = { Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(36.dp)) },
            onClick = onPlay,
        )

        Spacer(modifier = Modifier.height(24.dp))

        HomeButton(
            text = "Import Pack",
            icon = { Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(36.dp)) },
            onClick = onImport,
        )

        Spacer(modifier = Modifier.height(24.dp))

        HomeButton(
            text = "Manage Packs",
            icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(36.dp)) },
            onClick = onManage,
        )
    }
}

@Composable
private fun HomeButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        icon()
        Spacer(modifier = Modifier.size(12.dp))
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
