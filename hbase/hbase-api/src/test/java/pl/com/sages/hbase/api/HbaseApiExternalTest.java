package pl.com.sages.hbase.api;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pl.com.sages.hbase.api.util.HBaseUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.com.sages.hbase.api.util.HbaseConfigurationFactory.getConfiguration;

public class HbaseApiExternalTest {

    private static final TableName TEST_TABLE_NAME = HBaseUtil.getUserTableName("test_users_" + System.currentTimeMillis());
    private static final String FAMILY_NAME = "info";

    private Connection connection;
    private Admin admin;

    @Before
    public void createTestTable() throws Exception {
        Configuration configuration = getConfiguration();
        connection = ConnectionFactory.createConnection(configuration);
        admin = connection.getAdmin();

        HTableDescriptor table = new HTableDescriptor(TEST_TABLE_NAME);

        HColumnDescriptor columnFamily = new HColumnDescriptor(FAMILY_NAME);
        columnFamily.setMaxVersions(10);
        table.addFamily(columnFamily);

        admin.createTable(table);
    }

    @After
    public void deleteTable() throws Exception {
        if (admin != null) {
            admin.disableTable(TEST_TABLE_NAME);
            admin.deleteTable(TEST_TABLE_NAME);
            admin.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void shouldPutAndGetDataFromHbase() throws Exception {
        //given
        Table table = connection.getTable(TEST_TABLE_NAME);

        String id = "id";
        String qualifier = "cell";
        String value = "nasza testowa wartość";

        Put put = new Put(Bytes.toBytes(id));
        put.addColumn(Bytes.toBytes(FAMILY_NAME),
                Bytes.toBytes(qualifier),
                Bytes.toBytes(value));

        table.put(put);

        //when
        Get get = new Get(Bytes.toBytes(id));
        get.setMaxVersions(10);
        Result result = table.get(get);

        //then
        assertThat(value).isEqualToIgnoringCase(Bytes.toString(result.getValue(Bytes.toBytes(FAMILY_NAME), Bytes.toBytes(qualifier))));
    }

    @Test
    public void shouldPutAndGetDataFromHbaseWithVersions() throws Exception {
        //given
        Table table = connection.getTable(TEST_TABLE_NAME);

        String id = "id";
        String qualifier = "cell";
        String value1 = "nasza testowa wartość";
        String value2 = "nasza testowa wartość 2";

        Put put = new Put(Bytes.toBytes(id));
        put.addColumn(Bytes.toBytes(FAMILY_NAME),
                Bytes.toBytes(qualifier),
                Bytes.toBytes(value1));
        table.put(put);

        put = new Put(Bytes.toBytes(id));
        put.addColumn(Bytes.toBytes(FAMILY_NAME),
                Bytes.toBytes(qualifier),
                Bytes.toBytes(value2));
        table.put(put);

        //when
        Get get = new Get(Bytes.toBytes(id));
        get.setMaxVersions(10);
        Result result = table.get(get);

        //then
        assertThat(value2).isEqualToIgnoringCase(Bytes.toString(result.getValue(Bytes.toBytes(FAMILY_NAME), Bytes.toBytes(qualifier))));

        List<Cell> columnCells = result.getColumnCells(Bytes.toBytes(FAMILY_NAME), Bytes.toBytes(qualifier));
        assertThat(value2).isEqualToIgnoringCase(Bytes.toString(CellUtil.cloneValue(columnCells.get(0))));
        assertThat(value1).isEqualToIgnoringCase(Bytes.toString(CellUtil.cloneValue(columnCells.get(1))));
    }

    @Test
    public void shouldDeleteDataFromHbase() throws Exception {
        //given
        Table users = connection.getTable(TEST_TABLE_NAME);

        String id = "id";
        String column = "cell";
        String value1 = "nasza testowa wartość";
        String value2 = "nasza testowa wartość 2";

        Put put = new Put(Bytes.toBytes(id));
        put.addColumn(Bytes.toBytes(FAMILY_NAME),
                Bytes.toBytes(column),
                Bytes.toBytes(value1));
        users.put(put);

        put = new Put(Bytes.toBytes(id));
        put.addColumn(Bytes.toBytes(FAMILY_NAME),
                Bytes.toBytes(column),
                Bytes.toBytes(value2));
        users.put(put);

        //when
        Delete delete = new Delete(Bytes.toBytes(id));
        delete.addColumn(Bytes.toBytes(FAMILY_NAME), Bytes.toBytes(column));
        users.delete(delete);

        //then
        Get get = new Get(Bytes.toBytes(id));
        get.setMaxVersions(10);
        Result result = users.get(get);

        assertThat(value1).isEqualToIgnoringCase(Bytes.toString(result.getValue(Bytes.toBytes(FAMILY_NAME), Bytes.toBytes(column))));
    }

}
