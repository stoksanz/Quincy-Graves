package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityEntity
import com.example.data.model.FamilyEntity
import com.example.data.model.UserEntity
import com.example.data.repository.GuardianRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GuardianViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GuardianRepository.getRepository(application)

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val familyMembers: StateFlow<List<UserEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.familyCode != null) {
                repository.getFamilyMembers(user.familyCode)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val familyActivities: StateFlow<List<ActivityEntity>> = currentUser
        .flatMapLatest { user ->
            if (user?.familyCode != null) {
                repository.getFamilyActivities(user.familyCode)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentFamily = MutableStateFlow<FamilyEntity?>(null)
    val currentFamily: StateFlow<FamilyEntity?> = _currentFamily.asStateFlow()

    private val _selectedMember = MutableStateFlow<UserEntity?>(null)
    val selectedMember: StateFlow<UserEntity?> = _selectedMember.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user?.familyCode != null) {
                    val fam = repository.getFamilyByCode(user.familyCode)
                    _currentFamily.value = fam
                    // Trigger async sync from Supabase when logged in/loaded
                    launch { repository.syncFamilyMembers(user.familyCode) }
                    launch { repository.syncFamilyActivities(user.familyCode) }
                } else {
                    _currentFamily.value = null
                }
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Email dan password tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.loginUser(email.trim(), password)
                if (success) {
                    _success.value = "Berhasil masuk!"
                    onSuccess()
                } else {
                    _error.value = "Email atau password salah"
                }
            } catch (e: Throwable) {
                _error.value = "Terjadi kesalahan: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(email: String, name: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            _error.value = "Semua bidang wajib diisi"
            return
        }
        if (password.length < 6) {
            _error.value = "Password minimal 6 karakter"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newUser = UserEntity(
                    email = email.trim(),
                    name = name.trim(),
                    password = password
                )
                val success = repository.registerUser(newUser)
                if (success) {
                    _success.value = "Registrasi berhasil! Silakan login."
                    onSuccess()
                } else {
                    _error.value = "Email sudah terdaftar"
                }
            } catch (e: Throwable) {
                _error.value = "Terjadi kesalahan: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.logoutUser()
                onSuccess()
            } catch (e: Throwable) {
                _error.value = "Gagal keluar: ${e.message}"
            }
        }
    }

    fun createFamily(name: String, creatorRole: String, onSuccess: () -> Unit) {
        if (name.isBlank() || creatorRole.isBlank()) {
            _error.value = "Nama keluarga dan peran wajib diisi"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val code = repository.createFamily(name.trim(), creatorRole.trim())
                _success.value = "Keluarga berhasil dibuat dengan kode: $code"
                onSuccess()
            } catch (e: Throwable) {
                _error.value = "Gagal membuat keluarga: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinFamily(code: String, memberRole: String, onSuccess: () -> Unit) {
        if (code.isBlank() || memberRole.isBlank()) {
            _error.value = "Kode undangan dan peran wajib diisi"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.joinFamily(code.trim().uppercase(), memberRole.trim())
                if (success) {
                    _success.value = "Berhasil bergabung ke keluarga!"
                    onSuccess()
                } else {
                    _error.value = "Keluarga dengan kode tersebut tidak ditemukan"
                }
            } catch (e: Throwable) {
                _error.value = "Gagal bergabung: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leaveFamily(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.leaveFamily()
                _success.value = "Berhasil keluar dari keluarga"
                onSuccess()
            } catch (e: Throwable) {
                _error.value = "Gagal keluar keluarga: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(name: String, role: String, phone: String, emergencyContact: String) {
        if (name.isBlank() || role.isBlank()) {
            _error.value = "Nama dan peran wajib diisi"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateProfile(name.trim(), role.trim(), phone.trim(), emergencyContact.trim())
                _success.value = "Profil berhasil diperbarui"
            } catch (e: Throwable) {
                _error.value = "Gagal memperbarui profil: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStatus(status: String, battery: Int, location: String) {
        viewModelScope.launch {
            try {
                repository.updateStatus(status, battery, location)
            } catch (e: Throwable) {
                Log.e("GuardianViewModel", "Failed to update status", e)
            }
        }
    }

    fun selectMember(member: UserEntity) {
        _selectedMember.value = member
    }

    fun addManualActivity(desc: String) {
        if (desc.isBlank()) return
        viewModelScope.launch {
            try {
                repository.insertActivity(desc.trim())
                _success.value = "Aktivitas ditambahkan"
            } catch (e: Throwable) {
                Log.e("GuardianViewModel", "Failed to add activity", e)
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val user = currentUser.value
            if (user?.familyCode != null) {
                _isLoading.value = true
                try {
                    repository.syncFamilyMembers(user.familyCode)
                    repository.syncFamilyActivities(user.familyCode)
                    _success.value = "Data berhasil disinkronisasi dengan Supabase"
                } catch (e: Throwable) {
                    _error.value = "Gagal sinkronisasi: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            } else {
                _error.value = "Anda belum bergabung ke keluarga"
            }
        }
    }
}
