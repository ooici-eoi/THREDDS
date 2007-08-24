package thredds.server.wcs;

import thredds.wcs.WcsDataset;
import thredds.servlet.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.util.DiskCache2;

/**
 * Servlet handles serving data via WCS 1.0.
 *
 */
public class WCSServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 diskCache = null;
  private boolean allow = false, deleteImmediately = true;
  private long maxFileDownloadSize;

  private List<VersionHandler> versionHandlers;

  // must end with "/"
  protected String getPath() { return "wcs/"; }
  protected void makeDebugActions() {}

  public void init() throws ServletException
  {
    super.init();

    allow = ThreddsConfig.getBoolean("WCS.allow", false);
    maxFileDownloadSize = ThreddsConfig.getBytes("WCS.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("WCS.dir", contentPath + "/wcache");
    File cacheDir = new File(cache);
    cacheDir.mkdirs();

    int scourSecs = ThreddsConfig.getSeconds("WCS.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("WCS.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);
    WcsDataset.setDiskCache(diskCache);

    versionHandlers = new ArrayList<VersionHandler>(2);
    versionHandlers.add( new WCS_1_0());
    versionHandlers.add( new WCS_1_1_0());
  }

  public void destroy()
  {
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException
  {
    if (!allow) {
      // ToDo - Server not configured to support WCS. Should response code be 404 (Not Found) instead of 403 (Forbidden)?
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      ServletUtil.logServerAccess( HttpServletResponse.SC_FORBIDDEN, -1 );
      return;
    }

    ServletUtil.logServerAccessSetup( req );

    if ( req.getParameterMap().size() == 0 )
    {
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "GET request not a WCS KVP request." );
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
      return;
    }
    
    String service = ServletUtil.getParameterIgnoreCase( req, "Service");
    if ( service == null || ! service.equals( "WCS"))
    {
      res.sendError( HttpServletResponse.SC_BAD_REQUEST, "GET WCS KVP request missing SERVICE parameter.");
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
      return;
    }

    // Decide on requested version.
    String acceptableVersionsString = ServletUtil.getParameterIgnoreCase( req, "AcceptVersions");
    String reqVersionString = ServletUtil.getParameterIgnoreCase( req, "Version");
    VersionHandler targetHandler = null;

    // If "Version" given, use it or fail if invalid.
    // Otherwise, step through list in "AcceptVersions" and use first one that matches.
    if ( reqVersionString != null)
    {
      Version reqVersion = null;
      try
      {
        reqVersion = new Version( reqVersionString );
      }
      catch ( IllegalArgumentException e )
      {
        // ToDo return exception XML for invalid Version attribute value.
        res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Bad value for \"Version\" attribute <" + reqVersionString +">." );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
        return;
      }
      int loc = versionHandlers.indexOf( reqVersion);
      if ( loc != -1 )
      {
        targetHandler = versionHandlers.get( loc);
      }
      else
      {
        VersionHandler lowestVerHandler = versionHandlers.get( 0 );
        VersionHandler highestVerHandler = versionHandlers.get( versionHandlers.size() - 1 );
        if ( reqVersion.lessThan( lowestVerHandler.getVersion()))
          targetHandler = lowestVerHandler;
        else if ( reqVersion.greaterThan( highestVerHandler.getVersion()))
          targetHandler = highestVerHandler;
        else
        {
          // ToDo figure this out! The version is in between available versions.
          res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Bad value for \"Version\" attribute <" + reqVersionString + ">." );
          ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1 );
          return;
        }
      }
    }

    targetHandler.handleKVP( this, req, res);
  }
}
