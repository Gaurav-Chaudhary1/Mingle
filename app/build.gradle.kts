plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.soulmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.soulmate"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
//    implementation ("com.android.support:design:34.0.0")

    //Firebase Authentication library
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    //Facebook
    implementation ("com.facebook.android:facebook-login:17.0.1")

    // Add the dependencies for the Crashlytics and Analytics libraries
    implementation("com.google.firebase:firebase-crashlytics:19.2.1")
    implementation("com.google.firebase:firebase-analytics:22.1.2")

    // Add the dependency for the Firebase Authentication library
    implementation("com.google.firebase:firebase-auth:23.1.0")

    // Declare the dependency for the Cloud Firestore library
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")

    //Firebase Storage
    implementation ("com.google.firebase:firebase-storage:21.0.1")
    implementation ("com.google.firebase:firebase-firestore-ktx:25.1.1")
    implementation ("com.google.firebase:firebase-storage-ktx:21.0.1")

    //Firebase Realtime Database
    implementation ("com.google.firebase:firebase-database:21.0.0")

    //Firebase Cloud Messaging
    implementation ("com.google.firebase:firebase-messaging:24.1.0")

    //Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    //Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")

    //OkHttp
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")

    //Glide Image Loading
    implementation ("com.github.bumptech.glide:glide:5.0.0-rc01")
    ksp ("com.github.bumptech.glide:ksp:5.0.0-rc01")

    //GeoLocation
    implementation ("com.google.android.gms:play-services-location:21.3.0")

    //Kotlin Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    //Navigation Component
    implementation ("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation ("androidx.navigation:navigation-ui-ktx:2.8.4")

    //Lottie Animation
    implementation ("com.airbnb.android:lottie:6.6.0")

    //View Model & Live Data
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    //Pagination
    implementation("androidx.paging:paging-runtime-ktx:3.3.4")

    //Easy Permissions
    // For developers using AndroidX in their applications
    implementation ("pub.devrel:easypermissions:3.0.0")

    //For billing Subscriptions
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    //Country code picker
    implementation ("com.hbb20:ccp:2.7.0")

    //country picker
    implementation ("com.hbb20:android-country-picker:0.0.7")

    //Circle imageView
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    //Jetpack ExoPlayer
    implementation ("androidx.media3:media3-exoplayer:1.4.1")
    implementation ("androidx.media3:media3-ui:1.4.1")
    implementation ("androidx.media3:media3-extractor:1.4.1")

    //Lottie Animation
    implementation ("com.airbnb.android:lottie:6.6.0")

    //SwipeableCardStackView
    implementation ("com.github.yuyakaido:cardstackview:2.3.4")

    //work manager
    implementation ("androidx.work:work-runtime-ktx:2.10.0")

    implementation ("com.github.ZEGOCLOUD:zego_uikit_prebuilt_call_android:+")
    implementation ("com.github.ZEGOCLOUD:zego_uikit_signaling_plugin_android:+")
}