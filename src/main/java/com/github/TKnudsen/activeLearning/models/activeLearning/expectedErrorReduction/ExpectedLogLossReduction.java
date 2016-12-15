package com.github.TKnudsen.activeLearning.models.activeLearning.expectedErrorReduction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.TKnudsen.ComplexDataObject.data.entry.EntryWithComparableKey;
import com.github.TKnudsen.ComplexDataObject.data.features.numericalData.NumericalFeature;
import com.github.TKnudsen.ComplexDataObject.data.features.numericalData.NumericalFeatureVector;
import com.github.TKnudsen.ComplexDataObject.data.ranking.Ranking;
import com.github.TKnudsen.activeLearning.models.activeLearning.AbstractActiveLearningModel;
import com.github.TKnudsen.activeLearning.models.learning.classification.IClassifier;

/**
 * @author Christian Ritter
 */
public class ExpectedLogLossReduction extends AbstractActiveLearningModel {

	public ExpectedLogLossReduction(IClassifier<Double, NumericalFeatureVector> learningModel) {
		super(learningModel);
	}

	@Override
	protected void calculateRanking(int count) {
		ranking = new Ranking<>();
		remainingUncertainty = 0.0;

		if (learningCandidateFeatureVectors.size() < 1)
			return;

		int U = learningCandidateFeatureVectors.size();
		List<Map<String, Double>> dists = new ArrayList<>();
		for (NumericalFeatureVector fv : learningCandidateFeatureVectors) {
			dists.add(learningModel.getLabelDistribution(fv));
		}
		Set<String> labels = new HashSet<>();
		for (Map<String, Double> map : dists) {
			labels.addAll(map.keySet());
		}
		for (int i = 0; i < U; i++) {
			NumericalFeatureVector fv = learningCandidateFeatureVectors.get(i);
			Map<String, Double> dist = dists.get(i);
			double loss = 0;
			for (String label : labels) {
				List<NumericalFeatureVector> newTrainingSet = new ArrayList<>();
				newTrainingSet.addAll(learningCandidateFeatureVectors);
				// TODO is it ok to assign a label to the original feature
				// vector assuming that the label is only input but not output?
				fv.addFeature(new NumericalFeature("class", Double.valueOf(label)));
				newTrainingSet.add(fv);
				IClassifier<Double, NumericalFeatureVector> newClassifier = null;
				try {
					// newClassifier = learningModel.getClass().newInstance();
					newClassifier = learningModel.createParameterizedCopy();
					newClassifier.train(newTrainingSet, "class");
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				double sum = 0;
				for (int j = 0; j < U; j++) {
					if (newClassifier == null)
						break;
					if (i != j) {
						Map<String, Double> d = newClassifier.getLabelDistribution(learningCandidateFeatureVectors.get(j));
						for (String l : d.keySet()) {
							sum += d.get(l) * Math.log(d.get(l));
						}
					}
				}
				sum *= -1;
				loss += dist.get(label) * sum;
			}
			ranking.add(new EntryWithComparableKey<>(loss, fv));
			remainingUncertainty += loss;
		}
		remainingUncertainty /= U;
		System.out.println("Expected01LossReduction: remaining uncertainty = " + remainingUncertainty);
	}

	@Override
	public String getDescription() {
		return "ExpectedLogLossReduction";
	}

	@Override
	public String getName() {
		return "ExpectedLogLossReduction";
	}
}