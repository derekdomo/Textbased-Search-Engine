package Main;



import DataStructure.QryResult;
import QryOperator.Qryop;
import RetrievalModel.RetrievalModelBM25;
import RetrievalModel.RetrievalModelIndri;
import RetrievalModel.RetrievalModelLearnToRank;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Collections;
import java.util.Comparator;
/**
 * Learn to Rank:
 *  Features including
 *          BM
 *      1    title
 *      2    body
 *      3    inlink
 *      4    url
 *          Indri
 *      5    title
 *      6    body
 *      7    inlink
 *      8    url
 *          Overlap
 *      9    title
 *      10   body
 *      11   inlink
 *      12   url
 *
 *      13  spam
 *      14  depth
 *      15  wiki
 *      16  tf
 *      17  pagerank
 *      18  # of inlinks
 */
public class LearnToRank {
    static RetrievalModelLearnToRank r;
    static RetrievalModelBM25 bm;
    static RetrievalModelIndri ind;
    static ArrayList<Map.Entry<String,String>> qPairs;
    static ArrayList<Map.Entry<String, String>> testPairs;
    static Map<String, Map<Integer, Integer>> qDocPair;
    static Map<String, Map<Integer, Map<String, Double>>> qDocFeature;
    static ArrayList<String> qID;
    static Map<Integer, Double> pagerank;
    static String featureFile;
    static DocLengthStore dls;
    static Map<String, Map<String, Double>> resultMap;
    static ArrayList<String> disableFeature;
    static double minBodyBM = Double.MAX_VALUE;
    static double maxBodyBM = 0.0;
    static double minTitleBM = Double.MAX_VALUE;
    static double maxTitleBM = 0.0;
    static double minUrlBM = Double.MAX_VALUE;
    static double maxUrlBM = 0.0;
    static double minInlinkBM = Double.MAX_VALUE;
    static double maxInlinkBM = 0.0;
    static double minBodyInd = Double.MAX_VALUE;
    static double maxBodyInd = 0.0;
    static double minTitleInd = Double.MAX_VALUE;
    static double maxTitleInd = 0.0;
    static double minUrlInd = Double.MAX_VALUE;
    static double maxUrlInd = 0.0;
    static double minInlinkInd = Double.MAX_VALUE;
    static double maxInlinkInd = 0.0;
    static double minSpam = Double.MAX_VALUE;
    static double maxSpam = 0.0;
    static double minOverlapTitle = Double.MAX_VALUE;
    static double maxOverlapTitle = 0.0;
    static double minOverlapBody = Double.MAX_VALUE;
    static double maxOverlapBody = 0.0;
    static double minOverlapUrl = Double.MAX_VALUE;
    static double maxOverlapUrl = 0.0;
    static double minOverlapInlink = Double.MAX_VALUE;
    static double maxOverlapInlink = 0.0;
    static double minDepth = Double.MAX_VALUE;
    static double maxDepth = 0.0;
    static double maxTf = 0.0;
    static double minTf = Double.MAX_VALUE;
    static double maxPR = 0.0;
    static double minPR = Double.MAX_VALUE;
    static double maxInlink = 0.0;
    static double minInlink = Double.MAX_VALUE;
    static double countTf = 0;
    static double inlink = 0;
    static String execPath;
    static String modelOutputFile;
    static String c;
    static String testFeatureFile;
    static String classifyPath;
    static String finalScoreFile;
    static String trecEvalOutputPath;

