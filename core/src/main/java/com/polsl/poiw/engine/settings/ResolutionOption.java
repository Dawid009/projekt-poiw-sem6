package com.polsl.poiw.engine.settings;

public record ResolutionOption(int width, int height) {

    public String label() {
        return width + "x" + height;
    }
}