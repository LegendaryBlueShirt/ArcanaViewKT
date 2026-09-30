package com.justnopoint.arcana

import com.justnopoint.arcana.data.*
import com.justnopoint.arcana.data.AHBox.BoxType
import com.justnopoint.arcana.util.DecompressDDS
import com.justnopoint.arcana.util.HIPFile
import com.justnopoint.arcana.util.PacFileSystem
import com.justnopoint.arcana.util.pk3util
import okio.*
import okio.Path.Companion.toPath
import platform.posix.exit
import kotlin.time.Clock

lateinit var pacFile: PacFileSystem
lateinit var pacFileEf: PacFileSystem
lateinit var tblFile: TblFile
lateinit var tblFileEf: TblFile
lateinit var actFile: ActFile
lateinit var palFile: PalFile
var textures = emptyMap<Int, TextureInstance>()
var texturesEf = emptyMap<Int, TextureInstance>()
var currentCharacter: AHCharacters = AHCharacters.HEART
var characterLoaded: LoadStatus = LoadStatus.NOT_LOADED

enum class LoadStatus {
    NOT_LOADED, LOADING, LOADED
}

var currentPath: Path = "/".toPath()

//fun main() {
//    pk3util()
//}

//fun main() {
//    //val path = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ArcanaHeart3LMSS\\SteamData\\data\\ahdata\\act\\chara_effect\\chara14e\\0_rgb\\data_000.hip"
//    val path = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ArcanaHeart3LMSS\\SteamData\\data\\ahdata\\act\\chara_effect\\chara14e\\1_argb\\data_256.hip"
//    val palpath = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ArcanaHeart3LMSS\\SteamData\\data\\ahdata\\act\\chara_effect\\chara14e\\5_rgb_pal\\000_rgb_pal.hpl"
//    val pal = FileSystem.SYSTEM.source(palpath.toPath()).use { source ->
//        PalFile(source)
//    }
//    FileSystem.SYSTEM.openReadOnly(path.toPath()).use { source ->
//        val sheet = SpriteSheet(ByteArray(512*512*4), 512, 512, 4)
//        val hip = HIPFile(source, 0, sheet, 0, 0)
//        writeImageToFile(sheet.raster, null, sheet.width, sheet.height, "testout.png")
//    }
//
//}

