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

import {initialiseData, invokeFunction} from "../../../common";


const bindings = {
    defaultActiveTabIndex: '<',
    specDefinition: '<',
    selectableDefinitions: '<',
    onDefinitionSelect: '<'
};


const initialState = {
    defaultActiveTabIndex: 1,
    specDefinition: {},
    selectableDefinitions: [],
    onDefinitionSelect: (def) => console.log('psdp::onDefinitionSelect', def)
};


const template = require('./physical-spec-definition-panel.html');


function controller() {
    const vm = initialiseData(this, initialState);

    vm.definitionSelected =
        (def) => invokeFunction(vm.onDefinitionSelect, def);
}


const component = {
    controller,
    template,
    bindings
};


export default component;