package com.example.explive

import Concierto
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage

class DetallesConcierto : AppCompatActivity() {

    private lateinit var concierto: Concierto
    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalles_concierto)

        concierto = intent.getParcelableExtra("concierto") ?: Concierto()
        id = intent.getStringExtra("id") ?: ""

        val nombreConcierto = findViewById<TextView>(R.id.eventTitle)
        val fechaConcierto = findViewById<TextView>(R.id.fechaConcierto)
        val ciudadConcierto = findViewById<TextView>(R.id.ciudadConcierto)
        val lugarConcierto = findViewById<TextView>(R.id.lugarConcierto)
        val horaConcierto = findViewById<TextView>(R.id.horaConcierto)
        val artistImage = findViewById<ImageView>(R.id.artistImage)
        val btnIrSitio = findViewById<Button>(R.id.irStio)
        val btnGuardarMomentos = findViewById<Button>(R.id.guardarMomentos)
        val scrollImage1 = findViewById<ImageView>(R.id.scrollImage1)
        val scrollImage2 = findViewById<ImageView>(R.id.scrollImage2)
        val scrollImage3 = findViewById<ImageView>(R.id.scrollImage3)

        nombreConcierto.text = concierto.artista
        fechaConcierto.text = concierto.fecha
        ciudadConcierto.text = concierto.ciudad
        lugarConcierto.text = concierto.centro_de_eventos
        horaConcierto.text = "Hora: ${concierto.hora}"

        // Cargar la imagen desde Firebase Storage
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("Conciertos/portadas/${id}/imagen.jpg")

        Log.d("DetallesConcierto", "id: $id")

        imageRef.downloadUrl.addOnSuccessListener { uri ->
            // Usar Glide para cargar la imagen en el ImageView
            Glide.with(this)
                .load(uri)
                .into(artistImage)
        }.addOnFailureListener {
            // Manejar cualquier error
        }

        // Cargar las imágenes de momentos en los ImageView del scroll
        val momentosRef = storageReference.child("Conciertos/Momentos/$id")
        momentosRef.listAll().addOnSuccessListener { listResult ->
            val items = listResult.items.sortedBy { it.name }
            val lastItems = items.takeLast(3)

            lastItems.getOrNull(0)?.downloadUrl?.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(scrollImage1)
            }

            lastItems.getOrNull(1)?.downloadUrl?.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(scrollImage2)
            }

            lastItems.getOrNull(2)?.downloadUrl?.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(scrollImage3)
            }
        }.addOnFailureListener {
            // Manejar cualquier error
        }

        btnIrSitio.setOnClickListener {
            val intent = Intent(this, Mapasitio::class.java)
            intent.putExtra("ciudad", ciudadConcierto.text.toString())
            intent.putExtra("lugar", lugarConcierto.text.toString())
            startActivity(intent)
        }

        btnGuardarMomentos.setOnClickListener {
            val intent = Intent(this, Momentos::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
        }
    }
}
