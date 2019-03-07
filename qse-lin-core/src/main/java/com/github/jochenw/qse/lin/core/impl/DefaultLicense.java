/*
 *    Copyright 2019 Jochen Wiedmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.jochenw.qse.lin.core.impl;

import com.github.jochenw.qse.lin.core.api.ILicense;

public class DefaultLicense implements ILicense {
	private String id, familyId, name, description;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getFamilyId() {
		return familyId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setId(String pId) {
		id = pId;
	}

	public void setFamilyId(String pFamilyId) {
		familyId = pFamilyId;
	}

	public void setName(String pName) {
		name = pName;
	}

	public void setDescription(String pDescription) {
		description = pDescription;
	}
}
