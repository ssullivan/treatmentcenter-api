package com.github.ssullivan.db.postgres;

import com.github.ssullivan.model.MatchOperator;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.model.SetOperation;
import com.google.inject.ImplementedBy;
import java.util.List;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.TableField;

@ImplementedBy(ServiceConditionToSql.class)
public interface IServiceConditionToSql {
    <R extends Record> Condition toCondition(final TableField<R, Integer[]> field,
                                             final SetOperation setOperation,
                                             final List<ServicesCondition> conditions);

    String toSql(final SetOperation setOperation, final List<ServicesCondition> conditions);

    String toSql(final ServicesCondition condition);

    String toSql(final MatchOperator operator, final Set<Integer> items);
}
