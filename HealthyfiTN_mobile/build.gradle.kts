// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.1" apply false  // ← CHANGÉ de 8.2.0
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false  // ← CHANGÉ de 1.9.20
}

// 🔥 Firebase / Google Services
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}
