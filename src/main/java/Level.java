
public enum Level {
	BEGINNER("入门",RandomAI.class),
	EASY("简单",AI.Default.class),
	NORMAL("普通",SmartEvalAI.class),
	HARD("困难",SmartSearchAI.class),
	;
	public final String name;
	public final Class<? extends AI> value;

	private Level(String name, Class<? extends AI> clz) {
		this.name = name;
		this.value = clz;
	}
}
