package it.geosolutions.jaiext.iterators;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;

/**
 * Modified version of JAI {@link RandomIterFallbackByte} that stores the tile positions in a byte array with less memory usage. The current tile is
 * not cached but it is calculated every time.
 */
public class RandomIterFallbackByteNoCache implements RandomIter {

    protected RenderedImage im;

    protected Rectangle boundsRect;

    protected SampleModel sampleModel;

    protected int boundsX;

    protected int boundsY;

    private byte[] xTiles;

    private byte[] yTiles;

    public RandomIterFallbackByteNoCache(RenderedImage im, Rectangle bounds) {
        this.im = im;

        Rectangle imBounds = new Rectangle(im.getMinX(), im.getMinY(), im.getWidth(),
                im.getHeight());
        this.boundsRect = imBounds.intersection(bounds);
        this.sampleModel = im.getSampleModel();

        this.boundsX = boundsRect.x;
        this.boundsY = boundsRect.y;

        int width = boundsRect.width;
        int height = boundsRect.height;

        this.xTiles = new byte[width];
        this.yTiles = new byte[height];

        int tileWidth = im.getTileWidth();
        int tileGridXOffset = im.getTileGridXOffset();
        int minTileX = PlanarImage.XToTileX(boundsX, tileGridXOffset, tileWidth);
        int offsetX = boundsX - PlanarImage.tileXToX(minTileX, tileGridXOffset, tileWidth);
        byte tileX = (byte) (minTileX & 0xff);

        for (int i = 0; i < width; i++) {
            xTiles[i] = tileX;
            ++offsetX;
            if (offsetX == tileWidth) {
                ++tileX;
                offsetX = 0;
            }
        }

        int tileHeight = im.getTileHeight();
        int tileGridYOffset = im.getTileGridYOffset();
        int minTileY = PlanarImage.YToTileY(boundsY, tileGridYOffset, tileHeight);
        int offsetY = boundsY - PlanarImage.tileYToY(minTileY, tileGridYOffset, tileHeight);
        byte tileY = (byte) (minTileY & 0xff);

        for (int i = 0; i < height; i++) {
            yTiles[i] = tileY;
            ++offsetY;
            if (offsetY == tileHeight) {
                ++tileY;
                offsetY = 0;
            }
        }

    }

    /**
     * Sets dataBuffer to the correct buffer for the pixel (x, y) = (xLocal + boundsRect.x, yLocal + boundsRect.y).
     * 
     * @param xLocal the X coordinate in the local coordinate system.
     * @param yLocal the Y coordinate in the local coordinate system.
     */
    private Raster makeCurrent(int xLocal, int yLocal) {

        final int tileX = xTiles[xLocal];
        final int tileY = yTiles[yLocal];

        return im.getTile(tileX, tileY);
    }

    public int getSample(int x, int y, int b) {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSample(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                tile.getDataBuffer());
    }

    public float getSampleFloat(int x, int y, int b) {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSampleFloat(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                tile.getDataBuffer());
    }

    public double getSampleDouble(int x, int y, int b) {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getSampleDouble(x - sampleModelTranslateX, y - sampleModelTranslateY, b,
                tile.getDataBuffer());
    }

    public int[] getPixel(int x, int y, int[] iArray) {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, iArray,
                tile.getDataBuffer());
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, fArray,
                tile.getDataBuffer());
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        // get tile
        Raster tile = makeCurrent(x - boundsX, y - boundsY);

        // get value
        final int sampleModelTranslateX = tile.getSampleModelTranslateX();
        final int sampleModelTranslateY = tile.getSampleModelTranslateY();

        return sampleModel.getPixel(x - sampleModelTranslateX, y - sampleModelTranslateY, dArray,
                tile.getDataBuffer());
    }

    public void done() {
        xTiles = null;
        yTiles = null;
    }
}
