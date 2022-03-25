/*
      FILES TO MODIFY BEFORE RUNNING THIS PROGRAM FOR EACH EVENT:
         timestamps.txt
         gateTS.txt
*/

import java.io.*;
import java.util.*;

public class TSSort{
   public static void main(String[] args) throws FileNotFoundException, IOException{
   
      ////data members we need for TSSort
      Scanner console = new Scanner(System.in);
      System.out.print("file name (put .txt at end of file name): "); //have to add .txt to end of file name 
      String filename = console.next();
      Scanner file = new Scanner(new File(filename));             //input file --> original timestamp w/ (x,y)
      
      System.out.print("Split at: ");              //timestamp to split at
      double splitTS = console.nextDouble();      
      
      List<Double> ts = new ArrayList<>();
      List<Integer> xPos = new ArrayList<>();      
      List<Integer> yPos = new ArrayList<>();     
      
      PrintStream output = new PrintStream("SortedTimestamps.txt");   //output file   
      
      ////data members needed for TSForaging
      double[][] preForaging = new double[50][2];     // sorts out, pelletR, pelletL into 1 list to find Pre foraging times
      double[][] robotForaging = new double[50][2];   // sorts out, pelletR, pelletL into 1 list to find Robot foraging times
      
      ////data members needed for TSOutbound
      double[][] preOutbound = new double[70][2];     // sorts out, turnR, turnL, u-turn, in into 1 list to find Pre Outbound times
      double[][] robotOutbound = new double[70][2];   // sorts out, turnR, turnL, u-turn, in into 1 list to find Robot Outbound times
       
      //pre session
      readPre(file, ts, xPos, yPos, splitTS);
      
      int count = findOut(output, ts, xPos, yPos, preForaging, preOutbound);         //int count --> to keep track of current index for preForaging/robotForaging
      int count2 = findTurnR(output, ts, xPos, yPos, preOutbound, count);            //int count2 --> to keep track of current index for preOutbound/robotOutbound
      count2 = findTurnL(output, ts, xPos, yPos, preOutbound, count2);
      count = findPelletR(output, ts, xPos, yPos, preForaging, count);
      count = findPelletL(output, ts, xPos, yPos, preForaging, count);
      count2 = findUturn(ts, xPos, yPos, preOutbound, count2);
      //count2 = findIn(ts, xPos, yPos, preOutbound, count2);
      
      //robot session
      readRobot(file, ts, xPos, yPos);
      
      count = findOut(output, ts, xPos, yPos, robotForaging, robotOutbound);
      count2 = findTurnR(output, ts, xPos, yPos, robotOutbound, count);
      count2 = findTurnL(output, ts, xPos, yPos, robotOutbound, count2);
      count = findPelletR(output, ts, xPos, yPos, robotForaging, count);
      count = findPelletL(output, ts, xPos, yPos, robotForaging, count);
      count2 = findUturn(ts, xPos, yPos, robotOutbound, count2);
      //count2 = findIn(ts, xPos, yPos, preOutbound, count2);
      
      //find foraging duration --> from out to pellet
      output.println("============================================================");
      
      TSForaging outToPellet = new TSForaging(preForaging, robotForaging, output);
      output = outToPellet.findForaging();
   
      //for outbound duration --> from out to turn or until outbounds switches to inbound
      output.println("============================================================");
      TSOutbound outbound = new TSOutbound(preOutbound, robotOutbound, output);
      output = outbound.findOutbounds();
   }

   //reads timestamps during pre session
   public static void readPre(Scanner file, List<Double> ts, List<Integer> xPos, List<Integer> yPos, double splitTS){
      ts.clear();
      xPos.clear();
      yPos.clear();
      
      if(file.hasNextLine()) {
         file.nextLine();
         file.nextLine();
      }
      
      double temp = 0;
      
      while(temp < splitTS){
         Scanner lineScanner = new Scanner(file.nextLine());
         temp = lineScanner.nextDouble();
         ts.add(temp);
         xPos.add(lineScanner.nextInt());
         yPos.add(lineScanner.nextInt());
      }
      
      checkCoordinates(xPos, yPos);
   }
   
   //reads timestamps during robot session
   public static void readRobot(Scanner file, List<Double> ts, List<Integer> xPos, List<Integer> yPos){
      ts.clear();
      xPos.clear();
      yPos.clear();
      
      while (file.hasNextLine()){
         Scanner lineScanner = new Scanner(file.nextLine());
         ts.add(lineScanner.nextDouble());
         xPos.add(lineScanner.nextInt());
         yPos.add(lineScanner.nextInt());
      }
      
      checkCoordinates(xPos, yPos);
   }
   
