/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.xpack.core.ml.action.EvaluateDataFrameAction;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification.Accuracy;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification.Classification;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification.MulticlassConfusionMatrix;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification.MulticlassConfusionMatrix.ActualClass;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification.MulticlassConfusionMatrix.PredictedClass;
import org.junit.After;
import org.junit.Before;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ClassificationEvaluationIT extends MlNativeDataFrameAnalyticsIntegTestCase {

    private static final String ANIMALS_DATA_INDEX = "test-evaluate-animals-index";

    private static final String ANIMAL_NAME_FIELD = "animal_name";
    private static final String ANIMAL_NAME_PREDICTION_FIELD = "animal_name_prediction";
    private static final String NO_LEGS_FIELD = "no_legs";
    private static final String NO_LEGS_PREDICTION_FIELD = "no_legs_prediction";
    private static final String IS_PREDATOR_FIELD = "predator";
    private static final String IS_PREDATOR_PREDICTION_FIELD = "predator_prediction";

    @Before
    public void setup() {
        indexAnimalsData(ANIMALS_DATA_INDEX);
    }

    @After
    public void cleanup() {
        cleanUp();
    }

    public void testEvaluate_DefaultMetrics() {
        EvaluateDataFrameAction.Response evaluateDataFrameResponse =
            evaluateDataFrame(ANIMALS_DATA_INDEX, new Classification(ANIMAL_NAME_FIELD, ANIMAL_NAME_PREDICTION_FIELD, null));

        assertThat(evaluateDataFrameResponse.getEvaluationName(), equalTo(Classification.NAME.getPreferredName()));
        assertThat(evaluateDataFrameResponse.getMetrics(), hasSize(1));
        assertThat(
            evaluateDataFrameResponse.getMetrics().get(0).getMetricName(),
            equalTo(MulticlassConfusionMatrix.NAME.getPreferredName()));
    }

    public void testEvaluate_Accuracy_KeywordField() {
        EvaluateDataFrameAction.Response evaluateDataFrameResponse =
            evaluateDataFrame(
                ANIMALS_DATA_INDEX, new Classification(ANIMAL_NAME_FIELD, ANIMAL_NAME_PREDICTION_FIELD, List.of(new Accuracy())));

        assertThat(evaluateDataFrameResponse.getEvaluationName(), equalTo(Classification.NAME.getPreferredName()));
        assertThat(evaluateDataFrameResponse.getMetrics(), hasSize(1));

        Accuracy.Result accuracyResult = (Accuracy.Result) evaluateDataFrameResponse.getMetrics().get(0);
        assertThat(accuracyResult.getMetricName(), equalTo(Accuracy.NAME.getPreferredName()));
        assertThat(
            accuracyResult.getActualClasses(),
            equalTo(
                List.of(
                    new Accuracy.ActualClass("ant", 15, 1.0 / 15),
                    new Accuracy.ActualClass("cat", 15, 1.0 / 15),
                    new Accuracy.ActualClass("dog", 15, 1.0 / 15),
                    new Accuracy.ActualClass("fox", 15, 1.0 / 15),
                    new Accuracy.ActualClass("mouse", 15, 1.0 / 15))));
        assertThat(accuracyResult.getOverallAccuracy(), equalTo(5.0 / 75));
    }

    public void testEvaluate_Accuracy_IntegerField() {
        EvaluateDataFrameAction.Response evaluateDataFrameResponse =
            evaluateDataFrame(
                ANIMALS_DATA_INDEX, new Classification(NO_LEGS_FIELD, NO_LEGS_PREDICTION_FIELD, List.of(new Accuracy())));

        assertThat(evaluateDataFrameResponse.getEvaluationName(), equalTo(Classification.NAME.getPreferredName()));
        assertThat(evaluateDataFrameResponse.getMetrics(), hasSize(1));

        Accuracy.Result accuracyResult = (Accuracy.Result) evaluateDataFrameResponse.getMetrics().get(0);
        assertThat(accuracyResult.getMetricName(), equalTo(Accuracy.NAME.getPreferredName()));
        assertThat(
            accuracyResult.getActualClasses(),
            equalTo(List.of(
                new Accuracy.ActualClass("1", 15, 1.0 / 15),
                new Accuracy.ActualClass("2", 15, 2.0 / 15),
                new Accuracy.ActualClass("3", 15, 3.0 / 15),
                new Accuracy.ActualClass("4", 15, 4.0 / 15),
                new Accuracy.ActualClass("5", 15, 5.0 / 15))));
        assertThat(accuracyResult.getOverallAccuracy(), equalTo(15.0 / 75));
    }

    public void testEvaluate_Accuracy_BooleanField() {
        EvaluateDataFrameAction.Response evaluateDataFrameResponse =
            evaluateDataFrame(
                ANIMALS_DATA_INDEX, new Classification(IS_PREDATOR_FIELD, IS_PREDATOR_PREDICTION_FIELD, List.of(new Accuracy())));

        assertThat(evaluateDataFrameResponse.getEvaluationName(), equalTo(Classification.NAME.getPreferredName()));
        assertThat(evaluateDataFrameResponse.getMetrics(), hasSize(1));

        Accuracy.Result accuracyResult = (Accuracy.Result) evaluateDataFrameResponse.getMetrics().get(0);
        assertThat(accuracyResult.getMetricName(), equalTo(Accuracy.NAME.getPreferredName()));
        assertThat(
            accuracyResult.getActualClasses(),
            equalTo(List.of(
                new Accuracy.ActualClass("true", 45, 27.0 / 45),
                new Accuracy.ActualClass("false", 30, 18.0 / 30))));
        assertThat(accuracyResult.getOverallAccuracy(), equalTo(45.0 / 75));
    }

    public void testEvaluate_ConfusionMatrixMetricWithDefaultSize() {
        EvaluateDataFrameAction.Response evaluateDataFrameResponse =
            evaluateDataFrame(
                ANIMALS_DATA_INDEX,
                new Classification(ANIMAL_NAME_FIELD, ANIMAL_NAME_PREDICTION_FIELD, List.of(new MulticlassConfusionMatrix())));

        assertThat(evaluateDataFrameResponse.getEvaluationName(), equalTo(Classification.NAME.getPreferredName()));
        assertThat(evaluateDataFrameResponse.getMetrics(), hasSize(1));

        MulticlassConfusionMatrix.Result confusionMatrixResult =
            (MulticlassConfusionMatrix.Result) evaluateDataFrameResponse.getMetrics().get(0);
        assertThat(confusionMatrixResult.getMetricName(), equalTo(MulticlassConfusionMatrix.NAME.getPreferredName()));
        assertThat(
            confusionMatrixResult.getConfusionMatrix(),
            equalTo(List.of(
                new ActualClass("ant",
                    15,
                    List.of(
                        new PredictedClass("ant", 1L),
                        new PredictedClass("cat", 4L),
                        new PredictedClass("dog", 3L),
                        new PredictedClass("fox", 2L),
                        new PredictedClass("mouse", 5L)),
                    0),
                new ActualClass("cat",
                    15,
                    List.of(
                        new PredictedClass("ant", 3L),
                        new PredictedClass("cat", 1L),
                        new PredictedClass("dog", 5L),
                        new PredictedClass("fox", 4L),
                        new PredictedClass("mouse", 2L)),
                    0),
                new ActualClass("dog",
                    15,
                    List.of(
                        new PredictedClass("ant", 4L),
                        new PredictedClass("cat", 2L),
                        new PredictedClass("dog", 1L),
                        new PredictedClass("fox", 5L),
                        new PredictedClass("mouse", 3L)),
                    0),
                new ActualClass("fox",
                    15,
                    List.of(
                        new PredictedClass("ant", 5L),
                        new PredictedClass("cat", 3L),
                        new PredictedClass("dog", 2L),
                        new PredictedClass("fox", 1L),
                        new PredictedClass("mouse", 4L)),
                    0),
                new ActualClass("mouse",
                    15,
                    List.of(
                        new PredictedClass("ant", 2L),
                        new PredictedClass("cat", 5L),
                        new PredictedClass("dog", 4L),
                        new PredictedClass("fox", 3L),
                        new PredictedClass("mouse", 1L)),
                    0))));
        assertThat(confusionMatrixResult.getOtherActualClassCount(), equalTo(0L));
    }

    public void testEvaluate_ConfusionMatrixMetricWithUserProvidedSize() {
        EvaluateDataFrameAction.Response evaluateDataFrameResponse =
            evaluateDataFrame(
                ANIMALS_DATA_INDEX,
                new Classification(ANIMAL_NAME_FIELD, ANIMAL_NAME_PREDICTION_FIELD, List.of(new MulticlassConfusionMatrix(3))));

        assertThat(evaluateDataFrameResponse.getEvaluationName(), equalTo(Classification.NAME.getPreferredName()));
        assertThat(evaluateDataFrameResponse.getMetrics(), hasSize(1));
        MulticlassConfusionMatrix.Result confusionMatrixResult =
            (MulticlassConfusionMatrix.Result) evaluateDataFrameResponse.getMetrics().get(0);
        assertThat(confusionMatrixResult.getMetricName(), equalTo(MulticlassConfusionMatrix.NAME.getPreferredName()));
        assertThat(
            confusionMatrixResult.getConfusionMatrix(),
            equalTo(List.of(
                new ActualClass("ant",
                    15,
                    List.of(new PredictedClass("ant", 1L), new PredictedClass("cat", 4L), new PredictedClass("dog", 3L)),
                    7),
                new ActualClass("cat",
                    15,
                    List.of(new PredictedClass("ant", 3L), new PredictedClass("cat", 1L), new PredictedClass("dog", 5L)),
                    6),
                new ActualClass("dog",
                    15,
                    List.of(new PredictedClass("ant", 4L), new PredictedClass("cat", 2L), new PredictedClass("dog", 1L)),
                    8))));
        assertThat(confusionMatrixResult.getOtherActualClassCount(), equalTo(2L));
    }

    private static void indexAnimalsData(String indexName) {
        client().admin().indices().prepareCreate(indexName)
            .addMapping("_doc",
                ANIMAL_NAME_FIELD, "type=keyword",
                ANIMAL_NAME_PREDICTION_FIELD, "type=keyword",
                NO_LEGS_FIELD, "type=integer",
                NO_LEGS_PREDICTION_FIELD, "type=integer",
                IS_PREDATOR_FIELD, "type=boolean",
                IS_PREDATOR_PREDICTION_FIELD, "type=boolean")
            .get();

        List<String> animalNames = List.of("dog", "cat", "mouse", "ant", "fox");
        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk()
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        for (int i = 0; i < animalNames.size(); i++) {
            for (int j = 0; j < animalNames.size(); j++) {
                for (int k = 0; k < j + 1; k++) {
                    bulkRequestBuilder.add(
                        new IndexRequest(indexName)
                            .source(
                                ANIMAL_NAME_FIELD, animalNames.get(i),
                                ANIMAL_NAME_PREDICTION_FIELD, animalNames.get((i + j) % animalNames.size()),
                                NO_LEGS_FIELD, i + 1,
                                NO_LEGS_PREDICTION_FIELD, j + 1,
                                IS_PREDATOR_FIELD, i % 2 == 0,
                                IS_PREDATOR_PREDICTION_FIELD, (i + j) % 2 == 0));
                }
            }
        }
        BulkResponse bulkResponse = bulkRequestBuilder.get();
        if (bulkResponse.hasFailures()) {
            fail("Failed to index data: " + bulkResponse.buildFailureMessage());
        }
    }
}
