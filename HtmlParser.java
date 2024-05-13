import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HtmlParser{
    private Map<String,Integer> stockOderMap;
    private CircularLinkedList<String> list;
    private List<String> stockNameList;

    public HtmlParser(){
        this.stockOderMap = new HashMap<>();
        this.list = new CircularLinkedList<>();
        this.stockNameList = new ArrayList<>();
    }
    public static void main(String[] args){
        //command: java HtmlParser {mode} {task} {stock} {start} {end}
        WebCrawler webCrawler = WebCrawler.getWebCrawlwerInstance();
        HtmlParser htmlParser = new HtmlParser();
        StockAnalyzer analyzer = new StockAnalyzer();
        CommandController controller = new CommandController();
        controller.determineMode(args);
        String mode = controller.getMode();
        String pathName = "";
        //Variables for stockAnalyzer usage
        String searchName = "";
        String start = "";
        String end = "";
        List<String> perStackValue30DaysList = new ArrayList<>();
        List<Double> analysisResultList = new ArrayList<>();
        int searchIndex = 0;

        /*6 identities: "CRAWL", "OUTPUT_STOCK_DATA_IN_ORDER" 
         * "MOVING_AVERGAE", "STANDARD_DEVIATION", 
         * "TOP3_STANDARD_DEVIATION", "REGRESSION_LINE"
        */
        if(mode.equals("CRAWL")){
            pathName = "data.csv";
            //Call webCrawler to crawl
            webCrawler.getTableInfo("https://pd2-hw3.netdb.csie.ncku.edu.tw/");
            //Write all the information graped by webcrawler into the file "Stock30Days" 
            String day = webCrawler.getDay();
            day = day.substring(3);
            String stockName = webCrawler.getStockName();
            String stockValue = webCrawler.getStockValue();
            //String output = "DAY" + "," + stockName + "\n" + day + "," + stockValue;
            Path path = Paths.get(pathName);
            boolean fileExists = Files.exists(path);
            StringBuilder output = new StringBuilder("");
            //System.out.println(fileExists);
            /////if we echo "" > data.csv, the size of data.csv would not be 0
            if(fileExists){
                try{
                    if(Files.size(path)==0){
                        output.append("DAY" + "," + stockName + "\n" + day + "," + stockValue+"\n");
                    }
                    else{
                        output.append(day+","+stockValue+"\n");
                    }
                } catch(IOException e) { e.printStackTrace();}
            }
            else{
                output.append("DAY" + "," + stockName + "\n" + day + "," + stockValue +"\n");
            }
            htmlParser.fileWriter(output.toString(), pathName);
        }
        else if(mode.equals("OUTPUT_STOCK_DATA_IN_ORDER")){
            htmlParser.fileReader();
            List<List<String>> allList = htmlParser.list.getAllBlockList();
            StringBuilder output = new StringBuilder("");
            //access to the stock Name list
            for(int i=0; i<htmlParser.stockNameList.size(); i++){
                if(i>0){
                    output.append(",");
                }
                output.append(htmlParser.stockNameList.get(i));
            }
            output.append("\n");
            //access to the stock value list<list<string>>
            for(int i=0; i<allList.size(); i++){
                if(i>0){
                    output.append("\n");
                }
                for(int j=0; j<allList.get(i).size(); j++){
                    if(j>0){
                        output.append(",");
                    }
                    output.append(allList.get(i).get(j));
                }    
            }
            pathName = "output.csv";
            htmlParser.fileWriter(output.toString(), pathName);
        }
        else{//Handle the variable for the mathematic analysis of stock value 
            htmlParser.fileReader();
            searchName = args[2];
            searchIndex = htmlParser.stockOderMap.get(searchName);
            perStackValue30DaysList = htmlParser.list.getStockValue30Days(searchIndex);
            start = args[3];
            end = args[4];
            pathName = "output.csv";
        }
        
        //Anaylze stock mathematically
        if(mode.equals("MOVING_AVERGAE")){
            analysisResultList = analyzer.calculateMovingAverage(perStackValue30DaysList, 5, start, end);
            //Output handling
            StringBuilder output = new StringBuilder("");
            output.append(searchName + "," + start + "," + end + "\n");
            for(int i=0; i<analysisResultList.size(); i++){
                if(i>0){
                    output.append(",");
                }
                output.append(analyzer.omitDecimalZero(String.valueOf(analysisResultList.get(i))));
            }
            htmlParser.fileWriter(output.toString()+"\n", pathName); 
        }
        else if(mode.equals("STANDARD_DEVIATION")){
            double result = analyzer.calculateStandardDeviation(perStackValue30DaysList, start, end);
            //Output handling
            StringBuilder output = new StringBuilder("");
            output.append(searchName + "," + start + "," + end + "\n");
            output.append(analyzer.omitDecimalZero(String.valueOf(result)));
            htmlParser.fileWriter(output.toString()+"\n", pathName); 
        }
        else if(mode.equals("TOP3_STANDARD_DEVIATION")){
                double[] top3 = new double[3];
                String[] top3Name = new String[3];
            
                double standardDeviation;
                
            for(Map.Entry<String, Integer> entry : htmlParser.stockOderMap.entrySet()){
                searchName = entry.getKey();
                searchIndex = entry.getValue().intValue();
                perStackValue30DaysList = htmlParser.list.getStockValue30Days(searchIndex);
                standardDeviation = analyzer.calculateStandardDeviation(perStackValue30DaysList, start, end);

                for(int i=0; i<3; i++){
                    if(standardDeviation > top3[i]){
                        //swap 
                        double tempD = standardDeviation;
                        standardDeviation = top3[i] ;
                        top3[i] = tempD;

                        String tempS = searchName;
                        searchName = top3Name[i];
                        top3Name[i] = tempS;
                    }
                }
            }
            StringBuilder output = new StringBuilder("");
            for(int i=0;i<3;i++){
                output.append(top3Name[i]+",");
            }
            output.append(start+","+end+"\n");
            for(int i=0;i<3;i++){
                if(i>0){
                    output.append(",");
                }
                output.append(analyzer.omitDecimalZero(String.valueOf(top3[i])));
            }
            htmlParser.fileWriter(output.toString()+"\n", pathName);
        }
        else if(mode.equals("LINEAR_REGRESSION")){
            double [] result = new double[2];//[0] is slope
            result = analyzer.calculateLinearRegression(perStackValue30DaysList, start, end);
            StringBuilder output = new StringBuilder("");
            output.append(searchName + "," + start + "," + end + "\n");
            output.append(analyzer.omitDecimalZero(String.valueOf(result[0])));
            output.append(",");
            output.append(analyzer.omitDecimalZero(String.valueOf(result[1])));
            htmlParser.fileWriter(output.toString()+"\n", pathName);
        }    
        //htmlParser.list.display();
    }

    //Output 30 days stock value into the file
    public void fileWriter(String output, String pathName){
        try {
            File file = new File(pathName);
            if (!file.exists()) {
                file.createNewFile();
                
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(output);// Open the file in append mode
            } catch (IOException e) { e.printStackTrace();} // Handle the IOException from BufferedWriter
        } catch (IOException e) { e.printStackTrace();}// Handle the IOException from File operations
    }

    
    public void fileReader(){
        //Input file
        String csvFile = "data.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String eachLine = "";
            String firstRow = "";
            int stockLength = 0;
            if ((firstRow=br.readLine()) != null) {
                String[] stockName = firstRow.split(",");
                /*Creating a stock name to index order mapping 
                 *without creating to many objects Integer i and String stockname
                 *and without calculating too many times of stockName length
                */
                Integer i = 1;
                String stockname = "";
                stockLength = stockName.length;
                
                for(; i<stockLength; i++){
                    stockname = stockName[i.intValue()];
                    //record the name of all the stocks
                    stockNameList.add(stockname);
                    //create stockName -> print index order
                    if(!stockOderMap.containsKey(stockName)){
                        stockOderMap.put(stockname, i);
                    }
                }
            }
            ///////handle if duplicate/////////////
            //Since the first line has been already read, it then read from the second line
            //And store all the line ("stockValue only") into the circular nodes
            final int len = stockLength;
            String element = "";
            String day = "";
            while ((eachLine = br.readLine()) != null){
                    String[] elements = eachLine.split(",") ;
                    day = elements[0];
                    //store day in to the node
                    List<String> perDayList = new ArrayList<>();
                    //System.out.println(day);
                    for(int i=1; i<len; i++){
                        element = elements[i];
                        perDayList.add(element);
                        //append all the element in each line to the list on a node
                    }
                    //list.insert(day, perDayList);
                    //System.out.println(eachLine);
                    if(!list.findDayBlock(day)){//if not found we create new block
                            list.insert(day, perDayList);           
                    }
            }
        } 
        catch (IOException e) { e.printStackTrace();}
    }
}

