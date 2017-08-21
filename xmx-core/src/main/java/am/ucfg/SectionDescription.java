// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.ucfg;

import java.util.HashMap;
import java.util.Map;

public class SectionDescription {
	private String name;
	private String[] sectionComments;
	private OptionDescription[] options;
	private Map<String, OptionDescription> optionsByName;
	
	public SectionDescription(String name, String[] sectionComments,
			OptionDescription...options) {
		super();
		this.name = name;
		this.sectionComments = sectionComments == null ? new String[0] : sectionComments;
		this.options = options;
		this.optionsByName = new HashMap<>(options.length);
		for (OptionDescription opt : options) {
			optionsByName.put(opt.name, opt);
		}
	}
	
	public String getName() {
		return name;
	}

	public String[] getSectionComments() {
		return sectionComments;
	}

	public OptionDescription[] getOptions() {
		return options;
	}

	public Map<String, OptionDescription> getOptionsByName() {
		return optionsByName;
	}

	boolean hasOption(String name) {
		return optionsByName.containsKey(name);
	}
	
	// hashCode and equals only by name! 

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SectionDescription other = (SectionDescription) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}