package com.fitness_planet_gym

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminWorkoutManagementActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var categoryInput: EditText
    private lateinit var levelInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var exercisesInput: EditText
    private lateinit var descriptionInput: EditText
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_workout_management)
        titleInput=findViewById(R.id.workoutTitle)
        categoryInput=findViewById(R.id.workoutCategory)
        levelInput=findViewById(R.id.workoutLevel)
        durationInput=findViewById(R.id.workoutDuration)
        exercisesInput=findViewById(R.id.workoutExercises)
        descriptionInput=findViewById(R.id.workoutDescription)
        container=findViewById(R.id.workoutContainer)
        findViewById<Button>(R.id.saveWorkoutButton).setOnClickListener { saveWorkout() }
        findViewById<Button>(R.id.clearWorkoutButton).setOnClickListener { clearForm() }
        loadWorkouts()
    }

    private fun saveWorkout() {
        val title=titleInput.text.toString().trim()
        val category=categoryInput.text.toString().trim()
        val level=levelInput.text.toString().trim()
        val duration=durationInput.text.toString().trim()
        val exercises=exercisesInput.text.toString().trim()
        val description=descriptionInput.text.toString().trim()
        if(title.isBlank()||category.isBlank()||level.isBlank()||duration.isBlank()||exercises.isBlank()){
            Toast.makeText(this,"Fill title, category, level, duration and exercises",Toast.LENGTH_SHORT).show()
            return
        }
        val data=hashMapOf(
            "title" to title,"category" to category,"level" to level,
            "duration" to duration,"exercises" to exercises,
            "description" to description,"updatedAt" to System.currentTimeMillis()
        )
        val task=editingId?.let { db.collection("workouts").document(it).set(data) }
            ?: db.collection("workouts").add(data)
        task.addOnSuccessListener {
            Toast.makeText(this,if(editingId==null)"Workout added" else "Workout updated",Toast.LENGTH_SHORT).show()
            clearForm(); loadWorkouts()
        }.addOnFailureListener { Toast.makeText(this,"Save failed: " + it.message,Toast.LENGTH_LONG).show() }
    }

    private fun loadWorkouts() {
        db.collection("workouts").get().addOnSuccessListener { snap ->
            container.removeAllViews()
            snap.documents.sortedBy { it.getString("title").orEmpty().lowercase() }.forEach { doc ->
                val card=TextView(this).apply {
                    text=doc.getString("title").orEmpty() + "\n" +
                        doc.getString("category").orEmpty() + " • " +
                        doc.getString("level").orEmpty() + " • " +
                        doc.getString("duration").orEmpty() + "\nExercises: " +
                        doc.getString("exercises").orEmpty()
                    textSize=15f
                    setTextColor(resources.getColor(R.color.fitness_text,theme))
                    setPadding(18,18,18,18)
                    setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
                    setOnClickListener { editWorkout(doc.id) }
                }
                container.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=10})
            }
        }.addOnFailureListener { Toast.makeText(this,"Could not load workouts",Toast.LENGTH_LONG).show() }
    }

    private fun editWorkout(id:String) {
        db.collection("workouts").document(id).get().addOnSuccessListener { d ->
            editingId=id
            titleInput.setText(d.getString("title").orEmpty())
            categoryInput.setText(d.getString("category").orEmpty())
            levelInput.setText(d.getString("level").orEmpty())
            durationInput.setText(d.getString("duration").orEmpty())
            exercisesInput.setText(d.getString("exercises").orEmpty())
            descriptionInput.setText(d.getString("description").orEmpty())
            findViewById<Button>(R.id.saveWorkoutButton).text="UPDATE WORKOUT"
        }
    }

    private fun clearForm() {
        editingId=null
        titleInput.text.clear(); categoryInput.text.clear(); levelInput.text.clear()
        durationInput.text.clear(); exercisesInput.text.clear(); descriptionInput.text.clear()
        findViewById<Button>(R.id.saveWorkoutButton).text="SAVE WORKOUT"
    }
}