package DataStructure;



/**
 *  All query operators return DataStructure.QryResult objects.  DataStructure.QryResult objects
 *  encapsulate the inverted lists (DataStructure.InvList) produced by QryOperator.QryopIl query
 *  operators and the score lists (DataStructure.ScoreList) produced by QryOperator.QryopSl
 *  query operators.  QryOperator.QryopIl query operators populate the
 *  invertedList and and leave the docScores empty.  QryOperator.QryopSl query
 *  operators leave the invertedList empty and populate the docScores.
 *  Encapsulating the two types of QryOperator.Qryop results in a single class
 *  makes it easy to build structured queries with nested query
 *  operators.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

public class QryResult {

  // Store the results of different types of query operators.

  public ScoreList docScores = new ScoreList();
  public InvList invertedList = new InvList();
  /**
   * sort the scoreList by the score in descending order.
   * */
  public void sort() throws IOException{
    docScores.sort();
  }
}
