rootProject.name = "OzonePlugins"

/*
include(":Cooking")
include(":OzoneWintertodt")
include(":OzoneCannon")
include(":PestControl")
include("OzoneAutoSpec")
include("OzoneTempoross")
include("OzonePrayer")
include("unethical-zulrah")
include("OzoneZulrah")
include("KyleeZulrah")
include("OzoneTesting")
 */
include("unethical-zulrah")
include("OzoneTOA")

for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "${name.toLowerCase()}.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}

