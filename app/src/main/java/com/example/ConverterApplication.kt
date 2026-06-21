package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ConversionRepository

class ConverterApplication : Application() {
    lateinit var database: AppDatabase
        private set
    
    lateinit var repository: ConversionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "converter_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        
        repository = ConversionRepository(database.conversionDao())
    }
}
