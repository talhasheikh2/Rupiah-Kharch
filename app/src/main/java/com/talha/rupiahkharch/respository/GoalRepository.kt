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

    // Function to get goals for a specific user
    fun getAllGoals(userId: String): LiveData<List<SavingsGoal>> {
        return goalDao.getAllGoals(userId)
    }

    /**
     * @param isFromCloud If true, only saves to Room (used during Sync).
     * If false, saves to Room AND uploads to Firestore (used for new goals).
     */
    suspend fun insert(goal: SavingsGoal, isFromCloud: Boolean = false) {
        val userId = auth.currentUser?.uid ?: return

        // Ensure the goal is tagged with the current user's ID
        val goalWithUser = goal.copy(userId = userId)

        // 1. Save to Room
        val newId = goalDao.insertGoal(goalWithUser)

        // 2. Mirror to Cloud only if it's a new manual entry
        if (!isFromCloud) {
            val updatedGoal = goalWithUser.copy(id = newId.toInt())
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("goals")
                    .document(newId.toString())
                    .set(updatedGoal)
                    .await()
            } catch (e: Exception) {
                // Offline handling is automatic with Firestore
            }
        }
    }

    suspend fun update(goal: SavingsGoal) {
        val userId = auth.currentUser?.uid ?: return
        goalDao.updateGoal(goal)

        try {
            firestore.collection("users")
                .document(userId)
                .collection("goals")
                .document(goal.id.toString())
                .set(goal)
                .await()
        } catch (e: Exception) {}
    }

    suspend fun delete(goalId: Int) {
        val userId = auth.currentUser?.uid ?: return

        // 1. Delete from Room
        goalDao.deleteGoal(goalId)

        // 2. Delete from Cloud
        try {
            firestore.collection("users")
                .document(userId)
                .collection("goals")
                .document(goalId.toString())
                .delete()
                .await()
        } catch (e: Exception) {}
    }

    /**
     * Clears local goals and resets ID counter.
     * Call this during logout.
     */
    suspend fun deleteAll() {
        goalDao.deleteAll()
        goalDao.resetIdCounter()
    }
}