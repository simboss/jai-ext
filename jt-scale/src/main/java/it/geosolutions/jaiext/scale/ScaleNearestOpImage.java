/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
*    http://www.geo-solutions.it/
*    Copyright 2014 GeoSolutions


* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at

* http://www.apache.org/licenses/LICENSE-2.0

* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package it.geosolutions.jaiext.scale;

import it.geosolutions.jaiext.interpolators.InterpolationNearest;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

public class ScaleNearestOpImage extends ScaleOpImage {

    /** Nearest-Neighbor interpolator */
    protected InterpolationNearest interpN = null;

    /** Byte lookuptable used if no data are present */
    protected final byte[] byteLookupTable = new byte[256];

    public ScaleNearestOpImage(RenderedImage source, ImageLayout layout, Map configuration,
            BorderExtender extender, Interpolation interp, float scaleX, float scaleY,
            float transX, float transY, boolean useRoiAccessor) {
        super(source, layout, configuration, true, extender, interp, scaleX, scaleY, transX,
                transY, useRoiAccessor);
        scaleOpInitialization(source, interp);
    }

    private void scaleOpInitialization(RenderedImage source, Interpolation interp) {
        // If the source has an IndexColorModel, override the default setting
        // in OpImage. The dest shall have exactly the same SampleModel and
        // ColorModel as the source.
        // Note, in this case, the source should have an integral data type.
        ColorModel srcColorModel = source.getColorModel();
        if (srcColorModel instanceof IndexColorModel) {
            sampleModel = source.getSampleModel()
                    .createCompatibleSampleModel(tileWidth, tileHeight);
            colorModel = srcColorModel;
        }

        SampleModel sm = source.getSampleModel();
        // Source image data Type
        int srcDataType = sm.getDataType();

        // selection of the inverse scale parameters both for the x and y axis
        if (invScaleXRational.num > invScaleXRational.denom) {
            invScaleXInt = invScaleXRational.num / invScaleXRational.denom;
            invScaleXFrac = invScaleXRational.num % invScaleXRational.denom;
        } else {
            invScaleXInt = 0;
            invScaleXFrac = invScaleXRational.num;
        }

        if (invScaleYRational.num > invScaleYRational.denom) {
            invScaleYInt = invScaleYRational.num / invScaleYRational.denom;
            invScaleYFrac = invScaleYRational.num % invScaleYRational.denom;
        } else {
            invScaleYInt = 0;
            invScaleYFrac = invScaleYRational.num;
        }

        // Interpolator settings
        interpolator = interp;

        if (interpolator instanceof InterpolationNearest) {
            interpN = (InterpolationNearest) interpolator;
            this.interp = interpN;
            interpN.setROIdata(roiBounds, roiIter);
            noData = interpN.getNoDataRange();
            if (noData != null) {
                hasNoData = true;
                destinationNoDataDouble = interpN.getDestinationNoData();
            } else if (hasROI) {
                destinationNoDataDouble = interpN.getDestinationNoData();
            }
        }
        // subsample bits used for the bilinear and bicubic interpolation
        subsampleBits = interp.getSubsampleBitsH();

        // Internal precision required for position calculations
        one = 1 << subsampleBits;

        // Subsampling related variables
        shift2 = 2 * subsampleBits;
        round2 = 1 << (shift2 - 1);

        // Number of subsample positions
        one = 1 << subsampleBits;

        // Get the width and height and padding of the Interpolation kernel.
        interp_width = interp.getWidth();
        interp_height = interp.getHeight();
        interp_left = interp.getLeftPadding();
        interp_top = interp.getTopPadding();

        // Selection of the destination No Data
        switch (srcDataType) {
        case DataBuffer.TYPE_BYTE:
            destinationNoDataByte = (byte) (((byte) destinationNoDataDouble) & 0xff);
            // Creation of a lookuptable containing the values to use for no data
            if (hasNoData) {
                for (int i = 0; i < byteLookupTable.length; i++) {
                    byte value = (byte) i;
                    if (noData.contains(value)) {
                        byteLookupTable[i] = destinationNoDataByte;
                    } else {
                        byteLookupTable[i] = value;
                    }
                }
            }
            break;
        case DataBuffer.TYPE_USHORT:
            destinationNoDataUShort = (short) (((short) destinationNoDataDouble) & 0xffff);
            break;
        case DataBuffer.TYPE_SHORT:
            destinationNoDataShort = (short) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_INT:
            destinationNoDataInt = (int) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_FLOAT:
            destinationNoDataFloat = (float) destinationNoDataDouble;
            break;
        case DataBuffer.TYPE_DOUBLE:
            break;
        default:
            throw new IllegalArgumentException("Wrong data Type");
        }

        //Definition of the possible cases that can be found
        // caseA = no ROI nor No Data
        // caseB = ROI present but No Data not present
        // caseC = No Data present but ROI not present
        // Last case not defined = both ROI and No Data are present
        caseA = !hasROI && !hasNoData;
        caseB = hasROI && !hasNoData;
        caseC = !hasROI && hasNoData;
       

    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        computeRect(sources, dest, destRect, null);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect,
            Raster[] rois) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();
        // Only one source raster is used
        Raster source = sources[0];

