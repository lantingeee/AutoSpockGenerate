plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.16.0"
  id("org.jetbrains.kotlin.jvm") version "1.9.20"
}

group = "org.open"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.1.5")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf(
  "com.intellij.java"
  /* Plugin Dependencies */))
}

dependencies {
  // 示例：添加 Groovy 作为运行时依赖
//  runtimeOnly("org.codehaus.groovy:groovy-all:3.0.9")

  // 如果您的插件需要在运行时使用 Groovy PSI API
  // 注意：对于 IntelliJ IDEA 插件开发，通常不需要这样做，因为 Groovy 支持由 IDEA 平台提供
}


tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("231")
    untilBuild.set("241.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
