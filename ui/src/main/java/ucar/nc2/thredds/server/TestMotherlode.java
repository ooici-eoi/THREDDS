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

package ucar.nc2.thredds.server;

import ucar.nc2.util.IO;

import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestMotherlode {

  private static String server = "http://motherlode.ucar.edu:8080/thredds";

  static void ping(String url) throws IOException {
    String contents = IO.readURLcontentsWithException(server+url);
    System.out.println("Conyents of "+url);
    System.out.println(contents);
    System.out.println("==================");
  }

  public static void main(String args[]) throws Exception {
    args = new String[1];
    args[0] = server;

    ping("/ncss/metars/dataset.html");
    ping("/ncss/metars/dataset.xml");
    ping("/modelInventory/fmrc/NCEP/NAM/CONUS_80km/");
    ping("/ncss/grid/fmrc/NCEP/NAM/CONUS_80km/NCEP-NAM-CONUS_80km_best.ncd/dataset.html");
    ping("/ncss/grid/fmrc/NCEP/NAM/CONUS_80km/NCEP-NAM-CONUS_80km_best.ncd/dataset.xml");

    TestMotherlodeModels.main(args);
    TestIDVdatasets.main(args);
  }

}
