package com.example.sgwdemo.adjuster

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
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
        Log.i("PhotoAdapter", "Add image at $position")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update() {
        this.notifyDataSetChanged()
        Log.i("PhotoAdapter", "list size ${imageList.size}")
    }

    override fun getItemCount() = imageList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView

        init {
            imageView = view.findViewById(R.id.imageViewPane)
        }
    }
}
