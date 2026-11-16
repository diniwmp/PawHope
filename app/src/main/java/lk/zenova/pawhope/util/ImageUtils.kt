package lk.zenova.pawhope.util

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import kotlin.math.roundToInt

object ImageUtils {

    fun compress(resolver: ContentResolver, uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val maxSide = 1280f
            val width = info.size.width
            val height = info.size.height
            val scale = minOf(1f, maxSide / maxOf(width, height))
            decoder.setTargetSize((width * scale).roundToInt(), (height * scale).roundToInt())
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return out.toByteArray()
    }


    fun uploadAll(
        activity: Activity,
        uris: List<Uri>,
        folder: String,
        prefix: String,
        onResult: (List<String>?, String?) -> Unit
    ) {
        if (uris.isEmpty()) {
            onResult(emptyList(), null)
            return
        }
        val resolver = activity.contentResolver

        thread {
            val bytesList = try {
                uris.map { compress(resolver, it) }
            } catch (e: Exception) {
                null
            }

            activity.runOnUiThread {
                if (bytesList == null) {
                    onResult(null, "Couldn't read the photos")
                    return@runOnUiThread
                }
                val storage = FirebaseStorage.getInstance()
                val stamp = System.currentTimeMillis()

                val tasks = bytesList.mapIndexed { index, bytes ->
                    val ref = storage.reference.child("$folder/${prefix}_${stamp}_$index.jpg")
                    ref.putBytes(bytes).continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                        ref.downloadUrl
                    }
                }

                Tasks.whenAllSuccess<Uri>(tasks)
                    .addOnSuccessListener { urls -> onResult(urls.map { it.toString() }, null) }
                    .addOnFailureListener { e -> onResult(null, e.message ?: "Upload failed") }
            }
        }
    }
}