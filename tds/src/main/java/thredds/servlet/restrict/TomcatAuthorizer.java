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

package thredds.servlet.restrict;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;

import thredds.servlet.ServletUtil;
import thredds.servlet.AccessLog;

/**
 * Use Tomcat security.
 *
 * @author caron
 */
public class TomcatAuthorizer implements Authorizer {

  private boolean useSSL = false;
  private String sslPort = "8443";

  private boolean debugResourceControl = false;

  public boolean authorize(HttpServletRequest req, HttpServletResponse res, String role) throws IOException {
    if (req.isUserInRole(role))
      return true;
    
    // redirect for authentication / authorization
    HttpSession session = req.getSession();
    session.setAttribute("origRequest", ServletUtil.getRequest(req));
    session.setAttribute("role", role);

    String urlr = useSSL ? "https://" + req.getServerName() + ":"+ sslPort + ServletUtil.getContextPath()+"/restrictedAccess/" + role :
                           "http://" + req.getServerName() + ":"+ req.getServerPort() +ServletUtil.getContextPath()+"/restrictedAccess/" + role;


    if (debugResourceControl) System.out.println("redirect to = " + urlr);
    res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    res.addHeader("Location", urlr);
    res.setHeader("Last-Modified", "");
    return false;
  }

  public TomcatAuthorizer() {}
  public void init(HttpServlet servlet) throws ServletException {
    String s = servlet.getInitParameter("useSSL");
    if (null != s)
      useSSL = Boolean.valueOf(s);
    
    s = servlet.getInitParameter("portSSL");
    if (null != s)
      sslPort = s;
  }
  public void setRoleSource(RoleSource db) {
    // not used
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    AccessLog.log.info( AccessLog.setupInfo(req));

    HttpSession session = req.getSession();
    if (session != null) {
      String origURI = (String) session.getAttribute("origRequest");
      String role = (String) session.getAttribute("role");

      if (req.isUserInRole(role)) {

        if (origURI != null) {
          if (debugResourceControl) System.out.println("redirect to origRequest = "+origURI);
          res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
          String frag = (origURI.indexOf("?") > 0) ? "&auth" : "?auth";
          res.addHeader("Location", origURI+frag);
          return;

        } else {
          res.setStatus(HttpServletResponse.SC_OK); // someone came directly to this page
          return;
        }
      }
    }

    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authorized to access this dataset.");
  }

}
