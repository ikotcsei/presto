/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.tests.iceberg;

import io.prestosql.tempto.AfterTestWithContext;
import io.prestosql.tempto.BeforeTestWithContext;
import io.prestosql.tempto.ProductTest;
import io.prestosql.testng.services.Flaky;
import org.testng.annotations.Test;

import static io.prestosql.tempto.assertions.QueryAssert.Row.row;
import static io.prestosql.tempto.assertions.QueryAssert.assertThat;
import static io.prestosql.tests.TestGroups.ICEBERG;
import static io.prestosql.tests.TestGroups.STORAGE_FORMATS;
import static io.prestosql.tests.hive.util.TemporaryHiveTable.randomTableSuffix;
import static io.prestosql.tests.utils.QueryExecutors.onPresto;

public class TestIcebergCreateTable
        extends ProductTest
{
    @BeforeTestWithContext
    public void setUp()
    {
        onPresto().executeQuery("CREATE SCHEMA iceberg.iceberg");
    }

    @AfterTestWithContext
    public void cleanUp()
    {
        onPresto().executeQuery("DROP SCHEMA iceberg.iceberg");
    }

    @Test(groups = {ICEBERG, STORAGE_FORMATS})
    @Flaky(issue = "https://github.com/prestosql/presto/issues/4864", match = "Failed to read footer of file")
    public void testCreateTable()
    {
        String tableName = "iceberg.iceberg.test_create_table_" + randomTableSuffix();
        onPresto().executeQuery("CREATE TABLE " + tableName + "(a bigint, b varchar)");
        onPresto().executeQuery("INSERT INTO " + tableName + "(a, b) VALUES " +
                "(NULL, NULL), " +
                "(-42, 'abc'), " +
                "(9223372036854775807, 'abcdefghijklmnopqrstuvwxyz')");
        assertThat(onPresto().executeQuery("SELECT * FROM " + tableName))
                .containsOnly(
                        row(null, null),
                        row(-42, "abc"),
                        row(9223372036854775807L, "abcdefghijklmnopqrstuvwxyz"));
        onPresto().executeQuery("DROP TABLE " + tableName);
    }

    @Test(groups = {ICEBERG, STORAGE_FORMATS})
    @Flaky(issue = "https://github.com/prestosql/presto/issues/4864", match = "Failed to read footer of file")
    public void testCreateTableAsSelect()
    {
        String tableName = "iceberg.iceberg.test_create_table_as_select_" + randomTableSuffix();
        onPresto().executeQuery("" +
                "CREATE TABLE " + tableName + " AS " +
                "SELECT * FROM (VALUES " +
                "  (NULL, NULL), " +
                "  (-42, 'abc'), " +
                "  (9223372036854775807, 'abcdefghijklmnopqrstuvwxyz')" +
                ") t(a, b)");
        assertThat(onPresto().executeQuery("SELECT * FROM " + tableName))
                .containsOnly(
                        row(null, null),
                        row(-42, "abc"),
                        row(9223372036854775807L, "abcdefghijklmnopqrstuvwxyz"));
        onPresto().executeQuery("DROP TABLE " + tableName);
    }
}
