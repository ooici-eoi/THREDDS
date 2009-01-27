/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;

/**
 * CoordinateSystem Evaluation utilities.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class CoordSysEvaluator {

  static public void findCoords(TableConfig nt, NetcdfDataset ds) {

    CoordinateSystem use = findBestCoordinateSystem(ds);
    if (use == null) return;

    for (CoordinateAxis axis : use.getCoordinateAxes()) {
      if (axis.getAxisType() == AxisType.Lat)
        nt.lat = axis.getShortName();
      else if (axis.getAxisType() == AxisType.Lon)
        nt.lon = axis.getShortName();
      else if (axis.getAxisType() == AxisType.Time)
        nt.time = axis.getShortName();
      else if (axis.getAxisType() == AxisType.Height)
        nt.elev = axis.getShortName();
    }
  }

  static public String findCoordNameByType(NetcdfDataset ds, AxisType atype) {
    CoordinateAxis coordAxis = findCoordByType(ds, atype);
    return coordAxis == null ? null : coordAxis.getName();
  }


  static public CoordinateAxis findCoordByType(NetcdfDataset ds, AxisType atype) {
    CoordinateSystem use = findBestCoordinateSystem(ds);
    if (use == null) return null;

    for (CoordinateAxis axis : use.getCoordinateAxes()) {
      if (axis.getAxisType() == atype)
        return axis;
    }

    // try all the axes
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if (axis.getAxisType() == atype)
        return axis;
    }

    return null;
  }

  static public Dimension findDimensionByType(NetcdfDataset ds, AxisType atype) {
    CoordinateAxis axis = findCoordByType(ds, atype);
    if (axis == null) return null;
    if (axis.isScalar()) return null;
    return axis.getDimension(0);
  }

  static private CoordinateSystem findBestCoordinateSystem(NetcdfDataset ds) {
        // find coordinate system with highest rank (largest number of axes)
    CoordinateSystem use = null;
    for (CoordinateSystem cs : ds.getCoordinateSystems()) {
      if (use == null) use = cs;
      else if (cs.getCoordinateAxes().size() > use.getCoordinateAxes().size())
        use = cs;
    }
    return use;
  }

}
