[@ui.bambooSection titleKey="section.asoc"]
	[@ww.label labelKey="label.cred" name="selectedCred"/]
	[@ww.label labelKey="label.appid" name="appId"/]
	[@ww.label labelKey="label.test.type" name="selectedTestType"/]
	[@ui.bambooSection dependsOn="selectedTestType" showOn="DynamicAnalyzer"]
		[@ww.label labelKey="label.target" name="target"/]
		[@ww.checkbox labelKey="label.additional.options" name="additionalOptionsDyn" toggle="true" disabled="true"/]
		[@ui.bambooSection titleKey="section.additional.opts" descriptionKey="section.additional.opts.desc.dyn" dependsOn="additionalOptionsDyn" showOn="true"]
			[@ww.label labelKey="dyn.label.scan.type" name="ScanType"/]
			[@ww.label labelKey="dyn.label.test.optimization" name="TestOptimizationLevel"/]
			[@ww.label labelKey="dyn.label.login.user" name="LoginUser"/]
			[@ww.label labelKey="dyn.label.login.password" name="LoginPassword" showPassword="true"/]
			[@ww.label labelKey="dyn.label.third.credential" name="extraField"/]
			[@ww.label labelKey="dyn.label.presence" name="PresenceId"/]
			[@ww.label labelKey="dyn.label.scan.file" name="ScanFile"/]
		[/@ui.bambooSection]
	[/@ui.bambooSection]
	[@ui.bambooSection dependsOn="selectedTestType" showOn="StaticAnalyzer"]
		[@ww.checkbox labelKey="label.additional.options" name="additionalOptionsSt" toggle="true" disabled="true"/]
		[@ui.bambooSection titleKey="section.additional.opts" descriptionKey="section.additional.opts.desc.st" dependsOn="additionalOptionsSt" showOn="true"]
			[@ww.checkbox labelKey="label.open.source.only" name="openSourceOnly" toggle="true" disabled="true"/]
			[@ww.label labelKey="label.custom.target" name="customTarget"/]
		[/@ui.bambooSection]
	[/@ui.bambooSection]
	[@ww.checkbox labelKey="label.suspend" name="suspendJob" toggle="true" disabled="true"/]
[/@ui.bambooSection]

[@ui.bambooSection titleKey="section.fail" descriptionKey="section.fail.desc" dependsOn="suspendJob" showOn="true"]
	[@ww.label labelKey="label.high" name="maxHigh"/]
	[@ww.label labelKey="label.medium" name="maxMedium"/]
	[@ww.label labelKey="label.low" name="maxLow"/]
[/@ui.bambooSection]
