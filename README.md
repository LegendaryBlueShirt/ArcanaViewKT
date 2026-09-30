# Arcana Heart 3 Love Max Six Stars!!!!!! Xtend Frameviewer

Tool to load and visualize characters from the game with hitboxes and information.

## Project Information

### Dependencies

This project is set up to compile on Windows with mingw.  SDL2, SDL2_ttf, and libpng are used here and runtime DLLs are provided for convenience.

Okio is used for native file IO.

### Build

For a complete compile and run
```
./gradlew :runArcanaViewDebugExecutableMingwX64
```

To just compile
```
./gradlew :compileKotlinMingwX64
```

### Structure

The program's entry point is main.kt. Functions which are non-native are defined in renderinterfaces.kt and implemented in the nativeMain source folder.  Win32 menu operations are implemented in menu.kt, SDL rendering in sdlfunc.kt, and libpng operations in pngfunc.kt

Files defining and parsing Arcana Heart data are in the /data package

## Usage

Run arcanaView.exe and the GUI will appear.

File/Open

Load the arcana heart data folder. This is typically in ArcanaHeart3LMSS/SteamData/data/ahdata/act. Upon closing, the program will remember this location in a file called last.bin.

Left and Right changes the frame being displayed. Use the Animation menu to browse different animations for the loaded character. Spacebar pauses and resumes automatic animation.