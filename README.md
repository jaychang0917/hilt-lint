A custom lint check to detect missing hilt @AndroidEntryPoint issue.

# Download
**hilt-lint** is available on `mavenCentral()`.
```groovy
implementation "io.github.jaychang0917:hilt-lint-api:0.1.0"
lintChecks "io.github.jaychang0917:hilt-lint-checks:0.1.0"
```   

# Lint
### 1. Hilt Fragments must be attached to an @AndroidEntryPoint Activity.
The host Activity must be annotated with `@AndroidEntryPoint` if it uses a Hilt Fragment which annotated with `@AndroidEntryPoint`. The Fragment(s) declared in source code or xml(i.e. layout and navigation) will be checked.
```
ExampleActivity.kt:18: Error: com.example.ExampleFragment is annotated with @AndroidEntryPoint, com.example.ExampleActivity must be annotated with @AndroidEntryPoint. [RequiresAndroidEntryPoint]
            .replace(R.id.root, ExampleFragment())
                                ~~~~~~~~~~~~~~~~~
```
```
example.xml:5: Error: com.example.ExampleFragment is annotated with @AndroidEntryPoint, com.example.ExampleActivity must be annotated with @AndroidEntryPoint. [RequiresAndroidEntryPoint]
    <fragment android:id="@+id/example" android:name="com.example.ExampleFragment" />
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

### 2. Hilt view must be attached to an @AndroidEntryPoint Fragment or Activity.
The host Activity/Fragment must be annotated with `@AndroidEntryPoint` if it uses a Hilt View which annotated with `@AndroidEntryPoint`. The View(s) declared in source code or xml(i.e. layout) will be checked.
```
ExampleFragment.kt:14: Error: com.example.ExampleView is annotated with @AndroidEntryPoint, com.example.ExampleFragment must be annotated with @AndroidEntryPoint. [RequiresAndroidEntryPoint]
        ExampleView(requireContext())
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```
```
fragment_example.xml:8: Error: com.example.ExampleView is annotated with @AndroidEntryPoint, com.example.ExampleFragment must be annotated with @AndroidEntryPoint. [RequiresAndroidEntryPoint]
    <com.example.ExampleView
    ~~~~~~~~~~~~~~~~~~~~~~~~
```

### 3. Hilt ViewModel must be retrieved in an Activity/Fragment annotated with @AndroidEntryPoint.
The Activity/Fragment must be annotated with `@AndroidEntryPoint` if it uses a Hilt ViewModel which annotated with `@HiltViewModel`.
```
ExampleFragment.kt:17: Error: com.example.ExampleViewModel is annotated with @HiltViewModel, com.example.ExampleFragment must be annotated with @AndroidEntryPoint. [RequiresAndroidEntryPoint]
    private val viewModel by viewModels<ExampleViewModel>()
                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```
### 4. Type declared with @RequiresAndroidEntryPoint must be attached to an @AndroidEntryPoint component.
For polymorphism, we can use `@RequiresAndroidEntryPoint` to let the lint know that the usage of this type requires `@AndroidEntryPoint`.
```kotlin
interface ExampleApi {
    // The concrete implementation is a Hilt Fragment, which requires the host Activity/Fragment
    // to be annotated with @AndroidEntryPoint
    @RequiresAndroidEntryPoint
    fun exampleFragment() : Fragment
}
```
```
ExampleActivity.kt:25: Error: com.example.ExampleApi.exampleFragment is annotated with @RequiresAndroidEntryPoint, com.example.ExampleActivity must be annotated with @AndroidEntryPoint. [RequiresAndroidEntryPoint]
        val exampleFragment = exampleApi.exampleFragment()
                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

# License
```
 Copyright (C) 2024. Jay Chang
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
```