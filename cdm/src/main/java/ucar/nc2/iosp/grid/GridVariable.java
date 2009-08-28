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


package ucar.nc2.iosp.grid;


import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.iosp.mcidas.McIDASLookup;
import ucar.nc2.iosp.gempak.GempakLookup;
import ucar.nc2.units.DateFormatter;
import ucar.grid.GridRecord;
import ucar.grid.GridTableLookup;
import ucar.grid.GridParameter;
import ucar.grid.GridDefRecord;
import ucar.unidata.util.StringUtil;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib2.Grib2Tables;

import java.util.ArrayList;
import java.util.List;


/**
 * A Variable for a Grid dataset.
 *
 * @author caron
 */
public class GridVariable {

  /**
   * logger
   */
  static private org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(GridVariable.class);

  /**
   * parameter name
   */
  private String name;

  /**
   * parameter description
   */
  private String desc;

  /**
   * variable name
   */
  private String vname;

  /**
   * first grid record
   */
  private GridRecord firstRecord;

  /**
   * lookup table
   */
  private GridTableLookup lookup;

  /**
   * horizontal coord system
   */
  private GridHorizCoordSys hcs;

  /**
   * vertical coord system
   */
  private GridCoordSys vcs;  // maximal strategy (old way)

  /**
   * time coord system
   */
  private GridTimeCoord tcs;

  /**
   * ensemble coord system
   */
  private GridEnsembleCoord ecs = null;

  /**
   * vertical coordinate
   */
  private GridVertCoord vc;

  /**
   * list of records that make up this variable
   */
  private List<GridRecord> records = new ArrayList<GridRecord>();  // GridRecord

  /**
   * number of levels
   */
  private int nlevels;

  /**
   * number of Ensembles
   */
  private int nEnsembles;

  /**
   * number of times
   */
  private int ntimes;

  /**
   * record tracker
   */
  private GridRecord[] recordTracker;

  /**
   * flag for having a vertical coordinate
   */
  private boolean hasVert = false;

  /**
   * debug flag
   */
  private boolean showRecords = false;

  /**
   * debug flag
   */
  private boolean showGen = false;

  /**
   * Create a new GridVariable
   *
   * @param name   name
   * @param desc   description
   * @param hcs    horizontal coordinate system
   * @param lookup lookup table
   */
  GridVariable(String name, String desc, GridHorizCoordSys hcs,
               GridTableLookup lookup) {
    this.name = name;  // used to get unique grouping of products
    this.desc = desc;
    this.hcs = hcs;
    this.lookup = lookup;
  }

  /**
   * Add in a new product
   *
   * @param record grid  to add
   */
  void addProduct(GridRecord record) {
    records.add(record);
    if (firstRecord == null) {
      firstRecord = record;
    }
  }

  /**
   * Get the list of grids
   *
   * @return grid records
   */
  List<GridRecord> getRecords() {
    return records;
  }

  /**
   * get the first grid record
   *
   * @return the first in the list
   */
  GridRecord getFirstRecord() {
    return records.get(0);
  }

  /**
   * Get the horizontal coordinate system
   *
   * @return the horizontal coordinate system
   */
  GridHorizCoordSys getHorizCoordSys() {
    return hcs;
  }

  /**
   * Get the vertical coordinate system
   *
   * @return the vertical coordinate system
   */
  GridCoordSys getVertCoordSys() {
    return vcs;
  }

  /**
   * Get the vertical coordinate
   *
   * @return the vertical coordinate
   */
  GridVertCoord getVertCoord() {
    return vc;
  }

  /**
   * Does this have a vertical dimension
   *
   * @return true if has a vertical dimension
   */
  boolean hasVert() {
    return hasVert;
  }

  /**
   * Set the variable name
   *
   * @param vname the variable name
   */
  void setVarName(String vname) {
    this.vname = vname;
  }

  /**
   * Set the vertical coordinate system
   *
   * @param vcs the vertical coordinate system
   */
  void setVertCoordSys(GridCoordSys vcs) {
    this.vcs = vcs;
  }

  /**
   * Set the vertical coordinate
   *
   * @param vc the vertical coordinate
   */
  void setVertCoord(GridVertCoord vc) {
    this.vc = vc;
  }

  /**
   * Set the time coordinate
   *
   * @param tcs the time coordinate
   */
  void setTimeCoord(GridTimeCoord tcs) {
    this.tcs = tcs;
  }

  /**
   * Set the Ensemble coordinate
   *
   * @param ecs the Ensemble coordinate
   */
  void setEnsembleCoord(GridEnsembleCoord ecs) {
    this.ecs = ecs;
  }

