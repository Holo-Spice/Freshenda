import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val releaseSigning = Properties().apply {
    val propertiesFile = rootProject.file("signing.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use(::load)
}

android {
    namespace = "com.cake.freshenda"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cake.freshenda"
        minSdk = 26
        targetSdk = 37
        versionCode = 7
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = releaseSigning.getProperty("storeFile")?.let { rootProject.file(it) }
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("generateUpdateMetadata") {
    val updateMetadata = layout.buildDirectory.file("outputs/update/update.json").get().asFile
    val updateNotes = layout.projectDirectory.file("update-notes.txt").asFile
    val updateDetails = layout.projectDirectory.file("update-details.txt").asFile
    val updateVersionCode = requireNotNull(android.defaultConfig.versionCode)
    val updateVersionName = requireNotNull(android.defaultConfig.versionName)
    val updateMinSdk = requireNotNull(android.defaultConfig.minSdk)
    group = "distribution"
    description = "Generate update.json to upload alongside the APK in the matching GitHub Release."
    inputs.property("versionCode", updateVersionCode)
    inputs.property("versionName", updateVersionName)
    inputs.property("minSdk", updateMinSdk)
    inputs.file(updateNotes)
    inputs.file(updateDetails)
    outputs.file(updateMetadata)
    doLast {
        val notes = updateNotes.readLines().map(String::trim).filter(String::isNotEmpty)
        val details = updateDetails.readLines().map(String::trim).filter(String::isNotEmpty)
        require(notes.size <= 50 && notes.all { it.length <= 2_000 }) { "Update notes exceed supported limits" }
        require(details.size <= 50 && details.all { it.length <= 4_000 }) { "Update details exceed supported limits" }
        val metadata = linkedMapOf(
            "schemaVersion" to 1,
            "versionCode" to updateVersionCode,
            "versionName" to updateVersionName,
            "minSdk" to updateMinSdk,
            "releaseUrl" to "https://github.com/Holo-Spice/Freshenda/releases/tag/v$updateVersionName",
            "notes" to notes,
            "details" to details,
        )
        updateMetadata.apply {
            parentFile.mkdirs()
            writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(metadata)) + "\n")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
