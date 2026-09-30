plugins {
    alias(libs.plugins.kotlin.mpp)
    alias(libs.plugins.serialization)
}

group = "com.justnopoint"
version = "1.0-SNAPSHOT"
val nativelibs = "C:\\mingw64"

repositories {
    mavenCentral()
}

kotlin {
    mingwX64 {
        binaries {
            executable("arcanaView") {
                entryPoint = "com.justnopoint.arcana.main"
                linkerOpts = mutableListOf(//"$resFile",
                    "-L${nativelibs}\\lib",
                    "-L${nativelibs}\\bin", "-L${project.projectDir}\\native\\lib",
                    "-lmingw32", "-lSDL2main", "-lSDL2_ttf", "-lSDL2", "-lpng", "-lz",
                    "-mwindows")
                runTaskProvider?.configure {
                    args("-v")
                }
            }
            executable("pk3util") {
                entryPoint = "com.justnopoint.arcana.util.pk3util"
                linkerOpts = mutableListOf(//"$resFile",
                    "-L${nativelibs}\\lib",
                    "-L${nativelibs}\\bin", "-L${project.projectDir}\\native\\lib",
                    "-lmingw32", "-lSDL2main", "-lSDL2_ttf", "-lSDL2", "-lpng", "-lz",
                    "-mwindows")
            }
        }

        compilations.getByName("main").cinterops {
            val SDL by creating {
                includeDirs {
                    allHeaders("${nativelibs}\\include")
                }
                extraOpts = mutableListOf(
                    "-libraryPath", "${nativelibs}\\lib",
                    "-libraryPath", "${nativelibs}\\bin"
                )
            }
            val libpng by creating {
                extraOpts = mutableListOf("-libraryPath", "${nativelibs}\\lib",
                    "-libraryPath", "${nativelibs}\\bin")
                includeDirs("${nativelibs}\\include")
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        commonMain {
            dependencies {
                implementation(project.dependencies.platform(libs.okio.bom))
                implementation(libs.okio)
                implementation(libs.serialization.json)
                implementation(libs.serialization.json.okio)
                implementation(libs.coroutines)
                implementation(libs.cli)
            }
        }
    }
}