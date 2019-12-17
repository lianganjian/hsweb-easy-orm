package org.hswebframework.ezorm.rdb.supports.mysql;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Statement;
import org.hswebframework.ezorm.rdb.TestReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.executor.reactive.ReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.dialect.Dialect;
import org.hswebframework.ezorm.rdb.supports.BasicReactiveTests;
import reactor.core.publisher.SignalType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class MysqlReactiveTests extends BasicReactiveTests {
    @Override
    protected RDBSchemaMetadata getSchema() {
        return new MysqlSchemaMetadata("ezorm");
    }

    @Override
    protected Dialect getDialect() {
        return Dialect.MYSQL;
    }

    @Override
    protected ReactiveSqlExecutor getSqlExecutor() {

        return new TestReactiveSqlExecutor(new MysqlR2dbcConnectionProvider()) {
            @Override
            protected void releaseConnection(SignalType type, Connection connection) {
                super.releaseConnection(type, connection);
            }

            @Override
            protected void bindNull(Statement statement, int index, Class type) {
                if (type == Date.class) {
                    type = LocalDateTime.class;
                }
                statement.bindNull(index, type);
            }

            @Override
            protected void bind(Statement statement, int index, Object value) {
                if (value instanceof Date) {
                    value = ((Date) value)
                            .toInstant()
                            .atZone(ZoneOffset.systemDefault())
                            .toLocalDateTime();
                }
                statement.bind(index, value);
            }

            @Override
            protected SqlRequest convertRequest(SqlRequest sqlRequest) {
                return sqlRequest;
            }
        };
    }

}
