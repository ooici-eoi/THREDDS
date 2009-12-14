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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * CF-1 Convention.
 * see http://www.cgd.ucar.edu/cms/eaton/cf-metadata/index.html
 * <p/>
 * <i>
 * "The CF conventions for climate and forecast metadata are designed to promote the
 * processing and sharing of files created with the netCDF API. The conventions define
 * metadata that provide a definitive description of what the data in each variable
 * represents, and of the spatial and temporal properties of the data.
 * This enables users of data from different sources to decide which quantities are
 * comparable, and facilitates building applications with powerful extraction, regridding,
 * and display capabilities."
 * </i>
 *
 * @author caron
 */

public class CF1Convention extends CSMConvention {

  /**
   * Guess the value of ZisPositive based on z axis name and units
   *
   * @param zaxisName      z coordinate axis name
   * @param vertCoordUnits z coordinate axis name
   * @return CF.POSITIVE_UP or CF.POSITIVE_DOWN
   */
  public static String getZisPositive(String zaxisName, String vertCoordUnits) {
    if (vertCoordUnits == null) return CF.POSITIVE_UP;

    if (SimpleUnit.isCompatible("millibar", vertCoordUnits))
      return CF.POSITIVE_DOWN;

    if (SimpleUnit.isCompatible("m", vertCoordUnits))
      return CF.POSITIVE_UP;

    // dunno - make it up
    return CF.POSITIVE_UP;
  }

  private static String[] vertical_coords = {
          "atmosphere_sigma_coordinate",
          "atmosphere_hybrid_sigma_pressure_coordinate",
          "atmosphere_hybrid_height_coordinate",
          "atmosphere_sleve_coordinate",
          "ocean_sigma_coordinate",
          "ocean_s_coordinate",
          "ocean_sigma_z_coordinate",
          "ocean_double_sigma_coordinate",
          "ocean_s_coordinate_g1",          // -sachin 03/25/09
          "ocean_s_coordinate_g2"};

  public CF1Convention() {
    this.conventionName = "CF-1.X";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {

    // look for transforms
    List<Variable> vars = ds.getVariables();
    for (Variable v : vars) {
      // look for special standard_names
      String sname = ds.findAttValueIgnoreCase(v, "standard_name", null);
      if (sname != null) {
        sname = sname.trim();

        if (sname.equalsIgnoreCase("atmosphere_ln_pressure_coordinate")) { // LOOK why isnt this with other Transforms?
          makeAtmLnCoordinate(ds, v);
          continue;
        }

        if (sname.equalsIgnoreCase("forecast_reference_time")) { // experimental
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
          continue;
        }

        if (sname.equalsIgnoreCase("ensemble")) {  // experimental
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));
          continue;
        }

        for (String vertical_coord : vertical_coords)
          if (sname.equalsIgnoreCase(vertical_coord)) {
            v.addAttribute(new Attribute(_Coordinate.TransformType, TransformType.Vertical.toString()));
            if (v.findAttribute(_Coordinate.Axes) == null)
              v.addAttribute(new Attribute(_Coordinate.Axes, v.getName())); // LOOK: may also be time dependent
          }
      }

      // look for horiz transforms
      String grid_mapping_name = ds.findAttValueIgnoreCase(v, "grid_mapping_name", null);
      if (grid_mapping_name != null) {
        //grid_mapping_name = grid_mapping_name.trim();
        v.addAttribute(new Attribute(_Coordinate.TransformType, TransformType.Projection.toString()));
        v.addAttribute(new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
      }

    }

    ds.finish();
  }

  private void makeAtmLnCoordinate(NetcdfDataset ds, Variable v) {
    // get the formula attribute
    String formula = ds.findAttValueIgnoreCase(v, "formula_terms", null);
    if (null == formula) {
      String msg = " Need attribute 'formula_terms' on Variable " + v.getName() + "\n";
      parseInfo.format(msg);
      userAdvice.format(msg);
      return;
    }

    // parse the formula string
    Variable p0Var = null, levelVar = null;
    StringTokenizer stoke = new StringTokenizer(formula, " :");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken();
      if (toke.equalsIgnoreCase("p0")) {
        String name = stoke.nextToken();
        p0Var = ds.findVariable(name);
      } else if (toke.equalsIgnoreCase("lev")) {
        String name = stoke.nextToken();
        levelVar = ds.findVariable(name);
      }
    }

    if (null == p0Var) {
      String msg = " Need p0:varName on Variable " + v.getName() + " formula_terms\n";
      parseInfo.format(msg);
      userAdvice.format(msg);
      return;
    }

    if (null == levelVar) {
      String msg = " Need lev:varName on Variable " + v.getName() + " formula_terms\n";
      parseInfo.format(msg);
      userAdvice.format(msg);
      return;
    }

    String units = ds.findAttValueIgnoreCase(p0Var, "units", "hPa");

