buildscript {
    ext {
        compose_version = "2023.08.00"
        nav_compose_version = "2.7.5"
        hilt_version = "2.48"
        kotlin_version = "1.8.22"
    }
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
    }
}