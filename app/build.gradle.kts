import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val extensionClass = "LastFM"
val id = "lastfm"
val name = "LastFM"
val version = "1.0.0"
val description = "LastFM Extension for Echo"
val author = "rebelonion"
val iconUrl: String = "https://i.imgur.com/ArKIJYv.png"

android {
    namespace = "dev.rebelonion.echo.extension"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.rebelonion.echo.extension.lastfm"
        minSdk = 24
        targetSdk = 34

        versionCode = 1
        versionName = version

        resValue("string", "app_name", "Echo : $name Extension")
        resValue("string", "class_path", "$namespace.$extensionClass")
        resValue("string", "name", name)
        resValue("string", "id", id)
        resValue("string", "version", version)
        resValue("string", "description", description)
        resValue("string", "author", author)
        resValue("string", "icon_url", iconUrl)

        val properties = Properties().apply {
            load(project.rootProject.file("local.properties").inputStream())
        }

        buildConfigField("String", "API_KEY", "\"${properties.getProperty("API_KEY")}\"")
        buildConfigField("String", "API_SECRET", "\"${properties.getProperty("API_SECRET")}\"")
        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }



    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    @Suppress("UnstableApiUsage") testOptions {
        unitTests {
            this.isReturnDefaultValues = true
        }
    }
}


dependencies {
    val libVersion = "38e1df03f6"
    compileOnly("com.github.brahmkshatriya:echo:$libVersion")
    implementation("com.github.Blatzar:NiceHttp:0.4.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1-Beta")
    testImplementation("com.github.brahmkshatriya:echo:$libVersion")
}