final class WebCrawler{   
    //For Singleton use
    private static WebCrawler instance;
    private String day = "";
    /*To save all element in HTML via append method in the for-loop
     *sb1 is for stockName; sb2 is for stockValue*/
    private StringBuilder sb1 = new StringBuilder("");
    private StringBuilder sb2 = new StringBuilder("");

    private WebCrawler(){
    }
    //Make a Singleton(every object with different names points to the same object!!)
    public static WebCrawler getWebCrawlwerInstance(){
        if(instance == null) {
            instance = new WebCrawler();
        }
        return instance;
    }

    public void getTableInfo(String URL){
        try{
            //Fetch the HTML code
            Document doc = Jsoup.connect(URL).get();
            //day is in the title, we .first to grab the "element" then change to String type
            day = doc.select("title").first().text();
            //Find the table, which is first on the HTML code
            Element table = doc.select("table").first();
            //Find the header row, which is the first row of the table
            Element headerRow = table.select("tr").first();
            //Select all <th> elements within the header row
            Elements headers = headerRow.select("th");
            //Select all rows below the header row
            Elements rowsBelowHeaders = headerRow.parent().select("tr:gt(0)");
            //Header text is stockName, and we initialize here to avoid to many objects created in the loop
            String stockName="";
            String stockValue="";
            //Iterate the element on one rows
            for (Element header : headers){
                stockName = header.text();
                //get the index of current header
                int columnIndex = header.elementSiblingIndex();
                //grab the column string value depend on its header's index which is the same as column index in this table
                for (Element rowBelowHeader : rowsBelowHeaders){
                    /*Make sure we select the "td" in each 
                     *Grab the column string value depend on its header's index which 
                     * is the same as column index in this table*/
                    stockValue = rowBelowHeader.select("td").get(columnIndex).text();
                    sb1.append(stockName+",");
                    sb2.append(stockValue+",");
                }
            }
        //remove the last comma
        sb1 = sb1.deleteCharAt(sb1.length() - 1);
        sb2 = sb2.deleteCharAt(sb2.length() - 1);
        } catch (IOException e) { e.printStackTrace();}
    }

