// $Id: GridController.java,v 1.5 2005/11/17 00:48:19 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2.ui.grid;

import ucar.ma2.Array;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.grid.*;
import ucar.unidata.geoloc.*;

import thredds.viewer.ui.*;
import thredds.viewer.ui.event.*;
import thredds.viewer.ui.geoloc.*;
import thredds.ui.BAMutil;

import ucar.nc2.util.NamedObject;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.Debug;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

/**
 * The controller manages the interactions between GRID data and renderers.
 * @author John Caron
 * @version $Id: GridController.java,v 1.5 2005/11/17 00:48:19 caron Exp $
 */
public class GridController {
  private static final int DELAY_DRAW_AFTER_DATA_EVENT = 250;   // quarter sec
  private static final String LastMapAreaName = "LastMapArea";
  private static final String LastProjectionName = "LastProjection";
  private static final String LastDatasetName = "LastDataset";
  private static final String ColorScaleName = "ColorScale";

  private PreferencesExt store;
  private GridUI ui;

   // delegates
  private ColorScale cs;
  private NavigatedPanel np;
  private VertPanel vertPanel;
  private ProjectionImpl project;

    // state
  private String datasetUrlString;
  private NetcdfDataset netcdfDataset;
  private GridDataset gridDataset;
  private GeoGrid currentField;
  private ArrayList levels;
  private ArrayList times;
  private int currentLevel;
  private int currentSlice;
  private int currentTime;
  private boolean drawWinds = false;
  boolean drawHorizOn = true, drawVertOn = false;

    // rendering
  private AffineTransform atI = new AffineTransform();  // identity transform
  // private MyImageObserver imageObs = new MyImageObserver();
  // private MyPrintable printer = null;

  private thredds.viewer.ui.Renderer renderMap = null;
  private GridRenderer renderGrid;
  //private WindRenderer renderWind;
  private javax.swing.Timer redrawTimer;
  private Color mapColor = Color.black;

    // ui
  private javax.swing.JLabel dataValueLabel, posLabel;

    // event management
  AbstractAction dataProjectionAction, exitAction, helpAction, showGridAction,
    showContoursAction, showContourLabelsAction, showWindsAction;
  AbstractAction drawHorizAction, drawVertAction;

  JSpinner strideSpinner;

  private ActionSourceListener fieldSource, levelSource, timeSource;
  private boolean eventsOK = true;
  private boolean startOK = false;

    // optimize GC
  private ProjectionPointImpl projPoint = new ProjectionPointImpl();

    // debugging
  private final boolean debug = false, debugOpen = false, debugBeans = false, debugTime = false, debugThread = false;
  private final boolean debugBB = false, debugBounds = false, debugPrinting = false, debugChooser = false;

