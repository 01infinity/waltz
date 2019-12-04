import {attestationStatusColorScale} from "../common/colors";
import _ from "lodash";
import {toKeyCounts} from "../common";
import {keys} from "d3-collection";
import {mapToDisplayNames} from "../applications/application-utils";
import {mkDateGridCell, mkEntityLinkGridCell} from "../common/grid-utils";

const attestationStatus = {
    ATTESTED: {
        key: "ATTESTED",
        name: "Attested",
        description: "This flow has been attested",
        position: 10
    },
    NEVER_ATTESTED: {
        key: "NEVER_ATTESTED",
        name: "Never Attested",
        icon: null,
        description: "This flow has never been attested",
        position: 20
    }
};


export const attestationPieConfig = {
    colorProvider: (d) => attestationStatusColorScale(d.key),
    size: 40,
    labelProvider: (d) => attestationStatus[d.key] ? attestationStatus[d.key].name : "Unknown"
};

/**
 * Returns grid data with application, latest attestation, attested status
 * @param applications: all applications subjected for aggregation
 * @param attestationRuns: attestationRun for the applications
 * @param attestationInstances: all attestationInstances for the applications
 * @param attestedEntityKind: Entity Kind against which attestation data needs to be collected
 * **/
export function mkAppAttestationGridData(applications = [],
                                         attestationRuns = [],
                                         attestationInstances = [],
                                         attestedEntityKind,
                                         displayNameService) {
    const attestedRunIdsFilteredByEntityKind =
        _.chain(attestationRuns)
            .filter(d => d.attestedEntityKind === attestedEntityKind)
            .map(d => d.id)
            .value();

    const attestationByAppId = getAttestationByAppId(attestationInstances, attestedRunIdsFilteredByEntityKind);

    return _.map(applications, app => ({
        application: Object.assign({}, app, mapToDisplayNames(displayNameService, app)),
        isAttested: (_.includes(keys(attestationByAppId), String(app.id)) ? "ATTESTED" : "NEVER_ATTESTED"),
        attestation: _.maxBy(attestationByAppId[app.id], "attestedAt")
    }
    ));
}

export function prepareSummaryData(applications = []) {
    return toKeyCounts(applications, a => a.isAttested);
}


export const attestationSummaryColumnDefs = [
    mkEntityLinkGridCell("Name", "application"),
    { field: "application.assetCode", name: "Asset Code"},
    { field: "application.kindDisplay", name: "Kind"},
    { field: "businessCriticalityDisplay", name: "Business Criticality"},
    { field: "application.lifecyclePhaseDisplay", name: "Lifecycle Phase"},
    { field: "attestation.attestedBy", name: "Last Attested By"},
    mkDateGridCell("Last Attested at", "attestation.attestedAt")
];


function getAttestationByAppId(attestationInstances, attestedRunIds) {
    const attestedInstances = _.filter(attestationInstances, instance => isAttested(instance)
        && _.includes(attestedRunIds, instance.attestationRunId));

    return _.groupBy(attestedInstances, "parentEntity.id");

    function isAttested(instance) {
        return instance.attestedAt != null;
    }
}