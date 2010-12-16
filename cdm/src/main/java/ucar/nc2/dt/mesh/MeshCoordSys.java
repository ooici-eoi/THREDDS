/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt.mesh;

import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.nc2.units.*;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.VerticalPerspectiveView;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.geoloc.projection.RotatedLatLon;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.vertical.*;
import ucar.ma2.*;
import ucar.ma2.InvalidRangeException;

import java.util.*;
import java.io.IOException;

import ucar.nc2.units.DateRange;

/**
 *
 * @author Kyle
 */
public class MeshCoordSys extends CoordinateSystem implements ucar.nc2.dt.MeshCoordSystem {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MeshCoordSys.class);

  private ProjectionImpl proj;
  private CoordinateAxis Naxis, horizXaxis, horizYaxis;
  private CoordinateAxis1D vertZaxis, ensembleAxis;
  private CoordinateAxis1DTime timeTaxis, runTimeAxis;
  private CoordinateAxis1DTime[] timeAxisForRun;  
  private VerticalCT vCT;
  private VerticalTransform vt;
  private Dimension timeDim;

  private List<NamedObject> levels = null;
  private List<NamedObject> times = null;
  private Date[] timeDates = null;
  private boolean isDate = false;

  private boolean isLatLon = false;


  /**
   * Determine if this CoordinateSystem can be made into a GridCoordSys. Optionally for a given variable.
   * This currently assumes that the CoordinateSystem:
   * <ol>
   * <li> is georeferencing (cs.isGeoReferencing())
   * <li> x, y are 1 or 2-dimensional axes.
   * <li> z, t, if they exist, are 1-dimensional axes.
   * <li> domain rank > 1
   * </ol>
   *
   * @param sbuff place information messages here, may be null
   * @param cs    the CoordinateSystem to test
   * @param v     can it be used for this variable; v may be null
   * @return true if it can be made into a GridCoordSys.
   * @see CoordinateSystem#isGeoReferencing
   */
  public static boolean isMeshCoordSys(Formatter sbuff, CoordinateSystem cs, VariableEnhanced v) {
    // must be at least 2 axes

    if (cs.getRankDomain() < 2) {
      if (sbuff != null) {
        sbuff.format("%s: domain rank < 2%n", cs.getName());
      }
      return false;
    }

    // must be lat/lon or have x,y and projecction
    if (!cs.isLatLon()) {
      // do check for GeoXY ourself
      if ((cs.getXaxis() == null) || (cs.getYaxis() == null)) {
        if (sbuff != null) {
          sbuff.format("%s: NO Lat,Lon or X,Y axis%n", cs.getName());
        }
        return false;
      }
      if (null == cs.getProjection()) {
        if (sbuff != null) {
          sbuff.format("%s: NO projection found%n", cs.getName());
        }
        return false;
      }
    }

    // obtain the x,y or lat/lon axes. x,y normally must be convertible to km
    CoordinateAxis xaxis, yaxis;
    if (cs.isGeoXY()) {
      xaxis = cs.getXaxis();
      yaxis = cs.getYaxis();

      // change to warning
      ProjectionImpl p = cs.getProjection();
      if (!(p instanceof RotatedPole)) {
        if (!SimpleUnit.kmUnit.isCompatible(xaxis.getUnitsString())) {
          if (sbuff != null) {
            sbuff.format("%s: X axis units are not convertible to km%n", cs.getName());
          }
          //return false;
        }
        if (!SimpleUnit.kmUnit.isCompatible(yaxis.getUnitsString())) {
          if (sbuff != null) {
            sbuff.format("%s: Y axis units are not convertible to km%n", cs.getName());
          }
          //return false;
        }
      }
    } else {
      xaxis = cs.getLonAxis();
      yaxis = cs.getLatAxis();
    }

    // check x,y rank <= 1
    if ((xaxis.getRank() > 1) || (yaxis.getRank() > 1)) {
      if (sbuff != null)
        sbuff.format("%s: X or Y axis rank must be <= 1%n", cs.getName());
      return false;
    }

    // check that the x,y have at least 2 dimensions between them ( this eliminates point data)
    List<Dimension> xyDomain = CoordinateSystem.makeDomain(new CoordinateAxis[]{xaxis, yaxis});
    if (xyDomain.size() < 1) {
      if (sbuff != null)
        sbuff.format("%s: X and Y axis must have 1 or more dimensions%n", cs.getName());
      return false;
    }

    List<CoordinateAxis> testAxis = new ArrayList<CoordinateAxis>();
    testAxis.add(xaxis);
    testAxis.add(yaxis);

    //int countRangeRank = 2;

    CoordinateAxis z = cs.getHeightAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = cs.getPressureAxis();
    if ((z == null) || !(z instanceof CoordinateAxis1D)) z = cs.getZaxis();
    if ((z != null) && !(z instanceof CoordinateAxis1D)) {
      if (sbuff != null) {
        sbuff.format("%s: Z axis must be 1D%n", cs.getName());
      }
      return false;
    }
    if (z != null)
      testAxis.add(z);

    // tom margolis 3/2/2010
    // allow runtime independent of time
    CoordinateAxis t = cs.getTaxis();
    CoordinateAxis rt = cs.findAxis(AxisType.RunTime);

    // A runtime axis must be one-dimensional
    if (rt != null && !(rt instanceof CoordinateAxis1D)) {
      if (sbuff != null) {
        sbuff.format("%s: RunTime axis must be 1D%n", cs.getName());
      }
      return false;
    }

    // If time axis is two-dimensional...
    if ((t != null) && !(t instanceof CoordinateAxis1D) && (t.getRank() != 0)) {
      // ... a runtime axis is required
      if (rt == null) {
        if (sbuff != null) sbuff.format("%s: T axis must be 1D%n", cs.getName());
        return false;
      }

      if (t.getRank() != 2) {
        if (sbuff != null) {
          sbuff.format("%s: Time axis must be 2D when used with RunTime dimension%n", cs.getName());
        }
        return false;
      }

      CoordinateAxis1D rt1D = (CoordinateAxis1D) rt;
      if (!rt1D.getDimension(0).equals(t.getDimension(0))) {
        if (sbuff != null) {
          sbuff.format("%s: Time axis must use RunTime dimension%n", cs.getName());
        }
        return false;
      }
    }

    // Set the primary temporal axis - either Time or Runtime
    if (t != null) {
      testAxis.add(t);
    } else if (rt != null) {
      testAxis.add(rt);
    }

    CoordinateAxis ens = cs.getEnsembleAxis();
    if (ens != null)
      testAxis.add(ens);

    if (v != null) { // test to see that v doesnt have extra dimensions. LOOK RELAX THIS
      List<Dimension> testDomain = new ArrayList<Dimension>();
      for (CoordinateAxis axis : testAxis) {
        for (Dimension dim : axis.getDimensions()) {
          if (!testDomain.contains(dim))
            testDomain.add(dim);
        }
      }
      //if (!CoordinateSystem.isSubset(v.getDimensionsAll(), testDomain)) {
      //  if (sbuff != null) sbuff.format(" NOT complete\n");
      //  return false;
      //}
    }

    return true;
  }

  /**
   * Create a MeshCoordSys from an existing Coordinate System.
   * This will choose which axes are the XHoriz, YHoriz, Vertical, and Time.
   * If theres a Projection, it will set its map area
   *
   * @param cs    create from this Coordinate System
   * @param sbuff place information messages here, may be null
   */
  public MeshCoordSys(CoordinateSystem cs, Formatter sbuff) {
    super();
    this.ds = cs.getNetcdfDataset();

    if (cs.isGeoXY()) {
      horizXaxis = xAxis = cs.getXaxis();
      horizYaxis = yAxis = cs.getYaxis();

      ProjectionImpl p = cs.getProjection();
      if (!(p instanceof RotatedPole) && !(p instanceof RotatedLatLon)) {
        // make a copy of the axes if they need to change
        horizXaxis = convertUnits(horizXaxis);
        horizYaxis = convertUnits(horizYaxis);
      }
    } else if (cs.isLatLon()) {
      horizXaxis = lonAxis = cs.getLonAxis();
      horizYaxis = latAxis = cs.getLatAxis();
      isLatLon = true;

    } else {
      throw new IllegalArgumentException("CoordinateSystem is not geoReferencing");
    }
    
    coordAxes.add(horizXaxis);
    coordAxes.add(horizYaxis);

    // set canonical area
    ProjectionImpl projOrig = cs.getProjection();
    if (projOrig != null) {
      proj = projOrig.constructCopy();
      proj.setDefaultMapArea(getBoundingBox());  // LOOK too expensive for 2D
    }

   // LOOK: require 1D vertical - need to generalize to nD vertical.
    CoordinateAxis z_oneD = hAxis = cs.getHeightAxis();
    if ((z_oneD == null) || !(z_oneD instanceof CoordinateAxis1D)) z_oneD = pAxis = cs.getPressureAxis();
    if ((z_oneD == null) || !(z_oneD instanceof CoordinateAxis1D)) z_oneD = zAxis = cs.getZaxis();
    if ((z_oneD != null) && !(z_oneD instanceof CoordinateAxis1D))
      z_oneD = null;

    CoordinateAxis z_best = hAxis;
    if (pAxis != null) {
      if ((z_best == null) || !(z_best.getRank() > pAxis.getRank())) z_best = pAxis;
    }
    if (zAxis != null) {
      if ((z_best == null) || !(z_best.getRank() > zAxis.getRank())) z_best = zAxis;
    }

    if ((z_oneD == null) && (z_best != null)) { // cant find one-d z but have nD z
      if (sbuff != null) sbuff.format("MeshCoordSys needs a 1D Coordinate, instead has %s%n", z_best.getNameAndDimensions());
    }

    if (z_oneD != null) {
      vertZaxis = (CoordinateAxis1D) z_oneD;
      coordAxes.add(vertZaxis);
    } else {
      hAxis = pAxis = zAxis = null;
    }

    // timeTaxis must be CoordinateAxis1DTime
    CoordinateAxis t = cs.getTaxis();
    if (t != null) {

      if (t instanceof CoordinateAxis1D) {

        try {
          if (t instanceof CoordinateAxis1DTime)
            timeTaxis = (CoordinateAxis1DTime) t;
          else {
            timeTaxis = CoordinateAxis1DTime.factory(ds, t, sbuff);
          }

          tAxis = timeTaxis;
          coordAxes.add(timeTaxis);
          timeDim = t.getDimension(0);

        } catch (Exception e) {
          if (sbuff != null)
            sbuff.format("Error reading time coord= %s err= %s\n", t.getName(), e.getMessage());
        }

      } else { // 2d

        tAxis = t;
        timeTaxis = null;
        coordAxes.add(t); // LOOK ??
      }
    }

    // look for special axes
    ensembleAxis = (CoordinateAxis1D) cs.findAxis(AxisType.Ensemble);
    if (null != ensembleAxis) coordAxes.add(ensembleAxis);

    CoordinateAxis1D rtAxis = (CoordinateAxis1D) cs.findAxis(AxisType.RunTime);
    if (null != rtAxis) {
      try {
        if (rtAxis instanceof CoordinateAxis1DTime)
          runTimeAxis = (CoordinateAxis1DTime) rtAxis;
        else
          runTimeAxis = CoordinateAxis1DTime.factory(ds, rtAxis, sbuff);

        coordAxes.add(runTimeAxis);

      } catch (IOException e) {
        if (sbuff != null) {
          sbuff.format("Error reading runtime coord= %s err= %s\n", t.getName(), e.getMessage());
        }
      }
    }

    // look for VerticalCT
    List<CoordinateTransform> list = cs.getCoordinateTransforms();
    for (CoordinateTransform ct : list) {
      if (ct instanceof VerticalCT) {
        vCT = (VerticalCT) ct;
        break;
      }
    }

    // make name based on coordinate
    Collections.sort(coordAxes, new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    this.name = makeName(coordAxes);

    // copy all coordinate transforms into here
    this.coordTrans = new ArrayList<CoordinateTransform>(cs.getCoordinateTransforms());

    // collect dimensions
    for (CoordinateAxis axis : coordAxes) {
      List<Dimension> dims = axis.getDimensions();
      for (Dimension dim : dims) {
        if (!domain.contains(dim))
          domain.add(dim);
      }
    }

  }

  /**
   * get the Time axis, if its 1-dimensional
   */
  public CoordinateAxis1DTime getTimeAxis1D() {
    return timeTaxis;
  }

  /**
   * True if there is a Time Axis and it is 1D.
   */
  public boolean hasTimeAxis1D() {
    return timeTaxis != null;
  }

  private CoordinateAxis1DTime makeTimeAxisForRun(int run_index) {
    VariableDS section;
    try {
      section = (VariableDS) tAxis.slice(0, run_index);
      return CoordinateAxis1DTime.factory(ds, section, null);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  // we have to delay making these, since we dont identify the dimensions specifically until now
  void makeVerticalTransform(MeshDataset mds, Formatter parseInfo) {
    if (vt != null) return; // already done
    if (vCT == null) return;  // no vt

    vt = vCT.makeVerticalTransform(mds.getNetcdfDataset(), timeDim);

    if (vt == null) {
      if (parseInfo != null)
        parseInfo.format("  - ERR can't make VerticalTransform = %s\n", vCT.getVerticalTransformType());
    } else {
      if (parseInfo != null) parseInfo.format("  - VerticalTransform = %s\n", vCT.getVerticalTransformType());
    }
  }

  public CoordinateAxis1DTime getTimeAxisForRun(int run_index) {
    if (!hasTimeAxis() || hasTimeAxis1D()) return null;
    int nruns = (int) runTimeAxis.getSize();
    if ((run_index < 0) || (run_index >= nruns))
      throw new IllegalArgumentException("getTimeAxisForRun index out of bounds= " + run_index);

    if (timeAxisForRun == null)
      timeAxisForRun = new CoordinateAxis1DTime[nruns];

    if (timeAxisForRun[run_index] == null)
      timeAxisForRun[run_index] = makeTimeAxisForRun(run_index);

    return timeAxisForRun[run_index];
  }

  public DateRange getDateRange() {
    if (timeDates == null) makeTimes();
    if (isDate) {
      Date[] dates = getTimeDates();
      return new DateRange(dates[0], dates[dates.length - 1]);
    }

    return null;
  }

  /**
   * Determine if the CoordinateSystem cs can be made into a GridCoordSys for the Variable v.
   *
   * @param sbuff put debug information into this StringBuffer; may be null.
   * @param cs    CoordinateSystem to check.
   * @param v     Variable to check.
   * @return the GridCoordSys made from cs, else null.
   */
  public static MeshCoordSys makeMeshCoordSys(Formatter sbuff, CoordinateSystem cs, VariableEnhanced v) {
    if (sbuff != null) {
      sbuff.format(" ");
      v.getNameAndDimensions(sbuff, false, true);
      sbuff.format(" check CS %s: ", cs.getName());
    }

    if (isMeshCoordSys(sbuff, cs, v)) {
      MeshCoordSys gcs = new MeshCoordSys(cs, sbuff);
      if (sbuff != null) sbuff.format(" OK\n");
      return gcs;
    }

    return null;
  }

  /**
   * Get the list of times as Dates. Only valid if isDate() is true;
   *
   * @return array of java.util.Date, or null.
   */
  public java.util.Date[] getTimeDates() {
    if (timeDates == null) makeTimes();
    return timeDates;
  }
  
  private void makeTimes() {
    if ((timeTaxis == null) || (timeTaxis.getSize() == 0)) {
      times = new ArrayList<NamedObject>( 0);
      timeDates = new Date[0];
      isDate = false;
      return;
    }

    int n = (int) timeTaxis.getSize();
    timeDates = new Date[n];
    times = new ArrayList<NamedObject>( n);

    // see if it has a valid udunits unit
    try {
      DateUnit du = null;
      String units = timeTaxis.getUnitsString();
      if (units != null)
        du = new DateUnit(units);
      DateFormatter formatter = new DateFormatter();
      for (int i = 0; i < n; i++) {
        Date d = du.makeDate(timeTaxis.getCoordValue(i));
        String name = formatter.toDateTimeString(d);
        if (name == null)  // LOOK bug in udunits ??
          name = Double.toString(timeTaxis.getCoordValue(i));
        times.add(new ucar.nc2.util.NamedAnything(name, "date/time"));
        timeDates[i] = d;
      }
      isDate = true;
      return;
    } catch (Exception e) {
      // ok to fall through
    }

    // otherwise, see if its a String, and if we can parse the values as an ISO date
    if ((timeTaxis.getDataType() == DataType.STRING) || (timeTaxis.getDataType() == DataType.CHAR)) {
      isDate = true;
      DateFormatter formatter = new DateFormatter();
      for (int i = 0; i < n; i++) {
        String coordValue = timeTaxis.getCoordName(i);
        Date d = formatter.getISODate(coordValue);
        if (d == null) {
          isDate = false;
          times.add(new ucar.nc2.util.NamedAnything(coordValue, timeTaxis.getUnitsString()));
        } else {
          times.add(new ucar.nc2.util.NamedAnything(formatter.toDateTimeString(d), "date/time"));
          timeDates[i] = d;
        }
      }
      return;
    }

    // otherwise
    for (int i = 0; i < n; i++) {
      times.add(new ucar.nc2.util.NamedAnything(timeTaxis.getCoordName(i), timeTaxis.getUnitsString()));
    }
  }

  /**
   * true if increasing z coordinate values means "up" in altitude
   */
  public boolean isZPositive() {
    if (vertZaxis == null) return false;
    if (vertZaxis.getPositive() != null) {
      return vertZaxis.getPositive().equalsIgnoreCase(ucar.nc2.constants.CF.POSITIVE_UP);
    }
    if (vertZaxis.getAxisType() == AxisType.Height) return true;
    if (vertZaxis.getAxisType() == AxisType.Pressure) return false;
    return true; // default
  }

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the node index
   *
   * @param nindex n index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int nindex) {
    return null;
  }

  /**
   * Given a lat,lon point, find the x,y index in the coordinate system.
   *
   * @param lat    latitude position.
   * @param lon    longitude position.
   * @param result put result in here, may be null
   * @return int, node index in the coordinate system of the point. This will be -1 if out of range.
   */
  public int findNodeIndexFromLatLon(double lat, double lon, int result) {
    Projection dataProjection = getProjection();
    ProjectionPoint pp = dataProjection.latLonToProj(new LatLonPointImpl(lat, lon), new ProjectionPointImpl());

    return findNodeIndexFromCoord(pp.getX(), pp.getY(), result);
  }

  /**
   * Given a lat,lon point, find the n index in the coordinate system.
   * If outside the range, the closest point is returned
   *
   * @param lat    latitude position.
   * @param lon    longitude position.
   * @param result put result in here, may be null
   * @return int, n index in the coordinate system of the point.
   */
  public int findNodeIndexFromLatLonBounded(double lat, double lon, int result) {
    Projection dataProjection = getProjection();
    ProjectionPoint pp = dataProjection.latLonToProj(new LatLonPointImpl(lat, lon), new ProjectionPointImpl());

    //return 1;
    return findNodeIndexFromCoordBounded(pp.getX(), pp.getY(), result);
  }

  /**
   * Given a point in x,y coordinate space, find the node index in the coordinate system.
   * Not implemented yet for 2D.
   *
   * @param x_coord position in x coordinate space.
   * @param y_coord position in y coordinate space.
   * @param result  put result (x,y) index in here, may be null
   * @return int, node index in the coordinate system of the point. This will be -1 if out of range.
   */
  public int findNodeIndexFromCoord(double x_coord, double y_coord, int result) {
    return 1;
  }


  /**
   * Given a point in x,y coordinate space, find the node index in the coordinate system.
   * If outside the range, the closest point is returned
   * Not implemented yet for 2D.
   *
   * @param x_coord position in x coordinate space.
   * @param y_coord position in y coordinate space.
   * @param result  put result in here, may be null
   * @return int, n index in the coordinate system of the point.
   */
  public int findNodeIndexFromCoordBounded(double x_coord, double y_coord, int result) {
    return 1;
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * @return A Range object of the node indexes
   * @throws ucar.ma2.InvalidRangeException if llbb generates bad ranges
   */
  public Range getRangeFromLatLonRect(LatLonRect rect) throws InvalidRangeException {
    ProjectionImpl proj = getProjection();
    if (proj != null && !(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation)) { // LOOK kludge - how to do this generrally ??
      // first clip the request rectangle to the bounding box of the grid
      LatLonRect bb = getLatLonBoundingBox();
      rect = bb.intersect(rect);
      if (null == rect)
        throw new InvalidRangeException("Request Bounding box does not intersect Grid");
    }

    // Find all of the nodes contained within the bounding box
    return null;
  }

  private LatLonRect llbb = null;
  /**
   * Get horizontal bounding box in lat, lon coordinates.
   *
   * @return lat, lon bounding box.
   */
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {

        Projection dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();

        // look at all 4 corners of the bounding box
        LatLonPointImpl llpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerLeftPoint(), new LatLonPointImpl());
        LatLonPointImpl lrpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getLowerRightPoint(), new LatLonPointImpl());
        LatLonPointImpl urpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperRightPoint(), new LatLonPointImpl());
        LatLonPointImpl ulpt = (LatLonPointImpl) dataProjection.projToLatLon(bb.getUpperLeftPoint(), new LatLonPointImpl());

        // Check if grid contains poles.
        boolean includesNorthPole = false;
        int resultNP = findNodeIndexFromLatLon(90.0, 0, -1);
        if (resultNP != -1)
          includesNorthPole = true;
        boolean includesSouthPole = false;
        int resultSP = findNodeIndexFromLatLon(-90.0, 0, -1);
        if (resultSP != -1)
          includesSouthPole = true;

        if (includesNorthPole && !includesSouthPole) {
          llbb = new LatLonRect(llpt, new LatLonPointImpl(90.0, 0.0)); // ??? lon=???
          llbb.extend(lrpt);
          llbb.extend(urpt);
          llbb.extend(ulpt);
          // OR
          //llbb.extend( new LatLonRect( llpt, lrpt ));
          //llbb.extend( new LatLonRect( lrpt, urpt ) );
          //llbb.extend( new LatLonRect( urpt, ulpt ) );
          //llbb.extend( new LatLonRect( ulpt, llpt ) );
        } else if (includesSouthPole && !includesNorthPole) {
          llbb = new LatLonRect(llpt, new LatLonPointImpl(-90.0, -180.0)); // ??? lon=???
          llbb.extend(lrpt);
          llbb.extend(urpt);
          llbb.extend(ulpt);
        } else {
          double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
          double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

          // longitude is a bit tricky as usual
          double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
          double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

          llpt.set(latMin, lonMin);
          urpt.set(latMax, lonMax);

          llbb = new LatLonRect(llpt, urpt);
        }
    }

    return llbb;
  }

  private CoordinateAxis convertUnits(CoordinateAxis axis) {
    String units = axis.getUnitsString();
    SimpleUnit axisUnit = SimpleUnit.factory(units);
    double factor;
    try {
      factor = axisUnit.convertTo(1.0, SimpleUnit.kmUnit);
    } catch (IllegalArgumentException e) {
      log.warn("convertUnits failed", e);
      return axis;
    }
    if (factor == 1.0) return axis;

    Array data;
    try {
      data = axis.read();
    } catch (IOException e) {
      log.warn("convertUnits read failed", e);
      return axis;
    }

    DataType dtype = axis.getDataType();
    if (dtype.isFloatingPoint()) {
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext())
        ii.setDoubleCurrent(factor * ii.getDoubleNext());

      CoordinateAxis newAxis = axis.copyNoCache();
      newAxis.setCachedData(data, false);
      newAxis.setUnitsString("km");
      return newAxis;

    } else {  // convert to DOUBLE
      Array newData = Array.factory(DataType.DOUBLE, axis.getShape());
      IndexIterator newi = newData.getIndexIterator();
      IndexIterator ii = data.getIndexIterator();
      while (ii.hasNext() && newi.hasNext())
        newi.setDoubleNext(factor * ii.getDoubleNext());

      CoordinateAxis newAxis = axis.copyNoCache();
      newAxis.setDataType(DataType.DOUBLE);
      newAxis.setCachedData(newData, false);
      newAxis.setUnitsString("km");
      return newAxis;
    }
  }

  private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPointImpl.lonNormal(lon1, midpoint);
    lon2 = LatLonPointImpl.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

  private ProjectionRect mapArea = null;
  /**
   * Get the x,y bounding box in projection coordinates.
   */
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {

      if ((horizXaxis == null) || !horizXaxis.isNumeric() || (horizYaxis == null) || !horizYaxis.isNumeric())
        return null; // impossible

      // x,y may be 2D
      if (!(horizXaxis instanceof CoordinateAxis1D) || !(horizYaxis instanceof CoordinateAxis1D)) {
        /*  could try to optimize this - just get cord=ners or something
        CoordinateAxis2D xaxis2 = (CoordinateAxis2D) horizXaxis;
        CoordinateAxis2D yaxis2 = (CoordinateAxis2D) horizYaxis;
        MAMath.MinMax
        */

        mapArea = new ProjectionRect(horizXaxis.getMinValue(), horizYaxis.getMinValue(),
                horizXaxis.getMaxValue(), horizYaxis.getMaxValue());

      } else {

        CoordinateAxis1D xaxis1 = (CoordinateAxis1D) horizXaxis;
        CoordinateAxis1D yaxis1 = (CoordinateAxis1D) horizYaxis;

        /* add one percent on each side if its a projection. WHY?
        double dx = 0.0, dy = 0.0;
        if (!isLatLon()) {
          dx = .01 * (xaxis1.getCoordEdge((int) xaxis1.getSize()) - xaxis1.getCoordEdge(0));
          dy = .01 * (yaxis1.getCoordEdge((int) yaxis1.getSize()) - yaxis1.getCoordEdge(0));
        }

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0) - dx, yaxis1.getCoordEdge(0) - dy,
            xaxis1.getCoordEdge((int) xaxis1.getSize()) + dx,
            yaxis1.getCoordEdge((int) yaxis1.getSize()) + dy); */

        mapArea = new ProjectionRect(xaxis1.getCoordEdge(0), yaxis1.getCoordEdge(0),
                xaxis1.getCoordEdge((int) xaxis1.getSize()),
                yaxis1.getCoordEdge((int) yaxis1.getSize()));
      }
    }

    return mapArea;
  }

  /**
   * Get the vertical transform function, or null if none
   *
   * @return the vertical transform function, or null if none
   */
  public VerticalTransform getVerticalTransform() {
    return vt;
  }

  /**
   * Get the Coordinate Transform description.
   *
   * @return Coordinate Transform description, or null if none
   */
  public VerticalCT getVerticalCT() {
    return vCT;
  }

  public void setProjectionBoundingBox() {
    // set canonical area
    if (proj != null) {
      proj.setDefaultMapArea(getBoundingBox());  // LOOK too expensive for 2D
    }
  }

  /**
   * get the RunTime axis, else null
   */
  public CoordinateAxis1DTime getRunTimeAxis() {
    return runTimeAxis;
  }

  /**
   * get the Ensemble axis, else null
   */
  public CoordinateAxis1D getEnsembleAxis() {
    return ensembleAxis;
  }

  /**
   * get the Time axis
   */
  public CoordinateAxis getTimeAxis() {
    return tAxis;
  }

  /**
   * get the Vertical axis (either Geoz, Height, or Pressure)
   */
  public CoordinateAxis1D getVerticalAxis() {
    return vertZaxis;
  }

  /**
   * get the N Horizontal axis (either GeoN or Node)
   */
  public CoordinateAxis getNodeAxis() {
    return Naxis;
  }

}