        // Get the source rectangle
        Rectangle srcRect = source.getBounds();

        // SRC and destination accessors are used for simplifying calculations
        RasterAccessor srcAccessor = new RasterAccessor(source, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());

        RasterAccessor dstAccessor = new RasterAccessor(dest, destRect, formatTags[1],
                getColorModel());

        // Destination rectangle dimensions
        int dwidth = destRect.width;
        int dheight = destRect.height;
        // From the rasterAccessor are calculated the pixelStride and the scanLineStride
        int srcPixelStride = srcAccessor.getPixelStride();
        int srcScanlineStride = srcAccessor.getScanlineStride();
        // Initialization of the x and y position array
        int[] xpos = new int[dwidth];
        int[] ypos = new int[dheight];

        // ROI support
        int[] yposRoi = null;
        // Scanline stride. It is used as integer because it can return null values
        int roiScanlineStride = 0;
        // Roi rasterAccessor initialization
        RasterAccessor roiAccessor = null;
        // Roi raster initialization
        Raster roi = null;

        // ROI calculation only if the roi raster is present
        if (useRoiAccessor) {
            // Selection of the roi raster
            roi = rois[0];
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
            // ROI scanlinestride
            roiScanlineStride = roiAccessor.getScanlineStride();
            // Initialization of the roi y position array
            yposRoi = new int[dheight];

        }

        // Initialization of the x and y fractional array
        int[] xfracValues = new int[dwidth];
        int[] yfracValues = new int[dheight];

        preComputePositionsInt(destRect, srcRect.x, srcRect.y, srcPixelStride, srcScanlineStride,
                xpos, ypos, xfracValues, yfracValues, roiScanlineStride, yposRoi);
        // destination data type
        dataType = dest.getSampleModel().getDataType();

        // This methods differs only for the presence of the roi or if the image is a binary one

