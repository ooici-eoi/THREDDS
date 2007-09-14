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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.ncml4.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.IOException;

/**
 * ATD Radar file (ad hoc guesses).
 *
 * @author caron
 */

public class ATDRadarConvention extends CoordSysBuilder {

  /**
   * @param ncfile test this NetcdfFile
   * @return true if we think this is a ATDRadarConvention file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    // not really sure until we can examine more files
    String s = ncfile.findAttValueIgnoreCase(null, "sensor_name", "none");
    return s.equalsIgnoreCase("CRAFT/NEXRAD");
  }

  public ATDRadarConvention() {
    this.conventionName = "ATDRadar";
  }

  public void augmentDataset(NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
    NcMLReader.wrapNcMLresource(ncDataset, CoordSysBuilder.resourcesDir + "ATDRadar.ncml", cancelTask);
  }

}