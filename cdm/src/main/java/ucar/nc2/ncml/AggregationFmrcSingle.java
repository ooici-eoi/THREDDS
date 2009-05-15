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

package ucar.nc2.ncml;

import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

import thredds.crawlabledataset.CrawlableDataset;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 * with files that contain a single forecast time.
 *
 * @author caron
 */
public class AggregationFmrcSingle extends AggregationFmrc {
  private Calendar cal; // for date computations

  private Map<Date, List<DatasetFmrcSingle>> runHash = new HashMap<Date, List<DatasetFmrcSingle>>();
  private List<Date> runs; // list of run dates

  private CoordinateAxis1D timeAxis = null;
  private int max_times = 0;
  //private Dataset typicalDataset = null;
  //private NetcdfFile typicalFile;
  //private GridDataset typicalGridDataset = null;
  private boolean debug = false;

  private String runMatcher, forecastMatcher, offsetMatcher; // scanFmrc

  public AggregationFmrcSingle(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.forecastModelRunSingleCollection, recheckS);
    cal = new GregorianCalendar();
    cal.clear();
  }

  public void addDirectoryScanFmrc(String dirName, String suffix, String regexpPatternString, String subdirs, String olderThan,
                                   String runMatcher, String forecastMatcher, String offsetMatcher) throws IOException {

    this.runMatcher = runMatcher;
    this.forecastMatcher = forecastMatcher;
    this.offsetMatcher = offsetMatcher;

    // this.enhance = NetcdfDataset.getDefaultEnhanceMode();
    isDate = true;

    DatasetScanner d = new DatasetScanner(null, dirName, suffix, regexpPatternString, subdirs, olderThan);
    datasetManager.addDirectoryScan(d);
  }

  @Override
  protected void closeDatasets() throws IOException {
    /* if (typicalGridDataset != null) {
      typicalGridDataset.close();
    }  */

    for (Dataset ds : datasets) {
      OpenDataset ods = (OpenDataset) ds;
      if (ods.openFile != null)
        ods.openFile.close();
    }
  }

  @Override
  public void getDetailInfo(Formatter f) {
    super.getDetailInfo(f);
    if (runMatcher != null)
      f.format("  runMatcher=%s%n", runMatcher);
    if (forecastMatcher != null)
      f.format("  forecastMatcher=%s%n", forecastMatcher);
    if (offsetMatcher != null)
      f.format("  offsetMatcher=%s%n", offsetMatcher);
  }

  /* @Override
  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    buildNetcdfDataset(typicalDataset, typicalFile, typicalGridDataset, cancelTask);
  }  */

  @Override
  protected void makeDatasets(CancelTask cancelTask) throws IOException {

    // find the runtime, forecast time coordinates, put in list
    runHash = new HashMap<Date, List<DatasetFmrcSingle>>();

    Dataset typDataset = null;
    for (CrawlableDataset cd : datasetManager.getFiles()) {
      // create the dataset wrapping this file, each is 1 forecast time coordinate of the nested aggregation
      DatasetFmrcSingle ds = new DatasetFmrcSingle(cd);
      if (typDataset == null) // grab the first one
        typDataset = ds;

      // add to list for given run date
      List<DatasetFmrcSingle> runDatasets = runHash.get(ds.runDate);
      if (runDatasets == null) {
        runDatasets = new ArrayList<DatasetFmrcSingle>();
        runHash.put(ds.runDate, runDatasets);
      }
      runDatasets.add(ds);

      if (debug)
        System.out.println("  adding " + cd.getPath() + " forecast date= " + ds.coordValue + "(" + ds.coordValueDate + ")"
            + " run date= " + dateFormatter.toDateTimeStringISO(ds.runDate));
    }

    // LOOK - should cache the GridDataset directly    
    // open a "typical" dataset and make a GridDataset
    NetcdfFile typFile = typDataset.acquireFile(cancelTask);
    NetcdfDataset typDS = NetcdfDataset.wrap(typFile, null);
    GridDataset typGds = new ucar.nc2.dt.grid.GridDataset(typDS);

    // find the one time axis
    for (GridDatatype grid : typGds.getGrids()) {
      GridCoordSystem gcc = grid.getCoordinateSystem();
      timeAxis = gcc.getTimeAxis1D();
      if (null != timeAxis)
        break;
    }

    if (timeAxis == null)
      throw new IllegalStateException("No time variable");

    // create new list of Datasets
    datasets = new ArrayList<Dataset>();
    for (Aggregation.Dataset dataset : explicitDatasets) {
      dataset.enhance = fmrcEnhanceMode;
      datasets.add(dataset);
    }

    // loop over the runs; each becomes a nested dataset
    max_times = 0;
    runs = new ArrayList<Date>(runHash.keySet());
    Collections.sort(runs);
    for (Date runDate : runs) {
      String runDateS = dateFormatter.toDateTimeStringISO(runDate);

      List<DatasetFmrcSingle> runDatasets = runHash.get(runDate);
      max_times = Math.max(max_times, runDatasets.size());

      // within each list, sort the datasets by time coordinate
      Collections.sort(runDatasets);

      // create the dataset wrapping this run, each is 1 runtime coordinate of the outer aggregation
      NetcdfDataset ncd = new NetcdfDataset();
      ncd.setLocation("Run" + runDateS);
      DateFormatter format = new DateFormatter();
      if (debug) System.out.println("Run" + format.toDateTimeString(runDate));

      AggregationExisting agg = new AggregationExisting(ncd, timeAxis.getName(), null); // LOOK: dim name, existing vs new ??
      for (Dataset dataset : runDatasets) {
        agg.addDataset(dataset);
        if (debug)
          System.out.println("  adding Forecast " + format.toDateTimeString(((DatasetOuterDimension) dataset).coordValueDate) + " " + dataset.getLocation());
      }
      ncd.setAggregation(agg);
      agg.finish(cancelTask);
      ncd.finish();

      datasets.add(new OpenDataset(ncd, runDate, runDateS));
    }

    typGds.close();
  }

  private Date addHour(Date d, double hour) {
    cal.setTime(d);

    int ihour = (int) hour;
    int imin = (int) (hour - ihour) * 60;
    cal.add(Calendar.HOUR_OF_DAY, ihour);
    cal.add(Calendar.MINUTE, imin);
    return cal.getTime();
  }

  /*  used in buildDataset
  @Override
  protected Dataset getTypicalDataset() throws IOException {
    return typicalDataset;
  } */

  // for the case that we dont have a fmrcDefinition.
  @Override
  protected void makeTimeCoordinate(GridDataset gds, CancelTask cancelTask) throws IOException {
    String innerDimName = timeAxis.getName();
    Dimension innerDim = new Dimension(innerDimName, max_times);
    ncDataset.removeDimension(null, innerDimName); // remove previous declaration, if any
    ncDataset.addDimension(null, innerDim);

    int[] shape = new int[]{runs.size(), max_times};
    Array timeCoordVals = Array.factory(DataType.DOUBLE, shape);
    MAMath.setDouble(timeCoordVals, Double.NaN); // anything not set is missing
    Index ima = timeCoordVals.getIndex();

    // loop over the runs, calculate the offset for each dataset
    Date baseDate = null;
    for (int i = 0; i < runs.size(); i++) {
      Date runDate = runs.get(i);
      if (baseDate == null) baseDate = runDate;

      List<DatasetFmrcSingle> runDatasets = runHash.get(runDate);
      for (int j = 0; j < runDatasets.size(); j++) {
        DatasetFmrcSingle dataset = runDatasets.get(j);
        double offset = ForecastModelRunInventory.getOffsetInHours(baseDate, dataset.coordValueDate);
        timeCoordVals.setDouble(ima.set(i, j), offset);
      }
    }

    // construct new variable, replace old one, set values
    String dims = dimName + " " + innerDimName;
    String units = "hours since " + dateFormatter.toDateTimeStringISO(baseDate);
    String desc = "calculated forecast date from AggregationFmrcSingle processing";
    VariableDS vagg = new VariableDS(ncDataset, null, null, innerDimName, DataType.DOUBLE, dims, units, desc);
    vagg.setCachedData(timeCoordVals, false);
    DatasetConstructor.transferVariableAttributes(timeAxis, vagg);
    vagg.addAttribute(new Attribute("units", units));
    vagg.addAttribute(new Attribute("long_name", desc));
    vagg.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));

    //ncDataset.removeVariable(null, vagg.getName());
    ncDataset.addCoordinateAxis(vagg);

    if (debug) System.out.println("FmrcAggregation: promoted timeCoord " + innerDimName);
  }

  // the timeAxis will be 2D, and there's only one
  @Override
  protected void readTimeCoordinates(VariableDS timeAxis, CancelTask cancelTask) throws IOException {

    // redo the time dimension, makes things easier if you dont replace Dimension, just modify the length
    String dimName = timeAxis.getName();
    Dimension timeDim = ncDataset.findDimension(dimName); // LOOK use group
    timeDim.setLength(max_times);

    // reset all variables using this dimension
    List<Variable> vars = ncDataset.getVariables();
    for (Variable v : vars) {
      if (v.findDimensionIndex(dimName) >= 0) {
        v.resetDimensions();
        v.setCachedData(null, false); // get rid of any cached data, since its now wrong
      }
    }

    // create the data array for the time coordinate
    int[] shape = new int[]{runs.size(), max_times};
    Array timeCoordVals = Array.factory(DataType.DOUBLE, shape);
    MAMath.setDouble(timeCoordVals, Double.NaN); // anything not set is missing
    Index ima = timeCoordVals.getIndex();

    // loop over the runs, calculate the offset for each dataset
    Date baseDate = null;
    for (int i = 0; i < runs.size(); i++) {
      Date runDate = runs.get(i);
      if (baseDate == null) baseDate = runDate;

      List<DatasetFmrcSingle> runDatasets = runHash.get(runDate);
      for (int j = 0; j < runDatasets.size(); j++) {
        DatasetOuterDimension dataset = runDatasets.get(j);
        double offset = ForecastModelRunInventory.getOffsetInHours(baseDate, dataset.coordValueDate);
        timeCoordVals.setDouble(ima.set(i, j), offset);
      }
    }
    timeAxis.setCachedData(timeCoordVals, true);

    String units = "hours since " + dateFormatter.toDateTimeStringISO(baseDate);
    timeAxis.addAttribute(new Attribute("units", units));
  }

  public class DatasetFmrcSingle extends DatasetOuterDimension {
    Date runDate;
    Double offset;

    DatasetFmrcSingle(CrawlableDataset cd) {
      super(cd.getPath());
      this.cacheLocation = this.location;
      this.enhance = fmrcEnhanceMode;
      this.ncoord = 1;

      // parse for rundate
      if (runMatcher != null) {
        runDate = DateFromString.getDateUsingDemarkatedMatch(location, runMatcher, '#');
        if (null == runDate) {
          logger.error("Cant extract rundate from =" + location + " using format " + runMatcher);
        }
      }

      // parse for forecast date
      if (forecastMatcher != null) {
        coordValueDate = DateFromString.getDateUsingDemarkatedMatch(location, forecastMatcher, '#');
        if (null == coordValueDate) {
          logger.error("Cant extract forecast date from =" + location + " using format " + forecastMatcher);
        } else
          coordValue = dateFormatter.toDateTimeStringISO(coordValueDate);
      }

      // parse for forecast offset
      if (offsetMatcher != null) {
        offset = DateFromString.getHourUsingDemarkatedMatch(location, offsetMatcher, '#');
        if (null == offset) {
          logger.error("Cant extract forecast offset from =" + location + " using format " + offsetMatcher);
        }
        coordValueDate = addHour(runDate, offset);
        coordValue = dateFormatter.toDateTimeStringISO(coordValueDate);
      }
    }

  }

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  public class OpenDataset extends DatasetOuterDimension {
    private NetcdfFile openFile;

    /**
     * Dataset constructor with an opened NetcdfFile.
     * Used in nested aggregations like scanFmrc.
     *
     * @param openFile       already opened file
     * @param coordValueDate has this coordinate as a date
     * @param coordValue     has this coordinate as a String
     */
    protected OpenDataset(NetcdfFile openFile, Date coordValueDate, String coordValue) {
      super(openFile.getLocation());
      this.openFile = openFile;
      this.ncoord = 1;
      this.coordValueDate = coordValueDate;
      this.coordValue = coordValue;
    }

    @Override
    public NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      return openFile;
    }

    @Override
    protected void close(NetcdfFile ncfile) throws IOException {
      if (ncfile == null) return;
      cacheVariables(ncfile);
    }

  }

}
