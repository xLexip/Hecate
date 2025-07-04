import org.gradle.api.JavaVersion.VERSION_23

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "dev.lexip.hecate"
	compileSdk = 36

	defaultConfig {
		applicationId = "dev.lexip.hecate"
		minSdk = 31
		targetSdk = 36
		versionCode = 1
		versionName = "0.1.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = VERSION_23
		targetCompatibility = VERSION_23
	}
	kotlinOptions {
		jvmTarget = "23"
	}
	buildFeatures {
		compose = true
	}
	buildToolsVersion = "35.0.0"
	sourceSets {
		getByName("main") {
			resources {
				srcDirs("src/main/resources", "src/main/java/components")
			}
		}
	}
}

dependencies {
	implementation(libs.androidx.localbroadcastmanager)
	implementation(libs.androidx.core.splashscreen.v100)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.viewmodel.compose)
	implementation(libs.androidx.material3)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.material)
	implementation(platform(libs.androidx.compose.bom))
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)
}