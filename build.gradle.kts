plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "6.25.0"
    id("jacoco")
}

application {
    mainClass.set("ca.lajthabalazs.Main")
}

group = "ca.lajthabalazs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
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
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "1.0".toBigDecimal()
            }
        }
        rule {
            element = "PACKAGE"
            excludes = listOf(
                "ca.lajthabalazs.ui.view.*",
                "ca.lajthabalazs.main.*"
            )
            limit {
                minimum = "1.0".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "ca.lajthabalazs.ui.view.*",
                "ca.lajthabalazs.main.*"
            )
            limit {
                minimum = "1.0".toBigDecimal()
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