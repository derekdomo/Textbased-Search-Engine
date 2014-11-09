package QryOperator;
/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import DataStructure.*;
import Main.DocLengthStore;
import Main.MainEval;
import RetrievalModel.*;

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
  public double MLE;
  public String field;

  public DocLengthStore dls;
  /**
   *  Construct a new SCORE operator.  The SCORE operator accepts just
   *  one argument.
   *  @param q The query operator argument.
   *  @return @link{QryOperator.QryopSlScore}
   */
  public QryopSlScore(Qryop q) {
    this.args.add(q);
  }

  /**
   *  Construct a new SCORE operator.  Allow a SCORE operator to be
   *  created with no arguments.  This simplifies the design of some
   *  query parsing architectures.
   *  @return @link{QryOperator.QryopSlScore}
   */
  public QryopSlScore() {
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param a The query argument to append.
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluate the query operator.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean||r instanceof RetrievalModelRankedBoolean)
      return (evaluateBoolean (r));
    else if (r instanceof RetrievalModelIndri)
        return evaluateIndri(r);
    else if (r instanceof RetrievalModelBM25)
        return evaluateBM25(r);
    return null;
  }
    /**
     *  Evaluate the query operator for boolean retrieval models.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluateBM25(RetrievalModel r ) throws IOException {
        QryResult result = args.get(0).evaluate(r);
        double k_1 = ((RetrievalModelBM25) r).K_1();
        double k_3 = ((RetrievalModelBM25) r).K_3();
        double b = ((RetrievalModelBM25) r).B();
        int N = MainEval.READER.numDocs();
        dls = new DocLengthStore(MainEval.READER);
        String field = result.invertedList.field;
        int df = result.invertedList.df;
        float avg_docLen = MainEval.READER.getSumTotalTermFreq(field) /
                (float) MainEval.READER.getDocCount(field);
        for (int i=0; i<result.invertedList.df; i++) {
            long docLen = dls.getDocLength(field, result.invertedList.getDocid(i));
            double tempRes = (Math.log((N - df + 0.5) / (float) (df + 0.5)))
                    * ((result.invertedList.getTf(i)) /
                    (float) (result.invertedList.getTf(i) + k_1 * (1 - b + b * (docLen / (avg_docLen)))));
            result.docScores.add(result.invertedList.getDocid(i), tempRes);
        }
        return result;
    }
/**
 *  Evaluate the query operator for boolean retrieval models.
 *  @param r A retrieval model that controls how the operator behaves.
 *  @return The result of evaluating the query.
 *  @throws IOException
 */
   public QryResult evaluateIndri(RetrievalModel r ) throws IOException {
       QryResult result = args.get(0).evaluate(r);
       //Indri need to store the TF value and field information
       dls = new DocLengthStore(MainEval.READER);
       double lambda = ((RetrievalModelIndri) r).lambda();
       int mu = ((RetrievalModelIndri) r).mu();
       MLE=(result.invertedList.ctf)/((double)dls.getDocLength(result.invertedList.field));
       for (int i = 0; i < result.invertedList.df; i++) {
            double score = lambda*(result.invertedList.postings.get(i).tf+mu*MLE)
                    /((double)dls.getDocLength(result.invertedList.field,result.invertedList.postings.get(i).docid)+mu)
                    +(1-lambda)*MLE;
            result.docScores.add(result.invertedList.postings.get(i).docid,
                score);
       }
       this.field=result.invertedList.field;
       result.docScores.setField(result.invertedList.field);
       return result;
   }
 /**
   *  Evaluate the query operator for boolean retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    if (args.size()==0)
        return new QryResult();
    // Evaluate the query argument.
    QryResult result = args.get(0).evaluate(r);
    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.
    if (r instanceof RetrievalModelUnrankedBoolean)
        for (int i = 0; i < result.invertedList.df; i++) {

          // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
          // Unranked Boolean. All matching documents get a score of 1.0.
          result.docScores.add(result.invertedList.postings.get(i).docid,
                   (float) 1.0);
        }
    else if (r instanceof RetrievalModelRankedBoolean)
        for (int i = 0; i < result.invertedList.df; i++) {

            // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
            // Ranked Boolean. All matching documents get a score of 1.0.
            result.docScores.add(result.invertedList.postings.get(i).docid,
                    result.invertedList.getTf(i));
        }
    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.
    //if (result.invertedList.df > 0)
	//    result.invertedList = new InvList();
    return result;
  }

  /*
   *  Calculate the default score for a document that does not match
   *  the query argument.  This score is 0 for many retrieval models,
   *  but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);
    else if (r instanceof RetrievalModelIndri) {
        double lambda = ((RetrievalModelIndri) r).lambda();
        int mu = ((RetrievalModelIndri) r).mu();
        double defaultScore = MLE*
                (
                        lambda*mu/(double)(dls.getDocLength(field,(int)docid)+mu)
                                +1-lambda
                );

        return defaultScore;
    }
    return 0.0;
  }

  /**
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
      result += (i.next().toString() + " ");

    return ("#SCORE( " + result + ")");
  }
}
