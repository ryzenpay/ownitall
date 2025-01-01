plugins {
    id("application")
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

    // spotify wrapper
    implementation("se.michaelthelin.spotify:spotify-web-api-java:9.1.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

application {
    mainClass = "ryzen.ownitall.Main"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in` //to allow scanner
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    ignoreFailures = true
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
