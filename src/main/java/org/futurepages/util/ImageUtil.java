package org.futurepages.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Iterator;

/**
 * Utilities for JPEG manipulation and image resizing using standard Java Image I/O.
 * Removes dependencies on proprietary com.sun and JAI APIs.
 */
public class ImageUtil {

    static {
        // disable any native libs if present
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    public static BufferedImage getBufferedImage(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Could not read image from file: " + file);
        }
        return bufferedImgWithNoAlpha(img);
    }

    public static BufferedImage getBufferedImage(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                throw new IOException("Could not read image from byte array");
            }
            return bufferedImgWithNoAlpha(img);
        }
    }

    public static int getWidth(File file) throws IOException {
        return getBufferedImage(file).getWidth();
    }

    public static int getHeight(File file) throws IOException {
        return getBufferedImage(file).getHeight();
    }

    public static int[] getWidthAndHeight(File file) throws IOException {
        BufferedImage img = getBufferedImage(file);
        return new int[]{ img.getWidth(), img.getHeight() };
    }

    /**
     * Ensures image is opaque by drawing on a white background if alpha is present.
     */
    public static BufferedImage bufferedImgWithNoAlpha(BufferedImage image) {
        BufferedImage output = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = output.createGraphics();
        if (image.getTransparency() != Transparency.OPAQUE) {
            g2d.setBackground(Color.WHITE);
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.fill(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
        }
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        image.flush();
        return output;
    }

    /**
     * Basic image building: writes PNG or JPEG based on extension.
     */
    public static void buildImageFile(byte[] bytesOfImageFile, String pathNewFile) throws IOException {
        BufferedImage image = getBufferedImage(bytesOfImageFile);
        String ext = FileUtil.extensionFormat(pathNewFile);
        if ("png".equalsIgnoreCase(ext)) {
            ImageIO.write(image, "png", new File(pathNewFile));
        } else {
            createJPEG(image, 1.0f, pathNewFile);
        }
        image.flush();
    }

    // Deprecated wrappers that delegate to unified resize method.
    @Deprecated
    public static void resizeImage(File file, int width, int height, float quality, String pathNewFile) throws IOException {
        BufferedImage image = getBufferedImage(file);
        resize(image, width, height, quality, pathNewFile, true, true, null);
    }

    @Deprecated
    public static void resizeImage(File file, int width, int height, float quality,
                                   String pathNewFile, int[] subimage) throws IOException {
        BufferedImage image = getBufferedImage(file);
        resize(image, width, height, quality, pathNewFile, true, true, subimage);
    }

    private static void resize(BufferedImage image, int thumbW, int thumbH,
                               float quality, String pathNewFile,
                               boolean priorWidth, boolean stretchWhenSmaller,
                               int[] subimage) throws IOException {
        if (subimage != null) {
            image = image.getSubimage(subimage[0], subimage[1], subimage[2], subimage[3]);
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        double thumbRatio = (double) thumbW / thumbH;
        double imageRatio = (double) imageWidth / imageHeight;

        if (priorWidth) {
            if (thumbRatio < imageRatio) {
                thumbH = (int) (thumbW / imageRatio);
            } else {
                thumbW = (int) (thumbH * imageRatio);
            }
        } else {
            if (thumbRatio < imageRatio) {
                thumbW = (int) (thumbH * imageRatio);
            } else {
                thumbH = (int) (thumbW / imageRatio);
            }
        }

        if (thumbW >= imageWidth || thumbH >= imageHeight) {
            if (stretchWhenSmaller) {
                image = poorResize(image, null, thumbW, thumbH);
                createJPEG(image, quality, pathNewFile);
            } else {
                image = poorResize(image, null, imageWidth, imageHeight);
                createJPEG(image, quality, pathNewFile);
            }
        } else {
            // assuming GraphicsUtilities.createThumbnail is available
            image = GraphicsUtilities.createThumbnail(image, thumbW, thumbH);
            createJPEG(image, quality, pathNewFile);
        }
        image.flush();
    }

    /**
     * Cropping resize: cuts in ratio then resizes.
     */
    public static BufferedImage resizeCropping(BufferedImage image, int thumbW,
                                               int thumbH, String pathNewFile,
                                               boolean stretchWhenSmaller) throws IOException {
        if (thumbW >= image.getWidth() || thumbH >= image.getHeight()) {
            image = poorResize(image, null, thumbW, thumbH);
        } else {
            image = poorResize(image, null, thumbW, thumbH);
        }
        if (pathNewFile != null) {
            String ext = FileUtil.extensionFormat(pathNewFile);
            if ("png".equalsIgnoreCase(ext)) {
                ImageIO.write(image, "png", new File(pathNewFile));
            } else {
                createJPEG(image, 1.0f, pathNewFile);
            }
            image.flush();
        }
        return image;
    }

    /**
     * Reduces image keeping aspect ratio.
     */
    public static void reduceImage(File file, int thumbW, int thumbH,
                                   String pathNewFile, boolean stretchWhenSmaller) throws IOException {
        BufferedImage image = getBufferedImage(file);
        int oW = image.getWidth();
        int oH = image.getHeight();
        double ratio = (double) oW / oH;
        if (oW > oH) {
            thumbH = (int) (thumbW / ratio);
        } else {
            thumbW = (int) (thumbH * ratio);
        }
        image = poorResize(image, null, thumbW, thumbH);
        String ext = FileUtil.extensionFormat(pathNewFile);
        if ("png".equalsIgnoreCase(ext)) {
            ImageIO.write(image, "png", new File(pathNewFile));
        } else {
            createJPEG(image, 1.0f, pathNewFile);
        }
        image.flush();
    }

    /**
     * Performs a simple resize via Graphics2D.
     */
    private static BufferedImage poorResize(Image image, Color colorSquare,
                                             int width, int height) {
        BufferedImage thumb;
        thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumb.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return thumb;
    }

    /**
     * Writes JPEG with specified quality (0.0f - 1.0f).
     */
    public static void createJPEG(BufferedImage image, float quality,
                                  String pathNewFile) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(Math.max(0f, Math.min(1f, quality)));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(new File(pathNewFile))) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
            image.flush();
        }
    }

    /**
     * Overload: write JPEG to OutputStream.
     */
    public static void createJPEG(BufferedImage image, float quality,
                                  OutputStream out) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(Math.max(0f, Math.min(1f, quality)));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
            image.flush();
        }
    }
}


