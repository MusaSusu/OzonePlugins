project.extra["PluginName"] = "Ozone Zulrah"
project.extra["PluginDescription"] = "Does zulrah for you"

dependencies {
    implementation(project(mapOf("path" to ":OzoneUtils")))
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                "Plugin-Version" to project.version,
                "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                "Plugin-Provider" to project.extra["PluginProvider"],
                "Plugin-Description" to project.extra["PluginDescription"],
                "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}