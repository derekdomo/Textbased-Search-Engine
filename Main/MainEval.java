package Main;
/**
 *  Main.MainEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import DataStructure.QryResult;
import QryOperator.*;
import Prf.*;
import RetrievalModel.*;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.*;

public class MainEval {

    public static IndexReader READER;
    public static EnglishAnalyzerConfigurable analyzer =
            new EnglishAnalyzerConfigurable(Version.LUCENE_43);
    static {
        analyzer.setLowercase(true);
        analyzer.setStopwordRemoval(true);
        analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
    }

    //  Create and configure an English analyzer that will be used for
    //  query parsing.
    static String usage = "Usage:  java " + System.getProperty("sun.java.command")
            + " paramFile\n\n";
    //  The index file reader is accessible via a global variable. This
    //  isn't great programming style, but the alternative is for every
    //  query operator to store or pass this value, which creates its
    //  own headaches.
    static Map<Integer, String> externalID = new HashMap<Integer, String>();

    /**
     * Write an error message and exit.  This can be done in other
     * ways, but I wanted something that takes just one statement so
     * that it is easy to insert checks without cluttering the code.
     *
     * @param message The error message to write before exiting.
     */
    public static void fatalError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    /**
     * Get the external document id for a document specified by an
     * internal document id. If the internal id doesn't exists, returns null.
     *
     * @param iid The internal document id of the document.
     * @throws java.io.IOException
     */
    static String getExternalDocid(int iid) throws IOException {
        Document d = MainEval.READER.document(iid);
        return d.get("externalId");
    }

    /**
     * Compare the externalDocID
     *
     * @param i doc ID
     * @param j doc ID
     * @return bigger returns a value >0
     */
    public static int compareExternalDocid(int i, int j) throws IOException {
        if (externalID.get(i).compareTo(externalID.get(j)) > 0) {
            return 1;
        } else return -1;
    }

    /**
     * Finds the internal document id for a document specified by its
     * external id, e.g. clueweb09-enwp00-88-09710.  If no such
     * document exists, it throws an exception.
     *
     * @param externalId The external document id of a document.s
     * @return An internal doc id suitable for finding document vectors etc.
     * @throws Exception
     */
    static int getInternalDocid(String externalId) throws Exception {
        Query q = new TermQuery(new Term("externalId", externalId));
        IndexSearcher searcher = new IndexSearcher(MainEval.READER);
        TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        if (hits.length < 1) {
            throw new Exception("External id not found.");
        } else {
            return hits[0].doc;
        }
    }

    /**
     * parseQuery converts a query string into a query tree.
     *
     * @param qString A string containing a query.
     * @param r       RetrievalModel
     * @return QryOp
     * A query operator with its args
     * @throws java.io.IOException
     */
    static Qryop parseQuery(String qString, RetrievalModel r) throws IOException {

        Qryop currentOp = null;
        Stack<Qryop> stack = new Stack<Qryop>();

        // Add a default query operator to an unstructured query. This
        // is a tiny bit easier if unnecessary whitespace is removed.

        qString = qString.trim();

        if (qString.charAt(0) != '#') {
            if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean) {
                qString = "#or(" + qString + ")";
            } else if (r instanceof RetrievalModelBM25) {
                qString = "#sum(" + qString + ")";
            } else if (r instanceof RetrievalModelIndri)
                qString = "#and(" + qString + ")";
        }
        if (r instanceof RetrievalModelIndri)
            qString = "#and(" + qString + ")";
        // Tokenize the query.

        StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
        String token ;

        // Each pass of the loop processes one token. To improve
        // efficiency and clarity, the query operator on the top of the
        // stack is also stored in currentOp.

        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken();
            if (token.matches("[  ,(\t\n\r]")) {
                // Ignore most delimiters.
            } else
            if (token.equalsIgnoreCase("#and")) {
                currentOp = new QryopSlAnd();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wsum")) {
                currentOp = new QryopSlWSUM();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wand")) {
                currentOp = new QryopSlWAND();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#syn")) {
                currentOp = new QryopIlSyn();
                stack.push(currentOp);
            } else if (token.length() > 6 && token.substring(0, 5).equalsIgnoreCase("#near")) {
                currentOp = new QryopIlNear(Integer.parseInt(token.substring(6)));
                stack.push(currentOp);
            } else if ((token.length() == 5 && token.equalsIgnoreCase("#near"))
                    || (token.length() == 6 && token.substring(0, 5).equalsIgnoreCase("#near"))) {
                //if not the legal format of NEAR, use 1 as argument
                currentOp = new QryopIlNear(1);
                stack.push(currentOp);
            } else if (token.length() > 8 && token.substring(0,7).equalsIgnoreCase("#window")) {
                currentOp = new QryopIlWindow(Integer.parseInt(token.substring(8)));
                stack.push(currentOp);
            } else if ((token.length() == 7 && token.equalsIgnoreCase("#window"))
                    || (token.length() == 8 && token.substring(0, 7).equalsIgnoreCase("#window"))) {
                currentOp = new QryopIlWindow(5);
                stack.push(currentOp);
            }else if (token.equalsIgnoreCase("#or")) {
                currentOp = new QryopSlOr();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#sum")) {
                currentOp = new QryopSlSum();
                stack.push(currentOp);
            } else if (token.startsWith(")")) {
                // Finish current query operator.
                // If the current query operator is not an argument to
                // another query operator (i.e., the stack is empty when it
                // is removed), we're done (assuming correct syntax - see
                // below). Otherwise, add the current operator as an
                // argument to the higher-level operator, and shift
                // processing back to the higher-level operator.
                stack.pop();
                Qryop arg = currentOp;
                if (tokens.hasMoreTokens()) {
                    if (stack.empty()) {
                        if (r instanceof RetrievalModelBM25) {
                            currentOp = new QryopSlSum();
                        } else
                            currentOp = new QryopSlAnd();
                        stack.push(currentOp);
                    } else
                        currentOp = stack.peek();
                } else
                    break;
                currentOp.add(arg);
            } else {

                // NOTE: You should do lexical processing of the token before
                // creating the query term, and you should check to see whether
                // the token specifies a particular field (e.g., apple.title).
                //now process the token to check whether it specifies a particular field
                //if go with the first construction,else go to the second one
                if (currentOp == null) {
                    System.out.println("Error Parsing ");
                    return null;
                }
                //process the weighted version
                boolean weight = false;
                try {
                    Double.valueOf(token);
                    weight = true;
                } catch(Exception e) {
                    weight = false;
                }
                if ( weight ) {
                    if (currentOp instanceof QryopSlWAND) {
                        if (((QryopSlWAND) currentOp).lengthWeight() == ((QryopSlWAND) currentOp).lengthArgs()) {
                            ((QryopSlWAND) currentOp).addWeight(Double.valueOf(token));
                            token = tokens.nextToken();
                        }
                    } else if (currentOp instanceof QryopSlWSUM) {
                        if (((QryopSlWSUM) currentOp).lengthWeight() == ((QryopSlWSUM) currentOp).lengthArgs()) {
                            ((QryopSlWSUM) currentOp).addWeight(Double.valueOf(token));
                            token = tokens.nextToken();
                        }
                    }
                }
                if (token.contains(".")) {
                    String[] temp = token.split("\\.");
                    if (temp.length > 1) {
                        String[] tt = tokenizeQuery(temp[0]);
                        if (tt.length == 0) {
                            if (currentOp instanceof QryopSlWSUM) {
                                ((QryopSlWSUM) currentOp).weights.remove(((QryopSlWSUM) currentOp).weights.size()-1);
                            } else if (currentOp instanceof QryopSlWAND) {
                                ((QryopSlWAND) currentOp).weights.remove(((QryopSlWAND) currentOp).weights.size()-1);
                            }
                            continue;
                        }
                        temp[0] = tt[0];
                        currentOp.add(new QryopIlTerm(temp[0], temp[1]));
                    } else if (temp.length == 1) {
                        String[] tt = tokenizeQuery(temp[0]);
                        if (tt.length == 0) {
                            if (currentOp instanceof QryopSlWAND) {
                                ((QryopSlWAND) currentOp).weights.remove(((QryopSlWAND) currentOp).weights.size()-1);
                            } else if (currentOp instanceof QryopSlWSUM) {
                                ((QryopSlWSUM) currentOp).weights.remove(((QryopSlWSUM) currentOp).weights.size()-1);
                            }
                            continue;
                        }
                        temp[0] = tt[0];
                        currentOp.add(new QryopIlTerm(temp[0]));
                    }
                } else {
                    if (token.equals(" "))
                        continue;
                    String[] tt = tokenizeQuery(token);
                    if (tt.length == 0) {
                        if (currentOp instanceof QryopSlWAND) {
                            ((QryopSlWAND) currentOp).weights.remove(((QryopSlWAND) currentOp).weights.size()-1);
                        } else if (currentOp instanceof QryopSlWSUM) {
                            ((QryopSlWSUM) currentOp).weights.remove(((QryopSlWSUM) currentOp).weights.size()-1);
                        }
                        continue;
                    }
                    token = tt[0];
                    currentOp.add(new QryopIlTerm(token));
                }
            }
        }
        //if current op is not a score operator
        //then add a score operator
        if (r instanceof RetrievalModelBM25) {
            if (!(currentOp instanceof QryopSlSum)) {
                Qryop temp = currentOp;
                currentOp = new QryopSlSum();
                currentOp.add(temp);
            }
        }
        if (currentOp instanceof QryopIl) {
            Qryop temp = currentOp;
            currentOp = new QryopSlScore();
            currentOp.add(temp);
        }
        // A broken structured query can leave unprocessed tokens on the
        // stack, so check for that.
        if (tokens.hasMoreTokens()) {
            System.err.println("Error:  Query syntax is incorrect.  " + qString);
            return null;
        }
        return currentOp;
    }

    /**
     * Print a message indicating the amount of memory used.  The
     * caller can indicate whether garbage collection should be
     * performed, which slows the program but reduces memory usage.
     *
     * @param gc If true, run the garbage collector before reporting.
     */
    public static void printMemoryUsage(boolean gc) {

        Runtime runtime = Runtime.getRuntime();

        if (gc) {
            runtime.gc();
        }

        System.out.println("Memory used:  " +
                ((runtime.totalMemory() - runtime.freeMemory()) /
                        (1024L * 1024L)) + " MB");
    }

    /**
     * For test:
     * Print the query results.
     * <p/>
     * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
     * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
     * PAGE, WHICH IS:
     * <p/>
     * QueryID Q0 DocID Rank Score RunID
     *
     * @param queryID   Original query ID.
     * @param queryName Original query.
     * @param result    Result object generated by {@link QryOperator.Qryop#evaluate(RetrievalModel)}.
     * @throws java.io.IOException
     */
    static void printResults(String queryID, String queryName, QryResult result) throws IOException {
        System.out.println(queryName + ":  ");
        if (result.docScores.scores.size() < 1) {
            System.out.println("\tNo results.");
        } else {
            System.out.println("\t" + result.docScores.scores.size());
            for (int i = 0; i < result.docScores.scores.size(); i++) {
                System.out.print(queryID + "\t" + "Q0" + "\t"
                        + getExternalDocid(result.docScores.getDocid(i))
                        + "\t" + i + "\t"
                        + result.docScores.getDocidScore(i) + "\trun-1" + "\n");
            }
        }

    }

    /**
     * Given a query string, returns the terms one at a time with stopwords
     * removed and the terms stemmed using the Krovetz stemmer.
     * <p/>
     * Use this method to process raw query terms.
     *
     * @param query String containing query
     * @return Array of query tokens
     * @throws java.io.IOException
     */
    static String[] tokenizeQuery(String query) throws IOException {

        TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
        TokenStream tokenStream = comp.getTokenStream();

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        List<String> tokens = new ArrayList<String>();
        while (tokenStream.incrementToken()) {
            String term = charTermAttribute.toString();
            tokens.add(term);
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * @param args The only argument is the path to the parameter file.
     * @throws Exception
     */
    public void Entr(String[] args) throws Exception {

        // must supply parameter file
        if (args.length < 1) {
            System.err.println(usage);
            System.exit(1);
        }

        // read in the parameter file; one parameter per line in format of key=value
        Map<String, String> params = new HashMap<String, String>();
        Scanner scan = new Scanner(new File(args[0]));
        String line;
        do {
            line = scan.nextLine();
            String[] pair = line.split("=");
            params.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan.close();

    /*get the index*/
        if (!params.containsKey("indexPath")) {
            System.err.println("Error: Parameters were missing.");
            System.exit(1);
        }
        // open the index
        READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));
        if (READER == null) {
            System.err.println(usage);
            System.exit(1);
        }

    /*initiate the model*/
        RetrievalModel model = initModel(params);
    /*get the queries*/
        if (!params.containsKey("queryFilePath")) {
            System.err.println("Error: Parameters were missing.");
            System.exit(1);
        }
        Map<String, String> qPairs = new HashMap<String, String>();
        scan = new Scanner(new File(params.get("queryFilePath")));
        do {
            line = scan.nextLine();
            String[] pair = line.split(":");
            qPairs.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan.close();
        if (params.containsKey("fb"))
            if (params.get("fb").equalsIgnoreCase("true")) {
                qPairs = queryExpansion(params, model, qPairs);
            }
        /**
         *  The index is open. Start evaluating queries.
         *  The general pattern is to tokenize the  query term (so that it
         *  gets converted to lowercase, stopped, stemmed, etc), create a
         *  Term node to fetch the inverted list, create a Score node to
         *  convert an inverted list to a score list, evaluate the query,
         *  and print results.
         *
         */
        //evaluate each query and write the result into resultFile
        Qryop qTree;
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));
        try {
            for (Map.Entry<String, String> entry : qPairs.entrySet()) {
                qTree = parseQuery(entry.getValue(), model);
                QryResult result = qTree.evaluate(model);
                if (result.docScores.scores.size() < 1) {
                    writer.write(entry.getKey() + "\t" + "Q0" + "\t"
                            + "dummy\t"
                            + "1\t" + "0\t" + "run-1" + "\n");
                } else {
                    externalID = new HashMap<Integer, String>();
                    for (int i = 0; i < result.docScores.scores.size(); i++) {
                        if (!externalID.containsKey(result.docScores.getDocid(i)))
                            externalID.put(result.docScores.getDocid(i), getExternalDocid(result.docScores.getDocid(i)));
                    }
                    //System.out.println(getInternalDocid("clueweb09-en0004-37-01664"));
                    result.sort();
                    int length;
                    if (result.docScores.scores.size() <= 100)
                        length = result.docScores.scores.size();
                    else
                        length = 100;
                    for (int i = 0; i < length; i++) {
                        int t = i + 1;
                        writer.write(entry.getKey() + "\t" + "Q0" + "\t"
                                + externalID.get(result.docScores.getDocid(i))
                                + "\t" + t + "\t"
                                + result.docScores.getDocidScore(i) + "\trun-1" + "\n");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }


        // Later HW assignments will use more RAM, so you want to be aware
        // of how much memory your program uses.

        printMemoryUsage(false);

    }
    /**
     * QueryExpansion Process
     * */
    static Map<String, String> queryExpansion(Map<String, String> params, RetrievalModel model, Map<String, String> qPairs)
                                throws Exception{
        double weight = Double.valueOf(params.get("fbOrigWeight"));
        double mu = Double.valueOf(params.get("fbMu"));
        int docNum = Integer.parseInt(params.get("fbDocs"));
        int termNum = Integer.parseInt(params.get("fbTerms"));
        Map<String, Map<String,String>> candidateDoc=new HashMap<String, Map<String,String>>();
        //Get the ranked documents
        if (params.containsKey("fbInitialRankingFile")) {
            Scanner scan = new Scanner(new File(params.get("fbInitialRankingFile")));
            String line;
            int count = 0;
            long pre = -1;
            do {
                line = scan.nextLine();
                String[] pair = line.split(" ");
                if (!pair[0].equalsIgnoreCase(String.valueOf(pre))){
                    pre=Long.parseLong(pair[0]);
                    count=0;
                }
                if (count<docNum) {
                    count++;
                    if (candidateDoc.containsKey(pair[0].trim()))
                        candidateDoc.get(pair[0].trim()).put(String.valueOf(getInternalDocid(pair[2].trim())), pair[4].trim());
                    else {
                        candidateDoc.put(pair[0].trim(),new HashMap<String, String>());
                        candidateDoc.get(pair[0].trim()).put(String.valueOf(getInternalDocid(pair[2].trim())), pair[4].trim());
                    }
                }
            } while (scan.hasNext());
            scan.close();
        } else {
            Qryop qTree;
            for (Map.Entry<String, String> entry : qPairs.entrySet()) {
                System.out.println(entry.getValue());
                qTree = parseQuery(entry.getValue(), model);
                QryResult result = qTree.evaluate(model);
                externalID = new HashMap<Integer, String>();
                for (int i = 0; i < result.docScores.scores.size(); i++) {
                    externalID.put(result.docScores.getDocid(i), getExternalDocid(result.docScores.getDocid(i)));
                }
                result.sort();
                int length;
                if (result.docScores.scores.size() <= docNum)
                    length = result.docScores.scores.size();
                else
                    length = docNum;
                for (int i = 0; i < length; i++) {
                    if (!candidateDoc.containsKey(entry.getKey()))
                        candidateDoc.put(entry.getKey(),new HashMap<String, String>());
                    candidateDoc.get(entry.getKey()).
                            put(String.valueOf(result.docScores.getDocid(i)), String.valueOf(result.docScores.getDocidScore(i)));
                }
            }
        }
        System.out.println("Retrieval Complete");
        //Do the Indri Query Expansion
        PrfIndri p = new PrfIndri();
        String leanredfile=params.get("fbExpansionQueryFile");
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(leanredfile)));
        Map<String, String> learned = p.calTerm(candidateDoc, mu, termNum);
        System.out.println("Expansion Calculation Complete");
        for (Map.Entry<String, String> entry : learned.entrySet()) {
            String origin = qPairs.get(entry.getKey());
            if (qPairs.containsKey(entry.getKey()))
                qPairs.put(entry.getKey(), "#Wand( " + String.valueOf(weight) + " #AND(" + origin +
                        ") " + String.valueOf(1 - weight) + " " + entry.getValue() + ")");
            else
                continue;
            writer.write(entry.getKey() + ":\t" + entry.getValue() + "\n");
        }
        System.out.println("Expansion Complete");
        writer.close();
        return qPairs;
    }
    /**
     * Gather the model information
     * */
    static RetrievalModel initModel(Map<String, String> params) {
        RetrievalModel model;
        if (!params.containsKey("retrievalAlgorithm")) {
            model = new RetrievalModelUnrankedBoolean();
        }
        if (params.get("retrievalAlgorithm").equalsIgnoreCase("RankedBoolean")) {
            model = new RetrievalModelRankedBoolean();
        } else if (params.get("retrievalAlgorithm").equalsIgnoreCase("UnrankedBoolean")) {
            model = new RetrievalModelUnrankedBoolean();
        } else if (params.get("retrievalAlgorithm").equalsIgnoreCase("BM25")) {
            model = new RetrievalModelBM25();
        } else if (params.get("retrievalAlgorithm").equalsIgnoreCase("letor")){
            model = new RetrievalModelLearnToRank();
        } else {
            model = new RetrievalModelIndri();
        }
    /*set the parameters*/
        if (model instanceof RetrievalModelBM25) {
            model.setParameter("BM25:k_1", params.get("BM25:k_1"));
            model.setParameter("BM25:b", params.get("BM25:b"));
            model.setParameter("BM25:k_3", params.get("BM25:k_3"));
        } else if (model instanceof RetrievalModelIndri) {
            model.setParameter("Indri:mu", params.get("Indri:mu"));
            model.setParameter("Indri:lambda", params.get("Indri:lambda"));
        } else if (model instanceof RetrievalModelLearnToRank) {
            model.setParameter("trainingQueryFile", params.get("letor:trainingQueryFile")));
            model.setParameter("trainingQrelsFile", params.get("letor:trainingQrelsFile"));
            model.setParameter("trainingFeatureVectorsFile", params.get("letor:trainingFeatureVectorsFile"));
            model.setParameter("pageRankFile", params.get("letor:pageRankFile"));
            model.setParameter("featureDisable", params.get("letor:featureDisable"));
            model.setParameter("svmRankLearnPath", params.get("letor:svmRankClassifyPath"));
            model.setParameter("svmRankClassifyPath", params.get("letor:svmRankClassifyPath"));
            //model.setParameter("");
        }
        return model;
    }
 }
