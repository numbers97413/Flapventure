plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Set the namespace for generated R and BuildConfig classes.
    namespace = "com.example.flapventure"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.flapventure"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Tell Gradle to also scan the "values/themes" folder for resource files.
    sourceSets {
        getByName("main") {
            // 'src/main/res' is included by default.
            // Add the additional folder:
            res.srcDirs("src/main/res", "src/main/res/values/themes")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.8.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
