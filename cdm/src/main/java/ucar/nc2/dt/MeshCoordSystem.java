/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt;

import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.nc2.units.DateRange;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.LatLonPoint;

import java.util.List;

/**
 * A Coordinate System for mesh data. Assume:
 * <ul>
 * <li> N is 1 dimensional
 * <li> T is 1 or 2 dimensional. The 2D case is that it depends on runtime.
 * <li> We can create Dates out of the T and RT coordinate values.
 * <li> Z, E, RT are 1-dimensional
 * <li> An optional VerticalTransform can provide a height or pressure coordinate that may be 1-4 dimensional.
 * </ul>
 * <p/>
 *
 * @author Kyle
 */
public interface MeshCoordSystem {
  
  /**
   * The name of the Grid Coordinate System, consisting of the list of coordinate axes, seperated by blanks.
   * @return  name of the Grid Coordinate System
   */
  public String getName();

  /**
   * Get the list of dimensions used by any of the Axes in the Coordinate System.
   * @return List of Dimension
   */
  public List<Dimension> getDomain();

  // axes

  /**
   * Get the list of all axes.
   * @return List of CoordinateAxis.
   */
  public List<CoordinateAxis> getCoordinateAxes();

  /**
   * True if all axes are 1 dimensional.
   * @return true if all axes are 1 dimensional.
   */
  public boolean isProductSet();

  /**
   * Get the N axis. Must be 1 dimensional.
   * @return N CoordinateAxis, may not be null.
   */
  public CoordinateAxis getNodeAxis();

  /**
   * Get the Z axis. Must be 1 dimensional.
   * @return Y CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getVerticalAxis();

  /**
   * Get the Time axis, if it exists. May be 1 or 2 dimensional.
   * If 1D, will be a CoordinateAxis1DTime. If 2D, then you can use getTimeAxisForRun().
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date of measurement or valid forecast time.
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis getTimeAxis();

  /**
   * Get the ensemble axis. Must be 1 dimensional.
   * Typical meaning is an enumeration of ensemble Model runs.
   * @return ensemble CoordinateAxis, may be null.
   */
  public CoordinateAxis1D getEnsembleAxis();

  /**
   * Get the RunTime axis. Must be 1 dimensional.
   * A runtime coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date that a Forecast Model Run is made.
   * @return RunTime CoordinateAxis, may be null.
   */
  public CoordinateAxis1DTime getRunTimeAxis();

  // transforms

  /**
   * Get the list of all CoordinateTransforms.
   * @return List of CoordinateTransform.
   */
  public List<CoordinateTransform> getCoordinateTransforms();

  /**
   * Get the Projection CoordinateTransform. It must exist if !isLatLon().
   * @return ProjectionCT or null.
   */
  public ProjectionCT getProjectionCT();

  /**
   * Get the Projection that performs the transform math.
   * Same as getProjectionCT().getProjection().
   * @return ProjectionImpl or null.
   */
  public ucar.unidata.geoloc.ProjectionImpl getProjection();

  /**
   * Use the bounding box to set the default map are of the projection.
   * This can be expensive if its a 2D coordinate system.
   */
  public void setProjectionBoundingBox();

  /**
   * Get the Vertical CoordinateTransform, it it exists.
   * @return VerticalCT or null.
   */
  public VerticalCT getVerticalCT();

  /**
   * Get the VerticalTransform that performs the transform math.
   * Same as getVerticalCT().getVerticalTransform().
   * @return VerticalTransform or null.
   */
  public ucar.unidata.geoloc.vertical.VerticalTransform getVerticalTransform();

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   * For projection, only an approximation based on corners.
   * @return LatLonRect bounding box.
   */
  public ucar.unidata.geoloc.LatLonRect getLatLonBoundingBox();

   /**
   * Get horizontal bounding box in projection coordinates.
   * For lat/lon, the ProjectionRect has units of degrees north and east.
   * @return ProjectionRect bounding box.
   */
  public ucar.unidata.geoloc.ProjectionRect getBoundingBox();

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have CoordinateAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * @return A Range object of the node indexes
   * @throws ucar.ma2.InvalidRangeException if llbb generates bad ranges
   */
  public Range getRangeFromLatLonRect(ucar.unidata.geoloc.LatLonRect llbb) throws InvalidRangeException;

  /**
   * Given a point in x,y coordinate space, find the x,y indices.
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result  optionally pass in the result array to use.
   * @return int, node index These will be -1 if out of range.
   */
  public int findNodeIndexFromCoord(double x_coord, double y_coord, int result);

  /**
   * Given a point in x,y coordinate space, find the x,y indices.
   * If outside the range, the closest point is returned
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result  optionally pass in the result array to use.
   * @return int, node index
   */
  public int findNodeIndexFromCoordBounded(double x_coord, double y_coord, int result);

  /**
   * Given a lat,lon point, find the node index of the containing grid point.
   *
   * @param lat latitude position.
   * @param lon longitude position.
   * @param result  put result in here, may be null
   * @return int, node index. This will be -1 if out of range.
   */
  public int findNodeIndexFromLatLon(double lat, double lon, int result) ;

  /**
   * Given a lat,lon point, find the node index of the containing grid point.
   * If outside the range, the closest point is returned
   *
   * @param lat latitude position.
   * @param lon longitude position.
   * @param result return result here, may be null
   * @return int, node index
   */
  public int findNodeIndexFromLatLonBounded(double lat, double lon, int result) ;

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the node index.
   *
   * @param nindex  n index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int nindex);


  // vertical

  /**
   * True if increasing z coordinate values means "up" in altitude
   * @return true if increasing z coordinate values means "up" in altitude
   */
  public boolean isZPositive();

  // time

  /**
   * If there is a time coordinate, get the time covered.
   * @return DateRange or null if no time coordinate
   */
  public DateRange getDateRange();

  /**
   * True if there is a Time Axis.
   * @return true if there is a Time Axis.
   */
  public boolean hasTimeAxis();

  /**
   * True if there is a Time Axis and it is 1D.
   * @return true if there is a Time Axis and it is 1D.
   */
  public boolean hasTimeAxis1D();

  /**
   * Get the Time axis, if it exists, and its 1-dimensional.
   * @return the time coordinate axis, may be null.
   */
  public CoordinateAxis1DTime getTimeAxis1D();

  /**
   * This is the case of a 2D time axis, which depends on the run index.
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * @param run_index which run?
   * @return 1D time axis for that run.
   */
  public CoordinateAxis1DTime getTimeAxisForRun(int run_index);
}
