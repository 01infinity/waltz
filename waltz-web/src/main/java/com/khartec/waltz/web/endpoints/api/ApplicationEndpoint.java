/*
 *  This file is part of Waltz.
 *
 *     Waltz is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Waltz is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Waltz.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.web.endpoints.api;

import com.khartec.waltz.common.ListUtilities;
import com.khartec.waltz.model.*;
import com.khartec.waltz.model.application.AppRegistrationRequest;
import com.khartec.waltz.model.application.AppRegistrationResponse;
import com.khartec.waltz.model.application.Application;
import com.khartec.waltz.model.application.AssetCodeRelationshipKind;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.tally.Tally;
import com.khartec.waltz.service.application.ApplicationService;
import com.khartec.waltz.service.changelog.ChangeLogService;
import com.khartec.waltz.service.entity_alias.EntityAliasService;
import com.khartec.waltz.service.tags.AppTagService;
import com.khartec.waltz.web.DatumRoute;
import com.khartec.waltz.web.ListRoute;
import com.khartec.waltz.web.WebUtilities;
import com.khartec.waltz.web.action.AppChangeAction;
import com.khartec.waltz.web.endpoints.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.web.WebUtilities.*;
import static com.khartec.waltz.web.endpoints.EndpointUtilities.*;
import static java.lang.Long.parseLong;


@Service
public class ApplicationEndpoint implements Endpoint {


    private static final Logger LOG = LoggerFactory.getLogger(ApplicationEndpoint.class);

    private static final String BASE_URL = mkPath("api", "app");

    private final ApplicationService appService;
    private final ChangeLogService changeLogService;
    private final EntityAliasService entityAliasService;
    private final AppTagService appTagService;


    @Autowired
    public ApplicationEndpoint(ApplicationService appService,
                               ChangeLogService changeLogService,
                               EntityAliasService entityAliasService, 
                               AppTagService appTagService) {
        checkNotNull(appService, "appService must not be null");
        checkNotNull(changeLogService, "changeLogService must not be null");
        checkNotNull(entityAliasService, "entityAliasService cannot be null");
        checkNotNull(appTagService, "appTagService cannot be null");

        this.appService = appService;
        this.changeLogService = changeLogService;
        this.entityAliasService = entityAliasService;
        this.appTagService = appTagService;
    }


    @Override
    public void register() {
        registerQueries();
        registerAppRegistration();
        registerAppUpdate();
    }


    private void registerAppUpdate() {
        DatumRoute<Boolean> handleAppUpdateRequest = (req, res) -> {
            res.type(WebUtilities.TYPE_JSON);
            AppChangeAction appChange = readBody(req, AppChangeAction.class);
            LOG.info("Updating application: " + appChange);

            Long appId = appChange.app().id().get();

            EntityReference ref = ImmutableEntityReference.builder()
                    .kind(EntityKind.APPLICATION)
                    .id(appId)
                    .build();

            appChange.changes()
                    .forEach(c -> changeLogService.write(
                        ImmutableChangeLog.builder()
                                .message(c.toDescription())
                                .severity(Severity.INFORMATION)
                                .userId(WebUtilities.getUsername(req))
                                .parentReference(ref)
                                .operation(Operation.UPDATE)
                                .build()));

            appService.update(appChange.app());
            appTagService.updateTags(appId, appChange.tags());
            entityAliasService.updateAliases(ref, appChange.aliases());

            return true;
        };

        postForDatum(mkPath(BASE_URL, ":id"), handleAppUpdateRequest);

    }


    private void registerAppRegistration() {
        DatumRoute<AppRegistrationResponse> handleRegistrationRequest = (req, res) -> {
            res.type(WebUtilities.TYPE_JSON);
            AppRegistrationRequest registrationRequest = readBody(req, AppRegistrationRequest.class);

            LOG.info("Registering new application:" + registrationRequest);

            AppRegistrationResponse registrationResponse = appService
                    .registerApp(registrationRequest);

            if (registrationResponse.registered()) {
                ImmutableChangeLog changeLogEntry = ImmutableChangeLog.builder()
                        .message("Registered new application: " + registrationRequest.name())
                        .severity(Severity.INFORMATION)
                        .userId(WebUtilities.getUsername(req))
                        .parentReference(ImmutableEntityReference.builder()
                                .kind(EntityKind.APPLICATION)
                                .id(registrationResponse.id().get())
                                .build())
                        .operation(Operation.ADD)
                        .build();
                changeLogService.write(changeLogEntry);
            }

            return registrationResponse;
        };

        postForDatum(BASE_URL, handleRegistrationRequest);
    }


    private void registerQueries() {
        ListRoute<Application> searchRoute = (request, response) -> appService.search(request.params("query"));

        ListRoute<Tally<Long>> tallyByOrgUnitRoute = (request, response) -> appService.countByOrganisationalUnit();


        DatumRoute<Map<AssetCodeRelationshipKind, List<Application>>> findRelatedRoute
                = (req, res) -> appService.findRelated(getId(req));

        ListRoute<Application> findByIdsRoute = (req, res) -> {
            List<Long> ids = readIdsFromBody(req);
            if (ListUtilities.isEmpty(ids)) {
                return Collections.emptyList();
            }
            return appService
                    .findByIds(ids);
        };

        DatumRoute<Application> getByIdRoute = (req, res) -> {
            String id = req.params("id");
            return appService
                    .getById(parseLong(id));
        };

        ListRoute<String> findAllTagsRoute = (request, response)
                -> appTagService.findAllTags();

        ListRoute<Application> findByTagRoute = (request, response)
                -> appTagService.findByTag(request.body());

        ListRoute<Application> findBySelectorRoute = ((request, response)
                -> appService.findByAppIdSelector(readIdSelectionOptionsFromBody(request)));

        ListRoute<Application> findByAssetCodeRoute = ((request, response)
                -> appService.findByAssetCode(request.params("assetCode")));

        ListRoute<String> getAppTagsRoute = (request, response)
                -> appTagService.findTagsForApplication(getId(request));

        getForList(mkPath(BASE_URL, "search", ":query"), searchRoute);
        getForList(mkPath(BASE_URL, "count-by", "org-unit"), tallyByOrgUnitRoute);
        getForList(mkPath(BASE_URL, "tags"), findAllTagsRoute);

        getForDatum(mkPath(BASE_URL, "id", ":id"), getByIdRoute);
        getForList(mkPath(BASE_URL, "id", ":id", "tags"), getAppTagsRoute);
        getForDatum(mkPath(BASE_URL, "id", ":id", "related"), findRelatedRoute);

        postForList(mkPath(BASE_URL, "tags"), findByTagRoute);  // POST as may not be good for qparam
        postForList(mkPath(BASE_URL, "by-ids"), findByIdsRoute);
        postForList(mkPath(BASE_URL, "selector"), findBySelectorRoute);
        getForList(mkPath(BASE_URL, "asset-code", ":assetCode"), findByAssetCodeRoute);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApplicationEndpoint{");
        sb.append('}');
        return sb.toString();
    }
}