   //checks to see if there are glitches/jumps in the timestamps (too great of a jump || (0,0))
   public static void checkCoordinates(List<Integer> xPos, List<Integer> yPos){
      int x1 = 0;
      int x2 = 0;
      int y1 = 0;
      int y2 = 0;
      
      for (int i = 0; i < xPos.size() - 2; i++){
         x1 = xPos.get(i);
         x2 = xPos.get(i+1);
         y1 = yPos.get(i);
         y2 = yPos.get(i+1);
         
         if((Math.abs(x1-x2) > 40 && Math.abs(x1-x2) < 150) || (Math.abs(y1-y2) > 40 && Math.abs(x1-x2) < 150)){
            xPos.set(i+1, x1);
            yPos.set(i+1, y1);
         }
         
         if(x2 == 0 && y2 == 0){
            int[][] coordinates = new int[150][2];
            int count = 0;
            coordinates[count][0] = x2;
            coordinates[count][1] = y2;
            count = 1;
            
            while(xPos.get(i+1+count) == 0 && yPos.get(i+1+count) == 0){
               coordinates[count][0] = xPos.get(i+1+count);
               coordinates[count][1] = yPos.get(i+1+count);
               count++;               
            }
            
            int xNext = xPos.get(i+1+count);
            int yNext = yPos.get(i+1+count);
            
            int xAvg = (xNext + x1) / 2;
            int yAvg = (yNext + y1) / 2;
            
            for(int j = 0; j < count; j++){
               xPos.set(i+1+j, xAvg);
               yPos.set(i+1+j, yAvg);
            }
            
            i += count + 1;
         }
      }
   }
   
