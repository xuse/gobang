
public enum Level {
	BEGINNER("初学者",AI.Beginner.class),
	EASY("入门",AI.Default.class),
	NORMAL("进阶",SmartEvalAI.class),
	HARD("高手",SmartSearchAI.class),
	;
	public final String name;
	public final Class<? extends AI> value;

	private Level(String name, Class<? extends AI> clz) {
		this.name = name;
		this.value = clz;
	}
}
