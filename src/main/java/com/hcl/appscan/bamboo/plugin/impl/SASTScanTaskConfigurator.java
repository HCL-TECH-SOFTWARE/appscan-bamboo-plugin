/**
 * (c) Copyright IBM Corporation 2016.
 * (c) Copyright HCL Technologies Ltd. 2020.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import com.hcl.appscan.bamboo.plugin.util.Utility;
import com.hcl.appscan.sdk.CoreConstants;
import com.hcl.appscan.sdk.scanners.dynamic.DASTConstants;
import org.apache.commons.lang3.StringUtils;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.credentials.CredentialsManager;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskRequirementSupport;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.i18n.I18nBean;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Scanned
public class SASTScanTaskConfigurator extends AbstractTaskConfigurator implements TaskRequirementSupport, ISASTConstants {
	
	private static final String CRED_LIST = "credList";	//$NON-NLS-1$
	private static final String TEST_TYPE_LIST = "testTypeList";	//$NON-NLS-1$
	private static final String SCAN_TYPE_LIST = "scanTypeList";	//$NON-NLS-1$
	private static final String TEST_OPTIMIZATION_LIST = "testOptimizationList";	//$NON-NLS-1$

	private UIConfigSupport uiConfigSupport;
	private CredentialsManager credentialsManager;
	private I18nBean i18nBean;
	
	public SASTScanTaskConfigurator(
			@ComponentImport UIConfigSupport uiConfigSupport, 
			@ComponentImport CredentialsManager credentialsManager, 
			@ComponentImport I18nBeanFactory i18nBeanFactory) {
		
		this.uiConfigSupport = uiConfigSupport;
		this.credentialsManager = credentialsManager;
		this.i18nBean = i18nBeanFactory.getI18nBean();
	}
	
	private Map<Long, String> getCredentials() {
		Map<Long, String> credentials = new Hashtable<Long, String>();
		for (CredentialsData data : credentialsManager.getAllCredentials())
			credentials.put(data.getId(), data.getName());
		return credentials;
	}
	
	@Override
	public void populateContextForCreate(Map<String, Object> context) {
		context.put(CRED_LIST, getCredentials());
		context.put(TEST_TYPE_LIST, Utility.getTestTypes());
		context.put(SCAN_TYPE_LIST, Utility.getScanTypes());
		context.put(TEST_OPTIMIZATION_LIST, Utility.getTestOptimizations());
	}
	
	@Override
	public void populateContextForEdit(Map<String, Object> context, TaskDefinition taskDefinition) {
		context.put(CRED_LIST, getCredentials());
		context.put(TEST_TYPE_LIST, Utility.getTestTypes());
		context.put(SCAN_TYPE_LIST, Utility.getScanTypes());
		context.put(TEST_OPTIMIZATION_LIST, Utility.getTestOptimizations());
		Map<String, String> config = taskDefinition.getConfiguration();
		context.put(CFG_SELECTED_CRED, config.get(CFG_SELECTED_CRED));
		context.put(CFG_SEL_TEST_TYPE, config.get(CFG_SEL_TEST_TYPE));
		context.put(CFG_APP_ID, config.get(CFG_APP_ID));
		context.put(CFG_SUSPEND, Boolean.valueOf(config.get(CFG_SUSPEND)));
		context.put(CFG_MAX_HIGH, config.get(CFG_MAX_HIGH));
		context.put(CFG_MAX_MEDIUM, config.get(CFG_MAX_MEDIUM));
		context.put(CFG_MAX_LOW, config.get(CFG_MAX_LOW));
		context.put(CoreConstants.TARGET, config.get(CoreConstants.TARGET));
		context.put(CFG_ADDITIONAL_OPTION_DYN, config.get(CFG_ADDITIONAL_OPTION_DYN));
		context.put(CFG_SEL_SCAN_TYPE, config.get(CFG_SEL_SCAN_TYPE));
		context.put(CFG_SEL_TEST_OPTIMIZE, config.get(CFG_SEL_TEST_OPTIMIZE));
		context.put(CFG_LOGIN_USER, config.get(CFG_LOGIN_USER));
		context.put(CFG_LOGIN_PASSWORD, config.get(CFG_LOGIN_PASSWORD));
		context.put(CFG_THIRD_CREDENTIAL, config.get(CFG_THIRD_CREDENTIAL));
		context.put(CFG_SEL_PRESENCE, config.get(CFG_SEL_PRESENCE));
		context.put(CFG_SCAN_FILE, config.get(CFG_SCAN_FILE));
		context.put(CFG_ADDITIONAL_OPTION_ST, config.get(CFG_ADDITIONAL_OPTION_ST));
		context.put(OPEN_SOURCE_ONLY, config.get(OPEN_SOURCE_ONLY));
		context.put(CUSTOM_TARGET, config.get(CUSTOM_TARGET));
	}
	
	private void validateRequired(ActionParametersMap params, ErrorCollection errorCollection, String field) {
		String value = params.getString(field);
		if (value == null || value.trim().isEmpty())
			errorCollection.addError(field, i18nBean.getText("err." + field)); //$NON-NLS-1$
	}
	
	private void validateNumber(ActionParametersMap params, ErrorCollection errorCollection, String field) {
		String value = params.getString(field);
		if (!("".equals(value) || StringUtils.isNumeric(value)))		//$NON-NLS-1$
			errorCollection.addError(field, i18nBean.getText("err.nan"));	//$NON-NLS-1$
	}
	
	@Override
	public void validate(ActionParametersMap params, ErrorCollection errorCollection) {
		validateRequired(params, errorCollection, CFG_SELECTED_CRED);
		validateRequired(params, errorCollection, CFG_APP_ID);
		validateRequired(params, errorCollection, CFG_SEL_TEST_TYPE);
		if (params.getString(CFG_SEL_TEST_TYPE) != null && DASTConstants.DYNAMIC_ANALYZER.equals(params.getString(CFG_SEL_TEST_TYPE))) {
			validateRequired(params, errorCollection, CoreConstants.TARGET);
		}
		validateNumber(params, errorCollection, CFG_MAX_HIGH);
		validateNumber(params, errorCollection, CFG_MAX_MEDIUM);
		validateNumber(params, errorCollection, CFG_MAX_LOW);
	}
	
	@Override
	public Map<String, String> generateTaskConfigMap(ActionParametersMap params, TaskDefinition previousTaskDefinition) {
		Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
		config.put(CFG_SELECTED_CRED, params.getString(CFG_SELECTED_CRED));
		config.put(CFG_SEL_TEST_TYPE, params.getString(CFG_SEL_TEST_TYPE));
		config.put(CFG_APP_ID, params.getString(CFG_APP_ID));
		config.put(CFG_SUSPEND, Boolean.toString(params.getBoolean(CFG_SUSPEND)));
		config.put(CFG_MAX_HIGH, params.getString(CFG_MAX_HIGH));
		config.put(CFG_MAX_MEDIUM, params.getString(CFG_MAX_MEDIUM));
		config.put(CFG_MAX_LOW, params.getString(CFG_MAX_LOW));
		config.put(CoreConstants.TARGET, params.getString(CoreConstants.TARGET));
		if (params.getBoolean(CFG_ADDITIONAL_OPTION_DYN)) {
			config.put(CFG_SEL_SCAN_TYPE, params.getString(CFG_SEL_SCAN_TYPE));
			config.put(CFG_SEL_TEST_OPTIMIZE, params.getString(CFG_SEL_TEST_OPTIMIZE));
			config.put(CFG_LOGIN_USER, params.getString(CFG_LOGIN_USER));
			config.put(CFG_LOGIN_PASSWORD, params.getString(CFG_LOGIN_PASSWORD));
			config.put(CFG_THIRD_CREDENTIAL, params.getString(CFG_THIRD_CREDENTIAL));
			config.put(CFG_SEL_PRESENCE, params.getString(CFG_SEL_PRESENCE));
			config.put(CFG_SCAN_FILE, params.getString(CFG_SCAN_FILE));
			config.put(CFG_ADDITIONAL_OPTION_DYN, params.getString(CFG_ADDITIONAL_OPTION_DYN));
		}
		if (params.getBoolean(CFG_ADDITIONAL_OPTION_ST)) {
			config.put(OPEN_SOURCE_ONLY, params.getString(OPEN_SOURCE_ONLY));
			config.put(CUSTOM_TARGET, params.getString(CUSTOM_TARGET));
			config.put(CFG_ADDITIONAL_OPTION_ST, params.getString(CFG_ADDITIONAL_OPTION_ST));
		}
		return config;
	}
	
	@Override
	public Set<Requirement> calculateRequirements(TaskDefinition taskDefinition) {
//		String selectedUtil = SA_CLIENT_UTIL_KEY + '.' + taskDefinition.getConfiguration().get(CFG_SELECTED_UTIL);
//		Requirement req = new RequirementImpl(SYS_BUILDER_PREFIX + selectedUtil, true, ".*"); //$NON-NLS-1$
//		return Collections.singleton(req);
		return new HashSet<Requirement>();
	}
}