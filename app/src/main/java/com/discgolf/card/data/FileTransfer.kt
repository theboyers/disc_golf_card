package com.discgolf.card.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileTransfer(context: Context, private val repository: RoundRepository) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    data class ShareFile(val file: File, val roundCount: Int)

    /** Writes the same JSON backup to cache for an ACTION_SEND share (no document picker). */
    suspend fun prepareShareFile(fileName: String): Result<ShareFile> = withContext(Dispatchers.IO) {
        runCatching {
            val rounds = repository.exportAll()
            val directory = File(appContext.cacheDir, "share").apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val file = File(directory, fileName)
            file.writeText(BackupJson.encode(rounds), Charsets.UTF_8)
            ShareFile(file, rounds.size)
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
