plugins { id("com.android.application") }
android {
    namespace = "net.chateaulore.lcgroguesgallery"
    compileSdk = 37
    defaultConfig {
        applicationId = "net.chateaulore.lcgroguesgallery"
        minSdk = 26
        targetSdk = 37
        versionCode = 10
        versionName = "1.0-rc1"
    }
}
dependencies { testImplementation("junit:junit:4.13.2") }
