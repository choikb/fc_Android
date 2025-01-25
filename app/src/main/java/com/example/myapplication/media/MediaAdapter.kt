package com.example.myapplication.media

import android.content.Context
import android.view.*
import android.widget.*
import com.example.myapplication.R
import com.bumptech.glide.Glide
class MediaAdapter(private val context: Context, private val mediaList: List<MediaItem>) : BaseAdapter() {

    override fun getCount(): Int = mediaList.size

    override fun getItem(position: Int): MediaItem = mediaList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_media, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.textView)

        val media = getItem(position)
        textView.text = media.name

        // Glide 또는 Picasso를 사용하여 이미지 로드
        Glide.with(context)
            .load(media.path)
            .placeholder(android.R.drawable.ic_menu_gallery) // 기본 아이콘
            .into(imageView)

        return view
    }
}