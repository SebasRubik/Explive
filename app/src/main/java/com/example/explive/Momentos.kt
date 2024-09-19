package com.example.explive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.explive.databinding.ActivityMomentosBinding
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class Momentos : AppCompatActivity() {

    private lateinit var binding: ActivityMomentosBinding
    private val REQUEST_CAMERA_PERMISSION = 101
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMomentosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        id = intent.getStringExtra("id") ?: ""

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            dispatchTakePictureIntent()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "El permiso de cámara fue negado permanentemente. Por favor, habilita el permiso en ajustes.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                saveImageToFirebase(it)
            }
        }
    }

    private fun saveImageToFirebase(bitmap: Bitmap) {
        val storageReference = FirebaseStorage.getInstance().reference
        val momentosRef = storageReference.child("Conciertos/Momentos/$id")

        momentosRef.listAll().addOnSuccessListener { listResult ->
            val momentoCount = listResult.items.size + 1
            val imageRef = momentosRef.child("momento_$momentoCount.jpeg")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask = imageRef.putBytes(data)
            finish()
            uploadTask.addOnSuccessListener {
                Toast.makeText(this, "Imagen guardada exitosamente", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Error al guardar la imagen: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al acceder a los momentos: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
