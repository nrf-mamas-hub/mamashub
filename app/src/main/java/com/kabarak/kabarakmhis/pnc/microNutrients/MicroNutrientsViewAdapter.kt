package com.kabarak.kabarakmhis.pnc.microNutrients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kabarak.kabarakmhis.R
import com.kabarak.kabarakmhis.pnc.data_class.MicroNutrients

class MicroNutrientsViewAdapter(
    private val microNutrient: List<MicroNutrients>,
    private val onReasonClick: (String) -> Unit
) : RecyclerView.Adapter<MicroNutrientsViewAdapter.MicroNutrientsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MicroNutrientsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.micro_nutrients_item, parent, false)
        return MicroNutrientsViewHolder(view)
    }

    override fun onBindViewHolder(holder: MicroNutrientsViewHolder, position: Int) {
        val item = microNutrient[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onReasonClick(item.id)
        }
    }

    class MicroNutrientsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val AgeTextView: TextView = itemView.findViewById(R.id.AgeTextView)
        private val numberTextView: TextView = itemView.findViewById(R.id.numberTextView)
        private val currentDateTextView: TextView = itemView.findViewById(R.id.currentDateTextView)
        private val nextVisitDateTextView: TextView =
            itemView.findViewById(R.id.nextVisitDateTextView)

        fun bind(microNutrient: MicroNutrients) {
            AgeTextView.text = microNutrient.age.toString()
            numberTextView.text = microNutrient.noIssued.toString()
            currentDateTextView.text = microNutrient.dateIssued
            nextVisitDateTextView.text = microNutrient.dateOfNextVisit
        }
    }

    override fun getItemCount(): Int = microNutrient.size
}