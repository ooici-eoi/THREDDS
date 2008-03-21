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
package ucar.nc2.iosp.nexrad2;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import static ucar.nc2.iosp.nexrad2.Level2Record.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

/**
 * An IOServiceProvider for NEXRAD level II files.
 *
 * @author caron
 */
public class Nexrad2IOServiceProvider extends AbstractIOServiceProvider {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Nexrad2IOServiceProvider.class);
  static private final int MISSING_INT = -9999;
  static private final float MISSING_FLOAT = Float.NaN;

  public boolean isValidFile( RandomAccessFile raf) throws IOException {
    try {
      raf.seek(0);
      byte[] b = new byte[8];
      raf.read(b);
      String test = new String( b);
      return test.equals( Level2VolumeScan.ARCHIVE2) || test.equals( Level2VolumeScan.AR2V0001) ||
              test.equals( Level2VolumeScan.AR2V0003);
    } catch (IOException ioe) {
      return false;
    }
  }

  private Level2VolumeScan volScan;
 // private Dimension radialDim;
  private double radarRadius;
  private Variable v0, v1;
  private DateFormatter formatter = new DateFormatter();

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    NexradStationDB.init();

    volScan = new Level2VolumeScan( raf, cancelTask);
    if (volScan.hasDifferentDopplarResolutions())
      throw new IllegalStateException("volScan.hasDifferentDopplarResolutions");


    if( volScan.hasHighResolutions(0)) {

        if(volScan.getHighResReflectivityGroups() != null)
            makeVariable2( ncfile, Level2Record.REFLECTIVITY_HIGH, "Reflectivity", "Reflectivity", "R", volScan);
        if( volScan.getHighResVelocityGroups() != null)
            makeVariable2( ncfile, Level2Record.VELOCITY_HIGH, "RadialVelocity", "Radial Velocity", "V", volScan);

        if( volScan.getHighResSpectrumGroups() != null) {
            List<Level2Record> gps = volScan.getHighResSpectrumGroups();
            List<Level2Record> gp = (List)gps.get(0);
            Level2Record record = gp.get(0);
            makeVariableNoCoords( ncfile, Level2Record.SPECTRUM_WIDTH_HIGH, "SpectrumWidth_HI", "Radial Spectrum_HI", v1, record);
            makeVariableNoCoords( ncfile, Level2Record.SPECTRUM_WIDTH, "SpectrumWidth", "Radial Spectrum", v0, record);
        }
    }
    if( volScan.getReflectivityGroups() != null) {
        makeVariable( ncfile, Level2Record.REFLECTIVITY, "Reflectivity", "Reflectivity", "R", volScan.getReflectivityGroups(), 0);
        int velocity_type =  (volScan.getDopplarResolution() == Level2Record.DOPPLER_RESOLUTION_HIGH_CODE) ? Level2Record.VELOCITY_HI : Level2Record.VELOCITY_LOW;
        Variable v = makeVariable( ncfile, velocity_type, "RadialVelocity", "Radial Velocity", "V", volScan.getVelocityGroups(), 0);
        List<Level2Record> gps = volScan.getVelocityGroups();
        List<Level2Record> gp = (List)gps.get(0);
        Level2Record record = gp.get(0);
        makeVariableNoCoords( ncfile, Level2Record.SPECTRUM_WIDTH, "SpectrumWidth", "Spectrum Width", v, record);
    }
    if (volScan.getStationId() != null) {
      ncfile.addAttribute(null, new Attribute("Station", volScan.getStationId()));
      ncfile.addAttribute(null, new Attribute("StationName", volScan.getStationName()));
      ncfile.addAttribute(null, new Attribute("StationLatitude", volScan.getStationLatitude()));
      ncfile.addAttribute(null, new Attribute("StationLongitude", volScan.getStationLongitude()));
      ncfile.addAttribute(null, new Attribute("StationElevationInMeters", volScan.getStationElevation()));

      double latRadiusDegrees = Math.toDegrees( radarRadius / ucar.unidata.geoloc.Earth.getRadius());
      ncfile.addAttribute(null, new Attribute("geospatial_lat_min", volScan.getStationLatitude() - latRadiusDegrees));
      ncfile.addAttribute(null, new Attribute("geospatial_lat_max", volScan.getStationLatitude() + latRadiusDegrees));
      double cosLat = Math.cos( Math.toRadians(volScan.getStationLatitude()));
      double lonRadiusDegrees = Math.toDegrees( radarRadius / cosLat / ucar.unidata.geoloc.Earth.getRadius());
      ncfile.addAttribute(null, new Attribute("geospatial_lon_min", volScan.getStationLongitude() - lonRadiusDegrees));
      ncfile.addAttribute(null, new Attribute("geospatial_lon_max", volScan.getStationLongitude() + lonRadiusDegrees));


          // add a radial coordinate transform (experimental)
        /*
      Variable ct = new Variable(ncfile, null, null, "radialCoordinateTransform");
      ct.setDataType(DataType.CHAR);
      ct.setDimensions(""); // scalar
      ct.addAttribute( new Attribute("transform_name", "Radial"));
      ct.addAttribute( new Attribute("center_latitude", volScan.getStationLatitude()));
      ct.addAttribute( new Attribute("center_longitude", volScan.getStationLongitude()));
      ct.addAttribute( new Attribute("center_elevation", volScan.getStationElevation()));
      ct.addAttribute( new Attribute(_Coordinate.TransformType, "Radial"));
      ct.addAttribute( new Attribute(_Coordinate.AxisTypes, "RadialElevation RadialAzimuth RadialDistance"));

      Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[0], new char[] {' '});
      ct.setCachedData(data, true);
      ncfile.addVariable(null, ct);
      */
    }

    DateFormatter formatter = new DateFormatter();

    ncfile.addAttribute(null, new Attribute("Conventions", _Coordinate.Convention));
    ncfile.addAttribute(null, new Attribute("format", volScan.getDataFormat()));
    Date d = getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    ncfile.addAttribute(null, new Attribute("base_date", formatter.toDateOnlyString(d)));

    ncfile.addAttribute(null, new Attribute("time_coverage_start", formatter.toDateTimeStringISO(d)));
    ncfile.addAttribute(null, new Attribute("time_coverage_end", formatter.toDateTimeStringISO(volScan.getEndDate())));

    ncfile.addAttribute(null, new Attribute("history", "direct read of Nexrad Level 2 file into NetCDF-Java 2.2 API"));
    ncfile.addAttribute(null, new Attribute("DataType", "Radial"));

    ncfile.addAttribute(null, new Attribute("Title", "Nexrad Level 2 Station "+volScan.getStationId()+" from "+
        formatter.toDateTimeStringISO(volScan.getStartDate()) + " to " +
        formatter.toDateTimeStringISO(volScan.getEndDate())));

    ncfile.addAttribute(null, new Attribute("Summary", "Weather Surveillance Radar-1988 Doppler (WSR-88D) "+
        "Level II data are the three meteorological base data quantities: reflectivity, mean radial velocity, and "+
        "spectrum width."));

    ncfile.addAttribute(null, new Attribute("keywords", "WSR-88D; NEXRAD; Radar Level II; reflectivity; mean radial velocity; spectrum width"));

    ncfile.addAttribute(null, new Attribute("VolumeCoveragePatternName",
      getVolumeCoveragePatternName(volScan.getVCP())));
    ncfile.addAttribute(null, new Attribute("VolumeCoveragePattern", volScan.getVCP()));
    ncfile.addAttribute(null, new Attribute("HorizonatalBeamWidthInDegrees", (double) HORIZONTAL_BEAM_WIDTH));

    ncfile.finish();
  }

  public void makeVariable2(NetcdfFile ncfile, int datatype, String shortName, String longName, String abbrev, Level2VolumeScan vScan) throws IOException {
      List groups = null;

      if( shortName.startsWith("Reflectivity"))
        groups = vScan.getHighResReflectivityGroups();
      else if( shortName.startsWith("RadialVelocity"))
        groups = vScan.getHighResVelocityGroups();

      int nscans = groups.size();

    if (nscans == 0) {
      throw new IllegalStateException("No data for "+shortName);
    }

    ArrayList firstGroup = new ArrayList(groups.size());
    ArrayList secondGroup = new ArrayList(groups.size());

    for(int i = 0; i < nscans; i++) {
        List o = (List) groups.get(i);
        int s = o.size();
        if(s > 600)
            firstGroup.add(o);
        else
            secondGroup.add(o);
    }
    if(firstGroup != null)
        v1 = makeVariable(ncfile, datatype, shortName + "_HI", longName + "_HI",  abbrev + "_HI", firstGroup, 1);
    if(secondGroup != null)
        v0 = makeVariable(ncfile, datatype, shortName, longName,  abbrev, secondGroup, 0);

  }

  public int getMaxRadials(List groups) {
      int maxRadials = 0;
      for (int i = 0; i < groups.size(); i++) {
        ArrayList group = (ArrayList) groups.get(i);
        maxRadials = Math.max(maxRadials, group.size());
      }
      return maxRadials;
  }

  public Variable makeVariable(NetcdfFile ncfile, int datatype, String shortName, String longName, String abbrev, List groups, int rd) throws IOException {
    int nscans = groups.size();

    if (nscans == 0) {
      throw new IllegalStateException("No data for "+shortName);
    }

    // get representative record
    List<Level2Record> firstGroup = (List)groups.get(0);
    Level2Record firstRecord = firstGroup.get(0);
    int ngates = firstRecord.getGateCount(datatype);

    String scanDimName = "scan"+abbrev;
    String gateDimName = "gate"+abbrev;
    String radialDimName = "radial"+abbrev;
    Dimension scanDim = new Dimension(scanDimName, nscans);
    Dimension gateDim = new Dimension(gateDimName, ngates);
    Dimension radialDim = new Dimension(radialDimName, volScan.getMaxRadials(rd), true);
    ncfile.addDimension( null, scanDim);
    ncfile.addDimension( null, gateDim);
    ncfile.addDimension( null, radialDim);

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add( scanDim);
    dims.add( radialDim);
    dims.add( gateDim);

    Variable v = new Variable(ncfile, null, null, shortName);
    v.setDataType(DataType.BYTE);
    v.setDimensions(dims);
    ncfile.addVariable(null, v);

    v.addAttribute( new Attribute("units", getDatatypeUnits(datatype)));
    v.addAttribute( new Attribute("long_name", longName));


    byte[] b = new byte[2];
    b[0] = MISSING_DATA;
    b[1] = BELOW_THRESHOLD;
    Array missingArray = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[] {2}, b);

    v.addAttribute( new Attribute("missing_value", missingArray));
    v.addAttribute( new Attribute("signal_below_threshold", BELOW_THRESHOLD));
    v.addAttribute( new Attribute("scale_factor", firstRecord.getDatatypeScaleFactor(datatype)));
    v.addAttribute( new Attribute("add_offset", firstRecord.getDatatypeAddOffset(datatype)));
    v.addAttribute( new Attribute("_unsigned", "true"));
    if(rd == 1) {
       v.addAttribute( new Attribute("SNR_threshold" ,firstRecord.getDatatypeSNRThreshhold(datatype)));
       v.addAttribute( new Attribute("range_folding_threshold" ,firstRecord.getDatatypeRangeFoldingThreshhold(datatype)));
    }

    List<Dimension> dim2 = new ArrayList<Dimension>();
    dim2.add( scanDim);
    dim2.add( radialDim);

    // add time coordinate variable
    String timeCoordName = "time"+abbrev;
    Variable timeVar = new Variable(ncfile, null, null, timeCoordName);
    timeVar.setDataType(DataType.INT);
    timeVar.setDimensions(dim2);
    ncfile.addVariable(null, timeVar);


    // int julianDays = volScan.getTitleJulianDays();
    // Date d = Level2Record.getDate( julianDays, 0);
    Date d = getDate(volScan.getTitleJulianDays(), volScan.getTitleMsecs());
    String units = "msecs since "+formatter.toDateTimeStringISO(d);

    timeVar.addAttribute( new Attribute("long_name", "time since base date"));
    timeVar.addAttribute( new Attribute("units", units));
    timeVar.addAttribute( new Attribute("missing_value", MISSING_INT));
    timeVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    // add elevation coordinate variable
    String elevCoordName = "elevation"+abbrev;
    Variable elevVar = new Variable(ncfile, null, null, elevCoordName);
    elevVar.setDataType(DataType.FLOAT);
    elevVar.setDimensions(dim2);
    ncfile.addVariable(null, elevVar);

    elevVar.addAttribute( new Attribute("units", "degrees"));
    elevVar.addAttribute( new Attribute("long_name", "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular"));
    elevVar.addAttribute( new Attribute("missing_value", MISSING_FLOAT));
    elevVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString()));

    // add azimuth coordinate variable
    String aziCoordName = "azimuth"+abbrev;
    Variable aziVar = new Variable(ncfile, null, null, aziCoordName);
    aziVar.setDataType(DataType.FLOAT);
    aziVar.setDimensions(dim2);
    ncfile.addVariable(null, aziVar);

    aziVar.addAttribute( new Attribute("units", "degrees"));
    aziVar.addAttribute( new Attribute("long_name", "azimuth angle in degrees: 0 = true north, 90 = east"));
    aziVar.addAttribute( new Attribute("missing_value", MISSING_FLOAT));
    aziVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString()));

    // add gate coordinate variable
    String gateCoordName = "distance"+abbrev;
    Variable gateVar = new Variable(ncfile, null, null, gateCoordName);
    gateVar.setDataType(DataType.FLOAT);
    gateVar.setDimensions(gateDimName);
    Array data = Array.makeArray( DataType.FLOAT, ngates,
        (double) firstRecord.getGateStart(datatype), (double) firstRecord.getGateSize(datatype));
    gateVar.setCachedData( data, false);
    ncfile.addVariable(null, gateVar);
    radarRadius = firstRecord.getGateStart(datatype) + ngates * firstRecord.getGateSize(datatype);

    gateVar.addAttribute( new Attribute("units", "m"));
    gateVar.addAttribute( new Attribute("long_name", "radial distance to start of gate"));
    gateVar.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString()));

    // add number of radials variable
    String nradialsName = "numRadials"+abbrev;
    Variable nradialsVar = new Variable(ncfile, null, null, nradialsName);
    nradialsVar.setDataType(DataType.INT);
    nradialsVar.setDimensions(scanDim.getName());
    nradialsVar.addAttribute( new Attribute("long_name", "number of valid radials in this scan"));
    ncfile.addVariable(null, nradialsVar);

    // add number of gates variable
    String ngateName = "numGates"+abbrev;
    Variable ngateVar = new Variable(ncfile, null, null, ngateName);
    ngateVar.setDataType(DataType.INT);
    ngateVar.setDimensions(scanDim.getName());
    ngateVar.addAttribute( new Attribute("long_name", "number of valid gates in this scan"));
    ncfile.addVariable(null, ngateVar);

    makeCoordinateDataWithMissing( datatype, timeVar, elevVar, aziVar, nradialsVar, ngateVar, groups);

    // back to the data variable
    String coordinates = timeCoordName+" "+elevCoordName +" "+ aziCoordName+" "+gateCoordName;
    v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));

    // make the record map
    int nradials = radialDim.getLength();
    Level2Record[][] map = new Level2Record[nscans][nradials];
    for (int i = 0; i < groups.size(); i++) {
      Level2Record[] mapScan = map[i];
      List<Level2Record> group = (List) groups.get(i);
      for (Level2Record r : group) {
        int radial = r.radial_num - 1;
        mapScan[radial] = r;
      }
    }

    Vgroup vg = new Vgroup(datatype, map);
    v.setSPobject( vg);

    return v;
   }

  private void makeVariableNoCoords(NetcdfFile ncfile, int datatype, String shortName, String longName, Variable from,
                                    Level2Record record) {

    // get representative record

    Variable v = new Variable(ncfile, null, null, shortName);
    v.setDataType(DataType.BYTE);
    v.setDimensions( from.getDimensions());
    ncfile.addVariable(null, v);

    v.addAttribute( new Attribute("units", getDatatypeUnits(datatype)));
    v.addAttribute( new Attribute("long_name", longName));

    byte[] b = new byte[2];
    b[0] = MISSING_DATA;
    b[1] = BELOW_THRESHOLD;
    Array missingArray = Array.factory(DataType.BYTE.getPrimitiveClassType(), new int[] {2}, b);
    Attribute scale = from.findAttribute("scale_factor");
    Attribute offset = from.findAttribute("add_offset");
    v.addAttribute( new Attribute("missing_value", missingArray));
    v.addAttribute( new Attribute("signal_below_threshold", BELOW_THRESHOLD));
    v.addAttribute( new Attribute("scale_factor", record.getDatatypeScaleFactor(datatype)));
    v.addAttribute( new Attribute("add_offset", record.getDatatypeAddOffset(datatype)));
    v.addAttribute( new Attribute("_unsigned", "true"));
    if(datatype == Level2Record.SPECTRUM_WIDTH_HIGH){
       v.addAttribute( new Attribute("SNR_threshold" ,record.getDatatypeSNRThreshhold(datatype)));
       v.addAttribute( new Attribute("range_folding_threshold" ,record.getDatatypeRangeFoldingThreshhold(datatype)));
    }
    Attribute fromAtt = from.findAttribute(_Coordinate.Axes);
    v.addAttribute( new Attribute(_Coordinate.Axes, fromAtt));

    Vgroup vgFrom = (Vgroup) from.getSPobject();
    Vgroup vg = new Vgroup(datatype, vgFrom.map);
    v.setSPobject( vg);
  }

  private void makeCoordinateData(int datatype, Variable time, Variable elev, Variable azi, Variable nradialsVar,
                                  Variable ngatesVar, List groups) {

    Array timeData = Array.factory( time.getDataType().getPrimitiveClassType(), time.getShape());
    IndexIterator timeDataIter = timeData.getIndexIterator();

    Array elevData = Array.factory( elev.getDataType().getPrimitiveClassType(), elev.getShape());
    IndexIterator elevDataIter = elevData.getIndexIterator();

    Array aziData = Array.factory( azi.getDataType().getPrimitiveClassType(), azi.getShape());
    IndexIterator aziDataIter = aziData.getIndexIterator();

    Array nradialsData = Array.factory( nradialsVar.getDataType().getPrimitiveClassType(), nradialsVar.getShape());
    IndexIterator nradialsIter = nradialsData.getIndexIterator();

    Array ngatesData = Array.factory( ngatesVar.getDataType().getPrimitiveClassType(), ngatesVar.getShape());
    IndexIterator ngatesIter = ngatesData.getIndexIterator();


    int last_msecs = Integer.MIN_VALUE;
    int nscans = groups.size();
    int maxRadials = volScan.getMaxRadials(0);
    for (int i = 0; i < nscans; i++) {
      List scanGroup = (List) groups.get(i);
      int nradials = scanGroup.size();

      Level2Record first = null;
      for (int j = 0; j < nradials; j++) {
        Level2Record r =  (Level2Record) scanGroup.get(j);
        if (first == null) first = r;

        timeDataIter.setIntNext( r.data_msecs);
        elevDataIter.setFloatNext( r.getElevation());
        aziDataIter.setFloatNext( r.getAzimuth());

        if (r.data_msecs < last_msecs) logger.warn("makeCoordinateData time out of order "+r.data_msecs);
        last_msecs = r.data_msecs;
      }

      for (int j = nradials; j < maxRadials; j++) {
        timeDataIter.setIntNext( MISSING_INT);
        elevDataIter.setFloatNext( MISSING_FLOAT);
        aziDataIter.setFloatNext( MISSING_FLOAT);
      }

      nradialsIter.setIntNext( nradials);
      if (first != null) ngatesIter.setIntNext( first.getGateCount( datatype));
    }

    time.setCachedData( timeData, false);
    elev.setCachedData( elevData, false);
    azi.setCachedData( aziData, false);
    nradialsVar.setCachedData( nradialsData, false);
    ngatesVar.setCachedData( ngatesData, false);
  }

  private void makeCoordinateDataWithMissing(int datatype, Variable time, Variable elev, Variable azi, Variable nradialsVar,
                                  Variable ngatesVar, List groups) {

    Array timeData = Array.factory( time.getDataType().getPrimitiveClassType(), time.getShape());
    Index timeIndex = timeData.getIndex();

    Array elevData = Array.factory( elev.getDataType().getPrimitiveClassType(), elev.getShape());
    Index elevIndex = elevData.getIndex();

    Array aziData = Array.factory( azi.getDataType().getPrimitiveClassType(), azi.getShape());
    Index aziIndex = aziData.getIndex();

    Array nradialsData = Array.factory( nradialsVar.getDataType().getPrimitiveClassType(), nradialsVar.getShape());
    IndexIterator nradialsIter = nradialsData.getIndexIterator();

    Array ngatesData = Array.factory( ngatesVar.getDataType().getPrimitiveClassType(), ngatesVar.getShape());
    IndexIterator ngatesIter = ngatesData.getIndexIterator();

    // first fill with missing data
    IndexIterator ii = timeData.getIndexIterator();
    while (ii.hasNext())
      ii.setIntNext(MISSING_INT);

    ii = elevData.getIndexIterator();
    while (ii.hasNext())
      ii.setFloatNext(MISSING_FLOAT);

    ii = aziData.getIndexIterator();
    while (ii.hasNext())
      ii.setFloatNext(MISSING_FLOAT);

    // now set the  coordinate variables from the Level2Record radial
    int last_msecs = Integer.MIN_VALUE;
    int nscans = groups.size();
    for (int scan = 0; scan < nscans; scan++) {
      List scanGroup = (List) groups.get(scan);
      int nradials = scanGroup.size();

      Level2Record first = null;
      for (int j = 0; j < nradials; j++) {
        Level2Record r =  (Level2Record) scanGroup.get(j);
        if (first == null) first = r;

        int radial = r.radial_num-1;
        timeData.setInt( timeIndex.set(scan, radial), r.data_msecs);
        elevData.setFloat( elevIndex.set(scan, radial), r.getElevation());
        aziData.setFloat( aziIndex.set(scan, radial), r.getAzimuth());

        if (r.data_msecs < last_msecs) logger.warn("makeCoordinateData time out of order "+r.data_msecs);
        last_msecs = r.data_msecs;
      }

      nradialsIter.setIntNext( nradials);
      if (first != null) ngatesIter.setIntNext( first.getGateCount( datatype));
    }

    time.setCachedData( timeData, false);
    elev.setCachedData( elevData, false);
    azi.setCachedData( aziData, false);
    nradialsVar.setCachedData( nradialsData, false);
    ngatesVar.setCachedData( ngatesData, false);
  }

  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    Vgroup vgroup = (Vgroup) v2.getSPobject();

    Range scanRange = section.getRange(0);
    Range radialRange = section.getRange(1);
    Range gateRange = section.getRange(2);

    Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), section.getShape());
    IndexIterator ii = data.getIndexIterator();

    for (int i=scanRange.first(); i<=scanRange.last(); i+= scanRange.stride()) {
      Level2Record[] mapScan = vgroup.map[i];
      readOneScan(mapScan, radialRange, gateRange, vgroup.datatype, ii);
    }

    return data;
  }

  private void readOneScan(Level2Record[] mapScan, Range radialRange, Range gateRange, int datatype, IndexIterator ii) throws IOException {
    for (int i=radialRange.first(); i<=radialRange.last(); i+= radialRange.stride()) {
      Level2Record r = mapScan[i];
      readOneRadial(r, datatype, gateRange, ii);
    }
  }

  private void readOneRadial(Level2Record r, int datatype, Range gateRange, IndexIterator ii) throws IOException {
    if (r == null) {
      for (int i=gateRange.first(); i<=gateRange.last(); i+= gateRange.stride())
        ii.setByteNext( MISSING_DATA);
      return;
    }
    r.readData(volScan.raf, datatype, gateRange, ii);
  }

  private class Vgroup {
    Level2Record[][] map;
    int datatype;

    Vgroup( int datatype, Level2Record[][] map) {
      this.datatype = datatype;
      this.map = map;
    }
  }

  /////////////////////////////////////////////////////////////////////

  public void close() throws IOException {
    volScan.raf.close();
  }

}