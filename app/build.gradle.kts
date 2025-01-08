    plugins {
        id("application")
        id("java")
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
        // audio metadata
        implementation("net.jthink:jaudiotagger:3.0.1")

        //google api
        implementation("com.google.api-client:google-api-client:1.23.0")
        implementation("com.google.oauth-client:google-oauth-client-jetty:1.23.0")
        implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")

        //json
        implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
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

    tasks.javadoc {
        destinationDir = file("$rootDir/documentation")
        (options as StandardJavadocDocletOptions).apply {
            isAuthor = true
            isVersion = true
            encoding = "UTF-8"
            charSet = "UTF-8"
            windowTitle = "Own It All API"
            header = "<b>Own It All</b>"
            docTitle = "Own It All java documentation"
        }
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
