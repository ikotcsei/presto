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
package io.prestosql.execution;

import io.prestosql.Session;
import io.prestosql.testing.MaterializedResult;
import io.prestosql.testing.QueryRunner;
import org.intellij.lang.annotations.Language;

import java.util.Optional;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.testng.Assert.fail;

class EventsAwaitingQueries
{
    private final EventsCollector eventsCollector;

    private final QueryRunner queryRunner;

    EventsAwaitingQueries(EventsCollector eventsCollector, QueryRunner queryRunner)
    {
        this.eventsCollector = requireNonNull(eventsCollector, "eventsBuilder is null");
        this.queryRunner = requireNonNull(queryRunner, "queryRunner is null");
    }

    MaterializedResult runQueryAndWaitForEvents(@Language("SQL") String sql, int numEventsExpected, Session session)
            throws Exception
    {
        return runQueryAndWaitForEvents(sql, numEventsExpected, session, Optional.empty());
    }

    MaterializedResult runQueryAndWaitForEvents(@Language("SQL") String sql, int numEventsExpected, Session session, Optional<String> expectedExceptionRegEx)
            throws Exception
    {
        eventsCollector.reset(numEventsExpected);
        MaterializedResult result = null;
        try {
            result = queryRunner.execute(session, sql);
        }
        catch (RuntimeException exception) {
            if (expectedExceptionRegEx.isPresent()) {
                String regex = expectedExceptionRegEx.get();
                if (!nullToEmpty(exception.getMessage()).matches(regex)) {
                    fail(format("Expected exception message '%s' to match '%s' for query: %s", exception.getMessage(), regex, sql), exception);
                }
            }
            else {
                throw exception;
            }
        }

        eventsCollector.waitForEvents(10);

        return result;
    }
}
