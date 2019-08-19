package com.github.jochenw.qse.is.sonar.api;

import java.io.IOException;
import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;


public class SonarQseIsRulesDefinition implements RulesDefinition {
	private final SonarQseIsScannerProvider scannerProvider;

	public SonarQseIsRulesDefinition(SonarQseIsScannerProvider scannerProvider) {
		super();
		this.scannerProvider = scannerProvider;
	}


	@Override
	public void define(Context context) {
        final NewRepository repository = context.createRepository(
                SonarQseIsConstants.RULE_REPOSITORY_ID, "wm-is-flow").setName("SonarQseIs");
        findRules(repository);
        repository.done();
	}

	protected void findRules(NewRepository pRepository) {
		scannerProvider.getRules().forEach((r) -> {
			final String[] errorCodes = r.getErrorCodes();
			if (errorCodes != null) {
				for (String errorCode : errorCodes) {
					final String description = r.getMarkdownDescription(errorCode);
					NewRule rule = pRepository.createRule(r.getId() + ":" + errorCode)
							.setMarkdownDescription(description);
				}
			}
		});
	}
}
