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
package it.geosolutions.jaiext.lookup;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;

/** This class is an extension of the abstract class LookupTable handling Unsigned Short data types */
public class LookupTableUShort extends LookupTable {

    public LookupTableUShort(byte[] data) {
        super(data);
    }

    public LookupTableUShort(byte[] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(byte[][] data) {
        super(data);
    }

    public LookupTableUShort(byte[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(byte[][] data, int[] offsets) {
        super(data, offsets);
    }

    public LookupTableUShort(short[] data, boolean isUShort) {
        super(data, isUShort);
    }

    public LookupTableUShort(short[] data, int offset, boolean isUShort) {
        super(data, offset, isUShort);
    }

    public LookupTableUShort(short[][] data, boolean isUShort) {
        super(data, isUShort);
    }

    public LookupTableUShort(short[][] data, int offset, boolean isUShort) {
        super(data, offset, isUShort);
    }

    public LookupTableUShort(short[][] data, int[] offsets, boolean isUShort) {
        super(data, offsets, isUShort);
    }

    public LookupTableUShort(int[] data) {
        super(data);
    }

    public LookupTableUShort(int[] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(int[][] data) {
        super(data);
    }

    public LookupTableUShort(int[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(int[][] data, int[] offsets) {
        super(data, offsets);
    }

    public LookupTableUShort(float[] data) {
        super(data);
    }

    public LookupTableUShort(float[] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(float[][] data) {
        super(data);
    }

    public LookupTableUShort(float[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(float[][] data, int[] offsets) {
        super(data, offsets);
    }

    public LookupTableUShort(double[] data) {
        super(data);
    }

    public LookupTableUShort(double[] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(double[][] data) {
        super(data);
    }

    public LookupTableUShort(double[][] data, int offset) {
        super(data, offset);
    }

    public LookupTableUShort(double[][] data, int[] offsets) {
        super(data, offsets);
    }

    /**
     * Performs table lookup on a source UShort Raster, writing the result into a supplied WritableRaster. The destination must have a data type and
     * SampleModel appropriate to the results of the lookup operation. The table lookup operation is performed within a specified rectangle. If ROI or
     * no Data are present then they are taken into account.
     * 
     * <p>
     * The <code>dst</code> argument may be null, in which case a new WritableRaster is created using the appropriate SampleModel.
     * 
     * <p>
     * The rectangle of interest may be null, in which case the operation will be performed on the intersection of the source and destination bounding
     * rectangles.
     * 
     * @param source A Raster containing the source pixel data.
     * @param dst The WritableRaster to be computed, or null. If supplied, its data type and number of bands must be suitable for the source and
     *        lookup table.
     * @param rect The rectangle within the tile to be computed. If rect is null, the intersection of the source and destination bounds will be used.
     *        Otherwise, it will be clipped to the intersection of the source and destination bounds.
     */
    protected void lookup(Raster source, WritableRaster dst, Rectangle rect, Raster roi) {
        // Validate source.
        if (source == null) {
            throw new IllegalArgumentException("Source data must be present");
        }

        // If the image data type is not integral an exception is thrown
        SampleModel srcSampleModel = source.getSampleModel();
        if (!isIntegralDataType(srcSampleModel)) {
            throw new IllegalArgumentException("Only integral data type are handled");
        }

        // Source image data type and LookupTable subclass data type must be equal
        if (srcSampleModel.getDataType() != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException(
                    "Source data type must be equal to the table data type");
        }

        // Validate rectangle.
        if (rect == null) {
            rect = source.getBounds();
        } else {
            rect = rect.intersection(source.getBounds());
        }

        if (dst != null) {
            rect = rect.intersection(dst.getBounds());
        }

        // Validate destination.
        SampleModel dstSampleModel;
        if (dst == null) { // create dst according to table
            dstSampleModel = getDestSampleModel(srcSampleModel, rect.width, rect.height);
            dst = RasterFactory.createWritableRaster(dstSampleModel, new Point(rect.x, rect.y));
        } else {
            dstSampleModel = dst.getSampleModel();

            if (dstSampleModel.getTransferType() != getDataType()
                    || dstSampleModel.getNumBands() != getDestNumBands(srcSampleModel.getNumBands())) {
                throw new IllegalArgumentException(
                        "Destination image must have the same data type and band number of the Table");
            }
        }

        // Creation of the raster accessors for iterating on the source and destination tile
        int sTagID = RasterAccessor.findCompatibleTag(null, srcSampleModel);
        int dTagID = RasterAccessor.findCompatibleTag(null, dstSampleModel);

        RasterFormatTag sTag = new RasterFormatTag(srcSampleModel, sTagID);
        RasterFormatTag dTag = new RasterFormatTag(dstSampleModel, dTagID);

        RasterAccessor s = new RasterAccessor(source, rect, sTag, null);
        RasterAccessor d = new RasterAccessor(dst, rect, dTag, null);

        // Roi rasterAccessor initialization
        RasterAccessor roiAccessor = null;
        // ROI calculation only if the roi raster is present
        if (useROIAccessor) {
            // Get the source rectangle
            Rectangle srcRect = source.getBounds();
            // creation of the rasterAccessor
            roiAccessor = new RasterAccessor(roi, srcRect, RasterAccessor.findCompatibleTags(
                    new RenderedImage[] { srcROIImage }, srcROIImage)[0],
                    srcROIImage.getColorModel());
        }
        // Source and destination parameters
        int srcNumBands = s.getNumBands();

        int tblNumBands = getNumBands();
        int tblDataType = getDataType();

        int dstWidth = d.getWidth();
        int dstHeight = d.getHeight();
        int dstNumBands = d.getNumBands();
        int dstDataType = d.getDataType();

        // Source information.
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int[] srcBandOffsets = s.getBandOffsets();
        // Source data
        short[][] sSrcData = s.getShortDataArrays();
        // If source band number is less than destination band number, the source first band
        // is replicated for all the destination bands
        if (srcNumBands < dstNumBands) {
            int offset0 = srcBandOffsets[0];
            srcBandOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                srcBandOffsets[i] = offset0;
            }
            short[] sData0 = sSrcData[0];
            sSrcData = new short[dstNumBands][];
            for (int i = 0; i < dstNumBands; i++) {
                sSrcData[i] = sData0;
            }
        }

        // Table information.
        int[] tblOffsets = getOffsets();

        byte[][] bTblData = getByteData();
        short[][] sTblData = getShortData();
        int[][] iTblData = getIntData();
        float[][] fTblData = getFloatData();
        double[][] dTblData = getDoubleData();
        // If table band number is less than destination band number, then table first band
        // is replicated for all the destination bands
        if (tblNumBands < dstNumBands) {
            int offset0 = tblOffsets[0];
            tblOffsets = new int[dstNumBands];
            for (int i = 0; i < dstNumBands; i++) {
                tblOffsets[i] = offset0;
            }

            switch (tblDataType) {
            case DataBuffer.TYPE_BYTE:
                byte[] bData0 = bTblData[0];
                bTblData = new byte[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    bTblData[i] = bData0;
                }
                break;
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
                short[] sData0 = sTblData[0];
                sTblData = new short[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    sTblData[i] = sData0;
                }
                break;
            case DataBuffer.TYPE_INT:
                int[] iData0 = iTblData[0];
                iTblData = new int[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    iTblData[i] = iData0;
                }
                break;
            case DataBuffer.TYPE_FLOAT:
                float[] fData0 = fTblData[0];
                fTblData = new float[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    fTblData[i] = fData0;
                }
                break;
            case DataBuffer.TYPE_DOUBLE:
                double[] dData0 = dTblData[0];
                dTblData = new double[dstNumBands][];
                for (int i = 0; i < dstNumBands; i++) {
                    dTblData[i] = dData0;
                }
            }
        }

        // Destination information.
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();

        byte[][] bDstData = d.getByteDataArrays();
        short[][] sDstData = d.getShortDataArrays();
        int[][] iDstData = d.getIntDataArrays();
        float[][] fDstData = d.getFloatDataArrays();
        double[][] dDstData = d.getDoubleDataArrays();

        switch (dstDataType) {
        case DataBuffer.TYPE_BYTE:
            // Lookup operation for all the destination image types
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, sSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, bDstData,
                    tblOffsets, bTblData, roiAccessor, rect);
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, sSrcData, dstWidth,
                    dstHeight, dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets,
                    sDstData, tblOffsets, sTblData, roiAccessor, rect);

            break;

        case DataBuffer.TYPE_INT:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, sSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, iDstData,
                    tblOffsets, iTblData, roiAccessor, rect);
            break;

        case DataBuffer.TYPE_FLOAT:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, sSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, fDstData,
                    tblOffsets, fTblData, roiAccessor, rect);
            break;

        case DataBuffer.TYPE_DOUBLE:
            lookup(srcLineStride, srcPixelStride, srcBandOffsets, sSrcData, dstWidth, dstHeight,
                    dstNumBands, dstLineStride, dstPixelStride, dstBandOffsets, dDstData,
                    tblOffsets, dTblData, roiAccessor, rect);
            break;
        }

        d.copyDataToRaster();
    }

    // ushort to byte
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, byte[][] bDstData, int[] tblOffsets,
            byte[][] bTblData, RasterAccessor roi, Rectangle destRect) {

        // Destination image bounds
        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        // ROI parameters
        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        // Boolean indicating the possible situations: with or without ROI,
        // with or without No Data, and a special case when table data are not present
        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final byte[] d = bDstData[b];
                final byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int w = 0; w < dstWidth; w++) {
                        // Output value is taken from the table array
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            // Cycle on all the bands
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                byte[] d = bDstData[b];
                byte[] t = bTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];
                // Cycle on all the y dimension
                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;
                    // Cycle on all the x dimension
                    for (int x = 0; x < dstWidth; x++) {
                        // If the value is a not a noData, the table value is stored
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataByte;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // Calculation of the x position
                            int posx = (x - dst_min_x) * srcPixelStride;
                            // Calculation of the roi data array index
                            int windex = (posx / dstNumBands) + posyROI;
                            // From the selected index the value is taken
                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;
                            // If the roi value is 0 the value is outside the ROI, else the table value
                            // is taken

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataByte;
                            } else {
                                // If the value is a not a noData, the table value is stored
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                // Cycle on all the bands
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    byte[] d = bDstData[b];
                    byte[] t = bTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];
                    // Cycle on all the y dimension
                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;
                        // Cycle on all the x dimension
                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            // If the sample is inside ROI bounds
                            if (roiBounds.contains(x, y)) {
                                // ROI pixel value is calculated
                                int w = roiIter.getSample(x, y, 0);
                                // if is 0 means that the pixel is outside the ROI, else the table data is taken
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataByte;
                                } else {
                                    // If the value is a not a noData, the table value is stored
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataByte;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataByte;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // ushort to ushort/short
    private void lookup(int srcLineStride, int srcPixelStride,
            int[] srcBandOffsets, short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands,
            int dstLineStride, int dstPixelStride, int[] dstBandOffsets, short[][] sDstData,
            int[] tblOffsets, short[][] sTblData, RasterAccessor roi, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final short[] d = sDstData[b];
                final short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                short[] d = sDstData[b];
                short[] t = sTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataShort;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataShort;
                            } else {
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    short[] d = sDstData[b];
                    short[] t = sTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataShort;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataShort;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataShort;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        }
    }

    // ushort to int
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, int[][] iDstData, int[] tblOffsets,
            int[][] iTblData, RasterAccessor roi, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;
        final boolean caseNull = iTblData == null;

        if (caseA) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = data.getElem(b, s[srcPixelOffset] & 0xFFFF);

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];
                    final int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int h = 0; h < dstHeight; h++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int w = 0; w < dstWidth; w++) {
                            d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else if (caseB) {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = data.getElem(b, s[srcPixelOffset] & 0xFFFF);
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = data.getElem(b,
                                                s[srcPixelOffset] & 0xFFFF);
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {
                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF)
                                                - tblOffset];
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        } else if (caseC) {
            if (caseNull) {
                for (int b = 0; b < dstNumBands; b++) {
                    final short[] s = sSrcData[b];
                    final int[] d = iDstData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {

                            short value = (short) (s[srcPixelOffset] & 0xFFFF);
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = data.getElem(b, value);
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    int[] d = iDstData[b];
                    int[] t = iTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = 0; y < dstHeight; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = 0; x < dstWidth; x++) {
                            short value = (short) (s[srcPixelOffset] & 0xFFFF);
                            if (noData.contains(value)) {
                                d[dstPixelOffset] = destinationNoDataInt;
                            } else {
                                d[dstPixelOffset] = t[value - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        } else {
            if (useROIAccessor) {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = data.getElem(b, value);
                                    }
                                }

                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            int posyROI = (y - dst_min_y) * roiLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                int posx = (x - dst_min_x) * srcPixelStride;

                                int windex = (posx / dstNumBands) + posyROI;

                                int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            } else {
                if (caseNull) {
                    for (int b = 0; b < dstNumBands; b++) {
                        final short[] s = sSrcData[b];
                        final int[] d = iDstData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = data.getElem(b, value);
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                } else {
                    for (int b = 0; b < dstNumBands; b++) {
                        short[] s = sSrcData[b];
                        int[] d = iDstData[b];
                        int[] t = iTblData[b];

                        int srcLineOffset = srcBandOffsets[b];
                        int dstLineOffset = dstBandOffsets[b];
                        int tblOffset = tblOffsets[b];

                        for (int y = dst_min_y; y < dst_max_y; y++) {
                            int srcPixelOffset = srcLineOffset;
                            int dstPixelOffset = dstLineOffset;

                            srcLineOffset += srcLineStride;
                            dstLineOffset += dstLineStride;

                            for (int x = dst_min_x; x < dst_max_x; x++) {

                                if (roiBounds.contains(x, y)) {
                                    int w = roiIter.getSample(x, y, 0);
                                    if (w == 0) {
                                        d[dstPixelOffset] = destinationNoDataInt;
                                    } else {
                                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                        if (noData.contains(value)) {
                                            d[dstPixelOffset] = destinationNoDataInt;
                                        } else {
                                            d[dstPixelOffset] = t[value - tblOffset];
                                        }
                                    }
                                } else {
                                    d[dstPixelOffset] = destinationNoDataInt;
                                }
                                srcPixelOffset += srcPixelStride;
                                dstPixelOffset += dstPixelStride;
                            }
                        }
                    }
                }
            }
        }
    }

    // ushort to float
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, float[][] fDstData, int[] tblOffsets,
            float[][] fTblData, RasterAccessor roi, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final float[] d = fDstData[b];
                final float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                float[] d = fDstData[b];
                float[] t = fTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataFloat;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            } else {
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    float[] d = fDstData[b];
                    float[] t = fTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataFloat;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataFloat;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataFloat;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }
            }
        }
    }

    // ushort to double
    private void lookup(int srcLineStride, int srcPixelStride, int[] srcBandOffsets,
            short[][] sSrcData, int dstWidth, int dstHeight, int dstNumBands, int dstLineStride,
            int dstPixelStride, int[] dstBandOffsets, double[][] dDstData, int[] tblOffsets,
            double[][] dTblData, RasterAccessor roi, Rectangle destRect) {

        final int dst_min_x = destRect.x;
        final int dst_min_y = destRect.y;
        final int dst_max_x = destRect.x + destRect.width;
        final int dst_max_y = destRect.y + destRect.height;

        int roiLineStride = 0;
        byte[] roiDataArray = null;
        int roiDataLength = 0;
        if (useROIAccessor) {
            roiDataArray = roi.getByteDataArray(0);
            roiDataLength = roiDataArray.length;
            roiLineStride = roi.getScanlineStride();
        }

        final boolean caseA = !hasROI && !hasNoData;
        final boolean caseB = hasROI && !hasNoData;
        final boolean caseC = !hasROI && hasNoData;

        if (caseA) {
            for (int b = 0; b < dstNumBands; b++) {
                final short[] s = sSrcData[b];
                final double[] d = dDstData[b];
                final double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int h = 0; h < dstHeight; h++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int w = 0; w < dstWidth; w++) {
                        d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else if (caseB) {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {
                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[(s[srcPixelOffset] & 0xFFFF) - tblOffset];
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }

                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        } else if (caseC) {
            for (int b = 0; b < dstNumBands; b++) {
                short[] s = sSrcData[b];
                double[] d = dDstData[b];
                double[] t = dTblData[b];

                int srcLineOffset = srcBandOffsets[b];
                int dstLineOffset = dstBandOffsets[b];
                int tblOffset = tblOffsets[b];

                for (int y = 0; y < dstHeight; y++) {
                    int srcPixelOffset = srcLineOffset;
                    int dstPixelOffset = dstLineOffset;

                    srcLineOffset += srcLineStride;
                    dstLineOffset += dstLineStride;

                    for (int x = 0; x < dstWidth; x++) {
                        short value = (short) (s[srcPixelOffset] & 0xFFFF);
                        if (noData.contains(value)) {
                            d[dstPixelOffset] = destinationNoDataDouble;
                        } else {
                            d[dstPixelOffset] = t[value - tblOffset];
                        }

                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                }
            }

        } else {
            if (useROIAccessor) {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        int posyROI = (y - dst_min_y) * roiLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            int posx = (x - dst_min_x) * srcPixelStride;

                            int windex = (posx / dstNumBands) + posyROI;

                            int w = windex < roiDataLength ? roiDataArray[windex] & 0xff : 0;

                            if (w == 0) {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            } else {
                                short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                if (noData.contains(value)) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    d[dstPixelOffset] = t[value - tblOffset];
                                }
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            } else {
                for (int b = 0; b < dstNumBands; b++) {
                    short[] s = sSrcData[b];
                    double[] d = dDstData[b];
                    double[] t = dTblData[b];

                    int srcLineOffset = srcBandOffsets[b];
                    int dstLineOffset = dstBandOffsets[b];
                    int tblOffset = tblOffsets[b];

                    for (int y = dst_min_y; y < dst_max_y; y++) {
                        int srcPixelOffset = srcLineOffset;
                        int dstPixelOffset = dstLineOffset;

                        srcLineOffset += srcLineStride;
                        dstLineOffset += dstLineStride;

                        for (int x = dst_min_x; x < dst_max_x; x++) {

                            if (roiBounds.contains(x, y)) {
                                int w = roiIter.getSample(x, y, 0);
                                if (w == 0) {
                                    d[dstPixelOffset] = destinationNoDataDouble;
                                } else {
                                    short value = (short) (s[srcPixelOffset] & 0xFFFF);
                                    if (noData.contains(value)) {
                                        d[dstPixelOffset] = destinationNoDataDouble;
                                    } else {
                                        d[dstPixelOffset] = t[value - tblOffset];
                                    }
                                }
                            } else {
                                d[dstPixelOffset] = destinationNoDataDouble;
                            }
                            srcPixelOffset += srcPixelStride;
                            dstPixelOffset += dstPixelStride;
                        }
                    }
                }

            }
        }
    }
}
