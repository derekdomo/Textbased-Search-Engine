package QryOperator;

/**
 * This class implements the Near operator for all retrieval models
 * It handles the InvertedList
 * Created by XiangyuSun on 14-9-6.
 */
import DataStructure.InvList;
import DataStructure.QryResult;
import Main.*;
import RetrievalModel.RetrievalModel;
import java.io.IOException;
import java.util.*;
public class QryopIlNear extends QryopIl {
    //store the distance of the n words
    private int distance;
    /**
     *  The constructor is to accept a variable number as the distance.
     * #Near/distance(arg1 arg2 ...).
     */
    public QryopIlNear(int distance) {
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
        //iterate all the docs in the first term vector
        int length = this.daatPtrs.size();
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
            for (int i=this.daatPtrs.size()-1; i>=0; i--) {
                DaaTPtr ptri = this.daatPtrs.get(i);

                if (ptri.nextDoc >= ptri.invList.postings.size()) {
                    this.daatPtrs.remove (i);
                }
            }
            if (docs.size()!=length)
                continue;
            //check if the size of docs != size of daatPtrs
            for (int i = 0; i < docs.size(); i++) {
                if (i + 1 == docs.size()) {
                    //The last one has no one to get near
                    Collections.sort(positions);
                    if (positions.size() == 0)
                        break;
                    result.invertedList.appendPosting(nextDocid,positions);

                    break;
                }
                else {
                    List<Integer> it1 = new ArrayList<Integer>();
                    List<Integer> it2 = new ArrayList<Integer>();
                    if (i == 0)
                        it1.addAll(docs.get(0).positions);
                    else
                        it1 = positions;
                    //empty the result list
                    positions = new ArrayList<Integer>();

                    it2.addAll(docs.get(i + 1).positions);
                    int count1 = 0;
                    int count2 = 0;
                    Collections.sort(it1);
                    Collections.sort(it2);
                    while (count1 < it1.size() && count2 < it2.size()) {
                        if ((it2.get(count2) - it1.get(count1) <= distance) &&
                                (it2.get(count2) - it1.get(count1) >= 0)) {
                            positions.add(it2.get(count2));
                            count1++;
                            count2++;
                        } else if ((it2.get(count2) - it1.get(count1) < 0)) {
                            //iterator on list2 moves
                            count2++;
                        } else {//iterator on list1 moves
                            count1++;
                        }
                    }
                }
            }
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

        return ("#NEAR( " + result + ")");
    }
}
