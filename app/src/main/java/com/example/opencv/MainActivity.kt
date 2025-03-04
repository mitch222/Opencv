package com.example.opencv

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV no pudo cargarse.")
        } else {
            Log.d("OpenCV", "OpenCV cargado correctamente.")
        }

        // Configurar ajustes de ventana
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Enlazar los botones y el ImageView
        val btnCamera: Button = findViewById(R.id.btn_camera)
        val btnGallery: Button = findViewById(R.id.btn_gallery)
        imageView = findViewById(R.id.img_view)

        btnCamera.setOnClickListener { openCamera() }
        btnGallery.setOnClickListener { openGallery() }

        // Verificar permisos al iniciar
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.CAMERA] == true &&
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                    Log.d("Permisos", "Permisos concedidos")
                } else {
                    Log.e("Permisos", "Permisos denegados")
                }
            }

        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            Log.d("Permisos", "Permisos ya concedidos")
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    /**
     * Abre la galería para seleccionar una imagen.
     */
    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                imageUri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    val processedBitmap = applyGrayFilter(bitmap)
                    imageView.setImageBitmap(processedBitmap) // Mostrar imagen en el ImageView
                }
            }
        }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        selectImageLauncher.launch(intent)
    }

    /**
     * Abre la cámara y toma una foto.
     */
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                val processedBitmap = applyGrayFilter(it)
                imageView.setImageBitmap(processedBitmap) // Mostrar imagen en el ImageView
            }
        }

    private fun openCamera() {
        takePictureLauncher.launch(null)
    }

    /**
     * Aplica un filtro de escala de grises a la imagen usando OpenCV.
     */
    private fun applyGrayFilter(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // Convertir Bitmap a Mat
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY) // Convertir a escala de grises
        val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap) // Convertir Mat a Bitmap
        return resultBitmap
    }
}
