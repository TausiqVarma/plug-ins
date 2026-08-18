package com.example.fiji;

import org.scijava.Priority;
import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * An ImageJ2 (SciJava) IOPlugin for reading custom .evi files.
 * .evi files in this mock are float32 NumPy binary blobs (Z=64, Y=128, X=128).
 */
@Plugin(type = IOPlugin.class, priority = Priority.NORMAL)
public class EviReaderPlugin extends AbstractIOPlugin<Dataset> {

    @Parameter
    private DatasetService datasetService;

    // We must declare the type of data this plugin produces
    @Override
    public Class<Dataset> getDataType() {
        return Dataset.class;
    }

    // This is the equivalent of napari's filename_patterns
    @Override
    public boolean supportsOpen(String source) {
        return source.toLowerCase().endsWith(".evi");
    }

    // This is the equivalent of the napari reader_function
    @Override
    public Dataset open(String source) throws IOException {
        
        try (RandomAccessFile file = new RandomAccessFile(source, "r");
             FileChannel channel = file.getChannel()) {
             
            // NumPy .npy files have a 6 byte magic string + 2 byte version + 2 byte header length
            ByteBuffer prefix = ByteBuffer.allocate(10);
            prefix.order(ByteOrder.LITTLE_ENDIAN);
            
            while (prefix.hasRemaining()) {
                channel.read(prefix);
            }
            prefix.flip();
            
            // Skip the 6-byte magic string ("\x93NUMPY") and 2-byte version
            prefix.position(8);
            
            // Read the length of the ASCII header dict
            short headerLen = prefix.getShort();
            
            // Jump the channel position past the magic + version + length (10) + header length
            channel.position(10 + headerLen);
            
            // Calculate how many bytes of raw image data are left
            long remainingBytes = channel.size() - channel.position();
            int numFloats = (int) (remainingBytes / 4); // float32 = 4 bytes
            
            // Read the raw float32 bytes into memory
            ByteBuffer dataBuf = ByteBuffer.allocateDirect((int)remainingBytes);
            dataBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            while (dataBuf.hasRemaining()) {
                channel.read(dataBuf);
            }
            dataBuf.flip();
            
            // Convert to a Java float array
            float[] floatArray = new float[numFloats];
            dataBuf.asFloatBuffer().get(floatArray);
            
            // The sample.evi file was generated as a (64, 128, 128) array in Python.
            // Python/NumPy saves data in Z, Y, X order (C-contiguous).
            // ImgLib2 expects dimensions in X, Y, Z order.
            long[] dims = new long[] { 128, 128, 64 };
            
            // Wrap the raw flat array into an N-dimensional ImgLib2 Img
            Img<FloatType> img = ArrayImgs.floats(floatArray, dims);
            
            // Convert the raw Img to a rich Fiji Dataset and name it
            Dataset dataset = datasetService.create(img);
            dataset.setName("EVI Image");
            
            return dataset;
        }
    }
}
