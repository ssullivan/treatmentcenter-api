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
    private Tuple2<Boolean, Importance> heardingSupport = null;
    private Tuple2<Boolean, Importance> langSupport = null;
    private Tuple2<Boolean, Importance> medSupport = null;
    private Boolean mentalHealthSupport = false;
    private Tuple2<Boolean, Importance> milSupport = null;
    private Boolean smokingCessatonSupport = null;
    private Boolean isSmoker = false;
    private Boolean detoxStarted = false;
    private Tuple2<Boolean, Set<TraumaTypes>> traumaSupport;
    private String gender = null;

    public Builder withServiceCodes(final Set<String> serviceCodes) {
      this.serviceCodes = serviceCodes;
      return this;
    }

    public Builder withGender(final String gender) {
      this.gender = gender;
      return this;
    }

    public Builder withDateOfBirth(final LocalDate dateOfBirth) {
      if (null == dateOfBirth) return this;
      this.dateOfBirth = dateOfBirth;
      return this;
    }

    public Builder withHearingSupport(final boolean enabled, final Importance importance) {
      this.heardingSupport = new Tuple2<>(enabled, importance);
      return this;
    }

    public Builder withLangSupport(final boolean enabled, final Importance importance) {
      this.langSupport = new Tuple2<>(enabled, importance);
      return this;
    }

    public Builder withMedSupport(final boolean enabled, final Importance importance) {
      this.medSupport = new Tuple2<>(enabled, importance);
      return this;
    }

    public Builder withMedSupport(final boolean enabled) {
      this.medSupport = new Tuple2<>(enabled, Importance.NOT);
      return this;
    }

    public Builder withMentalHealthSupprt(final boolean enabled) {
      this.mentalHealthSupport = enabled;
      return this;
    }

    public Builder withMilitarySupport(final boolean enabled, final Importance importance) {
      this.milSupport = new Tuple2<>(enabled, importance);
      return this;
    }

    public Builder withSmokingCessationSupport(final boolean enabled, final Importance importance) {
      this.smokingCessatonSupport = enabled;
      return this;
    }

    public Builder withSmokingPolicy(final boolean enabled) {
      this.smokingCessatonSupport = enabled;
      return this;
    }

    public Builder withDetoxStarted(final boolean enabled) {
      this.detoxStarted = enabled;
      return this;
    }

    public Builder withTraumaSupport(final boolean enabled, final Set<TraumaTypes> traumas) {
      this.traumaSupport = new Tuple2<>(enabled, traumas);
      return this;
    }

    public CompositeFacilityScore build() {
      return new CompositeFacilityScore(
          dateOfBirth != null ? Optional.of(new ScoreByAge(dateOfBirth)) : Optional.empty(),
          gender != null ? Optional.of(new ScoreByGender(gender)) : Optional.empty(),
          heardingSupport != null ? Optional.of(new ScoreByHearingSupport(serviceCodes, heardingSupport.get_1(), heardingSupport.get_2())) : Optional.of(new ScoreByHearingSupport(serviceCodes)),
          langSupport != null ? Optional.of(new ScoreByLang(serviceCodes, langSupport.get_1(), langSupport.get_2())) : Optional.of(new ScoreByLang(serviceCodes)),
          medSupport != null ? Optional.of(new ScoreByMedAssistedTreatment(serviceCodes, medSupport.get_1())) : Optional.of(new ScoreByMedAssistedTreatment(serviceCodes)),
          mentalHealthSupport != null ? Optional.of(new ScoreByMentalHealth(serviceCodes, mentalHealthSupport)) : Optional.of(new ScoreByMentalHealth(serviceCodes)),
          milSupport != null ? Optional.of(new ScoreByMilitaryFamilyStatus(serviceCodes, milSupport.get_2(), milSupport.get_1())) : Optional.of(new ScoreByMilitaryFamilyStatus(serviceCodes)),
          Optional.of(new ScoreByServiceSetting(serviceCodes)),
          smokingCessatonSupport != null ? Optional.of(new ScoreBySmokingCessation(serviceCodes, smokingCessatonSupport)) : Optional.of(new ScoreBySmokingCessation(serviceCodes)),
          isSmoker != null ? Optional.of(new ScoreBySmokingPolicy(serviceCodes, isSmoker)) : Optional.of(new ScoreBySmokingPolicy(serviceCodes, isSmoker)),
          detoxStarted != null ? Optional.of(new ScoreBySubstanceDetoxServices(serviceCodes, detoxStarted)) : Optional.of(new ScoreBySubstanceDetoxServices(serviceCodes)),
          traumaSupport != null ? Optional.of(new ScoreByTraumaServices(traumaSupport.get_1(), traumaSupport.get_2(), serviceCodes)) : Optional.of(new ScoreByTraumaServices(serviceCodes))
          );
    }
  }
}
