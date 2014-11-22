package Main;

import DataStructure.QryResult;
import DataStructure.ScoreList;
import DataStructure.TermVector;
import QryOperator.Qryop;
import QryOperator.QryopIlTerm;
import RetrievalModel.*;
import sun.applet.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;

/**
 * Learn to Rank:
 *  Features including
 *      BM score
 *      Indri score
 *
 */
public class LearnToRank {
    static RetrievalModelLearnToRank r;
    static RetrievalModelBM25 bm;
    static RetrievalModelIndri ind;
    static Qryop qTree;
    static Map<String, String> qPairs;
    static Map<String, Map<Integer, Double>> qDocPair;
    static Map<String, Map<Integer, Map<String, Double>>> qDocFeature;
    static ArrayList<String> qID;

    /**
     * Class Construction
     * */
    public LearnToRank(RetrievalModelLearnToRank r, Qryop qTree, Map<String, String> params) throws Exception {
        this.r = r;
        bm.setParameter("BM25:k_1", r.k1);
        bm.setParameter("BM25:k_3", r.k3);
        bm.setParameter("BM25:b", r.b);
        ind.setParameter("Indri:mu", r.mu);
        ind.setParameter("Indri:lambda", r.lambda);
        this.qTree = qTree;
        qPairs = new HashMap<String, String>();
        qDocPair = new HashMap<String, Map<Integer, Double>>();
        qDocFeature = new HashMap<String, Map<Integer, Map<String, Double>>>();
        qID = new ArrayList<String>();
        Scanner scan = new Scanner(new File(params.get("letor:trainQueryFile")));
        do {
            String line = scan.nextLine();
            String[] pair = line.split(":");
            qPairs.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan.close();
        scan = new Scanner(new File(params.get("letor:trainingQrelsFile")));
        do {
            String line = scan.nextLine();
            String[] pair = line.split(" ");
            if (!qDocPair.containsKey(pair[0])) {
                qDocPair.put(pair[0], new HashMap<Integer, Double>());
            }
            qDocPair.get(pair[0]).put(MainEval.getInternalDocid(pair[2]), Double.parseDouble(pair[3]));
        } while (scan.hasNext());
    }
    /**
     *  Begin to cal Feature
     * */
    public static void calFeature() throws Exception{
        //cal the BM title
        calModelFeature("title", bm);
        //cal the BM body
        calModelFeature("body", bm);
        //cal the BM inlink
        calModelFeature("inlink", bm);
        //cal the BM url
        calModelFeature("url", bm);
        //cal the Indri title
        calModelFeature("title", ind);
        //cal the Indri body
        calModelFeature("body", ind);
        //cal the Indri inlink
        calModelFeature("inlink", ind);
        //cal the Indri url
        calModelFeature("url", ind);
        //cal the spam score, wikipedia score and URLDepth
        for (String it : qID) {
            for (Map.Entry<Integer, Map<String, Double>> doc : qDocFeature.get(it).entrySet()) {
                int docID = doc.getKey();
                Document d = MainEval.READER.document(docID);
                int spamscore = Integer.parseInt(d.get("score"));
                String rawUrl = d.get("rawUrl");
                String[] temp = rawUrl.split("\\/");
                int num = temp.length;
                int wiki = 0;
                if (rawUrl.equalsIgnoreCase("wikipedia.org")) {
                    wiki = 1;
                }
                qDocFeature.get(it).get(docID).put("spam", (double)spamscore);
                qDocFeature.get(it).get(docID).put("depth", (double)num);
                qDocFeature.get(it).get(docID).put("wiki", (double)wiki);
                calOverlap(it, docID);
            }
        }
    }
    /**
     * Cal the feature based on the model
     * */
    public static void calModelFeature(String field, RetrievalModel model) throws Exception{
        QryopIlTerm.setDefaultField(field);
        ParserQuery parser = new ParserQuery();
        Qryop qTree;
        for (Map.Entry<String, String> entry : qPairs.entrySet()) {
            qID.add(entry.getKey());
            parser.setPara(entry.getValue(), model);
            qTree = parser.parseIt();
            QryResult result = qTree.evaluate(model);
            Map<Integer, Double> calDoc = qDocPair.get(entry.getKey());
            //initiate the hashmap that store the docID and its features
            if (!qDocFeature.containsKey(entry.getKey())) {
                qDocFeature.put(entry.getKey(), new HashMap<Integer, Map<String, Double>>());
            }
            //initiate the hashmap that store score docID and
            Map<Integer, Double> docFeature = new HashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> it : calDoc.entrySet()) {
                docFeature.put(it.getKey(), result.docScores.getDocidScore(it.getKey()));
            }
            for (Map.Entry<Integer, Double> it : docFeature.entrySet()) {
                if (qDocFeature.get(entry.getKey()).containsKey(it.getKey()))
                    qDocFeature.get(entry.getKey()).put(it.getKey(), new HashMap<String, Double>());
                qDocFeature.get(entry.getKey()).get(it.getKey()).put("BM"+field, it.getValue());
            }
        }
    }
    /**
     * Cal the feature of overlap term in inlink
     * */
    public static void calOverlap(String qID, int docID) throws Exception {
        String[] query = qPairs.get(qID).split(" ");
        TermVector terms = new TermVector(docID, "body");
        double bodyOverlap = terms.overlap(query);
        terms = new TermVector(docID, "title");
        double titleOverlap = terms.overlap(query);
        terms = new TermVector(docID, "url");
        double urlOverlap = terms.overlap(query);
        terms = new TermVector(docID, "inlink");
        double inlinkOverlap = terms.overlap(query);
        qDocFeature.get(qID).get(docID).put("bodyOverlap", bodyOverlap);
        qDocFeature.get(qID).get(docID).put("titleOverlap", titleOverlap);
        qDocFeature.get(qID).get(docID).put("urlOverlap", urlOverlap);
        qDocFeature.get(qID).get(docID).put("inlinkOverlap", inlinkOverlap);
    }
 }