    // create the data and the variable
    try { // p(k) = p0 * exp(-lev(k))
      double p0 = p0Var.readScalarDouble();
      Array levelData = levelVar.read();
      Array pressureData = Array.factory(double.class, levelData.getShape());
      IndexIterator ii = levelData.getIndexIterator();
      IndexIterator iip = pressureData.getIndexIterator();
      while (ii.hasNext()) {
        double val = p0 * Math.exp(-1.0 * ii.getDoubleNext());
        iip.setDoubleNext(val);
      }

      CoordinateAxis1D p = new CoordinateAxis1D(ds, null, v.getShortName() + "_pressure", DataType.DOUBLE,
              levelVar.getDimensionsString(), units,
              "Vertical Pressure coordinate synthesized from atmosphere_ln_pressure_coordinate formula");
      p.setCachedData(pressureData, false);
      p.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      p.addAttribute(new Attribute(_Coordinate.AliasForDimension, p.getDimensionsString()));
      ds.addVariable(null, p);
      //Dimension d = p.getDimension(0);
      //d.addCoordinateVariable(p);
      parseInfo.format(" added Vertical Pressure coordinate %s\n", p.getName());

    } catch (IOException e) {
      String msg = " Unable to read variables from " + v.getName() + " formula_terms\n";
      parseInfo.format(msg);
      userAdvice.format(msg);
    }

  }

  // we assume that coordinate axes get identified by
  //  1) being coordinate variables or
  //  2) being listed in coordinates attribute.

  /**
   * Augment CSM axis type identification with "projection_x_coordinate", "projection_y_coordinate"
   * and  the various dimensionless vertical coordinates
   */
  protected AxisType getAxisType(NetcdfDataset ncDataset, VariableEnhanced v) {

    String sname = ncDataset.findAttValueIgnoreCase((Variable) v, "standard_name", null);
    if (sname != null) {
      sname = sname.trim();

      if (sname.equalsIgnoreCase("latitude"))
        return AxisType.Lat;

      if (sname.equalsIgnoreCase("longitude"))
        return AxisType.Lon;

      if (sname.equalsIgnoreCase("projection_x_coordinate") || sname.equalsIgnoreCase("grid_longitude"))
        return AxisType.GeoX;

      if (sname.equalsIgnoreCase("projection_y_coordinate") || sname.equalsIgnoreCase("grid_latitude"))
        return AxisType.GeoY;

      for (String vertical_coord : vertical_coords)
        if (sname.equalsIgnoreCase(vertical_coord))
          return AxisType.GeoZ;
    }

    AxisType at = super.getAxisType(ncDataset, v);

    // last choice is using axis attribute - only for X, Y, Z
    if (at == null) {
      String axis = ncDataset.findAttValueIgnoreCase((Variable) v, "axis", null);
      if (axis != null) {
        axis = axis.trim();
        String unit = v.getUnitsString();

        if (axis.equalsIgnoreCase("X")) {
          if (SimpleUnit.isCompatible("m", unit))
            return AxisType.GeoX;

        } else if (axis.equalsIgnoreCase("Y")) {
          if (SimpleUnit.isCompatible("m", unit))
            return AxisType.GeoY;

        } else if (axis.equalsIgnoreCase("Z")) {
          if (unit == null) return AxisType.GeoZ;
          if (SimpleUnit.isCompatible("m", unit))
            return AxisType.Height;
          else if (SimpleUnit.isCompatible("mbar", unit))
            return AxisType.Pressure;
          else
            return AxisType.GeoZ;
        }
      }
    }

    return at;
  }

  /**
   * Assign CoordinateTransform objects to Coordinate Systems.
   * <p/>
   * protected void assignCoordinateTransforms(NetcdfDataset ncDataset) {
   * super.assignCoordinateTransforms(ncDataset);
   * <p/>
   * // need to explicitly assign vertical transforms
   * for (int i = 0; i < varList.size(); i++) {
   * VarProcess vp = (VarProcess) varList.get(i);
   * if (vp.isCoordinateTransform && (vp.ct != null) && (vp.ct.getTransformType() == TransformType.Vertical)) {
   * List domain = getFormulaDomain(ncDataset, vp.v);
   * if (null == domain) continue;
   * <p/>
   * List csList = ncDataset.getCoordinateSystems();
   * for (int j = 0; j < csList.size(); j++) {
   * CoordinateSystem cs = (CoordinateSystem) csList.get(j);
   * if (!cs.containsAxis(vp.v.getShortName())) continue; // cs must contain the vertical axis
   * if (cs.containsDomain(domain)) { // cs must contain the formula domain
   * cs.addCoordinateTransform(vp.ct);
   * parseInfo.append(" assign (CF) coordTransform " + vp.ct + " to CoordSys= " + cs + "\n");
   * }
   * }
   * }
   * }
   * }
   */

  /* run through all the variables in the formula, and get their domain (list of dimensions)
  private List getFormulaDomain(NetcdfDataset ds, Variable v) {
    String formula = ds.findAttValueIgnoreCase(v, "formula_terms", null);
    if (null == formula) {
      parseInfo.format("*** Cant find formula_terms attribute ");
      return null;
    }

    ArrayList domain = new ArrayList();
    StringTokenizer stoke = new StringTokenizer(formula);
    while (stoke.hasMoreTokens()) {
      String what = stoke.nextToken();
      String varName = stoke.nextToken();
      Variable formulaV = ds.findVariable(varName);
      if (null == formulaV) {
        parseInfo.format("*** Cant find formula variable= %s for term= %s\n",varName, what);
        continue;
      }
      domain.addAll(formulaV.getDimensions());
    }

    return domain;
  } */

}

