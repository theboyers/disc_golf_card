package com.discgolf.card

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.discgolf.card.ui.AppNav
import com.discgolf.card.ui.importResultMessage
import com.discgolf.card.ui.theme.DiscGolfCardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var importMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            handleImportIntent(intent)
        }
        setContent {
            DiscGolfCardTheme {
                AppNav()
                importMessage?.let { message ->
                    AlertDialog(
                        onDismissRequest = { importMessage = null },
                        title = { Text(stringResource(R.string.import_title)) },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(onClick = { importMessage = null }) {
                                Text(stringResource(R.string.ok))
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return

        val uri = if (action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        } ?: return

        lifecycleScope.launch {
            val result = (application as DiscGolfApp).fileTransfer.import(uri)
            importMessage = result.fold(
                onSuccess = { importResultMessage(resources, it) },
                onFailure = { getString(R.string.import_failed) }
            )
        }
    }
}
