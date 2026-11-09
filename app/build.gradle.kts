plugins {
    alias(libs.plugins.android.application)
    // 1. 如果你使用了 Firebase，通常也需要加上 Google Services 插件（视项目根目录配置而定）
    // id("com.google.gms.google-services")
}

android {
    namespace = "com.example.umborrowbuddy"
    compileSdk = 34 // 修正：标准写法通常为数字，或保持你的编译版本

    defaultConfig {
        applicationId = "com.example.umborrowbuddy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 简化打包混淆配置，避免代码被错误裁剪
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // 基础 AndroidX 与 UI 组件
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // ==========================================
    // Module 3 核心依赖：Firebase、CameraX、ML Kit
    // ==========================================

    // 1. Firebase BoM（统一管理 Firebase 各组件版本，防止版本冲突）
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // 2. CameraX 核心库（用于开启相机实现二维码实时扫描预览）
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // 3. Google ML Kit 扫码库（用于识别饮水机上的二维码/条形码内容）
    implementation("com.google.mlkit:barcode-scanning:18.3.1")

    // 单元测试与仪器测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}