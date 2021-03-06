package org.hswebframework.ezorm.core.param;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 执行条件
 */
public class Term implements Cloneable {

    /**
     * 字段
     */
    private String column;

    /**
     * 值
     */
    private Object value;

    /**
     * 链接类型
     */
    private Type type = Type.and;

    /**
     * 条件类型
     */
    private String termType = TermType.eq;

    /**
     * 自定义选项,用于在某些自定义的termType中进行自定义生产sql的规则.
     * 可通过column进行设置如: name$like$reverse$func-concat,则column为name,termType为like,options为[reverse,func-concat]
     *
     * @since 3.0.1
     */
    private List<String> options = new ArrayList<>();

    /**
     * 嵌套的条件
     */
    private List<Term> terms = new LinkedList<>();


    public Term or(String term, Object value) {
        return or(term, TermType.eq, value);
    }

    public Term and(String term, Object value) {
        return and(term, TermType.eq, value);
    }

    public Term or(String term, String termType, Object value) {
        Term queryTerm = new Term();
        queryTerm.setTermType(termType);
        queryTerm.setColumn(term);
        queryTerm.setValue(value);
        queryTerm.setType(Type.or);
        terms.add(queryTerm);
        return this;
    }

    public Term and(String term, String termType, Object value) {
        Term queryTerm = new Term();
        queryTerm.setTermType(termType);
        queryTerm.setColumn(term);
        queryTerm.setValue(value);
        queryTerm.setType(Type.and);
        terms.add(queryTerm);
        return this;
    }

    public Term nest() {
        return nest(null, null);
    }

    public Term orNest() {
        return orNest(null, null);
    }

    public Term nest(String term, Object value) {
        Term queryTerm = new Term();
        queryTerm.setType(Type.and);
        queryTerm.setColumn(term);
        queryTerm.setValue(value);
        terms.add(queryTerm);
        return queryTerm;
    }

    public Term orNest(String term, Object value) {
        Term queryTerm = new Term();
        queryTerm.setType(Type.or);
        queryTerm.setColumn(term);
        queryTerm.setValue(value);
        terms.add(queryTerm);
        return queryTerm;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        if (column == null) return;
        if (column.contains("$")) {
            String tmp[] = column.split("[$]");
            setTermType(tmp[1]);
            column = tmp[0];
            if (tmp.length > 2) {
                options.addAll(Arrays.asList(tmp).subList(2, tmp.length));
            }
        }
        this.column = column;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getTermType() {
        return termType.toLowerCase();
    }

    public void setTermType(String termType) {
        if (termType.contains("$")) {
            String tmp[] = termType.split("[$]");
            termType = tmp[0];
            if (tmp.length > 1) {
                options.addAll(Arrays.asList(tmp).subList(1, tmp.length));
            }
        }
        this.termType = termType;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public void setTerms(List<Term> terms) {
        this.terms = terms;
    }

    public Term addTerm(Term term) {
        terms.add(term);
        return this;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @Override
    public Term clone() {
        Term term = new Term();
        term.setColumn(column);
        term.setValue(value);
        term.setTermType(termType);
        term.setType(type);
        terms.forEach(t -> term.addTerm(t.clone()));
        term.setOptions(new ArrayList<>(getOptions()));
        return term;
    }

    public enum Type {
        or, and;

        public static Type fromString(String str) {
            try {
                return Type.valueOf(str.toLowerCase());
            } catch (Exception e) {
                return and;
            }
        }
    }


}
