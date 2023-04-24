package com.example.sgwdemo.adjuster

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.couchbase.lite.Blob
import com.couchbase.lite.MutableDocument
import com.example.sgwdemo.R
import com.example.sgwdemo.cbdb.CouchbaseConnect
import com.example.sgwdemo.models.Picture
import com.example.sgwdemo.models.PictureList
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EditPhotos : AppCompatActivity() {

    private var TAG = "EditPhotos"
    private var cntx: Context = this
    var claimId: String? = null
    var adjusterId: String? = null
    var userIdValue: String? = null
    var regionValue: String? = null
    var pictureUri: Uri? = null
    var pictureCount: TextView? = null
    val imageList: ArrayList<PictureList> = arrayListOf()
    var handler: Handler = Handler(Looper.getMainLooper())

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actitivty_claim_photos)
        claimId = intent.getStringExtra("ClaimId")
        adjusterId = intent.getStringExtra("AdjusterId")
        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")
        pictureCount = findViewById(R.id.pictureCount)

        getSavesPictures()
        handler.post(runnableCode)
        val photoAdapter = PhotoAdapter(imageList)

        val recyclerview = findViewById<RecyclerView>(R.id.imageViewer)
        recyclerview.layoutManager = LinearLayoutManager(this)
        recyclerview.adapter = photoAdapter

        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.i(TAG, "Got result")
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    var imageBitmap: Bitmap? = null
                    if (data?.data != null) {
                        Log.i(TAG, "Uri: $data?.data")
                        data.data?.let {
                            val source =ImageDecoder.createSource(this.contentResolver, it)
                            imageBitmap = ImageDecoder.decodeBitmap(source)
                        }
                    } else {
                        Log.i(TAG, "Got live picture")
                        pictureUri?.let {
                            val source = ImageDecoder.createSource(this.contentResolver, it)
                            imageBitmap = ImageDecoder.decodeBitmap(source)
                        }
                    }
                    imageBitmap?.let {
                        val currentTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
                        val currentDate = currentTimeFormat.format(Date())
                        val pictureRecord = PictureList(it, currentDate)
                        imageList.add(pictureRecord)
                        Log.i(TAG, "Bitmap: ${it.byteCount}")
                        photoAdapter.update()
                    }
                }
            }

        val photoGalleryButton = findViewById<Button>(R.id.galleryAddButton)
        photoGalleryButton.setOnClickListener {
            resultLauncher.launch(getPickImageChooserIntent())
        }
    }

    private fun getCaptureImageOutputUri(): Uri {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("image", ".jpg", storageDir)
        return FileProvider.getUriForFile(
            this,
            "com.example.sgwdemo.fileprovider",
            imageFile)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPickImageChooserIntent(): Intent {
        val outputFileUri = getCaptureImageOutputUri()
        pictureUri = outputFileUri

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        val mediaIntent = Intent(MediaStore.ACTION_PICK_IMAGES)
        val extraIntents = arrayOf(mediaIntent)

        val chooserIntent = Intent.createChooser(captureIntent, "Select source")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)

        return chooserIntent
    }

    private fun getSavesPictures() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        var pictures: ArrayList<Picture>

        runBlocking {
            pictures = db.getPictureIdByClaim(claimId!!)
            for (picture in pictures) {
                Log.i(TAG, "Loading ${picture.metaId}")
                val pictureRecord = db.getImage(picture.metaId)
                pictureRecord?.let {
                    imageList.add(pictureRecord)
                }
            }
        }
    }

    private fun numPictures(): Int = runBlocking {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val pictures = db.getPictureIdByClaim(claimId!!)
        return@runBlocking pictures.size
    }

    fun onSaveTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        var counter = 0
        for (image in imageList) {
            counter += 1
            val docId = "picture::$claimId::$counter"
            val blob = ByteArrayOutputStream()
            image.bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob)
            val byteArray = blob.toByteArray()
            val mutableDoc = if (db.documentExists(docId)) {
                db.getDocument(docId)
            } else {
                MutableDocument(docId)
            }
            mutableDoc.setBlob("image", Blob("image/png", byteArray))
                .setString("claim_id", claimId)
                .setString("type", "picture")
                .setString("date", image.date)
                .setString("region", regionValue)
                .setInt("record_id", counter)
            db.updateDocument(mutableDoc)
        }
        returnToPreviousView()
    }

    fun onCancelTapped(view: View?) {
        returnToPreviousView()
    }

    private fun returnToPreviousView() {
        val intent = Intent(cntx, EditClaimActivity::class.java)
        handler.removeCallbacks(runnableCode)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        intent.putExtra("ClaimId", claimId)
        intent.putExtra("AdjusterId", adjusterId)
        intent.putExtra("Region", regionValue)
        intent.putExtra("UserName", userIdValue)
        startActivity(intent)
    }

    private val runnableCode: Runnable = object : Runnable {
        override fun run() {
            val currentCount = imageList.size
            val countDisplay = if (currentCount == 0) {
                "No Images. Click ADD to attach an image."
            } else {
                "Images: $currentCount"
            }
            pictureCount?.text = countDisplay
            handler.postDelayed(this, 1000)
        }
    }
}
