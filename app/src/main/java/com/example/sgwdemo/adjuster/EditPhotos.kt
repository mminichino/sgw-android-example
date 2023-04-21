package com.example.sgwdemo.adjuster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import com.example.sgwdemo.R


class EditPhotos : AppCompatActivity() {

    private var TAG = "EditPhotos"
    private var cntx: Context = this
    var claimId: String? = null
    var adjusterId: String? = null
    var userIdValue: String? = null
    var regionValue: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actitivty_claim_photos)
        claimId = intent.getStringExtra("ClaimId")
        adjusterId = intent.getStringExtra("AdjusterId")
        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")

        val defaultBitmap = BitmapFactory.decodeResource(cntx.resources, R.drawable.default_profile_icon_24)
        var imageList: ArrayList<Bitmap> = arrayListOf(defaultBitmap)

        val pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                if (uris.isNotEmpty()) {
                    Log.d("PhotoPicker", "Number of items selected: ${uris.size}")
                } else {
                    Log.d("PhotoPicker", "No media selected")
                }
            }

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
            }
        }

        val recyclerview = findViewById<RecyclerView>(R.id.imageViewer)
        recyclerview.layoutManager = LinearLayoutManager(this)
        val adapter = PhotoAdapter(imageList)
        recyclerview.adapter = adapter

        val photoGalleryButton = findViewById<Button>(R.id.galleryAddButton)
        photoGalleryButton.setOnClickListener {
            resultLauncher.launch(getPickImageChooserIntent())
        }
    }

    private fun getCaptureImageOutputUri(): Uri {
        val getImage = externalCacheDir
        return Uri.fromFile(File(getImage?.path, "image.png"))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPickImageChooserIntent(): Intent {
        val outputFileUri = getCaptureImageOutputUri()

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val mediaIntent = Intent(MediaStore.ACTION_PICK_IMAGES)
        val galleryIntent = Intent(Intent.ACTION_VIEW)
        galleryIntent.type = "image/*"
        val extraIntents = arrayOf(mediaIntent, galleryIntent)

        val chooserIntent = Intent.createChooser(captureIntent, "Select source")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)

        return chooserIntent
    }

    fun onSaveTapped(view: View?) {
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
