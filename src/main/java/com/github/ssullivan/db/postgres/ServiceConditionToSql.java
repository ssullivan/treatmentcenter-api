package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.MatchOperator;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.model.SetOperation;
import com.google.common.base.Strings;
import org.jooq.*;
import org.jooq.impl.DSL;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceConditionToSql implements IServiceConditionToSql {
    private final IServiceCodeLookupCache serviceCodeLookupCache;
    private static final String OR = "|";
    private static final String AND = "&";
    private static final String EMPTY = "";

    @Inject
    public ServiceConditionToSql(final IServiceCodeLookupCache cache) {
        this.serviceCodeLookupCache = cache;
    }

    public <R extends Record> Condition toCondition(final TableField<R, Integer[]> field,
                                                    final SetOperation setOperation,
                                                    final List<ServicesCondition> conditions) {
        final String rawSql = toSql(setOperation, conditions);
        if (rawSql == null || rawSql.isEmpty()) {
            return DSL.condition(true);
        }
        return DSL.condition(field.getName() + " @@ " + rawSql + "::query_int");
    }

    @Override
    public String toSql(final SetOperation setOperation, final List<ServicesCondition> conditions) {
        final List<String> sql = conditions
                .stream()
                .map(this::toSql)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .map(s -> "(" + s + ")")
                .collect(Collectors.toList());

        switch (setOperation) {
            case UNION:
                return String.join(OR, sql);
            case INTERSECTION:
            default:
                return String.join(AND, sql);
        }

    }

    @Override
    public String toSql(final ServicesCondition condition) {
        final Set<Integer> serviceCodesToMatch = this.serviceCodeLookupCache.lookupSet(condition.getServiceCodes());
        final Set<Integer> serviceCodesToNotMatch = this.serviceCodeLookupCache.lookupSet(condition.getMustNotServiceCodes());

        String sql = toSql(condition.getMatchOperator(), serviceCodesToMatch);
        if (!sql.isEmpty()) {
            sql += AND + toSql(MatchOperator.MUST_NOT, serviceCodesToNotMatch);
        }
        else {
            sql = toSql(MatchOperator.MUST_NOT, serviceCodesToNotMatch);
        }

        return sql;
    }

    @Override
    public String toSql(final MatchOperator operator, final Set<Integer> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        final Set<String> asStrings = items.stream().map(i -> EMPTY + i).collect(Collectors.toSet());

        switch (operator) {
            case SHOULD:
                return "(" + String.join(OR, asStrings) + ")";
            case MUST_NOT:
                return "!(" + String.join(OR, asStrings) + ")";
            case MUST:
            default:
                return "(" + String.join(AND, asStrings) + ")";
        }
    }
}
