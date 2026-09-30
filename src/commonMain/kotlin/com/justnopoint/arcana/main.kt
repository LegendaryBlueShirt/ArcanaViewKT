package com.justnopoint.arcana

import com.justnopoint.arcana.data.*
import com.justnopoint.arcana.data.AHBox.BoxType
import com.justnopoint.arcana.util.DecompressDDS
import com.justnopoint.arcana.util.HIPFile
import com.justnopoint.arcana.util.Logger
import com.justnopoint.arcana.util.PacFileSystem
import com.justnopoint.arcana.util.pk3util
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.*
import okio.Path.Companion.toPath
import platform.posix.exit

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
    NOT_LOADED, LOADING, PARTIAL, LOADED
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
    Logger.init()
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
                currentPath = it.toPath()
                beginCharacterLoad(currentPath)
                currentAnim = 0
                frameIndex = 0
            }
            characterSelected()?.let {
                val characterSelection = AHCharacters.entries[it]
                beginCharacterLoad(currentPath, characterSelection)
                currentAnim = 0
                frameIndex = 0
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

            if(characterLoaded == LoadStatus.LOADED || characterLoaded == LoadStatus.PARTIAL) {
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
                            if(characterLoaded == LoadStatus.LOADED) {
                                tblFileEf.getEntry(sprData.sprNo.toInt() - tblFile.getEntryCount())?.let { entry ->
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
                    if(characterLoaded == LoadStatus.PARTIAL) {
                        showText(Logger.internalLog.last(), 500, 564)
                    }
                }
            } else if (characterLoaded == LoadStatus.LOADING) {
                performRender {
                    Logger.internalLog.takeLast(20).forEachIndexed { index, string ->
                        showText(string, 5, 5 + 14*index)
                    }
                }
            }
        }
    }

    saveChooser()
    Logger.close()
    exit(0)
}

private var job: Job? = null

fun EngineContext.beginCharacterLoad(path: Path, character: AHCharacters = AHCharacters.HEART) {
    if (job?.isActive == true) {
        return
    }
    characterLoaded = LoadStatus.LOADING
    disableCharacterMenu()
    job = CoroutineScope(SupervisorJob()).launch {
        withContext(CoroutineExceptionHandler { _, throwable ->
            Logger.log(throwable.toString())
            Logger.log(throwable.stackTraceToString())
        }) {
            currentCharacter = character
            var success = loadCharacter(path)
            if (!success) {
                cancel()
            }
            setAnimList(actFile.getValidAnims())

            textures.forEach { (_, tex) ->
                clearTexture(tex)
            }
            textures =
                buildSheets(tblFile, pacFile, currentCharacter).mapValues { (_, sheet) ->
                    if (sheet.bytesPerPixel == 1) {
                        loadIndexedImage(sheet.raster, palFile.data, sheet.width, sheet.height)
                    } else {
                        loadRgbaImage(sheet.raster, sheet.width, sheet.height)
                    }
                }
            characterLoaded = LoadStatus.PARTIAL
            success = loadCharacterEffects(path)
            if (success) {
                texturesEf =
                    buildEffectSheets(tblFileEf, pacFileEf, currentCharacter).mapValues { (_, sheet) ->
                        if (sheet.bytesPerPixel == 1) {
                            loadIndexedImage(sheet.raster, palFile.data, sheet.width, sheet.height)
                        } else {
                            loadRgbaImage(sheet.raster, sheet.width, sheet.height)
                        }
                    }
                characterLoaded = LoadStatus.LOADED
            }
        }
    }.apply {
        invokeOnCompletion { e ->
            if (e != null) {
                println(e.toString())
                characterLoaded = LoadStatus.NOT_LOADED
            }
            enableCharacterMenu()
        }
    }
}

fun loadCharacter(path: Path): Boolean {
    //val path = "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ArcanaHeart3LMSS\\SteamData\\data\\ahdata\\act".toPath()
    Logger.clear()
    return try {
        val dataFile = path.div(currentCharacter.getDataFile())
        val spriteFile = path.div(currentCharacter.getPacFile())
        println(dataFile)
        val fileSystem = FileSystem.SYSTEM
        actFile = fileSystem.openReadOnly(dataFile).use { source ->
            ActFile(source.source(0))
        }
        Logger.log("Act loaded")
        pacFile = PacFileSystem(spriteFile)
        pacFile.list("/palimg".toPath())
        tblFile = pacFile.openReadOnly(currentCharacter.getTblFilePath()).use { handle ->
            handle.source().use {
                TblFile(it)
            }
        }
        Logger.log("Tbl loaded")
        try {
            palFile = pacFile.openReadOnly(currentCharacter.getPalFilePath(0)).use { handle ->
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
        Logger.log("Pal loaded")
        true
    } catch (e: Exception) {
        Logger.log(e.toString())
        Logger.log(e.stackTraceToString())
        false
    }
}

fun loadCharacterEffects(path: Path): Boolean {
    return try {
        val effectFile = path.div(currentCharacter.getEffectFile())
        pacFileEf = PacFileSystem(effectFile)
        tblFileEf = pacFileEf.openReadOnly(currentCharacter.getEffectTblFilePath()).use { handle ->
            handle.source().use {
                TblFile(it)
            }
        }
        Logger.log("TblEf loaded")
        true
    } catch (e: Exception) {
        Logger.log(e.toString())
        Logger.log(e.stackTraceToString())
        false
    }
}

fun buildEffectSheets(tbldata: TblFile, archive: PacFileSystem, character: AHCharacters): Map<Int, SpriteSheet> {
    return tbldata.getSheetIndices().associateWith { index ->
        archive.openReadOnly(character.getEffectSheet(index)).use { inFile ->
            Logger.log("Loading ${character.getEffectSheet(index)}")
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