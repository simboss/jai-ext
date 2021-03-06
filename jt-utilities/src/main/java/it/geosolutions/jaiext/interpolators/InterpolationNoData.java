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
package it.geosolutions.jaiext.interpolators;

import it.geosolutions.jaiext.range.Range;
/**
 * Simple interface for handling No Data for the interpolators
 * 
 * @author geosolutions
 *
 */
public interface InterpolationNoData {
    
    /**
     * Return NoData Range associated to the Interpolation object, if present.
     * 
     * @return NoData Range
     */
    public Range getNoDataRange();

    /**
     * Set NoData Range associated to the Interpolation object.
     * 
     */
    public void setNoDataRange(Range noDataRange);
    
    /**
     * Return the destinationNoData value associated to the Interpolation Object
     * 
     * @return destinationNoData
     */
    public double getDestinationNoData();
    
}
