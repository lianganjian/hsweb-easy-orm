package org.hswebframework.ezorm.rdb.operator.dml;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class ComplexQueryParameter {

    private List<SelectColumn> select = new ArrayList<>();

    private String from;

    private List<Term> where = new ArrayList<>();

    private List<Join> joins = new ArrayList<>();

    private List<SortOrder> orderBy = new ArrayList<>();

    private List<FunctionColumn> groupBy = new ArrayList<>();

    private List<FunctionTerm> having = new ArrayList<>();

    private Integer limit;

    private Integer offset;

    private Boolean forUpdate;

    public Optional<Join> findJoin(String targetName) {
        return Optional.ofNullable(joins)
                .flatMap(_joins -> _joins
                        .stream()
                        .filter(join -> join.equalsTargetOrAlias(targetName))
                        .findFirst());
    }
}