    public String getDay(){
        return this.day;
    } 
    public String getStockName(){
        return sb1.toString();
    }
    public String getStockValue(){
        return sb2.toString();
    }
}

//command: java HtmlParser {mode} {task} {stock} {start} {end}
class CommandController{
    private String mode = "";

    public void determineMode(String[] args){
        //command length can only be 1, 2, or 5
        String taskNumber = "";
        
        if(args.length == 1){
            //args[0] = 0
            this.mode = "CRAWL";
        }
        else if(args.length == 2){
            //args[0] = 1, args[1] = 0, (HtmlParser 1 0)
            this.mode = "OUTPUT_STOCK_DATA_IN_ORDER";
            if(!args[1].equals("0"))
                this.mode = "In OUTPUT_STOCK_DATA_IN_ORDER mode, task number should be 1";
        }
        else if(args.length == 5){
                taskNumber = args[1];
                //args[1] = 1 (HtmlParser 1 1 AAPL 1 30)
                if(taskNumber.equals("1"))
                    this.mode = "MOVING_AVERGAE";
                //args[1] = 2
                else if(taskNumber.equals("2"))
                    this.mode = "STANDARD_DEVIATION";
                //args[1] = 3
                else if(taskNumber.equals("3"))
                    this.mode = "TOP3_STANDARD_DEVIATION";
                //args[1] = 4
                else if(taskNumber.equals("4"))
                    this.mode = "LINEAR_REGRESSION"; 
                else
                    mode = "Wrong task number in STOCK ANALYSIS mode";
        }
        else{
            mode = "Wrong args length";
        }
    }

    public String getMode(){
        return mode;
    }
}

class StockAnalyzer{
    //tailing zero
    public String omitDecimalZero(String str){
        //To avoid tailing 120 to 12
        if(str.contains(".")) {
            if (str.endsWith(".00")) {
                // Turn "12.00" -> "12"
                str = str.substring(0, str.length() - 3);
            }
            else if (str.endsWith(".0")) {
                // Turn "12.0" -> "12"
                str = str.substring(0, str.length() - 2);
            }
            else if (str.endsWith("0")) {
                // Turn "12.20" -> "12.2"
                str = str.substring(0, str.length() - 1);
            }  
        }
        return str;
    }

