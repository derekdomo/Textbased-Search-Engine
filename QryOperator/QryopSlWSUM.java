package QryOperator;

import DataStructure.QryResult;
import Main.MainEval;
import RetrievalModel.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QryopSlWSUM extends QryopSl {
    public ArrayList<Double> weights = new ArrayList<Double>();
    public double sumweights=0;
    /**
     *  Appends an argument to the list of query operator arguments.  This
     *  simplifies the design of some query parsing architectures.
     *  @param a The query argument (query operator) to append.
     */
    public void addWeight (double a) {
        this.weights.add(a);
    }
    public int lengthWeight () {return this.weights.size(); }
    public int lengthArgs () {return this.args.size(); }
    /**
     *  Appends an argument to the list of query operator arguments.  This
     *  simplifies the design of some query parsing architectures.
     *  @param a The query argument (query operator) to append.
     */
    public void add (Qryop a) {
        this.args.add(a);
    }
    /**
     *  syntaxCheckArgResults does syntax checking that can only be done
     *  after query arguments are evaluated.
     *  @param ptrs A list of DaaTPtrs for this query operator.
     *  @return True if the syntax is valid, false otherwise.
     */
    public Boolean syntaxCheckArgResults (List<DaaTPtr> ptrs) {

        for (int i=0; i<this.args.size(); i++) {

            if (! (this.args.get(i) instanceof QryopSl))
                MainEval.fatalError("Error:  Invalid argument in " +
                        this.toString());
        }

        return true;
    }
    /**
     *  Evaluates the query operator, including any child operators and
     *  returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws java.io.IOException
     */
    public QryResult evaluate(RetrievalModel r) throws IOException {
        allocDaaTPtrs (r);
        syntaxCheckArgResults (this.daatPtrs);
        QryResult res = new QryResult();
        if (!(r instanceof RetrievalModelIndri)) {
            return res;
        }
        for (int i=0; i<weights.size(); i++)
            sumweights+=weights.get(i);
        //computes a score for a document that is a weighted average of the scores produced by its arguments
        int length=this.daatPtrs.size();
        while (this.daatPtrs.size() > 0) {
            int count=0;
            int nextDocID = getSmallestCurrentDocid();
            double tempRes = 0;
            if (nextDocID==Integer.MAX_VALUE)
                break;
            for (int i=0; i<this.daatPtrs.size(); i++) {
                DaaTPtr ptr = this.daatPtrs.get(i);
                if (ptr.nextDoc >= ptr.scoreList.scores.size()||nextDocID != ptr.scoreList.getDocid(ptr.nextDoc)) {
                    double defaultscore = ((QryopSl)this.args.get(i)).getDefaultScore(r, nextDocID);
                    tempRes += weights.get(i)/sumweights*defaultscore;
                } else if (ptr.scoreList.getDocid(ptr.nextDoc) == nextDocID) {
                    tempRes += weights.get(i)/sumweights*ptr.scoreList.getDocidScore(ptr.nextDoc);
                    ptr.nextDoc++;
                }
            }
            res.docScores.add(nextDocID, tempRes);
            for (int i=this.daatPtrs.size()-1; i>=0; i--) {
                DaaTPtr ptri = this.daatPtrs.get(i);
                if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
                    count++;
                }
            }
            if (count==length)
                break;
        }
        return res;
    }
    /**
     *  Return the smallest unexamined docid from the DaaTPtrs.
     *  @return The smallest internal document id.
     */
    public int getSmallestCurrentDocid () {

        int nextDocid = Integer.MAX_VALUE;

        for (int i=0; i<this.daatPtrs.size(); i++) {
            DaaTPtr ptri = this.daatPtrs.get(i);
            if (nextDocid > ptri.scoreList.getDocid (ptri.nextDoc))
                nextDocid = ptri.scoreList.getDocid (ptri.nextDoc);
        }

        return (nextDocid);
    }
    /**
     *  Return a string version of this query operator.
     *  @return The string version of this query operator.
     */
    public String toString(){

        String result ="";
        int j=0;
        for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); j++)
            result += (weights.get(j).toString()+" "+i.next().toString() + " ");

        return ("#WSUM( " + result + ")");
    }
    /*
  *  Calculate the default score for the specified document if it
  *  does not match the query operator.  This score is 0 for many
  *  retrieval models, but not all retrieval models.
  *  @param r A retrieval model that controls how the operator behaves.
  *  @param docid The internal id of the document that needs a default score.
  *  @return The default score.
  */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean)
            return (0.0);
        else if (r instanceof RetrievalModelIndri) {
            double score=0;
            for (int i=0; i<this.args.size(); i++) {
                if (this.args.get(i) instanceof QryopSl)
                    score=score+this.weights.get(i)/sumweights*((QryopSl) this.args.get(i)).getDefaultScore(r, docid);
            }
            return score;
        }

        return 0.0;
    }
}