  public GridController( GridUI ui, PreferencesExt store) {
    this.ui = ui;
    this.store = store;

    // colorscale
    Object bean = store.getBean( ColorScaleName, null);
    if ((null == bean) || !(bean instanceof ColorScale))
      cs = new ColorScale("default");
    else
      cs = (ColorScale) store.getBean( ColorScaleName, null);

    // set up the renderers; Maps are added by addMapBean()
    renderGrid = new GridRenderer(store);
    renderGrid.setColorScale(cs);
    //renderWind = new WindRenderer();

    // stride
    strideSpinner = new JSpinner( new SpinnerNumberModel(1, 1, 100, 1) );
    strideSpinner.addChangeListener( new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        Integer val = (Integer) strideSpinner.getValue();
        renderGrid.setHorizStride( val.intValue());
      }
    });

     // timer
    redrawTimer = new javax.swing.Timer(0, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater( new Runnable() {  // invoke in event thread
          public void run() {
            draw(false);
          }
        });
        redrawTimer.stop(); // one-shot timer
      }
    });
    redrawTimer.setInitialDelay(DELAY_DRAW_AFTER_DATA_EVENT);
    redrawTimer.setRepeats(false);

    makeActions();
  }

    // stuff to do after UI is complete
  void finishInit() {

      // some widgets from the GridUI
    np = ui.getNavigatedPanel();
    vertPanel = ui.getVertPanel();
    dataValueLabel = ui.getDataValueLabel();
    posLabel = ui.getPositionLabel();

      // get last saved Projection
    project = (ProjectionImpl) store.getBean(LastProjectionName, null);
    if (project != null)
      setProjection( project);

      // get last saved MapArea
    ProjectionRect ma = (ProjectionRect) store.getBean(LastMapAreaName, null);
    if (ma != null)
      np.setMapArea( ma);

    makeEventManagement();

    // last thing
    /* get last dataset filename and reopen it
    String filename = (String) store.get(LastDatasetName);
    if (filename != null)
      setDataset(filename); */
  }

  void start(boolean ok) {
    startOK = ok;
    renderGrid.makeStridedGrid();
  }

    // create all actions here
    // the actions can then be attached to buttcons, menus, etc
  private void makeActions() {
    boolean state;

    dataProjectionAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        ProjectionImpl dataProjection = renderGrid.getDataProjection();
        if ( null != dataProjection)
          setProjection( dataProjection);
      }
    };
    BAMutil.setActionProperties( dataProjectionAction, "DataProjection", "use Data Projection", false, 'D', 0);

     // draw horiz
    drawHorizAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        // System.out.println("showGridAction state "+state);
        drawHorizOn = state.booleanValue();
        ui.setDrawHorizAndVert( drawHorizOn, drawVertOn);
        draw(false);
      }
    };
    BAMutil.setActionProperties( drawHorizAction, "DrawHoriz", "draw horizontal", true, 'H', 0);
    state = store.getBoolean( "drawHorizAction", true);
    drawHorizAction.putValue(BAMutil.STATE, new Boolean(state));
    drawHorizOn = state;

     // draw Vert
    drawVertAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        // System.out.println("showGridAction state "+state);
        drawVertOn = state.booleanValue();
        ui.setDrawHorizAndVert( drawHorizOn, drawVertOn);
        draw(false);
       }
    };
    BAMutil.setActionProperties( drawVertAction, "DrawVert", "draw vertical", true, 'V', 0);
    state = store.getBoolean( "drawVertAction", false);
    drawVertAction.putValue(BAMutil.STATE, new Boolean(state));
    drawVertOn = state;

       // show grid
    showGridAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        // System.out.println("showGridAction state "+state);
        renderGrid.setDrawGrid( state.booleanValue());
        draw(false);
      }
    };
    BAMutil.setActionProperties( showGridAction, "Grid", "show grid", true, 'G', 0);
    state = store.getBoolean( "showGridAction", true);
    showGridAction.putValue(BAMutil.STATE, new Boolean(state));
    renderGrid.setDrawGrid( state);

     // contouring
    showContoursAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        renderGrid.setDrawContours( state.booleanValue());
        draw(false);
      }
    };
    BAMutil.setActionProperties( showContoursAction, "Contours", "show contours", true, 'C', 0);
    state = store.getBoolean( "showContoursAction", false);
    showContoursAction.putValue(BAMutil.STATE, new Boolean(state));
    renderGrid.setDrawContours( state);

     // contouring labels
    showContourLabelsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        renderGrid.setDrawContourLabels( state.booleanValue());
        draw(false);
      }
    };
    BAMutil.setActionProperties( showContourLabelsAction, "ContourLabels", "show contour labels", true, 'L', 0);
    state = store.getBoolean( "showContourLabelsAction", false);
    showContourLabelsAction.putValue(BAMutil.STATE, new Boolean(state));
    renderGrid.setDrawContourLabels( state);

     /* winds
    showWindsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        drawWinds = state.booleanValue();
        draw(true, false, false);
      }
    };
    BAMutil.setActionProperties( showWindsAction, "ShowWinds", "show wind", true, 'W', 0);
    */
  }

  private void makeEventManagement() {

      //// manage field selection events
    String actionName = "field";
    ActionCoordinator fieldCoordinator = new ActionCoordinator(actionName);
      // connect to the fieldChooser
    fieldCoordinator.addActionSourceListener(ui.getFieldChooser().getActionSourceListener());
      // connect to the gridTable
    fieldCoordinator.addActionSourceListener(ui.getGridTable().getActionSourceListener());
      // heres what to do when the currentField changes
    fieldSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        if (setField( e.getValue().toString())) {
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
            //colorScalePanel.paintImmediately(colorScalePanel.getBounds());   // kludgerino
          } else
            redrawLater();
        }
      }
    };
    fieldCoordinator.addActionSourceListener(fieldSource);

    //// manage level selection events
    actionName = "level";
    ActionCoordinator levelCoordinator = new ActionCoordinator(actionName);
      // connect to the levelChooser
    levelCoordinator.addActionSourceListener(ui.getLevelChooser().getActionSourceListener());
      // connect to the vertPanel
    levelCoordinator.addActionSourceListener(ui.getVertPanel().getActionSourceListener());
      // also manage Pick events from the vertPanel
    vertPanel.getDrawArea().addPickEventListener( new PickEventListener() {
      public void actionPerformed(PickEvent e) {
        int level = renderGrid.findLevelCoordElement(e.getLocation().getY());
         if ((level != -1) && (level != currentLevel)) {
          currentLevel = level;
          redrawLater();
          String selectedName = ((NamedObject)levels.get(currentLevel)).getName();
          if (Debug.isSet("pick/event"))
            System.out.println("pick.event Vert: "+selectedName);
          levelSource.fireActionValueEvent(ActionSourceListener.SELECTED, selectedName);
        }
      }
    });
      // heres what to do when a level changes
    levelSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int level = findIndexFromName( levels, e.getValue().toString());
        if ((level != -1) && (level != currentLevel)) {
          currentLevel = level;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
          } else
            redrawLater();
        }
      }
    };
    levelCoordinator.addActionSourceListener(levelSource);

    //// manage time selection events
    actionName = "time";
    ActionCoordinator timeCoordinator = new ActionCoordinator(actionName);
      // connect to the timeChooser
    timeCoordinator.addActionSourceListener(ui.getTimeChooser().getActionSourceListener());
      // heres what to do when the time changes
    timeSource = new ActionSourceListener(actionName) {
      public void actionPerformed(ActionValueEvent e) {
        int time = findIndexFromName( times, e.getValue().toString());
        if ((time != -1) && (time != currentTime)) {
          currentTime = time;
          if (e.getActionCommand().equals("redrawImmediate")) {
            draw(true);
            //colorScalePanel.paintImmediately(colorScalePanel.getBounds());   // kludgerino
          } else
            redrawLater();
        }
      }
    };
    timeCoordinator.addActionSourceListener(timeSource);

      // get Projection Events from the navigated panel
    np.addNewProjectionListener( new thredds.viewer.ui.geoloc.NewProjectionListener() {
      public void actionPerformed( NewProjectionEvent e) {
        if (Debug.isSet("event/NewProjection"))
           System.out.println("Controller got NewProjectionEvent "+ np.getMapArea());
        if (eventsOK && renderMap != null) {
          renderMap.setProjection( e.getProjection());
          renderGrid.setProjection( e.getProjection());
          drawH(false);
        }
      }
    });

          // get NewMapAreaEvents from the navigated panel
    np.addNewMapAreaListener( new NewMapAreaListener() {
      public void actionPerformed(NewMapAreaEvent e) {
        if (Debug.isSet("event/NewMapArea"))
           System.out.println("Controller got NewMapAreaEvent "+ np.getMapArea());
        drawH(false);
      }
    });


      // get Pick events from the navigated panel
    np.addPickEventListener( new PickEventListener() {
      public void actionPerformed(PickEvent e) {
        projPoint.setLocation(e.getLocation());
        int slice = renderGrid.findSliceFromPoint(projPoint);
        if (Debug.isSet("pick/event"))
          System.out.println("pick.event: "+projPoint+" "+slice);
        if ((slice >= 0) && (slice != currentSlice)) {
          currentSlice = slice;
          vertPanel.setSlice( currentSlice);
          redrawLater();
        }
      }
    });

      // get Move events from the navigated panel
    np.addCursorMoveEventListener( new CursorMoveEventListener() {
      public void actionPerformed(CursorMoveEvent e) {
        projPoint.setLocation(e.getLocation());
        String valueS = renderGrid.getXYvalueStr(projPoint);
        dataValueLabel.setText(valueS);
      }
    });

      // get Move events from the vertPanel
    vertPanel.getDrawArea().addCursorMoveEventListener( new CursorMoveEventListener() {
      public void actionPerformed(CursorMoveEvent e) {
        Point2D loc = e.getLocation();
        posLabel.setText(renderGrid.getYZpositionStr(loc));
        dataValueLabel.setText(renderGrid.getYZvalueStr(loc));
      }
    });


      // catch window resize events in vertPanel : LOOK event order problem?
    vertPanel.getDrawArea().addComponentListener( new ComponentAdapter() {
      public void componentResized( ComponentEvent e) {
        draw(false);
      }
    });
  }

  /////////////////////////////////////////////////////////////////////////////
  // these are some routines exposed to GridUI
  String getDatasetName() {
    return (null == gridDataset) ? null : gridDataset.getName();
  }

  String getDatasetUrlString() {
    return datasetUrlString;
  }

  String getDatasetXML() {
    if (gridDataset == null) return "";
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
      gridDataset.getNetcdfDataset().writeNcML( bos, null);
      return bos.toString();
    } catch (IOException ioe) {}
    return "";
  }

  NetcdfDataset getNetcdfDataset() { return netcdfDataset; }
  GeoGrid getCurrentField() { return currentField; }
  Array getCurrentHorizDataSlice() { return renderGrid.getCurrentHorizDataSlice(); }

  String getDatasetInfo() {
    return (null == gridDataset) ? "" : gridDataset.getDetailInfo();
  }

 /** iterator returns NamedObject CHANGE TO GENERIC */
  java.util.List getFields() {
    if (gridDataset == null)
      return null;
    else
      return gridDataset.getGrids();
  }

  public void setGridDataset(GridDataset gridDataset) {
    this.gridDataset = gridDataset;
    this.netcdfDataset = gridDataset.getNetcdfDataset();
    this.datasetUrlString = netcdfDataset.getLocation();
    startOK = false; // wait till redraw is hit before drawing
  }

  /* assume that this might be done in a backgound task
  boolean openDataset(thredds.catalog.InvAccess access, ucar.nc2.util.CancelTask task) {
    String urlString = access.getStandardUrlName();
    if (debugOpen) System.out.println("GridController.openDataset= "+urlString);

    InvService s = access.getService();
    if (s.getServiceType() == ServiceType.RESOLVER) {
      InvDatasetImpl rds = openResolver( urlString);
      if (rds == null) return false;
      access = rds.getAccess(ServiceType.DODS);
      if (access == null)
        access = rds.getAccess(ServiceType.NETCDF);
      if (access == null) {
        JOptionPane.showMessageDialog(null, rds.getName() + ": no access of type DODS or NETCDF");
        return false;
      }
      urlString = access.getStandardUrlName();
    }

    InvDatasetImpl ds = (InvDatasetImpl) access.getDataset();
    NetcdfFile ncfile = null;

    try {
      // check for NcML substitution
      java.util.List list = ds.getMetadata(MetadataType.NcML);
      if (list.size() > 0 ) {
        InvMetadata metadata = (InvMetadata) list.get(0);
        String ncmlUrlName = metadata.getXlinkHref();
        try {
          java.net.URI uri = ds.getParentCatalog().resolveUri(ncmlUrlName);
          ncmlUrlName = uri.toString();
          if (debugOpen)
            System.out.println(" got NcML metadata= " + ncmlUrlName);
          ncfile = new NetcdfDataset(ncmlUrlName, urlString);
        } catch (java.net.URISyntaxException e) {
          System.err.println("Error parsing ncmlUrlName= "+ncmlUrlName); //LOOK
        }
      }

      // check for DODS type
      else if (s.getServiceType() == ServiceType.DODS) {
        ncfile = new DODSNetcdfFile(urlString, task);

      // otherwise send it through the factory
      } else {
        ncfile = ucar.nc2.dataset.NetcdfDataset.factory( urlString, task);
      }

      // add conventions
      if (task.isCancel()) return false;
      NetcdfDataset ncDataset = ucar.nc2.dataset.conv.Convention.factory( ncfile);
      if (ncDataset == null)
        ncDataset = new NetcdfDataset( ncfile);

      // GeoGrid parsing
      if (task.isCancel()) return false;
      GridDataset gDataset = new GridDataset( ncDataset);

      // all ok !!
      datasetUrlString = ncDataset.getPathName();
      netcdfDataset = ncDataset;
      gridDataset = gDataset;
      return true;

    } catch (java.net.MalformedURLException ee) {
      task.setError("URL incorrectly formed "+urlString+"\n"+ee.getMessage());
      return false;
    } catch (java.rmi.RemoteException ee) {
      task.setError("Cannot open remote file "+urlString+"\n"+ee.getMessage());
      //ee.printStackTrace();
      return false;
    } catch (dods.dap.DODSException ee) {
      task.setError("Cannot open file "+urlString+"\nIOException = "+ee);
      ee.printStackTrace();
      return false;
    } catch (java.io.IOException ee) {
      task.setError("Cannot open file "+urlString+"\nIOException = "+ee);
      ee.printStackTrace();
      return false;
    } catch (java.lang.IllegalArgumentException ee) {
      ee.printStackTrace();
      task.setError("Cannot open file "+urlString+"\n"+ee.getMessage());
      return false;
    }
  }

  private InvDatasetImpl openResolver( String urlString) {
    try {
      InvCatalogFactory factory = new InvCatalogFactory("grid", true);
      InvCatalog catalog = factory.readXML( urlString);
      if (catalog == null) return null;
      StringBuffer buff = new StringBuffer();
      if (!catalog.check( buff)) {
        javax.swing.JOptionPane.showMessageDialog(null, "Invalid catalog  from Resolver <"+ urlString+">\n"+
          buff.toString());
        System.out.println("Invalid catalog from Resolver <"+ urlString+">\n"+buff.toString());
        return null;
      }
      InvDataset top = catalog.getDataset();
      if (top.hasAccess())
        return (InvDatasetImpl) top;
      else {
        java.util.List datasets = top.getDatasets();
        return (InvDatasetImpl) datasets.get(0);
      }

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  } */


  // assume that its done in the event thread
  boolean showDataset() {

      // temp kludge for initialization
    java.util.List grids = gridDataset.getGrids();
    if ((grids == null) || grids.size() == 0) {
      javax.swing.JOptionPane.showMessageDialog(null, "No gridded fields in file "+gridDataset.getName());
      return false;
    }

    currentField = (GeoGrid) grids.get(0);
    currentSlice = 0;
    currentLevel = 0;
    currentTime = 0;

    eventsOK = false; // dont let this trigger redraw
    renderGrid.setGeoGrid( currentField);
    ui.setFields( gridDataset.getGrids());
    setField( currentField.getName());

    // if possible, change the projection and the map area to one that fits this
    // dataset
    ProjectionImpl dataProjection = currentField.getProjection();
    if (dataProjection != null)
       setProjection(dataProjection);

    // ready to draw
    //draw(true);

    // events now ok
    eventsOK = true;
    return true;
  }

  //public GeoGrid getField() { return currentField; }
  private boolean setField(String name) {
    GeoGrid gg = gridDataset.findGridByName( name);
    if (null == gg)
      return false;

    renderGrid.setGeoGrid( gg);
    currentField = gg;

      // set levels
    levels = currentField.getLevels();
    if ((levels == null) || (currentLevel >= levels.size()))
      currentLevel = 0;
    vertPanel.setCoordSys( currentField.getCoordinateSystem(), currentLevel);

      // set times
    times = currentField.getTimes();
    if ((times == null) || (currentTime >= times.size()))
      currentTime = 0;

    ui.setField( gg);
    return true;
  }

  public int getCurrentLevelIndex() { return currentLevel; }
  public int getCurrentTimeIndex() { return currentTime; }
  public boolean setLevel(int index) {
    if ((index >= 0) && (index < levels.size())) {
      currentLevel = index;
    }
    redrawLater();
    return true;
  }

  private int findIndexFromName( ArrayList list, String name) {
     Iterator iter = list.iterator();
     int count = 0;
     while (iter.hasNext()) {
       NamedObject no = (NamedObject) iter.next();
       if (name.equals(no.getName()))
         return count;
       count++;
     }
     System.out.println("ERROR: Controller.setLevels cant find "+name);
     return -1;
  }

  public int getTimeIndex() { return currentTime; }
  public boolean setTime(int index) {
    if ((index >= 0) && (index < times.size())) {
      currentTime = index;
    }
    redrawLater();
    return true;
  }

  /* public Printable getPrintable() {
    if (printer == null)
      printer = new MyPrintable();
    return printer;
  } */

  synchronized void draw(boolean immediate) {
    if (!startOK) return;

    renderGrid.setLevel( currentLevel);
    renderGrid.setTime( currentTime);
    renderGrid.setSlice( currentSlice);

    if (drawHorizOn)
      drawH(immediate);
    if (drawVertOn)
      drawV(immediate);
  }

  private void drawH(boolean immediate) {
    if (!startOK) return;

    // cancel any redrawLater
    boolean already = redrawTimer.isRunning();
    if (debugThread && already) System.out.println( "redrawLater canceled ");
    if (already)
      redrawTimer.stop();

    long tstart = System.currentTimeMillis();
    long startTime, tookTime;

    //// horizontal slice
    // the Navigated Panel's BufferedImage graphics
    Graphics2D gNP = np.getBufferedImageGraphics();
    if (gNP == null) // panel not drawn on screen yet
      return;

      // clear
    gNP.setBackground(np.getBackgroundColor());
    gNP.fill(gNP.getClipBounds());

      // draw grid
    startTime = System.currentTimeMillis();
    renderGrid.renderPlanView(gNP, atI);
    if (Debug.isSet("timing/GridDraw")) {
      tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.GridDraw: " + tookTime*.001 + " seconds");
    }

    //draw Map
    if (renderMap != null) {
      startTime = System.currentTimeMillis();
      renderMap.draw(gNP, atI);
      if (Debug.isSet("timing/MapDraw")) {
        tookTime = System.currentTimeMillis() - startTime;
        System.out.println("timing/MapDraw: " + tookTime*.001 + " seconds");
      }
    }

    /* draw Winds
    if (drawWinds) {
      startTime = System.currentTimeMillis();
      renderWind.draw(gNP, currentLevel, currentTime);
      if (Debug.isSet("timing/WindsDraw")) {
        tookTime = System.currentTimeMillis() - startTime;
        System.out.println("timing.WindsDraw: " + tookTime*.001 + " seconds");
      }
    } */

     // copy buffer to the screen
    if (immediate)
      np.drawG();
    else
      np.repaint();

      // cleanup
    gNP.dispose();

    if (Debug.isSet("timing/total")) {
      tookTime = System.currentTimeMillis() - tstart;
      System.out.println("timing.total: " + tookTime*.001 + " seconds");
    }
  }

  private void drawV(boolean immediate) {
    if (!startOK) return;
    ScaledPanel drawArea = vertPanel.getDrawArea();
    Graphics2D gV = drawArea.getBufferedImageGraphics();
    if (gV == null)
      return;

    long startTime = System.currentTimeMillis();

    gV.setBackground(Color.white);
    gV.fill(gV.getClipBounds());
    renderGrid.renderVertView(gV, atI);

    if (Debug.isSet("timing/GridDrawVert")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.GridDrawVert: " + tookTime*.001 + " seconds");
    }
    gV.dispose();

    // copy buffer to the screen
     if (immediate)
      drawArea.drawNow();
    else
      drawArea.repaint();
  }

  private synchronized void redrawLater() {
    //redrawComplete |= complete;
    boolean already = redrawTimer.isRunning();
    if (debugThread) System.out.println( "redrawLater isRunning= "+ already);
    if (already)
      redrawTimer.restart();
    else
      redrawTimer.start();
  }

  public ColorScale getColorScale() { return cs; }
  public void setColorScale( ColorScale cs) {
    this.cs = cs;
    renderGrid.setColorScale( cs);
    redrawLater();
  }

  public void setProjection( ProjectionImpl p) {
    project = p;
    if (renderMap != null)
      renderMap.setProjection( p);
    renderGrid.setProjection( p);
    // renderWind.setProjection( p);
    np.setProjectionImpl(p);
    redrawLater();
  }

  public void setDataMinMaxType( int type) {
    renderGrid.setDataMinMaxType( type);
    redrawLater();
  }

  void setMapRenderer( thredds.viewer.ui.Renderer mapRenderer) {
    this.renderMap = mapRenderer;
    mapRenderer.setProjection(np.getProjectionImpl());
    mapRenderer.setColor(mapColor);
    redrawLater();
  }

  public void storePersistentData() {
    store.putBeanObject(LastMapAreaName, np.getMapArea());
    store.putBeanObject(LastProjectionName, np.getProjectionImpl());
    if (gridDataset != null)
      store.put(LastDatasetName, gridDataset.getName());
    store.putBeanObject(ColorScaleName, cs);

    store.putBoolean( "showGridAction", ((Boolean)showGridAction.getValue(BAMutil.STATE)).booleanValue());
    store.putBoolean( "showContoursAction", ((Boolean)showContoursAction.getValue(BAMutil.STATE)).booleanValue());
    store.putBoolean( "showContourLabelsAction", ((Boolean)showContourLabelsAction.getValue(BAMutil.STATE)).booleanValue());

  }

    /* necessary for g.drawImage()
  private class MyImageObserver implements java.awt.image.ImageObserver {
    public boolean imageUpdate(java.awt.Image image, int flags, int x, int y, int width, int height) {
      return true;
    }
  }

    /* for printing
  private class MyPrintable implements Printable {

    /* Render to a printer
        @param g        the Graphics context
        @param pf       describes the page format
        @param pi       page number

    public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
      if (pi >= 1) {
        return Printable.NO_SUCH_PAGE;
      }
      Graphics2D g2 = (Graphics2D) g;
      AffineTransform dFromN = g2.getTransform();   // device from normal coord transf

      // entire page printable size
      double pheight = pf.getImageableHeight();
      double pwidth = pf.getImageableWidth();
      double px = pf.getImageableX();
      double py = pf.getImageableY();

      double colorscale_size = 50;
      double title_size = 50;

      // image size
      double gheight = pheight;
      double gwidth = pwidth;
      double gx = px;
      double gy = py;

      // print the colorscale
      if ( PageFormat.LANDSCAPE == pf.getOrientation()) {
        // colorscale on the left side
        gwidth -= colorscale_size;
        gheight -= title_size;
        gx += colorscale_size;

        ColorScale.Panel colorScalePanel =
                new ColorScale.Panel(null, ColorScale.VERTICAL, getColorScale());
        //g2.drawRect( (int) px, (int) py, (int) colorscale_size, (int)(pheight-title_size));
        colorScalePanel.print(g2, px, py, colorscale_size, gheight);

       } else {
        // colorscale at the bottom
        gheight -= colorscale_size + title_size;

        ColorScale.Panel colorScalePanel =
                new ColorScale.Panel(null, ColorScale.HORIZONTAL, getColorScale());
        colorScalePanel.print(g2, px, py+gheight, gwidth, colorscale_size);
       }

      // text box size
      double theight = title_size;
      double twidth = pwidth;
      double tx = px;
      double ty = py + pheight - title_size;

        //line 1
      g2.setColor(Color.black);
      FontUtil.StandardFont fontTitle = FontUtil.getStandardFont( 20);
      g2.setFont( fontTitle.getFont());
      String line1 = currentField.getDescription() + " ("+currentField.getUnitString() + ")";
      Dimension d = fontTitle.getBoundingBox(line1);
      double starty = ty + d.getHeight() + 5;
      double startx = tx + (twidth - d.getWidth())/2;
      g2.drawString(line1, (int) startx, (int) starty);

         // line 2
      StringBuffer lineb2 = new StringBuffer(100);
      lineb2.setLength(0);
      GeoCoordSysImpl gcs = currentField.getGeoCoordSysImpl();
      if (gcs.getZaxis() != null)
        lineb2.append("level "+gcs.getLevelName(currentLevel)+" ");
      if (gcs.getTaxis() != null)
        lineb2.append(gcs.getTimeName(currentTime)+" ");
      lineb2.append(dataset.getName());
      String line2 = lineb2.toString();

      FontUtil.StandardFont fontLine2 = FontUtil.getStandardFont( 12);
      g2.setFont( fontLine2.getFont());
      d = fontLine2.getBoundingBox(line2);
      startx = tx + (twidth - d.getWidth())/2;
      starty += d.getHeight() + 5;
      g2.drawString(line2, (int) startx, (int) starty);

      g2.setColor(Color.black);
      g2.setClip( (int) gx, (int) gy, (int) gwidth, (int)gheight);
      g2.drawRect( (int) gx, (int) gy, (int) gwidth, (int)gheight);

        /// calculate the world -> device transform; set the graphics context to use it
      AffineTransform at2 = np.calcTransform(false, gx, gy, gwidth, gheight);
      g2.transform( at2);

        // set graphics attributes
      AffineTransform at = g2.getTransform();
      if (Debug.isSet("print/showTransform")) {
        System.out.println("print.showTransform: "+at+ " original = "+ dFromN);
      }

      //double scale = at.getScaleX();
      //g2.setStroke(new BasicStroke((float)(2.0/scale)));  // default stroke size is two pixels
      g2.setStroke(new BasicStroke(0.0f));  // default stroke size is one pixel LOOK! nonzero hanging printer
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

      renderGrid.setLevel( currentLevel);
      renderGrid.setTime( currentTime);
      renderGrid.setSlice( currentSlice);
      renderGrid.renderPlanView(g2, dFromN);  // LOOK wrong

      Color save = renderMap.getColor();
      renderMap.setColor(Color.black);
      renderMap.draw(g2, dFromN);
      renderMap.setColor(save);

          //draw Winds
      if (drawWinds) {
        renderWind.draw(g2, currentLevel, currentTime);
      }

      if (Debug.isSet("print/pageFormat")) {
        System.out.println("  Graphics clip "+ g2.getClipBounds());
        System.out.println("  Page Format     "+ pf.getOrientation());
        System.out.println("  getH/W          "+ pf.getHeight()+ " "+ pf.getWidth());
        System.out.println("  getImageableH/W "+ pf.getImageableHeight()+ " "+ pf.getImageableWidth());
        System.out.println("  getImageableX/Y "+ pf.getImageableX()+ " "+ pf.getImageableY());

        /* Paper paper = pf.getPaper();
        System.out.println("  Paper     ");
        System.out.println("  getH/W          "+ paper.getHeight()+ " "+ paper.getWidth());
        System.out.println("  getImageableH/W "+ paper.getImageableHeight()+ " "+ paper.getImageableWidth());
        System.out.println("  getImageableX/Y "+ paper.getImageableX()+ " "+ paper.getImageableY());
      }

      return Printable.PAGE_EXISTS;
    }
  }
  */
}

