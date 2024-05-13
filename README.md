
# HtmlParser.java
HtmlParser.java is a simple crawler and can also analyze the stock values without importing any Math librabry.

## Usage
For Mac
```
javac -cp ".:./jsoup.jar" HtmlParser.java
java -cp ".:./jsoup.jar" HtmlParser {mode} {task} {stock} {start} {end}
```


## Example

Start crawling a webpage and output into a file called "data.csv"
```
java HtmlParser 0
```

Output all the content in data.csv to output.csv in ordered(from day1 to day30)
```
java HtmlParser 1 0
```

Read data.csv and then calculate the "Moving Average" from day2 to day6(included) (these 5 days) to output.csv
```
java HtmlParser 1 1 AAL 2 6
```

```
cat output.csv
output:
AAL,2,6
1.11,2.22,3.33...
```


Read data.csv and then calculate the "Standard Deviation" from day1 to day3(included) to output.csv
```
java HtmlParser 1 2 AAL 1 3
```
```
AAL,1,3
1.11
```


Reading data.csv and then calculate the "top3 Standard Deviation" from day1 to day15(included) to output.csv
```
java HtmlParser 1 3 AAL 1 15
```
```
AAL,AAPL,ABT,1,15
100.11,99.22,98.33
```


Reading data.csv and then calculate the "Regression" from day1 to day30(included) to output.csv
```
java HtmlParser 1 4 AAL 1 30
```

```
AAL,1,30
100.11,99.22 
//intersection,slope
```