    /**
     * Class Construction
     * */
    public static void initPara(RetrievalModelLearnToRank model, Map<String, String> params) throws Exception {
        r = model;
        dls = new DocLengthStore(MainEval.READER);
        bm = new RetrievalModelBM25();
        ind = new RetrievalModelIndri();
        MainEval.externalID = new HashMap<Integer, String>();
        bm.setParameter("BM25:k_1", r.k1);
        bm.setParameter("BM25:k_3", r.k3);
        bm.setParameter("BM25:b", r.b);
        ind.setParameter("Indri:mu", r.mu);
        ind.setParameter("Indri:lambda", r.lambda);
        execPath = params.get("letor:svmRankLearnPath");
        modelOutputFile = params.get("letor:svmRankModelFile");
        featureFile = params.get("letor:trainingFeatureVectorsFile");
        testFeatureFile  = params.get("letor:testingFeatureVectorsFile");
        classifyPath = params.get("letor:svmRankClassifyPath");
        finalScoreFile = params.get("letor:testingDocumentScores");
        trecEvalOutputPath = params.get("trecEvalOutputPath");

        disableFeature = new ArrayList<String>();
        c = params.get("letor:svmRankParamC");
        Map<String, String> qPair = new HashMap<String, String>();
        Map<String, String> testPair = new HashMap<String, String>();
        qDocPair = new HashMap<String, Map<Integer, Integer>>();
        qDocFeature = new HashMap<String, Map<Integer, Map<String, Double>>>();
        pagerank = new HashMap<Integer, Double>();
        qID = new ArrayList<String>();
        Scanner scan = new Scanner(new File(params.get("letor:trainingQueryFile")));
        System.out.println("Scan training query file");
        do {
            String line = scan.nextLine();
            String[] pair = line.split(":");
            qPair.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan = new Scanner(new File(params.get("queryFilePath")));
        System.out.println("Scan testing query file");
        do {
            String line = scan.nextLine();
            String[] pair = line.split(":");
            testPair.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan.close();
        scan = new Scanner(new File(params.get("letor:trainingQrelsFile")));
        System.out.println("Scan the training rating file");
        do {
            String line = scan.nextLine();
            String[] pair = line.split(" ");
            if (!qDocPair.containsKey(pair[0])) {
                qDocPair.put(pair[0], new HashMap<Integer, Integer>());
            }
            qDocPair.get(pair[0]).put(MainEval.getInternalDocid(pair[2]), Integer.parseInt(pair[3]));
        } while (scan.hasNext());
        scan.close();
        scan = new Scanner(new File(params.get("letor:pageRankFile")));
        System.out.println("Scan PageRank File");
        do {
            String line = scan.nextLine();
            String[] pair = line.split("\t");
            int iid;
            try{
                iid=MainEval.getInternalDocid(pair[0]);
            }catch(Exception err) {
                iid=-1;
            }
            if (iid!=-1) {
                pagerank.put(iid, Double.parseDouble(pair[1]));
            }
        } while (scan.hasNext());
        scan.close();
        if (params.containsKey("letor:featureDisable")){
            scan = new Scanner(new File(params.get("letor:featureDisable")));
            if (scan.hasNext()){
                String line = scan.nextLine();
                String[] temp = line.split(",");
                for (int i=0; i<temp.length; i++) {
                    disableFeature.add(temp[i]);
                }
            }
        }
        qPairs = new ArrayList<Map.Entry<String,String>>(qPair.entrySet());
        testPairs = new ArrayList<Map.Entry<String, String>>(testPair.entrySet());
        Collections.sort(qPairs,new Comparator<Map.Entry<String,String>>() {
            //ascend
            public int compare(Map.Entry<String, String> o1,
                               Map.Entry<String, String> o2) {
                if (Integer.parseInt(o1.getKey())>Integer.parseInt(o2.getKey()))
                    return 1;
                return 0;
            }
        });
        Collections.sort(testPairs,new Comparator<Map.Entry<String,String>>() {
            //ascend
            public int compare(Map.Entry<String, String> o1,
                               Map.Entry<String, String> o2) {
                if (Integer.parseInt(o1.getKey())>Integer.parseInt(o2.getKey()))
                    return 1;
                return 0;
            }
        });
        System.out.println("Init Learn To Rank Complete");
    }
    /**
     * Cal the feature based on the model for training
     * */
    public static void calFeatureVector(String filename) throws Exception{
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
        for (Map.Entry<String, String> entry : qPairs) {
            String qID = entry.getKey();
            String query = entry.getValue();
            String[] stemTerms = ParserQuery.tokenizeQuery(query);
            ArrayList<String> stemList = new ArrayList<String>();
            for (int i = 0; i < stemTerms.length; i++)
                stemList.add(stemTerms[i]);
            //initiate the feature vector
            Map<Integer, Integer> trainDoc = qDocPair.get(qID);
            Map<Integer, Map<String, Double>> DocFeature = new HashMap<Integer, Map<String, Double>>();
            for (Map.Entry<Integer, Integer> doc : trainDoc.entrySet()) {
                int docID = doc.getKey();
                if (!MainEval.externalID.containsKey(docID))
                    MainEval.externalID.put(docID, MainEval.getExternalDocid(docID));
                Document d = MainEval.READER.document(docID);
                double spamscore = Double.parseDouble(d.get("score"));
                String rawUrl = d.get("rawUrl");
                double num =rawUrl.length() - rawUrl.replace("/", "").length();
                int wiki = 0;
                if (rawUrl.contains("wikipedia.org")) {
                    wiki = 1;
                }
                double pr=-1;
                if (pagerank.containsKey(docID)) {
                    pr=pagerank.get(docID);
                }
                if (spamscore > maxSpam)
                    maxSpam = spamscore;
                if (spamscore < minSpam)
                    minSpam = spamscore;
                if (num > maxDepth)
                    maxDepth = num;
                if (num < minDepth)
                    minDepth = num;
                if (countTf > maxTf)
                    maxTf = countTf;
                if (countTf < minTf)
                    minTf = countTf;
                if ( pr > maxPR && pr != -1.0)
                    maxPR = pr;
                if (pr < minPR && pr != -1.0)
                    minPR = pr;
                if (inlink > maxInlink)
                    maxInlink = inlink;
                if (inlink < minInlink)
                    minInlink = inlink;
                DocFeature.put(docID, new HashMap<String, Double>());
                DocFeature.get(docID).put("spam",  spamscore);
                DocFeature.get(docID).put("depth",  num);
                DocFeature.get(docID).put("wiki", (double) wiki);
                DocFeature.get(docID).put("inlink", inlink);
                DocFeature.get(docID).put("tf", countTf);
                DocFeature.get(docID).put("pagerank", pr);
                inlink = -1;
                countTf = -1;
                calField(doc, "title", stemList, DocFeature);
                calField(doc, "body", stemList, DocFeature);
                calField(doc, "url", stemList, DocFeature);
                calField(doc, "inlink", stemList, DocFeature);
            }
            for (Map.Entry<Integer, Integer> doc : trainDoc.entrySet()) {
                int docID = doc.getKey();
                normalize(DocFeature, docID);
            }
            reset();
            for (Map.Entry<Integer, Integer> doc : trainDoc.entrySet()) {
                int docID = doc.getKey();
                String score = String.valueOf(doc.getValue());
                String line=score + " qid:"+qID+" ";
                if (!disableFeature.contains(String.valueOf(1))) {
                    line = line + "1:" + String.valueOf(DocFeature.get(docID).get("spam")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(2))) {
                    line = line + "2:" + String.valueOf(DocFeature.get(docID).get("depth")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(3))) {
                    line = line + "3:" + String.valueOf(DocFeature.get(docID).get("wiki")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(4))) {
                    line = line + "4:" + String.valueOf(DocFeature.get(docID).get("pagerank")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(5))) {
                    line = line + "5:" + String.valueOf(DocFeature.get(docID).get("BMbody")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(6))) {
                    line = line + "6:" + String.valueOf(DocFeature.get(docID).get("Indbody")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(7))) {
                    line = line + "7:" + String.valueOf(DocFeature.get(docID).get("Overlapbody")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(8))) {
                    line = line + "8:" + String.valueOf(DocFeature.get(docID).get("BMtitle")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(9))) {
                    line = line + "9:" + String.valueOf(DocFeature.get(docID).get("Indtitle")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(10))) {
                    line = line + "10:" + String.valueOf(DocFeature.get(docID).get("Overlaptitle")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(11))) {
                    line = line + "11:" + String.valueOf(DocFeature.get(docID).get("BMurl")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(12))) {
                    line = line + "12:" + String.valueOf(DocFeature.get(docID).get("Indurl")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(13))) {
                    line = line + "13:" + String.valueOf(DocFeature.get(docID).get("Overlapurl")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(14))) {
                    line = line + "14:" + String.valueOf(DocFeature.get(docID).get("BMinlink")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(15))) {
                    line = line + "15:" + String.valueOf(DocFeature.get(docID).get("Indinlink")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(16))) {
                    line = line + "16:" + String.valueOf(DocFeature.get(docID).get("Overlapinlink")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(17))) {
                    line = line + "17:" + String.valueOf(DocFeature.get(docID).get("tf")) + " ";
                }
                if (!disableFeature.contains(String.valueOf(18))) {
                    line = line + "18:" + String.valueOf(DocFeature.get(docID).get("inlink")) + " ";
                }
                line = line+"# " + MainEval.externalID.get(docID) + "\n";

                writer.write(line);
            }
            System.out.println("Query " + qID + " Complete");
        }
        writer.close();
    }
    /**
     * Cal the score based on Field
     * */
    static void calField(Map.Entry<Integer, Integer> doc, String field, ArrayList<String> stemList,
                         Map<Integer, Map<String, Double>> DocFeature) throws Exception {
        int N = MainEval.READER.numDocs();
        float avg_docLenTitle = MainEval.READER.getSumTotalTermFreq(field) /
                (float) MainEval.READER.getDocCount(field);
        long docLen = dls.getDocLength(field, doc.getKey());
        Terms terms = MainEval.READER.getTermVector(doc.getKey(), field);
        if (terms == null) {
            DocFeature.get(doc.getKey()).put("BM" + field, -1.0);
            DocFeature.get(doc.getKey()).put("Ind" + field, -1.0);
            DocFeature.get(doc.getKey()).put("Overlap" + field, -1.0);
        } else {
            TermsEnum ithTerm = terms.iterator(null);
            double scoreBM = 0;
            double scoreInd = 1.0;
            int count = 0;
            Map<String, Long> trac = new HashMap<String, Long>();
            for (int i = 0; ithTerm.next() != null; i++) {
                String tt = ithTerm.term().utf8ToString();
                if (stemList.contains(tt)){
                    trac.put(tt, ithTerm.totalTermFreq());
                }
            }
            int matchedTerm=0;
            boolean inlinkFlag=false;
            for (String term : stemList) {
                if (trac.containsKey(term)) {
                    if (field.equalsIgnoreCase("inlink")&&!inlinkFlag) {
                        inlink += docLen;
                        inlinkFlag=true;
                    }
                    BytesRef termBytes = new BytesRef(term);
                    Term tempTerm = new Term(field, termBytes);
                    int df=MainEval.READER.docFreq(tempTerm);
                    double MLE = (MainEval.READER.totalTermFreq(tempTerm)) / ((double) dls.getDocLength(field));
                    count++;
                    long tf = trac.get(term);
                    countTf += tf;
                    // BM
                    double temp = (Math.log((N - df + 0.5) / (float) (df + 0.5)))
                            * ((tf) /
                            (float) (tf + bm.K_1() * (1 - bm.B() + bm.B() * (docLen / (avg_docLenTitle)))));
                    scoreBM = scoreBM + temp;
                    // Indri
                    temp = ind.lambda() * (tf + ind.mu() * MLE)
                            / ((double) docLen + ind.mu())
                            + (1 - ind.lambda()) * MLE;
                    scoreInd = scoreInd * temp;
                    matchedTerm++;
                } else {
                    BytesRef termBytes = new BytesRef(term);
                    Term tempTerm = new Term(field, termBytes);
                    double MLE = (MainEval.READER.totalTermFreq(tempTerm)) / ((double) dls.getDocLength(field));
                    double defaultScore = ind.lambda()*ind.mu()*MLE/(float)(docLen+ind.mu()) + (1-ind.lambda())*MLE;
                    scoreInd = scoreInd*defaultScore;
                }
            }
            if (matchedTerm==0)
                scoreInd=0;
            scoreInd = Math.pow(scoreInd, 1/(double)stemList.size());
            DocFeature.get(doc.getKey()).put("Overlap" + field, count /(double) stemList.size());
            DocFeature.get(doc.getKey()).put("BM" + field, scoreBM);
            DocFeature.get(doc.getKey()).put("Ind" + field, scoreInd);
            updateScore(field, scoreBM, scoreInd, count / (double)stemList.size());
        }
    }
    /**
     * update the max and min score
     * */
    static void updateScore(String field, double scoreBM, double scoreInd, double overlap) {
        if (field.equalsIgnoreCase("title")) {
            if (scoreBM > maxTitleBM)
                maxTitleBM = scoreBM;
            if (scoreBM < minTitleBM)
                minTitleBM = scoreBM;
            if (scoreInd > maxTitleInd)
                maxTitleInd = scoreInd;
            if (scoreInd < minTitleInd)
                minTitleInd = scoreInd;
            if (overlap > maxOverlapTitle)
                maxOverlapTitle = overlap;
            if (overlap < minOverlapTitle)
                minOverlapTitle = overlap;
        }
        if (field.equalsIgnoreCase("body")) {
            if (scoreBM > maxBodyBM)
                maxBodyBM = scoreBM;
            if (scoreBM < minBodyBM)
                minBodyBM = scoreBM;
            if (scoreInd > maxBodyInd)
                maxBodyInd = scoreInd;
            if (scoreInd < minBodyInd)
                minBodyInd = scoreInd;
            if (overlap > maxOverlapBody)
                maxOverlapBody = overlap;
            if (overlap < minOverlapBody)
                minOverlapBody = overlap;
        }
        if (field.equalsIgnoreCase("url")) {
            if (scoreBM > maxUrlBM)
                maxUrlBM = scoreBM;
            if (scoreBM < minUrlBM)
                minUrlBM = scoreBM;
            if (scoreInd > maxUrlInd)
                maxUrlInd = scoreInd;
            if (scoreInd < minUrlInd)
                minUrlInd = scoreInd;
            if (overlap > maxOverlapUrl)
                maxOverlapUrl = overlap;
            if (overlap < minOverlapUrl)
                minOverlapUrl = overlap;
        }
        if (field.equalsIgnoreCase("inlink")) {
            if (scoreBM > maxInlinkBM)
                maxInlinkBM = scoreBM;
            if (scoreBM < minInlinkBM)
                minInlinkBM = scoreBM;
            if (scoreInd > maxInlinkInd)
                maxInlinkInd = scoreInd;
            if (scoreInd < minInlinkInd)
                minInlinkInd = scoreInd;
            if (overlap > maxOverlapInlink)
                maxOverlapInlink = overlap;
            if (overlap < minOverlapInlink)
                minOverlapInlink = overlap;
        }
    }

    /**
     * Normalize the score
     */
    static void normalize(Map<Integer, Map<String, Double>> DocFeature, int docID) {
        Map<String, Double> featureVector = DocFeature.get(docID);
        double BMTitle = featureVector.get("BMtitle");
        double BMBody = featureVector.get("BMbody");
        double BMUrl = featureVector.get("BMurl");
        double BMInlink = featureVector.get("BMinlink");
        double IndTitle = featureVector.get("Indtitle");
        double IndBody = featureVector.get("Indbody");
        double IndUrl = featureVector.get("Indurl");
        double IndInlink = featureVector.get("Indinlink");
        double spam = featureVector.get("spam");
        double depth = featureVector.get("depth");
        double overlapTitle = featureVector.get("Overlaptitle");
        double overlapBody = featureVector.get("Overlapbody");
        double overlapUrl = featureVector.get("Overlapurl");
        double overlapInlink = featureVector.get("Overlapinlink");
        double wiki = featureVector.get("wiki");
        double tf = featureVector.get("tf");
        double pr = featureVector.get("pagerank");
        double inlink = featureVector.get("inlink");
        featureVector = new HashMap<String, Double>();
        if (BMTitle == -1.0) {
            featureVector.put("BMtitle", 0.0);
            featureVector.put("Indtitle", 0.0);
            featureVector.put("Overlaptitle", 0.0);
        } else {
            BMTitle=(BMTitle - minTitleBM) / (maxTitleBM - minTitleBM);
            if (maxTitleBM==minTitleBM)
                BMTitle=0;
            featureVector.put("BMtitle", BMTitle);
            if (IndTitle==maxTitleInd)
                System.out.println(docID);
            IndTitle=(IndTitle - minTitleInd) / (maxTitleInd - minTitleInd);
            if (maxTitleInd==minTitleInd)
                IndTitle=0;
            featureVector.put("Indtitle", IndTitle);
            overlapTitle=(overlapTitle - minOverlapTitle) / (maxOverlapTitle - minOverlapTitle);
            if (maxOverlapTitle==minOverlapTitle)
                overlapTitle=0;
            featureVector.put("Overlaptitle", overlapTitle);
        }
        if (BMBody == -1.0) {
            featureVector.put("BMbody", 0.0);
            featureVector.put("Indbody", 0.0);
            featureVector.put("Overlapbody", 0.0);
        } else {
            BMBody=(BMBody - minBodyBM) / (maxBodyBM - minBodyBM);
            if (maxBodyBM==minBodyBM)
                BMBody=0;
            featureVector.put("BMbody", BMBody);
            IndBody=(IndBody - minBodyInd) / (maxBodyInd - minBodyInd);
            if (maxBodyInd==minBodyInd)
                IndBody=0;
            featureVector.put("Indbody", IndBody);
            overlapBody=(overlapBody - minOverlapBody) / (maxOverlapBody - minOverlapBody);
            if (maxOverlapBody==minOverlapBody)
                overlapBody=0;
            featureVector.put("Overlapbody", overlapBody);
        }
        if (BMInlink == -1.0) {
            featureVector.put("BMinlink", 0.0);
            featureVector.put("Indinlink", 0.0);
            featureVector.put("Overlapinlink", 0.0);
        } else {
            BMInlink=(BMInlink - minInlinkBM) / (maxInlinkBM - minInlinkBM);
            if (maxInlinkBM==minInlinkBM)
                BMInlink=0;
            featureVector.put("BMinlink", BMInlink);
            IndInlink=(IndInlink - minInlinkInd) / (maxInlinkInd - minInlinkInd);
            if (maxInlinkInd==minInlinkInd)
                IndInlink=0;
            featureVector.put("Indinlink", IndInlink);
            overlapInlink=(overlapInlink - minOverlapInlink) / (maxOverlapInlink - minOverlapInlink);
            if (maxOverlapInlink==minOverlapInlink)
                overlapInlink=0;
            featureVector.put("Overlapinlink", overlapInlink);
        }
        if (BMUrl == -1.0) {
            featureVector.put("BMurl", 0.0);
            featureVector.put("Overlapurl", 0.0);
            featureVector.put("Indurl", 0.0);
        } else {
            BMUrl=(BMUrl - minUrlBM) / (maxUrlBM - minUrlBM);
            if (maxUrlBM==minUrlBM)
                BMUrl=0;
            featureVector.put("BMurl", BMUrl);
            overlapUrl=(overlapUrl - minOverlapUrl) / (maxOverlapUrl - minOverlapUrl);
            if (maxOverlapUrl==minOverlapUrl)
                overlapUrl=0;
            featureVector.put("Overlapurl", overlapUrl);
            IndUrl=(IndUrl - minUrlInd) / (maxUrlInd - minUrlInd);
            if (maxUrlInd==minUrlInd)
                IndUrl=0;
            featureVector.put("Indurl", IndUrl);
        }
        if (minSpam==maxSpam)
            featureVector.put("spam", 0.0);
        else
            featureVector.put("spam", (spam - minSpam) / (maxSpam - minSpam));
        if (minDepth==maxDepth)
            featureVector.put("depth", 0.0);
        else
            featureVector.put("depth", (depth - minDepth) / (maxDepth - minDepth));
        if (maxTf==minTf)
            featureVector.put("tf", 0.0);
        else
            featureVector.put("tf", (tf - minTf) / (maxTf - minTf));
        if (pr==-1.0)
            featureVector.put("pagerank", 0.0);
        else
            featureVector.put("pagerank", (pr - minPR) / (maxPR - minPR));
        if (maxInlink==minInlink)
            featureVector.put("inlink", 0.0);
        else
            featureVector.put("inlink", (inlink - minInlink) / (maxInlink - minInlink));
        featureVector.put("wiki", wiki);
        DocFeature.put(docID, featureVector);
    }

    /**
     * reset the max and min value
     */
    static void reset() {
        minBodyBM = Double.MAX_VALUE;
        maxBodyBM = 0.0;
        minTitleBM = Double.MAX_VALUE;
        maxTitleBM = 0.0;
        minUrlBM = Double.MAX_VALUE;
        maxUrlBM = 0.0;
        minInlinkBM = Double.MAX_VALUE;
        maxInlinkBM = 0.0;
        minBodyInd = Double.MAX_VALUE;
        maxBodyInd = 0.0;
        minTitleInd = Double.MAX_VALUE;
        maxTitleInd = 0.0;
        minUrlInd = Double.MAX_VALUE;
        maxUrlInd = 0.0;
        minInlinkInd = Double.MAX_VALUE;
        maxInlinkInd = 0.0;
        minSpam = Double.MAX_VALUE;
        maxSpam = 0.0;
        minOverlapTitle = Double.MAX_VALUE;
        maxOverlapTitle = 0.0;
        minOverlapBody = Double.MAX_VALUE;
        maxOverlapBody = 0.0;
        minOverlapUrl = Double.MAX_VALUE;
        maxOverlapUrl = 0.0;
        minOverlapInlink = Double.MAX_VALUE;
        maxOverlapInlink = 0.0;
        minDepth = Double.MAX_VALUE;
        maxDepth = 0.0;
        maxTf = 0.0;
        minTf = Double.MAX_VALUE;
        maxPR = 0.0;
        minPR = Double.MAX_VALUE;
        maxInlink = 0.0;
        minInlink = Double.MAX_VALUE;
    }
    static void train() throws Exception{
        calFeatureVector(featureFile);
        Process cmdProc = Runtime.getRuntime().exec(
                new String[] {
                        execPath, "-c", c, featureFile,
                        modelOutputFile
                }
        );
        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }
        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }
    }
    static void classify() throws Exception{
        Process cmdProc = Runtime.getRuntime().exec(
                new String[] {
                        classifyPath, testFeatureFile,
                        modelOutputFile, finalScoreFile
                }
        );
        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }
        // get the return value from the executable. 0 means success, non-zero
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
            throw new Exception("SVM Rank crashed.");
        }
    }
    /**
     * Cal the feature based on the model for training
     * */
    public static void calFinalResult() throws Exception{
        //retrieve documents based on BM25
        qPairs = testPairs;
        ParserQuery parser = new ParserQuery();
        Qryop qTree;
        qDocPair = new HashMap<String, Map<Integer, Integer>>();
        resultMap = new HashMap<String, Map<String, Double>>();
        Scanner scan;
        for (Map.Entry<String, String> entry : qPairs) {
            resultMap.put(entry.getKey(), new HashMap<String, Double>());
            parser.setPara(entry.getValue(), bm);
            qTree = parser.parseIt();
            QryResult result = qTree.evaluate(bm);
            result.sort();
            int length;
            if (result.docScores.scores.size() <= 100)
                length = result.docScores.scores.size();
            else
                length = 100;
            qDocPair.put(entry.getKey(), new HashMap<Integer, Integer>());
            for (int i = 0; i < length; i++) {
                qDocPair.get(entry.getKey()).put(result.docScores.getDocid(i),0);
            }
        }
        //form the final file format
        //calculate the feature vector and write it into file
        calFeatureVector(testFeatureFile);
        classify();
        //read the result into resultMap
        scan = new Scanner(new File(finalScoreFile));
        Map<String, Double> res;
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(trecEvalOutputPath)));
        for (Map.Entry<String, String> entry : qPairs) {
            if (!scan.hasNext())
                break;
            res = new HashMap<String, Double>();
            for (Map.Entry<Integer, Integer> doc : qDocPair.get(entry.getKey()).entrySet()) {
                String score = scan.nextLine();
                if (!MainEval.externalID.containsKey(doc.getKey())) {
                    String tmp = MainEval.getExternalDocid(doc.getKey());
                    res.put(tmp, Double.valueOf(score));
                }
                else
                    res.put(MainEval.externalID.get(doc.getKey()), Double.valueOf(score));
            }
            Comparator<Map.Entry<String, Double>> comp =  new Comparator<Map.Entry<String, Double>>() {
                @Override
                public int compare(Map.Entry<String, Double> Entry, Map.Entry<String, Double> Entry2){
                    int result=Double.compare(Entry2.getValue(), Entry.getValue());
                    if (result==0) {
                        if (Entry.getKey().compareTo(Entry2.getKey()) > 0) {
                            return 1;
                        } else return -1;
                    }else
                        return result;
                }
            };
            ArrayList<Map.Entry<String,Double>> tmp =new ArrayList<Map.Entry<String,Double>>(res.entrySet());
            Collections.sort(tmp, comp);
            int count=1;
            for (Map.Entry<String, Double> it : tmp) {
                writer.write(entry.getKey() + "\t" + "Q0" + "\t"
                        + it.getKey()
                        + "\t" + count + "\t"
                        + it.getValue() + "\trun-1" + "\n");
                count++;
            }
        }
        writer.close();
    }
 }
