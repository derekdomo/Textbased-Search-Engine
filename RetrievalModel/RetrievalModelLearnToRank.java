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
    public boolean setParameter (String parameterName, String value){
        if (parameterName.equalsIgnoreCase("trainQueryFile")) {
            trainQueryFile=value;
        } else if (parameterName.equalsIgnoreCase("trainingQrelsFile")) {
            trainingQrelsFile=value;
        } else if (parameterName.equalsIgnoreCase("trainingFeatureVectorsFile")) {
            trainingFeatureVectorsFile=value;
        } else if (parameterName.equalsIgnoreCase("pageRankFile")) {
            pageRankFile=value;
        } else if  (parameterName.equalsIgnoreCase("featureDisable")) {
            featureDisable=value;
        } else if (parameterName.equalsIgnoreCase("svmRankLearnPath")) {
            svmRankLearnPath=value;
        } else if (parameterName.equalsIgnoreCase("svmRankClassifyPath")) {
            svmRankClassifyPath=value;
        } else if (parameterName.equalsIgnoreCase("svmRankModelFile")) {
            svmRankModelFile=value;
        } else if (parameterName.equalsIgnoreCase("testingFeatureVectorsFile")) {
            testingFeatureVectorsFile=value;
        } else if (parameterName.equalsIgnoreCase("testingDocumentScores")) {
            testingDocumentScores=value;
        } else {
            return false;
        }
        return true;
    }
}
