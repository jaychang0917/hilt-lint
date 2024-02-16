package com.jaychang.hiltlint.sample

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExampleViewModel @Inject constructor() : ViewModel()