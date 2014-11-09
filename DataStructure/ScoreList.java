package DataStructure;
/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import Main.*;

import java.io.IOException;
import java.util.*;

public class ScoreList {
  //  A little utilty class to create a <docid, score> object.
  protected class ScoreListEntry {
    private int docid;
    private double score;
    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
  }
  protected String field;
  protected double MLE;

  public List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    if (this.scores.size()==0)
        return Integer.MAX_VALUE;
    if (this.scores.size()==n)
        return Integer.MAX_VALUE;
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }
  /**
   * check whether the docid is in the scores
   * @param docID
   * @return true or false
   * */
  public boolean existDoc(int docID) {
      for (int i=0; i<scores.size(); i++) {
          if (docID==scores.get(i).docid) {
              return true;
          }
      }
      return false;
  }

    /**
     * sort the score list by score
     * */
    public void sort() throws IOException{
        Comparator<ScoreListEntry> comp =  new Comparator<ScoreListEntry>() {
            @Override
            public int compare(ScoreListEntry scoreListEntry, ScoreListEntry scoreListEntry2){
                int result=Double.compare(scoreListEntry2.score,scoreListEntry.score);
                if (result==0) {
                    try{result=MainEval.compareExternalDocid(scoreListEntry.docid, scoreListEntry2.docid);}
                    catch(Exception e){}
                }
                return result;
            }
        };
        Collections.sort(this.scores,comp);
    }
    /**
     * FUNC for Indri: set the field for the term
     * */
    public void setField(String field) {
        this.field=field;
    }
    /**
     * FUNC for Indri: return the field for the term
     * */
    public String getField() {return this.field;}
    /**
     * FUNC for Indri: set the MLE for the term
     * */
    public void setMLE(double MLE) { this.MLE=MLE;}
    /**
     * FUNC for Indri: return the MLE for the term
     * */
    public double getMLE() {return this.MLE;}
 }
