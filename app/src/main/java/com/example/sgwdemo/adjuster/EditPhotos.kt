package com.example.sgwdemo.adjuster

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File


class EditPhotos : AppCompatActivity() {

    private var TAG = "EditPhotos"
    private var cntx: Context = this
    var claimId: String? = null
    var adjusterId: String? = null
    var userIdValue: String? = null
    var regionValue: String? = null
    var pictureUri: Uri? = null
    val imageList: ArrayList<Bitmap> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actitivty_claim_photos)
        claimId = intent.getStringExtra("ClaimId")
        adjusterId = intent.getStringExtra("AdjusterId")
        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")

        getSavesPictures()

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
                        imageList.add(it)
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
        val galleryIntent = Intent(Intent.ACTION_VIEW)
        galleryIntent.type = "image/*"
        val extraIntents = arrayOf(mediaIntent, galleryIntent)

        val chooserIntent = Intent.createChooser(captureIntent, "Select source")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)

        return chooserIntent
    }

    fun getSavesPictures() {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        val scope = CoroutineScope(Dispatchers.Default)
        var pictures: ArrayList<Picture>

        scope.launch {
            pictures = db.getPictureIdByClaim(claimId!!)
            for (picture in pictures) {
                if (picture.recordId >= 1) {
                    Log.i(TAG, "Loading ${picture.metaId}")
                    val bytes = db.getImage(picture.metaId)
                    bytes?.let {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageList.add(bitmap)
                    }
                }
            }
        }
    }

    fun onSaveTapped(view: View?) {
        val db: CouchbaseConnect = CouchbaseConnect.getInstance(cntx)
        var counter = 0
        for (image in imageList) {
            counter += 1
            val docId = "picture::$claimId::$counter"
            val blob = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, blob)
            val byteArray = blob.toByteArray()
            val mutableDoc = if (db.documentExists(docId)) {
                db.getDocument(docId)
            } else {
                MutableDocument(docId)
            }
            mutableDoc.setBlob("image", Blob("image/png", byteArray))
                .setString("claim_id", claimId)
                .setString("type", "picture")
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
}
