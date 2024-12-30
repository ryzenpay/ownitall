plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(libs.guava)
    
    // Add Hamcrest for additional assertions
    testImplementation("org.hamcrest:hamcrest:2.2")
    
    // Add Spotify Web API Java library
    implementation("se.michaelthelin.spotify:spotify-web-api-java:9.1.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "ryzen.ownitall.Main"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Configure source sets to use the correct package structure
sourceSets {
    main {
        java {
            srcDirs("src/main/java")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
    }
}
