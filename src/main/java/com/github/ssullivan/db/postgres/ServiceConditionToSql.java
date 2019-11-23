package com.github.ssullivan.db.postgres;

import com.github.ssullivan.model.MatchOperator;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.model.SetOperation;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class ServiceConditionToSql implements IServiceConditionToSql {

  private static final String OR = "|";
  private static final String AND = "&";
  private static final String EMPTY = "";
  private final IServiceCodeLookupCache serviceCodeLookupCache;

  @Inject
  public ServiceConditionToSql(final IServiceCodeLookupCache cache) {
    this.serviceCodeLookupCache = cache;
  }

  private static final String stripSuffix(final String s, final String suffix) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    int lastIndex = s.lastIndexOf(suffix);
    if (lastIndex > -1) {
      return s.substring(0, lastIndex);
    } else {
      return s;
    }
  }

  public <R extends Record> Condition toCondition(final TableField<R, Integer[]> field,
      final SetOperation setOperation,
      final List<ServicesCondition> conditions) {
    final String rawSql = toSql(setOperation, conditions);
    if (rawSql == null || rawSql.isEmpty()) {
      return DSL.trueCondition();
    }
    //https://www.postgresql.org/docs/9.1/intarray.html
    return DSL.condition(field.getName() + " @@ '" + rawSql + "'::query_int");
  }

  @Override
  public String toSql(final SetOperation setOperation, final List<ServicesCondition> conditions) {
    final List<String> conditionsAsSql = conditions
        .stream()
        .map(this::toSql)
        .filter(s -> !Strings.isNullOrEmpty(s))
        .map(s -> "(" + s + ")")
        .collect(Collectors.toList());

    return Joiner.on(toBoolOp(setOperation)).join(conditionsAsSql);
  }

  @Override
  public String toSql(final ServicesCondition condition) {
    final Set<Integer> serviceCodesToMatch = this.serviceCodeLookupCache
        .lookupSet(condition.getServiceCodes());
    final Set<Integer> serviceCodesToNotMatch = this.serviceCodeLookupCache
        .lookupSet(condition.getMustNotServiceCodes());

    final String toMatch = toSql(condition.getMatchOperator(), serviceCodesToMatch);
    final String toNotMatch = toSqlNot(MatchOperator.MUST_NOT, serviceCodesToNotMatch);

    return Joiner.on(AND).join(
        ImmutableList.of(toMatch, toNotMatch).stream().filter(it -> it != null && !it.isEmpty())
            .collect(
                Collectors.toList()));
  }

  @Override
  public String toSql(final MatchOperator operator, final Set<Integer> items) {
    if (items == null || items.isEmpty()) {
      return "";
    }

    final Set<String> asStrings = items.stream().map(i -> EMPTY + i).collect(Collectors.toSet());
    return Joiner.on(toBoolOp(operator)).skipNulls().join(asStrings);
  }


  public String toSqlNot(final MatchOperator operator, final Set<Integer> items) {
    if (items == null || items.isEmpty()) {
      return "";
    }

    final Set<String> asStrings = items.stream().map(i -> "!" + i).collect(Collectors.toSet());
    return Joiner.on(toBoolOp(operator)).skipNulls().join(asStrings);
  }

  private String toBoolOp(final SetOperation setOperation) {
    switch (setOperation) {
      case UNION:
        return OR;
      case INTERSECTION:
      default:
        return AND;
    }

  }

  private String toBoolOp(final MatchOperator matchOperator) {
    switch (matchOperator) {
      case SHOULD:
        return OR;
      case MUST_NOT:
        return AND;
      case MUST:
      default:
        return AND;
    }
  }


}
