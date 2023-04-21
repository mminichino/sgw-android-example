package com.example.sgwdemo.adjuster

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.example.sgwdemo.R
import java.io.File


class EditPhotos : AppCompatActivity() {

    private var TAG = "EditPhotos"
    private var cntx: Context = this
    var claimId: String? = null
    var adjusterId: String? = null
    var userIdValue: String? = null
    var regionValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actitivty_claim_photos)
        claimId = intent.getStringExtra("ClaimId")
        adjusterId = intent.getStringExtra("AdjusterId")
        userIdValue = intent.getStringExtra("UserName")
        regionValue = intent.getStringExtra("Region")

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

        val horizontalScrollView = HorizontalScrollView(this)
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        horizontalScrollView.layoutParams = layoutParams

        val linearLayout = LinearLayout(this)
        val linearParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        linearLayout.layoutParams = linearParams

        horizontalScrollView.addView(linearLayout)

        val defaultImage = ImageView(this)
        val defaultImageParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        defaultImage.layoutParams = defaultImageParams
        defaultImage.setImageResource(R.drawable.default_profile_icon_24)
        linearLayout.addView(defaultImage)

        val linearLayoutOuter = findViewById<RelativeLayout>(R.id.imageViewer)
        linearLayoutOuter?.addView(horizontalScrollView)

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
