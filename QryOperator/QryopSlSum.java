package QryOperator;

import DataStructure.*;
import Main.*;
import RetrievalModel.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.lang.Math;
/**
 * Created by XiangyuSun on 9/20/14.
 */
public class QryopSlSum extends QryopSl {
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
     *  Evaluates the query operator, including any child operators and
     *  returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws java.io.IOException
     */
    public QryResult evaluate(RetrievalModel r) throws IOException {
        //  Initialization
        allocDaaTPtrs (r);
        syntaxCheckArgResults (this.daatPtrs);
        if (!(r instanceof RetrievalModelBM25)) {
            return new QryResult();
        }
        QryResult res = new QryResult();
        //iterate all the terms to get the weight in each doc
        while (this.daatPtrs.size() > 0) {
            int nextDocID = getSmallestCurrentDocid();
            double tempRes = 0;
            for (int i=0; i<this.daatPtrs.size(); i++) {
                DaaTPtr ptr = this.daatPtrs.get(i);
                if (ptr.scoreList.getDocid(ptr.nextDoc) == nextDocID) {
                    tempRes = tempRes + ptr.scoreList.getDocidScore(ptr.nextDoc);
                    ptr.nextDoc++;
                }
            }
            res.docScores.add(nextDocID, tempRes);
            for (int i=this.daatPtrs.size()-1; i>=0; i--) {
                DaaTPtr ptri = this.daatPtrs.get(i);

                if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
                    this.daatPtrs.remove (i);
                }
            }
        }
        return res;
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
     * Get defaultScore
     * */
    public double getDefaultScore (RetrievalModel r, long docid) {
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

        return ("#SUM( " + result + ")");
    }
}
