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
package it.geosolutions.jaiext.mosaic;

import it.geosolutions.jaiext.range.Range;

import java.awt.image.RenderedImage;
import java.io.Serializable;

import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;

/**
 * This class implements a Java Bean used as input values container of the for the MosaicOpImage2 class. Inside this class are contained the
 * following values:
 * 
 * <ul>
 * <li>The input image</li>
 * <li>an eventual alpha channel</li>
 * <li>an eventual ROI</li>
 * <li>an eventual Range of No Data values</li>
 * </ul>
 * 
 * The all class methods are getters and setters of all the private variables. The constructor is parameterless and the class implements the
 * Serializable interface. These characteristics are fundamental for creating a Java Bean.
 */
public class ImageMosaicBean implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -8333010416931087950L;

    private RenderedImage image;

    private PlanarImage alphaChannel;

    private ROI imageRoi;

    private Range sourceNoData;

    public RenderedImage getImage() {
        return image;
    }

    public void setImage(RenderedImage image) {
        this.image = image;
    }

    public PlanarImage getAlphaChannel() {
        return alphaChannel;
    }

    public void setAlphaChannel(PlanarImage alphaChannel) {
        this.alphaChannel = alphaChannel;
    }

    public ROI getImageRoi() {
        return imageRoi;
    }

    public void setImageRoi(ROI imageRoi) {
        this.imageRoi = imageRoi;
    }

    public Range getSourceNoData() {
        return sourceNoData;
    }

    public void setSourceNoData(Range sourceNoData) {
        this.sourceNoData = sourceNoData;
    }

    // default constructor
    public ImageMosaicBean() {
    }

}
