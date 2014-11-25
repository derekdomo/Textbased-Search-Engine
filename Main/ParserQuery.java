package Main;

import QryOperator.*;
import RetrievalModel.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Created by XiangyuSun on 11/19/14.
 */
public class ParserQuery {
    //  Create and configure an English analyzer that will be used for
    public static EnglishAnalyzerConfigurable analyzer =
            new EnglishAnalyzerConfigurable(Version.LUCENE_43);
    static {
        analyzer.setLowercase(true);
        analyzer.setStopwordRemoval(true);
        analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
    }
    public String query;
    public RetrievalModel r;
    public ParserQuery(String query, RetrievalModel r) {
        this.query = query;
        this.r = r;
    }
    public ParserQuery(){
        query=null;
        r=null;
    }
    public Qryop parseIt() throws IOException {
        Qryop currentOp = null;
        Stack<Qryop> stack = new Stack<Qryop>();

        // Add a default query operator to an unstructured query. This
        // is a tiny bit easier if unnecessary whitespace is removed.

        String qString = query.trim();

        if (query.charAt(0) != '#') {
            if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean) {
                query = "#or(" + query + ")";
            } else if (r instanceof RetrievalModelBM25) {
                qString = "#sum(" + qString + ")";
            } else if (r instanceof RetrievalModelIndri)
                qString = "#and(" + qString + ")";
        }
        if (r instanceof RetrievalModelIndri)
            qString = "#and(" + qString + ")";
        // Tokenize the query.

        StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
        String token ;

