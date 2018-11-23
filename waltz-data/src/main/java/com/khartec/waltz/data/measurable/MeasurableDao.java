/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017 Waltz open source project
 * See README.md for more information
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

package com.khartec.waltz.data.measurable;


import com.khartec.waltz.common.DateTimeUtilities;
import com.khartec.waltz.data.FindEntityReferencesByIdSelector;
import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.EntityReference;
import com.khartec.waltz.model.measurable.ImmutableMeasurable;
import com.khartec.waltz.model.measurable.Measurable;
import com.khartec.waltz.schema.tables.records.MeasurableRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.data.JooqUtilities.TO_ENTITY_REFERENCE;
import static com.khartec.waltz.schema.tables.EntityHierarchy.ENTITY_HIERARCHY;
import static com.khartec.waltz.schema.tables.Measurable.MEASURABLE;
import static com.khartec.waltz.schema.tables.MeasurableRating.MEASURABLE_RATING;
import static java.util.Optional.ofNullable;


@Repository
public class MeasurableDao implements FindEntityReferencesByIdSelector {

    public static RecordMapper<Record, Measurable> TO_DOMAIN_MAPPER = record -> {
        MeasurableRecord r = record.into(MEASURABLE);

        return ImmutableMeasurable.builder()
                .id(r.getId())
                .parentId(ofNullable(r.getParentId()))
                .name(r.getName())
                .categoryId(r.getMeasurableCategoryId())
                .concrete(r.getConcrete())
                .description(r.getDescription())
                .externalId(ofNullable(r.getExternalId()))
                .externalParentId(ofNullable(r.getExternalParentId()))
                .provenance(r.getProvenance())
                .lastUpdatedAt(DateTimeUtilities.toLocalDateTime(r.getLastUpdatedAt()))
                .lastUpdatedBy(r.getLastUpdatedBy())
                .build();
    };


    private final DSLContext dsl;


    @Autowired
    public MeasurableDao(DSLContext dsl) {
        checkNotNull(dsl, "dsl cannot be null");
        this.dsl = dsl;
    }


    public List<Measurable> findAll() {
        return dsl
                .selectFrom(MEASURABLE)
                .fetch(TO_DOMAIN_MAPPER);
    }


    public List<Measurable> findMeasuresRelatedToEntity(EntityReference ref) {
        checkNotNull(ref, "ref cannot be null");
        return dsl
                .selectDistinct(MEASURABLE.fields())
                .from(ENTITY_HIERARCHY)
                .innerJoin(MEASURABLE_RATING).on(MEASURABLE_RATING.MEASURABLE_ID.eq(ENTITY_HIERARCHY.ID))
                .innerJoin(MEASURABLE).on(MEASURABLE.ID.eq(ENTITY_HIERARCHY.ANCESTOR_ID))
                .where(MEASURABLE_RATING.ENTITY_KIND.eq(ref.kind().name()))
                .and(MEASURABLE_RATING.ENTITY_ID.eq(ref.id()))
                .and(ENTITY_HIERARCHY.KIND.eq(EntityKind.MEASURABLE.name()))
                .fetch(TO_DOMAIN_MAPPER);
    }


    @Override
    public List<EntityReference> findByIdSelectorAsEntityReference(Select<Record1<Long>> selector) {
        checkNotNull(selector, "selector cannot be null");
        return dsl
                .select(MEASURABLE.ID, MEASURABLE.NAME, DSL.val(EntityKind.MEASURABLE.name()))
                .from(MEASURABLE)
                .where(MEASURABLE.ID.in(selector))
                .fetch(TO_ENTITY_REFERENCE);
    }


    public List<Measurable> findByMeasurableIdSelector(Select<Record1<Long>> selector) {
        checkNotNull(selector, "selector cannot be null");
        return dsl
                .selectFrom(MEASURABLE)
                .where(MEASURABLE.ID.in(selector))
                .fetch(TO_DOMAIN_MAPPER);
    }


    public Measurable getById(long id) {
        return dsl
                .selectFrom(MEASURABLE)
                .where(MEASURABLE.ID.eq(id))
                .fetchOne(TO_DOMAIN_MAPPER);
    }


    public Collection<Measurable> findByExternalId(String extId) {
        return dsl
                .selectFrom(MEASURABLE)
                .where(MEASURABLE.EXTERNAL_ID.eq(extId))
                .fetch(TO_DOMAIN_MAPPER);
    }


    public boolean updateConcreteFlag(Long id, boolean newValue, String userId) {
        return updateField(id, MEASURABLE.CONCRETE, newValue, userId);
    }


    public boolean updateName(long id, String newValue, String userId) {
        return updateField(id, MEASURABLE.NAME, newValue, userId);
    }


    public boolean updateDescription(long id, String newValue, String userId) {
        return updateField(id, MEASURABLE.DESCRIPTION, newValue, userId);
    }


    public boolean updateExternalId(long id, String newValue, String userId) {
        return updateField(id, MEASURABLE.EXTERNAL_ID, newValue, userId);
    }


    private <T> boolean updateField(long id, Field<T> field, T value, String userId) {
        return dsl
                .update(MEASURABLE)
                .set(field, value)
                .set(MEASURABLE.LAST_UPDATED_AT, DateTimeUtilities.nowUtcTimestamp())
                .set(MEASURABLE.LAST_UPDATED_BY, userId)
                .where(MEASURABLE.ID.eq(id))
                .execute() == 1;
    }


    public boolean create(Measurable measurable) {
        int rc = dsl.insertInto(MEASURABLE)
                .set(MEASURABLE.MEASURABLE_CATEGORY_ID, measurable.categoryId())
                .set(MEASURABLE.PARENT_ID, measurable.parentId().orElse(null))
                .set(MEASURABLE.EXTERNAL_ID, measurable.externalId().orElse(null))
                .set(MEASURABLE.EXTERNAL_PARENT_ID, measurable.externalParentId().orElse(null))
                .set(MEASURABLE.NAME, measurable.name())
                .set(MEASURABLE.CONCRETE, measurable.concrete())
                .set(MEASURABLE.DESCRIPTION, measurable.description())
                .set(MEASURABLE.PROVENANCE, "waltz")
                .set(MEASURABLE.LAST_UPDATED_BY, measurable.lastUpdatedBy())
                .set(MEASURABLE.LAST_UPDATED_AT, Timestamp.valueOf(measurable.lastUpdatedAt()))
                .execute();

        return rc == 1;
    }
}