val boxes = mutableMapOf(
    BoxType.HIT to true,
    BoxType.HURT to true,
    BoxType.CLSN to true,
    BoxType.CLASH to true,
    BoxType.THROW to true,
    BoxType.REFLECT to true,
    BoxType.OTHER to true,
)

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    initChooser()
    engineContext("Arcana Heart Frameviewer") {
        var currentAnim = 0
        val screenX = 320
        val screenY = 300
        var animating = true
        var frameIndex = 0
        setBoxChecks(boxes)

        setKeyCallback { key ->
            when(key) {
                Keys.LEFT -> {
                    animating = false
                    frameIndex--
                    if(frameIndex < 0) {
                        frameIndex = actFile.getAnimDef(currentAnim).size - 1
                    }
                }
                Keys.RIGHT -> {
                    animating = false
                    frameIndex++
                    if(frameIndex >= actFile.getAnimDef(currentAnim).size) {
                        frameIndex = 0
                    }
                }
                Keys.SPACEBAR -> animating = !animating
                else -> {}
            }
        }

        while(!isExiting()) {
            processInput()
            folderSelected()?.let {
                try {
                    currentPath = it.toPath()
                    characterLoaded = LoadStatus.LOADING
                    if (!loadCharacter(currentPath)) {
                        characterLoaded = LoadStatus.NOT_LOADED
                        disableCharacterMenu()
                    }
                } catch (e: Exception) {
                    println(e.message)
                    println(e.stackTraceToString())
                    exit(20)
                }
            }
            characterSelected()?.let {
                try {
                    val characterSelection = AHCharacters.entries[it]
                    characterLoaded = LoadStatus.LOADING
                    if (!loadCharacter(currentPath, characterSelection)) {
                        characterLoaded = LoadStatus.NOT_LOADED
                        disableCharacterMenu()
                    }
                } catch (e: Exception) {
                    println(e.message)
                    println(e.stackTraceToString())
                    exit(20)
                }
            }

            if (characterLoaded == LoadStatus.LOADING) {
                setAnimList(actFile.getValidAnims())
                currentAnim = 0
                frameIndex = 0

                textures.forEach { (_, tex) ->
                    clearTexture(tex)
                }
                textures =
                    buildSheets(tblFile, pacFile, currentCharacter).mapValues { (_, sheet) ->
                        if(sheet.bytesPerPixel == 1) {
                            loadIndexedImage(sheet.raster, palFile.data, sheet.width, sheet.height)
                        } else {
                            loadRgbaImage(sheet.raster, sheet.width, sheet.height)
                        }
                    }
                texturesEf =
                    buildEffectSheets(tblFileEf, pacFileEf, currentCharacter).mapValues { (_, sheet) ->
                        if(sheet.bytesPerPixel == 1) {
                            loadIndexedImage(sheet.raster, palFile.data, sheet.width, sheet.height)
                        } else {
                            loadRgbaImage(sheet.raster, sheet.width, sheet.height)
                        }
                    }
                enableCharacterMenu()
                characterLoaded = LoadStatus.LOADED
            }

            boxtypeSelected()?.let {
                val currentValue = boxes.getOrElse(it) { false }
                boxes[it] = !currentValue
                setBoxChecks(boxes)
            }

            animationSelected()?.let {
                currentAnim = it
                frameIndex = 0
            }

            if(characterLoaded == LoadStatus.LOADED) {
                performRender {
                    val anim = actFile.getAnimDef(currentAnim)
                    if(animating) {
                        frameIndex = actFile.getFrameForTime(anim, currentFrame())
                    }
                    val frame = actFile.getFrameDef(anim[frameIndex])
                    showText(frame.frameData.toHexString(), 10, 548)
                    showText(frame.frameData2.toHexString(), 10, 564)
                    frame.activeSprites.filter { it != 0 }.map {
                        actFile.getSprDef(it)
                    }.forEachIndexed { sprindex, sprData ->
                        if(sprData.sprNo >= tblFile.getEntryCount()) {
                            tblFileEf.getEntry(sprData.sprNo.toInt()-tblFile.getEntryCount())?.let { entry ->
                                showImage(
                                    textureHandle = texturesEf[entry.sheet]!!,
                                    posX = screenX + sprData.axisX,
                                    posY = screenY - sprData.axisY,
                                    srcX = entry.axisX,
                                    srcY = entry.axisY,
                                    width = entry.width,
                                    height = entry.height,
                                    transformations = sprData.transformations,
                                    renderMode = sprData.renderMode
                                )
                            }
                        } else {
                            tblFile.getEntry(sprData.sprNo.toInt())?.let { entry ->
                                showImage(
                                    textureHandle = textures[entry.sheet]!!,
                                    posX = screenX + sprData.axisX,
                                    posY = screenY - sprData.axisY,
                                    srcX = entry.axisX,
                                    srcY = entry.axisY,
                                    width = entry.width,
                                    height = entry.height,
                                    transformations = sprData.transformations,
                                    renderMode = sprData.renderMode
                                )
                            }
                        }
                        showText(sprData.flags.toHexString(), 100, 22 + 12 * sprindex)
                        showText(sprData.unk1.toHexString(), 132, 22 + 12 * sprindex)
                        showText(sprData.unk2.toHexString(), 450, 22 + 12 * sprindex)
                    }
                    var boxY = 160
                    frame.activeBoxes.filter { it != 0 }.map {
                        it to actFile.getBoxDef(it)
                    }.forEach { (index, boxData) ->
                        if(boxes[boxData.getBoxType()] == true) {
                            showText("$index - ${boxData.data.toHexString()} ${boxData.getExtraInformation()}", 5, boxY)
                            boxY+=14
                            drawBox(
                                color = boxData.getBoxColor(),
                                left = screenX + boxData.x,
                                top = screenY - (boxData.y + boxData.h),
                                width = boxData.w,
                                height = boxData.h
                            )
                        }
                    }
                    if((frame.throwBox[2] > 0) || (frame.throwBox[3] > 0)) {
                        if(boxes[BoxType.THROW] == true) {
                            drawBox(
                                color = 0xC0FF00B7,
                                left = screenX + frame.throwBox[0],
                                top = screenY - (frame.throwBox[1] + frame.throwBox[3]),
                                width = frame.throwBox[2],
                                height = frame.throwBox[3]
                            )
                        }
                    }
                    showText(currentCharacter.displayName, 10, 10)
                    showText("Anim - $currentAnim", 10, 28)
                    showText("Frame - ${frameIndex+1}/${anim.size}", 10, 46)
                    showText("Duration - ${frame.getDuration()}", 10, 64)
                    if(!animating) {
                        showText("Paused", 290, 340)
                    }
                    frame.knownFlags.forEachIndexed { flagIndex, flag ->
                        showText(flag, 600, 22 + 82*flagIndex)
                    }
                }
            }
        }
    }

    saveChooser()
    exit(0)
}

