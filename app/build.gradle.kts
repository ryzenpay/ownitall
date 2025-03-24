import org.gradle.jvm.tasks.Jar

plugins {
    id("application")
    id("java")
    //vaadin https://github.com/vaadin/base-starter-spring-gradle
    //id("com.vaadin") version "24.7.0"
}

repositories {
    mavenCentral()
}

dependencies {
    //testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(libs.guava)
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

    //json + web scraping
    implementation("org.jsoup:jsoup:1.18.3")

    //logging
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    //progress bar
    implementation("me.tongfei:progressbar:0.10.1")

    //vaadin (GUI)
    //implementation("com.vaadin:vaadin-spring-boot-starter:24.7.0")
}

// vaadin {
//     optimizeBundle = false
//     productionMode = false
// }

// tasks.register<Jar>("web") {
//     dependsOn("vaadinBuildFrontend")
//     archiveBaseName.set("web")
//     archiveVersion.set("")
//     archiveClassifier.set("")
//     from(sourceSets.main.get().output)
// }
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
