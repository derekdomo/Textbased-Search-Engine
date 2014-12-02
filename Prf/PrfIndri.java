package Prf;
import DataStructure.*;
import Main.*;
import java.util.*;
/**
 * Created by XiangyuSun on 10/30/14.
 */
public class PrfIndri {
    public Map<String, String> calTerm(Map<String, Map<String,String>> candidateDoc, double mu, int termNum) throws Exception{
        Map<String, String> learned = new HashMap<String, String>();
        for (Map.Entry<String, Map<String,String>> entry : candidateDoc.entrySet()) {
            //form the learned query for these documents
            Map<String, String> temp = entry.getValue();
            String queryID = entry.getKey();
            Map<TermVector, ScoreDocCombine> docs = new HashMap<TermVector, ScoreDocCombine>();
            for (Map.Entry<String, String> entry1 : temp.entrySet()) {
                docs.put(new TermVector(Integer.valueOf(entry1.getKey()), "body"), new ScoreDocCombine(Integer.valueOf(entry1.getKey()), Double.valueOf(entry1.getValue())));
            }
            Map<String, ScoreContainer> scores = new HashMap<String, ScoreContainer>();
            DocLengthStore dls = new DocLengthStore(MainEval.READER);
            long length = dls.getDocLength("body");
            Map<String, Double> existTerms = new HashMap<String, Double>();
            //Iterate all the documents to get all the terms
            for (Map.Entry<TermVector, ScoreDocCombine> entry1 : docs.entrySet()) {
                TermVector it = entry1.getKey();
                for (int i=1; i<it.stemsLength(); i++){
                    String term = it.stemString(i);
                    if (term.contains(".")||term.contains(","))
                        continue;
                    if (!existTerms.containsKey(term)) {
                        double MLE = (it.totalStemFreq(i)+0.0)/length;
                        existTerms.put(term, MLE);
                    }
                }
            }
            for (Map.Entry<String, Double> termIt : existTerms.entrySet()) {
                double MLE = termIt.getValue();
                String term = termIt.getKey();
                for (Map.Entry<TermVector, ScoreDocCombine> entry1 : docs.entrySet()){
                    TermVector it = entry1.getKey();
                    int termTotal = it.stemsLength();
                    double pID = entry1.getValue().score;
                    int id = entry1.getValue().docID;
                    int tf = 0;
                    for (int i=1; i<termTotal; i++) {
                        String term2 = it.stemString(i);
                        if (term.equalsIgnoreCase(term2))
                            tf = it.stemFreq(i);
                    }
                    double pTD = (tf+mu*MLE)/(dls.getDocLength("body", id)+mu);
                    double score = pID*pTD*Math.log(1/MLE);
                    if (scores.containsKey(term)) {
                        scores.get(term).scoreUpdate(score);
                    } else
                        scores.put(term, new ScoreContainer(score));
                }
            }
            Comparator<Map.Entry<String, ScoreContainer>> comp =  new Comparator<Map.Entry<String, ScoreContainer>>() {
                @Override
                public int compare(Map.Entry<String, ScoreContainer> entry1, Map.Entry<String, ScoreContainer> entry2){
                    return Double.compare(entry2.getValue().score,entry1.getValue().score);
                }
            };
            ArrayList<Map.Entry<String, ScoreContainer>> arr = new ArrayList<Map.Entry<String, ScoreContainer>>(scores.entrySet());
            Collections.sort(arr,comp);
            String query="#WAND(";
            int count=0;
            for (Map.Entry<String, ScoreContainer> entry1 : arr) {
                if (count==termNum)
                    break;
                else
                    count++;
                System.out.println(entry1.getKey());
                String termname = entry1.getKey();
                double score = entry1.getValue().score;
                query=query+" "+String.valueOf(score)+" "+termname+" ";
            }
            query=query+")";
            learned.put(queryID,query);
            System.out.println("Query "+queryID+" Expansion Complete");
        }
        return learned;
    }
}
class ScoreDocCombine {
    public int docID;
    public double score;
    public ScoreDocCombine(int docID, double score) {
        this.docID = docID;
        this.score = score;
    }
}
class ScoreContainer {
    public double score;
    public ScoreContainer(double score) {
        this.score = score;
    }
    public void scoreUpdate(double add) {
        this.score+=add;
    }
}