/**
 * (c) Copyright HCL Technologies Ltd. 2020, 2021.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.hcl.appscan.bamboo.plugin.auth.AppScanAPICredentials;
import com.hcl.appscan.bamboo.plugin.util.ExecutorUtil;
import com.hcl.appscan.sdk.error.InvalidTargetException;
import com.hcl.appscan.sdk.error.ScannerException;
import com.hcl.appscan.sdk.scanners.dynamic.DASTConstants;
import com.hcl.appscan.sdk.scanners.sast.SASTConstants;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ScannerTask implements TaskType, IScannerConstants {
	private static final String SA_DIR = ".sa"; //$NON-NLS-1$

	private LogHelper logger;
	private IScanner scanner;

	private ArtifactManager artifactManager;
	private CapabilityContext capabilityContext;
	private ProcessService processService;

	public ScannerTask(
			@ComponentImport I18nBeanFactory i18nBeanFactory,
			@ComponentImport ProcessService processService,
			@ComponentImport ArtifactManager artifactManager,
			@ComponentImport CapabilityContext capabilityContext) {

		logger = new LogHelper(i18nBeanFactory.getI18nBean());

		this.artifactManager = artifactManager;
		this.capabilityContext = capabilityContext;
		this.processService = processService;
	}

	private void setUsernameAndPassword(TaskContext taskContext) {
		String keyId = taskContext.getConfigurationMap().get(CFG_API_KEY);
		String secret = taskContext.getConfigurationMap().get(CFG_API_SECRET);
		AppScanAPICredentials credentials = new AppScanAPICredentials(keyId, secret);
		scanner.setCredential(credentials);

		// this ensures password is masked in build log
		taskContext.getBuildContext().getVariableContext().addLocalVariable("asoc_password", credentials.getPassword()); //$NON-NLS-1$
	}

	private boolean checkFail(String value, String key, long actual) {
		if (value == null || value.equals("")) {    //$NON-NLS-1$
			logger.info(key + ".none");        //$NON-NLS-1$
			return false;
		}

		long limit = Long.parseLong(value);
		if (actual > limit) {
			logger.error(key + ".fail", actual, limit); //$NON-NLS-1$
			return true;
		}

		logger.info(key + ".pass", actual, limit); //$NON-NLS-1$
		return false;
	}

	private TaskResultBuilder calculateResult(TaskContext taskContext, TaskResultBuilder result) {
		Map<String, String> failSeverityLevelConfig = scanner.getFailSeverityLevelConfig(taskContext);

		boolean failed = checkFail(failSeverityLevelConfig.get(FAIL_NON_COMPLIANCE), FAIL_NON_COMPLIANCE, scanner.getTotalCount());
		failed |= checkFail(failSeverityLevelConfig.get(CFG_MAX_TOTAL), CFG_MAX_TOTAL, scanner.getTotalCount());
		failed |= checkFail(failSeverityLevelConfig.get(CFG_MAX_HIGH), CFG_MAX_HIGH, scanner.getHighCount());
		failed |= checkFail(failSeverityLevelConfig.get(CFG_MAX_MEDIUM), CFG_MAX_MEDIUM, scanner.getMediumCount());
		failed |= checkFail(failSeverityLevelConfig.get(CFG_MAX_LOW), CFG_MAX_LOW, scanner.getLowCount());

		if (failed)
			return result.failed();

		return result.success();
	}

	@Override
	public TaskResult execute(TaskContext taskContext) throws TaskException {
		logger.setLogger(taskContext.getBuildLogger());
		initScanner(taskContext);

		setUsernameAndPassword(taskContext);
		TaskResultBuilder result = TaskResultBuilder.newBuilder(taskContext);
		try {
			try {
				scanner.setWorkingDir(scanner.initWorkingDir(taskContext));
				scanner.scheduleScan(taskContext);
			} catch (InvalidTargetException e) {
				logger.error("err.scan.schedule", e.getLocalizedMessage());
				return result.failedWithError().build();
			} catch (ScannerException e) {
				logger.error("err.scan.schedule", e.getLocalizedMessage());
				return result.failedWithError().build();
			} catch (ArtifactsUnavailableException e) {
				logger.error(e.getLocalizedMessage());
				return result.failedWithError().build();
			}

			try {
				if (taskContext.getConfigurationMap().getAsBoolean(CFG_SUSPEND)) {
					scanner.waitAndDownloadResult(taskContext);
					return calculateResult(taskContext, result).build();
				} else {
					final TaskContext taskContext1 = taskContext;
					final IScanner scanner1 = scanner;
					final LogHelper logger1 = logger;
					Callable<Boolean> callable = new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							try {
								scanner1.waitAndDownloadResult(taskContext1);
							} catch (Exception e) {
								logger1.error("scan.error", e.getLocalizedMessage());
							}
							return true;
						}
					};
					Future future = ExecutorUtil.submitTask(callable);
				}
				return result.success().build();
			} catch (InterruptedException e) {
				logger.error("scan.interrupt");
				Thread.currentThread().interrupt();
				return result.failedWithError().build();
			} catch (TaskFailedException e) {
				return result.failedWithError().build();
			}
		} finally {
			try {
				scanner.cleanUpWorkingDir(taskContext);
			} catch (Exception e) {
			}
		}
	}

	private void initScanner(TaskContext taskContext) throws TaskException {
		String selectedType = taskContext.getConfigurationMap().get(CFG_SEL_TEST_TYPE);
		if (selectedType != null && selectedType.trim().length() > 0) {
			if (SASTConstants.STATIC_ANALYZER.equals(selectedType))
				scanner = new ASoCSASTScanner(logger, artifactManager, processService);
			else if (DASTConstants.DYNAMIC_ANALYZER.equals(selectedType))
				scanner = new ASoCDASTScanner(logger, artifactManager, processService);
			else {
				logger.error("err.invalidScanType", selectedType);
				throw new TaskException(logger.getText("err.invalidScanType", selectedType));
			}
		} else {
			logger.error("err.emptyScanType");
			throw new TaskException(logger.getText("err.emptyScanType"));
		}
	}
}
