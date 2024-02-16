package com.jaychang.hiltlint.sample

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.jaychang.hiltlint.sample.databinding.ActivityExampleBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExampleActivity : FragmentActivity() {
    private lateinit var binding: ActivityExampleBinding

    private val viewModel by viewModels<ExampleViewModel>()

    @Inject
    lateinit var exampleApi: ExampleApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = ExampleFragment()
        val view = ExampleView(this)
        val exampleFragment = exampleApi.exampleFragment()
    }

}


