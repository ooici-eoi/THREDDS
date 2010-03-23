/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.fmrc;

import net.jcip.annotations.ThreadSafe;
import org.jdom.Element;
import thredds.inventory.*;
import ucar.nc2.dt.GridDataset;

import java.util.*;
import java.io.IOException;

/**
 * Forecast Model Run Collection, manages dynamic collections of GridDatasets.
 * Fmrc represents a virtual dataset.
 * To instantiate, you obtain an FmrcInv "snapshot" from which you can call getDatatset().
 *
 * Assumes that we dont have multiple runtimes in the same file.
 * Can handle different time steps in different files.
 * Can handle different grids in different files. However this creates problems for the "typical dataset".
 * Cannot handle different ensembles in different files.  (LOOK fix)
 * Cannot handle different levels in different files. ok
 *
 * @author caron
 * @since Jan 11, 2010
 */
@ThreadSafe
public class Fmrc {
  public static final String CAT = "catalog:";

  /**
   * Factory method
   * @param collection collectionSpec or catalog:catalogURL. collectionSpec date extraction is used to get rundates
   * @param errlog  place error messages here
   * @param debug  place debug messages here
   * @return Fmrc or null on error
   * @throws IOException on read error
   */
  public static Fmrc open(String collection, Formatter errlog, Formatter debug) throws IOException {
    if (collection.startsWith(CAT)) {
      String catUrl = collection.substring(CAT.length());
      DatasetCollectionFromCatalog manager = new DatasetCollectionFromCatalog(catUrl, null);
      return new Fmrc(manager, null);

    } else if (collection.endsWith(".ncml")) {
      NcmlCollectionReader ncmlCollection = NcmlCollectionReader.open(collection, errlog);
      if (ncmlCollection == null) return null;
      DateExtractor de = ncmlCollection.getDateExtractor();
      Fmrc fmrc = new Fmrc(ncmlCollection.getDatasetManager(), de);
      fmrc.setNcml(ncmlCollection.getNcmlOuter(), ncmlCollection.getNcmlInner());
      return fmrc;
    }

    return new Fmrc(collection, errlog, debug);
  }

  ////////////////////////////////////////////////////////////////////////
  private CollectionManager manager;
  private DateExtractor dateExtractor;
  private Element ncmlOuter, ncmlInner;

  // the current state - changing must be thread safe
  private FmrcInv fmrc;
  private FmrcDataset fmrcDataset;

  Fmrc(String collectionSpec, Formatter errlog, Formatter debug) {
    CollectionSpecParser sp = new CollectionSpecParser(collectionSpec, errlog);
    manager = new DatasetCollectionManager(sp, errlog);
    
    // optional date extraction is used to get rundates when not embedded in the file
    dateExtractor = (sp.getDateFormatMark() == null) ? new DateExtractorNone() : new DateExtractorFromName(sp.getDateFormatMark(), true);
    if (debug != null) debug.format("Datasets in collection for spec=%s%n", sp);
  }

  public Fmrc(CollectionManager manager, DateExtractor dateExtractor) {
    this.manager = manager;
    this.dateExtractor = dateExtractor;
  }

  public void setNcml(Element ncmlOuter, Element ncmlInner) {
    this.ncmlOuter = ncmlOuter;
    this.ncmlInner = ncmlInner;
  }

  public GridDataset getDataset2D(boolean force) throws IOException {
    FmrcDataset fmrcDataset = getFmrcDataset( force);
    return fmrcDataset.getNetcdfDataset2D( force);
  }

  private FmrcDataset getFmrcDataset(boolean force) throws IOException {
    // LOOK what about testing for if the list hasnt changed ??
    if (force || fmrcDataset == null || manager.timeToRescan())
      fmrcDataset = new FmrcDataset(getFmrcInv( force));
    return fmrcDataset;
  }

  /**
   * Creates an FmrcInv - make a snapshot of the current state of inventory
   * Check to see if a rescan is needed.
   * public for debugging
   * @param force ignore rescan rules
   * @return FmrcInv - current state of inventory
   * @throws java.io.IOException on read error
   */
  public FmrcInv getFmrcInv(boolean force) throws IOException {
    if (force || fmrc == null || manager.timeToRescan())
      fmrc = new FmrcInv(manager.getCollectionName(), scan(null));
    return fmrc;
  }

  private List<FmrInv> scan(Formatter debug) throws IOException {
    if (dateExtractor == null) dateExtractor = new DateExtractorNone();

    Map<Date, FmrInv> fmrMap = new HashMap<Date, FmrInv>(); // all files are grouped by run date in an FmrInv
    List<FmrInv> fmrList = new ArrayList<FmrInv>(); // an fmrc is a collection of fmr

    // get the inventory, sorted by path
    manager.scan(null);
    List<MFile> fileList = manager.getFiles();
    for (MFile f : fileList) {
      GridDatasetInv inv = GridDatasetInv.open(manager, f, dateExtractor); // inventory is discovered for each GDS
      Date runDate = inv.getRunDate();
      if (debug != null) debug.format("  opened %s rundate = %s%n", f.getPath(), inv.getRunDateString());

      // add to fmr for that rundate
      FmrInv fmr = fmrMap.get(runDate);
      if (fmr == null) {
        fmr = new FmrInv(runDate);
        fmrMap.put(runDate, fmr);
        fmrList.add(fmr);
      }
      fmr.addDataset(inv, debug);
    }
    if (debug != null) debug.format("%n");

    // finish the FmrInv
    Collections.sort(fmrList);
    for (FmrInv fmr : fmrList) {
      fmr.finish();
    }

    return fmrList;
  }

  // exposed for debugging
  public CollectionManager getManager() {
    return manager;
  }

  public static void main(String[] args) throws IOException {
    Formatter errlog = new Formatter();
    Formatter debug = new Formatter();

    String spec1 = "/data/testdata/ncml/nc/nam_c20s/NAM_CONUS_20km_surface_#yyyyMMdd_HHmm#.grib1";
    String spec2 = "/data/testdata/grid/grib/grib1/data/agg/.*grb";
    String spec3 = "/data/testdata/ncml/nc/ruc_conus40/RUC_CONUS_40km_#yyyyMMdd_HHmm#.grib1";
    String spec4 = "/data/testdata/cdmUnitTest/rtmodels/.*_nmm\\.GrbF[0-9]{5}$";

    String cat1 = "catalog:http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/RUC2/CONUS_40km/files/catalog.xml";
    String cat2 = "catalog:http://motherlode.ucar.edu:8080/thredds/catalog/fmrc/NCEP/NDFD/CONUS_5km/files/catalog.xml";

    String specH = "C:/data/datasets/nogaps/US058GMET-GR1mdl.*air_temp";
    String specH2 = "C:/data/ft/grid/cg/.*nc$";
    String specH3 = "C:/data/ft/grid/namExtract/#yyyyMMdd_HHmm#.*nc$";
    Fmrc fmrc = new Fmrc(specH3, errlog, debug);
    System.out.printf("errlog = %s%n", errlog);
    System.out.printf("debug = %s%n", debug);
  }
}
