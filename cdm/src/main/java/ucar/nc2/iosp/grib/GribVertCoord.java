// $Id: GribVertCoord.java 63 2006-07-12 21:50:51Z edavis $
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
package ucar.nc2.iosp.grib;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.grib.Index;
import ucar.grib.TableLookup;
import ucar.grib.grib1.Grib1Lookup;

import java.util.*;

/**
 * A Vertical Coordinate variable for a Grib variable.
 *
 * @author caron
 * @version $Revision: 63 $ $Date: 2006-07-12 15:50:51 -0600 (Wed, 12 Jul 2006) $
 */
public class GribVertCoord implements Comparable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GribServiceProvider.class);

  private Index.GribRecord typicalRecord;
  private String levelName;
  private TableLookup lookup;
  private int seq = 0;

  private double[] coordValues;
  boolean usesBounds = false;

  boolean dontUseVertical = false;
  String positive = "up";
  String units;

  GribVertCoord(String name) {
    this.levelName = name;
    dontUseVertical = true;
  }

  GribVertCoord(List records, String levelName, TableLookup lookup) {
    this.typicalRecord = (Index.GribRecord) records.get(0);
    this.levelName = levelName;
    this.lookup = lookup;

    dontUseVertical = !lookup.isVerticalCoordinate( typicalRecord);
    positive = lookup.isPositiveUp(typicalRecord) ? "up" :"down";
    units = lookup.getLevelUnit( typicalRecord);

    usesBounds = Index2NC.isLayer(this.typicalRecord, lookup);
    addLevels( records);

    if (GribServiceProvider.debugVert)
      System.out.println("GribVertCoord: "+getVariableName()+"("+typicalRecord.levelType1+") useVertical= "+
          (!dontUseVertical)+" positive="+positive+" units="+units);
  }

  GribVertCoord(Index.GribRecord record, String levelName, TableLookup lookup, double[] level1, double[] level2) {
    this.typicalRecord = record;
    this.levelName = levelName;
    this.lookup = lookup;

    dontUseVertical = !lookup.isVerticalCoordinate(record);
    positive = lookup.isPositiveUp(record) ? "up" : "down";
    units = lookup.getLevelUnit(record);
    usesBounds = Index2NC.isLayer(this.typicalRecord, lookup);

    levels = new ArrayList(level1.length);
    for (int i = 0; i < level1.length; i++) {
      levels.add(new LevelCoord(level1[i], (level2 == null) ? 0.0 : level2[i]));
    }

    Collections.sort( levels );
    if( positive.equals( "down") ) {
      Collections.reverse( levels );
    }
  }

  void setSequence( int seq) { this.seq = seq; }

  String getLevelName() { return levelName; }
  String getVariableName() {
    return (seq == 0) ? levelName : levelName+seq; // more than one with same levelName
  }
  int getNLevels() { return dontUseVertical ? 1 : levels.size(); }

  void addLevels( List records) {
    for (int i = 0; i < records.size(); i++) {
      Index.GribRecord record = (Index.GribRecord) records.get(i);
      /*if (record.levelValue2 != 0) {
        if (GribServiceProvider.debugVert)
          System.out.println(levelName+" has levelType= "+record.levelType1+" levelValues="+record.levelValue1+","+record.levelValue2);
        hasTwoCoords = true;
      } */

      if (coordIndex(record) < 0) {
        levels.add( new LevelCoord(record.levelValue1, record.levelValue2));
        if (dontUseVertical && levels.size() > 1) {
          if (GribServiceProvider.debugVert)
            logger.warn("GribCoordSys: unused level coordinate has > 1 levels = "+levelName+" "+record.levelType1+" "+levels.size());
        }
      }
    }
    Collections.sort( levels );
    if( positive.equals( "down") ) {
      Collections.reverse( levels );
    }
  }

  boolean matchLevels( List records) {

    // first create a new list
    ArrayList levelList = new ArrayList( records.size());
    for (int i = 0; i < records.size(); i++) {
      Index.GribRecord record = (Index.GribRecord) records.get(i);
      LevelCoord lc = new LevelCoord(record.levelValue1, record.levelValue2);
      if (!levelList.contains(lc))
        levelList.add(lc);
    }

    Collections.sort( levelList );
    if( positive.equals( "down") )
      Collections.reverse( levelList );

    // gotta equal existing list
    return levelList.equals( levels);
  }

  void addDimensionsToNetcdfFile( NetcdfFile ncfile, Group g) {
    if (dontUseVertical) return;
    int nlevs = levels.size();
    ncfile.addDimension(g, new Dimension(getVariableName(), nlevs, true));
  }

  void addToNetcdfFile( NetcdfFile ncfile, Group g) {
    if (dontUseVertical) return;

    if (g == null)
      g = ncfile.getRootGroup();

    // coordinate axis
    Variable v = new Variable( ncfile, g, null, getVariableName());
    v.setDataType( DataType.DOUBLE);

    String desc = lookup.getLevelDescription( typicalRecord);
    boolean isGrib1 = lookup instanceof Grib1Lookup;
    if (!isGrib1 && usesBounds) desc = "Layer between "+ desc;

    v.addAttribute( new Attribute("long_name", desc));
    v.addAttribute( new Attribute("units", lookup.getLevelUnit( typicalRecord)));

    // positive attribute needed for CF-1 Height and Pressure
    if (positive != null)
      v.addAttribute( new Attribute("positive", positive));

    if (units != null) {
      AxisType axisType;
      if (SimpleUnit.isCompatible("millibar", units))
        axisType = AxisType.Pressure;
      else if (SimpleUnit.isCompatible("m", units))
        axisType = AxisType.Height;
      else
        axisType = AxisType.GeoZ;

      v.addAttribute( new Attribute("GRIB_level_type", Integer.toString(typicalRecord.levelType1)));
      v.addAttribute( new Attribute(_Coordinate.AxisType, axisType.toString()));
    }

    if (coordValues == null) {
      coordValues = new double[levels.size()];
      for (int i = 0; i < levels.size(); i++) {
        LevelCoord lc = (LevelCoord) levels.get(i);
        coordValues[i] = lc.mid;
      }
    }
    Array dataArray = Array.factory( DataType.DOUBLE.getClassType(), new int [] {coordValues.length}, coordValues);

    v.setDimensions( getVariableName());
    v.setCachedData( dataArray, true);

    ncfile.addVariable( g, v);

    if (usesBounds) {
      String boundsDimName = "bounds_dim";
      if (g.findDimension(boundsDimName) == null)
        ncfile.addDimension(g, new Dimension(boundsDimName, 2, true));

      String bname = getVariableName() +"_bounds";
      v.addAttribute( new Attribute("bounds", bname));
      v.addAttribute( new Attribute(_Coordinate.ZisLayer, "true"));

      Variable b = new Variable( ncfile, g, null, bname);
      b.setDataType( DataType.DOUBLE);
      b.setDimensions( getVariableName()+" "+boundsDimName);
      b.addAttribute( new Attribute("long_name", "bounds for "+v.getName()));
      b.addAttribute( new Attribute("units", lookup.getLevelUnit( typicalRecord)));

      Array boundsArray = Array.factory( DataType.DOUBLE.getClassType(), new int [] {coordValues.length, 2});
      ucar.ma2.Index ima = boundsArray.getIndex();
      for (int i=0; i<coordValues.length; i++) {
        LevelCoord lc = (LevelCoord) levels.get(i);
        boundsArray.setDouble(ima.set(i,0), lc.value1);
        boundsArray.setDouble(ima.set(i,1), lc.value2);
      }
      b.setCachedData( boundsArray, true);

      ncfile.addVariable( g, b);
    }
  }

  int getIndex(Index.GribRecord record) {
    if (dontUseVertical) return 0;
    return coordIndex( record);
  }

  public int compareTo(Object o) {
    GribVertCoord gv = (GribVertCoord) o;
    return getLevelName().compareToIgnoreCase( gv.getLevelName());
  }

  private ArrayList levels = new ArrayList(); // LevelCoord
  private class LevelCoord implements Comparable {
    double mid;
    double value1, value2;
    LevelCoord( double value1, double value2) {
      this.value1 = value1;
      this.value2 = value2;
      if (usesBounds && (value1 > value2)) {
        this.value1 = value2;
        this.value2 = value1;
      }
      mid = usesBounds ? (value1 + value2)/2 : value1;
    }

    public int compareTo(Object o) {
      LevelCoord other = (LevelCoord) o;
      if (closeEnough(value1, other.value1) && closeEnough(value2, other.value2)) return 0;
      return (int) (mid - other.mid);
    }

    public boolean equals(Object oo) {
      if (this == oo) return true;
      if ( !(oo instanceof LevelCoord)) return false;
      LevelCoord other = (LevelCoord) oo;
      return (closeEnough(value1, other.value1) && closeEnough(value2, other.value2));
    }

    public int hashCode() {
      return (int) (value1 * 100000 + value2 * 100);
    }
  }

  private double TOL = 1.0e-8;
  private boolean closeEnough( double v1, double v2) {
    return Math.abs(v1 - v2) < TOL;
  }
  private int coordIndex(Index.GribRecord record) {
    double val = record.levelValue1;
    double val2 = record.levelValue2;
    if (usesBounds && (val > val2)) {
      val = record.levelValue2;
      val2 = record.levelValue1;
    }

    for (int i = 0; i < levels.size(); i++) {
      LevelCoord lc = (LevelCoord) levels.get(i);
      if (closeEnough(lc.value1, val) && closeEnough(lc.value2, val2))
        return i;
    }
    return -1;
  }

}