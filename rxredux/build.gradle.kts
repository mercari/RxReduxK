plugins {
    kotlin("jvm")

    jacoco

    id("maven-publish")
    id("com.jfrog.bintray")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin(Dependencies.kotlin, Version.kotlin))
    implementation(Dependencies.rxJava)

    // assertion
    testImplementation(TestDependencies.kluent)

    // spek2
    testImplementation(TestDependencies.spek)
    testRuntimeOnly(TestDependencies.spekRunner)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = Version.jacoco
}

task<JacocoReport>("codeCoverageReport") {
    group = "reporting"

    reports {
        xml.isEnabled = true
        xml.destination = file("${project.buildDir}/reports/jacoco/codeCoverageReport/report.xml")
        html.isEnabled = true
    }

    val tree = fileTree("${project.buildDir}/classes")

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(tree))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/*.exec")
    })

    val test: Test by tasks
    dependsOn(test)
}

//val artifactGroupId = extra.get("artifactGroupId") as String
val artifactGroupId = Artifact.groupdId
val artifactPublishVersion = Artifact.version

group = artifactGroupId
version = artifactPublishVersion

// publishing
val sourceSets = project.the<SourceSetContainer>()

val sourcesJar by tasks.creating(Jar::class) {
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

val doc by tasks.creating(Javadoc::class) {
    isFailOnError = false
    source = sourceSets["main"].allJava
}
val javadocJar by tasks.creating(Jar::class) {
    dependsOn(doc)
    from(doc)

    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register(project.name, MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            groupId = artifactGroupId
            artifactId = project.name
            version = artifactPublishVersion
        }
    }
}

// bintray
bintray {
    user = findProperty("BINTRAY_USER") as? String
    key = findProperty("BINTRAY_KEY") as? String
    override = false
    publish = true
    setPublications(project.name)
    pkg.apply {
        repo = "maven"
        name = "rxreduxk"
        desc = "micro-framework for Redux implemented in Kotlin"
        userOrg = "mercari-inc"
        websiteUrl = "https://github.com/mercari/rxredux"
        vcsUrl = "https://github.com/mercari/rxredux"
        setLicenses("MIT")
        version.apply {
            name = artifactPublishVersion
        }
    }
}
