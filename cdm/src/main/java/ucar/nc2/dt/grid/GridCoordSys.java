// $Id: GridCoordSys.java,v 1.2 2006/02/20 22:46:06 caron Exp $
/*
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt.grid;

import ucar.nc2.dataset.*;
import ucar.nc2.VariableIF;

import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.vertical.VerticalTransform;

import java.util.Date;
import java.util.ArrayList;

/**
 * A Grid Coordinate System
 * <ul>
 * <li> Seperable X-Y, Z, T
 * <li> X and Y are 1 or 2 dimensional
 * <li> Z, T are 1-dimensional
 * <li> An optional VerticalTransform can provide a height or pressure coordinate that may be 1-4 dimensional.
 * </ul>
 *
 * We could insist on one dimensional X, Y, Z, T, and treat optional HorizontalTransform the same as VerticalTransform.
 * Its job would be to provide lat(x,y) and lon(x,y) arrays.
 * @author john
 */
public interface GridCoordSys {

  public String getName();

  public ArrayList getCoordinateAxes();
  public ArrayList getCoordinateTransforms();

  public CoordinateAxis getHeightAxis();
  public CoordinateAxis getLatAxis();
  public CoordinateAxis getLonAxis();
  public CoordinateAxis getPressureAxis();
  public CoordinateAxis getTaxis();
  public CoordinateAxis getXaxis();
  public CoordinateAxis getYaxis();
  public CoordinateAxis getZaxis();

  public ProjectionImpl getProjection();

  public ArrayList getDomain(); // Dimensions
  public int getRankDomain();
  public int getRankRange();

  public boolean hasTimeAxis();
  public boolean hasVerticalAxis();

  public boolean isComplete(VariableIF v);
  public boolean isGeoReferencing();
  public boolean isGeoXY();
  public boolean isImplicit();
  public boolean isLatLon();
  public boolean isProductSet();
  public boolean isRegular();

  // grid
  public ProjectionRect getBoundingBox();
  public LatLonRect getLatLonBoundingBox();

  //public ProjectionImpl getProjection();
  public VerticalTransform getVerticalTransform();

  public CoordinateAxis1D getVerticalAxis();
  public CoordinateAxis1D getTimeAxis();
  public CoordinateAxis getXHorizAxis(); // require 1D ??
  public CoordinateAxis getYHorizAxis(); // require 1D ??

  public ArrayList getLevels(); // NamedObject
  public ArrayList getTimes(); // NamedObject
  public String getLevelName(int index);
  public String getTimeName(int index);
  public boolean isDate();
  public Date[] getTimeDates();

  public int findTimeCoordElement(Date p0);
  public int[] findXYCoordElement(double xpos, double ypos, int[] result);

  //public boolean isGridCoordSys(StringBuffer p0, CoordinateSystem p1);
  //public boolean isLatLon();

  public boolean isZPositive();

  //public GridCoordSys makeGridCoordSys(StringBuffer p0, CoordinateSystem p1, VariableEnhanced p2);
  //void makeVerticalTransform(GridDataset p0);

}

/* Change History:
   $Log: GridCoordSys.java,v $
   Revision 1.2  2006/02/20 22:46:06  caron
   start of netcdf server
   dods string bugs (1 on client and 1 on server)
   add last-modified

   Revision 1.1  2005/03/03 20:52:26  caron
   datatype checkin

*/