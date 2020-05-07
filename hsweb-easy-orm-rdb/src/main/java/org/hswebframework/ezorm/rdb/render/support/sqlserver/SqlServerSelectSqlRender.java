package org.hswebframework.ezorm.rdb.render.support.sqlserver;

import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.rdb.executor.SQL;
import org.hswebframework.ezorm.rdb.meta.Correlation;
import org.hswebframework.ezorm.rdb.meta.RDBColumnMetaData;
import org.hswebframework.ezorm.rdb.meta.RDBTableMetaData;
import org.hswebframework.ezorm.rdb.render.SqlAppender;
import org.hswebframework.ezorm.rdb.render.dialect.Dialect;
import org.hswebframework.ezorm.rdb.render.support.simple.CommonSqlRender;
import org.hswebframework.ezorm.rdb.render.support.simple.SimpleSQL;
import org.hswebframework.ezorm.rdb.render.support.simple.SimpleWhereSqlBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 2.0
 */
@SuppressWarnings("all")
public class SqlServerSelectSqlRender extends CommonSqlRender<QueryParam> {

    private Dialect dialect;

    public SqlServerSelectSqlRender(Dialect dialect) {
        this.dialect = dialect;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

    class SimpleSelectSqlRenderProcess extends SimpleWhereSqlBuilder {
        private RDBTableMetaData metaData;
        private QueryParam param;
        private List<OperationColumn> selectField;
        private SqlAppender whereSql = new SqlAppender();
        private Set<String> needSelectTable = new LinkedHashSet<>();
        private List<Sort> sorts = new ArrayList<>();

        public SimpleSelectSqlRenderProcess(RDBTableMetaData metaData, QueryParam param) {
            this.metaData = metaData;
            this.param = param;
            if (param.getIncludes().isEmpty() && param.getExcludes().isEmpty()) {
                param.includes("*");
                metaData.getCorrelations().stream().filter(correlation -> correlation.isOne2one())
                        .forEach(correlation -> param.includes(correlation.getAlias() + ".*"));
            }
            //解析要查询的字段
            this.selectField = parseOperationField(metaData, param);
            //解析查询条件
            buildWhere(metaData, "", param.getTerms(), whereSql, needSelectTable);
            if (!whereSql.isEmpty()) {
                whereSql.removeFirst();
            } ;
            //加入要查询的表
            needSelectTable.addAll(this.selectField.stream().map(OperationColumn::getTableName).collect(Collectors.toList()));

            param.getSorts().forEach(sort -> {
                RDBColumnMetaData rDBColumnMetaData = metaData.findColumn(sort.getName());
                if (rDBColumnMetaData.getName() == null) return;
                String tableName = getTableAlias(metaData, sort.getName());
                needSelectTable.add(tableName);
                sort.setName(getDialect().buildColumnName(tableName, rDBColumnMetaData.getName()));
                sorts.add(sort);
            });
        }

        public SQL process() {
            SqlAppender appender = new SqlAppender();
            appender.add("SELECT ");

            if (selectField.isEmpty()) {
                appender.add(" * ");
            }
            selectField.forEach(operationColumn -> {
                RDBColumnMetaData rDBColumnMetaData = operationColumn.getRDBColumnMetaData();
                String tableName = rDBColumnMetaData.getTableMetaData().getName();
                Correlation correlation = metaData.getCorrelation(tableName);
                if (correlation == null) {
                    appender.add(getDialect().buildColumnName(operationColumn.getTableName(), rDBColumnMetaData.getName())
                            , " AS "
                            , dialect.getQuoteStart()
                            , rDBColumnMetaData.getAlias()
                            , dialect.getQuoteEnd());
                } else {
                    //关联的另外一张表
                    if (correlation.isOne2one()) {
                        appender.add(getDialect().buildColumnName(operationColumn.getTableName(), rDBColumnMetaData.getName())
                                , " AS "
                                , dialect.getQuoteStart()
                                , operationColumn.getTableName(), ".", rDBColumnMetaData.getAlias()
                                , dialect.getQuoteEnd());
                    }
                }
                appender.add(",");
            });
            appender.removeLast();

            appender.add(" FROM ", metaData.getName(), " ", metaData.getAlias());

            //生成join
            needSelectTable.stream()
                    .filter(table -> !table.equals(metaData.getName()) && metaData.getCorrelation(table) != null)
                    .map(table -> metaData.getCorrelation(table))
                    .sorted()
                    .forEach(correlation -> {
                        appender.addSpc("", correlation.getJoin(), correlation.getTargetTable(), correlation.getAlias(), "ON");
                        SqlAppender joinOn = new SqlAppender();
                        buildWhere(metaData.getDatabaseMetaData().getTableMetaData(correlation.getTargetTable()), "", correlation.getTerms(), joinOn, new HashSet());
                        if (!joinOn.isEmpty()) joinOn.removeFirst();
                        appender.addAll(joinOn);
                    });
            if (!whereSql.isEmpty())
                appender.add(" WHERE ", "").addAll(whereSql);

            if (!sorts.isEmpty()) {
                appender.add(" ORDER BY ");
                for (Sort sort : sorts) {
                    appender.add(sort.getName(), " ", sort.getOrder(), ",");
                }
                appender.removeLast();
            }
            if (param.isForUpdate()) {
                appender.add(" FOR UPDATE");
            }
            String sql = appender.toString();

            return new SimpleSQL(sql, param);
        }

        @Override
        public Dialect getDialect() {
            return dialect;
        }
    }

    @Override
    public SQL render(RDBTableMetaData metaData, QueryParam param) {
        return new SimpleSelectSqlRenderProcess(metaData, param).process();
    }
}
