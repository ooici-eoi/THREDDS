// $Id: ForecastModelRunServlet.java 51 2006-07-12 17:13:13Z caron $
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

package thredds.servlet;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import ucar.nc2.dt.grid.ForecastModelRunCollection;
import ucar.nc2.dt.grid.FmrcDefinition;
import ucar.nc2.dt.grid.FmrcReport;
import ucar.nc2.dt.grid.ForecastModelRun;
import ucar.unidata.util.StringUtil;

/**
 * Servlet shows Forecast Model Run Collection Inventory.
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class ForecastModelRunServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 fmrCache = null;
  private boolean debug = false;

  public void init() throws ServletException {
    super.init();

    // remove caching in favor of creating these files when the grib indexer is run, externally as ldm user
    // cache the fmr inventory xml: keep for 10 days, scour once a day */
    //fmrCache = new ucar.nc2.util.DiskCache2(contentPath+"/cache", false, 60 * 24 * 10, 60 * 24);
    //fmrCache.setCachePathPolicy( DiskCache2.CACHEPATH_POLICY_NESTED_TRUNCATE, "grid/");
  }

  public void destroy() {
    if (fmrCache != null)
      fmrCache.exit();
  }

  protected String getPath() { return "modelInventory/"; }
  protected void makeDebugActions() { }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {

    ServletUtil.logServerAccessSetup(req);
    ForecastModelRunCollection fmr;
    String varName;

    String path = req.getPathInfo();
    String query = req.getQueryString();
    if (debug) System.out.println("path="+path+" query="+query);

    String report = req.getParameter("report");
    if (report != null) {
      try {
        report( res, report.equals("missing"));
      } catch (Exception e) {
        e.printStackTrace();
        log.error("report", e);
        ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      return;
    }

    DataRootHandler2 h = DataRootHandler2.getInstance();
    DataRootHandler2.DataRootMatch match = h.findDataRootMatch( req );

    if (match != null) {
      if (debug) System.out.println("match="+match.rootPath+" rem="+match.remaining+" dir="+match.dirLocation);

      varName = match.remaining;
      //fmr = (ForecastModelRunCollection) fmrHash.get( match.dirLocation);
      //if (fmr == null ) {
        try {
          String suffix = req.getParameter("suffix");
          if (suffix == null) suffix = "grib1";

          // LOOK major kludge
          int pos = match.dirLocation.indexOf("grid/");
          String name = match.dirLocation.substring(pos+5);
          name = StringUtil.replace(name, '/', "-");
          if (name.startsWith("-")) name = name.substring(1);
          if (name.endsWith("-")) name = name.substring(0, name.length()-1);

          if (debug) System.out.println("  fmrcDefinitionPath="+contentPath+" name="+name+" dir="+match.dirLocation);
          fmr = ForecastModelRunCollection.make(contentPath, name, fmrCache, match.dirLocation, suffix,
                  ForecastModelRun.OPEN_XML_ONLY);

        } catch (Exception e) {
          e.printStackTrace();
          log.error("ForecastModelRunCollection.make", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, match.dirLocation);
          return;
        }
      //}

      if (fmr == null) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
        res.sendError(HttpServletResponse.SC_NOT_FOUND, match.dirLocation);
        return;
      }

    } else {
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
      return;
    }

    String define = req.getParameter("define");
    if (define != null) {
      showDefinition( res, fmr, define);
      return;
    }

    if (varName.startsWith("/"))
      varName = varName.substring(1);
    if (varName.endsWith("/"))
      varName = varName.substring(0, varName.length()-1);

    String offsetHour = req.getParameter("offsetHour");
    if (offsetHour != null) {
      showOffsetHour( res, fmr, varName, offsetHour);
      return;
    }

    showInventory( res, fmr, varName, query, false);
  }

  private void showOffsetHour(HttpServletResponse res, ForecastModelRunCollection fmrc, String varName, String offsetHour) throws IOException {
    res.setContentType("text/plain; charset=iso-8859-1");
    String contents = fmrc.showOffsetHour(varName, offsetHour);

    OutputStream out = res.getOutputStream();
    out.write(contents.getBytes());
    out.flush();
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, contents.length());
  }

  private void report(HttpServletResponse res, boolean showMissing) throws Exception {
    res.setContentType("text/plain; charset=iso-8859-1");
    OutputStream out = res.getOutputStream();
    PrintStream ps = new PrintStream( out);

    String[] paths = getDatasetPaths();
    for (int i = 0; i < paths.length; i++) {
      String path = "http://motherlode.ucar.edu:8080" + paths[i];

      int pos = path.indexOf("model/");
      String path2 = path.substring(pos+6);
      String name = StringUtil.replace(path2, '/', "-");
      if (name.startsWith("-")) name = name.substring(1);
      if (name.endsWith("-")) name = name.substring(0, name.length()-1);
      String dir = "/data/ldm/pub/native/grid/" + path2;

      //System.out.println("  fmrcDefinitionPath="+contentPath+" name="+name+" dir="+dir);
      ps.println("\n*******Dataset" + dir);
      ForecastModelRunCollection fmrc = ForecastModelRunCollection.make(contentPath, name, fmrCache, dir, ".grib1",
              ForecastModelRun.OPEN_XML_ONLY);
      if (null == fmrc) {
        ps.println("  ERROR - no files were found");
      }  else {
        FmrcReport report = new FmrcReport();
        report.report( fmrc, ps, showMissing);
      }
      ps.flush();
    }

    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1);
  }

  private void showDefinition(HttpServletResponse res, ForecastModelRunCollection fmrc, String define) throws IOException {
    res.setContentType("text/xml; charset=iso-8859-1");
    FmrcDefinition def = fmrc.getDefinition();

    if (define.equals("write")) {
      FileOutputStream fos = new FileOutputStream( fmrc.getDefinitionPath());
      def = new FmrcDefinition();
      def.makeFromCollectionInventory(fmrc);
      def.writeDefinitionXML( fos);
      System.out.println(" write to "+fmrc.getDefinitionPath());

    } else if ((def != null) && (define.equals("addVert"))) {
      FileOutputStream fos = new FileOutputStream( fmrc.getDefinitionPath());
      def.addVertCoordsFromCollectionInventory(fmrc);
      def.writeDefinitionXML( fos);
      System.out.println(" write to "+fmrc.getDefinitionPath());
    }

    if (def == null) {
      def = new FmrcDefinition();
      def.makeFromCollectionInventory(fmrc);
    }

    String xmlString = def.writeDefinitionXML();
    OutputStream out = res.getOutputStream();
    out.write(xmlString.getBytes());
    out.flush();
    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, xmlString.length());
  }

  private void showInventory(HttpServletResponse res, ForecastModelRunCollection fmr, String varName, String type, boolean wantXml) throws IOException {

    String infoString;

    if (varName.length() == 0)
      varName = null;

    boolean matrix = (type != null) && (type.equalsIgnoreCase("Matrix"));

    if (wantXml) {
      infoString = fmr.writeMatrixXML( varName);

    } else {
      Document doc;
      InputStream xslt;
      if (varName == null) {
        xslt = matrix ? getXSLT("fmrMatrix.xsl") : getXSLT("fmrOffset.xsl");
        doc = fmr.makeMatrixDocument();
      } else {
        xslt = matrix ? getXSLT("fmrMatrixVariable.xsl") : getXSLT("fmrOffsetVariable.xsl");
        doc = fmr.makeMatrixDocument( varName);
      }

      try {
        XSLTransformer transformer = new XSLTransformer(xslt);
        Document html = transformer.transform(doc);
        XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
        infoString = fmt.outputString(html);

      } catch (Exception e) {
        log.error("ForecastModelRunServlet internal error", e);
        ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ForecastModelRunServlet internal error");
        return;
      }
    }

    res.setContentLength(infoString.length());
    if (wantXml)
      res.setContentType("text/xml; charset=iso-8859-1");
    else
      res.setContentType("text/html; charset=iso-8859-1");

    OutputStream out = res.getOutputStream();
    out.write(infoString.getBytes());
    out.flush();

    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, infoString.length());
  }

  static private InputStream getXSLT(String xslName) {
    Class c = ForecastModelRunServlet.class;
    return c.getResourceAsStream("/resources/thredds/xsl/" + xslName);
  }

  private String[] getDatasetPaths() {
    String[] all = {
      "/thredds/modelInventory/idd/model/NCEP/DGEX/CONUS_12km/",
      "/thredds/modelInventory/idd/model/NCEP/DGEX/Alaska_12km/",

      "/thredds/modelInventory/idd/model/NCEP/GFS/Alaska_191km/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/CONUS_80km/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/CONUS_95km/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/CONUS_191km/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/Global_0p5deg/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/Global_onedeg/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/Global_2p5deg/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/Hawaii_160km/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/N_Hemisphere_381km/",
      "/thredds/modelInventory/idd/model/NCEP/GFS/Puerto_Rico_191km/",

      "/thredds/modelInventory/idd/model/NCEP/NAM/Alaska_11km/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/Alaska_22km/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/Alaska_45km/noaaport/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/Alaska_45km/conduit/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/Alaska_95km/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_12km/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_20km/surface/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_20km/selectsurface/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_20km/noaaport/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_40km/noaaport/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_40km/conduit/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/CONUS_80km/",
      "/thredds/modelInventory/idd/model/NCEP/NAM/Polar_90km/",

      "/thredds/modelInventory/idd/model/NCEP/RUC2/CONUS_20km/surface/",
      "/thredds/modelInventory/idd/model/NCEP/RUC2/CONUS_20km/pressure/",
      "/thredds/modelInventory/idd/model/NCEP/RUC2/CONUS_20km/hybrid/",
      "/thredds/modelInventory/idd/model/NCEP/RUC/CONUS_40km/",
      "/thredds/modelInventory/idd/model/NCEP/RUC/CONUS_80km/",

      "/thredds/modelInventory/idd/model/NCEP/NDFD/CONUS_5km/",
    };

    return all;
  }

}
