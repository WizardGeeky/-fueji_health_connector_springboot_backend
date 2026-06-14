plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.healthconnector"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // Required for Spring AI milestone/RC artifacts
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

extra["springAiVersion"]    = "1.0.0-M6"
extra["jjwtVersion"]        = "0.12.6"
extra["mapstructVersion"]   = "1.6.3"
extra["resilience4jVersion"]= "2.2.0"

dependencies {
    // ── Spring Boot Core ─────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // ── Spring AI — Gemini (Google Cloud Vertex AI) ──────────────────
    implementation("org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter")

    // ── JWT ──────────────────────────────────────────────────────────
    implementation("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")

    // ── Swagger / OpenAPI 3 ──────────────────────────────────────────
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

    // ── MapStruct ────────────────────────────────────────────────────
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")

    // ── Resilience4j ─────────────────────────────────────────────────
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")

    // ── Cache — Caffeine ─────────────────────────────────────────────
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // ── Metrics / Prometheus ─────────────────────────────────────────
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── Apache Commons ───────────────────────────────────────────────
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("commons-codec:commons-codec:1.17.1")

    // ── Structured Logging ───────────────────────────────────────────
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // ── Lombok ───────────────────────────────────────────────────────
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ── Dev Tools ────────────────────────────────────────────────────
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ── Testing ──────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-parameters",
        "-Xlint:-deprecation"
    ))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
