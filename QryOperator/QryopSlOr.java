package QryOperator;

import DataStructure.*;
import RetrievalModel.*;
import java.io.IOException;
import java.util.Iterator;

/**
 * This class implements the Or operator for all retrieval models
 * Created by XiangyuSun on 14-9-6.
 */
public class QryopSlOr extends QryopSl {
    /**
     * Constructor of Or operator
     * */
    public QryopSlOr(Qryop... q) {
        for (int i = 0; i < q.length; i++)
            this.args.add(q[i]);
    }
    /**
     *  Appends an argument to the list of query operator arguments.  This
     *  simplifies the design of some query parsing architectures.
     *  @param {q} q The query argument (query operator) to append.
     *  @return void
     *  @throws IOException
     */
    public void add (Qryop a) {
        this.args.add(a);
    }

    /**
     *  Evaluates the query operator, including any child operators and
     *  returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluate(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelUnrankedBoolean||r instanceof RetrievalModelRankedBoolean)
            return (evaluateBoolean (r));
        return null;
    }

    /**
     *  Evaluates the query operator for boolean retrieval models,
     *  including any child operators and returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluateBoolean (RetrievalModel r) throws IOException {
        //  Initialization
        allocDaaTPtrs (r);
        QryResult result = new QryResult ();
        Boolean flag=false;
        while (this.daatPtrs.size()>0) {
            int nextDocId = getSmallestCurrentDocid();
            double docScore = 0;
            for (int i=0; i<this.daatPtrs.size(); i++) {
                DaaTPtr ptr = this.daatPtrs.get(i);
                if (ptr.scoreList.getDocid(ptr.nextDoc) == nextDocId) {
                    if (r instanceof RetrievalModelRankedBoolean) {
                       if (docScore<ptr.scoreList.getDocidScore(ptr.nextDoc)) {
                           docScore = ptr.scoreList.getDocidScore(ptr.nextDoc);
                       }
                    } else if (r instanceof RetrievalModelUnrankedBoolean) {
                        docScore = 1.0;
                    }
                    ptr.nextDoc++;
                }
            }
            result.docScores.add(nextDocId, docScore);
            for (int i=this.daatPtrs.size()-1; i>=0; i--) {
                DaaTPtr ptri = this.daatPtrs.get(i);

                if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
                    this.daatPtrs.remove (i);
                }
            }

        }

        freeDaaTPtrs ();
        return result;
    }

    /**
     *  Return a string version of this query operator.
     *  @return The string version of this query operator.
     */
    public String toString(){

        String result = new String ();

        for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
            result += (i.next().toString() + " ");

        return ("#or( " + result + ")");
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
     *  Calculate the default score for the specified document if it
     *  does not match the query operator.  This score is 0 for many
     *  retrieval models, but not all retrieval models.
     *  @param  r A retrieval model that controls how the operator behaves.
     *  @param docid The internal id of the document that needs a default score.
     *  @return The default score.
     */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean)
            return (0.0);
        else if (r instanceof RetrievalModelIndri) {
            double score=1;
            for (int i=0; i<this.args.size(); i++) {
                if (this.args.get(i) instanceof QryopSl)
                    score=score*(1-((QryopSl) this.args.get(i)).getDefaultScore(r, docid));
            }
            score = Math.pow(score, 1/(double)this.args.size());
            return score;
        }

        return 0.0;
    }

}