   //finds out timestamps
   public static int findOut(PrintStream output, List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] foraging, double[][] outbound){
      int x1 = 0;
      int x2 = 0;
      int y1 = 0;
      int y2 = 0;
      
      int count = 0;
      
      output.println("Out---------------------------------");
      
      for (int i = 0; i < ts.size() - 2; i++){
         x1 = xPos.get(i);
         x2 = xPos.get(i+1);
         y1 = yPos.get(i);
         y2 = yPos.get(i+1);
         
         if (x1 > 600 && x1 < 690 && x2 > 600 && x2 < 690 && y1 >= 550 && y1 <= 570 && y2 < 550) {
            output.printf("%f", ts.get(i+1));
            output.println();
            foraging[count][0] = 0;
            foraging[count][1] = ts.get(i+1);
            outbound[count][0] = 0;
            outbound[count][1] = ts.get(i+1);
            count++;
         }
      }
      output.println();
      return count;
   }

   //finds R turn timestamps
   public static int findTurnR(PrintStream output, List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] outbound, int count){       
      output.println("R turn");
 
      for (int i = 0; i < ts.size() - 2; i++){  
         int j = 0; 
         outer: { 
         
         while((xPos.get(i+j) > 600 && xPos.get(i+j) < 687 && yPos.get(i+j) >= 222 && yPos.get(i+j) < 260)
         || (xPos.get(i+j) > 390 && xPos.get(i+j) < 500 && yPos.get(i+j) > 110 && yPos.get(i+j) < 222)){ //1 || Left box
            j++;
            
            while(xPos.get(i+j) >= 500 && xPos.get(i+j) <= 820 && yPos.get(i+j) > 110 && yPos.get(i+j) <= 222
            || (xPos.get(i+j) == 0 || yPos.get(i+j) == 0)){ //2 || 0,0
               j++;
               
               if(xPos.get(i+j) > 820 && yPos.get(i+j) > 110 && yPos.get(i+j) <= 222){ //3
                  output.printf("%f", ts.get(i + j));
                  output.println();
                  outbound[count][0] = 1;
                  outbound[count][1] = ts.get(i + j);
                  count++;
                  break outer;
               } else if (i + j == ts.size() -2 ){
                  break outer;
               }
            }
            if(i + j == ts.size() -2){
               break outer;
            }
         }
         } 
         i += j;
      }      
      output.println();
      return count;
   }
   
   //finds L turn timestamps
   public static int findTurnL(PrintStream output, List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] outbound, int count){
      output.println("L turn");
    
      for (int i = 0; i < ts.size() - 2; i++){ 
         int j = 0;
         outer: {
         while((xPos.get(i+j) > 600 && xPos.get(i+j) < 687 && yPos.get(i+j) >= 222 && yPos.get(i+j) < 260)
         || (xPos.get(i+j) > 820 && xPos.get(i+j) < 900 && yPos.get(i+j) > 110 && yPos.get(i+j) < 222)){ //1 || Right box
            j++;
            while(xPos.get(i+j) >= 500 && xPos.get(i+j) <= 820 && yPos.get(i+j) > 110 && yPos.get(i+j) <= 222
            || (xPos.get(i+j) == 0 || yPos.get(i+j) == 0)){ //2 || 0,0     
               j++;
               if(xPos.get(i+j) < 500 && yPos.get(i+j) > 110 && yPos.get(i+j) <= 222){ //3
                  output.printf("%f", ts.get(i + j));
                  output.println();
                  outbound[count][0] = 2;
                  outbound[count][1] = ts.get(i + j);
                  count++;
                  break outer;
               } else if (i + j == ts.size() -2 ){
                  break outer;
               }
            }
            if(i + j == ts.size() -2 ){
               break outer;
            }
         }
         }
         i += j; 
      } 
      output.println();
      return count;
   }

   //finds L pellet timestamps
   public static int findPelletR(PrintStream output, List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] foraging, int count){
      int x1 = 0;
      int x2 = 0;
      int y1 = 0;
      int y2 = 0;
      
      output.println("R pellet");
      
      for (int i = 0; i < ts.size() - 2; i++){
         x1 = xPos.get(i);
         x2 = xPos.get(i+1);
         y1 = yPos.get(i);
         y2 = yPos.get(i+1);
         
         if (x1 >= 894 && x1 <= 980 && x2 >= 894 && x2 <= 980 && y1 >= 275 && y1 < 285 && y2 >= 285) {
            output.printf("%f", ts.get(i+1));
            output.println();
            foraging[count][0] = 1;
            foraging[count][1] = ts.get(i+1);
            count++;            
            i += 200;
         }  
      }      
      output.println();
      return count;
   }
   
   //finds L pellet timestamps
   public static int findPelletL(PrintStream output, List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] foraging, int count){
      int x1 = 0;
      int x2 = 0;
      int y1 = 0;
      int y2 = 0;
      
      output.println("L pellet");
      
      for (int i = 0; i < ts.size() - 2; i++){
         x1 = xPos.get(i);
         x2 = xPos.get(i+1);
         y1 = yPos.get(i);
         y2 = yPos.get(i+1);
         
         if (x1 >= 320 && x1 <= 395 && x2 >= 320 && x2 <= 395 && y1 >= 275 && y1 < 285 && y2 >= 285) {
            output.printf("%f", ts.get(i+1));
            output.println();
            foraging[count][0] = 2;
            foraging[count][1] = ts.get(i+1);
            count++;
            i += 200;
         }  
      } 
      return count;     
   }
   
   
   //finds u-turn timestamps 
   public static int findUturn(List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] outbound, int count){
      int x1 = 0;
      int x2 = 0;
      int y1 = 0;
      int y2 = 0;
      
      //counters: 
      //    i = index of out timestamp
      //    j = index of in timestamp
      //    k = count of indexes from out to in timestamp
      
      for (int i = 0; i < ts.size() - 2; i++){
         x1 = xPos.get(i);
         x2 = xPos.get(i+1);
         y1 = yPos.get(i);
         y2 = yPos.get(i+1);
         
         int j = 0;
         
         if (x1 > 600 && x1 < 690 && x2 > 600 && x2 < 690 && y1 >= 550 && y1 <= 570 && y2 < 550) {
            while (xPos.get(i + j) > 500 && xPos.get(i + j) < 820 && yPos.get(i + j) > 150 && yPos.get(i + j) < 570) {
                j++;
                if (yPos.get(i + j) < 550 && yPos.get(i + j) >= 530 && yPos.get(i + j + 1) > 555) {
                  // double uturn = 0;
//                   double yUturn = 0;                  
//                   for(int k = i; k < i + j; k++){
//                      if (yUturn < yPos.get(k))
//                         uturn = ts.get(k);
//                      
//                   } 
                  outbound[count][0] = 3;
//                   outbound[count][1] = uturn;
                  outbound[count][1] = ts.get(i + j + 1);
                  count++;                    
               }
            }
         }
         i += j;
      }
      return count;
   }
   
//    //find in timestamps
//    public static int findIn(List<Double> ts, List<Integer> xPos, List<Integer> yPos, double[][] outbound, int count){     
//            
//       int x1 = 0;
//       int x2 = 0;
//       int y1 = 0;
//       int y2 = 0;
//             
//       for (int i = 0; i < ts.size() - 2; i++){
//          x1 = xPos.get(i);
//          x2 = xPos.get(i+1);
//          y1 = yPos.get(i);
//          y2 = yPos.get(i+1);
//          
//          if (x1 > 600 && x1 < 690 && x2 > 600 && x2 < 690 && y1 >= 530 && y1 <= 550 && y2 > 550) {
//             foraging[count][0] = 4;
//             foraging[count][1] = ts.get(i+1);
//             count++;
//          }
//       }
//       return count;  
//    }
}