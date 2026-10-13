plugins { id("com.android.application") }
android {
    namespace = "net.chateaulore.lcgroguesgallery"
    compileSdk = 37
    defaultConfig {
        applicationId = "net.chateaulore.lcgroguesgallery"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"
    }
}
dependencies { testImplementation("junit:junit:4.13.2") }
