package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ConversionRepository

class MainViewModelFactory(private val repository: ConversionRepository, private val sharedPrefs: android.content.SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, sharedPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
