plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jai.mario"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jai.mario"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH"))
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            // gg
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        jniLibs.pickFirsts.add("lib/**/libvlc.so")
        jniLibs.pickFirsts.add("lib/**/libvlcjni.so")
        jniLibs.pickFirsts.add("lib/**/libc++_shared.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("org.videolan.android:libvlc-all:3.5.1")
    implementation("com.google.android.material:material:1.6.0")
}