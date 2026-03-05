package com.talha.rupiahkharch

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat

class ColorSpinnerAdapter(context: Context, private val colors: List<String>) :
    ArrayAdapter<String>(context, R.layout.item_color_spinner, colors) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createColorView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createColorView(position, convertView, parent)
    }

    private fun createColorView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_color_spinner, parent, false)

        val colorBar = view.findViewById<View>(R.id.colorBar)

        // Get the rounded drawable and change its color dynamically
        val background = ContextCompat.getDrawable(context, R.drawable.bg_color_bar) as GradientDrawable
        val colorDrawable = background.constantState?.newDrawable()?.mutate() as GradientDrawable

        try {
            colorDrawable.setColor(Color.parseColor(colors[position]))
            colorBar.background = colorDrawable
        } catch (e: Exception) {
            colorDrawable.setColor(Color.WHITE)
            colorBar.background = colorDrawable
        }

        return view
    }
}