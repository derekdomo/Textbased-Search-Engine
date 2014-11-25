package RetrievalModel;

/**
 * Created by XiangyuSun on 11/14/14.
 */
public class RetrievalModelLearnToRank extends RetrievalModel {
    public String trainQueryFile;
    public String trainingQrelsFile;
    public String trainingFeatureVectorsFile;
    public String pageRankFile;
    public String featureDisable;
    public String svmRankLearnPath;
    public String svmRankClassifyPath;
    public double svmRankParamC;
    public String svmRankModelFile;
    public String testingFeatureVectorsFile;
    public String testingDocumentScores;
    public double k1,b,k3;
    public double mu, lambda;

    /**
     *  Set a retrieval model parameter.
     *  @param parameterName The name of the parameter to set.
     *  @param value The parameter's value.
     *  @return true if the parameter is set successfully, false otherwise.
     */
    public boolean setParameter (String parameterName, double value){
        if (parameterName.equalsIgnoreCase("svmRankParamC")) {
            svmRankParamC=value;
            return true;
        }
        return false;
    }

    /**
     *  Set a retrieval model parameter.
     *  @param parameterName The name of the parameter to set.
     *  @param value The parameter's value.
     *  @return true if the parameter is set successfully, false otherwise.
     */
    public boolean setParameter(String parameterName, String value) {
        if (parameterName.equalsIgnoreCase("letor:trainQueryFile")) {
            trainQueryFile = value;
        } else if (parameterName.equalsIgnoreCase("letor:trainingQrelsFile")) {
            trainingQrelsFile = value;
        } else if (parameterName.equalsIgnoreCase("letor:trainingFeatureVectorsFile")) {
            trainingFeatureVectorsFile = value;
        } else if (parameterName.equalsIgnoreCase("letor:pageRankFile=")) {
            pageRankFile = value;
        } else if (parameterName.equalsIgnoreCase("letor:featureDisable")) {
            featureDisable=value;
        } else if (parameterName.equalsIgnoreCase("letor:svmRankLearnPath")) {
            svmRankLearnPath=value;
        } else if (parameterName.equalsIgnoreCase("letor:svmRankClassifyPath")) {
            svmRankClassifyPath=value;
        } else if (parameterName.equalsIgnoreCase("letor:svmRankModelFile")) {
            svmRankModelFile=value;
        } else if (parameterName.equalsIgnoreCase("letor:testingFeatureVectorsFile")) {
            testingFeatureVectorsFile=value;
        } else if (parameterName.equalsIgnoreCase("letor:testingDocumentScores")) {
            testingDocumentScores=value;
        } else if (parameterName.equalsIgnoreCase("svmRankParamC")) {
            svmRankParamC = Double.parseDouble(value);
        }else if (parameterName.equalsIgnoreCase("BM25:k_1")) {
            k1 = Double.parseDouble(value);
        } else if (parameterName.equalsIgnoreCase("BM25:b")) {
            b = Double.parseDouble(value);
        } else if (parameterName.equalsIgnoreCase("BM25:k_3")) {
            k3 = Double.parseDouble(value);
        } else if (parameterName.equalsIgnoreCase("Indri:mu")) {
            mu = Integer.parseInt(value);
        } else if (parameterName.equalsIgnoreCase("Indri:lambda")) {
            System.out.println(lambda);
            lambda = Double.parseDouble(value);
        } else {
            return false;
        }
        return true;
    }
}
