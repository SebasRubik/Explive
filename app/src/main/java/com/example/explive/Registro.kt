package com.example.explive

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.explive.databinding.ActivityRegistroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class Registro : AppCompatActivity() {

    companion object {
        const val PATH_USERS = "users/"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegistroBinding
    private lateinit var myRef: DatabaseReference
    private lateinit var imageView: ImageView
    private val database = Firebase.database
    private var storage = Firebase.storage
    private lateinit var user: User


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Habilitar la flecha de volver atrás en la barra de acción
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = Firebase.auth
        user = User()

        binding.btnRegister.setOnClickListener {
            user.email = binding.editTextText4.text.toString()
            user.password = binding.editTextText5.text.toString()
            user.name = binding.editTextText.text.toString()
            user.lastname = binding.editTextText2.text.toString()
            user.city = binding.editTextText3.text.toString()

            createUser(user.email, user.password)
        }
    }

    private fun createUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        user.uid = firebaseUser.uid
                    }

                    Log.d(TAG, "${user.uid}")

                    myRef = database.getReference(PATH_USERS + user.uid)
                    myRef.setValue(user)

                    Toast.makeText(this, "createUserWithEmail:Success", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, RedirectSpotify::class.java)
                    intent.putExtra("user", user)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this, "createUserWithEmail:Failure: " + task.exception.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    task.exception?.message?.let { Log.e(TAG, it) }
                }
            }
    }

}
