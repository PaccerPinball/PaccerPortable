package de.lookonthebrightsi.paccerportable

import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FileUtils
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import java.io.File
import java.net.URL
import java.nio.file.Files

/** Cache the arduino download zip/extracted dir and the LiquidCrystal zip? */
const val USE_CACHE = true

const val ARDUINO_PORTABLE = "https://downloads.arduino.cc/arduino-1.8.19-windows.zip"
const val LIQUIDCRYSTAL_I2C = 3146658L
const val ADAFRUIT_NEOPIXEL = 7136149L
val LIBRARIES = listOf("PaccerInput", "PaccerCommon", "PaccerOutput", "PaccerSound")
val SCRIPTS = listOf("PaccerMain", "lcd_test", "sound_test", "servo_test")

fun File.child(name: String): File {
    if (!exists()) mkdir()
    return File(path, name)
}

fun File.cacheOrDownload(urlString: String): File {
    val url = URL(urlString)
    val dest = child(url.file)
    if (dest.exists()) println("Using cached file for ${url.file}")
    else {
        print("Downloading new file: ${url.file}... ")
        Files.copy(url.openStream(), dest.toPath())
        println("✔")
    }
    return dest
}

fun GHRepository.cacheOrDownload(cache: File, destDir: File, rename: String? = null, ref: String? = null) {
    val name = rename ?: name
    if (cache.child("$name.zip").exists()) {
        println("Using cached zip for repository $name")
        val zip = ZipFile(cache.child("$name.zip"))
        // Extract zip to dest folder
        zip.extractAll(destDir.path)
        // Rename to proper name (without -main)
        destDir.listFiles()!!.first { name in it.name || name in it.name.replace('-', '_') }.renameTo(destDir.child(name))
    }
    else download(cache, destDir, rename, ref)
}

/** @param destDir target directory */
fun GHRepository.download(tempDir: File, destDir: File, rename: String? = null, ref: String? = null) {
    val name = rename ?: name
    print("Downloading $name... ")
    readZip({ `is` ->
        val dest = destDir.child(name)
        val tempFile = tempDir.child("$name.zip")
        // Download zip to temp folder
        Files.copy(`is`, tempFile.toPath())
        val zip = ZipFile(tempFile)
        // Extract zip to dest folder
        zip.extractAll(destDir.path)
        // Rename to proper name (without -main)
        destDir.listFiles()!!.first { name in it.name || name in it.name.replace('-', '_') }.renameTo(dest)
    }, ref)
    println("✔")
}

// Download link: https://downloads.arduino.cc/arduino-1.8.19-windows.zip
fun main() {
    // TODO option to change run dir with .env/config file
    val runDir = File("run")
    val finalZipFile = runDir.child("Paccer.zip")

    val cacheDir = runDir.child("cache")
    val buildDir = runDir.child("build")
    // Delete build dir
    if (buildDir.exists()) {
        print("Deleting old build directory... ")
        buildDir.deleteRecursively()
        println("✔")
    }
    // Delete Paccer.zip
    if (finalZipFile.exists()) {
        print("Deleting old Paccer.zip... ")
        finalZipFile.delete()
        println("✔")
    }
    // Maybe delete cache dir
    if (!USE_CACHE && cacheDir.exists()) {
        print("Deleting old cache directory because USE_CACHE is false... ")
        cacheDir.deleteRecursively()
        println("✔")
    }
    // Directory that will get zipped at the end.
    val buildOutDir = buildDir.child("out")

    // Main arduino files
    if (cacheDir.listFiles()?.any { it.extension != "zip" && "arduino-" in it.name } == true)
        println("Using cached extracted arduino dir.")
    else {
        val arduinoDownload = cacheDir.cacheOrDownload(ARDUINO_PORTABLE)
        val arduinoZip = ZipFile(arduinoDownload)

        print("Extracting arduino zip... ")
        arduinoZip.extractAll(cacheDir.path)
        println("✔")
    }
    print("Copying arduino zip from cache... ")
    val cachedArduinoDir = cacheDir.listFiles()!!.first { it.extension != "zip" && "arduino-" in it.name }
    val arduinoDir = buildOutDir.child(cachedArduinoDir.name)
    FileUtils.copyDirectory(cachedArduinoDir, arduinoDir)
    println("✔")

    val librariesDir = arduinoDir.child("libraries")

    // GitHub stuff - download the libraries to the librariesDir
    print("Getting repositories from GitHub... ")
    val github = GitHub.connect()
    val repositories = github.myself.repositories
    println("✔")
    // Only cache the LiquidCrystal_I2C library and always get the newest paccer libraries
    repositories.forEach {
        if (it.key in LIBRARIES) it.value.download(buildDir, librariesDir)
        else if (it.key in SCRIPTS) it.value.download(buildDir, buildOutDir)
    }
    github.getRepositoryById(LIQUIDCRYSTAL_I2C).cacheOrDownload(cacheDir, librariesDir, rename = "LiquidCrystal_I2C")
    github.getRepositoryById(ADAFRUIT_NEOPIXEL).cacheOrDownload(cacheDir, librariesDir)

    // Create dest zip file
    val finalZip = ZipFile(finalZipFile)
    print("Creating ${finalZipFile.name}... ")
    buildOutDir.listFiles()!!.forEach {
        finalZip.addFolder(it)
    }
    println("✔")
}