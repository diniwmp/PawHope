package lk.zenova.pawhope.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
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
}