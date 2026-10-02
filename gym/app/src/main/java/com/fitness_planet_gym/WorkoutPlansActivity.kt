package com.fitness_planet_gym

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class WorkoutPlansActivity : AppCompatActivity() {
    data class Workout(val title:String,val level:String,val duration:String,val exercises:Int,val description:String)
    private val workouts = listOf(
        Workout("Push • Chest & Triceps","INTERMEDIATE","45 min",6,"Build pressing strength with chest, shoulder and triceps movements."),
        Workout("Pull • Back & Biceps","INTERMEDIATE","45 min",6,"Develop your back and arm strength with controlled pulling exercises."),
        Workout("Legs • Strength","BEGINNER","50 min",7,"A complete lower-body session focused on strength and technique."),
        Workout("Full Body Starter","BEGINNER","35 min",5,"Simple full-body movements for building a consistent gym habit.")
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_plans)
        bindWorkout(R.id.workoutPush, workouts[0])
        bindWorkout(R.id.workoutPull, workouts[1])
        bindWorkout(R.id.workoutLegs, workouts[2])
        bindWorkout(R.id.workoutFullBody, workouts[3])
    }
    private fun bindWorkout(cardId:Int, workout:Workout) {
        val card=findViewById<MaterialCardView>(cardId)
        card.findViewById<TextView>(R.id.workoutTitle).text=workout.title
        card.findViewById<TextView>(R.id.workoutMeta).text="${workout.level}  •  ${workout.duration}  •  ${workout.exercises} exercises"
        card.setOnClickListener {
            startActivity(Intent(this,WorkoutDetailActivity::class.java).apply {
                putExtra("title",workout.title)
                putExtra("level",workout.level)
                putExtra("duration",workout.duration)
                putExtra("exercises",workout.exercises)
                putExtra("description",workout.description)
            })
        }
    }
}