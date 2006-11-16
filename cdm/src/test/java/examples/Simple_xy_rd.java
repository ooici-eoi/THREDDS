package examples;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.*;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Oct 16, 2006
 * Time: 10:16:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class Simple_xy_rd {

    public static void main(String args[]) throws Exception, java.lang.NullPointerException
    {

       final int NX = 6;
       final int NY = 12;
       // This is the array we will read.
       int[][] dataIn = new int[NX][NY];

       // Open the file. The ReadOnly parameter tells netCDF we want
       // read-only access to the file.
       NetcdfFile dataFile = null;
       String filename = "simple_xy.nc";
       // Open the file.
       try {

            try {
                System.out.println("**** Open "+filename);
                dataFile = NetcdfFile.open(filename, null);
            } catch (java.io.IOException e) {
                System.out.println("Error opening file " + filename);
                e.printStackTrace();
                return;
            }

           // Retrieve the variable named "data"
            Variable dataVar = dataFile.findVariable("data");

            if (dataVar == null) {
                System.out.println("Cant find Variable data");
                return;
            }

           // Read all the values from the "data" variable into memory.
            int [] shape = dataVar.getShape();
            int[] origin = new int[2];

            ArrayInt.D2 dataArray;
            try {
                dataArray = (ArrayInt.D2) dataVar.read(origin, shape);
            } catch (InvalidRangeException e) {
                System.out.println("Bad range reading " + filename);
                e.printStackTrace();
                return;
            }  catch (IOException e) {
                System.out.println("Error reading " + filename);
                e.printStackTrace();
                return;
            }

           // Check the values.
            assert shape[0] == NX;
            assert shape[1] == NY;

            for (int j=0; j<shape[0]; j++) {
               for (int i=0; i<shape[1]; i++) {
                  dataIn[j][i] = dataArray.get(j,i);
               }
            }


       // The file is closed no matter what by putting inside a try/catch block.
       } finally {
           if (dataFile != null)
           try {
             dataFile.close();
           } catch (IOException ioe) {
             ioe.printStackTrace();
           }
    }

    System.out.println( "*** SUCCESS reading example file simple_xy.nc!");

    }

}
