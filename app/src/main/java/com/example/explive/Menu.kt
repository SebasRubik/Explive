package com.example.explive

import Concierto
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.bumptech.glide.Glide
import com.example.explive.databinding.ActivityMenuBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class Menu : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val nombresConciertos = mutableListOf<String>()
    private val conciertosMap = mutableMapOf<Int, Concierto>()
    private val recomendacionesList = mutableListOf<Concierto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresConciertos)
        binding.listView1.adapter = adapter

        val currentUser = auth.currentUser

        if (currentUser != null) {
            loadUserProfile(currentUser.uid)
            loadConciertosFromFirebase(currentUser.uid)
        }

        binding.botonperfil.setOnClickListener {
            val intent = Intent(this, PerfilUsuario::class.java)
            startActivity(intent)
        }

        binding.listView1.setOnItemClickListener { parent, view, position, id ->
            val concierto = recomendacionesList[position]
            val intent = Intent(this, DetallesConcierto::class.java)
            intent.putExtra("id", concierto.id)
            intent.putExtra("concierto", concierto)
            startActivity(intent)
        }

        binding.todosconciertos.setOnClickListener {
            val intent = Intent(this, TotalConciertos::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserProfile(uid: String) {
        val userRef = database.child("users").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    binding.eventTitle.text = "¡Hola ${getFirstWord(user.name)}!, estos son los conciertos sugeridos para ti:"
                    user.photoUrl?.let { url ->
                        Glide.with(this@Menu)
                            .load(url)
                            .into(binding.botonperfil)
                    }
                } else {
                    Toast.makeText(this@Menu, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Menu, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadConciertosFromFirebase(uid: String) {
        val conciertosRef = database.child("conciertos")
        conciertosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                nombresConciertos.clear()
                conciertosMap.clear()
                val conciertosArray = JSONArray()
                for (conciertoSnapshot in snapshot.children) {
                    val concierto = conciertoSnapshot.getValue(Concierto::class.java)
                    if (concierto != null) {
                        val id = conciertoSnapshot.key?.toIntOrNull() ?: continue
                        conciertosMap[id] = concierto
                        nombresConciertos.add("${concierto.artista} - ${concierto.ciudad}")

                        val conciertoJson = JSONObject()
                        conciertoJson.put("id", concierto.id)
                        conciertoJson.put("artista", concierto.artista)
                        conciertoJson.put("ciudad", concierto.ciudad)
                        conciertoJson.put("centro_de_eventos", concierto.centro_de_eventos)
                        conciertoJson.put("fecha", concierto.fecha)
                        conciertoJson.put("hora", concierto.hora)
                        conciertoJson.put("generos", JSONArray(concierto.generos))
                        conciertosArray.put(conciertoJson)
                    }
                }
                adapter.notifyDataSetChanged()
                Log.d("Firebase", "Conciertos obtenidos: $conciertosArray")
                obtenerTopArtistas(uid, conciertosArray)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Menu, "Failed to load conciertos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun obtenerTopArtistas(uid: String, conciertos: JSONArray) {
        val userRef = database.child("users").child(uid).child("tokenAPISPOTIFY")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.getValue(String::class.java)
                if (token != null) {
                    fetchTopArtists(token, conciertos)
                } else {
                    Log.e("Spotify", "Token no encontrado para el usuario.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Spotify", "Error al obtener el token: ${error.message}")
            }
        })
    }

    private fun fetchTopArtists(token: String, conciertos: JSONArray) {
        val url = "https://api.spotify.com/v1/me/top/artists?time_range=medium_term&limit=10"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("Spotify", "Error al realizar la solicitud: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("Spotify", "Error en la respuesta: ${response.code}")
                    return
                }

                val responseData = response.body?.string()
                if (responseData != null) {
                    Log.d("Spotify", "Respuesta de la API: $responseData")
                    val topGenres = obtenerTopGeneros(responseData)
                    Log.d("Spotify", "Top 3 géneros: $topGenres")
                    val recomendaciones = generarRecomendaciones(topGenres, conciertos)
                    runOnUiThread {
                        actualizarListView(recomendaciones)
                    }
                } else {
                    Log.e("Spotify", "Respuesta vacía")
                }
            }
        })
    }

    private fun obtenerTopGeneros(responseData: String): List<String> {
        val jsonObject = JSONObject(responseData)
        val items = jsonObject.getJSONArray("items")
        val genreCount = mutableMapOf<String, Int>()

        for (i in 0 until items.length()) {
            val artist = items.getJSONObject(i)
            val genres = artist.getJSONArray("genres")

            for (j in 0 until genres.length()) {
                val genre = genres.getString(j).toLowerCase(Locale.getDefault())
                genreCount[genre] = genreCount.getOrDefault(genre, 0) + 1
            }
        }

        Log.d("Spotify", "Conteo de géneros: $genreCount")

        return genreCount.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }

    private fun generarRecomendaciones(topGenres: List<String>, conciertos: JSONArray): List<Concierto> {
        val recomendaciones = mutableListOf<Concierto>()
        Log.d("Spotify", "Generando recomendaciones basadas en los géneros: $topGenres")

        for (i in 0 until conciertos.length()) {
            val conciertoJson = conciertos.getJSONObject(i)
            val concierto = Concierto(
                conciertoJson.getString("id"),
                conciertoJson.getString("artista"),
                conciertoJson.getString("ciudad"),
                conciertoJson.getString("centro_de_eventos"),
                conciertoJson.getString("fecha"),
                conciertoJson.getString("hora"),
                conciertoJson.getJSONArray("generos").let { jsonArray ->
                    List(jsonArray.length()) { index -> jsonArray.getString(index).toLowerCase(Locale.getDefault()) }
                }
            )
            Log.d("Spotify", "Procesando concierto: $concierto")

            if (concierto.generos.any { it.containsAnyOf(topGenres) }) {
                Log.d("Spotify", "Añadiendo concierto a recomendaciones: ${concierto.artista} - ${concierto.ciudad}")
                recomendaciones.add(concierto)
            } else {
                Log.d("Spotify", "Concierto no relevante: ${concierto.artista} en ${concierto.ciudad}")
            }
        }

        return recomendaciones
    }

    private fun String.containsAnyOf(genres: List<String>): Boolean {
        return genres.any { this.contains(it, ignoreCase = true) }
    }

    private fun actualizarListView(recomendaciones: List<Concierto>) {
        recomendacionesList.clear()
        recomendacionesList.addAll(recomendaciones)

        val nombresRecomendaciones = recomendaciones.map { "${it.artista} - ${it.ciudad}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresRecomendaciones)
        binding.listView1.adapter = adapter
    }

    fun getFirstWord(text: String): String? {
        val words = text.split(" ")
        return if (words.isNotEmpty()) words[0] else null
    }
}