  /**
   * Get the number of Ensemble
   *
   * @return the number of Ensemble
   */
  int getNEnsembles() {
    return (ecs == null)
        ? 1
        : ecs.getNEnsembles();
  }

  /**
   * Get the Index of Ensemble
   * @param record GridRecord
   * @return the Index of Ensemble
   */
  int getEnsembleIndex( GridRecord record ) {
    return (ecs == null)
        ? 1
        : ecs.getIndex( record );
  }

  /**
   * Does this have a Ensemble dimension
   *
   * @return true if has a Ensemble dimension
   */
  boolean hasEnsemble() {
    return (ecs == null)
        ? false
        : ecs.getNEnsembles() > 1;
  }

  /**
   * Get the name of the ensemble dimension
   *
   * @return the name of the ensemble dimension
   */
  String getEnsembleName() {
    return ecs.getName();
  }

  /**
   * Get the number of vertical levels
   *
   * @return the number of vertical levels
   */
  int getVertNlevels() {
    return (vcs == null)
        ? vc.getNLevels()
        : vcs.getNLevels();
  }

  /**
   * Get the name of the vertical dimension
   *
   * @return the name of the vertical dimension
   */
  String getVertName() {
    return (vcs == null)
        ? vc.getVariableName()
        : vcs.getVerticalName();
  }

  /**
   * Get the name of the vertical level
   *
   * @return the name of the vertical level
   */
  String getVertLevelName() {
    return (vcs == null)
        ? vc.getLevelName()
        : vcs.getVerticalName();
  }

  /**
   * Is vertical used?
   *
   * @return true if vertical used
   */
  boolean getVertIsUsed() {
    return (vcs == null)
        ? !vc.dontUseVertical
        : !vcs.dontUseVertical;
  }

  /**
   * Get the index in the vertical for the particular grid
   *
   * @param p grid to check
   * @return the index
   */
  int getVertIndex(GridRecord p) {
    return (vcs == null)
        ? vc.getIndex(p)
        : vcs.getIndex(p);
  }

  /**
   * Get the number of times
   *
   * @return the number of times
   */
  int getNTimes() {
    return (tcs == null)
        ? 1
        : tcs.getNTimes();
  }

  /* String getSearchName() {
   Parameter param = lookup.getParameter( firstRecord);
   String vname = lookup.getLevelName( firstRecord);
   return param.getDescription() + " @ " + vname;
 } */

