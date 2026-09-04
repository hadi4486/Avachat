plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.avachat.app.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        buildConfig = true
    }
    // Workaround for Room 2.6.1 + KSP2 emitting `Continuation<Unit>` in generated
    // Java while kotlinc compiles suspend funs to `Continuation<? super Unit>`.
    // Patch the generated sources right after KSP runs, before javac sees them.
    tasks.register("patchKspContinuationSigs") {
        doLast {
            val genRoot = layout.buildDirectory.dir("generated/ksp/debug/java").get().asFile
            val target = genRoot.resolve("com/avachat/app/core/data/local/ChatDao_Impl.java")
            if (target.exists()) {
                val text = target.readText()
                val patched = text.replace("Continuation<Unit>", "Continuation<? super Unit>")
                    .replace("Continuation<List<MessageEntity>>", "Continuation<? super List<MessageEntity>>")
                    .replace("Continuation<Integer>", "Continuation<? super Integer>")
                    .replace("Continuation<List<ConversationEntity>>", "Continuation<? super List<ConversationEntity>>")
                    .replace("Continuation<ConversationEntity>", "Continuation<? super ConversationEntity>")
                    .replace("Continuation<Long>", "Continuation<? super Long>")
                    // Room 2.6.1 + KSP2 also assumes `getIsError()` (bean-style) for the
                    // `isError` property, but kotlinc emits `isError()`. Match kotlinc.
                    .replace("entity.getIsError()", "entity.isError()")
                if (patched != text) {
                    target.writeText(patched)
                    logger.lifecycle("patched KSP continuation signatures in ${target.name}")
                }
            }
        }
    }
    tasks.matching { it.name == "compileDebugJavaWithJavac" }.configureEach {
        dependsOn("patchKspContinuationSigs")
    }
    tasks.matching { it.name == "kspDebugKotlin" }.configureEach {
        finalizedBy("patchKspContinuationSigs")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)

    api(libs.androidx.datastore.preferences)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    api(libs.retrofit)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
