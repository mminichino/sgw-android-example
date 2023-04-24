package com.example.sgwdemo.adjuster

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sgwdemo.R
import com.example.sgwdemo.models.PictureList
import java.text.SimpleDateFormat
import java.util.Locale


class PhotoAdapter(var imageList: ArrayList<PictureList>) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context).inflate(R.layout.picture_card, parent, false)
        return ViewHolder(inflater)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val dateString = imageList[position].date
        val readFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
        val writeFormat = SimpleDateFormat("M/d/yy h:m a", Locale.US)
        val date = readFormat.parse(dateString)
        viewHolder.headerView.text = String.format("Image #%d - %s", position + 1, writeFormat.format(date!!))
        viewHolder.imageView.setImageBitmap(imageList[position].bitmap)
        viewHolder.imageView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position)
            }
        }
        Log.i("PhotoAdapter", "Add image at $position")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update() {
        this.notifyDataSetChanged()
        Log.i("PhotoAdapter", "list size ${imageList.size}")
    }

    override fun getItemCount() = imageList.size

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick(position: Int)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerView: TextView
        val imageView: ImageView

        init {
            headerView = view.findViewById(R.id.imageInfoPane)
            imageView = view.findViewById(R.id.imageViewPane)
        }
    }
}
