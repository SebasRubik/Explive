package com.example.explive

import Concierto
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AgregarConcierto : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var imageView: ImageView
    private val PICK_IMAGE_REQUEST = 71
    private lateinit var filePath: Uri
    private val PERMISSION_REQUEST_CODE = 100
    val editTextArtista = findViewById<EditText>(R.id.editTextText)
    val editTextCiudad = findViewById<EditText>(R.id.editTextText2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_concierto)

        database = Firebase.database.reference

        val editTextCentroDeEventos = findViewById<EditText>(R.id.editTextText3)
        val editTextFecha = findViewById<EditText>(R.id.editTextText4)
        val editTextHora = findViewById<EditText>(R.id.editTextText5)
        val editTextGenero = findViewById<EditText>(R.id.editTextGenero)
        val btnAgregar = findViewById<Button>(R.id.btnAgregar)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        imageView = findViewById(R.id.imageView)

        btnSelectImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Selecciona una Imagen"), PICK_IMAGE_REQUEST)
        }

        btnAgregar.setOnClickListener {
            val artista = editTextArtista.text.toString().trim()
            val ciudad = editTextCiudad.text.toString().trim()
            val centroDeEventos = editTextCentroDeEventos.text.toString().trim()
            val fecha = editTextFecha.text.toString().trim()
            val hora = editTextHora.text.toString().trim()
            val generoText = editTextGenero.text.toString().trim()

            if (artista.isEmpty() || ciudad.isEmpty() || centroDeEventos.isEmpty() || fecha.isEmpty() || hora.isEmpty() || generoText.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                val generos = generoText.split("/").map { it.trim() }
                val concierto = Concierto("", artista, ciudad, centroDeEventos, fecha, hora, generos)
                agregarConciertoAFirebase(concierto, editTextArtista, editTextCiudad, editTextCentroDeEventos, editTextFecha, editTextHora, editTextGenero)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            filePath = data.data!!
            imageView.setImageURI(filePath)
        }
    }

    private fun uploadImage(conciertoId: String) {
        if (::filePath.isInitialized) {
            val storageReference = FirebaseStorage.getInstance().reference
            val ref = storageReference.child("Conciertos/portadas/$conciertoId/imagen.jpg")
            ref.putFile(filePath)
                .addOnSuccessListener {
                    // Imagen subida con éxito
                    Toast.makeText(this, "Imagen subida exitosamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Manejar error
                    Toast.makeText(this, "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun agregarConciertoAFirebase(concierto: Concierto, editTextArtista: EditText, editTextCiudad: EditText, editTextCentroDeEventos: EditText, editTextFecha: EditText, editTextHora: EditText, editTextGenero: EditText) {
        val conciertosRef = database.child("conciertos")

        conciertosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastId = snapshot.children.maxOfOrNull { it.key?.toIntOrNull() ?: 0 } ?: 0
                val newId = lastId + 1
                concierto.id = newId.toString()
                conciertosRef.child(newId.toString()).setValue(concierto)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@AgregarConcierto, "Concierto agregado exitosamente", Toast.LENGTH_SHORT).show()
                            uploadImage(newId.toString())
                            // Limpiar los EditText
                            editTextArtista.text.clear()
                            editTextCiudad.text.clear()
                            editTextCentroDeEventos.text.clear()
                            editTextFecha.text.clear()
                            editTextHora.text.clear()
                            editTextGenero.text.clear()

                            // Crear canal de notificación
                            createNotificationChannel()
                            // Verificar permisos de notificación y enviar notificación
                            checkNotificationPermissionAndSend()

                            // Finalizar la actividad para volver a la pantalla anterior
                            finish()
                        } else {
                            Toast.makeText(this@AgregarConcierto, "Error al agregar concierto: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AgregarConcierto, "Error al acceder a la base de datos: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermissionAndSend() {
        if (ActivityCompat.checkSelfPermission(
                this,
               POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE
            )
        } else {
            sendNotification()
        }
    }

    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.icoexplive)
            .setContentTitle("¡Hay un concierto nuevo!")
            .setContentText("Se anunció un concierto de ${editTextArtista} en ${editTextCiudad}. ¡No te lo pierdas!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Crear una acción que se mostrará en el wearable
        val actionIntent = Intent(this, Menu::class.java).apply {
            putExtra("extra_data", "value")
        }
        val actionPendingIntent: PendingIntent =
            PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)

        val action = NotificationCompat.Action.Builder(
            R.drawable.icoexplive, "Ver Detalles", actionPendingIntent
        ).build()

        // Extender la notificación para wearables
        val wearableExtender = NotificationCompat.WearableExtender()
            .addAction(action)

        builder.extend(wearableExtender)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@AgregarConcierto,
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, builder.build())
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                sendNotification()
            } else {
                Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
