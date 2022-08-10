package dev.ex4.wetbulbweather

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat

class IconTextView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    init {
        gravity = Gravity.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.fontawesome6)
    }
}