fun loadCharacter(path: Path, character: AHCharacters = AHCharacters.HEART): Boolean {
    //val path = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ArcanaHeart3LMSS\\SteamData\\data\\ahdata\\act".toPath()
    currentCharacter = character
    return try {
        val dataFile = path.div(character.getDataFile())
        val spriteFile = path.div(character.getPacFile())
        val effectFile = path.div(character.getEffectFile())
        println(dataFile)
        val fileSystem = FileSystem.SYSTEM
        actFile = fileSystem.openReadOnly(dataFile).use { source ->
            ActFile(source.source(0))
        }
        println("Act loaded")
        pacFile = PacFileSystem(spriteFile)
        pacFile.list("/palimg".toPath())
        tblFile = pacFile.openReadOnly(character.getTblFilePath()).use { handle ->
            handle.source().use {
                TblFile(it)
            }
        }
        println("Tbl loaded")
        try {
            palFile = pacFile.openReadOnly(character.getPalFilePath(0)).use { handle ->
                handle.source().use {
                    PalFile(it)
                }
            }
        } catch(_: FileNotFoundException) {
            val fileList = pacFile.list("/paldata".toPath())
            palFile = pacFile.openReadOnly(fileList[0]).use { handle ->
                handle.source().use {
                    PalFile(it)
                }
            }
        }
        println("Pal loaded")
        pacFileEf = PacFileSystem(effectFile)
        tblFileEf = pacFileEf.openReadOnly(character.getEffectTblFilePath()).use { handle ->
            handle.source().use {
                TblFile(it)
            }
        }
        println("TblEf loaded")
        true
    } catch (e: Exception) {
        false
    }
}

fun buildEffectSheets(tbldata: TblFile, archive: PacFileSystem, character: AHCharacters): Map<Int, SpriteSheet> {
    var instant = Clock.System.now()
    return tbldata.getSheetIndices().associateWith { index ->
        archive.openReadOnly(character.getEffectSheet(index)).use { inFile ->
            val newInstant = Clock.System.now()
            val total = newInstant - instant
            instant = newInstant
            println("Time Taken: ${total.inWholeMilliseconds}")
            println("Loading ${character.getEffectSheet(index)}")
            if(index < 320) {
                val hip = HIPFile(inFile, 0)
                hip.sheet
            } else {
                inFile.source().buffer().use {
                    DecompressDDS(it)
                }
            }
        }
    }
}

fun buildSheets(tbldata: TblFile, archive: PacFileSystem, character: AHCharacters): Map<Int, SpriteSheet> {
    val sheets = tbldata.getSheetIndices().associateWith { _ ->
        SpriteSheet(1024, 1024, 1)
    }

    tbldata.sheetData.forEachIndexed { index, entry ->
        archive.openReadOnly(character.getSpriteFile(index)).use { inFile ->
            HIPFile(inFile, 0, sheets[entry.sheet]!!, entry.axisX, entry.axisY)
        }
    }

    return sheets
}

expect fun writeImageToFile(raster: ByteArray, paldata: ByteArray?, width: Int, height: Int, outFile: String)
expect fun initChooser()
expect fun saveChooser()
expect fun disableCharacterMenu()
expect fun enableCharacterMenu()
expect fun setAnimList(anims: List<Int>)
expect fun setBoxChecks(boxes: Map<AHBox.BoxType, Boolean>)