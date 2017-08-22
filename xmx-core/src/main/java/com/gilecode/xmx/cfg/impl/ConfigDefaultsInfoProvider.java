// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.ucfg.IConfigInfoProvider;
import com.gilecode.ucfg.SectionDescription;
import com.gilecode.xmx.cfg.CfgEntityLevel;
import com.gilecode.xmx.cfg.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Provides information about the default XMX configuration sections and
 * options.
 * <p/>
 * This implementation is NOT thread-safe!
 * 
 * @author Andrey Mogilev
 */
public class ConfigDefaultsInfoProvider implements IConfigInfoProvider {
	
	private String lastSectionName;
	private CfgEntityLevel lastSectionLevel;

	private Queue<SubstituteLoggingEvent> deferredLogEvents = new ArrayDeque<>();
	private List<SubstituteLogger> substLoggers = new ArrayList<>();
	private volatile boolean loggingInitialized;

	@Override
	public boolean isSupportedOption(String sectionName, String optionName) {
		CfgEntityLevel level;
		if (!sectionName.equals(lastSectionName)) {
			SectionHeader sh = new XmxCfgSectionNameParser().parseSectionHeader(sectionName);
			lastSectionLevel = sh.level;
			lastSectionName = sectionName;
		}
		level = lastSectionLevel;

		return Properties.isKnownProperty(level, optionName);
	}

	@Override
	public String[] getFileComments() {
		return ConfigDefaults.FILE_COMMENTS;
	}

	@Override
	public List<SectionDescription> getAllDefaultSectionsDescriptions() {
		return ConfigDefaults.ALL_SECTIONS;
	}

	@Override
	public List<SectionDescription> getAllDefaultHiddenSectionsDescriptions() {
		return ConfigDefaults.HIDDEN_INTERNAL_SECTIONS;
	}

	@Override
	public String getLineSeparator() {
		return ConfigDefaults.LINE_SEPARATOR;
	}

	@Override
	synchronized public Logger getLogger(Class<?> loggingClass) {
		if (loggingInitialized) {
			return LoggerFactory.getLogger(loggingClass);
		} else {
			SubstituteLogger sl = new SubstituteLogger(loggingClass.getName(), deferredLogEvents, false);
			substLoggers.add(sl);
			return sl;
		}
	}

	@Override
	synchronized public void onLoggingInitialized() {
		if (!loggingInitialized) {
			loggingInitialized = true;
			if (!substLoggers.isEmpty()) {
				updateSubstituteLoggers();
				replayDeferredLogEvents();
			}
		}
	}

	private void updateSubstituteLoggers() {
		for (SubstituteLogger substLogger : substLoggers) {
			substLogger.setDelegate(LoggerFactory.getLogger(substLogger.getName()));
		}
	}

	private void replayDeferredLogEvents() {
		if (!deferredLogEvents.isEmpty()) {
			if (!substLoggers.get(0).isDelegateEventAware()) {
				Logger thisLog = LoggerFactory.getLogger(this.getClass());
				thisLog.warn("SLF4J Logger implementation does not support interception during initialization; " +
						"lost {} logging events", deferredLogEvents.size());
			} else {
				for (SubstituteLoggingEvent event : deferredLogEvents) {
					SubstituteLogger substLogger = event.getLogger();
					substLogger.log(event);
				}
			}
		}
		deferredLogEvents.clear();
		substLoggers.clear();
	}
}
