package org.hswebframework.ezorm.rdb.supports.postgres;

import org.hswebframework.ezorm.TestSyncSqlExecutor;
import org.hswebframework.ezorm.rdb.TestReactiveSqlExecutor;
import org.hswebframework.ezorm.rdb.executor.SyncSqlExecutor;
import org.hswebframework.ezorm.rdb.metadata.RDBDatabaseMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBSchemaMetadata;
import org.hswebframework.ezorm.rdb.metadata.dialect.Dialect;
import org.hswebframework.ezorm.rdb.supports.BasicCommonTests;
import org.hswebframework.ezorm.rdb.supports.mssql.MSSQLR2dbcConnectionProvider;
import org.hswebframework.ezorm.rdb.supports.posgres.PostgreSQLSchemaMetadata;

public class PostgreSQLBasicTest extends BasicCommonTests {
    @Override
    protected RDBSchemaMetadata getSchema() {
        return new PostgreSQLSchemaMetadata("public");
    }

    @Override
    protected RDBDatabaseMetadata getDatabase() {
        RDBDatabaseMetadata database= super.getDatabase();

        database.addFeature(new TestReactiveSqlExecutor(new PostgreSQLR2dbcConnectionProvider()));

        return database;
    }

    @Override
    protected Dialect getDialect() {
        return Dialect.POSTGRES;
    }

    @Override
    protected SyncSqlExecutor getSqlExecutor() {
        return new TestSyncSqlExecutor(new PostgreSQLConnectionProvider());
    }
}