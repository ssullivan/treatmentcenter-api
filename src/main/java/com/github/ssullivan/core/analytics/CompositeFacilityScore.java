package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class CompositeFacilityScore implements IScoreFacility {
  private Optional<ScoreByAge> scoreByAge = Optional.empty();
  private Optional<ScoreByGender> scoreByGender = Optional.empty();
  private Optional<ScoreByHearingSupport> scoreByHearingSupport = Optional.empty();
  private Optional<ScoreByLang> scoreByLang = Optional.empty();
  private Optional<ScoreByMedAssistedTreatment> scoreByMedAssistedTreatment = Optional.empty();
  private Optional<ScoreByMentalHealth> scoreByMentalHealth = Optional.empty();
  private Optional<ScoreByMilitaryFamilyStatus> scoreByMilitaryFamilyStatus = Optional.empty();
  private Optional<ScoreByServiceSetting> scoreByServiceSetting = Optional.empty();
  private Optional<ScoreBySmokingCessation> scoreBySmokingCessation = Optional.empty();
  private Optional<ScoreBySmokingPolicy> scoreBySmokingPolicy = Optional.empty();
  private Optional<ScoreBySubstanceDetoxServices> scoreBySubstanceDetoxServices = Optional.empty();
  private Optional<ScoreByTraumaServices> scoreByTraumaServices = Optional.empty();
  private Set<String> serviceCodes = ImmutableSet.of();

  public CompositeFacilityScore(final Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;

    //
    // scoreByGender = Optional.of(new ScoreByGender())
    scoreByHearingSupport = Optional.of(new ScoreByHearingSupport(serviceCodes));
    scoreByLang = Optional.of(new ScoreByLang(serviceCodes));
    scoreByMedAssistedTreatment = Optional.of(new ScoreByMedAssistedTreatment(serviceCodes));
    scoreByMentalHealth = Optional.of(new ScoreByMentalHealth(serviceCodes));
    scoreByMilitaryFamilyStatus = Optional.of(new ScoreByMilitaryFamilyStatus(serviceCodes));
    scoreByServiceSetting = Optional.of(new ScoreByServiceSetting(serviceCodes));
    scoreBySmokingCessation = Optional.of(new ScoreBySmokingCessation(serviceCodes));
    scoreBySmokingPolicy = Optional.of(new ScoreBySmokingPolicy(serviceCodes));
    scoreBySubstanceDetoxServices = Optional.of(new ScoreBySubstanceDetoxServices(serviceCodes));
    scoreByTraumaServices = Optional.of(new ScoreByTraumaServices(serviceCodes));
  }

  @Override
  public double score(final Facility facility) {
    return Stream.of(scoreByAge,
        scoreByGender,
        scoreByHearingSupport,
        scoreByLang,
        scoreByMedAssistedTreatment,
        scoreByMentalHealth, scoreByMilitaryFamilyStatus,
        scoreByServiceSetting,
        scoreBySmokingCessation,
        scoreBySmokingPolicy,
        scoreBySubstanceDetoxServices,
        scoreByTraumaServices)
        .filter(Optional::isPresent)
        .map(it -> it.get().score(facility))
        .reduce(0.0, Double::sum);
  }
}
