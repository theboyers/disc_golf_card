package com.discgolf.card.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileTransfer(context: Context, private val repository: RoundRepository) {

    private val resolver = context.applicationContext.contentResolver

    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val rounds = repository.exportAll()
            val payload = BackupJson.encode(rounds)
            val stream = resolver.openOutputStream(uri, "wt")
                ?: error("Could not open the selected file for writing")
            stream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            rounds.size
        }
    }

    suspend fun import(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Could not read the selected file")
            val file = BackupJson.decode(text)
            repository.import(file.rounds)
        }
    }
}
