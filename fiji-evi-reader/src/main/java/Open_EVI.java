import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Legacy ImageJ1 plugin for opening .evi files.
 * The underscore in the class name is REQUIRED - it tells Fiji
 * "this is a plugin, put it in the Plugins menu".
 */
public class Open_EVI implements PlugIn {

    @Override
    public void run(String arg) {
        // Show a file chooser dialog
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an EVI file");
        chooser.setFileFilter(new FileNameExtensionFilter("EVI Files (*.evi)", "evi"));

        int result = chooser.showOpenDialog(IJ.getInstance());
        if (result != JFileChooser.APPROVE_OPTION) {
            return; // User cancelled
        }

        File eviFile = chooser.getSelectedFile();
        IJ.showStatus("Opening EVI: " + eviFile.getName());

        try (RandomAccessFile file = new RandomAccessFile(eviFile, "r");
             FileChannel channel = file.getChannel()) {

            // Read the 10-byte NumPy header prefix
            ByteBuffer prefix = ByteBuffer.allocate(10);
            prefix.order(ByteOrder.LITTLE_ENDIAN);
            while (prefix.hasRemaining()) {
                channel.read(prefix);
            }
            prefix.flip();

            // Skip magic (6 bytes) + version (2 bytes), read header length (2 bytes)
            prefix.position(8);
            short headerLen = prefix.getShort();
            channel.position(10 + headerLen);

            // Read raw float32 data
            long remainingBytes = channel.size() - channel.position();
            int numFloats = (int) (remainingBytes / 4);

            ByteBuffer dataBuf = ByteBuffer.allocateDirect((int) remainingBytes);
            dataBuf.order(ByteOrder.LITTLE_ENDIAN);
            while (dataBuf.hasRemaining()) {
                channel.read(dataBuf);
            }
            dataBuf.flip();

            float[] floatArray = new float[numFloats];
            dataBuf.asFloatBuffer().get(floatArray);

            // Build image stack (Z=64, Y=128, X=128)
            int width = 128;
            int height = 128;
            int depth = numFloats / (width * height);

            ImageStack stack = new ImageStack(width, height);
            for (int z = 0; z < depth; z++) {
                float[] slice = new float[width * height];
                System.arraycopy(floatArray, z * width * height, slice, 0, width * height);
                stack.addSlice("Slice " + (z + 1), new FloatProcessor(width, height, slice));
            }

            ImagePlus imp = new ImagePlus(eviFile.getName(), stack);
            imp.resetDisplayRange();
            imp.show();

            IJ.showStatus("EVI file loaded: " + width + "x" + height + "x" + depth);

        } catch (Exception e) {
            IJ.error("EVI Reader Error", "Failed to open file:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
}
