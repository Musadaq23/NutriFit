package com.example.nutrifit.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nutrifit.MealEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MealsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _meals = MutableLiveData<List<MealEntity>>(emptyList())
    val meals: LiveData<List<MealEntity>> = _meals

    private var listenerRegistration: ListenerRegistration? = null

    init {
        val user = auth.currentUser
        if (user != null) {
            startListening(user.uid)
        } else {
            _meals.value = emptyList()
        }
    }

    private fun startListening(uid: String) {
        listenerRegistration?.remove()

        listenerRegistration = db.collection("users")
            .document(uid)
            .collection("meals")
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val dateTime = doc.getString("dateTime") ?: return@mapNotNull null
                    val mealType = doc.getString("mealType") ?: return@mapNotNull null
                    val calories = doc.getLong("calories")?.toInt() ?: 0
                    val protein = doc.getLong("protein")?.toInt() ?: 0
                    val carbs = doc.getLong("carbs")?.toInt() ?: 0
                    val fats = doc.getLong("fats")?.toInt() ?: 0
                    val notes = doc.getString("notes")

                    MealEntity(
                        id = doc.id,
                        dateTime = dateTime,
                        mealType = mealType,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fats = fats,
                        notes = notes
                    )
                }

                _meals.value = list
            }
    }

    fun addMeal(meal: MealEntity) {
        val user = auth.currentUser ?: return

        val data = hashMapOf(
            "dateTime" to meal.dateTime,
            "mealType" to meal.mealType,
            "calories" to meal.calories,
            "protein" to meal.protein,
            "carbs" to meal.carbs,
            "fats" to meal.fats,
            "notes" to meal.notes
        )

        db.collection("users")
            .document(user.uid)
            .collection("meals")
            .add(data)
    }

    fun updateMeal(meal: MealEntity) {
        val user = auth.currentUser ?: return
        if (meal.id.isBlank()) return

        val data = hashMapOf(
            "dateTime" to meal.dateTime,
            "mealType" to meal.mealType,
            "calories" to meal.calories,
            "protein" to meal.protein,
            "carbs" to meal.carbs,
            "fats" to meal.fats,
            "notes" to meal.notes
        )

        db.collection("users")
            .document(user.uid)
            .collection("meals")
            .document(meal.id)
            .set(data)
    }

    fun deleteMeal(meal: MealEntity) {
        val user = auth.currentUser ?: return
        if (meal.id.isBlank()) return

        db.collection("users")
            .document(user.uid)
            .collection("meals")
            .document(meal.id)
            .delete()
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
