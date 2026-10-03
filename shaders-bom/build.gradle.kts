plugins {
    `java-platform`
    id("app.soulcramer.shaders.publishing")
    id("app.soulcramer.shaders.spotless")
}

dependencies {
    constraints {
        // Declare the dependencies to include in the BoM
        api("app.soulcramer.shaders:colorblindness:$version")
    }
}
