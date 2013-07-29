package it.geosolutions.jaiext.scale;

import static org.junit.Assert.assertEquals;
import it.geosolutions.jaiext.interpolators.InterpolationBicubicNew;
import it.geosolutions.jaiext.interpolators.InterpolationBilinearNew;
import it.geosolutions.jaiext.interpolators.InterpolationNearestNew;
import it.geosolutions.jaiext.scale.ScaleNoDataDescriptor;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.stream.FileImageInputStream;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROIShape;
import org.geotools.renderedimage.viewer.RenderedImageBrowser;
import org.junit.Test;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi;


/**
 * This class extends the TestScale class and tests the Scale operation on a RGB image. If the user want to see
 * the result, must set the JAI.Ext.Interactive parameter to true and JAI.Ext.TestSelector from 0 to 2 (the 3 
 * interpolation types) to the Console. The ROI is created by the roiCreation() method and its height and width
 * are half of the RGB image height and width. The 3 tests with the different interpolation types are executed by 
 * calling 3 times the testImage() method and changing each time the selected interpolation.  
 */
public class ImageRGBTest extends TestScale {
	/** RGB image width*/
    private int imageWidth;
    /** RGB image height*/
    private int imageHeigth;
    
    
    @Test
    public void testInterpolation() throws Throwable {

        boolean bicubic2Disabled = true;
        boolean useROIAccessor = true;
        boolean roiUsed = true;

        TIFFImageReader reader = null;

        FileImageInputStream stream_in = null;

        try {

            // Instantiation of the file-reader
            reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();

            File inputFile = new File(
                    "../jt-utilities/src/test/resources/it/geosolutions/jaiext/images/testImageLittle.tif");
            // Instantiation of the imageinputstream and imageoutputstrem
            stream_in = new FileImageInputStream(inputFile);

            // Setting the inputstream to the reader
            reader.setInput(stream_in);
            // Creation of a Renderedimage to store the image
            RenderedImage image = reader.readAsRenderedImage(0, null);

            imageWidth = image.getWidth();
            imageHeigth = image.getHeight();

            int dataType = image.getSampleModel().getDataType();

            testImage(image, useROIAccessor,roiUsed, bicubic2Disabled, dataType,
                    InterpolationType.NEAREST_INTERP);

            testImage(image, useROIAccessor,roiUsed, bicubic2Disabled, dataType,
                    InterpolationType.BILINEAR_INTERP);

            testImage(image, useROIAccessor,roiUsed, bicubic2Disabled, dataType,
                    InterpolationType.BICUBIC_INTERP);

        } finally {
            try {
                if (reader != null) {
                    reader.dispose();
                }
            } catch (Exception e) {
            }

            try {
                if (stream_in != null) {
                    stream_in.flush();
                    stream_in.close();
                }
            } catch (Exception e) {
            }

        }

    }

    protected ROIShape roiCreation() {
        int roiHeight = imageHeigth / 2;
        int roiWidth = imageWidth / 2;

        Rectangle roiBound = new Rectangle(0, 0, roiWidth, roiHeight);

        ROIShape roi = new ROIShape(roiBound);
        return roi;
    }

    private void testImage(RenderedImage sourceImage, boolean useROIAccessor,boolean roiUsed,
            boolean bicubic2Disabled, int dataType, InterpolationType interpType) {

        // Hints are used only with roiAccessor
        RenderingHints hints = null;
        // ROI creation
        ROIShape roi = null;
        if(roiUsed){
            if (useROIAccessor) {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
            }
            roi=roiCreation();
        }else{
            useROIAccessor=false;
        }



        // Interpolator initialization
        Interpolation interp = null;

        // Interpolators
        switch (interpType) {
        case NEAREST_INTERP:
            // Nearest-Neighbor
            interp = new InterpolationNearestNew(null, useROIAccessor, destinationNoData, dataType);
            break;
        case BILINEAR_INTERP:
            // Bilinear
            interp = new InterpolationBilinearNew(DEFAULT_SUBSAMPLE_BITS, null, useROIAccessor,
                    destinationNoData, dataType);
            if(hints!=null){
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
            } else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }

            break;
        case BICUBIC_INTERP:
            // Bicubic
            interp = new InterpolationBicubicNew(DEFAULT_SUBSAMPLE_BITS, null, useROIAccessor,
                    destinationNoData, dataType, bicubic2Disabled, DEFAULT_PRECISION_BITS);
            if(hints!=null){
                hints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
            }else {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            }
            break;
        default:
            throw new IllegalArgumentException("..."); 
        }

        // Scale operation
        RenderedImage destinationIMG = ScaleNoDataDescriptor.create(sourceImage, scaleX, scaleY,
                transX, transY, interp, roi, useROIAccessor, hints);

        if (INTERACTIVE && TEST_SELECTOR == interpType.getType()) {
            RenderedImageBrowser.showChain(destinationIMG, false, roiUsed);
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            
            // Forcing to retrieve an array of all the image tiles
            // image tile calculation for searching possible errors
            ((PlanarImage)destinationIMG).getTiles();
        }


        // Control if the scale operation has been correctly performed
        // width
        assertEquals((int) (imageWidth  * scaleX), destinationIMG.getWidth());
        // height
        assertEquals((int) (imageHeigth * scaleY), destinationIMG.getHeight());

    }
}
