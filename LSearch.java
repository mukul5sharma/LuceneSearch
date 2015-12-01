import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
//Apache Tika
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
//Apache Tika

/**
 * To create Apache Lucene index in a folder and add files into this index based
 * on the input of the user.
 */
public class LSearch {
    private static Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
    private static Analyzer sAnalyzer = new SimpleAnalyzer(Version.LUCENE_47);

    private IndexWriter writer;
    private ArrayList<File> queue = new ArrayList<File>();

    //Function to sort the term-frequency pairs according to frequency
    private static Map<String, Integer> sortByfrq(Map<String, Integer> unsortMap) {

        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>()
        {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
            Map.Entry<String, Integer> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    // Function to check if it the token is a number
    public static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    //Main Function
    public static void main(String[] args) throws IOException {
        System.out
                .println("Enter the FULL path where the index will be created: (e.g. /Usr/index or c:\\temp\\index)");

        String indexLocation = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String s = br.readLine();
        //String c = "/home/mukul/IdeaProjects/LuceneSearch/output";

        LSearch indexer = null;
        try {
            indexLocation = s;
            indexer = new LSearch(s);
        } catch (Exception ex) {
            System.out.println("Cannot create index..." + ex.getMessage());
            System.exit(-1);
        }

        // ===================================================
        // read input from user until he enters q for quit
        // ===================================================
        while (!s.equalsIgnoreCase("q")) {
            try {
                System.out
                        .println("Enter the FULL path to add into the index (q=quit): (e.g. /home/mydir/docs or c:\\Users\\mydir\\docs)");
                System.out
                        .println("[Acceptable file types: .xml, .html, .html, .txt]");
                s = br.readLine();
                //s = "/home/mukul/IdeaProjects/LuceneSearch/input/cacm";

                if (s.equalsIgnoreCase("q")) {
                    break;
                }

                // try to add file into the index
                indexer.indexFileOrDirectory(s);
            } catch (Exception e) {
                System.out.println("Error indexing " + s + " : "
                        + e.getMessage());
            }
        }

        // ===================================================
        // after adding, we always have to call the
        // closeIndex, otherwise the index is not created
        // ===================================================
        indexer.closeIndex();

        //====================================================
        //Code to read index and create a list of all the terms
        // with there frequencies
        //====================================================
        IndexReader myReader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
        int count = 0;
        Fields fields = MultiFields.getFields(myReader);
        HashMap<String, Integer> uniqueTerms = new HashMap<String, Integer>();

        if (fields != null) {
            //System.out.println("inside fields");

            // Extract terms from fields
            Terms terms = fields.terms("contents");
            if (terms != null) {
                //System.out.println("inside terms");
                //System.out.println("inside terms"+terms.getSumTotalTermFreq());
                TermsEnum iterator = terms.iterator(null);
                BytesRef byteRef = null;
                while ((byteRef = iterator.next()) != null) {
                    //System.out.println("inside while");
                    String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
                    if(!isNumeric(term)) {
                        //if(!term.equals("html") && !term.equals("pre")){
                            // System.out.println(term);
                            count++;
                            Term termInstance = new Term("contents", term);
                            long termFreq = myReader.totalTermFreq(termInstance);
                            int ntf = (int)termFreq;
                            uniqueTerms.put(term, ntf);
                        //}
                    }
                }
            }
        }
        //System.out.println(count);

        //Sort the values according to the frequencies
        Map<String, Integer> sortedMap = sortByfrq(uniqueTerms);

        //Print the sorted term-frequency pairs
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            System.out.println(entry.getKey()
                    + "  " + entry.getValue());
        }

        // =========================================================
        // Now search
        // =========================================================
        s = "";
        while (!s.equalsIgnoreCase("q")) {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
                    indexLocation)));
            IndexSearcher searcher = new IndexSearcher(reader);
            TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
            try {
                System.out.println("Enter the search query (q=quit):");
                s = br.readLine();
                if (s.equalsIgnoreCase("q")) {
                    break;
                }

                Query q = new QueryParser(Version.LUCENE_47, "contents",
                        sAnalyzer).parse(s);
                searcher.search(q, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;

                // 4. display results
                System.out.println("Found " + hits.length + " hits.");
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = searcher.doc(docId);
                    System.out.println((i + 1) + ". " + d.get("path")
                            + " score=" + hits[i].score);
                }
                // 5. term stats --> watch out for which "version" of the term
                // must be checked here instead!
                Term termInstance = new Term("contents", s);
                long termFreq = reader.totalTermFreq(termInstance);
                long docCount = reader.docFreq(termInstance);
                System.out.println(s + " Term Frequency " + termFreq
                        + " - Document Frequency " + docCount);

            } catch (Exception e) {
                System.out.println("Error searching " + s + " : "
                        + e.getMessage());
                break;
            }

        }


    }

    /**
     * Constructor
     *
     * @param indexDir
     *            the name of the folder in which the index should be created
     * @throws java.io.IOException
     *             when exception creating index.
     */
    LSearch(String indexDir) throws IOException {

        FSDirectory dir = FSDirectory.open(new File(indexDir));

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47,
                sAnalyzer);

        writer = new IndexWriter(dir, config);
    }

    /**
     * Indexes a file or directory
     *
     * @param fileName
     *            the name of a text file or a folder we wish to add to the
     *            index
     * @throws java.io.IOException
     *             when exception
     */
    public void indexFileOrDirectory(String fileName) throws IOException {
        // ===================================================
        // gets the list of files in a folder (if user has submitted
        // the name of a folder) or gets a single file name (is user
        // has submitted only the file name)
        // ===================================================
        addFiles(new File(fileName));

        int originalNumDocs = writer.numDocs();
        for (File f : queue) {
            FileReader fr = null;
            try {
                Document doc = new Document();

                // ===================================================
                // add contents of file
                // ===================================================
                fr = new FileReader(f);

                //------------------------------------------------------
                //Code to procee text and filter out html tags
                //using apache Tika library
                //------------------------------------------------------
                ContentHandler contenthandler = new BodyContentHandler();
                InputStream is = null;
                try {
                    is = new FileInputStream(f);
                    Metadata metadata = new Metadata();
                    Parser parser = new AutoDetectParser();
                    parser.parse(is, contenthandler, metadata, new ParseContext());
                    //System.out.println(contenthandler.toString());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (is != null) is.close();
                }

                doc.add(new TextField("contents", contenthandler.toString(), Field.Store.YES));
                doc.add(new StringField("path", f.getPath(), Field.Store.YES));
                doc.add(new StringField("filename", f.getName(),
                        Field.Store.YES));

                writer.addDocument(doc);

                //System.out.println(doc);
                //System.out.println(writer);
                //System.out.println("Added: " + f);
            } catch (Exception e) {
                System.out.println("Could not add: " + f);
            } finally {
                fr.close();
            }
        }

        int newNumDocs = writer.numDocs();
        System.out.println("");
        System.out.println("************************");
        System.out
                .println((newNumDocs - originalNumDocs) + " documents added.");
        System.out.println("************************");

        queue.clear();
    }

    private void addFiles(File file) {

        if (!file.exists()) {
            System.out.println(file + " does not exist.");
        }
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                addFiles(f);
            }
        } else {
            String filename = file.getName().toLowerCase();
            // ===================================================
            // Only index text files
            // ===================================================
            if (filename.endsWith(".htm") || filename.endsWith(".html")
                    || filename.endsWith(".xml") || filename.endsWith(".txt")) {
                queue.add(file);
            } else {
                System.out.println("Skipped " + filename);
            }
        }
    }

    /**
     * Close the index.
     *
     * @throws java.io.IOException
     *             when exception closing
     */
    public void closeIndex() throws IOException {
        writer.close();
    }
}