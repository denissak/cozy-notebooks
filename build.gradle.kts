plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.cozy"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

extra["testcontainersVersion"] = "1.20.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    runtimeOnly("com.mysql:mysql-connector-j")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    implementation("com.google.api-client:google-api-client:2.7.2")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    /*
     * Mirrors README "Local-MySQL mode". Pass -Pcozy.test.useLocalMysql=true so Gradle
     * sets USE_LOCAL_MYSQL on the forked JVM (handier than exporting in Git Bash/CMD).
     * Still start MySQL first: docker compose up -d mysql
     */
    val localMysqlGradleProp =
        project.findProperty("cozy.test.useLocalMysql")?.toString()?.equals("true", ignoreCase = true) == true
    if (localMysqlGradleProp) {
        environment("USE_LOCAL_MYSQL", "true")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.register<Exec>("startLocalMysql") {
    group = "verification"
    description = "Starts local MySQL service for local integration tests."

    onlyIf {
        System.getenv("USE_LOCAL_MYSQL")?.equals("true", ignoreCase = true) == true
                || project.findProperty("cozy.test.useLocalMysql")?.toString()?.equals("true", ignoreCase = true) == true
    }

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine("cmd", "/c", "docker compose up -d mysql")
    } else {
        commandLine("sh", "-c", "docker compose up -d mysql")
    }
}
