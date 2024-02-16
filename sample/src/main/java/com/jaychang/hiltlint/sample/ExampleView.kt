package com.jaychang.hiltlint.sample

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExampleView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attr, defStyleAttr)