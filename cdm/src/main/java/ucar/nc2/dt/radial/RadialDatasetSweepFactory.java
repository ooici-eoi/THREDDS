package ucar.nc2.dt.radial;

import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dataset.*;

/**
 * Factory to create RadialDatasets
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */
public class RadialDatasetSweepFactory {

  private StringBuffer log;
  public String getErrorMessages() { return log == null ? "" : log.toString(); }

  public RadialDatasetSweep open( String location, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException {
    log = new StringBuffer();
    NetcdfDataset ncd = NetcdfDatasetCache.acquire( location, cancelTask);
    return open( ncd);
  }

  public RadialDatasetSweep open( NetcdfDataset ncd) {

    String convention = ncd.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals("_Coordinates")) {
      String format = ncd.findAttValueIgnoreCase(null, "Format", null);
      if (format.equals("Unidata/netCDF/Dorade"))
        return new Dorade2Dataset( ncd);
      if (format.equals("ARCHIVE2")  || format.equals("AR2V0001"))
        return new LevelII2Dataset( ncd);
      if (format.equals("Level3/NIDS") )
        return new Nids2Dataset( ncd);
    }

    return null;
  }

}
