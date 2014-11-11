package QryOperator;
/**
 *  This class implements the AND operator for all retrieval models.
 *  It handles the ScoreList
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import DataStructure.*;
import Main.*;
import RetrievalModel.*;
import java.util.*;
import java.io.*;

public class QryopSlAnd extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlAnd(Qryop... q) {
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
    else if (r instanceof RetrievalModelIndri)
        return (evaluateIndri (r));
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

    if (args.size()==0)
        return new QryResult();
    //  Initialization

    allocDaaTPtrs (r);
    QryResult result = new QryResult ();

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match AND without changing
    //  the result.
    // Sort the term vectors by size
    for (int i=0; i<(this.daatPtrs.size()-1); i++) {
      for (int j=i+1; j<this.daatPtrs.size(); j++) {
	        if (this.daatPtrs.get(i).scoreList.scores.size() >
	            this.daatPtrs.get(j).scoreList.scores.size()) {
	            ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
	            this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
	            this.daatPtrs.get(j).scoreList = tmpScoreList;
	        }
      }
    }

    //  Exact-match AND requires that ALL scoreLists contain a
    //  document id.  Use the first (shortest) list to control the
    //  search for matches.

    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.

    DaaTPtr ptr0 = this.daatPtrs.get(0);
    //iterate the doc of the first term from the least doc id
    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
      double docScore=1.0;
      if (r instanceof RetrievalModelRankedBoolean)
        docScore = ptr0.scoreList.getDocidScore(ptr0.nextDoc);
      //  Do the other query arguments have the ptr0Docid?

      for (int j=1; j<this.daatPtrs.size(); j++) {

        DaaTPtr ptrj = this.daatPtrs.get(j);

        while (true) {
          if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
            break EVALUATEDOCUMENTS;		// No more docs can match
          else
            if (ptrj.scoreList.getDocid (ptrj.nextDoc) > ptr0Docid)
              continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
          else
            if (ptrj.scoreList.getDocid (ptrj.nextDoc) < ptr0Docid)
              ptrj.nextDoc ++;			// Not yet at the right doc.
          else
              break;				// ptrj matches ptr0Docid
        }
        if (r instanceof RetrievalModelRankedBoolean) {
            if (docScore>ptrj.scoreList.getDocidScore(ptrj.nextDoc))
                //docScore should be the least number if Ranked
                docScore=ptrj.scoreList.getDocidScore(ptrj.nextDoc);
        }
      }

      //  The ptr0Docid matched all query arguments, so save it.
      result.docScores.add (ptr0Docid, docScore);
    }

    freeDaaTPtrs ();

    return result;
  }
    /**
     *  Evaluates the query operator for Indri retrieval models,
     *  including any child operators and returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluateIndri (RetrievalModel r) throws IOException {
        //  Initialization

        allocDaaTPtrs (r);
        QryResult result = new QryResult ();

        boolean alldone=false;
        int length = this.daatPtrs.size();
        for (int i=0; i<this.daatPtrs.size(); i++) {
            if (this.daatPtrs.get(i).scoreList.scores.size()==0)
                length-=1;
        }
        while (!alldone) {
            int nextDocID = getSmallestCurrentDocid();
            if (nextDocID==Integer.MAX_VALUE)
                break;
            double score=1;
            for (int i=0; i<this.daatPtrs.size(); i++) {
                DaaTPtr ptr = this.daatPtrs.get(i);
                if (ptr.nextDoc>=ptr.scoreList.scores.size()||nextDocID != ptr.scoreList.getDocid(ptr.nextDoc)) {
                    double defaultscore = ((QryopSl)this.args.get(i)).getDefaultScore(r, nextDocID);
                    if (defaultscore==0||Double.isNaN(defaultscore))
                        defaultscore=1;
                    score = score * defaultscore;
                }else if (nextDocID == ptr.scoreList.getDocid(ptr.nextDoc)) {
                    score = score * ptr.scoreList.getDocidScore(ptr.nextDoc);
                    ptr.nextDoc++;
                }
            }
            score=Math.pow(score,1/(double)length);
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
        double score=1;
        int length=this.args.size();
        for (int i=0; i<this.args.size(); i++) {
            if (this.args.get(i).args.size()==0){
                length=length-1;
                continue;
            }
            if (this.args.get(i) instanceof QryopSl)
                score=score*((QryopSl) this.args.get(i)).getDefaultScore(r, docid);
        }
        score = Math.pow(score, 1/(double)length);
        if (this.args.size()==0)
            return 0;
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
            if (ptri.nextDoc>=ptri.scoreList.scores.size())
                continue;
            if (nextDocid > ptri.scoreList.getDocid (ptri.nextDoc))
                nextDocid = ptri.scoreList.getDocid (ptri.nextDoc);
        }

        return (nextDocid);
    }
  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#AND( " + result + ")");
  }
}
