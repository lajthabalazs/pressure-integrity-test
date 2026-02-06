plugins {
    id("java")
    id("application")
    id("com.diffplug.spotless") version "6.25.0"
    id("jacoco")
    id("com.github.spotbugs") version "6.0.22"
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
                minimum = "1.0".toBigDecimal() // 100% coverage
            }
        }
        rule {
            element = "PACKAGE"
            excludes = listOf(
                "ca.lajthabalazs.ui.view.*",
                "ca.lajthabalazs.main.*"
            )
            limit {
                minimum = "1.0".toBigDecimal() // 100% coverage for packages
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "ca.lajthabalazs.ui.view.*",
                "ca.lajthabalazs.main.*"
            )
            limit {
                minimum = "1.0".toBigDecimal() // 100% coverage for classes
            }
        }
    }
}

// SpotBugs configuration
spotbugs {
    toolVersion.set("4.8.3")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    val taskName = name
    reports.create("html") {
        required.set(true)
        outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${taskName}.html"))
    }
    reports.create("xml") {
        required.set(true)
        outputLocation.set(layout.buildDirectory.file("reports/spotbugs/${taskName}.xml"))
    }
}

// Task to ensure check runs all validations
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
    dependsOn(tasks.spotbugsMain)
    dependsOn(tasks.spotbugsTest)
}