  /**
   * Make the variable
   *
   * @param ncfile  netCDF file
   * @param g       group
   * @param useDesc true use the description
   * @return the variable
   */
  Variable makeVariable(NetcdfFile ncfile, Group g, boolean useDesc) {
    assert records.size() > 0 : "no records for this variable";
    nlevels = getVertNlevels();
    nEnsembles = getNEnsembles();
    ntimes = tcs.getNTimes();

    if (vname == null) {
      vname = AbstractIOServiceProvider.createValidNetcdfObjectName(
          useDesc ? desc : name);
    }

    //vname = StringUtil.replace(vname, '-', "_"); // Done in dods server now
    // need for Gempak and McIDAS
    vname = StringUtil.replace(vname, ' ', "_");

    Variable v = new Variable(ncfile, g, null, vname);
    v.setDataType(DataType.FLOAT);

    String dims = tcs.getName();

    if ( hasEnsemble() ) {
      dims = dims + " " + getEnsembleName();  // TODO: time first
      //dims = getEnsembleName() + " " + dims;
    }

    if (getVertIsUsed()) {
      dims = dims + " " + getVertName();
      hasVert = true;
    }

    if (hcs.isLatLon()) {
      dims = dims + " lat lon";
    } else {
      dims = dims + " y x";
    }

    v.setDimensions(dims);
    GridParameter param = lookup.getParameter(firstRecord);

    String unit = param.getUnit();
    if (unit == null) {
      unit = "";
    }
    v.addAttribute(new Attribute("units", unit));
    v.addAttribute(new Attribute("long_name",
        makeLongName(firstRecord, lookup)));
    v.addAttribute(
        new Attribute(
            "missing_value", new Float(lookup.getFirstMissingValue())));
    if (!hcs.isLatLon()) {
      if (ucar.nc2.iosp.grib.GribGridServiceProvider.addLatLon) {
        v.addAttribute(new Attribute("coordinates", "lat lon"));
      }
      v.addAttribute(new Attribute("grid_mapping", hcs.getGridName()));
    }

    /*
    * GribVariable that adds in the specific attributes.
    *
    */
    int icf = hcs.getGds().getInt(GridDefRecord.VECTOR_COMPONET_FLAG);
    String flag;
    if ( icf == 0 ) {
       flag = Grib2Tables.VectorComponentFlag.easterlyNortherlyRelative.toString();
    } else {
       flag = Grib2Tables.VectorComponentFlag.gridRelative.toString();
    }
    if (lookup instanceof Grib2GridTableLookup) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      int[] paramId = g2lookup.getParameterId(firstRecord);
      v.addAttribute(new Attribute("GRIB_param_discipline", lookup.getDisciplineName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_category", lookup.getCategoryName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_name", param.getName()));
      v.addAttribute(new Attribute("GRIB_param_id", Array.factory(int.class, new int[]{paramId.length}, paramId)));
      v.addAttribute(new Attribute("GRIB_product_definition_type", g2lookup.getProductDefinitionName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_level_type", new Integer(firstRecord.getLevelType1())));
      if (g2lookup.isEnsemble( firstRecord ))
        v.addAttribute(new Attribute("GRIB_forecasts_in_ensemble", g2lookup.NumberOfForecastsInEnsemble(firstRecord)));
      if (g2lookup.isProbability( firstRecord ))
        v.addAttribute(new Attribute("GRIB_forecasts_in_probability", g2lookup.NumberOfForecastsInProbability(firstRecord)));
      //if( firstRecord.getLevelType2() != 255)
      //   v.addAttribute( new Attribute("GRIB2_level_type2", new Integer(firstRecord.getLevelType2())));
      v.addAttribute(new Attribute("GRIB_"+ GridDefRecord.VECTOR_COMPONET_FLAG, flag ));
    } else if (lookup instanceof Grib1GridTableLookup) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      int[] paramId = g1lookup.getParameterId(firstRecord);
      v.addAttribute(new Attribute("GRIB_param_name", param.getDescription()));
      v.addAttribute(new Attribute("GRIB_center_id", new Integer(paramId[1])));
      v.addAttribute(new Attribute("GRIB_table_id", new Integer(paramId[2])));
      v.addAttribute(new Attribute("GRIB_param_number", new Integer(paramId[3])));
      v.addAttribute(new Attribute("GRIB_param_id", Array.factory(int.class, new int[]{paramId.length}, paramId)));
      v.addAttribute(new Attribute("GRIB_product_definition_type", g1lookup.getProductDefinitionName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_level_type", new Integer(firstRecord.getLevelType1())));
      v.addAttribute(new Attribute("GRIB_"+ GridDefRecord.VECTOR_COMPONET_FLAG, flag ));
    } else {
      v.addAttribute(new Attribute( GridDefRecord.VECTOR_COMPONET_FLAG, flag ));
    }
    v.setSPobject(this);

    if (showRecords) {
      System.out.println("Variable " + getName());
    }

    recordTracker = new GridRecord[ntimes * nEnsembles * nlevels];
    for (GridRecord p : records) {
      if (showRecords) {
        System.out.println(" " + vc.getVariableName() + " (type="
            + p.getLevelType1() + ","
            + p.getLevelType2() + ")  value="
            + p.getLevelType1() + ","
            + p.getLevelType2()
            //+" # genProcess="+p.typeGenProcess);
        );
      }
      int level = getVertIndex(p);
      if (!getVertIsUsed() && (level > 0)) {
        log.warn("inconsistent level encoding=" + level);
        level = 0;  // inconsistent level encoding ??
      }
      int time = tcs.getIndex(p);
      // System.out.println("time="+time+" level="+level);
      if (level < 0) {
        log.warn("NOT FOUND record; level=" + level + " time= "
            + time + " for " + getName() + " file="
            + ncfile.getLocation() + "\n" + "   "
            + getVertLevelName() + " (type=" + p.getLevelType1()
            + "," + p.getLevelType2() + ")  value="
            + p.getLevel1() + "," + p.getLevel2() + "\n");

        getVertIndex(p);  // allow breakpoint
        continue;
      }

      if (time < 0) {
        log.warn("NOT FOUND record; level=" + level + " time= "
            + time + " for " + getName() + " file="
            + ncfile.getLocation() + "\n" + " forecastTime= "
            + p.getValidTimeOffset() + " date= "
            + tcs.getValidTime(p) + "\n");

        tcs.getIndex(p);  // allow breakpoint
        continue;
      }
      int recno;
      if ( hasEnsemble() ) {
        int ens = getEnsembleIndex(p);
        recno = time * ( nEnsembles * nlevels ) + ( ens * nlevels ) + level;
        //recno = ens * ( ntimes * nlevels ) + ( time * nlevels ) + level;
      } else {
        recno = time * nlevels + level;
      }

      if (recordTracker[recno] == null) {
        recordTracker[recno] = p;
      } else {
        GridRecord q = recordTracker[recno];
        recordTracker[recno] = p;  // replace it with latest one
        // System.out.println("   gen="+p.typeGenProcess+" "+q.typeGenProcess+"=="+lookup.getTypeGenProcessName(p));
      }
    }

    // let all references to Index go, to reduce retained size
    records.clear();

    return v;

  }

  /**
   * Dump out the missing data
   */
  void dumpMissing() {
    //System.out.println("  " +name+" ntimes (across)= "+ ntimes+" nlevs (down)= "+ nlevels+":");
    System.out.println("  " + name);
    for (int j = 0; j < nlevels; j++) {
      System.out.print("   ");
      for (int i = 0; i < ntimes; i++) {
        boolean missing = recordTracker[i * nlevels + j] == null;
        System.out.print(missing
            ? "-"
            : "X");
      }
      System.out.println();
    }
  }

  /**
   * Dump out the missing data as a summary
   *
   * @return number of missing levels
   */
  int dumpMissingSummary() {
    if (nlevels == 1) {
      return 0;
    }

    int count = 0;
    int total = nlevels * ntimes;

    for (int i = 0; i < total; i++) {
      if (recordTracker[i] == null) {
        count++;
      }
    }

    System.out.println("  MISSING= " + count + "/" + total + " " + name);
    return count;
  }

  /**
   * Find the grid record for the time and level indices
   *
   * @param time  time index
   * @param level level index
   * @return the record or null
   */
  public GridRecord findRecord(int time, int level) {
    return recordTracker[time * nlevels + level];
  }

  /**
   * Check for equality
   *
   * @param oo object in question
   * @return true if they are equal
   */
  public boolean equals(Object oo) {
    if (this == oo) {
      return true;
    }
    if (!(oo instanceof GridVariable)) {
      return false;
    }
    return hashCode() == oo.hashCode();
  }

  /**
   * Get the name
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the parameter name
   *
   * @return description
   */
  public String getParamName() {
    return desc;
  }

  /**
   * Override Object.hashCode() to implement equals.
   *
   * @return equals;
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + name.hashCode();
      result += 37 * result + firstRecord.getLevelType1();
      result += 37 * result + hcs.getID().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  /**
   * hash code
   */
  private volatile int hashCode = 0;


  /**
   * Dump this variable
   *
   * @return the variable
   */
  public String dump() {
    DateFormatter formatter = new DateFormatter();
    StringBuilder sbuff = new StringBuilder();
    sbuff.append(name + " " + records.size() + "\n");
    for (GridRecord record : records) {
      sbuff.append(" level = " + record.getLevelType1() + " "
          + record.getLevel1());
      if (null != record.getValidTime()) {
        sbuff.append(
            " time = "
                + formatter.toDateTimeString(record.getValidTime()));
      }
      sbuff.append("\n");
    }
    return sbuff.toString();
  }

  /**
   * Make a long name for the variable
   *
   * @param gr     grid record
   * @param lookup lookup table
   * @return long variable name
   */
  private String makeLongName(GridRecord gr, GridTableLookup lookup) {
    /* // TODO: check / delete
    GridParameter param = lookup.getParameter(gr);
    String levelName = GridIndexToNC.makeLevelName(gr, lookup);
    //String levelName = lookup.getLevelDescription(gr);
    //String levelName = lookup.getLevelName( gr );
    return (levelName.length() == 0)
        ? param.getDescription()
        : param.getDescription() + " @ " + levelName;
   */
    GridParameter param = lookup.getParameter(gr);

    String levelName;
    if ( (lookup instanceof GempakLookup) || (lookup instanceof McIDASLookup)) {
      levelName = lookup.getLevelDescription( gr );
    } else {
      levelName = GridIndexToNC.makeLevelName(gr, lookup);
    }
    String ensembleName = GridIndexToNC.makeSuffixName(gr, lookup);
    String paramName = param.getDescription();
    paramName = (ensembleName.length() == 0)
        ? paramName : paramName + "_" + ensembleName;

    paramName = (levelName.length() == 0)
        ? paramName : paramName + " @ " + levelName;

    return paramName;
  }


}

