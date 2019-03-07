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
package com.github.jochenw.qse.lin.core.api;

import java.util.List;
import java.util.Map;

public interface IPluginRegistry {
	public enum Type {
		license, matcher, family, archive, exclusion
	}
	public static class PluginInfo {
		private final String id, familyId, name, description;
		private final Type type;
		private final String className;
		private final boolean overriding;
		private final Map<String,String> properties;

		public PluginInfo(String pId, String pFamilyId, String pName, String pDescription, Type pType,
				String pClassName, Map<String, String> pProperties, boolean pOverriding) {
			id = pId;
			familyId = pFamilyId;
			name = pName;
			description = pDescription;
			type = pType;
			className = pClassName;
			properties = pProperties;
			overriding = pOverriding;
		}

		public String getId() {
			return id;
		}

		public String getFamilyId() {
			return familyId;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public Type getType() {
			return type;
		}

		public String getClassName() {
			return className;
		}

		public Map<String, String> getProperties() {
			return properties;
		}

		public boolean isOverriding() {
			return overriding;
		}
	}

	public List<PluginInfo> getLicenses();
	public List<PluginInfo> getExclusionLists();
	public List<PluginInfo> getFamilies();
	public List<PluginInfo> getMatchers();
	public List<PluginInfo> getArchiveHandlers();
}
