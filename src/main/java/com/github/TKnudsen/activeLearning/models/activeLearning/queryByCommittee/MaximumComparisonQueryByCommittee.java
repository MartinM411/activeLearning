package com.github.TKnudsen.activeLearning.models.activeLearning.queryByCommittee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.TKnudsen.ComplexDataObject.data.entry.EntryWithComparableKey;
import com.github.TKnudsen.ComplexDataObject.data.features.numericalData.NumericalFeatureVector;
import com.github.TKnudsen.ComplexDataObject.data.ranking.Ranking;
import com.github.TKnudsen.activeLearning.models.learning.classification.IClassifier;

public class MaximumComparisonQueryByCommittee extends AbstractQueryByCommitteeActiveLearning {

	public MaximumComparisonQueryByCommittee(List<IClassifier<Double, NumericalFeatureVector>> learningModels) {
		super(learningModels);
	}

	@Override
	public String getComparisonMethod() {
		return "Measures the disagreeing suggestions for labels of different models.";
	}

	@Override
	protected void calculateRanking(int count) {
		for (IClassifier<Double, NumericalFeatureVector> classifier : learningModels)
			classifier.test(learningCandidateFeatureVectors);

		ranking = new Ranking<>();
		remainingUncertainty = 0.0;

		// calculate overall score
		for (NumericalFeatureVector fv : learningCandidateFeatureVectors) {
			List<Map<String, Double>> labelDistributions = new ArrayList<>();
			for (IClassifier<Double, NumericalFeatureVector> classifier : learningModels)
				labelDistributions.add(classifier.getLabelDistribution(fv));

			// create unified distribution arrays
			Set<String> labelSet = new HashSet<>();
			for (Map<String, Double> map : labelDistributions)
				if (map != null)
					for (String s : map.keySet())
						labelSet.add(s);

			List<List<Double>> distributions = new ArrayList<>();
			for (Map<String, Double> map : labelDistributions) {
				if (map == null)
					continue;
				List<Double> values = new ArrayList<>();
				for (String s : labelSet)
					if (map.get(s) != null)
						values.add(map.get(s));
					else
						values.add(0.0);
				distributions.add(values);
			}

			double dist = 0;

			if (distributions != null && distributions.size() > 0) {
				Set<String> winningLabels = new HashSet<>();
				for (IClassifier<Double, NumericalFeatureVector> classifier : learningModels) {
					List<String> test = classifier.test(Arrays.asList(fv));
					if (test != null && test.size() > 0)
						winningLabels.add(classifier.test(Arrays.asList(fv)).get(0));
				}
				dist = (winningLabels.size() - 1) / (double) distributions.size();
			} else
				dist = 1;

			// update ranking
			ranking.add(new EntryWithComparableKey<Double, NumericalFeatureVector>(1 - dist, fv));
			remainingUncertainty += dist;

			if (ranking.size() > count)
				ranking.remove(ranking.size() - 1);
		}

		remainingUncertainty /= (double) learningCandidateFeatureVectors.size();
		System.out.println("QueryByCommitteeActiveLearningModel: remaining uncertainty = " + remainingUncertainty);

	}

}