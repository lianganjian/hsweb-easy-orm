package org.hswebframework.ezorm.rdb.mapping.defaults;

import org.hswebframework.ezorm.core.CastUtil;
import org.hswebframework.ezorm.rdb.executor.wrapper.ResultWrapper;
import org.hswebframework.ezorm.rdb.mapping.SyncDelete;
import org.hswebframework.ezorm.rdb.mapping.SyncQuery;
import org.hswebframework.ezorm.rdb.mapping.SyncRepository;
import org.hswebframework.ezorm.rdb.mapping.SyncUpdate;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;

import java.util.*;
import java.util.function.Supplier;

public class DefaultSyncRepository<E, K> extends DefaultRepository<E> implements SyncRepository<E, K> {


    public DefaultSyncRepository(DatabaseOperator operator, String table, Class<E> type,ResultWrapper<E, ?> wrapper) {
        this(operator,
                ()->operator.getMetadata().getTable(table).orElseThrow(()->new UnsupportedOperationException("table [" + table + "] doesn't exist")),type,wrapper);
    }


    public DefaultSyncRepository(DatabaseOperator operator, RDBTableMetadata table, Class<E> type, ResultWrapper<E, ?> wrapper) {
        this(operator, () -> table, type, wrapper);
    }

    public DefaultSyncRepository(DatabaseOperator operator, Supplier<RDBTableMetadata> table, Class<E> type, ResultWrapper<E, ?> wrapper) {
        super(operator, table, wrapper);
        initMapping(type);
    }

    @Override
    public E newInstance() {
        return wrapper.newRowInstance();
    }

    @Override
    public int deleteById(Collection<K> idList) {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        return createDelete()
                .where().in(getIdColumn(), idList)
                .execute();
    }

    @Override
    public SaveResult save(Collection<E> list) {
        if (list == null || list.isEmpty()) {
            return SaveResult.of(0, 0);
        }
        List<E> readyToInsert = new ArrayList<>();
        int updated = 0;
        for (E datum : list) {
            K id = getPropertyOperator()
                    .getProperty(datum, getIdColumn())
                    .map(CastUtil::<K>cast)
                    .orElse(null);
            if (id == null) {
                readyToInsert.add(datum);
            } else {
                int thisUpdated = createUpdate().set(datum).where(getIdColumn(), id).execute();
                if (thisUpdated == 0) {
                    readyToInsert.add(datum);
                } else {
                    updated += thisUpdated;
                }
            }
        }
        int added = insertBatch(readyToInsert);

        return SaveResult.of(added, updated);
    }

    @Override
    public Optional<E> findById(K primaryKey) {
        return Optional.ofNullable(primaryKey)
                .flatMap(k -> createQuery().where(getIdColumn(), k).fetchOne());
    }

    @Override
    public List<E> findById(Collection<K> primaryKey) {
        if (primaryKey.isEmpty()) {
            return new ArrayList<>();
        }
        return createQuery().where().in(getIdColumn(), primaryKey).fetch();
    }

    @Override
    public void insert(E data) {
        doInsert(data).sync();
    }

    @Override
    public int insertBatch(Collection<E> batch) {
        if (batch.size() == 0) {
            return 0;
        }
        return doInsert(batch).sync();
    }

    @Override
    public SyncQuery<E> createQuery() {
        return new DefaultSyncQuery<>(getTable(), mapping, operator.dml(), wrapper);
    }

    @Override
    public SyncUpdate<E> createUpdate() {
        return new DefaultSyncUpdate<>(getTable(), operator.dml().update(getTable().getFullName()), mapping);
    }

    @Override
    public SyncDelete createDelete() {
        return new DefaultSyncDelete(getTable(), operator.dml().delete(getTable().getFullName()));
    }
}