/* Change History:
   $Log: GridController.java,v $
   Revision 1.5  2005/11/17 00:48:19  caron
   NcML aggregation
   caching close/synch
   grid subset bug

   Revision 1.4  2004/12/07 02:43:23  caron
   *** empty log message ***

   Revision 1.3  2004/12/07 01:29:32  caron
   redo convention parsing, use _Coordinate encoding.

   Revision 1.2  2004/11/10 17:00:29  caron
   no message

   Revision 1.1  2004/09/30 00:33:43  caron
   *** empty log message ***

   Revision 1.10  2004/09/24 03:26:36  caron
   merge nj22

   Revision 1.9  2004/06/12 02:11:33  caron
   minor

   Revision 1.8  2004/03/19 20:18:02  caron
   use thredds.datamodel to connect catalog with data types

   Revision 1.7  2004/03/05 23:45:31  caron
   all resolver datasets

   Revision 1.6  2004/02/20 05:02:55  caron
   release 1.3

   Revision 1.5  2003/05/29 23:07:51  john
   bug fixes

   Revision 1.4  2003/04/08 18:16:20  john
   nc2 v2.1

   Revision 1.3  2003/03/17 21:12:33  john
   new viewer

   Revision 1.2  2003/01/07 14:57:19  john
   lib updates

   Revision 1.1  2002/12/13 00:51:11  caron
   pass 2

   Revision 1.4  2002/10/18 18:21:15  caron
   thredds server

   Revision 1.3  2002/04/30 22:45:27  caron
   fix event typecast bug for Grid

   Revision 1.2  2002/04/29 22:39:21  caron
   add StationUI, clean up

   Revision 1.1.1.1  2002/02/26 17:24:49  caron
   import sources

*/


