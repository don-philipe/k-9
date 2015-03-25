
package de.tud.smime.util;

interface ISmimeService {
    Intent execute(in Intent data, in ParcelFileDescriptor input, in ParcelFileDescriptor output);
}
