import java.io.*;
import java.util.*;

//class finds foraging duration from out to pellet 
public class TSForaging {
   private double[][] preForaging;
   private double[][] robotForaging;
   private PrintStream output;
   private Scanner gateTSfile; 
   private List<Double> gateTS;
     
   //constructor 
   public TSForaging(double[][] preForaging, double[][] robotForaging, PrintStream output) throws FileNotFoundException, IOException {
      this.preForaging = preForaging;
      this.robotForaging = robotForaging;
      this.output = output; 
      this.gateTSfile = new Scanner(new File("gateTS.txt"));
      this.gateTS = new ArrayList<>();
      
      if (gateTSfile.hasNextLine()) {
         while (gateTSfile.hasNextDouble()) {
            gateTS.add(gateTSfile.nextDouble());
            gateTSfile.nextLine();
         }
      } 
   }
   
   public PrintStream findForaging() throws IOException {
      
      //find foraging duration --> from out to pellet
      // sort out, pelletR, pelletL into 1 list for each session
      // label out as 0, pelletR as 1, pelletL as 2 in column 1, column 2 is ts
      //      1 --> marker --> from 1 until 1 occurs again --> subtract first 1 index from (next 0 index-1) timestamp
      //          get label of (next 1 index -1) --> if 1 --> add to R pellet foraging time array
      //          if 2 --> add to L pellet foraging time array 
      // print R pellet FT array and L pellet FT array 
      
      getRidOfRepeatOuts(preForaging);
      
      readGateTSforNextSession();
      
      getRidOfRepeatOuts(robotForaging);
      
      sort(preForaging);
      sort(robotForaging);
          
      extractForaging(preForaging);
      output.println("-----------------------------------------");
      extractForaging(robotForaging);
      
      return output;
   }
   
   //read next group of gate timestamps 
   private void readGateTSforNextSession() {
      if (gateTSfile.hasNextLine()) {
         gateTSfile.nextLine();
         while (gateTSfile.hasNextDouble()) {
            gateTS.add(gateTSfile.nextDouble());
            gateTSfile.nextLine();
         }
      } 
   }
   
   //gets rid of out timestamps that we don't need --> out timestamps after first out timestamp that occured after 
   //a gate ts
   private void getRidOfRepeatOuts(double[][] foraging) {
      int j = 0;
      double temp = 0;
      
      for (int i = 0; i < gateTS.size(); i++) {
         temp = foraging[j][1];
         
         if (i == gateTS.size() - 1) {
            while (foraging[j][0] == 0) {
               foraging[j][1] = temp;
               j++;
            }
            return;
         }
                 
         while (foraging[j][1] < gateTS.get(i+1)) {
            if (foraging[j][0] == 0) {
               foraging[j][1] = temp;
            }
            j++;
           
            
            if (j + 1 == foraging.length) {
               return;
            }
         }
      }
   }
   
   //sorts timestamps in increasing order
   private void sort(double[][] foraging){
      Arrays.sort(foraging, new Comparator<double[]>() {
         public int compare(double[] first, double[] second) {
            if (first[1] > second[1]) {
               return 1; 
            } else return -1;
         }
      });
   }
   
   // extracts foraging intervals
   private void extractForaging(double[][] foraging) throws IOException{
      int count = 0;
      
      while(foraging[count][0] == 0) {
         count++;
      }
      count--;
      
      double outTS = 0;
      double pelletTS = 0;
      List<Double> leftForaging = new ArrayList<>();
      List<Double> rightForaging = new ArrayList<>();
      List<Double> rightOutTS = new ArrayList<>();
      List<Double> leftOutTS = new ArrayList<>();
      
      while (count < foraging.length) {
         outTS = foraging[count][1]; 
         count++;
              
         if (count > foraging.length - 1) {
            break;
         }     
         while (foraging[count][0] != 0) { //to get to last turn before next out ts
            count++;
            if (count == foraging.length) {
               break;
            }
         }
         count--;
         
         pelletTS = foraging[count][1];
         
         if (foraging[count][0] == 1) {
            rightForaging.add(pelletTS);
            rightOutTS.add(outTS);
         } else { // (foraging[count][0] == 2)
            leftForaging.add(pelletTS);
            leftOutTS.add(outTS);
         }
         
         count++;
         
         if (count >= foraging.length - 1) {
            break;
         }
         while (foraging[count][0] == 0) {
            if (count == foraging.length) {
               break;
            }
            count++;
         }
         count--;
      }
      
      output.println("RIGHT FORAGING");
      for (int i = 0; i < rightOutTS.size(); i++) {
         output.printf("%.6f\t%.6f\t%.6f\n", rightOutTS.get(i), rightForaging.get(i), (rightForaging.get(i) - rightOutTS.get(i)));
      }
      output.println("LEFT FORAGING");
      for (int i = 0; i < leftOutTS.size(); i++) {
         output.printf("%.6f\t%.6f\t%.6f\n", leftOutTS.get(i), leftForaging.get(i), (leftForaging.get(i) - leftOutTS.get(i)));
      }
   }
}