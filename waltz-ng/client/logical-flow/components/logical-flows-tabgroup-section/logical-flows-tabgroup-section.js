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

import _ from 'lodash';
import {mkSelectionOptions} from "../../../common/selector-utils";
import {determineStatMethod} from "../../logical-flow-utils";


const bindings = {
    parentEntityRef: '<'
};


const initialState = {
    export: () => console.log('lfts: default do-nothing export function'),
    visibility: {
        exportButton: false,
        sourcesOverlay: false
    }
};


function calcHasFlows(stats) {
    const counts = _.get(stats, 'flowCounts', {});
    const total = _.sum(_.values(counts));
    return total > 0;
}


function controller(serviceBroker) {
    const vm = _.defaultsDeep(this, initialState);

    const load = (selector) => {
        vm.loadingStats = true;

        serviceBroker
            .loadViewData(
                determineStatMethod(vm.parentEntityRef.kind),
                [ selector ])
            .then(r => {
                vm.loadingStats = false;
                vm.stats = r.data;
                vm.hasFlows = calcHasFlows(vm.stats);
            });
    };

    vm.$onInit = () => {
    };

    vm.$onChanges = (c) => {
        if (vm.parentEntityRef) {
            vm.selector = mkSelectionOptions(vm.parentEntityRef);
            load(vm.selector);
        }
    };

}


controller.$inject = [
    "ServiceBroker"
];


const component = {
    controller,
    bindings,
    template: require('./logical-flows-tabgroup-section.html')
};


export default component;