    public int abs(int x) {
        return (x < 0) ? -x : x;
    }
    public double abs(double x) {
        return (x < 0) ? -x : x;
    }

    public double pow(double base, int exponent){
        if(exponent == 0){
         return 1.0;
        }
        double result = 1.0;
        //Caculate the positive value first, if the exponent<0, we 1 over the positive value then
        int power = abs(exponent);
        while(power > 0){
            if((power & 1)== 1){
                result *= base;
            }
            //Since whenever pow == 2, value = itself*itself
            base *= base;
            power >>= 1;
        }
        return  (exponent < 0) ? 1/result : result;
     }

    public double sqrt(double number){
        if (number < 0) {
            throw new IllegalArgumentException("Cannot compute square root of negative number");
        }
        if(number == 0 || number == 1){
            return number;
        }
        //Change the precision to be more precise
        double precision = 1e-12;
        //Initial guess for the square root (e.g., half of the input value).
        double guess = number/2.0;
        /*We continue the iteration until the difference between the square of the
         *guess and the input value is within a specified precision threshold.*/
        while (abs(guess * guess - number) > precision){
            /*Iteratively refine the guess using the Newton-Raphson method
             *Consideration for solving x^2=a, then x=? (positive)
             *Let initial guess x0 f(x0) = x0^2 - a, then f'(x0) = 2x0
             *By Newton-Raphson method, x1 = x0 - f(x0)/f'(x0)
             *So x1 = x0 - (x0^2 - a)/2x0 = x0 - x0/2 + a/2x0
             *root = (1/2)(x0+a/x0) */
            guess = (guess + number / guess) / 2.0;
        }
        return guess;
    }

    public List<Double> calculateMovingAverage(List<String> data, final int windowSize, String startS, String endS) {
        if(windowSize<=0) {System.err.println("Wrong windowSize"); return null;}
        double[] window = new double[windowSize];
        int n = 0;
        int insert = 0;
        double sum = 0.0;
        int start = Integer.parseInt(startS);
        int end = Integer.parseInt(endS);
        List<Double> stock30DaysMovingList = new ArrayList<>();
        //Start from starting day's index
        for (int i=start-1; i < end; i++) {
            if (n < window.length) {
                n++;
            } else {
                sum -= window[insert];
            }
            //Turn String into Double first, and then ture Double into double. Double doubleObject = Double.valueOf(str);
            double datum = Double.valueOf(data.get(i)).doubleValue();
            //System.out.println(datum);
            sum += datum;
            window[insert] = datum;
            insert = (insert + 1) % window.length;
            //why
            if (i-start >= windowSize-2) { 
                double mean = sum / windowSize;
                mean = round(mean, 2);
                //System.out.println(mean);
                stock30DaysMovingList.add(mean);
            }
        }
        return stock30DaysMovingList;
    }
    
    public double calculateStandardDeviation(List<String> data, String startS, String endS){
        double mean = 0.0;
        double sumSquareDiff= 0.0;
        double standardDeviation= 0.0;
        int start  = Integer.parseInt(startS);
        int end = Integer.parseInt(endS);
        int periodSize = end - start + 1;
        // i=(Integer.parseInt(start)-1) means starting from the "starting day's index"
        //Caculate mean value
        for(int i=start-1; i<end; i++){
            double datum = Double.valueOf(data.get(i)).doubleValue();
            mean += datum;
        }
        mean /= periodSize; 
        //Caculate sumSquareDiff
        for(int i=start-1; i<end; i++){
            double datum = Double.valueOf(data.get(i)).doubleValue();
            sumSquareDiff += pow(abs(datum-mean), 2);
        }
        //Caculate standardDeviation
        standardDeviation = sqrt(sumSquareDiff/(double)(periodSize-1));
        standardDeviation = round(standardDeviation, 2);
        return standardDeviation;
    }

