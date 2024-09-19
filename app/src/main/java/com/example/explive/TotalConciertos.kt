package com.example.explive

import Concierto
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TotalConciertos : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var listaConciertos: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val conciertosList = mutableListOf<Concierto>()
    private val nombresConciertos = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_total_conciertos)

        database = Firebase.database.reference.child("conciertos")
        listaConciertos = findViewById(R.id.listView1)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresConciertos)
        listaConciertos.adapter = adapter

        val nombreUsuario = intent.getStringExtra("nombreUsuario") ?: "Usuario"

        val textViewSaludo = findViewById<TextView>(R.id.eventTitle)
        textViewSaludo.text = "Estos son todos los conciertos disponibles:"

        listaConciertos.setOnItemClickListener { parent, view, position, id ->
            val concierto = conciertosList[position]
            val intent = Intent(this, DetallesConcierto::class.java)
            intent.putExtra("id", (position+1).toString())
            intent.putExtra("concierto", concierto)
            startActivity(intent)
        }

        cargarConciertos()
    }

    private fun cargarConciertos() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                conciertosList.clear()
                nombresConciertos.clear()
                for (conciertoSnapshot in dataSnapshot.children) {
                    val concierto = conciertoSnapshot.getValue(Concierto::class.java)
                    if (concierto != null) {
                        conciertosList.add(concierto)
                        nombresConciertos.add("${concierto.artista} - ${concierto.ciudad}")
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error aquí
            }
        })
    }
}
