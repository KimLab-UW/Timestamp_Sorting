import java.io.*;
import java.util.*;

//outbound duration --> from out to turn or until outbounds switches to inbound
public class TSOutbound{
   private double[][] preOutbound;
   private double[][] robotOutbound;
   private PrintStream output;
   
   //constructor 
   public TSOutbound(double[][] preOutbound, double[][] robotOutbound, PrintStream output) throws FileNotFoundException, IOException {
      this.preOutbound = preOutbound;
      this.robotOutbound = robotOutbound;
      this.output = output; 
   }
   
   public PrintStream findOutbounds() throws IOException {
      sort(preOutbound);
      sort(robotOutbound);
      
      extractOutbounds(preOutbound);
      output.println("-----------------------------------------");
      extractOutbounds(robotOutbound);
      
      return output;
   }
   
   //sorts timestamps in increasing order
   private void sort(double[][] outbound){
      Arrays.sort(outbound, new Comparator<double[]>() {
         public int compare(double[] first, double[] second) {
            if (first[1] > second[1]) {
               return 1; 
            } else return -1;
         }
      });
   }
   
   // extracts outbound intervals
   public void extractOutbounds(double[][] outbound) throws IOException{
      int count = 0;
      
      while(outbound[count][1] == 0) {
         count++;
      }
      
      double outTS = 0;
      double decisionTS = 0;
      
      List<Double> rightOutbound = new ArrayList<>();
      List<Double> leftOutbound = new ArrayList<>();
      List<Double> inOutbound = new ArrayList<>();
      List<Double> rightOutTS = new ArrayList<>();
      List<Double> leftOutTS = new ArrayList<>();
      List<Double> inOutTS = new ArrayList<>();

      
      while (count < outbound.length) {
         if (count > outbound.length - 2) {
            break;
         }
         
         while (outbound[count][0] != 0) {
            count++;
         }
                 
         outTS = outbound[count][1];
         count++;
         decisionTS = outbound[count][1];
         
         if (outbound[count][0] == 1) {
            rightOutbound.add(decisionTS);
            rightOutTS.add(outTS);
         } else if (outbound[count][0] == 2) {
            leftOutbound.add(decisionTS);
            leftOutTS.add(outTS);
         } else if (outbound[count][0] == 3) {
            inOutbound.add(decisionTS);
            inOutTS.add(outTS);
         } else if (outbound[count][0] == 0) {
            count--;
         }
         count++;
      }
            
      output.println("RIGHT OUTBOUND");
      for (int i = 0; i < rightOutTS.size(); i++) {
         output.printf("%.6f\t%.6f\t%.6f\n", rightOutTS.get(i), rightOutbound.get(i), (rightOutbound.get(i) - rightOutTS.get(i)));
      }
      output.println("LEFT OUTBOUND");
      for (int i = 0; i < leftOutTS.size(); i++) {
         output.printf("%.6f\t%.6f\t%.6f\n", leftOutTS.get(i), leftOutbound.get(i), (leftOutbound.get(i) - leftOutTS.get(i)));
      }
      output.println("IN OUTBOUND");
      for (int i = 0; i < inOutTS.size(); i++) {
         output.printf("%.6f\t%.6f\t%.6f\n", inOutTS.get(i), inOutbound.get(i), (inOutbound.get(i) - inOutTS.get(i)));
      }
   }
}