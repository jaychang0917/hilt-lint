package com.jaychang.hiltlint.sample

import androidx.fragment.app.Fragment
import com.jaychang.hiltlint.RequiresAndroidEntryPoint
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ExampleApiModule {
    @Provides
    fun exampleApi(): ExampleApi {
        return ExampleImpl()
    }
}

interface ExampleApi {
    @RequiresAndroidEntryPoint
    fun exampleFragment() : Fragment
}

class ExampleImpl : ExampleApi {
    override fun exampleFragment(): Fragment {
        return ExampleFragment()
    }
}
