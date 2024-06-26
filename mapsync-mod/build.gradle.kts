plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
}

version = "${rootProject.extra["mod_version"]}-${project.extra["minecraft_version"]}"
group = "${rootProject.extra["maven_group"]}.mod.fabric"

base {
    archivesName.set(project.extra["archives_base_name"] as String)
}

loom {
    accessWidenerPath = file("src/main/resources/mapsync.accesswidener")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.extra["minecraft_version"]}")
    loom {
        @Suppress("UnstableApiUsage")
        mappings(layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${project.extra["minecraft_version"]}:${project.extra["parchment_version"]}@zip")
        })
    }

    modImplementation("net.fabricmc:fabric-loader:${project.extra["fabric_loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.extra["fabric_api_version"]}")

    // https://modrinth.com/mod/modmenu/version/9.0.0
    "maven.modrinth:modmenu:sjtVVlsA".also {
        modCompileOnly(it)
        modLocalRuntime(it)
    }

    // https://modrinth.com/mod/voxelmap-updated/version/1.20.4-1.12.17
    "maven.modrinth:voxelmap-updated:VYowiAJp".also {
        modCompileOnly(it)
        //modLocalRuntime(it) // Uncomment when you want to test VoxelMap
    }

    // https://modrinth.com/mod/journeymap/version/1.20.4-5.9.28-fabric
    "maven.modrinth:journeymap:mMICqfH9".also {
        modCompileOnly(it)
        //modLocalRuntime(it) // Uncomment when you want to test JourneyMap
    }
}

repositories {
    maven(url = "https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    maven(url = "https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    jar {
        from(file("../LICENSE")) {
            rename { "${it}_${project.extra["mod_name"]}" }
        }
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "mod_name" to project.extra["mod_name"],
                "mod_version" to project.version,
                "mod_description" to project.extra["mod_description"],
                "copyright_licence" to project.extra["copyright_licence"],

                "mod_home_url" to project.extra["mod_home_url"],
                "mod_source_url" to project.extra["mod_source_url"],
                "mod_issues_url" to project.extra["mod_issues_url"],

                "minecraft_version" to project.extra["minecraft_version"],
                "fabric_loader_version" to project.extra["fabric_loader_version"],
            )
            filter {
                it.replace(
                    "\"%FABRIC_AUTHORS_ARRAY%\"",
                    groovy.json.JsonBuilder((project.extra["mod_authors"] as String).split(",")).toString()
                )
            }
        }
        filesMatching("constants/mapsync/MOD_VERSION") {
            expand("mod_version" to project.version)
        }
    }
}
