package com.example.sgwdemo.adjuster

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.sgwdemo.R


class PhotoAdapter(var imageList: ArrayList<Bitmap>) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.picture_card, parent, false)
        return ViewHolder(inflater)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.imageView.setImageBitmap(imageList[position])
    }

    public fun addImage(picture: Bitmap) {
        val insertIndex = imageList.size
        imageList.add(picture)
        notifyItemInserted(insertIndex)
    }

    override fun getItemCount() = imageList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView

        init {
            imageView = view.findViewById(R.id.imageViewPane)
        }
    }
}
