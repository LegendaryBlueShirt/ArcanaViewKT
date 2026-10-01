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
    val index: Int,
    val isArcana: Boolean = false) {
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
    DHEART(displayName = "Dark Heart", index = 30),
    LOVE(displayName = "Love - Partinias", index = 0, isArcana = true),
    LIGHTNING(displayName = "Lightning - Bhanri", index = 1, isArcana = true),
    TIME(displayName = "Time - Anutpuda", index = 2, isArcana = true),
    WOOD(displayName = "Plant - Moriomoto", index = 3, isArcana = true),
    EARTH(displayName = "Earth - Ohtsuchi", index = 4, isArcana = true),
    FIRE(displayName = "Fire - Lang-Gong", index = 5, isArcana = true),
    WIND(displayName = "Wind - Tempestas", index = 6, isArcana = true),
    DARK(displayName = "Dark - Gier", index = 7, isArcana = true),
    EVIL(displayName = "Evil - Dieu Mort", index = 8, isArcana = true),
    WATER(displayName = "Water - Niptra", index = 9, isArcana = true),
    GOLD(displayName = "Metal - Oreichalkos", index = 10, isArcana = true),
    SACRED(displayName = "Sacred - Zillael", index = 12, isArcana = true),
    ICE(displayName = "Ice - Almacia", index = 13, isArcana = true),
    HALO(displayName = "Light - Mildred", index = 14, isArcana = true),
    PUNISH(displayName = "Punishment - Koshmar", index = 15, isArcana = true),
    CRIME(displayName = "Sin - Sorwat", index = 16, isArcana = true),
    MAGNET(displayName = "Magnetism - Medein", index = 17, isArcana = true),
    MIRROR(displayName = "Mirror - Heliogabalus", index = 18, isArcana = true),
    TONE(displayName = "Tone - Phenex", index = 20, isArcana = true),
    BLOSSOM(displayName = "Flower - Kayatsuhime", index = 21, isArcana = true),
    FENRIR(displayName = "Fenrir - Baldur", index = 23, isArcana = true),
    LUCK(displayName = "Luck - Saligrama", index = 24, isArcana = true),
    TYR(displayName = "Tyr - Gottfried", index = 25, isArcana = true),
    BLOOD(displayName = "Blood", index = 28, isArcana = true),
    LIFE(displayName = "Life - Parace L'sia", index = 29, isArcana = true)
}

fun AHCharacters.indexHex(): String {
    return index.toString().padStart(2, '0')
}

fun AHCharacters.getDataFile(): String {
    return if(isArcana) {
        "tenshi/act_${indexHex()}.pk3"
    } else {
        "chara/act_${indexHex()}.pk3"
    }
}

fun AHCharacters.getPacFile(): String {
    return if(isArcana) {
        "tenshi/tenshi_${indexHex()}.pac"
    } else {
        "chara/chara_split_${indexHex()}.pac"
    }
}

fun AHCharacters.getEffectFile(): String {
    return if(isArcana) {
        "tenshi_effect/tenshi${indexHex()}e.pac"
    } else {
        "chara_effect/chara${indexHex()}e.pac"
    }
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
    return if(isArcana) {
        "/".toPath().div(DIR_TBL).div("tenshi_${indexHex()}.tbl")
    } else {
        "/".toPath().div(DIR_TBL).div("chara_${indexHex()}.tbl")
    }
}

fun AHCharacters.getEffectTblFilePath(): Path {
    return if(isArcana) {
        "/".toPath().div(DIR_EF_TBL).div("tenshi_${indexHex()}e.tbl")
    } else {
        "/".toPath().div(DIR_EF_TBL).div("chara_${indexHex()}e.tbl")
    }
}

fun AHCharacters.getPalFilePath(color: Int, group: Int = 0): Path {
    val groupString = group.toString().padStart(3, '0')
    val colorChar = 'A' + color
    return "/".toPath().div(DIR_PAL).div("${groupString}_pal_${indexHex()}_$colorChar.hpl")
}