package com.example.explive

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class Spotify : AppCompatActivity() {

    private val CLIENT_ID = "92db88aa52af4ce1baaba6dc89d0df0e" // Reemplaza esto con tu Client ID
    private val REDIRECT_URI = "myapp://callback"

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spotify)

        // Inicializar Firebase Auth y Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = it.uid
            val userRef = database.child("users").child(userId).child("tokenAPISPOTIFY")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val token = snapshot.getValue(String::class.java)
                    if (token != null && token != "") {
                        // El usuario ya tiene un token registrado, redirigir a la pantalla de menú
                        val newIntent = Intent(this@Spotify, Menu::class.java)
                        startActivity(newIntent)
                        finish()
                    } else {
                        // No hay token, proceder con la autenticación de Spotify
                        iniciarAutenticacionSpotify()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Spotify", "Error al verificar el token: ${error.message}")
                    iniciarAutenticacionSpotify()
                }
            })
        } ?: run {
            iniciarAutenticacionSpotify()
        }
    }

    private fun iniciarAutenticacionSpotify() {
        val builder = AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
        builder.setScopes(arrayOf("user-top-read", "user-read-private", "user-read-email"))
        val request = builder.build()
        AuthorizationClient.openLoginInBrowser(this, request)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            val response = AuthorizationResponse.fromUri(uri)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val authToken = response.accessToken
                    Log.d("Spotify", "Auth Token: $authToken")

                    // Guarda el token en las preferencias compartidas o en una variable global para usarlo más tarde
                    val sharedPreferences = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("authToken", authToken)
                    editor.apply()

                    // Guardar el token en Firebase Realtime Database
                    val currentUser = auth.currentUser
                    currentUser?.let {
                        val userId = it.uid
                        val userRef = database.child("users").child(userId)
                        userRef.child("tokenAPISPOTIFY").setValue(authToken)
                            .addOnSuccessListener {
                                Log.d("Spotify", "Token guardado en Firebase")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Spotify", "Error al guardar el token en Firebase", e)
                            }
                    }

                    // Inicia la nueva actividad
                    val newIntent = Intent(this, Menu::class.java)
                    startActivity(newIntent)
                    finish()
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("Spotify", "Auth Error: ${response.error}")
                }
                else -> {
                    Log.w("Spotify", "Auth flow cancelled or other case.")
                }
            }
        }
    }
}
