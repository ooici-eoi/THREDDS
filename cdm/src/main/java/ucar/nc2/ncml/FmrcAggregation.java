// $Id: Aggregation.java,v 1.12 2006/05/12 20:19:28 caron Exp $
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
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.*;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 *
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */
public class FmrcAggregation extends Aggregation {
  private boolean debug = false;

  public FmrcAggregation(NetcdfDataset ncd, String dimName, String typeName, String recheckS) {
    super(ncd, dimName, typeName, recheckS);
  }

   // all elements are processed, finish construction
  public void finish(CancelTask cancelTask) throws IOException {
    nestedDatasets = new ArrayList();

    nestedDatasets.addAll(explicitDatasets);

    if (scanList.size() > 0)
      scan(nestedDatasets, cancelTask);

    // check persistence info
    // if ((diskCache2 != null) && (type == Type.JOIN_EXISTING)) persistRead();

    // for the moment, each file is considered 1 complete run, and is one slice of the runTime dimension
    buildCoords( cancelTask);
    buildDataset( false, ncd, cancelTask);

    this.lastChecked = System.currentTimeMillis();
  }

  private void buildDataset( boolean isNew, NetcdfDataset newds, CancelTask cancelTask) throws IOException {

    Group root = newds.getRootGroup();

    // grid
    NetcdfFile typical = getTypicalDataset();
    GridDataset gds = new ucar.nc2.dataset.grid.GridDataset( (NetcdfDataset) typical);

    // global attributes
    NcMLReader.transferGroupAttributes(typical.getRootGroup(), root);
    root.addAttribute(new Attribute("Conventions", "_Coordinates"));
    root.addAttribute(new Attribute("cdm_datatype", "ForecastModelRunCollection"));

    // needed dimensions, coordinate variables
    Iterator gcs = gds.getGridSets().iterator();
    while (gcs.hasNext()) {
      GridDataset.Gridset gset = (GridDataset.Gridset) gcs.next();
      GridCoordSystem gcc = gset.getGeoCoordSystem();

      // dimensions
      Iterator domain = gcc.getDomain().iterator();
      while (domain.hasNext()) {
        Dimension d = (Dimension) domain.next();
        if (null == root.findDimensionLocal( d.getName())) {
          Dimension newd = new Dimension(d.getName(), d.getLength(), d.isShared(), false, d.isVariableLength());
          root.addDimension( newd);
          if (debug) System.out.println("FmrcAggregation: added dimension "+newd.getName());
        }
      }

      // coordinate variables
      List axes = gcc.getCoordinateAxes();
      for (int i = 0; i < axes.size(); i++) {
        CoordinateAxis axis = (CoordinateAxis) axes.get(i);
        addVariable( root, axis, "axis");
      }
    }

    // look for coordinate transforms
    // LOOK: perhaps should make these accessible through CoordinateTransform object ??
    List vars = typical.getVariables();
    for (int i=0; i<vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (null != v.findAttribute("_CoordinateTransformType") || null != v.findAttribute("_CoordinateAxisTypes"))
        addVariable( root, v, "coordTransform");

      String coordTransVarNames = typical.findAttValueIgnoreCase(v, "_CoordinateTransforms", null);
      if (null != coordTransVarNames) {
        StringTokenizer stoker = new StringTokenizer( coordTransVarNames);
        int n = stoker.countTokens();
        while (stoker.hasMoreTokens()) {
          String toke = stoker.nextToken();
          Variable vt = typical.findVariable(toke);
          addVariable( root, vt, "coordTransform");
        }
      }
    }

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension( dimName, getTotalCoords(), true);
    newds.removeDimension( null, dimName); // remove previous declaration, if any
    newds.addDimension( null, aggDim);

    // create aggregation coordinate variable
    DataType coordType;
    Variable coordVar = newds.getRootGroup().findVariable(dimName);
    if (coordVar == null) {
      coordType = getCoordinateType();
      coordVar = new VariableDS(newds, null, null, dimName, coordType, dimName, null, null);
      coordVar.addAttribute(new Attribute("long_name", "Run time for ForecastModelRunCollection"));
      newds.addVariable(null, coordVar);
      if (debug) System.out.println("FmrcAggregation: added coordVar "+coordVar.getName());

    } else {
      coordType = coordVar.getDataType();
      coordVar.setDimensions(dimName); // reset its dimension
      if (!isNew) coordVar.setCachedData(null, false); // get rid of any cached data, since its now wrong
    }

    // if not already set, set its values
    if (!coordVar.hasCachedData()) {
      int[] shape = new int[] { getTotalCoords() };
      Array coordData = Array.factory(coordType.getPrimitiveClassType(), shape);
      Index ima = coordData.getIndex();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        if (coordType == DataType.STRING)
          coordData.setObject(ima.set(i), nested.getCoordValueString());
        else
          coordData.setDouble(ima.set(i), nested.getCoordValue());
      }
      coordVar.setCachedData( coordData, true);
    } else {
      Array data = coordVar.read();
      IndexIterator ii = data.getIndexIterator();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        nested.setCoordValue( ii.getDoubleNext());
      }
    }

    if (isDate()) {
      coordVar.addAttribute( new ucar.nc2.Attribute("_CoordinateAxisType", "Time"));
    }

    // promote all grid variables
    List grids = gds.getGrids();
    for (int i=0; i<grids.size(); i++) {
      GridDatatype grid = (GridDatatype) grids.get(i);
      Variable v = (Variable) grid.getVariable();

      // add new dimension
      String dims = dimName +" "+ v.getDimensionsString();

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(), dims, null, null);
      vagg.setAggregation( this);
      NcMLReader.transferVariableAttributes( v, vagg);

      newds.removeVariable( null, v.getShortName());
      newds.addVariable( null, vagg);
      if (debug) System.out.println("FmrcAggregation: added grid "+v.getName());

    }
  }

  private void addVariable(Group root, Variable v, String what) {
    if (null == v) return;
    if (null == root.findVariable(v.getShortName())) {
      root.addVariable( v); // reparent
      v.setDimensions( v.getDimensionsString()); // rediscover dimensions
      if (debug) System.out.println("FmrcAggregation: added "+ what+" "+v.getName());
    }
  }