        // if is binary
        // computeLoopBynary(srcAccessor, source, dest, destRect, xpos, ypos,yposRoi, xfracvalues,
        // yfracvalues,roi,yposRoi,srcRect.x, srcRect.y);

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, srcRect, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, destRect, dstAccessor, xpos, ypos, roiAccessor, yposRoi);
            break;
        }

    }

    private void byteLoop(RasterAccessor src, Rectangle srcRect, Rectangle dstRect,
            RasterAccessor dst, int[] xpos, int[] ypos, RasterAccessor roi, int[] yposRoi) {

        // BandOffsets
        final int srcScanlineStride = src.getScanlineStride();
        final int srcPixelStride = src.getPixelStride();
        final int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        final int dwidth = dstRect.width;
        final int dheight = dstRect.height;
        // Destination image band numbers
        final int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        final byte[][] srcDataArrays = src.getByteDataArrays();

        final byte[][] dstDataArrays = dst.getByteDataArrays();

        final byte[] roiDataArray;
        final int roiDataLength;
        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        } else {
            roiDataArray = null;
            roiDataLength = 0;
        }
        
        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                final byte[] srcData = srcDataArrays[k];
                final byte[] dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    int posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection

                        int posx = xpos[i];
                        int pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            int posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];

                                int pos = posx + posy;

                                int windex = (posx / dnumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                int pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    int w = roiIter.getSample(x0, y0, 0) & 0xff;
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataByte;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }

                }
            } else {
                if (caseC) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        final byte[] srcData = srcDataArrays[k];
                        final byte[] dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            int posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                int posx = xpos[i];
                                int pos = posx + posy;

                                int value = srcData[pos];

                                dstData[dstPixelOffset] = byteLookupTable[value&0xFF];

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    if (useRoiAccessor) {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final byte[] srcData = srcDataArrays[k];
                            final byte[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // roi y position initialization
                                int posyROI = yposRoi[j];
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];

                                    int pos = posx + posy;

                                    int value = srcData[pos];

                                    if (byteLookupTable[value&0xFF] == destinationNoDataByte) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        int windex = (posx / dnumBands) + posyROI;
                                        int w = windex < roiDataLength ? roiDataArray[windex] & 0xff
                                                : 0;

                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataByte;
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = byteLookupTable[value&0xFF];
                                        }
                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    } else {
                        // for all bands
                        for (int k = 0; k < dnumBands; k++) {

                            final byte[] srcData = srcDataArrays[k];
                            final byte[] dstData = dstDataArrays[k];
                            // Line and band Offset initialization
                            int dstlineOffset = dstBandOffsets[k];
                            int bandOffset = bandOffsets[k];
                            // cycle on the y values
                            for (int j = 0; j < dheight; j++) {
                                // pixel offset initialization
                                int dstPixelOffset = dstlineOffset;
                                // y position selection
                                int posy = ypos[j] + bandOffset;
                                // cycle on the x values
                                for (int i = 0; i < dwidth; i++) {
                                    // x position selection
                                    int posx = xpos[i];
                                    int pos = posx + posy;
                                    int value = srcData[pos];

                                    if (byteLookupTable[value&0xFF] == destinationNoDataByte) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        // PixelPositions
                                        int x0 = src.getX() + posx / srcPixelStride;
                                        int y0 = src.getY() + (posy - bandOffset)
                                                / srcScanlineStride;

                                        if (roiBounds.contains(x0, y0)) {
                                            int w = roiIter.getSample(x0, y0, 0) & 0xff;
                                            if (w == 0) {
                                                // The destination no data value is saved in the destination array
                                                dstData[dstPixelOffset] = destinationNoDataByte;
                                            } else {
                                                // The interpolated value is saved in the destination array
                                                dstData[dstPixelOffset] = byteLookupTable[value&0xFF];
                                            }
                                        } else {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataByte;
                                        }
                                    }
                                    // destination pixel offset update
                                    dstPixelOffset += dstPixelStride;
                                }
                                // destination line offset update
                                dstlineOffset += dstScanlineStride;
                            }
                        }
                    }
                }
            }
        }
    }

    private void ushortLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        short[][] srcDataArrays = src.getShortDataArrays();

        short[][] dstDataArrays = dst.getShortDataArrays();

        // Destination and source data arrays (for a single band)
        short[] dstData = null;
        short[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] & 0xffff : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0) & 0xffff;
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            short value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataUShort;
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
                            }

                            // destination pixel offset update
                            dstPixelOffset += dstPixelStride;
                        }
                        // destination line offset update
                        dstlineOffset += dstScanlineStride;
                    }
                }
            } else {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] & 0xffff : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataUShort;
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0) & 0xffff;
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataUShort;
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataUShort;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            }
        }
    }

    private void shortLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        short[][] srcDataArrays = src.getShortDataArrays();

        short[][] dstDataArrays = dst.getShortDataArrays();

        // Destination and source data arrays (for a single band)
        short[] dstData = null;
        short[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            short value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
                            }

                            // destination pixel offset update
                            dstPixelOffset += dstPixelStride;
                        }
                        // destination line offset update
                        dstlineOffset += dstScanlineStride;
                    }
                }
            } else {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                short value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataShort;
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataShort;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            }
        }
    }

    private void intLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi) {
        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        int[][] srcDataArrays = src.getIntDataArrays();

        int[][] dstDataArrays = dst.getIntDataArrays();

        // Destination and source data arrays (for a single band)
        int[] dstData = null;
        int[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            int value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
                            }

                            // destination pixel offset update
                            dstPixelOffset += dstPixelStride;
                        }
                        // destination line offset update
                        dstlineOffset += dstScanlineStride;
                    }
                }
            } else {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                int value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                int value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataInt;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        float[][] srcDataArrays = src.getFloatDataArrays();

        float[][] dstDataArrays = dst.getFloatDataArrays();

        // Destination and source data arrays (for a single band)
        float[] dstData = null;
        float[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            float value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
                            }

                            // destination pixel offset update
                            dstPixelOffset += dstPixelStride;
                        }
                        // destination line offset update
                        dstlineOffset += dstScanlineStride;
                    }
                }
            } else {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                float value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                float value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataFloat;
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataFloat;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, Rectangle dstRect, RasterAccessor dst, int[] xpos,
            int[] ypos, RasterAccessor roi, int[] yposRoi) {

        // BandOffsets
        int srcScanlineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int bandOffsets[] = src.getBandOffsets();
        // Destination rectangle dimensions
        int dwidth = dstRect.width;
        int dheight = dstRect.height;
        // Destination image band numbers
        int dnumBands = dst.getNumBands();
        // Destination bandOffsets, PixelStride and ScanLineStride
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        // Destination and source data arrays (for all bands)
        double[][] srcDataArrays = src.getDoubleDataArrays();

        double[][] dstDataArrays = dst.getDoubleDataArrays();

        // Destination and source data arrays (for a single band)
        double[] dstData = null;
        double[] srcData = null;

        byte[] roiDataArray = null;
        int roiDataLength = 0;

        int w, posx, posy, pos, posyROI, windex;

        if (useRoiAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
        }

        if (caseA) {
            // for all bands
            for (int k = 0; k < dnumBands; k++) {

                srcData = srcDataArrays[k];
                dstData = dstDataArrays[k];
                // Line and band Offset initialization
                int dstlineOffset = dstBandOffsets[k];
                int bandOffset = bandOffsets[k];
                // cycle on the y values
                for (int j = 0; j < dheight; j++) {
                    // pixel offset initialization
                    int dstPixelOffset = dstlineOffset;
                    // y position selection
                    posy = ypos[j] + bandOffset;
                    // cycle on the x values
                    for (int i = 0; i < dwidth; i++) {
                        // x position selection
                        posx = xpos[i];

                        pos = posx + posy;
                        // The interpolated value is saved in the destination array
                        dstData[dstPixelOffset] = srcData[pos];

                        // destination pixel offset update
                        dstPixelOffset += dstPixelStride;
                    }
                    // destination line offset update
                    dstlineOffset += dstScanlineStride;
                }
            }
        } else {
            if (caseB) {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {
                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];
                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                windex = (posx / dnumBands) + posyROI;
                                w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                if (w == 0) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    // The interpolated value is saved in the destination array
                                    dstData[dstPixelOffset] = srcData[pos];
                                }

                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];
                                pos = posx + posy;
                                // PixelPositions
                                int x0 = src.getX() + posx / srcPixelStride;
                                int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                if (roiBounds.contains(x0, y0)) {
                                    w = roiIter.getSample(x0, y0, 0);
                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = srcData[pos];
                                    }
                                } else {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            } else if (caseC) {
                // for all bands
                for (int k = 0; k < dnumBands; k++) {

                    srcData = srcDataArrays[k];
                    dstData = dstDataArrays[k];

                    // Line and band Offset initialization
                    int dstlineOffset = dstBandOffsets[k];
                    int bandOffset = bandOffsets[k];
                    // cycle on the y values
                    for (int j = 0; j < dheight; j++) {
                        // pixel offset initialization
                        int dstPixelOffset = dstlineOffset;
                        // y position selection
                        posy = ypos[j] + bandOffset;
                        // cycle on the x values
                        for (int i = 0; i < dwidth; i++) {
                            // x position selection
                            posx = xpos[i];

                            pos = posx + posy;

                            double value = srcData[pos];

                            if (noData.contains(value)) {
                                // The destination no data value is saved in the destination array
                                dstData[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                // The interpolated value is saved in the destination array
                                dstData[dstPixelOffset] = value;
                            }

                            // destination pixel offset update
                            dstPixelOffset += dstPixelStride;
                        }
                        // destination line offset update
                        dstlineOffset += dstScanlineStride;
                    }
                }
            } else {
                if (useRoiAccessor) {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // roi y position initialization
                            posyROI = yposRoi[j];
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                double value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    windex = (posx / dnumBands) + posyROI;
                                    w = windex < roiDataLength ? roiDataArray[windex] : 0;

                                    if (w == 0) {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        // The interpolated value is saved in the destination array
                                        dstData[dstPixelOffset] = value;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                } else {
                    // for all bands
                    for (int k = 0; k < dnumBands; k++) {

                        srcData = srcDataArrays[k];
                        dstData = dstDataArrays[k];

                        // Line and band Offset initialization
                        int dstlineOffset = dstBandOffsets[k];
                        int bandOffset = bandOffsets[k];
                        // cycle on the y values
                        for (int j = 0; j < dheight; j++) {
                            // pixel offset initialization
                            int dstPixelOffset = dstlineOffset;
                            // y position selection
                            posy = ypos[j] + bandOffset;
                            // cycle on the x values
                            for (int i = 0; i < dwidth; i++) {
                                // x position selection
                                posx = xpos[i];

                                pos = posx + posy;

                                double value = srcData[pos];

                                if (noData.contains(value)) {
                                    // The destination no data value is saved in the destination array
                                    dstData[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    // PixelPositions
                                    int x0 = src.getX() + posx / srcPixelStride;
                                    int y0 = src.getY() + (posy - bandOffset) / srcScanlineStride;

                                    if (roiBounds.contains(x0, y0)) {
                                        w = roiIter.getSample(x0, y0, 0);
                                        if (w == 0) {
                                            // The destination no data value is saved in the destination array
                                            dstData[dstPixelOffset] = destinationNoDataDouble;
                                        } else {
                                            // The interpolated value is saved in the destination array
                                            dstData[dstPixelOffset] = value;
                                        }
                                    } else {
                                        // The destination no data value is saved in the destination array
                                        dstData[dstPixelOffset] = destinationNoDataDouble;
                                    }
                                }
                                // destination pixel offset update
                                dstPixelOffset += dstPixelStride;
                            }
                            // destination line offset update
                            dstlineOffset += dstScanlineStride;
                        }
                    }
                }
            }
        }
    }
}
