// $Id$
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
package thredds.viewer.gis.shapefile;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import thredds.datamodel.gis.*;
import thredds.viewer.gis.shapefile.EsriShapefile;

import java.awt.Color;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a convenient interface to ESRI shapefiles by creating lists of
 * ucar.unidata.gis.AbstractGisFeature.  Java2D Shape or VisAD SampledSet
 * objects can be created from these.
 *
 * @author Russ Rew
 * @author John Caron
 * @version $Id$
 */
public class EsriShapefileRenderer extends thredds.viewer.gis.GisFeatureRendererMulti {
  private static java.util.Map sfileHash = new HashMap(); // map of (filename -> EsriShapefileRenderer)
  private static double defaultCoarseness = 0.0; // expose later?

  /**
   * Use factory to obtain a EsriShapefileRenderer.  This caches the EsriShapefile for reuse.
   * <p>
   * Implementation note: should switch to weak references.
   */
  static public EsriShapefileRenderer factory(String filename) {
    if (sfileHash.containsKey(filename))
      return (EsriShapefileRenderer) sfileHash.get(filename);

    try {
      EsriShapefileRenderer sfile = new EsriShapefileRenderer(filename);
      sfileHash.put(filename, sfile);
      return sfile;
    } catch (Exception ex) {
      System.err.println("EsriShapefileRenderer failed on " + filename+"\n"+ex);
      //ex.printStackTrace();
      return null;
    }
  }

  static public EsriShapefileRenderer factory(String key, InputStream stream) {
    if (sfileHash.containsKey(key))
      return (EsriShapefileRenderer) sfileHash.get(key);

    try {
      EsriShapefileRenderer sfile = new EsriShapefileRenderer(stream);
      sfileHash.put(key, sfile);
      return sfile;
    } catch (Exception ex) {
      System.err.println("EsriShapefileRenderer failed on " + stream+"\n"+ex);
      return null;
    }
  }

  ////////////////////////////////////////
  private EsriShapefile esri = null;
  private ProjectionImpl dataProject = new LatLonProjection ("Cylindrical Equidistant");

  private EsriShapefileRenderer(InputStream stream) throws IOException {
    super();
    esri = new EsriShapefile(stream, null, defaultCoarseness);

    double avgD = getStats(esri.getFeatures().iterator());
    createFeatureSet(avgD);
    createFeatureSet(2*avgD);
    createFeatureSet(3*avgD);
    createFeatureSet(5*avgD);
    createFeatureSet(8*avgD);
  }

  private EsriShapefileRenderer(String filename) throws IOException {
    this(filename, defaultCoarseness);
  }

  private EsriShapefileRenderer(String filename, double coarseness) throws IOException {
    super();
    esri = new EsriShapefile(filename, coarseness);

    double avgD = getStats(esri.getFeatures().iterator());
    createFeatureSet(2*avgD);
    createFeatureSet(4*avgD);
    createFeatureSet(8*avgD);
    createFeatureSet(16*avgD);
  }

  public LatLonRect getPreferredArea() {
    Rectangle2D bb = esri.getBoundingBox();
    return new LatLonRect(new LatLonPointImpl(bb.getMinY(), bb.getMinX()), bb.getHeight(), bb.getWidth());
  }

  protected java.util.List getFeatures() {
    return esri.getFeatures();
  }

  protected ProjectionImpl getDataProjection() { return dataProject; }

}


/* Change History:
   $Log: EsriShapefileRenderer.java,v $
   Revision 1.5  2005/04/02 19:50:56  caron
   cat 1.1 - first pass

   Revision 1.4  2004/09/24 03:26:38  caron
   merge nj22

   Revision 1.3  2003/04/08 18:16:22  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:36  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.2  2002/04/29 22:32:18  caron
   use key for hash, not stream

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources

   Revision 1.15  2000/08/18 04:15:27  russ
   Licensed under GNU LGPL.

   Revision 1.14  2000/05/16 22:38:04  caron
   factor GisFeatureRenderer

   Revision 1.13  2000/02/17 20:15:59  caron
   tune resolution on zoom in

   Revision 1.12  2000/02/11 01:24:45  caron
   add getDataProjection()

   Revision 1.11  2000/02/10 17:45:16  caron
   add GisFeatureRenderer,GisFeatureAdapter

   Revision 1.10  2000/01/21 23:07:46  russ
   Add coarseness ShapefileShapeList constructors.  Make use of
   coarseness constructor using default of 1.0 to speed up rendering by a
   factor of 3.

   Revision 1.9  2000/01/05 16:04:59  russ
   Use particular instead of general feature type in projectShape()
   method, now that GisFeature interface has been simplified.

   Revision 1.8  1999/12/28 17:37:55  russ
   Oops, fixed bad import.

   Revision 1.7  1999/12/28 17:13:19  russ
   Eliminate unnecesssary dependence on Java2D.  Removed coarseness
   parameter for smaller coarser resolution maps (may add back in
   later).  Allow use of .zip files in constructor with bounding box.
   Made EsriFeature extend AbstractGisFeature for getShape() method.
   Have getGisParts() return iterator for list of GisPart.  Cosmetic
   changes to EsriShapefileRenderer, ShapefileShapeList.

   Revision 1.6  1999/12/16 22:57:35  caron
   gridded data viewer checkin

   Revision 1.5  1999/07/28 19:30:56  russ
   Adapted EsriShapefile to read from a DataInputStream instead of a
   RandomAccessFile.  Added URL constructor, so can read from a URL.
   Instead of using file length, read until EOF.  Still need to make
   independent of Java2D ...

   Removed java.awt.Dimension parameter from ShapefileShapeList
   constructor used in determining line segments to omit (just assume
   1000 pixel display).

   Removed unused PathIterator from EsriShapefileRenderer.

   Revision 1.4  1999/06/11 21:27:59  russ
   Cosmetic changes, preperatory to eliminating use of RandomAccessFile I/O.

   Revision 1.3  1999/06/03 01:43:57  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:24  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:43  caron
   startAgain
*/
