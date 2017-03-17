/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016  Khartec Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.data.measurable.search;

import com.khartec.waltz.data.DatabaseVendorSpecific;
import com.khartec.waltz.data.FullTextSearch;
import com.khartec.waltz.data.JooqUtilities;
import com.khartec.waltz.data.measurable.MeasurableDao;
import com.khartec.waltz.model.entity_search.EntitySearchOptions;
import com.khartec.waltz.model.measurable.Measurable;
import org.jooq.DSLContext;

import java.util.List;

import static com.khartec.waltz.common.StringUtilities.mkTerms;
import static com.khartec.waltz.schema.tables.Measurable.MEASURABLE;
import static java.util.Collections.emptyList;

public class SqlServerMeasurableSearch implements FullTextSearch<Measurable>, DatabaseVendorSpecific {

    @Override
    public List<Measurable> search(DSLContext dsl, String query, EntitySearchOptions options) {
        List<String> terms = mkTerms(query);

        if (terms.isEmpty()) {
            return emptyList();
        }

        return dsl
                .selectFrom(MEASURABLE)
                .where(JooqUtilities.MSSQL.mkContains(terms))
                .limit(options.limit())
                .fetch(MeasurableDao.TO_DOMAIN_MAPPER);
    }

}
