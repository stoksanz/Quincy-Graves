package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.ActivityDao
import com.example.data.local.AppDatabase
import com.example.data.local.FamilyDao
import com.example.data.local.UserDao
import com.example.data.model.ActivityEntity
import com.example.data.model.FamilyEntity
import com.example.data.model.UserEntity
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseSignUpRequest
import com.example.data.remote.SupabaseSignInRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.random.Random

class GuardianRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val familyDao: FamilyDao,
    private val activityDao: ActivityDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("guardian_prefs", Context.MODE_PRIVATE)
    
    private val _currentUserEmail = MutableStateFlow<String?>(prefs.getString("current_user_email", null))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = _currentUserEmail.flatMapLatest { email ->
        if (email != null) {
            userDao.getUserFlowByEmail(email)
        } else {
            flowOf(null)
        }
    }

    suspend fun registerUser(user: UserEntity): Boolean {
        // 1. Supabase Register
        if (SupabaseClient.isConfigured()) {
            try {
                val response = SupabaseClient.authService?.signUp(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    request = SupabaseSignUpRequest(
                        email = user.email,
                        password = user.password,
                        data = mapOf("name" to user.name)
                    )
                )
                if (response != null && response.isSuccessful) {
                    val token = response.body()?.access_token
                    if (token != null) {
                        prefs.edit().putString("supabase_access_token", token).apply()
                    }
                    
                    // Insert profile into database table
                    val authHeader = SupabaseClient.getBearerToken(token)
                    SupabaseClient.databaseService?.insertUser(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authHeader = authHeader,
                        user = user
                    )
                    Log.d("GuardianRepository", "Registered and inserted user into Supabase")
                } else {
                    Log.e("GuardianRepository", "Supabase SignUp failed: ${response?.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Supabase SignUp Exception", e)
            }
        }

        // 2. Local database fallback
        val existing = userDao.getUserByEmail(user.email)
        if (existing != null) return false
        
        userDao.insertUser(user)
        return true
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        var supabaseLoginSuccess = false
        var fetchedUser: UserEntity? = null

        if (SupabaseClient.isConfigured()) {
            try {
                val response = SupabaseClient.authService?.signIn(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    request = SupabaseSignInRequest(email = email, password = password)
                )
                if (response != null && response.isSuccessful) {
                    val token = response.body()?.access_token
                    if (token != null) {
                        prefs.edit().putString("supabase_access_token", token).apply()
                        supabaseLoginSuccess = true
                        
                        // Fetch the user's details from database table
                        val authHeader = SupabaseClient.getBearerToken(token)
                        val users = SupabaseClient.databaseService?.getUsers(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authHeader = authHeader,
                            select = "*",
                            emailFilter = "eq.$email"
                        )
                        if (!users.isNullOrEmpty()) {
                            fetchedUser = users.first()
                        }
                    }
                } else {
                    Log.e("GuardianRepository", "Supabase SignIn failed: ${response?.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Supabase SignIn Exception", e)
            }
        }

        // Handle profile sync or local check
        if (supabaseLoginSuccess) {
            val finalUser = fetchedUser?.copy(status = "Online") ?: UserEntity(
                email = email,
                name = email.substringBefore("@"),
                password = password,
                status = "Online"
            )
            userDao.insertUser(finalUser)
            
            // Save session
            prefs.edit().putString("current_user_email", email).apply()
            _currentUserEmail.value = email
            return true
        } else {
            // Offline/Local check as fallback
            val user = userDao.getUserByEmail(email)
            if (user != null && user.password == password) {
                // Update status to Online
                val updatedUser = user.copy(status = "Online")
                userDao.insertUser(updatedUser)
                
                // Save session
                prefs.edit().putString("current_user_email", email).apply()
                _currentUserEmail.value = email
                return true
            }
        }
        return false
    }

    suspend fun logoutUser() {
        _currentUserEmail.value?.let { email ->
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                val offlineUser = user.copy(status = "Offline")
                // Update in Room
                userDao.insertUser(offlineUser)
                
                // Update in Supabase
                if (SupabaseClient.isConfigured()) {
                    try {
                        val token = prefs.getString("supabase_access_token", "")
                        val authHeader = SupabaseClient.getBearerToken(token)
                        SupabaseClient.databaseService?.updateUser(
                            apiKey = SupabaseClient.supabaseAnonKey,
                            authHeader = authHeader,
                            emailFilter = "eq.$email",
                            fields = mapOf("status" to "Offline")
                        )
                    } catch (e: Exception) {
                        Log.e("GuardianRepository", "Failed to update offline status in Supabase on logout", e)
                    }
                }
            }
        }
        prefs.edit().remove("current_user_email").remove("supabase_access_token").apply()
        _currentUserEmail.value = null
    }

    suspend fun createFamily(familyName: String, creatorRole: String): String {
        val email = _currentUserEmail.value ?: throw IllegalStateException("No user logged in")
        val user = userDao.getUserByEmail(email) ?: throw IllegalStateException("User not found")

        // Generate custom 6 character alphanumeric code
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val randomString = (1..6)
            .map { allowedChars.random() }
            .joinToString("")
        val familyCode = "GF-$randomString"

        val family = FamilyEntity(code = familyCode, name = familyName)

        // Supabase DB insert family
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                
                SupabaseClient.databaseService?.insertFamily(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    family = family
                )
                
                // Update remote user profile
                val fields = mapOf(
                    "familyCode" to familyCode,
                    "roleInFamily" to creatorRole,
                    "status" to "Online"
                )
                SupabaseClient.databaseService?.updateUser(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    emailFilter = "eq.$email",
                    fields = fields
                )

                // Log activity in Supabase
                val act = ActivityEntity(
                    familyCode = familyCode,
                    userName = user.name,
                    description = "membuat keluarga baru \"$familyName\""
                )
                SupabaseClient.databaseService?.insertActivity(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    activity = act
                )
                Log.d("GuardianRepository", "Created family and updated user in Supabase")
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to create family in Supabase", e)
            }
        }

        // Room Local inserts
        familyDao.insertFamily(family)

        // Update user state locally
        val updatedUser = user.copy(
            familyCode = familyCode,
            roleInFamily = creatorRole,
            status = "Online"
        )
        userDao.insertUser(updatedUser)

        // Add activity locally
        val activity = ActivityEntity(
            familyCode = familyCode,
            userName = user.name,
            description = "membuat keluarga baru \"$familyName\""
        )
        activityDao.insertActivity(activity)

        return familyCode
    }

    suspend fun joinFamily(familyCode: String, memberRole: String): Boolean {
        val email = _currentUserEmail.value ?: return false
        val user = userDao.getUserByEmail(email) ?: return false

        var familyFound: FamilyEntity? = null

        // Supabase query family first
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                val families = SupabaseClient.databaseService?.getFamilies(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    codeFilter = "eq.$familyCode"
                )
                if (!families.isNullOrEmpty()) {
                    familyFound = families.first()
                    
                    // Update user in Supabase
                    val fields = mapOf(
                        "familyCode" to familyCode,
                        "roleInFamily" to memberRole,
                        "status" to "Online"
                    )
                    SupabaseClient.databaseService?.updateUser(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authHeader = authHeader,
                        emailFilter = "eq.$email",
                        fields = fields
                    )

                    // Log activity in Supabase
                    val act = ActivityEntity(
                        familyCode = familyCode,
                        userName = user.name,
                        description = "bergabung ke dalam keluarga"
                    )
                    SupabaseClient.databaseService?.insertActivity(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authHeader = authHeader,
                        activity = act
                    )
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to join family in Supabase", e)
            }
        }

        // Fallback to local family check if remote was not found or failed
        if (familyFound == null) {
            val localFamily = familyDao.getFamilyByCode(familyCode)
            if (localFamily != null) {
                familyFound = localFamily
            }
        }

        if (familyFound != null) {
            // Replicate locally
            familyDao.insertFamily(familyFound)
            val updatedUser = user.copy(
                familyCode = familyCode,
                roleInFamily = memberRole,
                status = "Online"
            )
            userDao.insertUser(updatedUser)

            val activity = ActivityEntity(
                familyCode = familyCode,
                userName = user.name,
                description = "bergabung ke dalam keluarga"
            )
            activityDao.insertActivity(activity)
            return true
        }

        return false
    }

    suspend fun leaveFamily() {
        val email = _currentUserEmail.value ?: return
        val user = userDao.getUserByEmail(email) ?: return
        val code = user.familyCode ?: return

        // Update in Supabase
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                
                // Log activity in Supabase
                val act = ActivityEntity(
                    familyCode = code,
                    userName = user.name,
                    description = "keluar dari keluarga"
                )
                SupabaseClient.databaseService?.insertActivity(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    activity = act
                )

                // Update user remote
                val fields = mapOf(
                    "familyCode" to null,
                    "roleInFamily" to null
                )
                SupabaseClient.databaseService?.updateUser(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    emailFilter = "eq.$email",
                    fields = fields
                )
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to leave family in Supabase", e)
            }
        }

        // Add activity locally before leaving
        val activity = ActivityEntity(
            familyCode = code,
            userName = user.name,
            description = "keluar dari keluarga"
        )
        activityDao.insertActivity(activity)

        // Update user locally
        val updatedUser = user.copy(
            familyCode = null,
            roleInFamily = null
        )
        userDao.insertUser(updatedUser)
    }

    suspend fun updateProfile(name: String, role: String, phone: String, emergencyContact: String) {
        val email = _currentUserEmail.value ?: return
        val user = userDao.getUserByEmail(email) ?: return
        val code = user.familyCode

        // Update in Supabase
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                val fields = mapOf(
                    "name" to name,
                    "roleInFamily" to role,
                    "phoneNumber" to phone,
                    "emergencyContact" to emergencyContact
                )
                SupabaseClient.databaseService?.updateUser(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    emailFilter = "eq.$email",
                    fields = fields
                )

                if (code != null) {
                    val act = ActivityEntity(
                        familyCode = code,
                        userName = name,
                        description = "memperbarui profil"
                    )
                    SupabaseClient.databaseService?.insertActivity(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authHeader = authHeader,
                        activity = act
                    )
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to update profile in Supabase", e)
            }
        }
        
        val updatedUser = user.copy(
            name = name,
            roleInFamily = role,
            phoneNumber = phone,
            emergencyContact = emergencyContact
        )
        userDao.insertUser(updatedUser)
        
        // Add activity logs locally if family exists
        code?.let {
            activityDao.insertActivity(
                ActivityEntity(
                    familyCode = it,
                    userName = name,
                    description = "memperbarui profil"
                )
            )
        }
    }

    suspend fun updateStatus(status: String, battery: Int, location: String) {
        val email = _currentUserEmail.value ?: return
        val user = userDao.getUserByEmail(email) ?: return
        val code = user.familyCode

        // Update in Supabase
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                val fields = mapOf(
                    "status" to status,
                    "battery" to battery,
                    "lastLocationName" to location,
                    "lastSeen" to "Baru saja"
                )
                SupabaseClient.databaseService?.updateUser(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    emailFilter = "eq.$email",
                    fields = fields
                )

                if (code != null) {
                    val act = ActivityEntity(
                        familyCode = code,
                        userName = user.name,
                        description = "mengubah status menjadi \"$status\" di $location"
                    )
                    SupabaseClient.databaseService?.insertActivity(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authHeader = authHeader,
                        activity = act
                    )
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to update status in Supabase", e)
            }
        }
        
        val updatedUser = user.copy(
            status = status,
            battery = battery,
            lastLocationName = location,
            lastSeen = "Baru saja"
        )
        userDao.insertUser(updatedUser)

        code?.let {
            activityDao.insertActivity(
                ActivityEntity(
                    familyCode = it,
                    userName = user.name,
                    description = "mengubah status menjadi \"$status\" di $location"
                )
            )
        }
    }

    suspend fun syncFamilyMembers(familyCode: String) {
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                val remoteUsers = SupabaseClient.databaseService?.getUsers(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    familyCodeFilter = "eq.$familyCode"
                )
                if (remoteUsers != null) {
                    for (remoteUser in remoteUsers) {
                        userDao.insertUser(remoteUser)
                    }
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to sync family members from Supabase", e)
            }
        }
    }

    suspend fun syncFamilyActivities(familyCode: String) {
        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                val remoteActivities = SupabaseClient.databaseService?.getActivities(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    familyCodeFilter = "eq.$familyCode"
                )
                if (remoteActivities != null) {
                    for (activity in remoteActivities) {
                        activityDao.insertActivity(activity)
                    }
                }
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to sync family activities from Supabase", e)
            }
        }
    }

    fun getFamilyMembers(familyCode: String): Flow<List<UserEntity>> {
        return userDao.getUsersByFamily(familyCode)
    }

    fun getFamilyActivities(familyCode: String): Flow<List<ActivityEntity>> {
        return activityDao.getActivitiesByFamily(familyCode)
    }

    suspend fun getFamilyByCode(code: String): FamilyEntity? {
        return familyDao.getFamilyByCode(code)
    }

    suspend fun insertActivity(description: String) {
        val email = _currentUserEmail.value ?: return
        val user = userDao.getUserByEmail(email) ?: return
        val code = user.familyCode ?: return
        
        val activity = ActivityEntity(
            familyCode = code,
            userName = user.name,
            description = description
        )

        if (SupabaseClient.isConfigured()) {
            try {
                val token = prefs.getString("supabase_access_token", "")
                val authHeader = SupabaseClient.getBearerToken(token)
                SupabaseClient.databaseService?.insertActivity(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authHeader = authHeader,
                    activity = activity
                )
            } catch (e: Exception) {
                Log.e("GuardianRepository", "Failed to insert activity in Supabase", e)
            }
        }

        activityDao.insertActivity(activity)
    }

    companion object {
        @Volatile
        private var INSTANCE: GuardianRepository? = null

        fun getRepository(context: Context): GuardianRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = GuardianRepository(
                    context.applicationContext,
                    db.userDao(),
                    db.familyDao(),
                    db.activityDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
