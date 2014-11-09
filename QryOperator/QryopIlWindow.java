package QryOperator;

import DataStructure.InvList;
import DataStructure.QryResult;
import Main.MainEval;
import RetrievalModel.RetrievalModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by XiangyuSun on 10/10/14.
 */
public class QryopIlWindow extends QryopIl {
    //store the distance of the n words
    private int distance;
    /**
     *  The constructor is to accept a variable number as the distance.
     * #Near/distance(arg1 arg2 ...).
     */
    public QryopIlWindow(int distance) {
        this.distance=distance;
    }
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
        //near with only one argument
        if (this.daatPtrs.size() == 1) {
            QryResult temp = new QryResult();
            temp.invertedList = getInvertedList(0);
            return temp;
        }
        //near with no arguments
        if (args.size()==0)
            return new QryResult();
        //initiate the result list
        QryResult result = new QryResult();
        result.invertedList.field = new String (this.daatPtrs.get(0).invList.field);
        //iterate all the docs in the first term vector\
        int len = this.daatPtrs.size();
        while (this.daatPtrs.size() > 0) {
            int nextDocid = getSmallestCurrentDocid();
            //  Create a list that stores all the docPostings
            //  that match the nextDocID
            //  And create a temp list that stores the new phrase's position
            List<InvList.DocPosting> docs = new ArrayList<InvList.DocPosting>();
            List<Integer> positions = new ArrayList<Integer>();
            //iterate all the terms and get the targeted docPostings
            for (int i = 0; i < this.daatPtrs.size(); i++) {
                DaaTPtr ptri = this.daatPtrs.get(i);//represent a term vector
                if (ptri.invList.getDocid(ptri.nextDoc) == nextDocid) {
                    docs.add(ptri.invList.getDoc(ptri.nextDoc));
                    ptri.nextDoc++;
                }
            }
            int length = this.daatPtrs.size();
            //iterate all the terms to discard those that have been scanned all docs
            for (int i = length-1; i >= 0; i--) {
                DaaTPtr ptri = this.daatPtrs.get(i);

                if (ptri.nextDoc >= ptri.invList.postings.size()) {
                    this.daatPtrs.remove (i);
                }
            }
            if  (docs.size() != len) {
                continue;
            }
            //iterate all the terms to check whether it satisfy WINDOWS
            int max = 0;
            int min = 0;
            boolean check = true;
            while (check) {
                //find the max position and min position
                for ( int i = 0; i < length; i++ ) {
                    if ( docs.get(max).positions.get(0) <
                            docs.get(i).positions.get(0) )
                        max = i;
                    else if ( docs.get(min).positions.get(0) >
                            docs.get(i).positions.get(0) )
                        min = i;
                }
                if ( docs.get(max).positions.get(0) - docs.get(min).positions.get(0)+1 <= distance ) {
                    positions.add( docs.get(max).positions.get(0) );
                    for (int i = 0; i < length; i++ ) {
                        docs.get(i).positions.removeElementAt(0);
                    }
                }
                else {
                    docs.get(min).positions.removeElementAt(0);
                }
                for ( int i = 0; i < length; i++ ) {
                    if ( docs.get(i).positions.size() == 0 ){
                        check = false;
                        break;
                    }
                }
            }
            if (positions.size()!=0)
                result.invertedList.appendPosting(nextDocid, positions);
        }
        freeDaaTPtrs();
        return result;
    }
    /**
     *  Return the smallest unexamined docid from the DaaTPtrs.
     *  @return The smallest internal document id.
     */
    public int getSmallestCurrentDocid () {

        int nextDocid = Integer.MAX_VALUE;

        for (int i=0; i<this.daatPtrs.size(); i++) {
            DaaTPtr ptri = this.daatPtrs.get(i);
            if (nextDocid > ptri.invList.getDocid (ptri.nextDoc))
                nextDocid = ptri.invList.getDocid (ptri.nextDoc);
        }

        return (nextDocid);
    }

    /**
     *  syntaxCheckArgResults does syntax checking that can only be done
     *  after query arguments are evaluated.
     *  @param ptrs A list of DaaTPtrs for this query operator.
     *  @return True if the syntax is valid, false otherwise.
     */
    public Boolean syntaxCheckArgResults (List<DaaTPtr> ptrs) {

        for (int i=0; i<this.args.size(); i++) {

            if (! (this.args.get(i) instanceof QryopIl))
                MainEval.fatalError("Error:  Invalid argument in " +
                        this.toString());
            else
            if ((i>0) &&
                    (! ptrs.get(i).invList.field.equals (ptrs.get(0).invList.field)))
                MainEval.fatalError("Error:  Arguments must be in the same field:  " +
                        this.toString());
        }

        return true;
    }

    /*
     *  Return a string version of this query operator.
     *  @return The string version of this query operator.
     */
    public String toString(){

        String result = new String ();

        for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
            result += (i.next().toString() + " ");

        return ("#WINDOW( " + result + ")");
    }
}