/*
  private void buildDataset( boolean isNew, NetcdfDataset newds, CancelTask cancelTask) throws IOException {
    // open a "typical"  nested dataset and copy it to newds
    NetcdfFile typical = getTypicalDataset();
    NcMLReader.transferDataset(typical, newds, isNew ? null : new MyReplaceVariableCheck());

    // create aggregation dimension
    String dimName = getDimensionName();
    Dimension aggDim = new Dimension( dimName, getTotalCoords(), true);
    newds.removeDimension( null, dimName); // remove previous declaration, if any
    newds.addDimension( null, aggDim);

    // create aggregation coordinate variable
    DataType coordType;
    Variable coordVar = newds.getRootGroup().findVariable(dimName);
    if (coordVar == null) {
      coordType = getCoordinateType();
      coordVar = new VariableDS(newds, null, null, dimName, coordType, dimName, null, null);
      newds.addVariable(null, coordVar);
    } else {
      coordType = coordVar.getDataType();
      coordVar.setDimensions(dimName); // reset its dimension
      if (!isNew) coordVar.setCachedData(null, false); // get rid of any cached data, since its now wrong
    }

    // if not already set, set its values
    if (!coordVar.hasCachedData()) {
      int[] shape = new int[] { getTotalCoords() };
      Array coordData = Array.factory(coordType.getPrimitiveClassType(), shape);
      Index ima = coordData.getIndex();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        if (coordType == DataType.STRING)
          coordData.setObject(ima.set(i), nested.getCoordValueString());
        else
          coordData.setDouble(ima.set(i), nested.getCoordValue());
      }
      coordVar.setCachedData( coordData, true);
    } else {
      Array data = coordVar.read();
      IndexIterator ii = data.getIndexIterator();
      List nestedDataset = getNestedDatasets();
      for (int i = 0; i < nestedDataset.size(); i++) {
        Aggregation.Dataset nested = (Aggregation.Dataset) nestedDataset.get(i);
        nested.setCoordValue( ii.getDoubleNext());
      }
    }

    if (isDate()) {
      coordVar.addAttribute( new ucar.nc2.Attribute("_CoordinateAxisType", "Time"));
    }

    // promote all grid variables
    GridDataset gds = new GridDataset( (NetcdfDataset) typical);
    List grids = gds.getGrids();
    for (int i=0; i<grids.size(); i++) {
      GeoGrid grid = (GeoGrid) grids.get(i);
      Variable v = newds.getRootGroup().findVariable( grid.getName());
      if (v == null) {
        logger.error(ncd.getLocation()+" aggNewDimension cant find variable "+grid.getName());
        continue;
      }

      // if theres an unlimited dimension, remove it
      Dimension udim = v.getDimension(0);
      if (udim.isUnlimited())
      udim.setUnlimited( false);

      // add new dimension
      String dims = dimName +" "+ v.getDimensionsString();

      // construct new variable, replace old one
      VariableDS vagg = new VariableDS(newds, null, null, v.getShortName(), v.getDataType(), dims, null, null);
      vagg.setAggregation( this);
      NcMLReader.transferVariableAttributes( v, vagg);

      newds.removeVariable( null, v.getShortName());
      newds.addVariable( null, vagg);
    }
  }


*/

  // not ready yet
  public boolean syncExtend() throws IOException {
    /* if (scanList.size() == 0) return false;

    // rescan
    ArrayList newDatasets = new ArrayList();
    scan(newDatasets, null);

    // are there any new datasets?
    Dataset lastOld = (Dataset) nestedDatasets.get(nestedDatasets.size() - 1);
    int nextNew;
    for (nextNew = 0; nextNew < newDatasets.size(); nextNew++) {
      Dataset dataset = (Dataset) newDatasets.get(nextNew);
      if (dataset.location.equals(lastOld.location)) break;
    }
    nextNew++;
    if (nextNew >= newDatasets.size())
      return false;

    for (int i = nextNew; i < newDatasets.size(); i++) {
      Dataset newDataset = (Dataset) newDatasets.get(i);
      nestedDatasets.add(newDataset);
      totalCoords += newDataset.setStartEnd(totalCoords, null);
    }

    /* if (getType() == Aggregation.Type.JOIN_NEW)
      resetNewDimension();
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      resetAggDimensionLength();*/ // LOOK

    return true;
  }

  // sync if the recheckEvery time has passed
  public boolean sync() throws IOException {
    /* if (getType() == Aggregation.Type.UNION)
      return false;

    // see if we need to recheck
    if (recheck == null)
      return false;
    Date now = new Date();
    Date lastCheckedDate = new Date(lastChecked);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need))
      return false;

    // ok were gonna recheck
    lastChecked = System.currentTimeMillis();

    // rescan
    ArrayList newDatasets = new ArrayList();
    scan(newDatasets, null);

    // replace with previous datasets if they exist
    boolean wasChanged = false;
    for (int i = 0; i < newDatasets.size(); i++) {
      Dataset newDataset = (Dataset) newDatasets.get(i);
      int index = nestedDatasets.indexOf(newDataset);
      if (index >= 0) {
        newDatasets.set(i, nestedDatasets.get(index));
        logger.debug("Agg.sync oldDataset= {}", newDataset.location);
      } else {
        wasChanged = true;
        logger.debug("Agg.sync newDataset= {}", newDataset.location);
      }
    }

    // see if anything is changed
    if (!wasChanged) return false;

    // recreate the list of datasets
    nestedDatasets = new ArrayList();
    nestedDatasets.addAll(explicitDatasets);
    nestedDatasets.addAll(newDatasets);
    buildCoords( null);

    // chose a new typical dataset
    if (typical != null) {
      typical.close();
      typical = null;
      typicalDataset = null;
    }
    //ncd.empty();

    // rebuild the metadata
    if (getType() == Aggregation.Type.JOIN_NEW)
      aggNewDimension( false, ncd, null);
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      aggExistingDimension( false, ncd, null);
    else if (getType() == Aggregation.Type.FORECAST_MODEL)
      aggExistingDimension( false, ncd, null);

    ncd.finish(); */

    return true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

 /* public Array read(VariableDS mainv, CancelTask cancelTask) throws IOException {

    Array allData = Array.factory(mainv.getOriginalDataType().getPrimitiveClassType(), mainv.getShape());
    int destPos = 0;

    //System.out.println("Variable read "+mainv.getName()+" has length "+allData.getSize());
    Iterator iter = nestedDatasets.iterator();
    while (iter.hasNext()) {
      Dataset vnested = (Dataset) iter.next();
      Array varData = vnested.read(mainv, cancelTask);
      if ((cancelTask != null) && cancelTask.isCancel())
        return null;

      //System.out.println("  copy "+varData.getSize()+" starting at "+destPos);
      Array.arraycopy(varData, 0, allData, destPos, (int) varData.getSize());
      destPos += varData.getSize();
    }

    return allData;
  }

  public Array read(VariableDS mainv, CancelTask cancelTask, List section) throws IOException, InvalidRangeException {

    Array sectionData = Array.factory(mainv.getOriginalDataType(), Range.getShape(section));
    int destPos = 0;

    Range joinRange = (Range) section.get(0);
    List nestedSection = new ArrayList(section); // copy
    List innerSection = section.subList(1, section.size());

    /* if (type == Type.JOIN_NEW) {
      // iterate over the outer range
      for (int i=joinRange.first(); i<=joinRange.last(); i+= joinRange.stride()) {
        Dataset nested = (Dataset) nestedDatasets.get(i);
        Array varData = nested.read(mainv, cancelTask, innerSection);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;

        // copy the result
        Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
        destPos += varData.getSize();
      }
    } else {   */

      /* if (debug) System.out.println("agg wants range="+joinRange);

      Iterator iter = nestedDatasets.iterator();
      while (iter.hasNext()) {
        Aggregation.Dataset nested = (Aggregation.Dataset) iter.next();
        Range nestedJoinRange = nested.getNestedJoinRange(joinRange);
        if (nestedJoinRange == null)
          continue;
        if (debug) System.out.println("agg use "+nested.aggStart+":"+nested.aggEnd+" range= "+nestedJoinRange+" file "+nested.getLocation());

        Array varData;
          nestedSection.set(0, nestedJoinRange);
          varData = nested.read(mainv, cancelTask, nestedSection);

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Array.arraycopy(varData, 0, sectionData, destPos, (int) varData.getSize());
        destPos += varData.getSize();
      }
    // }

    return sectionData;
  } */

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* private class PolymorphicReader implements NetcdfFileFactory, NetcdfDatasetFactory {

      public NetcdfDataset openDataset(String cacheName, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException {
         NetcdfDataset ncfile = NetcdfDataset.openDataset(location, true, cancelTask);
         return ncfile;
      }

      public NetcdfFile open(String location, CancelTask cancelTask) throws IOException {
        NetcdfFile ncfile = NetcdfDataset.openFile(location, cancelTask);
        return ncfile;
      }
    }
  } */

}