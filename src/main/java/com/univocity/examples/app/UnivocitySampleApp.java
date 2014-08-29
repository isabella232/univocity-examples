/*******************************************************************************
 * Copyright (c) 2014 uniVocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 ******************************************************************************/
package com.univocity.examples.app;

import com.univocity.examples.app.swing.*;

public class UnivocitySampleApp extends DataAnalysisWindow {

	private static final long serialVersionUID = -4290799952797464413L;

	public UnivocitySampleApp() {
		super(new SampleAppConfig());
	}

	public static void main(String... args) {
		UnivocitySampleApp app = new UnivocitySampleApp();
		WindowUtils.displayWindow(app);
	}

}
