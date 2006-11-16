package examples;

import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Nov 16, 2006
 * Time: 11:19:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class Pres_temp_4D_rd {


 public static void main(String args[]) {
   final int NLVL = 2;
   final int NLAT = 6;
   final int NLON = 12;

   // These are used to construct some example data.
   final float SAMPLE_PRESSURE = 900.0f;
   final float SAMPLE_TEMP = 9.0f;
   final float START_LAT = 25.0f;
   final float START_LON = -125.0f;

   // Open the file.
   String filename = "pres_temp_4D.nc";
   NetcdfFile dataFile = null;
   try {

     try {
       dataFile = NetcdfFile.open(filename, null);
     } catch (java.io.IOException e) {
       System.out.println("Error opening file " + filename);
       e.printStackTrace();
       return;
     }

     // Get the latitude and longitude Variables.
     Variable latVar = dataFile.findVariable("latitude");
     if (latVar == null) {
       System.out.println("Cant find Variable latitude");
       return;
     }

     Variable lonVar = dataFile.findVariable("longitude");
     if (lonVar == null) {
       System.out.println("Cant find Variable longitude");
       return;
     }

     // Get the lat/lon data from the file.
     ArrayFloat.D1 latArray;
     ArrayFloat.D1 lonArray;
     try {
       latArray = (ArrayFloat.D1) latVar.read();
       lonArray = (ArrayFloat.D1) lonVar.read();
     } catch (IOException e) {
       System.out.println("Error reading " + filename);
       e.printStackTrace();
       return;
     }

     // Check the coordinate variable data.
     for (int lat = 0; lat < NLAT; lat++)
       if (latArray.get(lat) != START_LAT + 5. * lat)
         System.err.println("ERROR incorrect value in variable latitude");

     for (int lon = 0; lon < NLON; lon++)
       if (lonArray.get(lon) != START_LON + 5. * lon)
         System.err.println("ERROR incorrect value in variable longtitude");

     // Get the pressure and temperature variables.
     Variable presVar = dataFile.findVariable("pressure");
     if (presVar == null) {
       System.out.println("Cant find Variable pressure");
       return;
     }

     Variable tempVar = dataFile.findVariable("temperature");
     if (lonVar == null) {
       System.out.println("Cant find Variable temperature");
       return;
     }

     int [] shape = presVar.getShape();
     int recLen = shape[0]; // number of times

     int[] origin = new int[4];
     shape[0] = 1; // only one rec per read

     // loop over the rec dimension
     for (int rec = 0; rec < recLen; rec++) {
       origin[0] = rec;  // read this index

       // read 3D array for that index
       ArrayFloat.D3 presArray, tempArray;
       try {
         presArray = (ArrayFloat.D3) (presVar.read(origin, shape).reduce());
         tempArray = (ArrayFloat.D3) (tempVar.read(origin, shape).reduce());
       } catch (InvalidRangeException e) {
         System.out.println("Bad range reading " + filename);
         e.printStackTrace();
         return;
       }  catch (IOException e) {
         System.out.println("Error reading " + filename);
         e.printStackTrace();
         return;
       }

       // now checking the value
       int count = 0;
       for (int lvl = 0; lvl < NLVL; lvl++)
         for (int lat = 0; lat < NLAT; lat++)
           for (int lon = 0; lon < NLON; lon++) {
             if ((presArray.get(lvl, lat, lon) != SAMPLE_PRESSURE + count) ||
                     (tempArray.get(lvl, lat, lon) != SAMPLE_TEMP + count))
               System.err.println("ERROR incorrect value in variable pressure or temperature");
             count++;
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
   System.out.println("*** SUCCESS reading example file "+filename);
 }

}