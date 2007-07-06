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

package ucar.nc2.dataset;

import ucar.nc2.util.CancelTask;
import java.io.IOException;

/**
 * Implement this interface to add Coordinate Systems to a NetcdfDataset.
 * @author john caron
 */
public interface CoordSysBuilderIF {
  
  /**
   * Pass in the name of the Convention used to locate this CoordSysBuilderIF.
   * @param convName the name of the Convention
   */
  public void setConventionUsed( String convName);

  /**
   * Get the name of the Convention. In the case where the Convention attribute is not set in the file,
   *  this name cannot be used to identify the Convention. The isMine() method is called instead.
   * @return Convention name
   */
  public String getConventionUsed();

  /**
   * Make changes to the dataset, like adding new variables, attribuites, etc.
   *
   * @param ncDataset modify this dataset
   * @param cancelTask give user a chance to bail out
   * @throws java.io.IOException on error
   */
  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException;

  /**
   * Create the coordinate system objects: coordinate axes, coordinate systems, coordinate transforms.
   * @param ncDataset add to this dataset
   */
  public void buildCoordinateSystems( NetcdfDataset ncDataset);

  /** Give advice for a user trying to figure out why things arent working
   * @param advice add this advice to the User Advice String
   */
  public void addUserAdvice( String advice);
}



 /*
 public void buildCoordinateSystems(NetcdfDataset ncDataset) {

    // Bookeeping info for each variable is kept in the VarProcess inner class
    addVariables(ncDataset, ncDataset.getVariables(), varList);
       // Identify Coordinate Variables from AxisType, Alias
       varProcessList.add( new VarProcess(ncDataset, v));

    // identify which variables are coordinate axes from coordAxes, coordinates attributes
    findCoordinateAxes(ncDataset);
       findCoordinateAxes(vp, vp.coordAxes);
       findCoordinateAxes(vp, vp.coordinates);

    // identify which variables are used to describe coordinate system
    findCoordinateSystems(ncDataset);

    // identify which variables are used to describe coordinate transforms
    findCoordinateTransforms(ncDataset);

    // turn Variables into CoordinateAxis
    makeCoordinateAxes(ncDataset);

    // make Coordinate Systems for all Coordinate Systems Variables
    makeCoordinateSystems(ncDataset);
       if (vp.isCoordinateSystem) vp.makeCoordinateSystem();
          named by  _Coordinate.coordAxes

    // assign explicit CoordinateSystem objects to variables
    assignExplicitCoordinateSystems(ncDataset);
       getAxes() named by  _Coordinate.coordAxes

    // assign implicit CoordinateSystem objects to variables
    makeCoordinateSystemsImplicit(ncDataset);
       vp.findCoordinateAxes(true) coordAxes, coordinates, isCoordinateVariable, alias

    // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
    if (useMaximalCoordSys)
      makeCoordinateSystemsMaximal(ncDataset);

    // make Coordinate Transforms
    makeCoordinateTransforms(ncDataset);

    // assign Coordinate Transforms
    assignCoordinateTransforms(ncDataset);
  }

   */
