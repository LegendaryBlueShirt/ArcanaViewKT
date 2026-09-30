package com.justnopoint.arcana

import SDL.*
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Texture
import cnames.structs.SDL_Window
import com.justnopoint.arcana.data.AHBox
import com.justnopoint.arcana.util.Logger
import kotlinx.cinterop.*
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.opengl32.*
import platform.posix.getenv
import platform.windows.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlin.time.times

private val millisPerFrame = (1000.0/60).milliseconds
val chooserOptions = MemScope().alloc<_browseinfoW>()
var lastPath = ""

object Context: EngineContext, RenderContext {
    private lateinit var window: CPointer<SDL_Window>
    private var texCounter = 0
    private val textures = mutableMapOf<Int, CPointer<SDL_Texture>>()
    private lateinit var renderer: CPointer<SDL_Renderer>
    private val srcRect: SDL_Rect = MemScope().alloc()
    private val dstRect: SDL_Rect = MemScope().alloc()
    private val texW = MemScope().alloc<IntVar>()
    private val texH = MemScope().alloc<IntVar>()
    private val event = MemScope().alloc<SDL_Event>()
    private var glyphs = emptyMap<Char, Glyph>()
    private var nextFrame = TimeSource.Monotonic.markNow()
    private var frameCounter: Int = 0
    private var exiting = false
    private var selectedFolder: String? = null
    private var selectedCharacter: Int? = null
    private var selectedAnimation: Int? = null
    private var selectedBoxType: AHBox.BoxType? = null

    private var subBlendMode: SDL_BlendMode = 0u
    private fun createSubBlendMode() {
        subBlendMode = SDL_ComposeCustomBlendMode(
            SDL_BLENDFACTOR_DST_COLOR,
            SDL_BLENDFACTOR_SRC_COLOR,
            SDL_BLENDOPERATION_SUBTRACT,
            SDL_BLENDFACTOR_DST_ALPHA,
            SDL_BLENDFACTOR_SRC_ALPHA,
            SDL_BLENDOPERATION_SUBTRACT
        )
    }

    override fun isExiting(): Boolean {
        return exiting
    }

    override fun currentFrame(): Int {
        return frameCounter
    }

    private var keyCallback: ((Keys) -> Unit)? = null
    override fun setKeyCallback(callback: (Keys) -> Unit) {
        keyCallback = callback
    }

