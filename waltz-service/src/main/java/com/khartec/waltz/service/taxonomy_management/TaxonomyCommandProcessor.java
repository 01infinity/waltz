package com.khartec.waltz.service.taxonomy_management;

import com.khartec.waltz.common.Checks;
import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.taxonomy_management.TaxonomyChangeCommand;
import com.khartec.waltz.model.taxonomy_management.TaxonomyChangePreview;
import com.khartec.waltz.model.taxonomy_management.TaxonomyChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface TaxonomyCommandProcessor {

    Logger LOG = LoggerFactory.getLogger(TaxonomyCommandProcessor.class);

    default void checkType(TaxonomyChangeCommand cmd, TaxonomyChangeType expectedType) {
        Checks.checkTrue(
                cmd.changeType() == expectedType,
                "Incorrect type, expected [$s] got [%s]",
                expectedType,
                cmd.changeType());
    }


    default void checkDomain(TaxonomyChangeCommand cmd, EntityKind expectedDomain) {
        Checks.checkTrue(
                cmd.changeDomain().kind() == expectedDomain,
                "Incorrect domain, expected [$s] got [%s]",
                expectedDomain,
                cmd.changeDomain().kind());
    }


    default <T> boolean hasNoChange(T currentValue, T newValue, String fieldName) {
        if (currentValue.equals(newValue)) {
            LOG.info("Command will have no effect, '{}' is already '{}'", fieldName, newValue);
            return true;
        } else {
            return false;
        }
    }


    TaxonomyChangePreview preview(TaxonomyChangeCommand cmd);
    TaxonomyChangeCommand apply(TaxonomyChangeCommand command, String userId);

    TaxonomyChangeType type();
    EntityKind domain();


    default void doBasicValidation(TaxonomyChangeCommand cmd) {
        cmd.validate();
        checkDomain(cmd, domain());
        checkType(cmd, type());
    }

}
