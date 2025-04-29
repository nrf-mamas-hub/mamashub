package com.kabarak.kabarakmhis.pnc.reasonsForSpecialCare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.pnc.data_class.ReasonsForSpecialCare

class ReasonsViewAdapter(
    private val item: List<ReasonsForSpecialCare>,
    private val onReasonClick: (String) -> Unit
) : RecyclerView.Adapter<ReasonsViewAdapter.ReasonsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReasonsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reasons_item, parent, false)
        return ReasonsViewHolder(view)    }

    override fun onBindViewHolder(holder: ReasonsViewHolder, position: Int) {
        val reason = item[position]
        holder.bind(reason)
        holder.itemView.setOnClickListener {
            onReasonClick(reason.id)
        }
    }

    class ReasonsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val reasonTextView: TextView = itemView.findViewById(R.id.reasonItem)
        fun bind(reason : ReasonsForSpecialCare){
            reasonTextView.text = reason.reasons.joinToString("\n")
        }
    }

    override fun getItemCount(): Int=item.size
}