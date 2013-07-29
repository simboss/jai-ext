package it.geosolutions.jaiext.scale;




import org.junit.Test;

/**
 * This test-class extends the TestScale class and is used for testing the bicubic interpolation inside the Scale operation.
 * The first method tests the scale operation without the presence of a ROI or a No Data Range. The 2nd method introduces a 
 * ROI object calculated using a ROI RasterAccessor while the 3rd method uses an Iterator on the ROI Object. The 4th method 
 * performs the scale operation with all the components. The last method is similar to the 4th method but executes its operations 
 * on binary images. 
 */
public class BicubicScaleTest extends TestScale{
    @Test
    public void testImageScaling() {
        boolean roiPresent=false;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
              
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BICUBIC_INTERP, TestSelection.NO_ROI_ONLY_DATA);
    }

    @Test
    public void testImageScalingROIAccessor() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ACCESSOR_ONLY_DATA);
    }

    @Test
    public void testImageScalingROIBounds() {
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=false;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=false;
              
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ONLY_DATA);
    }
    
    @Test
    public void testImageScalingTotal() {        
        
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=false;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
              
        
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BICUBIC_INTERP,TestSelection.ROI_ACCESSOR_NO_DATA);
    }
    
    @Test
    public void testImageScalingBinary() {
        boolean roiPresent=true;
        boolean noDataRangeUsed=true;
        boolean isBinary=true;
        boolean bicubic2DIsabled= true;
        boolean useROIAccessor=true;
                      
        testGlobal(useROIAccessor,isBinary,bicubic2DIsabled,noDataRangeUsed
                ,roiPresent,InterpolationType.BICUBIC_INTERP,TestSelection.BINARY_ROI_ACCESSOR_NO_DATA);
    }

}
