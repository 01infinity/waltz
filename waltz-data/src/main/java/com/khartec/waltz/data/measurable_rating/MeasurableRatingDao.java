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

package com.khartec.waltz.data.measurable_rating;

import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.EntityReference;
import com.khartec.waltz.model.measurable_rating.ImmutableMeasurableRating;
import com.khartec.waltz.model.measurable_rating.MeasurableRating;
import com.khartec.waltz.model.measurable_rating.RemoveMeasurableRatingCommand;
import com.khartec.waltz.model.measurable_rating.SaveMeasurableRatingCommand;
import com.khartec.waltz.model.tally.ImmutableMeasurableRatingTally;
import com.khartec.waltz.model.tally.MeasurableRatingTally;
import com.khartec.waltz.model.tally.Tally;
import com.khartec.waltz.schema.tables.records.MeasurableRatingRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.common.DateTimeUtilities.toLocalDateTime;
import static com.khartec.waltz.common.StringUtilities.firstChar;
import static com.khartec.waltz.data.JooqUtilities.calculateLongTallies;
import static com.khartec.waltz.schema.tables.Measurable.MEASURABLE;
import static com.khartec.waltz.schema.tables.MeasurableRating.MEASURABLE_RATING;

@Repository
public class MeasurableRatingDao {

    private static final RecordMapper<? super Record, MeasurableRating> TO_DOMAIN_MAPPER = record -> {
        MeasurableRatingRecord r = record.into(MEASURABLE_RATING);
        return ImmutableMeasurableRating.builder()
                .entityReference(EntityReference.mkRef(
                        EntityKind.valueOf(r.getEntityKind()),
                        r.getEntityId()))
                .description(r.getDescription())
                .provenance(r.getProvenance())
                .rating(firstChar(r.getRating(), 'Z'))
                .measurableId(r.getMeasurableId())
                .lastUpdatedAt(toLocalDateTime(r.getLastUpdatedAt()))
                .lastUpdatedBy(r.getLastUpdatedBy())
                .build();
    };


    private static final RecordMapper<Record3<Long,String,Integer>, MeasurableRatingTally> TO_TALLY_MAPPER = record -> {
        Long measurableId = record.value1();
        Integer count = record.value3();

        return ImmutableMeasurableRatingTally.builder()
                .id(measurableId)
                .rating(firstChar(record.value2(), 'Z'))
                .count(count)
                .build();
    };

    private final Function<SaveMeasurableRatingCommand, MeasurableRatingRecord> TO_RECORD_MAPPER = command -> {
        MeasurableRatingRecord record = new MeasurableRatingRecord();
        record.setEntityId(command.entityReference().id());
        record.setEntityKind(command.entityReference().kind().name());
        record.setMeasurableId(command.measurableId());
        record.setRating(Character.toString(command.rating()));
        record.setDescription(command.description());
        record.setLastUpdatedAt(Timestamp.valueOf(command.lastUpdate().at()));
        record.setLastUpdatedBy(command.lastUpdate().by());
        record.setProvenance(command.provenance());
        return record;
    };


    private final DSLContext dsl;


    @Autowired
    public MeasurableRatingDao(DSLContext dsl) {
        checkNotNull(dsl, "dsl cannot be null");
        this.dsl = dsl;
    }


    public List<MeasurableRating> findForEntity(EntityReference ref) {
        checkNotNull(ref, "ref cannot be null");
        return dsl
                .selectFrom(MEASURABLE_RATING)
                .where(MEASURABLE_RATING.ENTITY_KIND.eq(ref.kind().name()))
                .and(MEASURABLE_RATING.ENTITY_ID.eq(ref.id()))
                .fetch(TO_DOMAIN_MAPPER);
    }


    public List<MeasurableRating> findByMeasurableIdSelector(Select<Record1<Long>> selector) {
        checkNotNull(selector, "selector cannot be null");
        return dsl
                .selectFrom(MEASURABLE_RATING)
                .where(MEASURABLE_RATING.MEASURABLE_ID.in(selector))
                .fetch(TO_DOMAIN_MAPPER);
    }


