package com.github.jochenw.qse.is.core.api;

import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.NSName;

public interface IServiceInvocationListener {
	void serviceInvocation(IsPackage pPackage, NSName pSource, NSName pTarget);
}
