package com.github.ssullivan.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApiModel
public class AvailableServices {
  private ImmutableList<Category> categoryCodes;

  public AvailableServices() {
    this.categoryCodes = ImmutableList.of();
  }

  public AvailableServices(final Collection<Category> categoryCodes) {
    this.categoryCodes = ImmutableList.copyOf(categoryCodes);
  }

  public List<Category> getCategoryCodes() {
    if (this.categoryCodes == null)
      return new ArrayList<>();

    return new ArrayList<>(categoryCodes);
  }

  public void setCategoryCodes(final List<Category> categoryCodes) {
    if (categoryCodes == null)
      return;

    this.categoryCodes = ImmutableList.copyOf(categoryCodes);
  }

  public void addCategory(final Category category) {
    final ImmutableList.Builder<Category> builder = new Builder<>();
    builder.add(category);
    builder.addAll(categoryCodes);

    this.categoryCodes = builder.build();
  }
}
