package com.jaychang.hiltlint

/**
 * The hilt-lint checks detects if the usage of type annotated with @RequiresAndroidEntryPoint
 * is annotated with @AndroidEntryPoint.
 *
 * Example:
 *
 * ```
 * interface ExampleApi {
 *     @RequiresAndroidEntryPoint
 *     fun exampleFragment(): Fragment
 * }
 *
 * // This Activity requires to be annotated with @AndroidEntryPoint,
 * // otherwise, hilt-lint reports this issue.
 * @AndroidEntryPoint
 * class ExampleActivity {
 *     val exampleFragment = exampleApi.exampleFragment()
 * }
 * ```
 * */
annotation class RequiresAndroidEntryPoint
