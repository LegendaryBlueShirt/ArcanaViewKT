package com.justnopoint.arcana

import SDL.*
import com.justnopoint.arcana.data.AHBox
import com.justnopoint.arcana.data.AHCharacters
import kotlinx.cinterop.*
import platform.windows.*

const val MENU_OPEN = 0x000001
const val MENU_QUIT = 0x000002
const val MENU_CHARACTER = 0x800000
const val MENU_ANIMATION = 0x400000
const val MENU_BOXES = 0x200000

val menubar = CreateMenu()
val fileMenu = CreateMenu()
val charMenu = CreateMenu()
var animMenu = CreateMenu()
var boxMenu = CreateMenu()

const val MENU_BOX_HIT = 0x000100
const val MENU_BOX_HURT = 0x000200
const val MENU_BOX_CLSN = 0x000400
const val MENU_BOX_CLSH = 0x000800
const val MENU_BOX_THRW = 0x001000
const val MENU_BOX_RFLC = 0x002000
const val MENU_BOX_OTHR = 0x004000

val boxMapping = mapOf(
    AHBox.BoxType.HIT to MENU_BOX_HIT,
    AHBox.BoxType.HURT to MENU_BOX_HURT,
    AHBox.BoxType.CLASH to MENU_BOX_CLSH,
    AHBox.BoxType.CLSN to MENU_BOX_CLSN,
    AHBox.BoxType.THROW to MENU_BOX_THRW,
    AHBox.BoxType.REFLECT to MENU_BOX_RFLC,
    AHBox.BoxType.OTHER to MENU_BOX_OTHR
)

val boxLabels = mapOf(
    MENU_BOX_HIT to "Hit",
    MENU_BOX_HURT to "Hurt",
    MENU_BOX_CLSN to "Push",
    MENU_BOX_CLSH to "Clash",
    MENU_BOX_THRW to "Throw",
    MENU_BOX_RFLC to "Reflect",
    MENU_BOX_OTHR to "Other",
)

fun addMenu(window: CPointer<cnames.structs.SDL_Window>) {
    val info: SDL_SysWMinfo = MemScope().alloc()
    SDL_GetWindowWMInfo(window, info.ptr)
    val handle: HWND? = info.info.win.window

    //val buffer = MemScope().allocArray<WCHARVar>(MAX_PATH)
    //buffer[0] = 0u
    chooserOptions.hwndOwner = handle
    //chooserOptions.pszDisplayName = buffer
    chooserOptions.lpszTitle = "Arcana Heart \"act\" folder".wcstr.getPointer(MemScope())
    chooserOptions.ulFlags = (BIF_NONEWFOLDERBUTTON).toUInt()

    (AppendMenu!!)(menubar, MF_POPUP.toUInt(), fileMenu.toLong().toULong(), "&File".wcstr.getPointer(MemScope()))
    (AppendMenu!!)(fileMenu, MF_STRING.toUInt(), MENU_OPEN.toULong(), "Open".wcstr.getPointer(MemScope()))
    (AppendMenu!!)(fileMenu, MF_STRING.toUInt(), MENU_QUIT.toULong(), "Quit".wcstr.getPointer(MemScope()))
    (AppendMenu!!)(menubar, MF_POPUP.toUInt(), charMenu.toLong().toULong(), "&Character".wcstr.getPointer(MemScope()))
    AHCharacters.entries.forEachIndexed { index, char ->
        (AppendMenu!!)(charMenu, MF_STRING.toUInt(), (index or MENU_CHARACTER).toULong(), char.displayName.wcstr.getPointer(MemScope()))
    }
    (AppendMenu!!)(menubar, MF_POPUP.toUInt(), animMenu.toLong().toULong(), "&Animation".wcstr.getPointer(MemScope()))
    (AppendMenu!!)(menubar, MF_POPUP.toUInt(), boxMenu.toLong().toULong(), "&Boxes".wcstr.getPointer(MemScope()))
    boxLabels.forEach { (index, label) ->
        (AppendMenu!!)(boxMenu, MF_STRING.toUInt(), (index or MENU_BOXES).toULong(), label.wcstr.getPointer(MemScope()))
    }
    disableCharacterMenu()

    SetMenu(handle, menubar)

    SDL_EventState(SDL_SYSWMEVENT, SDL_ENABLE)
}

actual fun disableCharacterMenu() {
    AHCharacters.entries.forEachIndexed { index, char ->
        EnableMenuItem(charMenu, index.toUInt(), (MF_BYPOSITION or MF_GRAYED).toUInt())
    }
}

actual fun enableCharacterMenu() {
    AHCharacters.entries.forEachIndexed { index, char ->
        EnableMenuItem(charMenu, index.toUInt(), (MF_BYPOSITION or MF_ENABLED).toUInt())
    }
}

actual fun setAnimList(anims: List<Int>) {
    val newMenu = CreateMenu()
    RemoveMenu(menubar, 2u, MF_BYPOSITION.toUInt())
    DestroyMenu(animMenu)
    animMenu = newMenu
    anims.forEach { index ->
        (AppendMenu!!)(animMenu, MF_STRING.toUInt(), (index or MENU_ANIMATION).toULong(), "Anim $index".wcstr.getPointer(MemScope()))
    }
    (InsertMenu!!)(menubar, 2u, (MF_POPUP or MF_BYPOSITION).toUInt(), animMenu.toLong().toULong(), "&Animation".wcstr.getPointer(MemScope()))
}

actual fun setBoxChecks(boxes: Map<AHBox.BoxType, Boolean>) {
    val newMenu = CreateMenu()
    RemoveMenu(menubar, 3u, MF_BYPOSITION.toUInt())
    DestroyMenu(boxMenu)
    boxMenu = newMenu
    boxes.forEach { (index, checked) ->
        val id = boxMapping[index]!!
        val label = boxLabels[id]!!
        val flags = if(checked) MF_STRING or MF_CHECKED else MF_STRING
        (AppendMenu!!)(boxMenu, flags.toUInt(), (id or MENU_BOXES).toULong(), label.wcstr.getPointer(MemScope()))
    }
    (InsertMenu!!)(menubar, 3u, (MF_POPUP or MF_BYPOSITION).toUInt(), boxMenu.toLong().toULong(), "&Boxes".wcstr.getPointer(MemScope()))
}

//fun WndProc(hwnd: HWND?, msg: UINT, wParam: WPARAM, lParam: LPARAM) : LRESULT {
//    when(msg) {
//        else -> return (DefWindowProc!!)(hwnd, msg, wParam, lParam)
//    }
//    return 0
//}