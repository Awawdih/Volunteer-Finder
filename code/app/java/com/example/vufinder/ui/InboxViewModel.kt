package com.example.vufinder.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InboxViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is inbox Fragment"
    }
    val text: LiveData<String> = _text
}