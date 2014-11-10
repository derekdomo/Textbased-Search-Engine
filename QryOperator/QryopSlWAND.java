package QryOperator;

import DataStructure.QryResult;
import Main.MainEval;
import RetrievalModel.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by XiangyuSun on 10/10/14.
 */
public class QryopSlWAND extends QryopSl {
    public ArrayList<Double> weights = new ArrayList<Double>();
    public double sumweights=0;
    /**
     *  Appends an argument to the list of query operator arguments.  This
     *  simplifies the design of some query parsing architectures.
     *  @param a The query argument (query operator) to append.
     *  @return void
     *  @throws java.io.IOException
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
     *  @return void
     *  @throws java.io.IOException
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
        QryResult result = new QryResult();
        if (!(r instanceof RetrievalModelIndri)) {
            return result;
        }
        for (int i=0; i<weights.size(); i++)
            sumweights+=weights.get(i);
        //computes a score for a document that is a weighted product of the scores produced by its arguments
        boolean alldone=false;
        int length=this.daatPtrs.size();
        while (!alldone) {
            int nextDocID = getSmallestCurrentDocid();
            if (nextDocID==Integer.MAX_VALUE)
                break;
            double score=1;
            for (int i=0; i<this.daatPtrs.size(); i++) {
                DaaTPtr ptr = this.daatPtrs.get(i);
                if (ptr.nextDoc>=ptr.scoreList.scores.size()||nextDocID != ptr.scoreList.getDocid(ptr.nextDoc)) {
                    double defaultscore = ((QryopSl)this.args.get(i)).getDefaultScore(r, nextDocID);
                    if (defaultscore==0)
                        defaultscore=1;
                    score = score * Math.pow(defaultscore, this.weights.get(i)/sumweights);
                }else if (nextDocID == ptr.scoreList.getDocid(ptr.nextDoc)) {
                    double tmp = ptr.scoreList.getDocidScore(ptr.nextDoc);
                    score = score * Math.pow(tmp, this.weights.get(i)/sumweights);
                    ptr.nextDoc++;
                }
            }
            result.docScores.add(nextDocID, score);
            int count=0;
            for (int i=this.daatPtrs.size()-1; i>=0; i--) {
                DaaTPtr ptri = this.daatPtrs.get(i);
                if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
                    count++;
                }
            }
            if (count==length)
                alldone=true;
        }
        freeDaaTPtrs ();
        return result;
    }
    /**
     * Get defaultScore
     * */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean)
            return (0.0);
        else if (r instanceof RetrievalModelIndri) {
            double score=1;
            for (int i=0; i<this.args.size(); i++) {
                if (this.args.get(i) instanceof QryopSl)
                    score=score*Math.pow(((QryopSl) this.args.get(i)).getDefaultScore(r, docid), this.weights.get(i)/sumweights);
            }
            return score;
        }

        return 0.0;
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

        String result = new String ();

        for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
            result += (i.next().toString() + " ");

        return ("#WAND( " + result + ")");
    }

}
