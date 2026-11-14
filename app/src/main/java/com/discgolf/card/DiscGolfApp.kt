package com.discgolf.card

import android.app.Application
import com.discgolf.card.data.AppDatabase
import com.discgolf.card.data.FileTransfer
import com.discgolf.card.data.RoundRepository

class DiscGolfApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }

    val repository: RoundRepository by lazy { RoundRepository(database.roundDao()) }

    val fileTransfer: FileTransfer by lazy { FileTransfer(this, repository) }
}
