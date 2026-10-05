package com.koude.aurora.ui.misc

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCrashedScreen(logs: String) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(DesignR.string.application_crashed), fontWeight = FontWeight.Medium) }) }) { insets ->
        SelectionContainer {
            Text(
                logs,
                modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkBrokenScreen(releasesUrl: String, onOpenReleases: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(DesignR.string.application_broken), fontWeight = FontWeight.Medium) }) }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(stringResource(DesignR.string.application_broken_tips), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { onOpenReleases(releasesUrl) }, modifier = Modifier.padding(top = 20.dp)) {
                Text(stringResource(DesignR.string.github_releases))
            }
        }
    }
}
