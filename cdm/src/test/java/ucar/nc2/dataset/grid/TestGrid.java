package ucar.nc2.dataset.grid;

import junit.framework.*;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestGrid {
  public static String topDir = "test/data/";

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();

    suite.addTest(new TestSuite(TestGeoGrid.class));

    suite.addTest(new TestSuite(TestGridRanks.class));
    suite.addTest(new TestSuite(TestGridRank2.class));
    suite.addTest(new TestSuite(TestWritePermute.class));
    suite.addTest(new TestSuite(TestReadPermute.class));

    suite.addTest(new TestSuite(TestReadandCount.class));
    suite.addTest(new TestSuite(TestReadandCountGrib.class)); // */
    suite.addTest(new TestSuite(TestReadAndCountDods.class)); // */

    suite.addTest(new TestSuite(TestSubset.class));
    suite.addTest(new TestSuite(TestVerticalTransforms.class));

    return suite;
  }
}