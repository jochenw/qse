package com.github.jochenw.qse.is.core.rules;

import java.util.function.Predicate;

import com.github.jochenw.qse.is.core.model.Node;

public interface NodeFilter extends Predicate<Node> {
	public String getId();
	public static NodeFilter ACCEPT_ALL = new NodeFilter() {
		@Override
		public boolean test(Node pNode) {
			return true;
		}

		@Override
		public String getId() {
			return "all";
		}
	};
	public static NodeFilter ACCEPT_NONE = new NodeFilter() {
		@Override
		public boolean test(Node pNode) {
			return false;
		}

		@Override
		public String getId() {
			return "all";
		}
	};

	public static NodeFilter[] BUILTIN_FILTERS = { ACCEPT_ALL, ACCEPT_NONE };
}
