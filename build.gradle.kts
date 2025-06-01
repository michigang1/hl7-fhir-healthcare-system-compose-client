import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.0" // Явно указываем версию Kotlin
    kotlin("plugin.serialization") version "2.0.0" // Добавляем плагин для kotlinx-serialization
    id("org.jetbrains.compose") version "1.6.10"
    id("org.jetbrains.kotlin.plugin.compose") version  "2.0.0"
}

group = "michigang1.healthcare"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // Рекомендуется обновить до последней стабильной
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")


    // Retrofit & OkHttp (рекомендуется проверить наличие более новых версий)
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Рекомендуется обновить
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // Для логирования запросов

    // Removed Moshi converter to avoid conflicts with kotlinx-serialization

    // Koin for Dependency Injection
    val koinVersion = "4.0.4"
    implementation("io.insert-koin:koin-core:$koinVersion")

    // Compose Desktop Dependencies
    implementation(compose.desktop.currentOs) // Основная зависимость, включает ui, foundation, runtime
    implementation(compose.material3)      // Рекомендуется использовать Material 3
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "hl7-fhir-healthcare-system-compose-client"
            packageVersion = "1.0.0"
        }
    }
}

kotlin {
    jvmToolchain(18)
}
