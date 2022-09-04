package com.brandon3055.edaprotoibom;

import java.io.InputStream;

/**
 * Created by brandon3055 on 04/09/2022
 */
public class PadFootprint extends FootprintProcessor {
    public PadFootprint(Pad pad) {
        super(null, "[fallback-footprint]");
        pads.put(pad.padId, pad);
        updateBB(pad);
    }

    @Override
    protected void processFile(InputStream fileStream) {

    }

    @Override
    public void postProcess() {
    }
}
