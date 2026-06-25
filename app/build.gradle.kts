plugins { id("com.android.application") }

android {
    namespace = "com.xixijiuguan.gougulocaltavern"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.xixijiuguan.gougulocaltavern"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2-esmfix"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        externalNativeBuild { cmake { cppFlags += listOf("-std=c++17"); arguments += listOf("-DANDROID_STL=c++_shared") } }
    }

    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }

    sourceSets { getByName("main") { jniLibs.srcDirs("libnode/bin") } }

    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }

    packaging {
        resources { excludes += listOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/DEPENDENCIES") }
        jniLibs { pickFirsts += listOf("**/libnode.so", "**/libc++_shared.so") }
    }
}
