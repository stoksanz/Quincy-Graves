package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivityEntity
import com.example.data.model.FamilyEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserFlowByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE familyCode = :familyCode")
    fun getUsersByFamily(familyCode: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface FamilyDao {
    @Query("SELECT * FROM families WHERE code = :code LIMIT 1")
    suspend fun getFamilyByCode(code: String): FamilyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: FamilyEntity)

    @Query("DELETE FROM families WHERE code = :code")
    suspend fun deleteFamily(code: String)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE familyCode = :familyCode ORDER BY timestamp DESC LIMIT 30")
    fun getActivitiesByFamily(familyCode: String): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)
}
