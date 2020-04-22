package org.hswebframework.ezorm.rdb.supports.postgres;

import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.rdb.R2dbcConnectionProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URL;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

public class Postgresql12R2dbcConnectionProvider implements R2dbcConnectionProvider {


    Supplier<Mono<Connection>> connectionSupplier;

    @SneakyThrows
    public Postgresql12R2dbcConnectionProvider() {

        String username = System.getProperty("postgres.username", "postgres");
        String password = System.getProperty("postgres.password", "admin");
        String url = System.getProperty("postgres.url", "127.0.0.1:15433");
        String db = System.getProperty("postgres.db", "ezorm");

        URL hostUrl = new URL("file://" + url);

        PostgresqlConnectionFactory connectionFactory = (PostgresqlConnectionFactory) ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, hostUrl.getHost())  // file, mem
                .option(PORT, hostUrl.getPort())  // file, mem
                .option(USER, username)
                .option(PASSWORD, password)
                .option(DATABASE, db)
                .build());
        connectionSupplier = () -> connectionFactory.create().map(Connection.class::cast);
    }

    @Override
    public Mono<Connection> getConnection() {
        return connectionSupplier.get();
    }

    @Override
    public void releaseConnection(Connection connection) {
        connection.close();
    }
}
