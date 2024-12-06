// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.3")
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
        classpath ("com.android.tools.build:gradle:7.0.4")
    }
}

plugins {
    id ("com.android.application") version "8.2.0" apply false
    id ("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id ("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    // Add the dependency for the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}
