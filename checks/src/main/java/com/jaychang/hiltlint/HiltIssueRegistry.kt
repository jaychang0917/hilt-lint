package com.jaychang.hiltlint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

class HiltIssueRegistry : IssueRegistry() {
    override val issues = listOf(
        RequiresAndroidEntryPointDetector.ISSUE
    )

    override val api: Int
        get() = CURRENT_API

    override val minApi: Int
        get() = 8 // works with Android Studio 4.1 or later

    override val vendor: Vendor
        get() = Vendor(vendorName = "hilt-lint")
}
