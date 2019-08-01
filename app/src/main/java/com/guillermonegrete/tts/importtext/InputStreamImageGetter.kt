package com.guillermonegrete.tts.importtext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html

class InputStreamImageGetter(
    private val basePath: String,
    private val context: Context,
    private val zipFileReader: ZipFileReader
): Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        println("getDrawable source: $source")
        val inputStream = source?.let {
            val fullPath = if(basePath.isEmpty()) it else "$basePath/$it"
            zipFileReader.getFileStream(fullPath)
        }
        val bitmap = BitmapFactory.decodeStream(inputStream)
        bitmap?.density = Bitmap.DENSITY_NONE
        val drawable = BitmapDrawable(context.resources, bitmap)
        drawable.bounds = Rect(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable
    }
}