package com.polsl.poiw;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class TexturePackerTool {

    public static void main(String[] args) {
        String inputDir = "assets/raw/objects";
        String outputDir = "assets/graphics";
        String packFileName = "objects";

        TexturePacker.process(inputDir, outputDir, packFileName);
    }

}
