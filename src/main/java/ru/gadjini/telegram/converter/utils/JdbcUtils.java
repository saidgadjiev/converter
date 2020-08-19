package ru.gadjini.telegram.converter.utils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class JdbcUtils {

    private JdbcUtils() {

    }

    public static Set<String> getColumnNames(ResultSetMetaData metaData) throws SQLException {
        Set<String> result = new HashSet<>();

        for (int i = 1; i < metaData.getColumnCount() + 1; ++i) {
            String name = metaData.getColumnName(i);

            result.add(name);
        }

        return result;
    }
}
