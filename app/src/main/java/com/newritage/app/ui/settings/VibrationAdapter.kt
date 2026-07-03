package com.newritage.app.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newritage.app.R
import com.newritage.app.databinding.ItemVibrationBinding

class VibrationAdapter(
    private val items: List<VibrationPattern>,
    private val onPlayClick: (VibrationPattern) -> Unit,
    private val onSelectClick: (VibrationPattern) -> Unit
) : RecyclerView.Adapter<VibrationAdapter.ViewHolder>() {

    var enabled: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var playingId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(val binding: ItemVibrationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVibrationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isSelected = item.id == selectedId
        val isPlaying = item.id == playingId

        holder.binding.tvName.text = item.name
        holder.binding.ivCheck.setImageResource(
            if (isSelected) R.drawable.ic_check_circle_filled else R.drawable.ic_check_circle_outline
        )
        holder.binding.btnPlay.setImageResource(
            if (isPlaying) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
        )

        holder.binding.root.alpha = if (enabled) 1f else 0.4f
        holder.binding.btnPlay.isEnabled = enabled
        holder.binding.root.isEnabled = enabled

        holder.binding.btnPlay.setOnClickListener { if (enabled) onPlayClick(item) }
        holder.binding.ivCheck.setOnClickListener { if (enabled) onSelectClick(item) }
        holder.binding.root.setOnClickListener { if (enabled) onSelectClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
