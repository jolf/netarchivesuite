/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.webinterface;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.eav.EAV;

/**
 * Contains utility methods for supporting event harvest GUI.
 */
public final class EventHarvestUtil {

    static final Logger log = LoggerFactory.getLogger(EventHarvestUtil.class);

    /**
     * Private Constructor. Instances are not meaningful.
     */
    private EventHarvestUtil() {
    }

    /**
     * Adds a bunch of configurations to a given PartialHarvest. For full definitions of the parameters, see
     * Definitions-add-event-seeds.jsp. For each seed in the list, the following steps are taken: 1) The domain is
     * parsed out of the seed. If no such domain is known, it is created with the usual defaults. 2) For each domain, a
     * configuration with the name &lt;harvestDefinition&gt;_&lt;orderTemplate&gt;_&lt;maxBytes&gt;Bytes is created
     * unless it already exists. The configuration uses orderTemplate, and the specified maxBytes. If maxBytes is
     * unspecified, its default value is used. The configuration is added to the harvest specified by the
     * harvestDefinition argument. 3) For each domain, a seedlist with the name
     * &lt;harvestDefinition&gt;_&lt;orderTemplate&gt;_&lt;maxBytes&gt;Bytes is created if it does not already exist and
     * the given url is added to it. This seedlist is the only seedlist associated with the configuration of the same
     * name.
     *
     * @param context the current JSP context
     * @param i18n the translation information to use in this context
     * @param eventHarvestName The name of the partial harvest to which these seeds are to be added
     * @throws ForwardedToErrorPage If maxBytes is not a number, or if any of the seeds is badly formatted such that no
     * domain name can be parsed from it, or if orderTemplate is not given or unknown.
     */
    public static void addConfigurations(PageContext context, I18n i18n, String eventHarvestName) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNull(eventHarvestName, "String eventHarvestName");

        HTMLUtils.forwardOnMissingParameter(context, Constants.SEEDS_PARAM);
        ServletRequest request = context.getRequest();

        // If no seeds are specified, just return
        String seeds = request.getParameter(Constants.SEEDS_PARAM);
        if (seeds == null || seeds.trim().length() == 0) {
            return;
        }
        // split the seeds up into individual seeds
        // Note: Matches any sort of newline (unix/mac/dos), but won't get empty
        // lines, which is fine for this purpose

        Set<String> seedSet = new HashSet<String>();
        for (String seed : seeds.split("[\n\r]+")) {
            seedSet.add(seed);
        }

        HTMLUtils.forwardOnEmptyParameter(context, Constants.ORDER_TEMPLATE_PARAM);
        String orderTemplate = request.getParameter(Constants.ORDER_TEMPLATE_PARAM);
        // Check that order template exists
        if (!TemplateDAO.getInstance().exists(orderTemplate)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.template.0.does.not.exist",
                    orderTemplate);
            throw new ForwardedToErrorPage("The orderTemplate with name '" + orderTemplate + "' does not exist!");
        }

        // Check that numerical parameters are meaningful and replace null or
        // empty with default values
        long maxBytes = HTMLUtils.parseOptionalLong(context, Constants.MAX_BYTES_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);
        long maxObjectsL = HTMLUtils.parseOptionalLong(context, Constants.MAX_OBJECTS_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS);
        int maxObjects = (int) maxObjectsL;
        
        Map<String,String> attributeValues = new HashMap<String,String>();
        // Fetch all attributes from context to be used later
        for (String attrParam: EAV.getAttributeNames(EAV.DOMAIN_TREE_ID)){
            String paramValue = context.getRequest().getParameter(attrParam);
            log.debug("Read attribute {}. The value in form: {}", attrParam, paramValue);
            attributeValues.put(attrParam, paramValue);
        }
        
        // All parameters are valid, so call method
        try {
            PartialHarvest eventHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                    eventHarvestName);
            eventHarvest.addSeeds(seedSet, orderTemplate, maxBytes, maxObjects, attributeValues);
        } catch (Exception e) {
            log.error("Unexpected exception thrown", e);
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;error.adding.seeds.to.0", eventHarvestName, e);
            throw new ForwardedToErrorPage("Error while adding seeds", e);
        }
    }

    /**
     * Add configurations to an existing selective harvest.
     *
     * @param context The current JSP context
     * @param i18n The translation information to use in this context
     * @param eventHarvestName The name of the partial harvest to which these seeds are to be added
     * @param seeds The seeds as a file (each seed on a separate line)
     * @param maxbytesString The given maxbytes as a string
     * @param maxobjectsString The given maxobjects as a string
     * @param maxrateString The given maxrate as a string (currently not used)
     * @param ordertemplate The name of the ordertemplate to use
     * @param attributes A list of attributes and form values
     */
    public static void addConfigurationsFromSeedsFile(PageContext context, I18n i18n, String eventHarvestName,
            File seeds, String maxbytesString, String maxobjectsString, String maxrateString, String ordertemplate, Map<String,String> attributes) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNullOrEmpty(eventHarvestName, "String eventHarvestName");
        ArgumentNotValid.checkNotNull(seeds, "String seeds");
        ArgumentNotValid.checkNotNull(ordertemplate, "String ordertemplate");

        long maxBytes = 0L;
        int maxObjects = 0;

        try {
            if (maxbytesString == null) {
                maxBytes = dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES;
            } else {
                Locale loc = HTMLUtils.getLocaleObject(context);
                maxBytes = HTMLUtils.parseLong(loc, maxbytesString, Constants.MAX_BYTES_PARAM,
                        dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);
            }

            if (maxobjectsString == null) {
                maxObjects = (int) dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS;
            } else {
                Locale loc = HTMLUtils.getLocaleObject(context);
                long maxObjectsL = HTMLUtils.parseLong(loc, maxobjectsString, Constants.MAX_OBJECTS_PARAM,
                        dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS);
                maxObjects = (int) maxObjectsL;
            }

        } catch (Exception e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "Exception.thrown.when.adding.seeds", e);
            return;
        }
        // Check that order template exists
        if (!TemplateDAO.getInstance().exists(ordertemplate)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.template.0.does.not.exist",
                    ordertemplate);
            throw new ForwardedToErrorPage("The orderTemplate with name '" + ordertemplate + "' does not exist!");
        }

        // All parameters are valid, so call method
        try {
            PartialHarvest eventHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                    eventHarvestName);
            
            eventHarvest.addSeedsFromFile(seeds, ordertemplate, maxBytes, maxObjects, attributes);
        } catch (Exception e) {
            log.error("Unexpected exception thrown", e);
            HTMLUtils
                    .forwardWithErrorMessage(context, i18n, "errormsg;error.adding.seeds.to.0", e, eventHarvestName, e);
            throw new ForwardedToErrorPage("Error while adding seeds", e);
        }
    }
}
