plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.notes_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.notes_app"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation ("com.github.timonknispel:KTLoadingButton:1.2.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    implementation ("androidx.work:work-runtime:2.10.0")

    implementation("androidx.navigation:navigation-fragment:2.8.5")
    implementation("androidx.navigation:navigation-ui:2.8.5")
    implementation("androidx.activity:activity:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Calligraphy
    implementation("io.github.inflationx:calligraphy3:3.1.1")
    implementation("io.github.inflationx:viewpump:2.0.3")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Design
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    implementation ("com.google.guava:guava:33.3.1-android")
    // Scalable Size Unit
    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.intuit.ssp:ssp-android:1.0.6")

    // Rounded ImageView
    implementation("com.makeramen:roundedimageview:2.3.0")

    // Material Calendar View
    implementation ("com.applandeo:material-calendar-view:1.9.2")

    // Paging
    implementation("androidx.paging:paging-compose:3.3.5")

    implementation ("net.danlew:android.joda:2.10.13")

    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.material3:material3")
    implementation ("androidx.compose.material:material-icons-extended")

    // Room Database
    implementation ("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")

    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation ("com.getbase:floatingactionbutton:1.10.1")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation ("androidx.navigation:navigation-ui-ktx:2.8.5")

    implementation ("com.github.st235:expandablebottombar:1.5.4")
    implementation ("nl.joery.animatedbottombar:library:1.1.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation ("de.hdodenhof:circleimageview:3.0.0")
    implementation ("io.github.medyo:android-about-page:2.0.0")
}
