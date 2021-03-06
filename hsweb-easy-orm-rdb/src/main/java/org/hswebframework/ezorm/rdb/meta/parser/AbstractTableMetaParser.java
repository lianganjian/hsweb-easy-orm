package org.hswebframework.ezorm.rdb.meta.parser;

import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.core.ObjectWrapper;
import org.hswebframework.ezorm.rdb.executor.SqlExecutor;
import org.hswebframework.ezorm.rdb.meta.RDBColumnMetaData;
import org.hswebframework.ezorm.rdb.meta.RDBTableMetaData;
import org.hswebframework.ezorm.rdb.meta.expand.SimpleMapWrapper;
import org.hswebframework.ezorm.rdb.render.dialect.Dialect;
import org.hswebframework.ezorm.rdb.render.support.simple.SimpleSQL;
import org.hswebframework.utils.StringUtils;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractTableMetaParser implements TableMetaParser {
    protected static Map<JDBCType, Class> defaultJavaTypeMap = new HashMap<>();

    static {
        defaultJavaTypeMap.put(JDBCType.VARCHAR, String.class);
        defaultJavaTypeMap.put(JDBCType.CLOB, String.class);
        defaultJavaTypeMap.put(JDBCType.BLOB, byte[].class);
        defaultJavaTypeMap.put(JDBCType.DATE, Date.class);
        defaultJavaTypeMap.put(JDBCType.TIME, Date.class);
        defaultJavaTypeMap.put(JDBCType.TIMESTAMP, Date.class);
        defaultJavaTypeMap.put(JDBCType.BIT, Byte.class);
        defaultJavaTypeMap.put(JDBCType.BIGINT, Long.class);
        defaultJavaTypeMap.put(JDBCType.NUMERIC, Double.class);
        defaultJavaTypeMap.put(JDBCType.INTEGER, Integer.class);
        defaultJavaTypeMap.put(JDBCType.DOUBLE, Double.class);
        defaultJavaTypeMap.put(JDBCType.FLOAT, Float.class);
        defaultJavaTypeMap.put(JDBCType.CHAR, String.class);
        defaultJavaTypeMap.put(JDBCType.TINYINT, Byte.class);
    }

    protected Map<JDBCType, Class> javaTypeMap = new HashMap<>();

    @Setter
    protected String databaseName;

    public String getDatabaseName() {
        return databaseName;
    }

    protected SqlExecutor sqlExecutor;

    abstract Dialect getDialect();

    public AbstractTableMetaParser(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    abstract String getTableMetaSql(String tname);

    abstract String getTableCommentSql(String tname);

    abstract String getAllTableSql();

    abstract String getTableExistsSql();

    @Override
    public boolean tableExists(String name) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("table", name);
            Map<String, Object> res = sqlExecutor.single(new SimpleSQL(getTableExistsSql(), param), new LowerCasePropertySimpleMapWrapper());
            return res.get("total") != null && StringUtils.toInt(res.get("total")) > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SneakyThrows
    public RDBTableMetaData parse(String name) {
        if (!tableExists(name)) return null;
        RDBTableMetaData metaData = new RDBTableMetaData();
        metaData.setName(name);
        metaData.setAlias(name);
        Map<String, Object> param = new HashMap<>();
        param.put("table", name);
        List<RDBColumnMetaData> metaDatas = sqlExecutor.list(new SimpleSQL(getTableMetaSql(name), param), new RDBColumnMetaDataWrapper());
        metaDatas.forEach(metaData::addColumn);
        Map<String, Object> comment = sqlExecutor.single(new SimpleSQL(getTableCommentSql(name), param), new LowerCasePropertySimpleMapWrapper());
        if (null != comment && comment.get("comment") != null) {
            metaData.setComment(String.valueOf(comment.get("comment")));
        }
        return metaData;
    }

    @Override
    public List<RDBTableMetaData> parseAll() throws SQLException {
        List<Map<String, Object>> tables = sqlExecutor.list(new SimpleSQL(getAllTableSql()), new LowerCasePropertySimpleMapWrapper());
        return tables.stream()
                .map(map -> (String) map.get("name"))
                .filter(Objects::nonNull)
                .map(this::parse).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    class LowerCasePropertySimpleMapWrapper extends SimpleMapWrapper {
        @Override
        public void wrapper(Map<String, Object> instance, int index, String attr, Object value) {
            attr = attr.toLowerCase();
            super.wrapper(instance, index, attr, value);
        }
    }

    class RDBColumnMetaDataWrapper implements ObjectWrapper<RDBColumnMetaData> {
        @Override
        public Class<RDBColumnMetaData> getType() {
            return RDBColumnMetaData.class;
        }

        @Override
        public RDBColumnMetaData newInstance() {
            return new RDBColumnMetaData();
        }

        @Override
        public void wrapper(RDBColumnMetaData instance, int index, String attr, Object value) {
            String stringValue;
            if (value instanceof String) {
                stringValue = ((String) value).toLowerCase();
            } else {
                stringValue = value == null ? "" : value.toString();
            }
            if (attr.equalsIgnoreCase("name")) {
                instance.setName(stringValue);
                instance.setProperty("old-name", stringValue);
            } else if (attr.equalsIgnoreCase("comment")) {
                instance.setComment(stringValue);
            } else {
                if (attr.toLowerCase().equals("not-null")) {
                    value = "1".equals(stringValue);
                    instance.setNotNull((boolean) value);
                }
                instance.setProperty(attr.toLowerCase(), value);
            }
        }

        @Override
        public boolean done(RDBColumnMetaData instance) {
            String data_type = instance.getProperty("data_type").toString().toLowerCase();
            int len = instance.getProperty("data_length").toInt();
            int data_precision = instance.getProperty("data_precision").toInt();
            int data_scale = instance.getProperty("data_scale").toInt();
            instance.setLength(len);
            instance.setPrecision(data_precision);
            instance.setScale(data_scale);

            JDBCType jdbcType = getDialect().getJdbcType(data_type);
            Class javaType = Optional.ofNullable(javaTypeMap.get(jdbcType))
                    .orElseGet(() -> defaultJavaTypeMap.getOrDefault(jdbcType, String.class));

            instance.setJdbcType(jdbcType);
            instance.setJavaType(javaType);
            instance.setDataType(getDialect().buildDataType(instance));
            return true;
        }
    }
}
