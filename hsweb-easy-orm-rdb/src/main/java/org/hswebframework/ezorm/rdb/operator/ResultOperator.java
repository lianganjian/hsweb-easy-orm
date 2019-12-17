package org.hswebframework.ezorm.rdb.operator;

import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

public interface ResultOperator<E, R> {

    R sync();

    Publisher<E> reactive();

}