    override fun processInput() {
        while (SDL_PollEvent(event.ptr) != 0) {
            when (event.type) {
                SDL_QUIT -> {
                    exiting = true
                    unregisterGlobalMouseWheelHook()
                }
                SDL_KEYDOWN -> {
                    when(event.key.keysym.scancode) {
                        SDL_SCANCODE_LEFT -> keyCallback?.invoke(Keys.LEFT)
                        SDL_SCANCODE_RIGHT -> keyCallback?.invoke(Keys.RIGHT)
                        SDL_SCANCODE_UP -> keyCallback?.invoke(Keys.UP)
                        SDL_SCANCODE_DOWN -> keyCallback?.invoke(Keys.DOWN)
                        SDL_SCANCODE_SPACE -> keyCallback?.invoke(Keys.SPACEBAR)
                    }
                }
                SDL_SYSWMEVENT -> {
                    val winmsg = event.syswm.msg?.pointed?.msg?.win
                    when(winmsg?.msg?.toInt()) {
                        WM_COMMAND -> {
                            when(val command = winmsg.wParam.toInt()) {
                                MENU_QUIT -> exiting = true
                                MENU_OPEN -> {
                                    selectedFolder = showFolderChooser()
                                }
                                else -> {
                                    if((command and MENU_CHARACTER) == MENU_CHARACTER) {
                                        selectedCharacter = command xor MENU_CHARACTER
                                    } else if(command and MENU_ANIMATION == MENU_ANIMATION) {
                                        selectedAnimation = command xor MENU_ANIMATION
                                    } else if(command and MENU_BOXES == MENU_BOXES) {
                                        val boxTypeId = command xor MENU_BOXES
                                        selectedBoxType = boxMapping.firstNotNullOfOrNull {
                                            if(it.value == boxTypeId) {
                                                it.key
                                            } else {
                                                null
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal inline operator fun <reified T : CVariable> CPointer<T>.set(index: Int, item: CValues<T>) {
        val offset = index * sizeOf<T>()
        item.place(interpretCPointer(rawValue + offset)!!)
    }

    override fun loadIndexedImage(raster: ByteArray, palette: UByteArray, width: Int, height: Int): TextureInstance = memScoped {
        val newSurface = SDL_CreateRGBSurface(0u, width, height, 8, 0u, 0u, 0u, 0u)
        val paletteBuffer = allocArray<SDL_Color>(256)
        //palette.toCValues().place(interpretCPointer(paletteBuffer.rawValue)!!)
        for(n in 0 until 256) {
            paletteBuffer[n].r = palette[n*4+2]
            paletteBuffer[n].g = palette[n*4+1]
            paletteBuffer[n].b = palette[n*4+0]
            paletteBuffer[n].a = palette[n*4+3]
        }
        val format = newSurface?.pointed?.format
        val palette = format?.pointed?.palette
        SDL_SetPaletteColors(palette, paletteBuffer, 0, 256)
        SDL_memcpy(newSurface?.pointed?.pixels, raster.refTo(0), raster.size.toULong())
        SDL_SetColorKey(newSurface, SDL_TRUE.toInt(), 0u)
        return TextureInstance(surfaceToTexture(newSurface))
    }

    override fun loadRgbaImage(raster: ByteArray, width: Int, height: Int): TextureInstance {
        val newSurface = SDL_CreateRGBSurface(0u, width, height, 32, 0xFFu, 0xFF00u, 0xFF0000u, 0xFF000000u)
        SDL_memcpy(newSurface?.pointed?.pixels, raster.refTo(0), raster.size.toULong())
        return TextureInstance(surfaceToTexture(newSurface))
    }

    private fun surfaceToTexture(surface: CPointer<SDL_Surface>?): Int {
        val tex = SDL_CreateTextureFromSurface(renderer, surface)
        SDL_FreeSurface(surface)
        if (tex == null) {
            return -1
        }
        textures[texCounter++] = tex
        return texCounter - 1
    }

    override fun showText(text: String, posX: Int, posY: Int) {
        var cursorX = posX
        var cursorY = posY
        text.forEach { ascii ->
            val glyph = glyphs[ascii]?:return@forEach
            dstRect.apply {
                x = cursorX
                y = cursorY
                w = glyph.width
                h = glyph.height
            }
            SDL_RenderCopy(renderer, glyph.texture, null, dstRect.ptr)
            cursorX += glyph.width
        }
    }

    fun showImage(textureHandle: TextureInstance, posX: Int, posY: Int, scale: Int) {
        val texture = textures[textureHandle.texture]
        SDL_QueryTexture(texture, null, null, texW.ptr, texH.ptr)
        dstRect.x = posX * scale
        dstRect.y = posY * scale
        dstRect.w = texW.value * scale
        dstRect.h = texH.value * scale
        SDL_RenderCopy(renderer, texture, null, dstRect.ptr)
    }

    fun showImage(textureHandle: TextureInstance, posX: Int, posY: Int, width: Int, height: Int) {
        val texture = textures[textureHandle.texture]
        SDL_QueryTexture(texture, null, null, texW.ptr, texH.ptr)
        val ratio = texW.value.toDouble() / texH.value
        if(ratio < 1.4) {
            val scale = height.toDouble() / texH.value
            dstRect.x = posX + width/2 - (texW.value * scale).toInt()/2
            dstRect.y = posY
            dstRect.w = (texW.value * scale).toInt()
            dstRect.h = height
        } else {
            val scale = width.toDouble() / texW.value
            dstRect.x = posX
            dstRect.y = posY + height/2 - (texH.value * scale).toInt()/2
            dstRect.w = width
            dstRect.h = (texH.value * scale).toInt()
        }
        SDL_RenderCopy(renderer, texture, null, dstRect.ptr)
    }

    private var angle: Double = 0.0
    private var flip: SDL_RendererFlip = SDL_FLIP_NONE

    override fun showImage(
        textureHandle: TextureInstance,
        posX: Int,
        posY: Int,
        srcX: Int,
        srcY: Int,
        width: Int,
        height: Int,
        transformations: List<Transformation>,
        renderMode: RenderMode
    ) {
        val texture = textures[textureHandle.texture]
        SDL_QueryTexture(texture, null, null, texW.ptr, texH.ptr)
        dstRect.x = posX
        dstRect.y = posY
        dstRect.w = width
        dstRect.h = height
        srcRect.x = srcX
        srcRect.y = srcY
        srcRect.w = width
        srcRect.h = height
        angle = 0.0
        flip = SDL_FLIP_NONE

        transformations.forEach {
            when(it) {
                is Transformation.Flip -> {
                    if(it.vertical) flip = flip or SDL_FLIP_VERTICAL
                    if(it.horizontal) flip = flip or SDL_FLIP_HORIZONTAL
                }
                is Transformation.Rotation -> angle += it.degrees
            }
        }

        when(renderMode) {
            RenderMode.Normal -> {
                SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_BLEND)
            }
            is RenderMode.Additive -> {
                SDL_SetTextureBlendMode(texture, SDL_BLENDMODE_ADD)
            }
            RenderMode.Subtractive -> {
                SDL_SetTextureBlendMode(texture, subBlendMode)
            }
        }
        SDL_RenderCopyEx(renderer, texture, srcRect.ptr, dstRect.ptr, angle, null, flip)
    }

    override fun drawBox(color: Long, left: Int, top: Int, width: Int, height: Int) {
        dstRect.x = left
        dstRect.y = top
        dstRect.w = width
        dstRect.h = height
        val a = (color and 0xFF000000) shr 24
        val b = (color and 0xFF0000) shr 16
        val g = (color and 0xFF00) shr 8
        val r = (color and 0xFF)
        SDL_SetRenderDrawColor(renderer, r.toUByte(), g.toUByte(), b.toUByte(), a.toUByte())
        SDL_RenderDrawRect(renderer, dstRect.ptr)
    }

    override fun folderSelected(): String? {
        val returnval = selectedFolder
        selectedFolder = null
        return returnval
    }

    override fun characterSelected(): Int? {
        val returnval = selectedCharacter
        selectedCharacter = null
        return returnval
    }

    override fun animationSelected(): Int? {
        val returnval = selectedAnimation
        selectedAnimation = null
        return returnval
    }

    override fun boxtypeSelected(): AHBox.BoxType? {
        val returnval = selectedBoxType
        selectedBoxType = null
        return returnval
    }

    override fun <T> performRender(renderFunction: RenderContext.() -> T) {
        if(nextFrame.elapsedNow() > millisPerFrame) {
            SDL_SetRenderDrawColor(renderer, 0u,0u,0u,255u)
            SDL_RenderClear(renderer)
            renderFunction(this)
            SDL_RenderPresent(renderer)
            if(nextFrame.elapsedNow() >= (2 * millisPerFrame)) {
                frameCounter+=2
                nextFrame += (2 * millisPerFrame)
            } else {
                frameCounter++
                nextFrame += millisPerFrame
            }
            if(frameCounter < 0) {
                frameCounter = 0
            }
        }
    }

    fun initSdl(title: String) {
        if(SDL_Init(SDL_INIT_GAMECONTROLLER or SDL_INIT_VIDEO or SDL_INIT_JOYSTICK) != 0) {
            val error = SDL_GetError()
            error("SDL could not initialize! SDL_Error: $error")
        }

        val newWindow = SDL_CreateWindow(title, SDL_WINDOWPOS_UNDEFINED.toInt(), SDL_WINDOWPOS_UNDEFINED.toInt(), 800, 600, 0u)
        if(newWindow == null) {
            val error = SDL_GetError()
            error("SDL could not create window! SDL_Error: $error")
        }
        window = newWindow
        addMenu(window)

        val renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED)
        if(renderer == null) {
            val error = SDL_GetError()
            error("SDL could not create renderer! SDL_Error: $error")
        } else {
            Context.renderer = renderer
        }

        registerMousewheelHook()

        TTF_Init()
        createAlphabet()
        createSubBlendMode()
    }

    private fun createAlphabet() {
        val windir = getenv("windir")?.toKString()
        val font = TTF_OpenFont("$windir\\Fonts\\arial.ttf", 12) ?: error("Couldn't create font!")
        val fontColor: SDL_Color = MemScope().alloc<SDL_Color>().apply {
            a = 255u
            r = 255u
            g = 255u
            b = 255u
        }
        glyphs = (32 .. 126).map { it.toChar() }.associateWith { asciiChar ->
            val surface = TTF_RenderText_Solid(font, "$asciiChar", fontColor.readValue())
            val texture = SDL_CreateTextureFromSurface(renderer, surface)
            val glyph = Glyph(texture!!, surface?.pointed?.w?:0, surface?.pointed?.h?:0)
            SDL_FreeSurface(surface)
            glyph
        }
    }

    override fun clearTexture(handle: TextureInstance) {
        SDL_DestroyTexture(textures.remove(handle.texture))
    }
}

private var mouseHook: HHOOK? = null

private fun registerMousewheelHook() {
    if (mouseHook != null) return

    val hook = staticCFunction { nCode: Int, wParam: WPARAM, lParam: LPARAM ->
        if (nCode >= 0 && wParam.toUInt() == WM_MOUSEWHEEL.toUInt()) {

            // Reinterpret lParam to pull data from the global MSLLHOOKSTRUCT
            val mouseStruct = lParam.toCPointer<MSLLHOOKSTRUCT>()?.pointed

            // The scroll delta is stored in the high-order 16 bits of mouseData
            val mouseData = mouseStruct!!.mouseData
            val delta = ((mouseData shr 16) and 0xFFFFu).toShort()

            handleMousewheel(delta.toInt())
        }

        // Pass the event along so the native menu continues working normally
        CallNextHookEx(mouseHook, nCode, wParam, lParam)
    }

    mouseHook = SetWindowsHookEx!!(
        14, // WH_MOUSE_LL
        hook,
        GetModuleHandle!!(null),
        0.toUInt() // 0 binds it globally to all inputs
    )

    if (mouseHook == null) {
        Logger.log("Failed to install global mouse hook. Error: ${GetLastError()}")
    } else {
        Logger.logVerbose("Global Mouse Hook successfully active!")
    }
}

fun unregisterGlobalMouseWheelHook() {
    mouseHook?.let {
        UnhookWindowsHookEx(it)
        mouseHook = null
        Logger.logVerbose("Global Mouse Hook unregistered safely.")
    }
}

// This kinda sucks, may want to explore Imgui or a custom menu system
private fun handleMousewheel(y: Int) {
    val menuHwnd = FindWindowEx!!(null, null, "#32768".wcstr.getPointer(MemScope()), null)
        ?: // No active native menu is currently open on screen
        return

    val targetKey = if (y > 0) VK_UP else VK_DOWN

    // Take the absolute value to determine how many spaces to scroll
    val iterations = 3

    // 3. Loop and post the exact keystroke sequences directly into the menu's loop
    for (i in 0 until iterations) {
        PostMessage!!(
            menuHwnd,
            WM_KEYDOWN.toUInt(),
            targetKey.toULong(),
            0x01E00001.toLong() // Directs message routing directly to scrolling logic
        )

        PostMessage!!(
            menuHwnd,
            WM_KEYUP.toUInt(),
            targetKey.toULong(),
            0xC1E00001.toLong() // Directs message routing directly to scrolling logic
        )
    }
}

data class Glyph(
    val texture: CPointer<SDL_Texture>,
    val width: Int,
    val height: Int
)

val pathBuffer = MemScope().allocArray<WCHARVar>(MAX_PATH)

actual fun initChooser() {
    val path = "last.bin".toPath()
    val fs = FileSystem.SYSTEM
    lastPath = if(fs.exists(path)) {
        fs.source("last.bin".toPath()).use { source ->
            source.buffer().use { buffer ->
                buffer.readUtf8()
            }
        }
    } else {
        "C:\\Program Files (x86)\\Steam\\steamapps\\common\\ArcanaHeart3LMSS\\SteamData\\data\\ahdata"
    }
    //val pidl = SHSimpleIDListFromPath(lastPath.wcstr.getPointer(MemScope()))
    chooserOptions.lParam = lastPath.wcstr.getPointer(MemScope()).rawValue.toLong()
    chooserOptions.lpfn = staticCFunction(::BrowseCallbackProc)
}

fun BrowseCallbackProc(hwnd: HWND?, uMsg: UINT, lParam: LPARAM, lpData: LPARAM): Int {
    when(uMsg) {
        BFFM_INITIALIZED.toUInt() -> {
            (SendMessage!!)(hwnd, BFFM_SETSELECTION.toUInt(), TRUE.toULong(), lpData)
        }
    }
    return 0
}

actual fun saveChooser() {
    val path = "last.bin".toPath()
    val fs = FileSystem.SYSTEM
    fs.sink(path).use { sink ->
        sink.buffer().use { buffer ->
            buffer.writeUtf8(lastPath)
            buffer.flush()
        }
    }
}

fun showFolderChooser(): String? {
    val selection = (SHBrowseForFolder!!)(chooserOptions.ptr)

    return selection?.let {
        (SHGetPathFromIDList!!)(it, pathBuffer)
        chooserOptions.lParam = pathBuffer.getPointer(MemScope()).rawValue.toLong()
        lastPath = pathBuffer.toKString()
        pathBuffer.toKString()
    }
}

actual inline fun <T> engineContext(title: String, contextFunction: EngineContext.() -> T) {
    Context.initSdl(title)
    contextFunction.invoke(Context)
}