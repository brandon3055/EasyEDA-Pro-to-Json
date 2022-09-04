package com.brandon3055.edaprotoibom;

import java.io.InputStream;

/**
 * Created by brandon3055 on 04/09/2022
 */
public class FallbackFootprint extends FootprintProcessor {
    public FallbackFootprint() {
        super(null, "[fallback-footprint]");
    }

    @Override
    protected void processFile(InputStream fileStream) {}

    @Override
    public void postProcess() {
        startPos = new Point(-10, -10);
        endPos = new Point(10, 10);
    }
}
