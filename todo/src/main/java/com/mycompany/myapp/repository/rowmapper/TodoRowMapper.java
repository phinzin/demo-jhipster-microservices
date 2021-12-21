package com.mycompany.myapp.repository.rowmapper;

import com.mycompany.myapp.domain.Todo;
import com.mycompany.myapp.service.ColumnConverter;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Todo}, with proper type conversions.
 */
@Service
public class TodoRowMapper implements BiFunction<Row, String, Todo> {

    private final ColumnConverter converter;

    public TodoRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Todo} stored in the database.
     */
    @Override
    public Todo apply(Row row, String prefix) {
        Todo entity = new Todo();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setTask(converter.fromRow(row, prefix + "_task", String.class));
        entity.setDescription(converter.fromRow(row, prefix + "_description", String.class));
        entity.setCompleted(converter.fromRow(row, prefix + "_completed", Boolean.class));
        entity.setCategoryId(converter.fromRow(row, prefix + "_category_id", Long.class));
        return entity;
    }
}
