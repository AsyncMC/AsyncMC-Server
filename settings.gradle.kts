rootProject.name = "asyncmc-server"
include(":module:remote-world-generator")
include(":module:world-ge")
include(":module:remote-world-generator:remote-world-gen-server-paper-nms")
include(":module:remote-world-generator:remote-world-gen-server-paper")
include(":module:remote-world-generator:remote-world-gen-client-powernukkit")
//includeBuild("module/remote-world-generator/remote-world-gen-server-fabric")
includeBuild("module/remote-world-generator/remote-world-gen-data")
