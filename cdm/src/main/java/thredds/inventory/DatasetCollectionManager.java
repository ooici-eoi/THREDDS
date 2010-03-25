/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.inventory;

import net.jcip.annotations.ThreadSafe;
import ucar.nc2.units.TimeUnit;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.IOException;

import thredds.inventory.filter.*;
import thredds.inventory.bdb.MetadataManager;

/**
 * Manage one or more Directory Scanners that find MFiles
 * Tracks when they need to be rescanned.
 * Used in:
 * <ul>
 *  <li> replaces older version in ncml.Aggregation
 *  <li> ucar.nc2.ft.point.collection.TimedCollectionImpl
 * </ul>
 *
 * looks like we need to be thread safe, for InvDatasetFeatureCollection
 *
 * @author caron
 * @since Jul 8, 2009
 */
@ThreadSafe
public class DatasetCollectionManager implements CollectionManager {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetCollectionManager.class);
  static private final boolean debugSyncDetail = false;
  static private MController controller;

  /**
   * Set the MController used by scan. Defaults to thredds.filesystem.ControllerOS() if not set.
   * @param _controller use this MController
   */
  static public void setController(MController _controller) {
    controller = _controller;
  }

  ////////////////////////////////////////////////////////////////////

  protected String collectionName;
  private final List<MCollection> scanList = new ArrayList<MCollection>();
  protected TimeUnit recheck; // how often to recheck LOOK what should default be ?

  private Map<String, MFile> map; // current map of MFile
  private long lastScanned; // last time scanned

  protected DatasetCollectionManager() {}

  public DatasetCollectionManager(String collectionSpec, Formatter errlog) {
    this.collectionName = collectionSpec; // StringUtil.escape(collectionSpec, "");
    CollectionSpecParser sp = new CollectionSpecParser(collectionSpec, errlog);

    MFileFilter mfilter = (null == sp.getFilter()) ? null : new WildcardMatchOnName(sp.getFilter());
    DateExtractor dateExtractor = (sp.getDateFormatMark() == null) ? null : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getTopDir(), sp.getTopDir(), sp.wantSubdirs(), mfilter, dateExtractor, null));
  }

  public DatasetCollectionManager(CollectionSpecParser sp, Formatter errlog) {
    this.collectionName = sp.getSpec();
    MFileFilter mfilter = (null == sp.getFilter()) ? null : new WildcardMatchOnName(sp.getFilter());
    DateExtractor dateExtractor = (sp.getDateFormatMark() == null) ? null : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getTopDir(), sp.getTopDir(), sp.wantSubdirs(), mfilter, dateExtractor, null));
  }

  public DatasetCollectionManager(CollectionSpecParser sp, String olderThanFilter, Formatter errlog) {
    this.collectionName = sp.getSpec();

    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != sp.getFilter())
      filters.add( new WildcardMatchOnName(sp.getFilter()));

    if (olderThanFilter != null) {
      try {
        TimeUnit tu = new TimeUnit(olderThanFilter);
        filters.add( new LastModifiedLimit((long) (1000 * tu.getValueInSeconds())));
      } catch (Exception e) {
        logger.error("Invalid time unit for olderThan = {}", olderThanFilter);
      }
    }

    MFileFilter mfilter = (filters.size() == 0) ? null : ((filters.size() == 1) ? filters.get(0) : new Composite(filters));
    DateExtractor dateExtractor = (sp.getDateFormatMark() == null) ? null : new DateExtractorFromName(sp.getDateFormatMark(), true);
    scanList.add(new MCollection(sp.getTopDir(), sp.getTopDir(), sp.wantSubdirs(), mfilter, dateExtractor, null));
  }

  /**
   * Must also call addDirectoryScan one or more times
   * @param recheckS a undunit time unit, specifying how often to rscan
   */
  public DatasetCollectionManager(String recheckS) {
    setRecheck(recheckS);
  }

  public void setRecheck(String recheckS) {
    if (recheckS != null) {
      try {
        this.recheck = new TimeUnit(recheckS);
      } catch (Exception e) {
        logger.error("Invalid time unit for recheckEvery = {}", recheckS);
      }
    }
  }

  /**
   * Add a directory scan to the collection
   * @param dirName scan this directory
   * @param suffix  require this suffix (overriddden by regexp), may be null
   * @param regexpPatternString if present, use this reqular expression to filter files , may be null
   * @param subdirsS if "true", descend into subdirectories, may be null
   * @param olderS udunit time unit - files must be older than this amount of time (now - lastModified > olderTime), may be null
   * @param dateFormatString dateFormatMark string, may be null
   * @param auxInfo attach this object to any MFile found by this scan
   */
  public void addDirectoryScan(String dirName, String suffix, String regexpPatternString, String subdirsS, String olderS,
                               String dateFormatString, Object auxInfo) {
    List<MFileFilter> filters = new ArrayList<MFileFilter>(3);
    if (null != regexpPatternString)
      filters.add(new RegExpMatchOnName(regexpPatternString));
    else if (suffix != null)
      filters.add(new WildcardMatchOnPath("*" + suffix));

    if (olderS != null) {
      try {
        TimeUnit tu = new TimeUnit(olderS);
        filters.add(new LastModifiedLimit((long) (1000 * tu.getValueInSeconds())));
      } catch (Exception e) {
        logger.error("Invalid time unit for olderThan = {}", olderS);
      }
    }

    DateExtractor dateExtractor = (dateFormatString == null) ? null : new DateExtractorFromName(dateFormatString, true);

    boolean wantSubdirs = true;
    if ((subdirsS != null) && subdirsS.equalsIgnoreCase("false"))
      wantSubdirs = false;

    MFileFilter filter = (filters.size() == 0) ? null : ((filters.size() == 1) ? filters.get(0) : new Composite(filters));
    MCollection mc = new thredds.inventory.MCollection(dirName, dirName, wantSubdirs, filter, dateExtractor, auxInfo);

    // create name
    StringBuilder sb = new StringBuilder(dirName);
    if (wantSubdirs)
      sb.append("**/");
    if (null != regexpPatternString)
      sb.append(regexpPatternString);
    else if (suffix != null)
      sb.append(suffix);
    else
      sb.append("noFilter");

    collectionName = sb.toString();
    scanList.add(mc);
  }

  @Override
  public String getCollectionName() {
    return "fmrc:" + collectionName;
  }

  /**
   * Scan the directory(ies) and create MFile objects.
   * Get the results from getFiles()
   *
   * @param cancelTask allow user to cancel
   * @throws java.io.IOException if io error
   */
  public void scan(CancelTask cancelTask) throws IOException {
    Map<String, MFile> newMap = new HashMap<String, MFile>();
    scan(newMap, cancelTask);
    synchronized(this) {
      map = newMap;
      this.lastScanned = System.currentTimeMillis();
    }
  }

  /**
   * Compute if rescan is needed.
   * True if scanList not empty, recheckEvery not null, and recheckEvery time has passed since last scanned.
   *
   * @return true is rescan time has passed
   */
  public boolean timeToRescan() {
    if (scanList.isEmpty()) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, no scanners");
      return false;
    }

    // see if we need to recheck
    if (recheck == null) {
      if (debugSyncDetail) System.out.println(" *Sync not needed, recheck is null");
      return false;
    }

    Date now = new Date();
    Date lastCheckedDate = new Date(lastScanned);
    Date need = recheck.add(lastCheckedDate);
    if (now.before(need)) {
      if (logger.isDebugEnabled()) logger.debug(" *Sync not needed, last= " + lastCheckedDate + " now = " + now);
      return false;
    }

    return true;
  }

  /**
   * Rescan directories. Files may be deleted or added.
   * If the MFile already exists in the current list, leave it in the list.
   * If returns true, get the results from getFiles(), otherwise nothing has changed.
   *
   * @return true if anything actually changed.
   * @throws IOException on I/O error
   */
  public boolean rescan() throws IOException {
    if (logger.isDebugEnabled()) logger.debug(" *Sync at " + new Date());

    // rescan
    Map<String, MFile> oldMap = map;
    Map<String, MFile> newMap = new HashMap<String, MFile>();
    scan(newMap, null);

    // replace with previous datasets if they exist
    boolean changed = false;
    for (MFile newDataset : newMap.values()) {
      String path = newDataset.getPath();
      MFile oldDataset = oldMap.get(path);
      if (oldDataset != null) {
        newMap.put(path, oldDataset);
        if (debugSyncDetail) System.out.println("  sync using old Dataset= " + path);
      } else {
        changed = true;
        if (debugSyncDetail) System.out.println("  sync found new Dataset= " + path);
      }
    }

    if (!changed) { // check for deletions
      for (MFile oldDataset : oldMap.values()) {
        String path = oldDataset.getPath();
        MFile newDataset = newMap.get(path);
        if (newDataset == null) {
          changed = true;
          if (debugSyncDetail) System.out.println("  sync found deleted Dataset= " + path);
          break;
        }
      }
    }

    if (changed) {
      synchronized (this) {
        map = newMap;
        this.lastScanned = System.currentTimeMillis();
      }
    }

    return changed;
  }

  /**
   * Get how often to rescan
   *
   * @return time dureation of rescan period, or null if none.
   */
  public TimeUnit getRecheck() {
    return recheck;
  }

  /**
   * Get the last time scanned
   *
   * @return msecs since 1970
   */
  public long getLastScanned() {
    return lastScanned;
  }

  /**
   * Get the current collection of MFile, since last scan or rescan.
   *
   * @return current list of MFile, sorted by name
   */
  public List<MFile> getFiles() {
    List<MFile> result = new ArrayList<MFile>(map.values());
    Collections.sort(result);
    return result;
  }

  protected void scan(java.util.Map<String, MFile> map, CancelTask cancelTask) throws IOException {
    if (null == controller) controller = new thredds.filesystem.ControllerOS();  // default

    // run through all scanners and collect MFile instances into the Map
    for (MCollection mc : scanList) {

      Iterator<MFile> iter = (mc.wantSubdirs()) ? controller.getInventory(mc) : controller.getInventoryNoSubdirs(mc);
      if (iter == null) {
        logger.error("DatasetCollectionManager Invalid collection= " + mc);
        continue;
      }

      while (iter.hasNext()) {
        MFile mfile = iter.next();
        mfile.setAuxInfo(mc.getAuxInfo());
        map.put(mfile.getPath(), mfile);
      }

      if ((cancelTask != null) && cancelTask.isCancel())
        return;
    }
  }

  @Override
  public String toString() {
    return "DatasetCollectionManager{" +
            "collectionName='" + collectionName + '\'' +
            ", scanList=" + scanList +
            ", recheck=" + recheck +
            ", lastScanned=" + lastScanned +
            ", mm=" + mm +
            '}';
  }

  ///////////////////////
  private MetadataManager mm;

  private void initMM() {
    try {
      mm = new MetadataManager(collectionName);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public void putMetadata(MFile file, String key, byte[] value) {
    if (mm == null) initMM();
    mm.put(file.getPath()+"#"+key, value);
  }

  @Override
  public byte[] getMetadata(MFile file, String key) {
    if (mm == null) initMM();
    return mm.getBytes(file.getPath()+"#"+key);
  }


}
