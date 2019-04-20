package com.github.jochenw.qse.is.core.api;

import java.util.function.Consumer;

import com.github.jochenw.qse.is.core.Scanner;

public interface Rule extends Consumer<Scanner> {
	String getId();
}
