package com.talha.rupiahkharch.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user_table WHERE email = :email AND password = :password LIMIT 1")
    suspend fun loginCheck(email: String, password: String): User?

    // Add this line to fix the 'isEmailRegistered' error
    @Query("SELECT * FROM user_table WHERE email = :email LIMIT 1")
    suspend fun isEmailRegistered(email: String): User?
}