package com.example.explive

import Concierto
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MenuAdmin : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var adapter: ArrayAdapter<String>
    private val nombresConciertos = mutableListOf<String>()
    private val conciertosMap = mutableMapOf<Int, Concierto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_admin)

        database = FirebaseDatabase.getInstance().reference.child("conciertos")

        val listaConciertos = findViewById<ListView>(R.id.listViewAdmon)
        val btnAgregar = findViewById<Button>(R.id.btnAgregar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        var auth = FirebaseAuth.getInstance()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nombresConciertos)
        listaConciertos.adapter = adapter

        btnAgregar.setOnClickListener {
            val intent = Intent(this, AgregarConcierto::class.java)
            startActivity(intent)
        }

        btnEliminar.setOnClickListener {
            val intent = Intent(this, EliminarConcierto::class.java)
            startActivity(intent)
        }

        listaConciertos.setOnItemClickListener { parent, view, position, id ->
            val conciertoId = conciertosMap.keys.elementAt(position)
            val concierto = conciertosMap[conciertoId]
            Log.d("Anterior", "concierto: ${conciertosMap[conciertoId]}")
            Log.d("Actual", "id: $conciertoId")
            Log.d("Siguiente", "concierto: ${conciertosMap[conciertoId + 1]}")
            val intent = Intent(this, DetallesConcierto::class.java)
            intent.putExtra("id", conciertoId.toString())
            Log.d("MenuAdmin", "id: $conciertoId")
            intent.putExtra("concierto", concierto)
            startActivity(intent)
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        cargarConciertos()
    }

    private fun cargarConciertos() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                nombresConciertos.clear()
                conciertosMap.clear()
                for (conciertoSnapshot in snapshot.children) {
                    val id = conciertoSnapshot.key?.toIntOrNull() ?: continue
                    val concierto = conciertoSnapshot.getValue(Concierto::class.java) ?: continue
                    conciertosMap[id] = concierto
                    nombresConciertos.add("${concierto.artista} - ${concierto.ciudad}")
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MenuAdmin, "Error al cargar conciertos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
