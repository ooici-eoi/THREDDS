package ucar.nc2.ncml4;

import junit.framework.*;

import ucar.nc2.*;
import ucar.nc2.dt.grid.GridDataset;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcmlReadGridDataset extends TestCase {

  public TestNcmlReadGridDataset( String name) {
    super(name);
  }

  GridDataset gds = null;
  String location = "file:"+TestAll.upcShareTestDataDir + "grid/netcdf/cf/bora_test_agg.ncml";

  public void setUp() {
    try {
      gds = GridDataset.open(location);
      //System.out.println("ncfile opened = "+location);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }
  }

  protected void tearDown() throws IOException {
    gds.close();
  }

  public void testStructure() {
    System.out.println("gds opened = "+location+"\n"+ gds);
  }
}
