buildscript {

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", extra.get("kotlinVersion") as String))
        classpath("org.junit.platform:junit-platform-gradle-plugin:${extra.get("junitPlatformVersion")}")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:${extra.get("bintrayVersion")}")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { setUrl("https://dl.bintray.com/spekframework/spek-dev") }
    }
}

tasks.create<Wrapper>("wrapper") {
   gradleVersion = "4.10.2"
}
