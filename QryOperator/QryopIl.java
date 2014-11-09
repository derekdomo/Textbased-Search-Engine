package QryOperator;
/**
 *  All query operators that return inverted lists are subclasses of
 *  the QryOperator.QryopIl class.  This class has two main purposes.  First, it
 *  allows query operators to easily recognize any nested query
 *  operator that returns an inverted list (e.g., #AND (a #NEAR/1 (b c)).
 *  Second, it is a place to store data structures and methods that are
 *  common to all query operators that return inverted lists.
 *  
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import DataStructure.QryResult;
import RetrievalModel.RetrievalModel;

import java.io.*;

public abstract class QryopIl extends Qryop {

  /**
   *  Use the specified retrieval model to evaluate the query arguments.
   *  Define and return DaaT pointers that the query operator can use.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return void
   *  @throws IOException
   */
  public void allocDaaTPtrs (RetrievalModel r) throws IOException {
    //iterate all the operators in the list
    for (int i=0; i<this.args.size(); i++) {
      //get a doc structure
      DaaTPtr ptri = new DaaTPtr ();
      //evaluate the pre op and put the result into the doc
      QryResult res = this.args.get(i).evaluate(r);
      ptri.invList = res.invertedList;
      ptri.scoreList = null;
      ptri.nextDoc = 0;
      this.daatPtrs.add (ptri);
    }
  }

}
