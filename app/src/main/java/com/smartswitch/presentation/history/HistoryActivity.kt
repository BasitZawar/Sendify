package com.smartswitch.presentation.history

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.text.style.TypefaceSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.smartswitch.R
import com.smartswitch.databinding.LayoutHistoryBinding
import com.smartswitch.utils.Constant


class HistoryActivity : AppCompatActivity() {
    private var binding: LayoutHistoryBinding? = null
    private var currentFragment: Fragment? = null
    private var historyFragment: HistoryFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutHistoryBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        try {
            Constant.customSystemBars(
                this@HistoryActivity, R.color.white, R.color.white, true
            )
        } catch (_: Exception) {
        }
        historyFragment = HistoryFragment()
        historyFragment?.let { updateFragment(it) }

        binding?.imgBack?.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

    }

    @SuppressLint("CommitTransaction")
    private fun updateFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit()
    }

    class CustomTypefaceSpan(family: String, private val newType: Typeface?) :
        TypefaceSpan(family) {

        override fun updateDrawState(ds: TextPaint) {
            applyCustomTypeFace(ds, newType)
        }

        override fun updateMeasureState(paint: TextPaint) {
            applyCustomTypeFace(paint, newType)
        }

        private fun applyCustomTypeFace(paint: TextPaint, tf: Typeface?) {
            tf?.let {
                val oldStyle: Int
                val old = paint.typeface
                oldStyle = old?.style ?: 0

                val fake = oldStyle and tf.style.inv()
                if (fake and Typeface.BOLD != 0) {
                    paint.isFakeBoldText = true
                }
                if (fake and Typeface.ITALIC != 0) {
                    paint.textSkewX = -0.25f
                }
                paint.typeface = tf
            }
        }
    }

}