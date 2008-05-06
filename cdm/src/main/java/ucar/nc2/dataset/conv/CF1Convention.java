/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
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


  private static String[] vertical_coords = {
          "atmosphere_sigma_coordinate",
          "atmosphere_hybrid_sigma_pressure_coordinate",
          "atmosphere_hybrid_height_coordinate",
          "atmosphere_sleve_coordinate",
          "ocean_sigma_coordinate",
          "ocean_s_coordinate",
          "ocean_sigma_z_coordinate",
          "ocean_double_sigma_coordinate"};

  public CF1Convention() {
    this.conventionName = "CF-1.0";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) {

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
      parseInfo.append(msg);
      userAdvice.append(msg);
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
      parseInfo.append(msg);
      userAdvice.append(msg);
      return;
    }

    if (null == levelVar) {
      String msg = " Need lev:varName on Variable " + v.getName() + " formula_terms\n";
      parseInfo.append(msg);
      userAdvice.append(msg);
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
      parseInfo.append(" added Vertical Pressure coordinate ").append(p.getName()).append("\n");

    } catch (IOException e) {
      String msg = " Unable to read variables from " + v.getName() + " formula_terms\n";
      parseInfo.append(msg);
      userAdvice.append(msg);
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

  // run through all the variables in the formula, and get their domain (list of dimensions)
  private List getFormulaDomain(NetcdfDataset ds, Variable v) {
    String formula = ds.findAttValueIgnoreCase(v, "formula_terms", null);
    if (null == formula) {
      parseInfo.append("*** Cant find formula_terms attribute ");
      return null;
    }

    ArrayList domain = new ArrayList();
    StringTokenizer stoke = new StringTokenizer(formula);
    while (stoke.hasMoreTokens()) {
      String what = stoke.nextToken();
      String varName = stoke.nextToken();
      Variable formulaV = ds.findVariable(varName);
      if (null == formulaV) {
        parseInfo.append("*** Cant find formula variable=").append(varName).append(" for term=").append(what);
        continue;
      }
      domain.addAll(formulaV.getDimensions());
    }

    return domain;
  }

}

