Files Included:
'''''''''''''''

/* Source code for indexing and retrieval */
LSearch.java

/* A sorted (by frequency) list of (term, term_freq pairs) */
term-frq.txt

/* A plot of the resulting Zipfian curve */
zipfianCurve.png

/* Four lists (one per query) each containing at MOST 100 docIDs ranked by score */
rankedDocs.txt

/*A table comparing the total number of documents retrieved per query using Lucene’s
 scoring function vs. using your search engine (index with BM25) from the previous 
 assignment*/

Table:
  +--------+--------+
  | BM25   | Lucene |
  +--------+--------+
Q1|  440   |   872  | [portable operating systems]
  +--------+--------+
Q2|  1579  |  1632  | [code optimization for space efficiency]
  +--------+--------+
Q3|  272   |  1386  | [parallel algorithms]
  +--------+--------+
Q4|  1529  |  1540  | [parallel processor in information retrieval]
  +--------+--------+

Instructions:
External libraries used -
/* For Html tags filtering */
Apache Tika (tika-app-1.11.jar)

1. Execute the program and provide the input and output folder paths
   Output : A list of term, frequency pairs
2. provide Query 
   Output : A list fo 100 documents for the query with their scores
3. input q to exit the program