    public Collection<MeasurableRating> findByApplicationIdSelector(Select<Record1<Long>> selector) {
        checkNotNull(selector, "selector cannot be null");
        Condition condition = MEASURABLE_RATING.ENTITY_ID.in(selector)
                .and(MEASURABLE_RATING.ENTITY_KIND.eq(DSL.val(EntityKind.APPLICATION.name())));
        return dsl
                .selectFrom(MEASURABLE_RATING)
                .where(condition)
                .fetch(TO_DOMAIN_MAPPER);
    }

    public Collection<MeasurableRating> findByCategory(long id) {
        return dsl
                .select(MEASURABLE_RATING.fields())
                .from(MEASURABLE_RATING)
                .innerJoin(MEASURABLE).on(MEASURABLE_RATING.MEASURABLE_ID.eq(MEASURABLE.ID))
                .where(MEASURABLE.MEASURABLE_CATEGORY_ID.eq(id))
                .fetch(TO_DOMAIN_MAPPER);
    }

    public boolean create(SaveMeasurableRatingCommand command) {
        MeasurableRatingRecord record = TO_RECORD_MAPPER.apply(command);
        return dsl.executeInsert(record) == 1;
    }


    public boolean update(SaveMeasurableRatingCommand command) {
        MeasurableRatingRecord record = TO_RECORD_MAPPER.apply(command);
        return dsl.executeUpdate(record) == 1;
    }


    public boolean remove(RemoveMeasurableRatingCommand command) {
        EntityReference ref = command.entityReference();
        return dsl.deleteFrom(MEASURABLE_RATING)
                .where(MEASURABLE_RATING.ENTITY_KIND.eq(ref.kind().name()))
                .and(MEASURABLE_RATING.ENTITY_ID.eq(ref.id()))
                .and(MEASURABLE_RATING.MEASURABLE_ID.eq(command.measurableId()))
                .execute() == 1;
    }


    public List<Tally<Long>> tallyByMeasurableId() {
        return calculateLongTallies(
                dsl,
                MEASURABLE_RATING,
                MEASURABLE_RATING.MEASURABLE_ID,
                DSL.trueCondition());
    }


    public List<MeasurableRatingTally> statsByAppSelector(Select<Record1<Long>> selector) {
        return dsl.select(MEASURABLE_RATING.MEASURABLE_ID, MEASURABLE_RATING.RATING, DSL.count())
                .from(MEASURABLE_RATING)
                .where(MEASURABLE_RATING.ENTITY_KIND.eq(EntityKind.APPLICATION.name()))
                .and(MEASURABLE_RATING.ENTITY_ID.in(selector))
                .groupBy(MEASURABLE_RATING.MEASURABLE_ID, MEASURABLE_RATING.RATING)
                .fetch(TO_TALLY_MAPPER);
    }


    public List<MeasurableRatingTally> statsForRelatedMeasurable(long measurableId) {
        com.khartec.waltz.schema.tables.MeasurableRating related = MEASURABLE_RATING.as("related");
        com.khartec.waltz.schema.tables.MeasurableRating orig = MEASURABLE_RATING.as("orig");

        SelectConditionStep<Record1<Long>> relatedAppIds = DSL
                .selectDistinct(orig.ENTITY_ID)
                .from(orig)
                .where(orig.MEASURABLE_ID.eq(measurableId))
                .and(orig.ENTITY_KIND.eq(EntityKind.APPLICATION.name()));

        return dsl
                .select(related.MEASURABLE_ID,
                        related.RATING,
                        DSL.count())
                .from(related)
                .where(related.ENTITY_ID.in(relatedAppIds))
                .groupBy(related.MEASURABLE_ID,
                        related.RATING)
                .fetch(TO_TALLY_MAPPER);
    }


}
