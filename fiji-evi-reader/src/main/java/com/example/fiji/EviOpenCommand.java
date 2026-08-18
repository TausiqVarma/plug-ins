package com.example.fiji;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * A SciJava Command that adds a menu item to open .evi files.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Open EVI File...")
public class EviOpenCommand implements Command {

    @Parameter
    private File eviFile;

    @Parameter
    private UIService uiService;

    @Override
    public void run() {
        // Pop up a guaranteed message to prove the command was clicked
        ij.IJ.showStatus("Opening EVI file...");
        
        try (RandomAccessFile file = new RandomAccessFile(eviFile, "r");
             FileChannel channel = file.getChannel()) {
             
            ByteBuffer prefix = ByteBuffer.allocate(10);
            prefix.order(ByteOrder.LITTLE_ENDIAN);
            
            while (prefix.hasRemaining()) {
                channel.read(prefix);
            }
            prefix.flip();
            
            prefix.position(8);
            short headerLen = prefix.getShort();
            channel.position(10 + headerLen);
            
            long remainingBytes = channel.size() - channel.position();
            int numFloats = (int) (remainingBytes / 4);
            
            // Check if file size matches our hardcoded dims
            if (numFloats != 128 * 128 * 64) {
                ij.IJ.showMessage("Error", "The EVI file size does not match 128x128x64 floats! Found: " + numFloats + " floats.");
                return;
            }
            
            ByteBuffer dataBuf = ByteBuffer.allocateDirect((int)remainingBytes);
            dataBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            while (dataBuf.hasRemaining()) {
                channel.read(dataBuf);
            }
            dataBuf.flip();
            
            float[] floatArray = new float[numFloats];
            dataBuf.asFloatBuffer().get(floatArray);
            
            int width = 128;
            int height = 128;
            int depth = 64;
            
            ImageStack stack = new ImageStack(width, height);
            for (int z = 0; z < depth; z++) {
                float[] slice = new float[width * height];
                System.arraycopy(floatArray, z * width * height, slice, 0, width * height);
                stack.addSlice(new FloatProcessor(width, height, slice));
            }
            
            ImagePlus imp = new ImagePlus(eviFile.getName(), stack);
            // Auto-adjust brightness/contrast so float data is visible (not black!)
            imp.resetDisplayRange();
            imp.show();
            // Zoom to fit the screen so it's not tiny
            ij.IJ.run(imp, "Set... ", "zoom=400");
            
        } catch (Exception e) {
            ij.IJ.handleException(e);
            ij.IJ.showMessage("EVI Error", "Failed to open EVI: " + e.getMessage());
        }
    }
}
