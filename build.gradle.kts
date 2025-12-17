// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("maven-publish")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Publishing configuration for GitHub Packages
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.aether.wallpaper"
            artifactId = "aether"
            version = project.findProperty("versionName") as String? ?: "0.1.0-alpha"
            
            pom {
                name.set("Aether Live Wallpaper")
                description.set("GPU-accelerated Android live wallpaper with customizable particle effects")
                url.set("https://github.com/YOUR_USERNAME/Aether_lwp")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("aether")
                        name.set("Aether Team")
                    }
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "YOUR_USERNAME/Aether_lwp"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
