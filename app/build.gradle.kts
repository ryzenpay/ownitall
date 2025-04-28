plugins {
    id("application")
    id("java")
    // web gui
    id("org.springframework.boot") version "3.4.4"
}

repositories {
    mavenCentral()
}

dependencies {
    //testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.hamcrest:hamcrest:2.2")

    // spotify wrapper
    implementation("se.michaelthelin.spotify:spotify-web-api-java:9.1.0")

    // audio metadata: https://bitbucket.org/ijabz/jaudiotagger/src/master/ | http://www.jthink.net/jaudiotagger/
    implementation("net.jthink:jaudiotagger:3.0.1")

    //google api
    implementation("com.google.api-client:google-api-client:1.23.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.23.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")

    //json
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    //logging
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    //progress bar
    implementation("me.tongfei:progressbar:0.10.1")

    //cli input options (common cli)
    implementation("commons-cli:commons-cli:1.9.0")

    //web gui (spring boot) https://docs.spring.io/spring-boot/tutorial/first-application/index.html
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.4")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:3.4.4")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.4.4")
    //https://docs.spring.io/spring-boot/how-to/logging.html
    implementation("org.springframework.boot:spring-boot-starter-log4j2:3.4.4")
    modules {
		module("org.springframework.boot:spring-boot-starter-logging") {
			replacedBy("org.springframework.boot:spring-boot-starter-log4j2", "Use Log4j2 instead of Logback")
		}
	}
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "ryzen.ownitall.Main"
}

// remove this to not have any logs in terminal
tasks.withType<JavaExec> {
    systemProperty("consoleLogLevel", "INFO")
}
tasks.named<JavaExec>("run") {
    standardInput = System.`in` //to allow scanner
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    ignoreFailures = true
}

tasks.javadoc {
    destinationDir = file("$rootDir/docs")
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
//TODO: needs fixes, not using all imported classes
//example: jackson with duration
tasks.register<Jar>("compile") {
    archiveClassifier.set("uber")
    archiveFileName.set("ownitall.jar")
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })

    manifest {
        attributes(mapOf(
            "Main-Class" to application.mainClass.get()
        ))
    }

    destinationDirectory.set(rootDir)

    // Add this line to handle duplicate entries
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "consoleLogLevel" to "OFF"
        )
    }
}
