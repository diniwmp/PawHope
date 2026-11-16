package lk.zenova.pawhope.util

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File


class PhotoPicker(
    private val activity: AppCompatActivity,
    private val maxPhotos: Int,
    private val onChanged: (List<Uri>) -> Unit
) {
    val photos = mutableListOf<Uri>()
    private var cameraUri: Uri? = null

    private val takePicture =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) cameraUri?.let { add(listOf(it)) }
        }

    private val pickImages = activity.registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxOf(2, maxPhotos))
    ) { uris -> add(uris) }

    private fun add(uris: List<Uri>) {
        uris.take(maxPhotos - photos.size).forEach { photos.add(it) }
        onChanged(photos)
    }

    fun removeAt(index: Int) {
        if (index in photos.indices) {
            photos.removeAt(index)
            onChanged(photos)
        }
    }

    fun show() {
        if (photos.size >= maxPhotos) {
            Toast.makeText(activity, "You can add up to $maxPhotos photos", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("Add a photo")
            .setItems(arrayOf("Take a photo", "Choose from gallery")) { _, which ->
                if (which == 0) launchCamera()
                else pickImages.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            .show()
    }

    private fun launchCamera() {
        val dir = File(activity.cacheDir, "camera").apply { mkdirs() }
        val file = File.createTempFile("photo_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        cameraUri = uri
        takePicture.launch(uri)
    }
}