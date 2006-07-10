// $Id: GisFeatureAdapter.java,v 1.2 2004/09/24 03:26:32 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.datamodel.gis;


/**
 * This adapts a Gisfeature into a subclass of AbstractGisFeature.
 * Part of te ADT middleware pattern.
 *
 * @author John Caron
 * @version $Id: GisFeatureAdapter.java,v 1.2 2004/09/24 03:26:32 caron Exp $
 */

public class GisFeatureAdapter extends AbstractGisFeature  {
  private GisFeature gisFeature; // adaptee

  public GisFeatureAdapter( GisFeature gisFeature) {
    this.gisFeature = gisFeature;
  }

    /**
     * Get the bounding box for this feature.
     *
     * @return rectangle bounding this feature
     */
    public java.awt.geom.Rectangle2D getBounds2D() { return gisFeature.getBounds2D(); }

    /**
     * Get total number of points in all parts of this feature.
     *
     * @return total number of points in all parts of this feature.
     */
    public int getNumPoints(){ return gisFeature.getNumPoints(); }

    /**
     * Get number of parts comprising this feature.
     *
     * @return number of parts comprising this feature.
     */
    public int getNumParts(){ return gisFeature.getNumParts(); }

    /**
     * Get the parts of this feature, in the form of an iterator.
     *
     * @return the iterator over the parts of this feature.  Each part
     * is a GisPart.
     */
    public java.util.Iterator getGisParts(){ return gisFeature.getGisParts(); }

} // GisFeatureAdapter

/* Change History:
   $Log: GisFeatureAdapter.java,v $
   Revision 1.2  2004/09/24 03:26:32  caron
   merge nj22

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:46  caron
   import sources

   Revision 1.2  2000/08/18 04:15:24  russ
   Licensed under GNU LGPL.

   Revision 1.1  2000/02/10 17:45:10  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/
