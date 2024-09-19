package com.example.explive

import Concierto
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.example.explive.databinding.ActivityEliminarConciertoBinding
import com.google.firebase.ktx.Firebase

class EliminarConcierto : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var binding: ActivityEliminarConciertoBinding

    private val conciertosList = mutableListOf<Concierto>()
    private val conciertosNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar ViewBinding
        binding = ActivityEliminarConciertoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = Firebase.database.reference.child("conciertos")

        cargarConciertos()

        binding.eliminar.setOnClickListener {
            val position = binding.spinner.selectedItemPosition
            if (position != AdapterView.INVALID_POSITION) {
                val conciertoSeleccionado = conciertosList[position]
                eliminarConcierto(conciertoSeleccionado)
            } else {
                Toast.makeText(this, "Por favor, seleccione un concierto", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarConciertos() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                conciertosList.clear()
                conciertosNames.clear()
                for (conciertoSnapshot in snapshot.children) {
                    val concierto = conciertoSnapshot.getValue(Concierto::class.java)
                    if (concierto != null) {
                        conciertosList.add(concierto)
                        conciertosNames.add("${concierto.artista} - ${concierto.ciudad}")
                    }
                }
                val adapter = ArrayAdapter(this@EliminarConcierto, R.layout.spinner_item, conciertosNames)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                binding.spinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EliminarConcierto, "Error al cargar los conciertos", Toast.LENGTH_SHORT).show()
            }
        })

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val concierto = conciertosList[position]
                binding.artista.text = concierto.artista
                binding.fechaConcierto.text = concierto.fecha
                binding.ciudadConcierto.text = concierto.ciudad
                binding.lugarConcierto.text = concierto.centro_de_eventos
                binding.horaConcierto.text = concierto.hora
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No se seleccionó nada
            }
        }
    }

    private fun eliminarConcierto(concierto: Concierto) {
        val conciertoRef = database.orderByChild("artista").equalTo(concierto.artista)
        conciertoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (conciertoSnapshot in snapshot.children) {
                    conciertoSnapshot.ref.removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@EliminarConcierto, "Evento eliminado", Toast.LENGTH_SHORT).show()
                            cargarConciertos()  // Recargar la lista de conciertos
                        } else {
                            Toast.makeText(this@EliminarConcierto, "Error al eliminar el evento: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EliminarConcierto, "Error al acceder a la base de datos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
