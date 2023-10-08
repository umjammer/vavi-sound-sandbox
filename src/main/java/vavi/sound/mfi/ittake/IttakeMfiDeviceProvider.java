/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.ittake;

import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.spi.MfiDeviceProvider;


/**
 * MfiDeviceProvider implemented by Ittake.
 *
 * @author ittake
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041212 nsano initial version <br>
 */
public class IttakeMfiDeviceProvider extends MfiDeviceProvider {

    /** */
    private final MfiDevice[] devices;

    /** */
    public IttakeMfiDeviceProvider() {
        devices = new MfiDevice[] {
            new IttakeMidiConverter()
        };
    }

    /** */
    public boolean isDeviceSupported(MfiDevice.Info info) {
        for (MfiDevice device : devices) {
            if (device.getDeviceInfo().equals(info)) {
                return true;
            }
        }
        return false;
    }

    /** */
    public MfiDevice.Info[] getDeviceInfo() {
        MfiDevice.Info[] infos = new MfiDevice.Info[devices.length];
        for (int i = 0; i < devices.length; i++) {
            infos[i] = devices[i].getDeviceInfo();
        }

        return infos;
    }

    /** */
    public MfiDevice getDevice(MfiDevice.Info info)
        throws IllegalArgumentException {

        for (MfiDevice device : devices) {
            if (device.getDeviceInfo().equals(info)) {
                return device;
            }
        }

        throw new IllegalArgumentException(info + " is not supported");
    }
}

/* */
