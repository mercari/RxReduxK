object Artifact {
  const val groupdId = "com.mercari.rxredux"
  const val version = "1.0.0-rc6"
}

object MavenUrl {
  const val spekDev = "https://dl.bintray.com/spekframework/spek-dev"
}

object Version {
  const val bintray = "1.8.4"
  const val kotlin = "1.3.30"
  const val rxJava = "2.2.19"
  const val kluent = "1.60"
  const val spek = "2.0.10"
  const val jacoco = "0.8.4"
}

object Classpath {
  const val kotlin = "gradle-plugin"
  const val bintray = "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Version.bintray}"
}

object Dependencies {
  const val kotlin = "stdlib"
  const val rxJava = "io.reactivex.rxjava2:rxjava:${Version.rxJava}"
}

object TestDependencies {
  const val kluent = "org.amshove.kluent:kluent-android:${Version.kluent}"
  const val spek = "org.spekframework.spek2:spek-dsl-jvm:${Version.spek}"
  const val spekRunner = "org.spekframework.spek2:spek-runner-junit5:${Version.spek}"
}
