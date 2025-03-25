plugins {
  java
  id ("org.springframework.boot") version "2.5.6"
  id ("io.spring.dependency-management") version "1.1.7"

  // Code quality plugins
  checkstyle
  jacoco
  id("org.sonarqube") version "4.0.0.2929"
}

group = "uk.nhs.hee.tis.revalidation"
version = "0.4.2"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

repositories {
  mavenCentral()

  maven {
    url = uri("https://hee-430723991443.d.codeartifact.eu-west-1.amazonaws.com/maven/Health-Education-England/")
    credentials {
      username = "aws"
      password = System.getenv("CODEARTIFACT_AUTH_TOKEN")
    }
  }
}

dependencies {
  // Spring Boot starters
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web")
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }

  // Apache Camel
  implementation(platform("org.apache.camel.springboot:camel-spring-boot-bom:3.20.5"))
  implementation("org.apache.camel.springboot:camel-spring-boot-starter")
  implementation("org.apache.camel.springboot:camel-http-starter")
  implementation("org.apache.camel.springboot:camel-servlet-starter")
  implementation("org.apache.camel.springboot:camel-jackson-starter")
  implementation("org.apache.camel.springboot:camel-aws-xray-starter")

  // Lombok
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  // MapStruct
  implementation("org.mapstruct:mapstruct:1.4.2.Final")
  annotationProcessor("org.mapstruct:mapstruct-processor:1.4.2.Final")
  testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.4.2.Final")

  // Sentry reporting
  val sentryVersion = "5.3.0"
  implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
  implementation("io.sentry:sentry-logback:$sentryVersion")

  // Keycloak
  implementation("com.transformuk.hee:keycloak-client:3.0.0")

  //RabbitMQ
  implementation("org.springframework.boot:spring-boot-starter-amqp")

  // AWS cloud
  val cloudVersion = "2.3.2"
  implementation("io.awspring.cloud:spring-cloud-starter-aws:$cloudVersion")
  implementation("io.awspring.cloud:spring-cloud-starter-aws-messaging:$cloudVersion")

  // Elastic Search
  implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

  // Java Persistence
  implementation("javax.persistence:javax.persistence-api:2.2")

  // Jackson Json
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")

  // Faker
  implementation("com.github.javafaker:javafaker:1.0.2")

  // MongoDB
  val mongoDriverVersion = "4.5.1"
  implementation("org.mongodb:mongodb-driver-core:$mongoDriverVersion")
  implementation("org.mongodb:bson:$mongoDriverVersion")
}

checkstyle {
  toolVersion = "8.29"
  val archive = configurations.checkstyle.get().resolve().filter {
    it.name.startsWith("checkstyle")
  }
  config = resources.text.fromArchiveEntry(archive, "google_checks.xml")
}

tasks.jacocoTestReport {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}

sonarqube {
  properties {
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.login", System.getenv("SONAR_TOKEN"))
    property("sonar.organization", "health-education-england")
    property("sonar.projectKey", "Health-Education-England_tis-revalidation-integration")

    property("sonar.java.checkstyle.reportPaths",
      "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml")
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
  useJUnitPlatform()
}
