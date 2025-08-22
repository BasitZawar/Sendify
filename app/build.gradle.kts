plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-android")
    id("androidx.navigation.safeargs")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}


android {
    namespace = "com.smartswitch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phoneclone.quickshare.sendanywhere.smartswitch.mobiletransfer.sendit"
        minSdk = 28
        targetSdk = 35
        versionCode = 11
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty(
            "archivesBaseName",
            "Smart Switch" + "_vc_" + versionCode + "_vn_" + versionName + "_"
        )
    }
    signingConfigs {
//        create("release") {
//            storeFile = file("G:\\Sendify\\Sendify\\app\\sendit.jks")
//            storePassword = "sendit"
//            keyAlias = "sendit"
//            keyPassword = "sendit"
//        }
    }
    buildTypes {
        debug {
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")


            resValue("string", "app_open_on_resume", "ca-app-pub-3940256099942544/9257395921")
            resValue("string", "app_open_splash", "ca-app-pub-3940256099942544/9257395921")
            resValue("string", "banner_all", "ca-app-pub-3940256099942544/9214589741")
            resValue("string", "collaps_banner_home", "ca-app-pub-3940256099942544/2014213617")
            resValue("string", "exit_screen_native", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "inter_all", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "inter_send_receive_button", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "language_screen_inter", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "language_screen_native", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_home", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_intro_screen", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "native_splash", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "premium_screen_inter", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "transfer_complete_inter" ,"ca-app-pub-3940256099942544/1033173712" )
        }



        release {
            isDebuggable = false
//            signingConfig = signingConfigs.getByName("release")
            resValue("string", "admob_app_id", "ca-app-pub-2493449427846338~1728402649")
            resValue("string", "app_open_on_resume", "ca-app-pub-2493449427846338/6383939312")
            resValue("string", "app_open_splash", "ca-app-pub-2493449427846338/4715634427")
            resValue("string", "banner_all", "ca-app-pub-2493449427846338/2587022898")
            resValue("string", "collaps_banner_home", "ca-app-pub-2493449427846338/7849305477")
            resValue("string", "exit_screen_native", "ca-app-pub-2493449427846338/5624249777")
            resValue("string", "inter_all", "ca-app-pub-2493449427846338/8401638110")
            resValue("string", "inter_send_receive_button", "ca-app-pub-2493449427846338/2466769246")
            resValue("string", "language_screen_inter", "ca-app-pub-2493449427846338/1379024642")
            resValue("string", "language_screen_native", "ca-app-pub-2493449427846338/5476075966")
            resValue("string", "native_home", "ca-app-pub-2493449427846338/5223142133")
            resValue("string", "native_intro_screen", "ca-app-pub-2493449427846338/7936694134")
            resValue("string", "native_splash", "ca-app-pub-2493449427846338/6919367182")
            resValue("string", "premium_screen_inter", "ca-app-pub-2493449427846338/3971422602")
            resValue("string", "transfer_complete_inter" ,"ca-app-pub-2493449427846338/8317861724")

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

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.1")

    //hilt
    implementation("com.google.dagger:hilt-android:2.49")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")

    //sdp ssp
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    //coil
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    //lottie
    implementation("com.airbnb.android:lottie:6.4.0")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // for language
    implementation("com.zeugmasolutions.localehelper:locale-helper-android:1.5.1")
    // implementation ("com.android.support:multidex:1.0.3")

    //Ads
    implementation(libs.play.services.ads)

    implementation("com.google.firebase:firebase-analytics:22.0.2")
    implementation("com.google.firebase:firebase-crashlytics:19.0.3")
    implementation ("com.google.firebase:firebase-messaging:24.1.0")
  //  implementation ("com.google.firebase:firebase-config-ktx:21.3.0")


    //billing
    implementation(libs.billing)

    implementation ("androidx.multidex:multidex:2.0.1")
    implementation ("com.facebook.shimmer:shimmer:0.5.0")
}