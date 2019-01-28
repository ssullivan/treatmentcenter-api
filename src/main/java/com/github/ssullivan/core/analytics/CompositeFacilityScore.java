package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.collections.Tuple2;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import reactor.util.function.Tuple3;

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

  public CompositeFacilityScore(final Set<String> serviceCodes) {

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

  private CompositeFacilityScore(
      Optional<ScoreByAge> scoreByAge,
      Optional<ScoreByGender> scoreByGender,
      Optional<ScoreByHearingSupport> scoreByHearingSupport,
      Optional<ScoreByLang> scoreByLang,
      Optional<ScoreByMedAssistedTreatment> scoreByMedAssistedTreatment,
      Optional<ScoreByMentalHealth> scoreByMentalHealth,
      Optional<ScoreByMilitaryFamilyStatus> scoreByMilitaryFamilyStatus,
      Optional<ScoreByServiceSetting> scoreByServiceSetting,
      Optional<ScoreBySmokingCessation> scoreBySmokingCessation,
      Optional<ScoreBySmokingPolicy> scoreBySmokingPolicy,
      Optional<ScoreBySubstanceDetoxServices> scoreBySubstanceDetoxServices,
      Optional<ScoreByTraumaServices> scoreByTraumaServices) {
    this.scoreByAge = scoreByAge;
    this.scoreByGender = scoreByGender;
    this.scoreByHearingSupport = scoreByHearingSupport;
    this.scoreByLang = scoreByLang;
    this.scoreByMedAssistedTreatment = scoreByMedAssistedTreatment;
    this.scoreByMentalHealth = scoreByMentalHealth;
    this.scoreByMilitaryFamilyStatus = scoreByMilitaryFamilyStatus;
    this.scoreByServiceSetting = scoreByServiceSetting;
    this.scoreBySmokingCessation = scoreBySmokingCessation;
    this.scoreBySmokingPolicy = scoreBySmokingPolicy;
    this.scoreBySubstanceDetoxServices = scoreBySubstanceDetoxServices;
    this.scoreByTraumaServices = scoreByTraumaServices;
  }

  @Override
  public double score(final Facility facility) {
    if (facility == null) return 0.0;
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

  public static class Builder {
    private Set<String> serviceCodes = new HashSet<>();
    private LocalDate dateOfBirth = null;
    private Importance hearingSupport = null;
    private Importance langSupport = null;
    private Importance milSupport = null;

    private Importance medSupport = null;
    private Tuple2<Boolean, Set<TraumaTypes>> traumaSupport;

    public Builder withServiceCodes(final Set<String> serviceCodes) {
      this.serviceCodes = serviceCodes;
      return this;
    }

    public Builder withDateOfBirth(final LocalDate dateOfBirth) {
      if (null == dateOfBirth) return this;
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder withHearingSupport(final Importance importance) {
      this.hearingSupport = importance;
      return this;
    }

    public Builder withLangSupport(final Importance importance) {
      this.langSupport =  importance;
      return this;
    }

    public Builder withMedSupport(final Importance importance) {
      this.medSupport = importance;
      return this;
    }

    public Builder withMilitarySupport(final Importance importance) {
      this.milSupport = importance;
      return this;

    }

    public Builder withTraumaSupport(final Set<TraumaTypes> traumas) {
      this.traumaSupport = new Tuple2<>(traumas != null && !traumas.isEmpty(), traumas);
      return this;
    }

    public CompositeFacilityScore build() {
      return new CompositeFacilityScore(
          dateOfBirth != null ? Optional.of(new ScoreByAge(dateOfBirth)) : Optional.empty(),
          Optional.of(new ScoreByGender(serviceCodes)),
          Optional.of(new ScoreByHearingSupport(serviceCodes, hearingSupport)),
          Optional.of(new ScoreByLang(serviceCodes, langSupport)),
          Optional.of(new ScoreByMedAssistedTreatment(serviceCodes)),
          Optional.of(new ScoreByMentalHealth(serviceCodes)),
          Optional.of(new ScoreByMilitaryFamilyStatus(serviceCodes, milSupport)),
          Optional.of(new ScoreByServiceSetting(serviceCodes)),
          Optional.of(new ScoreBySmokingCessation(serviceCodes)),
          Optional.of(new ScoreBySmokingPolicy(serviceCodes)),
          Optional.of(new ScoreBySubstanceDetoxServices(serviceCodes)),
          traumaSupport != null ? Optional.of(new ScoreByTraumaServices(traumaSupport.get_1(), traumaSupport.get_2(), serviceCodes)) : Optional.of(new ScoreByTraumaServices(serviceCodes))
          );
    }
  }
}
