package com.petcare.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.local.UserDao
import com.petcare.app.data.model.UserEntity
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao: UserDao = AppDatabase.getDatabase(application).userDao()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _authError = MutableLiveData<String?>()
    val authError: LiveData<String?> = _authError
    private val _isLoggedIn = MutableLiveData(false)
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun login(email: String, password: String) {
        _isLoading.value = true
        if (email.isNotEmpty() && password.isNotEmpty()) {
            viewModelScope.launch {
                val user = UserEntity(
                    id = 0,
                    name = email.substringBefore("@"),
                    email = email,
                    passwordHash = password,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(user)
                _isLoggedIn.value = true
                _isLoading.value = false
            }
        } else {
            _authError.value = "Please enter email and password"
            _isLoading.value = false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
    }
}