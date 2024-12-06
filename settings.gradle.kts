pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") // Add JitPack here for plugin resolution if ne
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven{url = uri("https://jitpack.io") }// Add JitPack here for dependency resolution
        maven{url = uri ("https://storage.zego.im/maven" ) }  // <- Add this line.
        maven {url = uri( "https://www.jitpack.io") } // <- Add this line.
    }
}

rootProject.name = "Soul Mate"
include(":app")
