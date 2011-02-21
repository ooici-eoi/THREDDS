/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ucar.nc2.dt.ugrid;

import java.util.ArrayList;

/**
 *
 * @author Kyle
 */
public class Face extends Entity {
  private ArrayList<Node> nodes;

  public ArrayList<Node> getNodes() {
    return nodes;
  }

  public void setNodes(ArrayList<Node> n) {
    nodes = n;
  }
}
