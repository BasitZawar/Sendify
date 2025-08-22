package com.smartswitch.presentation.onBoarding
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartswitch.databinding.ItemIntroBinding

class ViewPagerAdapter(private  val list:ArrayList<Intro>) : RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder( ItemIntroBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
         val intro=list[position]
        //holder.binding.icIntroImg1.setImageResource(intro.img)
        Glide.with(holder.itemView.context).load(intro.img).into(holder.binding.icIntroImg1)
//        val heading = intro.textHeading.split(" ")
//        holder.binding.tvHeadingFirst.text = heading[0]
//        holder.binding.tvHeadingSecond.text = heading[1]
        //holder.binding.tvHeadingFirst.text = intro.textHeading
        holder.binding.tvDetails.text = intro.textDetails
        applyGradientToText(holder)
    }

    override fun getItemCount(): Int {
        return list.size // We have 3 pages
    }

    override fun getItemViewType(position: Int): Int {
        return position // Return position as view type to differentiate between layouts
    }

    class ViewHolder(val binding:ItemIntroBinding) : RecyclerView.ViewHolder(binding.root) {

    }




    private fun applyGradientToText(holder : ViewHolder) {
        val paint = holder.binding.tvDetails.paint
        val width = paint.measureText(holder.binding.tvDetails.text.toString())
        val textShader: Shader = LinearGradient(
            0f, 0f, width, holder.binding.tvDetails.textSize,
            intArrayOf(
                Color.parseColor("#000000"), // Green (start)
                Color.parseColor("#3993FE"), // Green (middle)
                Color.parseColor("#000000")  // Red (end)
            ),
            floatArrayOf(0f, 0.5f, 1f), // Positions for color stops (0 = start, 0.5 = middle, 1 = end)
            Shader.TileMode.CLAMP
        )
        holder.binding.tvDetails.paint.shader = textShader
    }
}
