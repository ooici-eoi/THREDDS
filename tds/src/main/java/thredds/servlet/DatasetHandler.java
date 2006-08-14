package thredds.servlet;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileFactory;
import ucar.nc2.NetcdfFileCache;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetFmrc;


/**
 * CDM Datasets.
 *   1) if dataset with ncml, open that
 *   2) if datasetScan with ncml, wrap
 */
public class DatasetHandler {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatasetHandler.class);
  static HashMap ncmlDatasetHash = new HashMap();

  static public void reinit() {
    ncmlDatasetHash = new HashMap();
  }

  public static void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("catalogs");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showNcml", "Show ncml datasets") {
      public void doAction(DebugHandler.Event e) {
        Iterator iter = ncmlDatasetHash.keySet().iterator();
        while (iter.hasNext()) {
          String key = (String) iter.next();
          e.pw.println(" url=" + key);
        }
      }
    };
    debugHandler.addAction( act);
  }

  static public NetcdfFile getNetcdfFile( String  reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants "+reqPath);

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // look for a dataset that has an ncml element
    InvDatasetImpl ds = (InvDatasetImpl) ncmlDatasetHash.get(reqPath);
    if (ds != null) {
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found NcmlDataset= "+ds);
      return getNcmlDataset( ds);
    }

    // look for an fmrc dataset
    DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch( reqPath);
    if ((match != null) && (match.dataRoot.fmrc != null)) {
      InvDatasetFmrc fmrc = match.dataRoot.fmrc;
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found InvDatasetFmrc= "+fmrc);
      return fmrc.getDataset( match.remaining);
    }

    // otherwise, must have a datasetRoot in the path
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile( reqPath);
    if (file == null)
      return null;

    // acquire it
    NetcdfFile ncfile = NetcdfDataset.acquireFile(file.getPath(), null);

    // wrap with ncml if needed
    org.jdom.Element netcdfElem = DataRootHandler.getInstance().getNcML( reqPath);
    if (netcdfElem != null) {
      NetcdfDataset ncd = new NetcdfDataset( ncfile, false); // do not enhance !!
      new NcMLReader().readNetcdf( reqPath, ncd, ncd, netcdfElem, null);
      if (log.isDebugEnabled()) log.debug("  -- DatasetHandler found DataRoot NcML = "+ds);
      return ncd;
    }

    return ncfile;
  }

  static public GridDataset openGridDataset(String reqPath) throws IOException {

    // fetch it as a NetcdfFile; this deals with possible NcML
    NetcdfFile ncfile = getNetcdfFile(reqPath);
    if (ncfile == null) throw new FileNotFoundException(reqPath);

    // convert to NetcdfDataset with enhance
    NetcdfDataset ncd;
    if (ncfile instanceof NetcdfDataset) {
      ncd = (NetcdfDataset) ncfile;
      ncd.enhance();
    } else {
      ncd = new NetcdfDataset( ncfile, true);
    }

    // convert to a GridDataset
    return new ucar.nc2.dataset.grid.GridDataset( ncd);
  }

  /**
   * This tracks Dataset elements that have embedded NcML
   * @param path the req.getPathInfo() of the dataset.
   * @param ds the dataset
   */
  static void putNcmlDataset( String path, InvDatasetImpl ds) {
    if (log.isDebugEnabled()) log.debug("putNcmlDataset "+path+" for "+ds.getName());
    ncmlDatasetHash.put( path, ds);
  }

  static private NetcdfFile getNcmlDataset( InvDatasetImpl ds) throws IOException {
    String cacheName = ds.getUniqueID();
    return NetcdfFileCache.acquire(cacheName, -1, null, null, new NcmlFileFactory(ds));
  }

  static private NetcdfFile getFmrcDataset( InvDatasetImpl ds) throws IOException {
    String cacheName = ds.getUniqueID();
    return NetcdfFileCache.acquire(cacheName, -1, null, null, new NcmlFileFactory(ds));
  }

  static private class NcmlFileFactory implements NetcdfFileFactory {
    private InvDatasetImpl ds;
    NcmlFileFactory( InvDatasetImpl ds) { this.ds = ds; }

    public NetcdfFile open(String cacheName, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      /* File ncmlFile = DiskCache.getCacheFile(cacheName);

      if (ncmlFile.exists()) {
        log.debug("ncmlFile.exists() file= "+ncmlFile.getPath()+" lastModified= "+new Date(ncmlFile.lastModified()));
        return NetcdfDataset.openDataset( ncmlFile.getPath());
      }  */

      // otherwise, open and write it out
      NetcdfDataset ncd = new NetcdfDataset();
      ncd.setCacheName(cacheName);
      org.jdom.Element netcdfElem = ds.getNcmlElement();

      // transfer the ncml into the dataset
      new NcMLReader().readNetcdf( null, ncd, ncd, netcdfElem, null);

      /* cache the full NcML - this has to read the datasets, so may be slow
      OutputStream out = new BufferedOutputStream( new FileOutputStream( ncmlFile));
      ncd.writeNcML(out, null);
      out.close();

      System.out.println("new ncmlFile file= "+ncmlFile.getPath()+" lastModified= "+new Date(ncmlFile.lastModified())); */

      return ncd;
    }
  }


}
