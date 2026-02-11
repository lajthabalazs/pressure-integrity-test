plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "8.2.1"
    id("jacoco")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("ca.lajthabalazs.pressure_integrity_test.main.Main")
}

group = "ca.lajthabalazs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("org.jfree:jfreechart:1.5.4")
    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    archiveBaseName.set("pressure-integrity-test")
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
}

tasks.compileJava {
    dependsOn(tasks.spotlessApply)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// Spotless configuration
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map {
            fileTree(it) {
                exclude(
                    "ca/lajthabalazs/pressure_integrity_test/main/**",
                    "ca/lajthabalazs/pressure_integrity_test/ui/**",
                    "ca/lajthabalazs/pressure_integrity_test/io/**"
                )
            }
        }
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test, tasks.jacocoTestReport)
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map {
            fileTree(it) {
                exclude(
                    "ca/lajthabalazs/pressure_integrity_test/main/**",
                    "ca/lajthabalazs/pressure_integrity_test/ui/**",
                    "ca/lajthabalazs/pressure_integrity_test/io/**"
                )
            }
        }
    )
    // Force the task to always execute by removing the default onlyIf condition
    setOnlyIf { true }
    doFirst {
        // Check if test sources exist by checking the test source set
        val testSourceSet = sourceSets.test.get()
        val testJavaSrcDirs = testSourceSet.java.srcDirs
        val hasTestSources = testJavaSrcDirs.any { dir ->
            dir.exists() && dir.walkTopDown().any { it.isFile && it.extension == "java" }
        }
        
        if (!hasTestSources) {
            throw GradleException(
                "No test sources found. Coverage verification requires tests to exist. " +
                "Please add tests to verify code coverage."
            )
        }

        val executionDataFile = layout.buildDirectory.file("jacoco/test.exec").get().asFile
        if (!executionDataFile.exists()) {
            throw GradleException(
                "No coverage data found at ${executionDataFile.path}. " +
                "Tests must be run and produce coverage data. " +
                "Coverage verification requires at least one test execution."
            )
        }
    }
    violationRules {
        isFailOnViolation = true
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = BigDecimal("1.0")
            }
        }
        rule {
            element = "PACKAGE"
            excludes = listOf(
                "ca.lajthabalazs.pressure_integrity_test.main.*",
                "ca.lajthabalazs.pressure_integrity_test.ui.*",
                "ca.lajthabalazs.pressure_integrity_test.io.*"
            )
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = BigDecimal("1.0")
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "ca.lajthabalazs.pressure_integrity_test.main.*",
                "ca.lajthabalazs.pressure_integrity_test.ui.*",
                "ca.lajthabalazs.pressure_integrity_test.io.*"
            )
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = BigDecimal("1.0")
            }
        }
        // Explicit coverage requirement for LeakageMeasurementVectorStream
        rule {
            element = "CLASS"
            includes = listOf(
                "ca.lajthabalazs.pressure_integrity_test.measurement.processing.LeakageMeasurementVectorStream"
            )
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = BigDecimal("1.0")
            }
        }
    }
}

// Task to create a Windows app-image (directory with .exe launcher + bundled JRE via jpackage)
tasks.register<Exec>("packageExe") {
    dependsOn(tasks.jar)

    val appName = "pressure-integrity-test"
    // jpackage requires a numeric version, so strip any suffix like "-SNAPSHOT"
    val appVersion = project.version.toString().substringBefore("-")
    val jarFileName = "${appName}-${project.version}.jar"

    val buildDirPath = layout.buildDirectory.get().asFile.path
    // Clean existing app-image directory, since this jpackage version doesn't support --force
    val imageDir = file("$buildDirPath/dist/$appName")
    project.delete(imageDir)

    commandLine(
        "jpackage",
        "--win-console",
        "--type", "app-image",
        "--input", "$buildDirPath/libs",
        "--dest", "$buildDirPath/dist",
        "--name", appName,
        "--main-jar", jarFileName,
        "--main-class", application.mainClass.get(),
        "--app-version", appVersion
    )
}

// Task to zip the generated app-image for distribution
tasks.register<Zip>("packageZip") {
    dependsOn(tasks.named("packageExe"))

    val appName = "pressure-integrity-test"
    val appVersion = project.version.toString().substringBefore("-")

    val buildDirPath = layout.buildDirectory.get().asFile.path
    val imageDir = file("$buildDirPath/dist/$appName")

    from(imageDir)
    archiveBaseName.set(appName)
    archiveVersion.set(appVersion)
    destinationDirectory.set(file("$buildDirPath/dist"))
}

// Task to ensure check runs all validations (including formatting)
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
    dependsOn(tasks.spotlessCheck)
}