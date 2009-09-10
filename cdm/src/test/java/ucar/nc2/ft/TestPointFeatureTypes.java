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

package ucar.nc2.ft;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.util.*;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.TestAll;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.Station;
import ucar.unidata.geoloc.LatLonPointImpl;

/**
 * Test PointFeatureTypes.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class TestPointFeatureTypes extends TestCase {
  private static String topdir = TestAll.testdataDir + "cdmUnitTest/";

  public TestPointFeatureTypes(String name) {
    super(name);
  }

  public void testAll() throws IOException {
    scanDir(topdir + "cfPoint/", new MyFileFilter());
    //scanDir(ucar.nc2.TestAll.testdataDir + "station/");
  }

  private void scanDir(String dir, FileFilter ff) throws IOException {
    TestAll.actOnAll(dir, ff, new TestAll.Act() {

      public int doAct(String filename) throws IOException {
        testPointDataset(filename, FeatureType.ANY_POINT, true);
        //testPointDataset(filename, FeatureType.POINT, true);
        return 1;
      }
    });
  }


  class MyFileFilter implements FileFilter {
    public boolean accept(File pathname) {
      String path = pathname.getPath();
      // eliminate once that have been replaced by ncml
      if (new File(path + ".ncml").exists()) return false;
      if (path.endsWith(".ncml")) return true;
      int pos = path.lastIndexOf(".");
      if (new File(path.substring(0, pos) + ".ncml").exists()) return false;
      return true;
    }
  }


  public void testCF() throws IOException {

    /////// POINT
    // CF 1.1 psuedo-structure
    testPointDataset(topdir + "cfPoint/point/filtered_apriori_super_calibrated_binned1.nc", FeatureType.POINT, true);

    // CF 1.5 psuedo-structure
    testPointDataset(topdir + "cfPoint/point/nmcbob.shp.nc", FeatureType.POINT, true);

    /////// STATION
    // CF 1.3 ragged contiguous, single station
    testPointDataset(topdir + "cfPoint/station/rig_tower.2009-02-01.ncml", FeatureType.STATION, true);

    // CF 1.5 station unlimited, multidim
    testPointDataset(topdir + "cfPoint/station/billNewDicast.nc", FeatureType.STATION, true);

    // CF 1.5 station regular (not unlimited), multidim
    testPointDataset(topdir + "cfPoint/station/billOldDicast.nc", FeatureType.STATION, true);

    // CF 1.0 multidim with dimensions reversed
    //testPointDataset(topdir+"cfPoint/station/solrad_point_pearson.ncml", FeatureType.STATION, true);

    // CF 1.5 multidim stations, stn dim unlimited, must distinguish station table from obs.
    testPointDataset(topdir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, true);

  }

  public void testGempak() throws IOException {
    // (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    testPointDataset(TestAll.cdmUnitTestDir + "formats/gempak/surface/19580807_sao.gem", FeatureType.STATION, true);

    // stationAsPoint (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    //testPointDataset(TestAll.cdmUnitTestDir + "formats/gempak/surface/20090521_sao.gem", FeatureType.POINT, true);

    testGempakAll(TestAll.cdmUnitTestDir + "formats/gempak/surface/20090524_sao.gem");
    //testGempakAll(TestAll.cdmUnitTestDir+"C:/data/ft/station/09052812.sf");
  }

  public void testGempakAll(String filename) throws IOException {
    testPointDataset(filename, FeatureType.ANY_POINT, true);
    testLocation(filename, FeatureType.ANY_POINT, true);
    //testPointDataset(filename, FeatureType.POINT, true);
    //testLocation(filename, FeatureType.POINT, true);
  }


  public void utestProblemGemPak() throws IOException {
    testPointDataset("D:\\formats\\gempak\\surface\\20090521_sao.gem", FeatureType.STATION, true);
  }

  public void utestProblem() throws IOException {
    testPointDataset("D:\\datasets\\metars\\Surface_METAR_20070513_0000.nc", FeatureType.STATION, true);
  }

  public void utestCollectionGempak() throws IOException {
    //testPointDataset("collection:C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);
    testPointDataset("collection:D:/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);
  }

  public void utestCollection() throws IOException {
    //Surface_METAR_20070326_0000.nc
    testPointDataset("collection:C:/data/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", FeatureType.STATION, true);
    //testPointDataset("collection:D:/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", FeatureType.STATION, true);
  }

  public void utestCdmRemote() throws IOException {
    testPointDataset("cdmremote:http://localhost:8080/thredds/cdmremote/station/testCdmRemote/gempak/19580807_sao.gem", FeatureType.STATION, true);
  }

  public void testCdmRemoteCollection() throws IOException {
    testDons("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", false);
    //testDons("collection:C:/data/datasets/metars/Surface_METAR_#yyyyMMdd_HHmm#.nc", true);
    //testDons("C:/data/datasets/metars/Surface_METAR_20070326_0000.nc", true);
    //testDons("cdmremote:http://localhost:8080/thredds/cdmremote/idd/metar/ncdecodedLocalHome", true);
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", FeatureType.STATION, true);
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", FeatureType.ANY_POINT, true);
    //testPointDataset("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", FeatureType.POINT, true);
  }

  public void utestCdmRemoteCollectionSubsets() throws IOException {
    Formatter f = new Formatter();
    String location = "cdmremote:http://localhost:8080/thredds/cdmremote/gempakSurface.xml/collection";
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.STATION, location, null, f);
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    assert fdpoint.getPointFeatureCollectionList().size() == 1;
    FeatureCollection fc = fdpoint.getPointFeatureCollectionList().get(0);
    assert (fc instanceof StationTimeSeriesFeatureCollection);
    StationTimeSeriesFeatureCollection stnc = (StationTimeSeriesFeatureCollection) fc;

    Station stn = stnc.getStation("04V");
    assert (stn != null);
    StationTimeSeriesFeature stnFeature = stnc.getStationFeature(stn);
    assert (stnFeature != null);
    stnFeature.calcBounds();
    int n = stnFeature.size();
    System.out.printf(" n=%d from %s ", n, stnFeature);

    testPointFeatureCollection(stnFeature, true);

    fdataset.close();
  }

  int readAllDir(String dirName, FileFilter ff, FeatureType type) throws IOException {
    int count = 0;

    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude")) {
        try {
          testPointDataset(name, type, false);
        } catch (Throwable t) {
          t.printStackTrace();
        }
        count++;
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += readAllDir(f.getAbsolutePath(), ff, type);
    }

    return count;
  }

  private void testPointDataset(String location, FeatureType type, boolean show) throws IOException {
    System.out.printf("----------- Read %s %n", location);
    long start = System.currentTimeMillis();

    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }

    // FeatureDataset
    if (show) {
      fdataset.getDetailInfo(out);
      System.out.printf("%s %n", out);
    } else {
      System.out.printf("  Feature Type %s %n", fdataset.getFeatureType());
    }

    Date d1 = fdataset.getStartDate();
    Date d2 = fdataset.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals(d2);

    List dataVars = fdataset.getDataVariables();
    assert dataVars != null;
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) dataVars.get(i);
      assert null != fdataset.getDataVariable(v.getShortName());
    }

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();

      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        int count = testPointFeatureCollection(pfc, show);
        System.out.println(" getData count= " + count + " size= " + pfc.size());
        assert count == pfc.size();

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        testStationFeatureCollection((StationTimeSeriesFeatureCollection) fc);
        //testNestedPointFeatureCollection((StationTimeSeriesFeatureCollection) fc, show);

      } else {

        testNestedPointFeatureCollection((NestedPointFeatureCollection) fc, show);
      }
    }

    fdataset.close();
    long took = System.currentTimeMillis() - start;
    System.out.println(" took= " + took + " msec");
  }

  // loop through all PointFeatureCollection
  void testNestedPointFeatureCollection(NestedPointFeatureCollection npfc, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    PointFeatureCollectionIterator iter = npfc.getPointFeatureCollectionIterator(-1);
    while (iter.hasNext()) {
      PointFeatureCollection pfc = iter.next();
      count += testPointFeatureCollection(pfc, false);
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testNestedPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
  }

  int testPointFeatureCollection(PointFeatureCollection pfc, boolean show) throws IOException {
    if (show) {
      System.out.println(" test PointFeatureCollection " + pfc.getName());
      System.out.println(" calcBounds");
    }
    pfc.calcBounds();
    if (show) {
      System.out.println("  bb= " + pfc.getBoundingBox());
      System.out.println("  dateRange= " + pfc.getDateRange());
      System.out.println("  npts= " + pfc.size());
    }

    int n = pfc.size();
    if (n == 0) {
      System.out.println("  empty " + pfc);
      return 0; // empty
    }

    LatLonRect bb = pfc.getBoundingBox();
    assert bb != null;
    DateRange dr = pfc.getDateRange();
    assert dr != null;

    // read all the data - check that it is contained in the bbox, dateRange
    if (show) System.out.println(" complete iteration");
    long start = System.currentTimeMillis();
    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      testPointFeature(pf);
      assert bb.contains(pf.getLocation().getLatLon()) : pf.getLocation().getLatLon();
      if (!dr.contains(pf.getObservationTimeAsDate()))
        System.out.printf("  date out of Range= %s on %s %n", pf.getObservationTimeAsDate(), pfc.getName());
      count++;
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");

    // subset with a bounding box, test result is in the bounding box
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
    PointFeatureCollection subset = pfc.subset(bb2, null);
    if (show) System.out.println(" subset bb= " + bb2.toString2());

    start = System.currentTimeMillis();
    int counts = 0;
    PointFeatureIterator iters = subset.getPointFeatureIterator(-1);
    while (iters.hasNext()) {
      PointFeature pf = iters.next();
      assert pf != null;
      assert pf.getLocation() != null;

      assert bb2.contains(pf.getLocation().getLatLon()) : bb2.toString2() + " does not contains point " + pf.getLocation().getLatLon();
      //System.out.printf(" contains point %s%n",pf.getLocation().getLatLon());

      testPointFeature(pf);
      counts++;
    }
    took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");

    return count;
  }

  // check that the location and times are filled out
  // read and test the data
  private void testPointFeature(PointFeature pobs) throws java.io.IOException {

    EarthLocation loc = pobs.getLocation();
    assert loc != null;

    assert null != pobs.getNominalTimeAsDate();
    assert null != pobs.getObservationTimeAsDate();

    DateUnit timeUnit = pobs.getTimeUnit();
    assert timeUnit.makeDate(pobs.getNominalTime()).equals(pobs.getNominalTimeAsDate());
    assert timeUnit.makeDate(pobs.getObservationTime()).equals(pobs.getObservationTimeAsDate());

    StructureData sdata = pobs.getData();
    assert null != sdata;
    testData(sdata);
  }

  // read each field, check datatype
  private void testData(StructureData sdata) {

    for (StructureMembers.Member member : sdata.getMembers()) {
      DataType dt = member.getDataType();
      if (dt == DataType.FLOAT) {
        sdata.getScalarFloat(member);
        sdata.getJavaArrayFloat(member);
      } else if (dt == DataType.DOUBLE) {
        sdata.getScalarDouble(member);
        sdata.getJavaArrayDouble(member);
      } else if (dt == DataType.BYTE) {
        sdata.getScalarByte(member);
        sdata.getJavaArrayByte(member);
      } else if (dt == DataType.SHORT) {
        sdata.getScalarShort(member);
        sdata.getJavaArrayShort(member);
      } else if (dt == DataType.INT) {
        sdata.getScalarInt(member);
        sdata.getJavaArrayInt(member);
      } else if (dt == DataType.LONG) {
        sdata.getScalarLong(member);
        sdata.getJavaArrayLong(member);
      } else if (dt == DataType.CHAR) {
        sdata.getScalarChar(member);
        sdata.getJavaArrayChar(member);
        sdata.getScalarString(member);
      } else if (dt == DataType.STRING) {
        sdata.getScalarString(member);
      }

      if ((dt != DataType.STRING) && (dt != DataType.CHAR) && (dt != DataType.STRUCTURE)) {
        sdata.convertScalarFloat(member.getName());
      }

    }
  }

  ////////////////////////////////////////////////////////////

  void testStationFeatureCollection(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf("Complete Iteration for %s %n", sfc.getName());
    int countAll = count(sfc);

    // try a subset
    LatLonRect bb = sfc.getBoundingBox();
    assert bb != null;
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
    System.out.println("Subset= " + bb2.toString2());
    StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
    int countSub = count(sfcSub);
    assert countSub <= countAll;

    System.out.println("Flatten= " + bb2.toString2());
    PointFeatureCollection flatten = sfc.flatten(bb2, null);
    int countFlat = count(flatten);

    assert countFlat <= countAll;
  }

  int count(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf(" Station List Size = %d %n", sfc.getStations().size());

    // check uniqueness
    Map<String, StationTimeSeriesFeature> stns = new HashMap<String, StationTimeSeriesFeature>(5000);
    Map<MyLocation, StationTimeSeriesFeature> locs = new HashMap<MyLocation, StationTimeSeriesFeature>(5000);

    sfc.resetIteration();
    while (sfc.hasNext()) {
      StationTimeSeriesFeature sf = sfc.next();
      StationTimeSeriesFeature other = stns.get(sf.getName());
      if (other != null) {
        System.out.printf("  duplicate name = %s %n", sf);
        System.out.printf("   of = %s %n", other);
      } else
        stns.put(sf.getName(), sf);

      MyLocation loc = new MyLocation(sf);
      StationTimeSeriesFeature already = locs.get(loc);
      if (already != null) {
        System.out.printf("  duplicate location %s(%s) of %s(%s) %n", sf.getName(), sf.getDescription(),
                already.getName(), already.getDescription());
      } else
        locs.put(loc, sf);
    }

    System.out.printf(" unique locs = %d %n", locs.size());
    System.out.printf(" unique stns = %d %n", stns.size());

    return stns.size();
  }


  /////////////////////////////////////////////////////////


  void testLocation(String location, FeatureType type, boolean show) throws IOException {
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    List<FeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();

    FeatureCollection fc = collectionList.get(0);

    if (fc instanceof PointFeatureCollection) {
      PointFeatureCollection pfc = (PointFeatureCollection) fc;
      count(pfc);

      LatLonRect bb = pfc.getBoundingBox();
      LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
      PointFeatureCollection subset = pfc.subset(bb2, null);
      count(subset);

    } else if (fc instanceof StationTimeSeriesFeatureCollection) {
      StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) fc;
      PointFeatureCollection pfcAll = sfc.flatten(null, null);
      System.out.printf("Unique Locations all = %d %n", count(pfcAll));

      LatLonRect bb = sfc.getBoundingBox();
      assert bb != null;
      LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2);
      PointFeatureCollection pfcSub = sfc.flatten(bb2, null);
      System.out.printf("Unique Locations sub1 = %d %n", count(pfcSub));

      StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
      PointFeatureCollection pfcSub2 = sfcSub.flatten(null, null);
      System.out.printf("Unique Locations sub2 = %d %n", count(pfcSub2));

      // Dons
      sfc = sfc.subset(bb2);
      PointFeatureCollection subDon = sfc.flatten(bb2, null);
      System.out.printf("Unique Locations subDon = %d %n", count(subDon));
    }

  }

  int count(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    Set<MyLocation> locs = new HashSet<MyLocation>(80000);
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      MyLocation loc = new MyLocation(pf.getLocation());
      if (!locs.contains(loc)) locs.add(loc);
      count++;
      //if (count % 1000 == 0) System.out.printf("Count %d%n", count);
    }

    System.out.printf("Count Points  = %d Unique points = %d %n", count, locs.size());
    return locs.size();

    //The problem is that all the locations are coming up with the same value.  This:
    //always returns the same lat/lon/alt (of the first observation).
    //(pos was populated going through the PointFeatureIterator).

  }

  private class MyLocation {
    double lat, lon, alt;

    public MyLocation(EarthLocation from) {
      this.lat = from.getLatitude();
      this.lon = from.getLongitude();
      this.alt = Double.isNaN(from.getAltitude()) ? 0.0 : from.getAltitude();
    }

    @Override
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof MyLocation)) return false;
      MyLocation other = (MyLocation) oo;
      return (lat == other.lat) && (lon == other.lon) && (alt == other.alt);
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        result += 37 * result + lat * 10000;
        result += 37 * result + lon * 10000;
        result += 37 * result + alt * 10000;
        hashCode = result;
      }
      return hashCode;
    }

    private int hashCode = 0;
  }

  /////////////////////////////////////////////////////////

  private void testDons(String file, boolean showTime) throws IOException {
    long start = System.currentTimeMillis();
    if (showTime) {
      ucar.unidata.io.RandomAccessFile.setDebugAccess(true);
    }

    Formatter buf = new Formatter();
    FeatureDatasetPoint pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.POINT, file, null, buf);
    if (pods == null) {  // try as ANY_POINT
      System.out.println("trying as ANY_POINT");
      pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
              ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
    } else
      System.out.println("Open as POINT");

    if (pods == null) {
      throw new IOException("can't open file error=" + buf);
    }

    List<FeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    FeatureCollection fc = collectionList.get(0);
    LatLonRect llr = new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
    System.out.println("llr = " + llr);

    PointFeatureCollection collection = null;
    if (fc instanceof PointFeatureCollection) {
      collection = (PointFeatureCollection) fc;
      if (llr != null) {
        collection = collection.subset(llr, null);
      }
    } else if (fc instanceof NestedPointFeatureCollection) {
      NestedPointFeatureCollection npfc = (NestedPointFeatureCollection) fc;
      // npfc = npfc.subset(llr);
      collection = npfc.flatten(llr, null);
    } else {
      throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
    }

    PointFeatureIterator dataIterator = collection.getPointFeatureIterator(-1);
    try {
      int numObs = 0;
      while (dataIterator.hasNext()) {
        PointFeature po = (PointFeature) dataIterator.next();
        numObs++;
        ucar.unidata.geoloc.EarthLocation el = po.getLocation();
        assert llr.contains(el.getLatLon()) : el.getLatLon();
        if (numObs % 1000 == 0)
          System.out.printf("%d el = %s %n", numObs, el);
      }

        long took = System.currentTimeMillis() - start;
        System.out.printf("response took %d msecs nobs = %d%n  seeks= %d nbytes read= %d%n", took, numObs,
                ucar.unidata.io.RandomAccessFile.getDebugNseeks(), ucar.unidata.io.RandomAccessFile.getDebugNbytes());
    } finally {
      if (dataIterator != null)
        dataIterator.finish();
    }
    long took = System.currentTimeMillis() - start;
    System.out.printf("%ntotal response took %d msecs%n", took);
  }

  public static void main(String arg[]) throws IOException {
    TestPointFeatureTypes test = new TestPointFeatureTypes("");
    test.testDons("cdmremote:http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak", false);

  }


}

