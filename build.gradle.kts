buildscript {

    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", extra.get("kotlinVersion") as String))
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
