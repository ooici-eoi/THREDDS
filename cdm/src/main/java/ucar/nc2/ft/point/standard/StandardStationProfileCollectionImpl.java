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
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.*;
import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.unidata.geoloc.Station;

import java.io.IOException;
import java.util.Iterator;

/**
 * Object Heirarchy:
 * StationProfileFeatureCollection (StandardStationProfileCollectionImpl)
 * StationProfileFeature (StandardStationProfileFeature)
 * ProfileFeature (StandardProfileFeature)
 * PointFeatureIterator (StandardPointFeatureIterator)
 * PointFeature
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardStationProfileCollectionImpl extends StationProfileCollectionImpl {
  private DateUnit timeUnit;
  private NestedTable ft;

  StandardStationProfileCollectionImpl(NestedTable ft, DateUnit timeUnit) throws IOException {
    super(ft.getName());
    this.ft = ft;
    this.timeUnit = timeUnit;
  }

  @Override
  protected void initStationHelper() {
    try {
      stationHelper = new StationHelper();
      StructureDataIterator siter = ft.getStationDataIterator(-1);
      while (siter.hasNext()) {
        StructureData stationData = siter.next();
        Station s = makeStation(stationData, siter.getCurrentRecno());
        if (s != null)
          stationHelper.addStation( s);
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public Station makeStation(StructureData stationData, int recnum) {
    Station s = ft.makeStation(stationData);
    if (s == null) return null;
    return new StandardStationProfileFeature(s, stationData, recnum);
  }

  // iterate over stations
  public NestedPointFeatureCollectionIterator getNestedPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new NestedPointFeatureCollectionIterator() {
      private Iterator iter = getStations().iterator();

      public boolean hasNext() throws IOException {
        return iter.hasNext();
      }

      public NestedPointFeatureCollection next() throws IOException {
        return (StandardStationProfileFeature) iter.next();
      }

      public void setBufferSize(int bytes) {
      }
    };
  }

  // a time series of profiles at one station
  private class StandardStationProfileFeature extends StationProfileFeatureImpl {
    Station s;
    StructureData stationData;
    int recnum;

    StandardStationProfileFeature(Station s, StructureData stationData, int recnum) {
      super(s, StandardStationProfileCollectionImpl.this.timeUnit, -1);
      this.s = s;
      this.stationData = stationData;
      this.recnum = recnum;
    }

    // iterate over series of profiles at a given station
    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      // cursor.what = s;
      cursor.recnum[2] = recnum; // the station record
      cursor.tableData[2] = stationData; // obs(leaf) = 0, profile=1, station(root)=2
      cursor.parentIndex = 2; // LOOK ??
      return new StandardStationProfileFeatureIterator(cursor);
    }

    private class StandardStationProfileFeatureIterator implements PointFeatureCollectionIterator {
      private Cursor cursor;
      private ucar.ma2.StructureDataIterator iter;
      private int count = 0;

      StandardStationProfileFeatureIterator(Cursor cursor) throws IOException {
        this.cursor = cursor;
        iter = ft.getMiddleFeatureDataIterator(cursor, -1);
      }

      public boolean hasNext() throws IOException {
        boolean r = iter.hasNext();
        if (!r)
          timeSeriesNpts = count; // field in StationProfileFeatureImpl
        return r;
      }

      public PointFeatureCollection next() throws IOException {
        Cursor cursorIter = cursor.copy();
        cursorIter.tableData[1] = iter.next(); // the profile record
        cursorIter.recnum[1] = iter.getCurrentRecno();
        cursorIter.parentIndex = 1; // LOOK ??
        count++;

        // double time = ft.getObsTime(cursorIter);
        return new StandardProfileFeature(s, cursorIter);
      }

      public void setBufferSize(int bytes) {
        iter.setBufferSize(bytes);
      }

      public void finish() {
      }
    }
  }

  // one profile
  private class StandardProfileFeature extends ProfileFeatureImpl {
    private Cursor cursor;

    StandardProfileFeature(Station s, Cursor cursor) throws IOException {
      super(ft.getFeatureName(cursor), s.getLatitude(), s.getLongitude(), -1);
      this.cursor = cursor;
      //this.desc = "time=" + time + "stn=" + s.getDescription();
    }

    // iterate over obs in the profile
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      StructureDataIterator structIter = ft.getLeafFeatureDataIterator(cursor, bufferSize);
      StandardPointFeatureIterator iter = new StandardProfileFeatureIterator(ft, timeUnit, structIter, cursor.copy());
      if ((boundingBox == null) || (dateRange == null) || (npts < 0))
        iter.setCalculateBounds(this);
      return iter;
    }
  }

  private class StandardProfileFeatureIterator extends StandardPointFeatureIterator {

    StandardProfileFeatureIterator(NestedTable ft, DateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
      super(ft, timeUnit, structIter, cursor);
    }

    protected boolean filter() throws IOException {
      if (!super.filter()) return false;

      // must also check for missing z values
      return ft.isAltMissing(this.cursor);
    }
  }

}
