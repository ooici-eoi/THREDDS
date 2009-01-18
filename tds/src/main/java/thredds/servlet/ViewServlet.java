/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

import ucar.unidata.util.StringUtil;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.IO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvAccess;
import thredds.catalog.ServiceType;

/**
 * Catalog Serving
 *
 * handles /view/*
 */
public class ViewServlet extends AbstractServlet {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewServlet.class);
  static private HashMap<String,String> templates = new HashMap<String,String>();
  static private ArrayList<Viewer> viewerList;
  static {
    viewerList = new ArrayList<Viewer>();
    registerViewer( new IDV());
    registerViewer( new Nj22ToolsUI());
    registerViewer( new StaticView());
  }

 static public void registerViewer( String className) {
   Class vClass;
   try {
     vClass = ViewServlet.class.getClassLoader().loadClass(className);
   } catch (ClassNotFoundException e) {
     log.error("Attempt to load Viewer class "+className+" not found");
     return;
   }

   if (!(Viewer.class.isAssignableFrom( vClass))) {
     log.error("Attempt to load class "+className+" does not implement "+Viewer.class.getName());
     return;
   }

   // create instance of the class
   Object instance;
   try {
     instance = vClass.newInstance();
   } catch (InstantiationException e) {
     log.error("Attempt to load Viewer class "+className+" cannot instantiate, probably need default Constructor.");
     return;
   } catch (IllegalAccessException e) {
     log.error("Attempt to load Viewer class "+className+" is not accessible.");
     return;
   }

    registerViewer( (Viewer) instance);
  }

  static public void registerViewer(Viewer v) {
    viewerList.add( v);
  }

  static private String getTemplate( String path) {
    String template = templates.get( path);
    if (template != null) return template;

    try {
      template = IO.readFile(path);
    } catch (IOException ioe) {
      return null;
    }

    templates.put( path, template);
    return template;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    log.info( UsageLog.setupInfo( req ));

    String path = req.getPathInfo();
    int pos = path.lastIndexOf("/");
    String filename = "views/" + path.substring(pos + 1);
    log.debug("**ViewManager req= "+path+" look for "+ServletUtil.getRootPath() + filename);

    String template = getTemplate( ServletUtil.getRootPath() + filename);
    if (template == null)
      template = getTemplate( contentPath + filename);
    if (template == null) {
      log.info( UsageLog.accessInfo( HttpServletResponse.SC_NOT_FOUND, 0 ));
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    StringBuffer sbuff = new StringBuffer( template);

    Enumeration params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        String sname = "{"+name+"}";
        for (String value : values) {
          StringUtil.substitute(sbuff, sname, value); // multiple ok
        }
      }
    }

    try {
      res.setContentType("application/x-java-jnlp-file");
      ServletUtil.returnString(sbuff.toString(), res);
      // System.out.println(" jnlp="+sbuff.toString());

    } catch (Throwable t) {
      log.error(" jnlp="+sbuff.toString(), t);
      log.info( UsageLog.accessInfo( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 ));
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  static public void showViewers( StringBuilder sbuff, InvDatasetImpl dataset, HttpServletRequest req) {
    int count = 0;
    for (Viewer viewer : viewerList) {
      if (viewer.isViewable(dataset)) count++;
    }
    if (count == 0) return;

    sbuff.append("<h3>Viewers:</h3><ul>\r\n");
    for (Viewer viewer : viewerList) {
      if (viewer.isViewable(dataset)) {
        sbuff.append("  <li> ");
        sbuff.append(viewer.getViewerLinkHtml(dataset, req));
        sbuff.append("</li>\n");
      }
    }
    sbuff.append("</ul>\r\n");
  }

  /* static private void showViews( StringBuffer sbuff, InvDatasetImpl dataset, String viewer) {
    List list = View.getViews(); // findViews( dataset.getParentCatalog().getUriString(), dataset.getID(), viewer);
    if (list.size() == 0) return;
    //sbuff.append("<h4>Contributed Views:</h4>\n<ol>\n");
    sbuff.append("<ul>\n");
    for (int i=0; i<list.size(); i++) {
      View v = (View) list.get(i);
      v.writeHtml( sbuff);
    }
    sbuff.append("\n</ul><p>\n");
  } */

  // must end with "/"
  protected String getPath() { return "view/";  }
  protected void makeDebugActions() { }

  private static class Nj22ToolsUI implements Viewer {

    public  boolean isViewable( InvDatasetImpl ds) {
      String id = ds.getID();
      return ((id != null) && ds.hasAccess());
    }

    public String  getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req) {
      return "<a href='" + req.getContextPath() + "/view/nj22UI.jnlp?" + ds.getSubsetUrl()+"'>NetCDF-Java Tools (webstart)</a>";
    }
  }

  private static class IDV implements Viewer {

    public boolean isViewable( InvDatasetImpl ds) {
      InvAccess access = ds.getAccess(ServiceType.DODS);
      if (access == null) access = ds.getAccess(ServiceType.OPENDAP);
      if (access == null) return false;

      FeatureType dt = ds.getDataType();
      if (dt != FeatureType.GRID) return false;
      return true;
    }

    public String getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req) {
      InvAccess access = ds.getAccess(ServiceType.DODS);
      if (access == null) access = ds.getAccess(ServiceType.OPENDAP);

      URI dataURI = access.getStandardUri();
      if (!dataURI.isAbsolute()) {
        try {
          URI base = new URI( req.getRequestURL().toString());
          dataURI = base.resolve( dataURI);
          // System.out.println("Resolve URL with "+req.getRequestURL()+" got= "+dataURI.toString());
        } catch (URISyntaxException e) {
          log.error("Resolve URL with "+req.getRequestURL(),e);
        }
      }

      return "<a href='" + req.getContextPath() + "/view/idv.jnlp?url="+dataURI.toString()+"'>Integrated Data Viewer (IDV) (webstart)</a>";
    }

  }

  private static class StaticView implements Viewer {

    public  boolean isViewable( InvDatasetImpl ds) {
      return null != ds.findProperty("viewer");
    }

    public String  getViewerLinkHtml( InvDatasetImpl ds, HttpServletRequest req) {
      String viewer = ds.findProperty("viewer");
      String[] parts = viewer.split(",");
      String link = StringUtil.quoteHtmlContent( sub(parts[0], ds));
      return "<a href='"+link+"'>"+parts[1]+"</a>";
    }

    public String sub(String org, InvDatasetImpl ds) {
      List<InvAccess> access = ds.getAccess();
      if (access.size() == 0) return org;

      // look through all access for {serviceName}
      for (InvAccess acc : access) {
        String sname = "{"+acc.getService().getServiceType()+"}";
        if (org.indexOf(sname) >= 0) {
          return StringUtil.substitute(org, sname, acc.getStandardUri().toString());
        }
      }

      String sname = "{url}";
      if (org.indexOf(sname) >= 0) {
        InvAccess acc = access.get(0);
        return StringUtil.substitute(org, sname, acc.getStandardUri().toString());
      }

      return org;
    }
  }

}
