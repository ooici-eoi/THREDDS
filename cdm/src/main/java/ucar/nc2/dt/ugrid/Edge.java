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
public class Edge extends Entity {
  private ArrayList<Node> nodes;

  public ArrayList<Node> getNodes() {
    return nodes;
  }

  public void setNodes(ArrayList<Node> n) {
    nodes = n;
  }

  @Override
  public boolean isBoundry() {
    if (this.getConnectingCells().length == 0)
      return true;
    else
      return false;
  }
}
