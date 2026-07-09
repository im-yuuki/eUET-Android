plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.june8th.euet"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "me.june8th.euet"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("debug")
        multiDexEnabled = true
    }

    buildTypes {
        release {
            versionNameSuffix = "-release"
            optimization {
                enable = false
            }
        }
        getByName("debug") {
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}