package com.talha.rupiahkharch.repository

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.rupiahkharch.model.GoalDao
import com.talha.rupiahkharch.model.SavingsGoal
import kotlinx.coroutines.tasks.await

class GoalRepository(private val goalDao: GoalDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val allGoals: LiveData<List<SavingsGoal>> = goalDao.getAllGoals()

    suspend fun insert(goal: SavingsGoal) {
        // 1. Save to Room and get the generated ID
        val newId = goalDao.insertGoal(goal)

        // 2. Create a copy with the correct ID for Cloud
        val updatedGoal = goal.copy(id = newId.toInt())

        // 3. Mirror to Cloud Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("goals")
                    .document(newId.toString())
                    .set(updatedGoal)
                    .await()
            } catch (e: Exception) {
                // Firestore handles offline queueing automatically
            }
        }
    }

    suspend fun update(goal: SavingsGoal) {
        goalDao.updateGoal(goal)

        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("goals").document(goal.id.toString())
                .set(goal)
        }
    }

    suspend fun delete(goalId: Int) {
        // 1. Delete from Room
        goalDao.deleteGoal(goalId)

        // 2. Delete from Cloud
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("goals").document(goalId.toString())
                .delete()
        }
    }
}