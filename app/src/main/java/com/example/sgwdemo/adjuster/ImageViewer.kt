package com.example.sgwdemo.adjuster

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.sgwdemo.R
import java.io.File

private const val TAG = "ImageViewer"

class ImageViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_viewer)
        val filename = intent.getStringExtra("image")
        Log.i(TAG, "Viewing image $filename")

        val imageView: ImageView = findViewById(R.id.imageView)
        val stream = openFileInput(filename)
        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        imageView.setImageBitmap(bitmap)
        filename?.let {
            val tempFile = File(filename)
            if (tempFile.exists()) tempFile.delete()
        }

        val doneButton = findViewById<Button>(R.id.doneButton)
        doneButton.setOnClickListener {
            finish()
        }
    }
}
