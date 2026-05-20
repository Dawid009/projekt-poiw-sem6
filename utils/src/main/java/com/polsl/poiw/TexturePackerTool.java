package com.polsl.poiw;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class TexturePackerTool {

    public static void main(String[] args) {
        String inputDir = "assets/raw/creatures";
        String outputDir = "assets/graphics";
        String packFileName = "creatures";

        TexturePacker.process(inputDir, outputDir, packFileName);
    }

}
