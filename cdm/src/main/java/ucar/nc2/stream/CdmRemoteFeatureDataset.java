/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.stream;

import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointDatasetRemote;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;

/**
 * Describe
 *
 * @author caron
 * @since May 19, 2009
 */
public class CdmRemoteFeatureDataset {
  static private boolean showXML = true;

  static public FeatureDataset factory(FeatureType wantFeatureType, String endpoint) throws IOException {
    if (endpoint.startsWith(CdmRemote.SCHEME))
      endpoint = endpoint.substring(CdmRemote.SCHEME.length());

    Document doc = getCapabilities(endpoint);
    Element root = doc.getRootElement();
    Element elem = root.getChild("featureDataset");
    String fType = elem.getAttribute("type").getValue();
    String datasetUri = elem.getAttribute("url").getValue();

    System.out.printf("CdmRemoteDatasetFactory endpoint %s%n ftype= %s uri=%s%n", endpoint, fType, datasetUri);

    FeatureType ft = FeatureType.getType(fType);
    CdmRemote ncremote = new CdmRemote(datasetUri, null);
    NetcdfDataset ncd = new NetcdfDataset(ncremote, null);

    if (ft == null || ft == FeatureType.GRID) {
      return new GridDataset(ncd);
    } else {
      return new PointDatasetRemote(ft, ncd, ncremote);
    }
  }

  static private org.jdom.Document getCapabilities(String endpoint) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(false);
      doc = builder.build(endpoint+"?req=capabilities"); // LOOK - not using httpclient
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }

    if (showXML) {
      System.out.printf("*** endpoint = %s %n", endpoint);
      XMLOutputter xmlOut = new XMLOutputter();
      System.out.printf("*** NetcdfDataset/showParsedXML = %n %s %n", xmlOut.outputString(doc));
    }

    return doc;
  }

  public static void main(String args[]) throws IOException {
    String endpoint = "http://localhost:8080/thredds/cdmremote/idd/metar/gempakLocalHome";
    FeatureDatasetPoint fd = (FeatureDatasetPoint) CdmRemoteFeatureDataset.factory(FeatureType.ANY, endpoint);
    FeatureCollection fc = fd.getPointFeatureCollectionList().get(0);
    System.out.printf("Result= %s %n %s %n", fd, fc);

    /* StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) fc;
    PointFeatureIterator pfIter = sfc.get(-1);
    try {
      while (pfIter.hasNext()) {
        PointFeature pf = pfIter.next();
        System.out.println("pf= " + pf);
      }
    } finally {
      pfIter.finish();
    } */
  }


}
