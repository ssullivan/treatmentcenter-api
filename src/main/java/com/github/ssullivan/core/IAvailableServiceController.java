package com.github.ssullivan.core;

import com.github.ssullivan.model.Facility;
import com.google.inject.ImplementedBy;
import io.dropwizard.lifecycle.Managed;
import java.util.List;

@ImplementedBy(AvailableServiceController.class)
public interface IAvailableServiceController extends Managed {

  List<Facility> applyList(final List<Facility> facilities);

  Facility apply(final Facility facility);
}
