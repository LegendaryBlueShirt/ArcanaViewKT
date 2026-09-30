package com.justnopoint.arcana.data

import okio.Path
import okio.Path.Companion.toPath

val DIR_IMG = "palimg"
val DIR_PAL = "paldata"
val DIR_TBL = "tbldata"
val DIR_EF_RGB = "0_rgb"
val DIR_EF_ARGB = "1_argb"
val DIR_EF_DXT = "2_dxt"
val DIR_EF_TBL = "4_tbl"
val DIR_EF_PAL = "5_rgb_pal"

enum class AHCharacters(
    val displayName: String,
    val subfolder: String = "chara",
    val index: Int) {
    HEART(displayName = "Heart Aino", index = 0),
    SAKI(displayName = "Saku Tsuzura", index = 1),
    KAMUI(displayName = "Kamui Tokinomiya", index = 2),
    KONOHA(displayName = "Konoha", index = 3),
    MAORI(displayName = "Maori Kasuga", index = 4),
    MEIFANG(displayName = "Mei-Fang", index = 5),
    LILICA(displayName = "Lilica Felchenerow", index = 6),
    LIESELOTTE(displayName = "Lieselotte Achenbach", index = 7),
    YORIKO(displayName = "Yoriko Yasuzumi", index = 8),
    KIRA(displayName = "Kira Daidouji", index = 9),
    FIONA(displayName = "Fiona Mayfield", index = 10),
    MILDRED(displayName = "Mildred Avallone", index = 11),
    PETRA(displayName = "Petra Johanna Lagerkvist", index = 12),
    ZENIA(displayName = "Zenia Valov", index = 13),
    ANGELIA(displayName = "Angelia Avallone", index = 14),
    ELSA(displayName = "Elsa La Conti", index = 15),
    CLARICE(displayName = "Clarice Di Lanza", index = 16),
    CATHERINE(displayName = "Catherine Kyobashi", index = 17),
    DOROTHY(displayName = "Dorothy Albright", index = 18),
    AKANE(displayName = "Akane Inukawa", index = 20),
    NAZUNA(displayName = "Nazuna Inukawa", index = 21),
    PARACE(displayName = "Parace L'sia", index = 22),
    SCHARLACHROT(displayName = "Scharlachrot", index = 23),
    EKO(displayName = "Eko", index = 24),
    WEISS(displayName = "Weiss", index = 25),
    RAGNAROK(displayName = "Ragnarok", index = 27),
    MINORI(displayName = "Minori Amanohara", index = 28),
    PISTRIX(displayName = "Pistrix", index = 29),
    DHEART(displayName = "Dark Heart", index = 30)
}

fun AHCharacters.indexHex(): String {
    return index.toString().padStart(2, '0')
}

fun AHCharacters.getDataFile(): String {
    return "$subfolder/act_${indexHex()}.pk3"
}

fun AHCharacters.getPacFile(): String {
    return "$subfolder/chara_split_${indexHex()}.pac"
}

fun AHCharacters.getEffectFile(): String {
    return "${subfolder}_effect/chara${indexHex()}e.pac"
}

fun AHCharacters.getSpriteFile(index: Int): Path {
    val indexStr = index.toString().padStart(3, '0')
    return "/".toPath().div(DIR_IMG).div("data_$indexStr.hip")
}

fun AHCharacters.getEffectSheet(index: Int): Path {
    val indexString = index.toString().padStart(3, '0')
    return if(index < 256) {
        "/".toPath().div(DIR_EF_RGB).div("data_$indexString.hip")
    } else if(index < 320) {
        "/".toPath().div(DIR_EF_ARGB).div("data_$indexString.hip")
    } else {
        "/".toPath().div(DIR_EF_DXT).div("data_$indexString.dds")
    }
}

fun AHCharacters.getTblFilePath(): Path {
    return "/".toPath().div(DIR_TBL).div("chara_${indexHex()}.tbl")
}

fun AHCharacters.getEffectTblFilePath(): Path {
    return "/".toPath().div(DIR_EF_TBL).div("chara_${indexHex()}e.tbl")
}

fun AHCharacters.getPalFilePath(color: Int, group: Int = 0): Path {
    val groupString = group.toString().padStart(3, '0')
    val colorChar = 'A' + color
    return "/".toPath().div(DIR_PAL).div("${groupString}_pal_${indexHex()}_$colorChar.hpl")
}