        // Each pass of the loop processes one token. To improve
        // efficiency and clarity, the query operator on the top of the
        // stack is also stored in currentOp.

        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken();
            if (token.matches("[  ,(\t\n\r]")) {
                // Ignore most delimiters.
            } else
            if (token.equalsIgnoreCase("#and")) {
                currentOp = new QryopSlAnd();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wsum")) {
                currentOp = new QryopSlWSUM();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wand")) {
                currentOp = new QryopSlWAND();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#syn")) {
                currentOp = new QryopIlSyn();
                stack.push(currentOp);
            } else if (token.length() > 6 && token.substring(0, 5).equalsIgnoreCase("#near")) {
                currentOp = new QryopIlNear(Integer.parseInt(token.substring(6)));
                stack.push(currentOp);
            } else if ((token.length() == 5 && token.equalsIgnoreCase("#near"))
                    || (token.length() == 6 && token.substring(0, 5).equalsIgnoreCase("#near"))) {
                //if not the legal format of NEAR, use 1 as argument
                currentOp = new QryopIlNear(1);
                stack.push(currentOp);
            } else if (token.length() > 8 && token.substring(0,7).equalsIgnoreCase("#window")) {
                currentOp = new QryopIlWindow(Integer.parseInt(token.substring(8)));
                stack.push(currentOp);
            } else if ((token.length() == 7 && token.equalsIgnoreCase("#window"))
                    || (token.length() == 8 && token.substring(0, 7).equalsIgnoreCase("#window"))) {
                currentOp = new QryopIlWindow(5);
                stack.push(currentOp);
            }else if (token.equalsIgnoreCase("#or")) {
                currentOp = new QryopSlOr();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#sum")) {
                currentOp = new QryopSlSum();
                stack.push(currentOp);
            } else if (token.startsWith(")")) {
                // Finish current query operator.
                // If the current query operator is not an argument to
                // another query operator (i.e., the stack is empty when it
                // is removed), we're done (assuming correct syntax - see
                // below). Otherwise, add the current operator as an
                // argument to the higher-level operator, and shift
                // processing back to the higher-level operator.
                stack.pop();
                Qryop arg = currentOp;
                if (tokens.hasMoreTokens()) {
                    if (stack.empty()) {
                        if (r instanceof RetrievalModelBM25) {
                            currentOp = new QryopSlSum();
                        } else
                            currentOp = new QryopSlAnd();
                        stack.push(currentOp);
                    } else
                        currentOp = stack.peek();
                } else
                    break;
                currentOp.add(arg);
            } else {

                // NOTE: You should do lexical processing of the token before
                // creating the query term, and you should check to see whether
                // the token specifies a particular field (e.g., apple.title).
                //now process the token to check whether it specifies a particular field
                //if go with the first construction,else go to the second one
                if (currentOp == null) {
                    System.out.println("Error Parsing ");
                    return null;
                }
                //process the weighted version
                boolean weight = false;
                try {
                    Double.valueOf(token);
                    weight = true;
                } catch(Exception e) {
                    weight = false;
                }
                if ( weight ) {
                    if (currentOp instanceof QryopSlWAND) {
                        if (((QryopSlWAND) currentOp).lengthWeight() == ((QryopSlWAND) currentOp).lengthArgs()) {
                            ((QryopSlWAND) currentOp).addWeight(Double.valueOf(token));
                            token = tokens.nextToken();
                        }
                    } else if (currentOp instanceof QryopSlWSUM) {
                        if (((QryopSlWSUM) currentOp).lengthWeight() == ((QryopSlWSUM) currentOp).lengthArgs()) {
                            ((QryopSlWSUM) currentOp).addWeight(Double.valueOf(token));
                            token = tokens.nextToken();
                        }
                    }
                }
                if (token.contains(".")) {
                    String[] temp = token.split("\\.");
                    if (temp.length > 1) {
                        String[] tt = tokenizeQuery(temp[0]);
                        if (tt.length == 0) {
                            if (currentOp instanceof QryopSlWSUM) {
                                ((QryopSlWSUM) currentOp).weights.remove(((QryopSlWSUM) currentOp).weights.size()-1);
                            } else if (currentOp instanceof QryopSlWAND) {
                                ((QryopSlWAND) currentOp).weights.remove(((QryopSlWAND) currentOp).weights.size()-1);
                            }
                            continue;
                        }
                        temp[0] = tt[0];
                        currentOp.add(new QryopIlTerm(temp[0], temp[1]));
                    } else if (temp.length == 1) {
                        String[] tt = tokenizeQuery(temp[0]);
                        if (tt.length == 0) {
                            if (currentOp instanceof QryopSlWAND) {
                                ((QryopSlWAND) currentOp).weights.remove(((QryopSlWAND) currentOp).weights.size()-1);
                            } else if (currentOp instanceof QryopSlWSUM) {
                                ((QryopSlWSUM) currentOp).weights.remove(((QryopSlWSUM) currentOp).weights.size()-1);
                            }
                            continue;
                        }
                        temp[0] = tt[0];
                        currentOp.add(new QryopIlTerm(temp[0]));
                    }
                } else {
                    if (token.equals(" "))
                        continue;
                    String[] tt = tokenizeQuery(token);
                    if (tt.length == 0) {
                        if (currentOp instanceof QryopSlWAND) {
                            ((QryopSlWAND) currentOp).weights.remove(((QryopSlWAND) currentOp).weights.size()-1);
                        } else if (currentOp instanceof QryopSlWSUM) {
                            ((QryopSlWSUM) currentOp).weights.remove(((QryopSlWSUM) currentOp).weights.size()-1);
                        }
                        continue;
                    }
                    token = tt[0];
                    currentOp.add(new QryopIlTerm(token));
                }
            }
        }
        //if current op is not a score operator
        //then add a score operator
        if (r instanceof RetrievalModelBM25) {
            if (!(currentOp instanceof QryopSlSum)) {
                Qryop temp = currentOp;
                currentOp = new QryopSlSum();
                currentOp.add(temp);
            }
        }
        if (currentOp instanceof QryopIl) {
            Qryop temp = currentOp;
            currentOp = new QryopSlScore();
            currentOp.add(temp);
        }
        // A broken structured query can leave unprocessed tokens on the
        // stack, so check for that.
        if (tokens.hasMoreTokens()) {
            System.err.println("Error:  Query syntax is incorrect.  " + qString);
            return null;
        }
        return currentOp;
    }

    /**
     * Given a query string, returns the terms one at a time with stopwords
     * removed and the terms stemmed using the Krovetz stemmer.
     * <p/>
     * Use this method to process raw query terms.
     *
     * @param query String containing query
     * @return Array of query tokens
     * @throws java.io.IOException
     */
    public static String[] tokenizeQuery(String query) throws IOException {

        Analyzer.TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
        TokenStream tokenStream = comp.getTokenStream();

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        List<String> tokens = new ArrayList<String>();
        while (tokenStream.incrementToken()) {
            String term = charTermAttribute.toString();
            tokens.add(term);
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    public void setPara(String query, RetrievalModel r) {
        this.query = query;
        this.r = r;
    }

}
