plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    checkstyle
    //id("com.github.spotbugs") version "6.2.1"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.postgresql)
    implementation(libs.jackson)
    implementation(libs.jsqlparser)
    implementation(libs.slf4j)

    // Use TestNG framework, also requires calling test.useTestNG() below
    testImplementation(libs.testng)
    testImplementation(libs.testcontainers)
    testImplementation(libs.postgresql)
    testImplementation(libs.slf4jdk)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

checkstyle {
    toolVersion = "10.26.1"
}

tasks.named<Test>("test") {
    // Use TestNG for unit tests.
    useTestNG()
    systemProperty(
        "java.util.logging.config.file",
        "${projectDir}/src/test/resources/logging.properties"
    )
    systemProperty(
        "user.timezone",
        "Europe/Kyiv"
    )
    testLogging {
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

}

tasks.withType<Checkstyle>().configureEach {
    configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
    reports {
        xml.required = true
        html.required = true
    }
}

