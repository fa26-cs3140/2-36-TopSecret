plugins {
    id("java")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

tasks.test {
    useJUnitPlatform()
    // Lets Mockito run on newer Java versions (21 to 26)
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Dnet.bytebuddy.experimental=true")
}

application {
    mainClass.set("TopSecret")
}

tasks.jar {
    archiveFileName.set("TopSecret.jar")
    manifest { attributes["Main-Class"] = "TopSecret" }
    // Bundle the SQLite driver so "java -jar TopSecret.jar" can open the database
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