    public double[] calculateLinearRegression(List<String> stockValuedata, String startS, String endS){
        //x is time, y is stockValue 
        double slope;
        double intercept;
        double start = Double.parseDouble(startS);
        double end = Double.parseDouble(endS);
        double sumX = 0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXX = 0.0;
        int periodSize = Integer.parseInt(endS) - Integer.parseInt(startS) + 1;
        double n = (double)periodSize;// n is time period, the number of the serie
        //Get mean X (sum of series with d = 1.0, and sum of sqare series)
        //Calculate sum of squares of differences
        sumX = (n * (2*start + (n-1) * 1))/2;
        //Sum of square series, n->m = 1->m - 1->(n-1) 
        sumXX = end * (end+1) * (2*end+1)/ 6 - (start-1) * ((start-1)+1) * (2*(start-1)+1) / 6;
        //Get the sum of X*Y and sum of Y i nthe smae time
        double stockValuedatum;
        for(int i=(Integer.parseInt(startS)-1) ;i<Integer.parseInt(endS);i++){
            //stockValuedatum = stockValuedata.get(i);
            stockValuedatum = Double.parseDouble(stockValuedata.get(i));
            sumY += stockValuedatum;
            sumXY += start*stockValuedatum;//x*y
            start++;
        }

        slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        intercept = (sumY - slope * sumX) / n;
        slope = round(slope, 2);
        intercept = round(intercept, 2);
        return new double[]{slope, intercept};
    } 
    
    public double round(double value, int decimalPlaces) {
        // Check if the value is negative
        boolean isNegative = value < 0;
        // Get the absolute value
        value = abs(value);
        // Calculate the factor to multiply by to shift the decimal point
        double factor = pow(10, decimalPlaces);
        // Multiply by the factor to shift the decimal point, add 0.5 to round up, subtract 0.5 to round down
        value = value * factor + 0.5;
        // Convert to integer to truncate the decimal part
        int intValue = (int) value;
        // Divide by the factor to shift the decimal point back
        double result = intValue / factor;
        // If the original value was negative, make the result negative
        if (isNegative) {
            result *= -1;
        }
        return result;
    }
}

//Node class for the circular linked list
class Node<T> {
    String day;
    List<String> stockList;
    Node<T> next;

    public Node(String day, List<String> stockValue) {
        this.day = day;
        this.next = null;
        this.stockList = stockValue;
    }
}

//CircularLinkedList includes insert(head adding is also allowed) node method
class CircularLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;

    public CircularLinkedList() {
        head = null;
        tail = null;
    }
    // Method to insert a node at the end of the circular linked list
    public void insert(String day, List<String> stockValue) {
        Node<T> newNode = new Node<>(day, stockValue);
        if (head == null) {
            head = newNode;
            tail = newNode;
            newNode.next = head; // Make it circular
        } 
        else {
            tail.next = newNode;
            tail = newNode;
            tail.next = head; // Make it circular
        }
    }

    // Method to print the circular linked list
    public void display() {
        if (head == null) {
            System.out.println("Circular Linked List is empty");
            return;
        }
        Node<T> current = head;
        do {
            System.out.print(current.day + "\n Value: " + current.stockList + "\n");
            current = current.next;
        } while (current != head);
    }
    
    public boolean findDayBlock(String day) {
        if (head == null) {
            return false;
        }
        Node<T> current = head;
        do {
            current = current.next;
            if(current.day.equals(day)){
                return true;
            }
        } while (current != head);
        return false;
    }

    public List<String> getStockValue30Days(int searchIndex){
        if (head == null) {
            System.out.println("Circular Linked List is empty");
            return null;
        }
        //If the head is not empty, then we start to search
        Node<T> current = head;
        List<String> perStock30DaysList = new ArrayList<>();
        //Start from day1 block
        do {
            current = current.next;
        } while (!current.day.equals("1"));
        //End in 1 block
        do {
            //The real searching index is searchIndex-1, since index of list start from 0
            perStock30DaysList.add(current.stockList.get(searchIndex-1));
            current = current.next;//now it is pointing to day2 block
        } while (!current.day.equals("1"));
        return perStock30DaysList;
    }

    public List<List<String>> getAllBlockList() {
        if (head == null) {
            System.out.println("Circular Linked List is empty");
            return null;
        }
        //If the head is not empty, then we start to search
        Node<T> current = head;
        List<List<String>> all30BlockList = new ArrayList<>();
        //Start from day1 block
        do {
            current = current.next;
        } while (!current.day.equals("1"));
        //End in 1 block
        do {
            //add all 30 days lists into a list in oder(D1->D30)
            all30BlockList.add(current.stockList);
            current = current.next;//now it is pointing to day2 block
        } while (!current.day.equals("1"));

        return all30BlockList;
    }
}