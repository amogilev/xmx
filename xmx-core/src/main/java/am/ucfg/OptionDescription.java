package am.ucfg;

public class OptionDescription {
	String name;
	String defautValue;
	String[] comments;
	
	public OptionDescription(String name, Object defautValue,
			String... comments) {
		super();
		this.name = name;
		this.defautValue = defautValue.toString();
		this.comments = comments;
	}

	public String getName() {
		return name;
	}

	public String getDefautValue() {
		return defautValue;
	}

	public String[] getComments() {
		return comments;
	}
}