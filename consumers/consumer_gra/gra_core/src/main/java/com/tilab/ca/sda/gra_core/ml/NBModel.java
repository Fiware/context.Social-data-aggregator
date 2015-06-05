/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tilab.ca.sda.gra_core.ml;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.util.MLUtils;


public class NBModel implements MlModel{
    
    private NaiveBayesModel model = null;

    @Override
    public void init(JavaSparkContext jsc, String trainingFilePath) {
        JavaRDD<LabeledPoint> trainingData = MLUtils.loadLibSVMFile(jsc.sc(), trainingFilePath).toJavaRDD();
        init(trainingData);
    }
    
    @Override
    public void init(JavaRDD<LabeledPoint> labeledPointsTrainingData) {
        model=NaiveBayes.train(labeledPointsTrainingData.rdd(), 1.0);
    }

    @Override
    public double predict(Vector features) {
        return model.predict(features);
    }
    
}
