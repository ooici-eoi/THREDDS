// $Id: $
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

package ucar.nc2.dataset.conv;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public interface _Coordinate {
  static public final String AliasForDimension = "_CoordinateAliasForDimension";
  static public final String Axes = "_CoordinateAxes";
  static public final String AxisType = "_CoordinateAxisType";
  static public final String AxisTypes = "_CoordinateAxisTypes";
  static public final String Convention = "_Coordinates";
  static public final String ModelRunDate = "_CoordinateModelRunDate"; // experimental
  static public final String Systems = "_CoordinateSystems";
  static public final String Transforms = "_CoordinateTransforms";
  static public final String TransformType = "_CoordinateTransformType";
  static public final String ZisLayer = "_CoordinateZisLayer";
  static public final String ZisPositive = "_CoordinateZisPositive";
}
