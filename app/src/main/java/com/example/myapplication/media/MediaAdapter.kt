package com.example.myapplication.media

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.bumptech.glide.Glide
class MediaAdapter(
    private val context: Context,
    private val mediaItems: MutableList<MediaItem>,
    private val onMediaItemClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<MediaItem>() // 선택된 항목 저장

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textView: TextView = view.findViewById(R.id.textView)
        val textViewFolderDate: TextView = view.findViewById(R.id.textViewFolderDate)
        val textViewFolderCount: TextView = view.findViewById(R.id.textViewFolderCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaItem = mediaItems[position]
        holder.textView.text = mediaItem.name

        if (mediaItem.isFolder) {
            holder.imageView.setImageResource(R.drawable.ic_folder) // Replace with a built-in icon

            holder.textViewFolderDate.text = mediaItem.creationTime ?: "날짜 없음"
            holder.textViewFolderCount.text = "${mediaItem.photoCount}개"

            holder.textViewFolderDate.visibility = View.VISIBLE
            holder.textViewFolderCount.visibility = View.VISIBLE

            holder.imageView.foreground = null
           // context.getDrawable(R.drawable.default_item_border)

        } else {
            Glide.with(context).load(mediaItem.path).into(holder.imageView) // Glide로 이미지 로드
            holder.textViewFolderDate.visibility = View.GONE
            holder.textViewFolderCount.visibility = View.GONE

            // ✅ 이미지 부분에만 테두리 적용
            holder.imageView.foreground = if (selectedItems.contains(mediaItem)) {
                context.getDrawable(R.drawable.selected_item_border) // ✅ 선택된 경우 테두리 적용
            } else {
                null // ✅ 기본 상태 (테두리 없음)
            }
        }

        // ✅ 사진 선택/해제 로직
        holder.itemView.setOnClickListener {
            if (!mediaItem.isFolder) { // ✅ 폴더 선택은 제외
                if (selectedItems.contains(mediaItem)) {
                    selectedItems.remove(mediaItem)
                } else {
                    selectedItems.add(mediaItem)
                }
                onMediaItemClick(mediaItem)
                notifyItemChanged(position) // UI 업데이트
            } else {
                onMediaItemClick(mediaItem)
            }
        }
    }

    override fun getItemCount(): Int = mediaItems.size

    // ✅ 다른 폴더 이동 시 선택 항목 초기화
    @SuppressLint("NotifyDataSetChanged")
    fun clearSelectedItems() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

}

//class MediaAdapter(
//    private val context: Context,
//    private val mediaItems: MutableList<MediaItem>,
//    private val onMediaItemClick: (MediaItem) -> Unit
//) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
//
//    private val selectedItems = mutableSetOf<MediaItem>() // 선택된 항목 저장
//
//    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val imageView: ImageView = view.findViewById(R.id.imageView)
//        val textView: TextView = view.findViewById(R.id.textView)
//        val textViewFolderDate: TextView = view.findViewById(R.id.textViewFolderDate)
//        val textViewFolderCount: TextView = view.findViewById(R.id.textViewFolderCount)
//        val layoutContainer: LinearLayout = view.findViewById(R.id.layoutContainer) // 추가됨
//        fun bind(mediaItem: MediaItem) {
//            textView.text = mediaItem.name
//
//            if (mediaItem.isFolder) {
//                imageView.setImageResource(R.drawable.ic_add_icon) // 폴더 아이콘
//                textViewFolderDate.text = mediaItem.creationTime ?: "날짜 없음"
//                textViewFolderCount.text = "${mediaItem.photoCount}개"
//
//                textViewFolderDate.visibility = View.VISIBLE
//                textViewFolderCount.visibility = View.VISIBLE
//            } else {
//                Glide.with(context).load(mediaItem.path).into(imageView) // Glide로 이미지 로드
//                textViewFolderDate.visibility = View.GONE
//                textViewFolderCount.visibility = View.GONE
//            }
//
//            // ✅ 선택된 항목 테두리 강조
//            itemView.background = if (selectedItems.contains(mediaItem)) {
//                context.getDrawable(R.drawable.selected_item_border)
//            } else {
//                context.getDrawable(R.drawable.default_item_border)
//            }
//
//            // ✅ 선택/해제 기능 추가
//            itemView.setOnClickListener {
//                if (selectedItems.contains(mediaItem)) {
//                    selectedItems.remove(mediaItem)
//                } else {
//                    selectedItems.add(mediaItem)
//                }
//                notifyItemChanged(adapterPosition) // UI 업데이트
//                onMediaItemClick(mediaItem)
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        //holder.bind(mediaItems[position])
//
//        val mediaItem = mediaItems[position]
//        holder.textView.text = mediaItem.name
//
//        // ✅ 폴더 처리 (테두리 제외, 폴더 정보 표시)
//        if (mediaItem.isFolder) {
//            holder.imageView.setImageResource(R.drawable.ic_add_icon) // 폴더 아이콘
//            holder.textViewFolderDate.text = mediaItem.creationTime ?: "날짜 없음"
//            holder.textViewFolderCount.text = "${mediaItem.photoCount}개"
//
//            holder.textViewFolderDate.visibility = View.VISIBLE
//            holder.textViewFolderCount.visibility = View.VISIBLE
//
//            holder.imageView.background = null // ✅ 폴더에는 선택 테두리 없음
//        } else {
//            // ✅ 사진 처리 (이미지 불러오기 + 테두리 적용)
//            Glide.with(context).load(mediaItem.path).into(holder.imageView)
//            holder.textViewFolderDate.visibility = View.GONE
//            holder.textViewFolderCount.visibility = View.GONE
//
//            // ✅ 선택된 항목만 테두리 적용 (텍스트 제외)
//            holder.imageView.background = if (selectedItems.contains(mediaItem)) {
//                context.getDrawable(R.drawable.selected_item_border)
//            } else {
//                null
//            }
//        }
//
//        // ✅ 사진 선택/해제 로직
//        holder.itemView.setOnClickListener {
//            if (!mediaItem.isFolder) { // ✅ 폴더 선택은 제외
//                if (selectedItems.contains(mediaItem)) {
//                    selectedItems.remove(mediaItem)
//                } else {
//                    selectedItems.add(mediaItem)
//                }
//                notifyItemChanged(position) // UI 업데이트
//                onMediaItemClick(mediaItem)
//            }
//            else
//            {
//                onMediaItemClick(mediaItem)
//            }
//
//
//        }
//    }
//
//    override fun getItemCount(): Int = mediaItems.size
//
//    // ✅ 다른 폴더 이동 시 선택 항목 초기화
//    fun clearSelectedItems() {
//        selectedItems.clear()
//        notifyDataSetChanged()
//    }
//}
