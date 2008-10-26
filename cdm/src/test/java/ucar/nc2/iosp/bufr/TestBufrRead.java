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

package ucar.nc2.iosp.bufr;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.unidata.io.RandomAccessFile;

import java.io.*;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 * @since Apr 1, 2008
 */
public class TestBufrRead extends TestCase {

  public void testReadAll() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = 0;
    count += TestAll.readAllDir("C:/data/bufr2/mlode/", new MyFileFilter());
    System.out.println("***READ " + count + " files");
  }

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return !pathname.getName().endsWith(".bfx");
    }
  }

  public void testReadOneBufrMessage() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = 0;
    count += TestAll.actOnAll("R:/testdata/bufr/edition3/", new MyFileFilter(), new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        return readBufr(filename);
      }
    });
    System.out.println("***READ " + count + " files");
  }

  public void testScanOne() throws IOException {
    openNetdf("R:/testdata/bufr/edition3/idd/profiler/PROFILER_1.bufr");
  }

  private int readBufr(String filename) throws IOException {
    boolean oneRecord = true;
    boolean getData = false;

    // Reading of Bufr files must be inside a try-catch block
    RandomAccessFile raf = new RandomAccessFile(filename, "r");
    MessageScanner scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      int totalObs = m.getNumberDatasets();
      System.out.println("Total number observations =" + totalObs);
      break;
    }
    return 1;
  }

  private void openNetdf(String filename) throws IOException {
    NetcdfFile.open(filename);
  }

}
